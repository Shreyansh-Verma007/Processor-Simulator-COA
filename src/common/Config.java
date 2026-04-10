package common;

import cache.CacheConfig;
import cache.CacheConfig.ReplacementPolicy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Configures instruction latencies, forwarding, and cache parameters.
public class Config {
    private Map<Opcode, Integer> latencies;
    private boolean forwardingEnabled;

    // ── Default cache parameters (single source of truth) ────────────────
    private static final int DEF_L1I_SIZE = 1024;
    private static final int DEF_L1I_BLOCK = 64;
    private static final int DEF_L1I_ASSOC = 2;
    private static final int DEF_L1I_LATENCY = 5;

    private static final int DEF_L1D_SIZE = 1024;
    private static final int DEF_L1D_BLOCK = 64;
    private static final int DEF_L1D_ASSOC = 2;
    private static final int DEF_L1D_LATENCY = 5;

    private static final int DEF_L2_SIZE = 8192;
    private static final int DEF_L2_BLOCK = 64;
    private static final int DEF_L2_ASSOC = 4;
    private static final int DEF_L2_LATENCY = 50;

    private static final int DEF_MEMORY_LATENCY = 200;
    private static final ReplacementPolicy DEF_POLICY = ReplacementPolicy.LRU;
    private static final boolean DEF_FORWARDING = true;

    // Cache configuration (null = no cache, direct memory access)
    private CacheConfig l1i;
    private CacheConfig l1d;
    private CacheConfig l2;
    private int mainMemoryLatency;

    public Config() {
        this.latencies = new HashMap<>();
        this.forwardingEnabled = DEF_FORWARDING;

        // R-Type
        latencies.put(Opcode.ADD, 1);
        latencies.put(Opcode.SUB, 1);
        latencies.put(Opcode.MUL, 3);
        latencies.put(Opcode.DIV, 4);
        latencies.put(Opcode.SLL, 1);
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
        this.l1i = new CacheConfig(DEF_L1I_SIZE, DEF_L1I_BLOCK, DEF_L1I_ASSOC, DEF_L1I_LATENCY, DEF_POLICY);
        this.l1d = new CacheConfig(DEF_L1D_SIZE, DEF_L1D_BLOCK, DEF_L1D_ASSOC, DEF_L1D_LATENCY, DEF_POLICY);
        this.l2 = new CacheConfig(DEF_L2_SIZE, DEF_L2_BLOCK, DEF_L2_ASSOC, DEF_L2_LATENCY, DEF_POLICY);
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
        return l1i != null && l1d != null && l2 != null;
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

    /**
     * Load cache configuration from a key=value text file.
     * Keys: L1I_SIZE, L1I_BLOCK_SIZE, L1I_ASSOCIATIVITY, L1I_LATENCY,
     * L1D_SIZE, L1D_BLOCK_SIZE, L1D_ASSOCIATIVITY, L1D_LATENCY,
     * L2_SIZE, L2_BLOCK_SIZE, L2_ASSOCIATIVITY, L2_LATENCY,
     * MEMORY_LATENCY, REPLACEMENT_POLICY (LRU or FIFO).
     */
    public void loadCacheConfig(String path) throws IOException {
        Map<String, String> props = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                int eq = line.indexOf('=');
                if (eq < 0)
                    continue;
                props.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        }

        ReplacementPolicy policy = DEF_POLICY;
        String policyStr = props.getOrDefault("REPLACEMENT_POLICY", DEF_POLICY.name()).toUpperCase();
        if (policyStr.equals("FIFO"))
            policy = ReplacementPolicy.FIFO;

        l1i = new CacheConfig(
                Integer.parseInt(props.getOrDefault("L1I_SIZE", String.valueOf(DEF_L1I_SIZE))),
                Integer.parseInt(props.getOrDefault("L1I_BLOCK_SIZE", String.valueOf(DEF_L1I_BLOCK))),
                Integer.parseInt(props.getOrDefault("L1I_ASSOCIATIVITY", String.valueOf(DEF_L1I_ASSOC))),
                Integer.parseInt(props.getOrDefault("L1I_LATENCY", String.valueOf(DEF_L1I_LATENCY))),
                policy);

        l1d = new CacheConfig(
                Integer.parseInt(props.getOrDefault("L1D_SIZE", String.valueOf(DEF_L1D_SIZE))),
                Integer.parseInt(props.getOrDefault("L1D_BLOCK_SIZE", String.valueOf(DEF_L1D_BLOCK))),
                Integer.parseInt(props.getOrDefault("L1D_ASSOCIATIVITY", String.valueOf(DEF_L1D_ASSOC))),
                Integer.parseInt(props.getOrDefault("L1D_LATENCY", String.valueOf(DEF_L1D_LATENCY))),
                policy);

        l2 = new CacheConfig(
                Integer.parseInt(props.getOrDefault("L2_SIZE", String.valueOf(DEF_L2_SIZE))),
                Integer.parseInt(props.getOrDefault("L2_BLOCK_SIZE", String.valueOf(DEF_L2_BLOCK))),
                Integer.parseInt(props.getOrDefault("L2_ASSOCIATIVITY", String.valueOf(DEF_L2_ASSOC))),
                Integer.parseInt(props.getOrDefault("L2_LATENCY", String.valueOf(DEF_L2_LATENCY))),
                policy);

        mainMemoryLatency = Integer.parseInt(props.getOrDefault("MEMORY_LATENCY", String.valueOf(DEF_MEMORY_LATENCY)));

        String fwdStr = props.getOrDefault("FORWARDING_ENABLED", String.valueOf(DEF_FORWARDING)).trim().toLowerCase();
        forwardingEnabled = fwdStr.equals("true") || fwdStr.equals("1") || fwdStr.equals("yes");
    }
}
