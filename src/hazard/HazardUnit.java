package hazard;

import common.Config;
import common.Opcode;
import pipeline_registers.ID_EX;
import pipeline_registers.IF_ID;

// Detects hazards that require stalling (e.g., load-use or multi-cycle ops).
public class HazardUnit {

    public boolean needsStall(ID_EX idEx, IF_ID ifId, Config cfg) {
        if (idEx.isNop)
            return false; // nothing in EX — no hazard

        // Case 1: Multi-cycle op still executing
        if (idEx.latencyCyclesLeft > 0)
            return true;

        // Case 2: Load-use hazard (always applies, even with forwarding)
        // The instruction in EX is a load, and the instruction in ID needs the result
        if (isLoad(idEx.opcode) && !ifId.isNop && ifId.instruction != null) {
            int incomingRs1 = ifId.instruction.rs1();
            int incomingRs2 = ifId.instruction.rs2();
            if (idEx.rd != 0 && (idEx.rd == incomingRs1 || idEx.rd == incomingRs2)) {
                return true; // must stall 1 cycle
            }
        }

        // Case 3: No forwarding — stall for ALL RAW data dependencies
        if (!cfg.isForwardingEnabled() && !ifId.isNop && ifId.instruction != null) {
            if (idEx.rd != 0 && writesBack(idEx.opcode)) {
                int incomingRs1 = ifId.instruction.rs1();
                int incomingRs2 = ifId.instruction.rs2();
                if (idEx.rd == incomingRs1 || idEx.rd == incomingRs2) {
                    return true; // must stall until WB completes
                }
            }
        }
        return false;
    }

    // Check if an opcode writes to a destination register
    private boolean writesBack(Opcode op) {
        return op != Opcode.SW && op != Opcode.SB
                && op != Opcode.BEQ && op != Opcode.BNE
                && op != Opcode.BLT && op != Opcode.BGE
                && op != Opcode.ECALL && op != Opcode.HALT;
    }

    private boolean isLoad(Opcode op) {
        return op == Opcode.LW || op == Opcode.LB;
    }
}
