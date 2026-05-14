package common;

import cache.CacheConfig;
import cache.CacheConfig.ReplacementPolicy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Configures instruction latencies, forwarding, cache parameters, and VM settings.
public class Config {
    private Map<Opcode, Integer> latencies;
    private boolean forwardingEnabled;

    // ── Default cache parameters (single source of truth) ────────────────
    // Updated realistic cache defaults (sizes in bytes)
    private static final int DEF_L1I_SIZE = 32768;  // 32 KB L1 instruction cache
    private static final int DEF_L1I_BLOCK = 64;    // 64 B block
    private static final int DEF_L1I_ASSOC = 4;    // 4‑way set associative
    private static final int DEF_L1I_LATENCY = 1;

    private static final int DEF_L1D_SIZE = 32768;  // 32 KB L1 data cache
    private static final int DEF_L1D_BLOCK = 64;
    private static final int DEF_L1D_ASSOC = 4;
    private static final int DEF_L1D_LATENCY = 1;

    private static final int DEF_L2_SIZE = 262144; // 256 KB L2 cache
    private static final int DEF_L2_BLOCK = 64;
    private static final int DEF_L2_ASSOC = 8;
    private static final int DEF_L2_LATENCY = 10;

    private static final int DEF_MEMORY_LATENCY = 50;
    private static final ReplacementPolicy DEF_POLICY = ReplacementPolicy.LRU;
    private static final boolean DEF_FORWARDING = true;

    // Cache configuration (null = no cache, direct memory access)
    private CacheConfig l1i;
    private CacheConfig l1d;
    private CacheConfig l2;
    private int mainMemoryLatency;

    // ── Virtual Memory parameters ────────────────────────────────────────
    private int virtualSizeBytes = 536870912;   // 512 MB default
    private int physicalSizeBytes = 262144;     // 256 KB default
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

    // ── Virtual Memory accessors ─────────────────────────────────────────

    public int getVirtualSizeBytes() {
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

    /**
     * Load configuration from a key=value text file.
     * Supports two formats:
     *   1. Flat key=value (backward compatible with Phase 2)
     *   2. INI-style with [section] headers (Phase 3)
     *
     * INI sections:
     *   [pipeline]  — forwarding_enabled
     *   [latencies] — ADD, MUL, etc.
     *   [memory]    — virtual_size_bytes, physical_size_bytes, page_size_bytes
     *   [vm]        — dtlb_entries, tlb_hit_latency, page_walk_latency,
     *                 page_fault_latency, replacement_policy
     *   [cache]     — L1I_*, L1D_*, L2_*, MEMORY_LATENCY, REPLACEMENT_POLICY
     */
    public void loadConfig(String path) throws IOException {
        // Parse file into section→{key→value} maps
        Map<String, Map<String, String>> sections = new HashMap<>();
        Map<String, String> currentSection = new HashMap<>();
        sections.put("", currentSection); // default (no section)
        boolean hasIniSections = false;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";"))
                    continue;

                // Check for section header [section_name]
                if (line.startsWith("[") && line.endsWith("]")) {
                    String sectionName = line.substring(1, line.length() - 1).trim().toLowerCase();
                    hasIniSections = true;
                    currentSection = sections.get(sectionName);
                    if (currentSection == null) {
                        currentSection = new HashMap<>();
                        sections.put(sectionName, currentSection);
                    }
                    continue;
                }

                int eq = line.indexOf('=');
                if (eq < 0)
                    continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                currentSection.put(key, value);
            }
        }

        if (hasIniSections) {
            loadIniConfig(sections);
        } else {
            // Backward-compatible flat format (Phase 2)
            loadFlatConfig(sections.get(""));
        }
    }

    /**
     * Load from INI-style sectioned config.
     */
    private void loadIniConfig(Map<String, Map<String, String>> sections) {
        // [pipeline]
        Map<String, String> pipeline = sections.getOrDefault("pipeline", new HashMap<>());
        String fwdStr = pipeline.getOrDefault("forwarding_enabled",
                String.valueOf(DEF_FORWARDING)).trim().toLowerCase();
        forwardingEnabled = fwdStr.equals("true") || fwdStr.equals("1") || fwdStr.equals("yes");

        // [latencies]
        Map<String, String> lats = sections.getOrDefault("latencies", new HashMap<>());
        for (Map.Entry<String, String> entry : lats.entrySet()) {
            try {
                Opcode op = Opcode.valueOf(entry.getKey().toUpperCase());
                latencies.put(op, Integer.parseInt(entry.getValue()));
            } catch (IllegalArgumentException e) {
                // Unknown opcode in config — skip
            }
        }

        // [memory]
        Map<String, String> mem = sections.getOrDefault("memory", new HashMap<>());
        if (mem.containsKey("virtual_size_bytes"))
            virtualSizeBytes = Integer.parseInt(mem.get("virtual_size_bytes"));
        if (mem.containsKey("physical_size_bytes"))
            physicalSizeBytes = Integer.parseInt(mem.get("physical_size_bytes"));
        if (mem.containsKey("page_size_bytes"))
            pageSizeBytes = Integer.parseInt(mem.get("page_size_bytes"));

        // [vm]
        Map<String, String> vm = sections.getOrDefault("vm", new HashMap<>());
        if (vm.containsKey("dtlb_entries"))
            dtlbEntries = Integer.parseInt(vm.get("dtlb_entries"));
        if (vm.containsKey("tlb_hit_latency"))
            tlbHitLatency = Integer.parseInt(vm.get("tlb_hit_latency"));
        if (vm.containsKey("page_walk_latency"))
            pageWalkLatency = Integer.parseInt(vm.get("page_walk_latency"));
        if (vm.containsKey("page_fault_latency"))
            pageFaultLatency = Integer.parseInt(vm.get("page_fault_latency"));
        if (vm.containsKey("replacement_policy"))
            vmReplacementPolicy = vm.get("replacement_policy").trim().toLowerCase();

        // [cache]
        Map<String, String> cacheSection = sections.getOrDefault("cache", new HashMap<>());
        if (!cacheSection.isEmpty()) {
            loadCacheFromMap(cacheSection);
        }
        // If no [cache] section, check default section for backward compat
        Map<String, String> defaultSection = sections.getOrDefault("", new HashMap<>());
        if (cacheSection.isEmpty() && !defaultSection.isEmpty()) {
            loadCacheFromMap(defaultSection);
        }
    }

    /**
     * Load from flat key=value format (Phase 2 backward compatibility).
     */
    private void loadFlatConfig(Map<String, String> props) {
        loadCacheFromMap(props);

        String fwdStr = props.getOrDefault("FORWARDING_ENABLED",
                String.valueOf(DEF_FORWARDING)).trim().toLowerCase();
        forwardingEnabled = fwdStr.equals("true") || fwdStr.equals("1") || fwdStr.equals("yes");
    }

    /**
     * Load cache configuration from a key-value map.
     */
    private void loadCacheFromMap(Map<String, String> props) {
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

        mainMemoryLatency = Integer.parseInt(
                props.getOrDefault("MEMORY_LATENCY", String.valueOf(DEF_MEMORY_LATENCY)));
    }
}
