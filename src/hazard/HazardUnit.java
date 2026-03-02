package hazard;

import common.Opcode;
import pipeline_registers.ID_EX;
import pipeline_registers.IF_ID;

// Detects hazards that require stalling (e.g., load-use or multi-cycle ops).
public class HazardUnit {

    public boolean needsStall(ID_EX idEx, IF_ID ifId) {
        if (idEx.isNop)
            return false; // nothing in EX — no hazard

        // Case 1: Multi-cycle op still executing
        if (idEx.latencyCyclesLeft > 0)
            return true;

        // Case 2: Load-use hazard
        // The instruction in EX is a load, and the instruction in ID needs the result
        if (isLoad(idEx.opcode) && !ifId.isNop && ifId.instruction != null) {
            int incomingRs1 = ifId.instruction.rs1(); // source registers of next instruction
            int incomingRs2 = ifId.instruction.rs2();
            if (idEx.rd != 0 && (idEx.rd == incomingRs1 || idEx.rd == incomingRs2)) {
                return true; // must stall 1 cycle
            }
        }
        return false;
    }

    private boolean isLoad(Opcode op) {
        return op == Opcode.LW || op == Opcode.LB;
    }
}
