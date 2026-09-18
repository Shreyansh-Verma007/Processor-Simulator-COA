package step;

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
import pipeline_stages.*;

import java.util.List;

/**
 * Holds the full mutable pipeline state between step requests.
 *
 * Unlike PipelineController which runs to completion, StepSession
 * exposes a single step() method that advances the pipeline exactly
 * one clock cycle and returns a CycleSnapshot of the resulting state.
 */
public class StepSession {

    private static final int DRAIN_THRESHOLD = 3;
    private static final int MAX_CYCLE_LIMIT = 100_000;

    // Program being simulated
    private final List<Instruction> program;

    // Hardware components
    private final Memory mem;
    private final RegisterFile rf;
    private final Config cfg;
    private final Stats stats;
    private final CacheHierarchy cache;

    // Pipeline stages
    private final IF_Stage ifStage;
    private final ID_Stage idStage;
    private final EX_Stage exStage;
    private final MEM_Stage memStage;
    private final WB_Stage wbStage;

    // Hazard / forwarding
    private final HazardUnit hazardUnit;
    private final ForwardingUnit forwardingUnit;

    // Pipeline registers — mutable state between steps
    private IF_ID ifId;
    private ID_EX idEx;
    private EX_MEM exMem;
    private MEM_WB memWb;

    // PC and stall counters
    private int pc;
    private int ifStallCycles;
    private int memStallCycles;
    private int drainCycles;

    // Termination flag
    private boolean done;

    public StepSession(List<Instruction> program, Memory mem, RegisterFile rf,
                       Config cfg, Stats stats, CacheHierarchy cache) {
        this.program = program;
        this.mem = mem;
        this.rf = rf;
        this.cfg = cfg;
        this.stats = stats;
        this.cache = cache;

        // Load program into memory for cache-based fetch (same as PipelineController)
        if (cache != null) {
            mem.loadProgram(program, 0);
        }

        // Initialize pipeline stages
        this.ifStage = new IF_Stage();
        this.idStage = new ID_Stage();
        this.exStage = new EX_Stage(rf, cfg);
        this.memStage = new MEM_Stage();
        this.wbStage = new WB_Stage();

        this.hazardUnit = new HazardUnit();
        this.forwardingUnit = new ForwardingUnit();

        // Start with empty pipeline registers
        this.ifId = new IF_ID();
        this.idEx = new ID_EX();
        this.exMem = new EX_MEM();
        this.memWb = new MEM_WB();

        this.pc = 0x0000;
        this.ifStallCycles = 0;
        this.memStallCycles = 0;
        this.drainCycles = 0;
        this.done = false;
    }

    public boolean isDone() {
        return done;
    }

    /**
     * Advance the pipeline exactly one clock cycle.
     * Mirrors the logic in PipelineController.run() exactly.
     * Returns a CycleSnapshot capturing the state at the END of this cycle.
     */
    public CycleSnapshot step() {
        if (done) {
            return buildSnapshot("NONE", false, false, memWb);
        }

        boolean stall = false;
        boolean flush = false;
        String hazardType = "NONE";

        // ── Handle cache stalls (MEM takes priority over IF) ─────────────
        if (memStallCycles > 0 || ifStallCycles > 0) {
            if (memStallCycles > 0) memStallCycles--;
            else ifStallCycles--;

            stats.cycles++;
            stats.stalls++;
            stall = true;
            hazardType = "CACHE_STALL";

            // During a cache stall nothing moves — capture state as-is
            CycleSnapshot snap = buildSnapshot(hazardType, stall, flush, memWb);
            checkTermination();
            return snap;
        }

        stall = hazardUnit.needsStall(idEx, ifId, exMem, memWb, cfg);
        boolean isMultiCycleStall = (idEx.latencyCyclesLeft > 0);

        // Determine hazard type for UI
        if (stall) {
            if (idEx.opcode != null && (idEx.opcode == Opcode.LW || idEx.opcode == Opcode.LB)) {
                hazardType = "LOAD_USE";
            } else if (isMultiCycleStall) {
                hazardType = "MULTI_CYCLE";
            } else {
                hazardType = "RAW";
            }
        }

        // ── Tick stages in reverse order (WB → MEM → EX) ─────────────────
        // Capture memWb BEFORE wbStage.tick() so WB stage shows correct instruction
        MEM_WB wbSnapshot = memWb;
        wbStage.tick(memWb, rf, stats);
        MEM_WB newMemWb = memStage.tick(exMem, mem, cache);
        EX_MEM newExMem = exStage.tick(idEx, exMem, newMemWb, wbSnapshot, forwardingUnit);

        if (newMemWb.memLatencyLeft > 0) {
            memStallCycles = newMemWb.memLatencyLeft;
            newMemWb.memLatencyLeft = 0;
        }

        memWb = newMemWb;
        exMem = newExMem;

        // ── Branch / stall resolution ────────────────────────────────────
        if (!newExMem.isNop && newExMem.branchMispredicted) {
            flush = true;
            hazardType = "BRANCH_FLUSH";
            pc = newExMem.branchRecoveryPC;
            ifId = new IF_ID();
            idEx = new ID_EX();
            stats.branchFlushes++;
            if (stall) stats.stalls++;

        } else if (!newExMem.isNop && newExMem.branchTaken && newExMem.opcode == Opcode.JAL) {
            flush = true;
            hazardType = "BRANCH_FLUSH";
            pc = newExMem.jumpTarget;
            ifId = new IF_ID();
            idEx = new ID_EX();
            stats.branchFlushes++;
            if (stall) stats.stalls++;

        } else if (stall) {
            if (!isMultiCycleStall) {
                idEx = new ID_EX();
            }
            stats.stalls++;

        } else {
            // Normal flow: ID then IF
            idEx = idStage.tick(ifId, cfg);

            if (idEx.predictedPC != -1) {
                pc = idEx.predictedPC;
            }

            ifId = ifStage.tick(program, pc, cache);
            pc += 4;

            if (ifId.fetchLatencyLeft > 0) {
                ifStallCycles = ifId.fetchLatencyLeft;
                ifId.fetchLatencyLeft = 0;
            }
        }

        stats.cycles++;

        if (exStage.haltFlag && ++drainCycles >= DRAIN_THRESHOLD) {
            done = true;
        }

        boolean pcPastEnd = (pc / 4) >= program.size();
        if (pcPastEnd && ifId.isNop && idEx.isNop && exMem.isNop && memWb.isNop) {
            done = true;
        }

        if (stats.cycles > MAX_CYCLE_LIMIT) {
            done = true;
        }

        // Collect cache stats at end
        if (done && cache != null) {
            stats.collectCacheStats(cache);
        }

        return buildSnapshot(hazardType, stall, flush, wbSnapshot);
    }

    /**
     * Build a CycleSnapshot from the current pipeline register state.
     * wbSnapshot is the MEM_WB register captured BEFORE wbStage.tick() ran,
     * so the WB slot shows the instruction that is completing this cycle.
     */
    private CycleSnapshot buildSnapshot(String hazardType, boolean stall, boolean flush, MEM_WB wbSnap) {
        CycleSnapshot snap = new CycleSnapshot();
        snap.cycle = stats.cycles;
        snap.done = done;
        snap.stall = stall;
        snap.flush = flush;
        snap.hazardType = hazardType;
        snap.pc = pc;
        snap.registers = rf.getAll();
        snap.totalCycles = stats.cycles;
        snap.totalStalls = stats.stalls;
        snap.totalFlushes = stats.branchFlushes;
        snap.instructionsRetired = stats.instructionsRetired;

        snap.stages = new CycleSnapshot.StageState[5];
        snap.stages[0] = makeStage("IF",  ifId,  stall, flush);
        snap.stages[1] = makeStage("ID",  idEx,  stall, flush);
        snap.stages[2] = makeStage("EX",  exMem, false, flush);
        snap.stages[3] = makeStage("MEM", memWb, false, false);
        snap.stages[4] = makeStage("WB",  wbSnap,false, false);

        return snap;
    }

    // ── Stage state builders (overloaded for each register type) ────────────

    private CycleSnapshot.StageState makeStage(String name, IF_ID reg, boolean stall, boolean flush) {
        CycleSnapshot.StageState s = new CycleSnapshot.StageState();
        s.name = name;
        s.isNop = reg.isNop;
        s.isStall = stall && !reg.isNop;
        s.isFlush = flush && reg.isNop;
        s.instrPc = reg.pc;
        if (reg.isNop || reg.instruction == null) {
            s.label = stall ? "STALL" : "NOP";
        } else {
            Instruction instr = reg.instruction;
            s.label = CycleSnapshot.labelForOpcode(
                instr.opcode(), instr.rd(), instr.rs1(), instr.rs2(), instr.immediate(), reg.pc);
        }
        return s;
    }

    private CycleSnapshot.StageState makeStage(String name, ID_EX reg, boolean stall, boolean flush) {
        CycleSnapshot.StageState s = new CycleSnapshot.StageState();
        s.name = name;
        s.isNop = reg.isNop;
        s.isStall = stall && !reg.isNop;
        s.isFlush = flush && reg.isNop;
        s.instrPc = reg.pc;
        if (reg.isNop || reg.opcode == null) {
            s.label = stall ? "STALL" : "NOP";
        } else {
            s.label = CycleSnapshot.labelForOpcode(
                reg.opcode, reg.rd, reg.rs1, reg.rs2, reg.immediate, reg.pc);
        }
        return s;
    }

    private CycleSnapshot.StageState makeStage(String name, EX_MEM reg, boolean stall, boolean flush) {
        CycleSnapshot.StageState s = new CycleSnapshot.StageState();
        s.name = name;
        s.isNop = reg.isNop;
        s.isStall = false;
        s.isFlush = flush && !reg.isNop;
        s.instrPc = 0;
        if (reg.isNop || reg.opcode == null) {
            s.label = flush ? "FLUSHED" : "NOP";
        } else {
            s.label = CycleSnapshot.labelForOpcode(reg.opcode, reg.rd, 0, 0, 0, 0);
        }
        return s;
    }

    private CycleSnapshot.StageState makeStage(String name, MEM_WB reg, boolean stall, boolean flush) {
        CycleSnapshot.StageState s = new CycleSnapshot.StageState();
        s.name = name;
        s.isNop = reg.isNop;
        s.isStall = false;
        s.isFlush = false;
        s.instrPc = 0;
        if (reg.isNop || reg.opcode == null) {
            s.label = "NOP";
        } else {
            s.label = CycleSnapshot.labelForOpcode(reg.opcode, reg.rd, 0, 0, 0, 0);
        }
        return s;
    }

    private void checkTermination() {
        if (stats.cycles > MAX_CYCLE_LIMIT) {
            done = true;
        }
    }
}
