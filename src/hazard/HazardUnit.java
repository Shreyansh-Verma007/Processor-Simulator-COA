package hazard;

import common.Config;
import pipeline_registers.EX_MEM;
import pipeline_registers.ID_EX;
import pipeline_registers.IF_ID;

import pipeline_registers.MEM_WB;

// Detects hazards that require stalling (e.g., load-use, branch, or multi-cycle ops).
public class HazardUnit {

    public boolean needsStall(ID_EX idEx, IF_ID ifId, EX_MEM exMem, MEM_WB memWb, Config cfg) {
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
            if (idEx.opcode.isLoad()) {
                if (idEx.rd != 0 && (idEx.rd == incomingRs1 || idEx.rd == incomingRs2)) {
                    return true;
                }
            }

            // Case 3: No-forwarding RAW hazard — producer in EX (ID/EX)
            if (!cfg.isForwardingEnabled()) {
                if (idEx.rd != 0 && idEx.opcode.writesBack()) {
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
            if (!exMem.isNop && exMem.rd != 0 && exMem.opcode.writesBack()) {
                if (exMem.rd == incomingRs1 || exMem.rd == incomingRs2) {
                    return true;
                }
            }
        }

        // Case 5: No-forwarding RAW hazard — producer in WB (MEM/WB)
        // Note: Assuming the register file supports internal forwarding (write first half,
        // read second half), we do NOT stall if the producer is in the WB stage.
        // If internal forwarding was not supported, we would stall here:
        /*
        if (!cfg.isForwardingEnabled()) {
            if (!memWb.isNop && memWb.rd != 0 && memWb.opcode.writesBack()) {
                if (memWb.rd == incomingRs1 || memWb.rd == incomingRs2) {
                    return true;
                }
            }
        }
        */

        return false;
    }

}
