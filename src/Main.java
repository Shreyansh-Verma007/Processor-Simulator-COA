import compiler.CompilationResult;
import compiler.Compiler;
import common.Config;
import core.Processor;

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

        // Redirect all output to console.txt (nothing printed to terminal)
        Processor processor = new Processor(cfg);
        try (PrintStream fileOut = new PrintStream(new FileOutputStream("console.txt"), true)) {
            System.setOut(fileOut);

            System.out.println("=== RISC-V Pipeline Simulator ===");
            System.out.println("Loading: " + asmPath);
            if (cacheCfgPath != null) {
                System.out.println("Cache config: " + cacheCfgPath);
            }

            // Compile assembly
            CompilationResult result = new Compiler().compile(asmPath);
            System.out.println("Compiled " + result.getInstructions().size() + " instructions.\n");

            // Main simulation loop
            processor.run(result);
        }

        // Save stats to file
        try (PrintStream statsOut = new PrintStream(new FileOutputStream("output.txt"))) {
            statsOut.println("=== Simulation Stats ===");
            statsOut.println("Cycles             : " + processor.getStats().cycles);
            statsOut.println("Stalls             : " + processor.getStats().stalls);
            statsOut.println("Branch Flushes     : " + processor.getStats().branchFlushes);
            statsOut.println("Instructions Retired: " + processor.getStats().instructionsRetired);
            statsOut.printf("IPC                : %.3f%n", processor.getStats().getIPC());

            // Cache stats (if cache was configured)
            core.Stats s = processor.getStats();
            statsOut.println("\n--- Cache Statistics ---");
            statsOut.printf("L1I  : %d hits, %d misses, miss rate %.3f%n",
                    s.l1iHits, s.l1iMisses, s.getMissRate(s.l1iHits, s.l1iMisses));
            statsOut.printf("L1D  : %d hits, %d misses, miss rate %.3f%n",
                    s.l1dHits, s.l1dMisses, s.getMissRate(s.l1dHits, s.l1dMisses));
            statsOut.printf("L2   : %d hits, %d misses, miss rate %.3f%n",
                    s.l2Hits, s.l2Misses, s.getMissRate(s.l2Hits, s.l2Misses));
        }
    }
}
