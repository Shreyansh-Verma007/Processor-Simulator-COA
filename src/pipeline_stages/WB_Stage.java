package pipeline_stages;

import common.Opcode;
import core.RegisterFile;
import core.Stats;
import pipeline_registers.MEM_WB;

// Write Back (WB)
public class WB_Stage {
    public void tick(MEM_WB memWb, RegisterFile rf, Stats stats) {
        if (memWb.isNop)
            return;

        // Only write back for instructions that produce a register result
        if (memWb.rd != 0 && writesBack(memWb.opcode)) {
            rf.write(memWb.rd, memWb.result);
        }
        stats.instructionsRetired++;
    }

    private boolean writesBack(Opcode op) {
        return op != Opcode.SW && op != Opcode.SB
                && op != Opcode.BEQ && op != Opcode.BNE
                && op != Opcode.BLT && op != Opcode.BGE
                && op != Opcode.ECALL && op != Opcode.HALT;
    }
}
