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
//   Pipeline mode : java Main [input.asm]
//   Single trace  : java Main --trace <trace_file>
//   Batch traces  : java Main --trace-all <trace_dir>
//   API server    : java Main --server
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length >= 1 && args[0].equals("--trace")) {
            runTraceMode(args);
        } else if (args.length >= 1 && args[0].equals("--trace-all")) {
            runBatchTraceMode(args);
        } else if (args.length >= 1 && args[0].equals("--server")) {
            ApiServer.start();
            // Keep the main thread alive
            Thread.currentThread().join();
        } else {
            runPipelineMode(args);
        }
    }

    // ── Single trace replay ──────────────────────────────────────────────

    /**
     * Usage: java Main --trace <trace_file>
     */
    private static void runTraceMode(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java Main --trace <trace_file>");
            System.exit(1);
        }

        String tracePath = args[1];
        String configPath = "default (Config.java)";

        Config cfg = new Config();

        List<TraceInstruction> instructions = TraceParser.parse(tracePath);
        TraceSimulator simulator = new TraceSimulator(cfg);
        simulator.run(instructions);

        // Write results to traces_output/<trace_name>_output.txt
        File outDir = new File("traces_output");
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        File traceFile = new File(tracePath);
        String baseName = traceFile.getName();
        if (baseName.endsWith(".trace")) {
            baseName = baseName.substring(0, baseName.length() - 6);
        }
        File outFile = new File(outDir, baseName + "_output.txt");

        try (PrintStream out = new PrintStream(new FileOutputStream(outFile))) {
            StatsPrinter.printTraceHeader(out, tracePath, configPath, instructions.size());
            StatsPrinter.printTraceStats(out, simulator.getStats());
            StatsPrinter.printConfigSection(out, cfg);
        }

        System.err.println("Trace replay complete. Results written to " + outFile.getPath());
    }

    // ── Batch trace replay (all traces → one file) ───────────────────────

    /**
     * Usage: java Main --trace-all <trace_dir>
     * Runs every *.trace file in the directory and writes individual results
     * into the traces_output/ directory.
     */
    private static void runBatchTraceMode(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java Main --trace-all <trace_dir>");
            System.exit(1);
        }

        String traceDir = args[1];
        String configPath = "default (Config.java)";

        Config cfg = new Config();

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

        // Create traces_output directory
        File outDir = new File("traces_output");
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        System.err.println("=== Batch Trace Replay ===");
        System.err.println("Config: " + configPath);
        System.err.println("Traces: " + traceFiles.length + " files from " + traceDir);
        System.err.println();

        for (int i = 0; i < traceFiles.length; i++) {
            File traceFile = traceFiles[i];
            System.err.println("[" + (i + 1) + "/" + traceFiles.length + "] Processing " + traceFile.getName() + "...");

            // Config is stateless after load — reuse the same instance
            List<TraceInstruction> instructions = TraceParser.parse(traceFile.getPath());
            TraceSimulator simulator = new TraceSimulator(cfg);
            simulator.run(instructions);

            // e.g., trace01.trace -> trace01_output.txt
            String baseName = traceFile.getName();
            if (baseName.endsWith(".trace")) {
                baseName = baseName.substring(0, baseName.length() - 6);
            }
            File outFile = new File(outDir, baseName + "_output.txt");

            try (PrintStream out = new PrintStream(new FileOutputStream(outFile))) {
                StatsPrinter.printTraceHeader(out, traceFile.getPath(), configPath, instructions.size());
                StatsPrinter.printTraceStats(out, simulator.getStats());
                StatsPrinter.printConfigSection(out, cfg);
            }
        }

        System.err.println("Batch complete. Results written to traces_output/ directory.");
    }

    // ── Pipeline mode (Phase 1/2 backward compatible) ────────────────────

    /**
     * Usage: java Main [input.asm]
     */
    private static void runPipelineMode(String[] args) throws Exception {
        String asmPath = (args.length > 0) ? args[0] : "input.asm";
        runPipelinePublic(asmPath);
    }

    /**
     * Public entry point for pipeline mode — callable by ApiServer.
     * Accepts an explicit ASM file path so each API request uses its own temp file.
     * Uses default Config (backward-compatible).
     */
    public static void runPipelinePublic(String asmPath) throws Exception {
        runPipelinePublic(asmPath, new Config());
    }

    /**
     * Public entry point for pipeline mode with a custom Config.
     * Called by ApiServer when the frontend sends config query params.
     */
    public static void runPipelinePublic(String asmPath, Config cfg) throws Exception {

        Processor processor = new Processor(cfg);

        // Save original stdout so we can restore it after redirecting to console.txt
        PrintStream origOut = System.out;

        try (PrintStream fileOut = new PrintStream(new FileOutputStream("console.txt"), true)) {
            System.setOut(fileOut);

            System.out.println("=== RISC-V Pipeline Simulator ===");
            System.out.println("Loading: " + asmPath);

            CompilationResult result = new Compiler().compile(asmPath);
            System.out.println("Compiled " + result.getInstructions().size() + " instructions.\n");

            processor.run(result);

            // If the program had a data segment, dump it to verify memory (e.g., sorted arrays)
            if (result.getDataItems() != null && !result.getDataItems().isEmpty()) {
                int totalBytes = 0;
                int startAddr = result.getDataItems().get(0).address;
                for (compiler.DataItem item : result.getDataItems()) {
                    totalBytes += item.bytes.length;
                }
                int dataWords = (totalBytes + 3) / 4;

                System.out.println("\n=== Memory Dump (Data Segment) ===");
                for (int i = 0; i < dataWords; i++) {
                    int addr = startAddr + i * 4;
                    int val;
                    if (processor.getCache() != null) {
                        val = processor.getCache().readData(addr).data;
                    } else {
                        val = processor.getMemory().readWord(addr);
                    }
                    System.out.printf("0x%04X: %d\n", addr, val);
                }
            }
        } finally {
            // Always restore stdout so the HTTP server keeps working after simulation
            System.setOut(origOut);
        }

        // Write stats to output.txt via the shared StatsPrinter
        Stats s = processor.getStats();
        try (PrintStream out = new PrintStream(new FileOutputStream("output.txt"))) {
            StatsPrinter.printPipelineStats(out, s, cfg);
        }
    }
}
