package pipeline_stages;

import cache.CacheHierarchy;
import common.Config;
import common.Instruction;
import common.Opcode;
import core.Memory;
import core.RegisterFile;
import core.Stats;
import hazard.ForwardingUnit;
import hazard.HazardUnit;
import pipeline_registers.EX_MEM;
import pipeline_registers.ID_EX;
import pipeline_registers.IF_ID;
import pipeline_registers.MEM_WB;

import java.util.List;

/**
 * Orchestrates the 5-stage pipeline: IF → ID → EX → MEM → WB.
 *
 * Supports optional cache hierarchy for variable-latency IF and MEM stages.
 * When cache is null, operates in Phase 1 mode (single-cycle IF/MEM).
 */
public class PipelineController {

    private static final int DRAIN_THRESHOLD = 3;
    private static final int MAX_CYCLE_LIMIT = 100_000;

    public void run(List<Instruction> program, Memory mem, RegisterFile rf,
            Config cfg, Stats stats, CacheHierarchy cache) {

        // If cache is enabled, load program into memory for cache-based fetch
        if (cache != null) {
            mem.loadProgram(program, 0);
        }

        // Pipeline registers
        IF_ID ifId = new IF_ID();
        ID_EX idEx = new ID_EX();
        EX_MEM exMem = new EX_MEM();
        MEM_WB memWb = new MEM_WB();

        // Pipeline stages
        IF_Stage ifStage = new IF_Stage();
        ID_Stage idStage = new ID_Stage();
        EX_Stage exStage = new EX_Stage(rf, cfg);
        MEM_Stage memStage = new MEM_Stage();
        WB_Stage wbStage = new WB_Stage();

        // Hazard / forwarding units
        HazardUnit hazard = new HazardUnit();
        ForwardingUnit forwarding = new ForwardingUnit();

        int pc = 0x0000;
        int drainCycles = 0;

        // Cache stall counters
        int ifStallCycles = 0; // remaining IF stall cycles (cache miss)
        int memStallCycles = 0; // remaining MEM stall cycles (cache miss)

        // ── Simulation loop ──────────────────────────────────────────────

        while (true) {

            // ── Handle Cache Stalls (MEM takes priority over IF) ─────────
            if (memStallCycles > 0 || ifStallCycles > 0) {
                if (memStallCycles > 0)
                    memStallCycles--;
                else
                    ifStallCycles--;

                stats.cycles++;
                stats.stalls++;
                if (stats.cycles > MAX_CYCLE_LIMIT) {
                    System.err.println("WARNING: cycle limit reached.");
                    break;
                }
                continue;
            }

            boolean stall = hazard.needsStall(idEx, ifId, exMem, memWb, cfg);
            boolean isMultiCycleStall = (idEx.latencyCyclesLeft > 0);

            // Tick stages in reverse order (WB → MEM → EX)
            MEM_WB oldMemWb = memWb;
            wbStage.tick(memWb, rf, stats);
            MEM_WB newMemWb = memStage.tick(exMem, mem, cache);
            EX_MEM newExMem = exStage.tick(idEx, exMem, newMemWb, oldMemWb, forwarding);

            // Check for MEM cache stall
            if (newMemWb.memLatencyLeft > 0) {
                memStallCycles = newMemWb.memLatencyLeft;
                newMemWb.memLatencyLeft = 0;
            }

            memWb = newMemWb;
            exMem = newExMem;

            // ── Branch / stall resolution ────────────────────────────────

            if (!newExMem.isNop && newExMem.branchMispredicted) {
                pc = newExMem.branchRecoveryPC;
                ifId = new IF_ID();
                idEx = new ID_EX();
                stats.branchFlushes++;
                if (stall)
                    stats.stalls++;

            } else if (!newExMem.isNop && newExMem.branchTaken
                    && newExMem.opcode == Opcode.JAL) {
                pc = newExMem.jumpTarget;
                ifId = new IF_ID();
                idEx = new ID_EX();
                stats.branchFlushes++;
                if (stall)
                    stats.stalls++;

            } else if (stall) {
                if (!isMultiCycleStall) {
                    idEx = new ID_EX();
                }
                stats.stalls++;

            } else {
                // Normal flow: ID then IF
                idEx = idStage.tick(ifId, cfg);

                // BTFNT: backward branch predicted taken — redirect PC
                if (idEx.predictedPC != -1) {
                    pc = idEx.predictedPC;
                }

                ifId = ifStage.tick(program, pc, cache);
                pc += 4;

                // Check for IF cache stall
                if (ifId.fetchLatencyLeft > 0) {
                    ifStallCycles = ifId.fetchLatencyLeft;
                    ifId.fetchLatencyLeft = 0;
                }
            }

            // ── Cycle bookkeeping & termination ──────────────────────────

            stats.cycles++;

            if (exStage.haltFlag && ++drainCycles >= DRAIN_THRESHOLD) {
                break;
            }

            boolean pcPastEnd = (pc / 4) >= program.size();
            if (pcPastEnd && ifId.isNop && idEx.isNop && exMem.isNop && memWb.isNop) {
                break;
            }

            if (stats.cycles > MAX_CYCLE_LIMIT) {
                System.err.println("WARNING: cycle limit reached.");
                break;
            }
        }

        // Collect cache stats at end of simulation
        stats.collectCacheStats(cache);
    }
}
