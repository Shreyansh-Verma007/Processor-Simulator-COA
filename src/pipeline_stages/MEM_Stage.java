package pipeline_stages;

import common.Opcode;
import core.Memory;
import pipeline_registers.EX_MEM;
import pipeline_registers.MEM_WB;

// Memory Access (MEM)
public class MEM_Stage {
    public MEM_WB tick(EX_MEM exMem, Memory mem) {
        MEM_WB out = new MEM_WB();
        if (exMem.isNop)
            return out;

        out.isNop = false;
        out.opcode = exMem.opcode;
        out.rd = exMem.rd;

        Opcode op = exMem.opcode;
        if (op == Opcode.LW) {
            out.result = mem.readWord(exMem.aluResult);
        } else if (op == Opcode.LB) {
            out.result = mem.readByte(exMem.aluResult);
        } else if (op == Opcode.SW) {
            mem.writeWord(exMem.aluResult, exMem.writeData);
        } else if (op == Opcode.SB) {
            mem.writeByte(exMem.aluResult, exMem.writeData);
        } else {
            out.result = exMem.aluResult;
        }
        return out;
    }
}
