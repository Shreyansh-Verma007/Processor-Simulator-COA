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

    // Cache configuration (null = no cache, direct memory access)
    private CacheConfig l1i;
    private CacheConfig l1d;
    private CacheConfig l2;
    private int mainMemoryLatency = 100;

    public Config() {
        this.latencies = new HashMap<>();
        this.forwardingEnabled = true;

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

        // Setup default cache hierarchy
        ReplacementPolicy policy = ReplacementPolicy.LRU;
        this.l1i = new CacheConfig(1024, 64, 2, 1, policy);
        this.l1d = new CacheConfig(1024, 64, 2, 1, policy);
        this.l2 = new CacheConfig(8192, 64, 4, 4, policy);
        this.mainMemoryLatency = 100;
    }

    // Returns how many cycles an instruction takes. Defaults to 1 if not found.
    public int getLatency(Opcode op) {
        Integer lat = latencies.get(op);
        return lat == null ? 1 : lat;
    }

    public boolean isForwardingEnabled() {
        return forwardingEnabled;
    }

    public void setForwardingEnabled(boolean enabled) {
        this.forwardingEnabled = enabled;
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
     * Programmatically enable cache components.
     */
    public void enableCache(CacheConfig l1i, CacheConfig l1d, CacheConfig l2, int mainMemoryLatency) {
        this.l1i = l1i;
        this.l1d = l1d;
        this.l2 = l2;
        this.mainMemoryLatency = mainMemoryLatency;
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

        ReplacementPolicy policy = ReplacementPolicy.LRU;
        String policyStr = props.getOrDefault("REPLACEMENT_POLICY", "LRU").toUpperCase();
        if (policyStr.equals("FIFO"))
            policy = ReplacementPolicy.FIFO;

        l1i = new CacheConfig(
                Integer.parseInt(props.getOrDefault("L1I_SIZE", "1024")),
                Integer.parseInt(props.getOrDefault("L1I_BLOCK_SIZE", "64")),
                Integer.parseInt(props.getOrDefault("L1I_ASSOCIATIVITY", "2")),
                Integer.parseInt(props.getOrDefault("L1I_LATENCY", "1")),
                policy);

        l1d = new CacheConfig(
                Integer.parseInt(props.getOrDefault("L1D_SIZE", "1024")),
                Integer.parseInt(props.getOrDefault("L1D_BLOCK_SIZE", "64")),
                Integer.parseInt(props.getOrDefault("L1D_ASSOCIATIVITY", "2")),
                Integer.parseInt(props.getOrDefault("L1D_LATENCY", "1")),
                policy);

        l2 = new CacheConfig(
                Integer.parseInt(props.getOrDefault("L2_SIZE", "8192")),
                Integer.parseInt(props.getOrDefault("L2_BLOCK_SIZE", "64")),
                Integer.parseInt(props.getOrDefault("L2_ASSOCIATIVITY", "4")),
                Integer.parseInt(props.getOrDefault("L2_LATENCY", "4")),
                policy);

        mainMemoryLatency = Integer.parseInt(props.getOrDefault("MEMORY_LATENCY", "100"));
    }
}
