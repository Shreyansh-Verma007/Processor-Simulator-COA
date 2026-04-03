package pipeline_stages;

import common.Config;
import common.Instruction;
import core.RegisterFile;
import pipeline_registers.ID_EX;
import pipeline_registers.IF_ID;

/**
 * Instruction Decode (ID) Stage.
 *
 * Decodes the fetched instruction and applies BTFNT static branch prediction:
 * - Backward branches (negative offset) → predicted TAKEN
 * - Forward branches (positive offset) → predicted NOT TAKEN
 */
public class ID_Stage {

    public ID_EX tick(IF_ID ifId, RegisterFile rf, Config cfg) {
        ID_EX out = new ID_EX();
        if (ifId.isNop || ifId.instruction == null) {
            return out;
        }

        Instruction instr = ifId.instruction;

        // Populate decoded fields
        out.isNop = false;
        out.opcode = instr.opcode();
        out.rd = instr.rd();
        out.rs1 = instr.rs1();
        out.rs2 = instr.rs2();

        out.immediate = instr.immediate();
        out.pc = ifId.pc;

        // Multi-cycle latency (e.g., MUL, DIV)
        int latency = cfg.getLatency(instr.opcode());
        out.latencyCyclesLeft = (latency > 1) ? latency - 1 : 0;

        // BTFNT: predict backward branches as taken
        if (out.opcode.isBranch() && out.immediate < 0) {
            out.branchPredictedTaken = true;
            out.predictedPC = out.pc + out.immediate;
        }

        return out;
    }
}
