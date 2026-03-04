package core;

import common.Config;
import common.Instruction;
import pipeline_stages.PipelineController;

import java.util.List;

// Top-level simulator controller.
public class Processor {
    private Memory mem = new Memory();
    private RegisterFile rf = new RegisterFile();
    private Config cfg = new Config();
    private Stats stats = new Stats();

    public void run(List<Instruction> program) {
        new PipelineController().run(program, mem, rf, cfg, stats);
    }

    // --- Accessors (used by test runner to check results after execution) ---

    public int getRegister(int n) {
        return rf.read(n);
    }

    public int getMemory(int address) {
        return mem.readWord(address);
    }

    public Stats getStats() {
        return stats;
    }

    public void preload(int address, int[] values) {
        mem.preload(address, values);
    }
}
