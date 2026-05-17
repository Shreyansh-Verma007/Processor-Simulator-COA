package trace;

import common.Config;
import core.Memory;
import core.Stats;
import vm.TranslationResult;
import vm.VirtualMemoryUnit;
import cache.CacheHierarchy;

import pipeline_registers.ID_EX;
import pipeline_registers.EX_MEM;
import pipeline_registers.IF_ID;
import pipeline_registers.MEM_WB;
import hazard.HazardUnit;

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

    // Pipeline registers and Hazard Unit for hazard stall detection
    private final HazardUnit hazardUnit = new HazardUnit();
    private ID_EX idEx = new ID_EX();
    private EX_MEM exMem = new EX_MEM();
    private MEM_WB memWb = new MEM_WB();
    
    private int pc = 0x400000; // artificial PC for trace simulation

    public TraceSimulator(Config cfg) {
        this.cfg = cfg;
        this.stats = new Stats();

        // Set up physical memory first (VMU needs it for swap)
        this.physicalMemory = new Memory(cfg.getPhysicalSizeBytes());
        this.vmu = new VirtualMemoryUnit(cfg, physicalMemory);

        // Set up cache hierarchy
        if (cfg.hasCacheConfig()) {
            this.cache = new CacheHierarchy(
                    cfg.getL1I(), cfg.getL1D(), cfg.getL2(),
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

    private common.Instruction createMockInstruction(TraceInstruction t) {
        switch (t.type) {
            case LOAD:
                // rd is written. rs1 is not available, so use 0.
                return common.Instruction.iType(common.Opcode.LW, t.rd, 0, 0);
            case STORE:
                // rs1 is the value being stored.
                return common.Instruction.sType(common.Opcode.SW, 0, t.rs1, 0);
            case ADD:
                return common.Instruction.rType(common.Opcode.ADD, t.rd, t.rs1, t.rs2);
            case MUL:
                return common.Instruction.rType(common.Opcode.MUL, t.rd, t.rs1, t.rs2);
            default:
                return common.Instruction.uType(common.Opcode.HALT);
        }
    }

    private void executeInstruction(TraceInstruction instr) {
        // --- HAZARD DETECTION ---
        common.Instruction mockInstr = createMockInstruction(instr);
        IF_ID ifId = new IF_ID();
        ifId.isNop = false;
        ifId.instruction = mockInstr;

        while (hazardUnit.needsStall(idEx, ifId, exMem, memWb, cfg)) {
            stats.cycles++;
            stats.stalls++;
            
            boolean isMultiCycleStall = (idEx.latencyCyclesLeft > 0);
            if (isMultiCycleStall) {
                idEx.latencyCyclesLeft--;
                
                // Advance MEM to WB
                MEM_WB nextMemWb = new MEM_WB();
                if (!exMem.isNop) {
                    nextMemWb.isNop = false;
                    nextMemWb.opcode = exMem.opcode;
                    nextMemWb.rd = exMem.rd;
                }
                memWb = nextMemWb;
                
                exMem = new EX_MEM(); // bubble in MEM
            } else {
                // advance EX/MEM to MEM/WB
                MEM_WB nextMemWb = new MEM_WB();
                if (!exMem.isNop) {
                    nextMemWb.isNop = false;
                    nextMemWb.opcode = exMem.opcode;
                    nextMemWb.rd = exMem.rd;
                }
                memWb = nextMemWb;
                
                // advance ID/EX to EX/MEM
                EX_MEM nextExMem = new EX_MEM();
                if (!idEx.isNop) {
                    nextExMem.isNop = false;
                    nextExMem.opcode = idEx.opcode;
                    nextExMem.rd = idEx.rd;
                }
                exMem = nextExMem;
                idEx = new ID_EX(); // bubble in EX
            }
        }

        // Advance pipeline: ID -> EX, EX -> MEM, MEM -> WB
        MEM_WB nextMemWb = new MEM_WB();
        if (!exMem.isNop) {
            nextMemWb.isNop = false;
            nextMemWb.opcode = exMem.opcode;
            nextMemWb.rd = exMem.rd;
        }
        memWb = nextMemWb;
        
        EX_MEM nextExMem = new EX_MEM();
        if (!idEx.isNop) {
            nextExMem.isNop = false;
            nextExMem.opcode = idEx.opcode;
            nextExMem.rd = idEx.rd;
        }
        exMem = nextExMem;
        
        idEx = new ID_EX();
        idEx.isNop = false;
        idEx.opcode = mockInstr.opcode();
        idEx.rd = mockInstr.rd();
        idEx.rs1 = mockInstr.rs1();
        idEx.rs2 = mockInstr.rs2();
        int baseLatency = cfg.getLatency(idEx.opcode);
        idEx.latencyCyclesLeft = (baseLatency > 1) ? baseLatency - 1 : 0;
        // --- END HAZARD DETECTION ---



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
            case BRANCH:
            case JUMP:
                // Branches/Jumps execute in EX. For trace simulation, they cause a control hazard flush penalty.
                // Assuming a 2-cycle penalty for taken branches or mispredictions.
                stats.cycles += 2;
                stats.stalls += 2;
                stats.branchFlushes++;
                // Clear instructions behind the branch (simulating a flush)
                idEx = new ID_EX(); // bubble in EX
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
     * ADD: rd = rs1 + rs2, base execution cycle.
     */
    private void executeAdd(TraceInstruction instr) {
        int result = registers[instr.rs1] + registers[instr.rs2];
        if (instr.rd != 0) {
            registers[instr.rd] = result;
        }
        // Base cycle added; structural stalls handled by HazardUnit
        stats.cycles += 1;
    }

    /**
     * MUL: rd = rs1 * rs2, base execution cycle.
     */
    private void executeMul(TraceInstruction instr) {
        int result = registers[instr.rs1] * registers[instr.rs2];
        if (instr.rd != 0) {
            registers[instr.rd] = result;
        }
        // Base cycle added; structural stalls handled by HazardUnit
        stats.cycles += 1;
    }


    public Stats getStats() {
        return stats;
    }
}
