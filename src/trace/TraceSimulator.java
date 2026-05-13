package trace;

import common.Config;
import core.Memory;
import core.Stats;
import vm.TranslationResult;
import vm.VirtualMemoryUnit;

import java.io.PrintStream;
import java.util.List;

/**
 * Trace replay simulator.
 *
 * Reads pre-parsed trace instructions and simulates execution with:
 *   - Virtual memory (TLB + page table + page fault handling)
 *   - L1D data cache (PIPT — physically indexed, physically tagged)
 *   - Simple latency accumulation (no pipeline stages)
 *
 * For each instruction, the simulator charges:
 *   - L/S: translation latency + cache access latency
 *   - ADD: configured latency (default 1 cycle)
 *   - MUL: configured latency (default 3 cycles)
 */
public class TraceSimulator {

    private final Config cfg;
    private final Stats stats;
    private final VirtualMemoryUnit vmu;

    // Simple L1D cache (no L2 in trace mode, direct to memory on miss)
    private TraceDataCache dataCache;
    private Memory physicalMemory;

    // Simple register file for trace mode
    private final int[] registers = new int[32];

    public TraceSimulator(Config cfg) {
        this.cfg = cfg;
        this.stats = new Stats();

        // Set up physical memory first (VMU needs it for swap)
        this.physicalMemory = new Memory(cfg.getPhysicalSizeBytes());
        this.vmu = new VirtualMemoryUnit(cfg, physicalMemory);

        // Set up L1D cache for trace mode (no L2, direct to memory on miss)
        if (cfg.getL1D() != null) {
            this.dataCache = new TraceDataCache(
                    cfg.getL1D(), cfg.getMainMemoryLatency(), physicalMemory);
        }
    }

    /**
     * Run the trace simulation.
     */
    public void run(List<TraceInstruction> instructions) {
        int totalInstructions = instructions.size();

        for (int i = 0; i < totalInstructions; i++) {
            TraceInstruction instr = instructions.get(i);
            executeInstruction(instr);
        }

        // Collect cache stats at end
        if (dataCache != null) {
            stats.l1dHits = dataCache.getHits();
            stats.l1dMisses = dataCache.getMisses();
        }

        // Collect VM stats
        stats.tlbHits = vmu.getTlbHits();
        stats.tlbMisses = vmu.getTlbMisses();
        stats.pageWalks = vmu.getPageWalks();
        stats.pageFaults = vmu.getPageFaults();
        stats.pageEvictions = vmu.getPageEvictions();
        stats.dirtyEvictions = vmu.getDirtyEvictions();
        stats.totalTranslationPenaltyCycles = vmu.getTotalTranslationPenalty();
        stats.swapOuts = vmu.getSwapOuts();
        stats.swapIns = vmu.getSwapIns();

        // Write swap space dump to swap.txt
        vmu.writeSwapFile();
    }

    private void executeInstruction(TraceInstruction instr) {
        switch (instr.type) {
            case LOAD:
                executeLoad(instr);
                break;
            case STORE:
                executeStore(instr);
                break;
            case ADD:
                executeAdd(instr);
                break;
            case MUL:
                executeMul(instr);
                break;
        }
        stats.instructionsRetired++;
    }

    /**
     * LOAD: translate VA → PA, then read through L1D cache.
     * Total cycles = translation_latency + cache_latency
     */
    private void executeLoad(TraceInstruction instr) {
        // Step 1: Virtual → Physical translation
        TranslationResult tr = vmu.translateAddress(instr.address, false);
        int physAddr = tr.physicalAddress;
        int cycles = tr.latencyCycles;

        // Step 2: Access L1D cache with physical address (PIPT)
        if (dataCache != null) {
            TraceDataCache.Result ar = dataCache.read(physAddr);
            cycles += ar.latency;
            if (instr.rd != 0) {
                registers[instr.rd] = ar.data;
            }
        } else {
            cycles += 1; // default 1 cycle if no cache
            if (instr.rd != 0) {
                registers[instr.rd] = physicalMemory.readWord(physAddr);
            }
        }

        stats.cycles += cycles;
        stats.stalls += (cycles - 1); // 1 cycle is "normal", rest are stalls
    }

    /**
     * STORE: translate VA → PA (mark dirty), then write through L1D cache.
     * Total cycles = translation_latency + cache_latency
     */
    private void executeStore(TraceInstruction instr) {
        // Step 1: Virtual → Physical translation (isStore=true sets dirty bits)
        TranslationResult tr = vmu.translateAddress(instr.address, true);
        int physAddr = tr.physicalAddress;
        int cycles = tr.latencyCycles;

        // Step 2: Access L1D cache with physical address (PIPT)
        int storeValue = registers[instr.rs1];
        if (dataCache != null) {
            TraceDataCache.Result ar = dataCache.write(physAddr, storeValue);
            cycles += ar.latency;
        } else {
            cycles += 1; // default 1 cycle if no cache
            physicalMemory.writeWord(physAddr, storeValue);
        }

        stats.cycles += cycles;
        stats.stalls += (cycles - 1);
    }

    /**
     * ADD: rd = rs1 + rs2, costs configured latency (default 1 cycle).
     */
    private void executeAdd(TraceInstruction instr) {
        int result = registers[instr.rs1] + registers[instr.rs2];
        if (instr.rd != 0) {
            registers[instr.rd] = result;
        }
        int latency = cfg.getLatency(common.Opcode.ADD);
        stats.cycles += latency;
        if (latency > 1) {
            stats.stalls += (latency - 1);
        }
    }

    /**
     * MUL: rd = rs1 * rs2, costs configured latency (default 3 cycles).
     */
    private void executeMul(TraceInstruction instr) {
        int result = registers[instr.rs1] * registers[instr.rs2];
        if (instr.rd != 0) {
            registers[instr.rd] = result;
        }
        int latency = cfg.getLatency(common.Opcode.MUL);
        stats.cycles += latency;
        if (latency > 1) {
            stats.stalls += (latency - 1);
        }
    }

    /**
     * Print all simulation statistics.
     */
    public void printStats(PrintStream out) {
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

    public Stats getStats() {
        return stats;
    }
}
