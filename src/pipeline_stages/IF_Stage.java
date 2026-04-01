package pipeline_stages;

import cache.AccessResult;
import cache.CacheHierarchy;
import common.Instruction;
import common.InstructionEncoder;
import pipeline_registers.IF_ID;

import java.util.List;

// Instruction Fetch (IF) — fetches via cache hierarchy when available.
public class IF_Stage {

    /**
     * Fetch an instruction. If cache hierarchy is available, fetches from memory
     * through L1I cache (variable latency). Otherwise, fetches directly from the
     * instruction list (1-cycle, backward compatible).
     */
    public IF_ID tick(List<Instruction> program, int pc, CacheHierarchy cache) {
        IF_ID out = new IF_ID();

        if (cache != null) {
            // Fetch through cache hierarchy
            AccessResult result = cache.fetchInstruction(pc);
            Instruction instr = InstructionEncoder.decode(result.data);
            if (instr != null) {
                out.instruction = instr;
                out.pc = pc;
                out.isNop = false;
                out.fetchLatencyLeft = result.latencyCycles - 1; // -1 because this tick counts as 1
            }
        } else {
            // Direct fetch from list (Phase 1 backward compatibility)
            int index = pc / 4;
            if (index >= 0 && index < program.size()) {
                out.instruction = program.get(index);
                out.pc = pc;
                out.isNop = false;
            }
        }
        return out;
    }
}
