package pipeline_stages;

import core.RegisterFile;
import core.Stats;
import pipeline_registers.MEM_WB;

// Write Back (WB)
public class WB_Stage {
    public void tick(MEM_WB memWb, RegisterFile rf, Stats stats) {
        if (memWb.isNop)
            return;
        rf.write(memWb.rd, memWb.result);
        stats.instructionsRetired++;
    }
}
