package pipeline_stages;

import core.RegisterFile;
import core.Stats;
import pipeline_registers.MEM_WB;

// Write Back (WB)
public class WB_Stage {
    public void tick(MEM_WB memWb, RegisterFile rf, Stats stats) {
        if (memWb.isNop)
            return;

        // Only write back for instructions that produce a register result
        if (memWb.rd != 0 && memWb.opcode.writesBack()) {
            rf.write(memWb.rd, memWb.result);
        }
        stats.instructionsRetired++;
    }
}
