package pipeline_stages;

import common.Instruction;
import pipeline_registers.IF_ID;
import java.util.List;

// Instruction Fetch (IF)
public class IF_Stage {
    public IF_ID tick(List<Instruction> program, int pc) {
        IF_ID out = new IF_ID();
        int index = pc / 4;
        if (index >= 0 && index < program.size()) {
            out.instruction = program.get(index);
            out.pc = pc;
            out.isNop = false;
        }
        // else: stays as NOP (end of program reached)
        return out;
    }
}
