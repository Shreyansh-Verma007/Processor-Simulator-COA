import compiler.CompilationResult;
import compiler.Compiler;
import common.Config;
import common.StatsPrinter;
import core.Processor;
import core.Stats;
import trace.TraceInstruction;
import trace.TraceParser;
import trace.TraceSimulator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

// Entry point for the RISC-V pipeline simulator.
// Usage:
//   Pipeline mode : java Main [input.asm] [config.txt]
//   Single trace  : java Main --trace <trace_file> <config.txt>
//   Batch traces  : java Main --trace-all <trace_dir> <config.txt>
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length >= 1 && args[0].equals("--trace")) {
            runTraceMode(args);
        } else if (args.length >= 1 && args[0].equals("--trace-all")) {
            runBatchTraceMode(args);
        } else {
            runPipelineMode(args);
        }
    }

    // ── Single trace replay ──────────────────────────────────────────────

    /**
     * Usage: java Main --trace <trace_file> <config.txt>
     */
    private static void runTraceMode(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: java Main --trace <trace_file> <config.txt>");
            System.exit(1);
        }

        String tracePath = args[1];
        String configPath = args[2];

        Config cfg = new Config();
        cfg.loadConfig(configPath);

        List<TraceInstruction> instructions = TraceParser.parse(tracePath);
        TraceSimulator simulator = new TraceSimulator(cfg);
        simulator.run(instructions);

        // Write results to output.txt
        try (PrintStream out = new PrintStream(new FileOutputStream("output.txt"))) {
            StatsPrinter.printTraceHeader(out, tracePath, configPath, instructions.size());
            simulator.printStats(out);
            StatsPrinter.printConfigSection(out, cfg);
        }

        System.err.println("Trace replay complete. Results written to output.txt");
    }

    // ── Batch trace replay (all traces → one file) ───────────────────────

    /**
     * Usage: java Main --trace-all <trace_dir> <config.txt>
     * Runs every *.trace file in the directory and writes all results
     * into a single all_results.txt.
     */
    private static void runBatchTraceMode(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: java Main --trace-all <trace_dir> <config.txt>");
            System.exit(1);
        }

        String traceDir = args[1];
        String configPath = args[2];

        Config cfg = new Config();
        cfg.loadConfig(configPath);

        // Find all .trace files, sorted by name
        File dir = new File(traceDir);
        if (!dir.isDirectory()) {
            System.err.println("Error: " + traceDir + " is not a directory");
            System.exit(1);
        }
        File[] traceFiles = dir.listFiles((d, name) -> name.endsWith(".trace"));
        if (traceFiles == null || traceFiles.length == 0) {
            System.err.println("Error: no .trace files found in " + traceDir);
            System.exit(1);
        }
        Arrays.sort(traceFiles);

        // Run all traces, write consolidated output
        try (PrintStream out = new PrintStream(new FileOutputStream("all_results.txt"))) {
            out.println("=== Batch Trace Replay Results ===");
            out.println("Config: " + configPath);
            out.println("Traces: " + traceFiles.length + " files from " + traceDir);
            StatsPrinter.printConfigSection(out, cfg);
            out.println();

            for (int i = 0; i < traceFiles.length; i++) {
                File traceFile = traceFiles[i];
                System.err.println("[" + (i + 1) + "/" + traceFiles.length + "] " + traceFile.getName());

                // Config is stateless after load — reuse the same instance
                List<TraceInstruction> instructions = TraceParser.parse(traceFile.getPath());
                TraceSimulator simulator = new TraceSimulator(cfg);
                simulator.run(instructions);

                out.println("═══════════════════════════════════════════════════");
                out.println("  Trace " + (i + 1) + ": " + traceFile.getName());
                out.println("  Instructions: " + instructions.size());
                out.println("═══════════════════════════════════════════════════");
                simulator.printStats(out);
                out.println();
            }

            out.println("=== End of Batch Results ===");
        }

        System.err.println("Batch complete. All results written to all_results.txt");
    }

    // ── Pipeline mode (Phase 1/2 backward compatible) ────────────────────

    /**
     * Usage: java Main [input.asm] [config.txt]
     */
    private static void runPipelineMode(String[] args) throws Exception {
        String asmPath = (args.length > 0) ? args[0] : "input.asm";
        String cfgPath = (args.length > 1) ? args[1] : null;

        Config cfg = new Config();
        if (cfgPath != null) {
            cfg.loadConfig(cfgPath);
        }

        Processor processor = new Processor(cfg);
        // Redirect pipeline output to console.txt
        try (PrintStream fileOut = new PrintStream(new FileOutputStream("console.txt"), true)) {
            System.setOut(fileOut);

            System.out.println("=== RISC-V Pipeline Simulator ===");
            System.out.println("Loading: " + asmPath);
            if (cfgPath != null) {
                System.out.println("Config: " + cfgPath);
            }

            CompilationResult result = new Compiler().compile(asmPath);
            System.out.println("Compiled " + result.getInstructions().size() + " instructions.\n");

            processor.run(result);
        }

        // Write stats to output.txt via the shared StatsPrinter
        Stats s = processor.getStats();
        try (PrintStream out = new PrintStream(new FileOutputStream("output.txt"))) {
            StatsPrinter.printPipelineStats(out, s, cfg);
        }
    }
}
