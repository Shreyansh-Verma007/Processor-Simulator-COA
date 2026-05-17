package common;

import cache.CacheConfig;
import cache.CacheConfig.ReplacementPolicy;

import java.util.HashMap;
import java.util.Map;

// Configures instruction latencies, forwarding, cache parameters, and VM settings.
public class Config {
    private Map<Opcode, Integer> latencies;
    private boolean forwardingEnabled;

    // ── Default cache parameters (single source of truth) ────────────────
    // Updated realistic cache defaults (sizes in bytes)
    private static final int DEF_L1I_SIZE = 4096;   // 4 KB L1 instruction cache
    private static final int DEF_L1I_BLOCK = 64;    // 64 B block
    private static final int DEF_L1I_ASSOC = 1;     // Direct mapped
    private static final int DEF_L1I_LATENCY = 1;

    private static final int DEF_L1D_SIZE = 4096;   // 4 KB L1 data cache
    private static final int DEF_L1D_BLOCK = 64;
    private static final int DEF_L1D_ASSOC = 1;     // Direct mapped
    private static final int DEF_L1D_LATENCY = 1;

    private static final int DEF_L2_SIZE = 0;       // No L2 cache per spec
    private static final int DEF_L2_BLOCK = 64;
    private static final int DEF_L2_ASSOC = 1;
    private static final int DEF_L2_LATENCY = 10;

    private static final int DEF_MEMORY_LATENCY = 10;
    private static final ReplacementPolicy DEF_POLICY = ReplacementPolicy.LRU;
    private static final boolean DEF_FORWARDING = true;

    // Cache configuration (null = no cache, direct memory access)
    private CacheConfig l1i;
    private CacheConfig l1d;
    private CacheConfig l2;
    private int mainMemoryLatency;

    // ── Virtual Memory parameters ────────────────────────────────────────
    private long virtualSizeBytes = 4294967296L;   // 4 GB default
    private int physicalSizeBytes = 262144;     // 64 frames * 4KB = 256 KB
    private int pageSizeBytes = 4096;           // 4 KB default
    private int dtlbEntries = 16;
    private int tlbHitLatency = 1;
    private int pageWalkLatency = 10;
    private int pageFaultLatency = 50;
    private String vmReplacementPolicy = "lru";

    public Config() {
        this.latencies = new HashMap<>();
        this.forwardingEnabled = DEF_FORWARDING;

        // R-Type
        latencies.put(Opcode.ADD, 1);
        latencies.put(Opcode.SUB, 1);
        latencies.put(Opcode.MUL, 3);
        latencies.put(Opcode.DIV, 4);
        latencies.put(Opcode.SLL, 1)    ;
        latencies.put(Opcode.SRL, 1);
        latencies.put(Opcode.XOR, 1);
        latencies.put(Opcode.OR, 1);
        latencies.put(Opcode.AND, 1);

        // I-Type
        latencies.put(Opcode.ADDI, 1);
        latencies.put(Opcode.LW, 1);
        latencies.put(Opcode.LB, 1);
        latencies.put(Opcode.LI, 1);

        // S-Type
        latencies.put(Opcode.SW, 1);
        latencies.put(Opcode.SB, 1);

        // B-Type
        latencies.put(Opcode.BEQ, 1);
        latencies.put(Opcode.BNE, 1);
        latencies.put(Opcode.BLT, 1);
        latencies.put(Opcode.BGE, 1);

        // J-Type
        latencies.put(Opcode.JAL, 1);

        // U-Type
        latencies.put(Opcode.ECALL, 1);
        latencies.put(Opcode.HALT, 1);

        // Setup default cache hierarchy (uses the constants above)
        this.l1i = null; // L1I disabled per user request
        this.l1d = new CacheConfig(DEF_L1D_SIZE, DEF_L1D_BLOCK, DEF_L1D_ASSOC, DEF_L1D_LATENCY, DEF_POLICY);
        this.l2 = null; // No L2 cache per spec
        this.mainMemoryLatency = DEF_MEMORY_LATENCY;
    }

    // Returns how many cycles an instruction takes. Defaults to 1 if not found.
    public int getLatency(Opcode op) {
        Integer lat = latencies.get(op);
        return lat == null ? 1 : lat;
    }

    public boolean isForwardingEnabled() {
        return forwardingEnabled;
    }

    // ── Cache configuration ──────────────────────────────────────────────

    public boolean hasCacheConfig() {
        return l1i != null || l1d != null;
    }

    public CacheConfig getL1I() {
        return l1i;
    }

    public CacheConfig getL1D() {
        return l1d;
    }

    public CacheConfig getL2() {
        return l2;
    }

    public int getMainMemoryLatency() {
        return mainMemoryLatency;
    }

    // ── Virtual Memory accessors ─────────────────────────────────────────

    public long getVirtualSizeBytes() {
        return virtualSizeBytes;
    }

    public int getPhysicalSizeBytes() {
        return physicalSizeBytes;
    }

    public int getPageSizeBytes() {
        return pageSizeBytes;
    }

    public int getDtlbEntries() {
        return dtlbEntries;
    }

    public int getTlbHitLatency() {
        return tlbHitLatency;
    }

    public int getPageWalkLatency() {
        return pageWalkLatency;
    }

    public int getPageFaultLatency() {
        return pageFaultLatency;
    }

    public String getVmReplacementPolicy() {
        return vmReplacementPolicy;
    }


}
