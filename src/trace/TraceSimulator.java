package trace;

import cache.CacheHierarchy;
import cache.AccessResult;
import common.Config;
import common.StatsPrinter;
import core.Memory;
import core.RegisterFile;
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
 *   - L1D data cache via shared CacheHierarchy (L2=null, PIPT)
 *   - Simple latency accumulation (no pipeline stages)
 *
 * For each instruction, the simulator charges:
 *   - L/S: translation latency + cache access latency
 *   - ADD: configured latency (default 1 cycle)
 *   - MUL: configured latency (default 3 cycles)
 *
 * Reuses core.RegisterFile and cache.CacheHierarchy instead of
 * duplicating their logic — keeping the codebase modular.
 */
public class TraceSimulator {

    private final Config cfg;
    private final Stats stats;
    private final VirtualMemoryUnit vmu;

    // Reuses the shared CacheHierarchy (with L2=null for trace mode)
    private final CacheHierarchy cache;
    private final Memory physicalMemory;

    // Reuses the shared RegisterFile from core package
    private final RegisterFile registers;

    public TraceSimulator(Config cfg) {
        this.cfg = cfg;
        this.stats = new Stats();
        this.vmu = new VirtualMemoryUnit(cfg);

        // Set up physical memory sized to physical_size_bytes
        this.physicalMemory = new Memory(cfg.getPhysicalSizeBytes());

        // Reuse core.RegisterFile instead of a raw int[]
        this.registers = new RegisterFile();

        // Reuse CacheHierarchy with L1I=null, L2=null (trace mode: L1D only → memory)
        this.cache = new CacheHierarchy(
                null,               // no L1I in trace mode
                cfg.getL1D(),       // L1D config
                null,               // no L2 in trace mode
                cfg.getMainMemoryLatency(),
                physicalMemory);
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

        // Collect cache stats via the shared Stats.collectCacheStats()
        stats.collectCacheStats(cache);

        // Collect VM stats
        stats.tlbHits = vmu.getTlbHits();
        stats.tlbMisses = vmu.getTlbMisses();
        stats.pageWalks = vmu.getPageWalks();
        stats.pageFaults = vmu.getPageFaults();
        stats.pageEvictions = vmu.getPageEvictions();
        stats.dirtyEvictions = vmu.getDirtyEvictions();
        stats.totalTranslationPenaltyCycles = vmu.getTotalTranslationPenalty();
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
        AccessResult ar = cache.readData(physAddr);
        cycles += ar.latencyCycles;
        if (instr.rd != 0) {
            registers.write(instr.rd, ar.data);
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
        int storeValue = registers.read(instr.rs1);
        AccessResult ar = cache.writeData(physAddr, storeValue);
        cycles += ar.latencyCycles;

        stats.cycles += cycles;
        stats.stalls += (cycles - 1);
    }

    /**
     * ADD: rd = rs1 + rs2, costs configured latency (default 1 cycle).
     */
    private void executeAdd(TraceInstruction instr) {
        int result = registers.read(instr.rs1) + registers.read(instr.rs2);
        if (instr.rd != 0) {
            registers.write(instr.rd, result);
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
        int result = registers.read(instr.rs1) * registers.read(instr.rs2);
        if (instr.rd != 0) {
            registers.write(instr.rd, result);
        }
        int latency = cfg.getLatency(common.Opcode.MUL);
        stats.cycles += latency;
        if (latency > 1) {
            stats.stalls += (latency - 1);
        }
    }

    /**
     * Print all simulation statistics via the shared StatsPrinter.
     */
    public void printStats(PrintStream out) {
        StatsPrinter.printTraceStats(out, stats);
    }

    public Stats getStats() {
        return stats;
    }
}
