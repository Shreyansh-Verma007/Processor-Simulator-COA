package pipeline_stages;

import common.Config;
import common.Instruction;
import core.RegisterFile;
import pipeline_registers.ID_EX;
import pipeline_registers.IF_ID;

// Instruction Decode (ID)
public class ID_Stage {
    public ID_EX tick(IF_ID ifId, RegisterFile rf, Config cfg) {
        ID_EX out = new ID_EX();
        if (ifId.isNop || ifId.instruction == null)
            return out;

        Instruction instr = ifId.instruction;
        out.isNop = false;
        out.opcode = instr.opcode();
        out.rd = instr.rd();
        out.rs1 = instr.rs1();
        out.rs2 = instr.rs2();
        out.valA = rf.read(instr.rs1());
        out.valB = rf.read(instr.rs2());
        out.immediate = instr.immediate();
        out.pc = ifId.pc;

        // Multi-cycle ops (e.g., MUL, DIV)
        int latency = cfg.getLatency(instr.opcode());
        out.latencyCyclesLeft = latency > 1 ? latency - 1 : 0;

        return out;
    }

}
