package hazard;

import common.Opcode;
import pipeline_registers.EX_MEM;
import pipeline_registers.ID_EX;
import pipeline_registers.MEM_WB;

// Handles data hazards via forwarding/bypassing.
public class ForwardingUnit {

    // Check if rs1 needs forwarding.
    public ForwardResult getForwardA(ID_EX idEx, EX_MEM exMem, MEM_WB memWb) {
        if (!exMem.isNop && exMem.rd != 0
                && exMem.rd == idEx.rs1
                && writesBackFromEX(exMem.opcode)) {
            return ForwardResult.FROM_EX_MEM;
        }
        if (!memWb.isNop && memWb.rd != 0
                && memWb.rd == idEx.rs1
                && writesBackFromMEM(memWb.opcode)) {
            return ForwardResult.FROM_MEM_WB;
        }
        return ForwardResult.NONE; // no hazard — use register file value
    }

    // Same logic for operand B (rs2).
    public ForwardResult getForwardB(ID_EX idEx, EX_MEM exMem, MEM_WB memWb) {
        if (!exMem.isNop && exMem.rd != 0
                && exMem.rd == idEx.rs2
                && writesBackFromEX(exMem.opcode)) {
            return ForwardResult.FROM_EX_MEM;
        }
        if (!memWb.isNop && memWb.rd != 0
                && memWb.rd == idEx.rs2
                && writesBackFromMEM(memWb.opcode)) {
            return ForwardResult.FROM_MEM_WB;
        }
        return ForwardResult.NONE;
    }

    // Check if opcode writes back from EX stage (standard R-type/I-type).
    private boolean writesBackFromEX(Opcode op) {
        if (op == null)
            return false;
        return op != Opcode.SW && op != Opcode.SB // stores
                && op != Opcode.BEQ && op != Opcode.BNE // branches
                && op != Opcode.BLT && op != Opcode.BGE
                && op != Opcode.ECALL && op != Opcode.HALT // system
                && op != Opcode.LW && op != Opcode.LB; // loads (not ready yet!)
    }

    // Check if opcode writes back from MEM stage (includes loads).
    private boolean writesBackFromMEM(Opcode op) {
        if (op == null)
            return false;
        return op != Opcode.SW && op != Opcode.SB
                && op != Opcode.BEQ && op != Opcode.BNE
                && op != Opcode.BLT && op != Opcode.BGE
                && op != Opcode.ECALL && op != Opcode.HALT;
        // Note: LW and LB ARE included here — their data is now available
    }
}
