package hazard;

import common.Config;
import common.Opcode;
import pipeline_registers.EX_MEM;
import pipeline_registers.ID_EX;
import pipeline_registers.IF_ID;

// Detects hazards that require stalling (e.g., load-use, branch, or multi-cycle ops).
public class HazardUnit {

    public boolean needsStall(ID_EX idEx, IF_ID ifId, EX_MEM exMem, Config cfg) {
        // Case 1: Multi-cycle op still executing in EX
        if (!idEx.isNop && idEx.latencyCyclesLeft > 0)
            return true;

        if (ifId.isNop || ifId.instruction == null)
            return false;

        int incomingRs1 = ifId.instruction.rs1();
        int incomingRs2 = ifId.instruction.rs2();

        // Checks that depend on the instruction currently in EX (ID_EX)
        if (!idEx.isNop) {
            // Case 2: Load-use hazard (always stall, even with forwarding)
            // The instruction in EX is a load, and the instruction in ID needs the result
            if (isLoad(idEx.opcode)) {
                if (idEx.rd != 0 && (idEx.rd == incomingRs1 || idEx.rd == incomingRs2)) {
                    return true;
                }
            }

            // Case 3: No-forwarding RAW hazard — producer in EX (ID/EX)
            if (!cfg.isForwardingEnabled()) {
                if (idEx.rd != 0 && writesBack(idEx.opcode)) {
                    if (idEx.rd == incomingRs1 || idEx.rd == incomingRs2) {
                        return true;
                    }
                }
            }
        }

        // Case 4: No-forwarding RAW hazard — producer in MEM (EX/MEM)
        // This must run even when idEx is a NOP bubble (inserted by a previous stall),
        // because the producer may have advanced from EX to MEM and still hasn't
        // written back to the register file yet.
        if (!cfg.isForwardingEnabled()) {
            if (!exMem.isNop && exMem.rd != 0 && writesBack(exMem.opcode)) {
                if (exMem.rd == incomingRs1 || exMem.rd == incomingRs2) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean writesBack(Opcode op) {
        if (op == null)
            return false;
        return op != Opcode.SW && op != Opcode.SB
                && op != Opcode.BEQ && op != Opcode.BNE
                && op != Opcode.BLT && op != Opcode.BGE
                && op != Opcode.ECALL && op != Opcode.HALT;
    }

    private boolean isLoad(Opcode op) {
        return op == Opcode.LW || op == Opcode.LB;
    }

    private boolean isBranch(Opcode op) {
        return op == Opcode.BEQ || op == Opcode.BNE
                || op == Opcode.BLT || op == Opcode.BGE;
    }
}
