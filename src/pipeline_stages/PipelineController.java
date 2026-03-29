package pipeline_stages;

import common.Config;
import common.Instruction;
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

// Orchestrates the 5-stage pipeline (IF -> ID -> EX -> MEM -> WB).
public class PipelineController {

    public void run(List<Instruction> program, Memory mem, RegisterFile rf, Config cfg, Stats stats) {
        IF_ID ifId = new IF_ID();
        ID_EX idEx = new ID_EX();
        EX_MEM exMem = new EX_MEM();
        MEM_WB memWb = new MEM_WB();

        IF_Stage ifStage = new IF_Stage();
        ID_Stage idStage = new ID_Stage();
        EX_Stage exStage = new EX_Stage(rf, cfg);
        MEM_Stage memStage = new MEM_Stage();
        WB_Stage wbStage = new WB_Stage();

        HazardUnit hazard = new HazardUnit();
        ForwardingUnit forwarding = new ForwardingUnit();

        int pc = 0x0000;
        int drainCycles = 0;

        // Simulation loop
        while (true) {

            boolean stall = hazard.needsStall(idEx, ifId, exMem, cfg);
            boolean isMultiCycleStall = (idEx.latencyCyclesLeft > 0);

            MEM_WB oldMemWb = memWb;
            wbStage.tick(memWb, rf, stats);

            MEM_WB newMemWb = memStage.tick(exMem, mem);

            EX_MEM newExMem = exStage.tick(idEx, exMem, newMemWb, oldMemWb, forwarding);

            memWb = newMemWb;
            exMem = newExMem;

            // Check if the instruction just processed by EX is a taken branch.
            // This must be checked regardless of stall state, because the branch
            // was resolved during this cycle's EX tick.
            if (!newExMem.isNop && newExMem.branchTaken) {
                // Branch was taken — flush and redirect
                pc = newExMem.jumpTarget;
                ifId = new IF_ID();
                idEx = new ID_EX();
                stats.branchFlushes++;
                if (stall) stats.stalls++; // the stall cycle still counts
            } else if (stall) {
                if (!isMultiCycleStall) {
                    idEx = new ID_EX();
                }
                stats.stalls++;
            } else {
                idEx = idStage.tick(ifId, rf, cfg);
                ifId = ifStage.tick(program, pc);
                pc += 4;
            }

            stats.cycles++;

            if (exStage.haltFlag) {
                drainCycles++;
                if (drainCycles >= 3)
                    break;
            }

            // Natural program end: all instructions have drained through the pipeline
            boolean pcPastEnd = (pc / 4) >= program.size();
            if (pcPastEnd && ifId.isNop && idEx.isNop && exMem.isNop && memWb.isNop) {
                break;
            }

            // Safety limit to prevent infinite loops
            if (stats.cycles > 100_000) {
                System.err.println("WARNING: cycle limit reached.");
                break;
            }
        }
    }
}
