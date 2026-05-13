package common;

import core.Stats;

import java.io.PrintStream;

/**
 * Centralized statistics and configuration printer.
 * Both pipeline mode and trace replay mode use this single class
 * for all formatted output — no more scattered print logic.
 */
public class StatsPrinter {

    // ── Pipeline Mode Output ─────────────────────────────────────────────

    /**
     * Print pipeline simulation stats (Phase 1/2 format).
     */
    public static void printPipelineStats(PrintStream out, Stats s, Config cfg) {
        out.println("=== Simulation Stats ===");
        out.println("Cycles             : " + s.cycles);
        out.println("Stalls             : " + s.stalls);
        out.println("Branch Flushes     : " + s.branchFlushes);
        out.println("Instructions Retired: " + s.instructionsRetired);
        out.printf("IPC                : %.3f%n", s.getIPC());

        out.println("\n--- Cache Configuration ---");
        if (cfg.getL1I() != null)
            out.println("L1I  : " + cfg.getL1I());
        if (cfg.getL1D() != null)
            out.println("L1D  : " + cfg.getL1D());
        if (cfg.getL2() != null)
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

    // ── Trace Replay Output ──────────────────────────────────────────────

    /**
     * Print trace replay header (trace path, config, instruction count).
     */
    public static void printTraceHeader(PrintStream out, String tracePath,
                                        String configPath, int instrCount) {
        out.println("=== RISC-V Trace Replay Simulator ===");
        out.println("Trace : " + tracePath);
        out.println("Config: " + configPath);
        out.println("Instructions: " + instrCount);
        out.println();
    }

    /**
     * Print full trace simulation stats (execution + VM + cache).
     */
    public static void printTraceStats(PrintStream out, Stats stats) {
        out.println("=== Trace Replay Simulation Stats ===");
        out.println();

        out.println("--- Execution ---");
        out.println("Total Cycles              : " + stats.cycles);
        out.println("Instructions Retired      : " + stats.instructionsRetired);
        out.printf("IPC                       : %.4f%n", stats.getIPC());
        out.println("Stalls                    : " + stats.stalls);
        out.println();

        out.println("--- Virtual Memory ---");
        out.println("TLB Hits                  : " + stats.tlbHits);
        out.println("TLB Misses                : " + stats.tlbMisses);
        out.printf("TLB Hit Rate              : %.4f%n",
                (stats.tlbHits + stats.tlbMisses) == 0 ? 0.0 :
                        (double) stats.tlbHits / (stats.tlbHits + stats.tlbMisses));
        out.println("Page Walks                : " + stats.pageWalks);
        out.println("Page Faults               : " + stats.pageFaults);
        out.println("Page Evictions            : " + stats.pageEvictions);
        out.println("Dirty Evictions           : " + stats.dirtyEvictions);
        out.println("Swap Outs (to disk)       : " + stats.swapOuts);
        out.println("Swap Ins  (from disk)     : " + stats.swapIns);
        out.println("Translation Penalty Cycles: " + stats.totalTranslationPenaltyCycles);
        out.println();

        out.println("--- Cache Statistics ---");
        out.printf("L1D Hits                  : %d%n", stats.l1dHits);
        out.printf("L1D Misses                : %d%n", stats.l1dMisses);
        out.printf("L1D Miss Rate             : %.4f%n",
                stats.getMissRate(stats.l1dHits, stats.l1dMisses));
    }

    // ── Shared Config Section ────────────────────────────────────────────

    /**
     * Print the VM + cache configuration section (used by both modes).
     */
    public static void printConfigSection(PrintStream out, Config cfg) {
        out.println();
        out.println("--- Configuration ---");
        out.println("Virtual Memory    : " + cfg.getVirtualSizeBytes() + " bytes");
        out.println("Physical Memory   : " + cfg.getPhysicalSizeBytes() + " bytes");
        out.println("Page Size         : " + cfg.getPageSizeBytes() + " bytes");
        out.println("DTLB Entries      : " + cfg.getDtlbEntries());
        out.println("TLB Hit Latency   : " + cfg.getTlbHitLatency() + " cycles");
        out.println("Page Walk Latency : " + cfg.getPageWalkLatency() + " cycles");
        out.println("Page Fault Latency: " + cfg.getPageFaultLatency() + " cycles");
        out.println("Replacement Policy: " + cfg.getVmReplacementPolicy());
        out.println("Forwarding        : " + (cfg.isForwardingEnabled() ? "enabled" : "disabled"));
        if (cfg.getL1D() != null)
            out.println("L1D Cache         : " + cfg.getL1D());
    }
}
