package trace;

import common.Config;
import core.Memory;
import core.Stats;
import vm.TranslationResult;
import vm.VirtualMemoryUnit;
import cache.CacheHierarchy;

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

    // Cache hierarchy for trace mode (supports L1D and L2)
    private CacheHierarchy cache;
    private Memory physicalMemory;

    // Simple register file for trace mode
    private final int[] registers = new int[32];

    public TraceSimulator(Config cfg) {
        this.cfg = cfg;
        this.stats = new Stats();

        // Set up physical memory first (VMU needs it for swap)
        this.physicalMemory = new Memory(cfg.getPhysicalSizeBytes());
        this.vmu = new VirtualMemoryUnit(cfg, physicalMemory);

        // Set up cache hierarchy (no L1I in trace mode, but L1D and L2 are supported)
        if (cfg.hasCacheConfig()) {
            this.cache = new CacheHierarchy(
                    null, cfg.getL1D(), cfg.getL2(),
                    cfg.getMainMemoryLatency(), physicalMemory);
            // Wire VMU → cache for PIPT frame invalidation on page eviction
            this.vmu.setCacheHierarchy(this.cache);
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
        if (cache != null) {
            stats.collectCacheStats(cache);
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
        // Instruction fetch latency: each instruction incurs a memory fetch penalty.
        int fetchCycles = cfg.getMainMemoryLatency();
        stats.cycles += fetchCycles;
        if (fetchCycles > 1) {
            stats.stalls += (fetchCycles - 1);
        }
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

        // Step 2: Access L1D/L2 cache with physical address (PIPT)
        if (cache != null) {
            cache.AccessResult ar = cache.readData(physAddr);
            cycles += ar.latencyCycles;
            if (instr.rd != 0) {
                registers[instr.rd] = ar.data;
            }
        } else {
            // No cache — charge main memory latency directly
            cycles += cfg.getMainMemoryLatency();
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

        // Step 2: Access L1D/L2 cache with physical address (PIPT)
        int storeValue = registers[instr.rs1];
        if (cache != null) {
            cache.AccessResult ar = cache.writeData(physAddr, storeValue);
            cycles += ar.latencyCycles;
        } else {
            // No cache — charge main memory latency directly
            cycles += cfg.getMainMemoryLatency();
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


    public Stats getStats() {
        return stats;
    }
}
