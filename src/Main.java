import compiler.CompilationResult;
import compiler.Compiler;
import common.Config;
import core.Processor;
import core.Stats;

import java.io.FileOutputStream;
import java.io.PrintStream;

// Entry point for the RISC-V pipeline simulator.
// Usage: java Main [input.asm] [cache_config.txt]
public class Main {
    public static void main(String[] args) throws Exception {
        String asmPath = (args.length > 0) ? args[0] : "input.asm";
        String cacheCfgPath = (args.length > 1) ? args[1] : null;

        // Build configuration
        Config cfg = new Config();
        if (cacheCfgPath != null) {
            cfg.loadCacheConfig(cacheCfgPath);
        }

        // Redirect simulation output to console.txt
        Processor processor = new Processor(cfg);
        try (PrintStream fileOut = new PrintStream(new FileOutputStream("console.txt"), true)) {
            System.setOut(fileOut);

            System.out.println("=== RISC-V Pipeline Simulator ===");
            System.out.println("Loading: " + asmPath);
            if (cacheCfgPath != null) {
                System.out.println("Cache config: " + cacheCfgPath);
            }

            CompilationResult result = new Compiler().compile(asmPath);
            System.out.println("Compiled " + result.getInstructions().size() + " instructions.\n");

            processor.run(result);
        }

        // Write stats to output.txt
        Stats s = processor.getStats();
        try (PrintStream out = new PrintStream(new FileOutputStream("output.txt"))) {
            out.println("=== Simulation Stats ===");
            out.println("Cycles             : " + s.cycles);
            out.println("Stalls             : " + s.stalls);
            out.println("Branch Flushes     : " + s.branchFlushes);
            out.println("Instructions Retired: " + s.instructionsRetired);
            out.printf("IPC                : %.3f%n", s.getIPC());

            out.println("\n--- Cache Configuration ---");
            out.println("L1I  : " + cfg.getL1I());
            out.println("L1D  : " + cfg.getL1D());
            out.println("L2   : " + cfg.getL2());
            out.println("Memory Latency: " + cfg.getMainMemoryLatency() + " cycles");
            out.println("Forwarding    : " + (cfg.isForwardingEnabled() ? "enabled" : "disabled"));

            out.println("\n--- Cache Statistics ---");
            out.printf("L1I  : %d hits, %d misses, miss rate %.3f%n",
                    s.l1iHits, s.l1iMisses, s.getMissRate(s.l1iHits, s.l1iMisses));
            out.printf("L1D  : %d hits, %d misses, miss rate %.3f%n",
                    s.l1dHits, s.l1dMisses, s.getMissRate(s.l1dHits, s.l1dMisses));
            out.printf("L2   : %d hits, %d misses, miss rate %.3f%n",
                    s.l2Hits, s.l2Misses, s.getMissRate(s.l2Hits, s.l2Misses));
        }
    }
}
