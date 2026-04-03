package hazard;

import pipeline_registers.EX_MEM;
import pipeline_registers.ID_EX;
import pipeline_registers.MEM_WB;

// Handles data hazards via forwarding/bypassing.
public class ForwardingUnit {

    // Check if rs1 needs forwarding.
    public ForwardResult getForwardA(ID_EX idEx, EX_MEM exMem, MEM_WB memWb) {
        if (!exMem.isNop && exMem.rd != 0
                && exMem.rd == idEx.rs1
                && exMem.opcode.writesBack() && !exMem.opcode.isLoad()) {
            return ForwardResult.FROM_EX_MEM;
        }
        if (!memWb.isNop && memWb.rd != 0
                && memWb.rd == idEx.rs1
                && memWb.opcode.writesBack()) {
            return ForwardResult.FROM_MEM_WB;
        }
        return ForwardResult.NONE; // no hazard — use register file value
    }

    // Same logic for operand B (rs2).
    public ForwardResult getForwardB(ID_EX idEx, EX_MEM exMem, MEM_WB memWb) {
        if (!exMem.isNop && exMem.rd != 0
                && exMem.rd == idEx.rs2
                && exMem.opcode.writesBack() && !exMem.opcode.isLoad()) {
            return ForwardResult.FROM_EX_MEM;
        }
        if (!memWb.isNop && memWb.rd != 0
                && memWb.rd == idEx.rs2
                && memWb.opcode.writesBack()) {
            return ForwardResult.FROM_MEM_WB;
        }
        return ForwardResult.NONE;
    }
}
