package core;

import cache.CacheHierarchy;
import common.Config;
import common.Instruction;
import compiler.CompilationResult;
import pipeline_stages.PipelineController;

import java.util.List;

// Top-level simulator controller.
public class Processor {
    private final Memory mem;
    private final RegisterFile rf;
    private final Config cfg;
    private final Stats stats;
    private CacheHierarchy cache;

    public Processor(Config cfg) {
        this.mem = new Memory();
        this.rf = new RegisterFile();
        this.cfg = cfg;
        this.stats = new Stats();

        // Build cache hierarchy if configured
        if (cfg.hasCacheConfig()) {
            this.cache = new CacheHierarchy(
                    cfg.getL1I(), cfg.getL1D(), cfg.getL2(),
                    cfg.getMainMemoryLatency(), mem);
        }
    }

    /** Run from a CompilationResult — loads .data items into memory first. */
    public void run(CompilationResult result) {
        if (result.getDataItems() != null && !result.getDataItems().isEmpty()) {
            mem.loadDataItems(result.getDataItems());
        }
        run(result.getInstructions());
    }

    /** Internal: run from a plain instruction list. */
    private void run(List<Instruction> program) {
        new PipelineController().run(program, mem, rf, cfg, stats, cache);
    }

    // --- Accessors ---

    public Stats getStats() {
        return stats;
    }
}
