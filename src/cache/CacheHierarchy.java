package cache;

import core.Memory;

/**
 * Multi-level cache hierarchy with optional L2:
 *   L1I (instruction) ──→ [L2 (unified)] ──→ Main Memory
 *   L1D (data)        ──→ [L2 (unified)] ──→ Main Memory
 *
 * When L2 is null (e.g., trace replay mode), L1 misses go directly
 * to main memory — eliminating the need for a separate trace cache.
 *
 * On a miss at any level, the block is fetched from the next level,
 * and the total access latency is the sum of all levels traversed.
 * Uses write-back: dirty evictions propagate down the hierarchy.
 *
 * Stats policy: each pipeline request counts as exactly ONE L1 access
 * (hit or miss). If L1 misses, exactly ONE L2 access is counted.
 * Internal block fills and write-backs use no-stats methods.
 */
public class CacheHierarchy {

    private final CacheLevel l1i;   // may be null in trace mode
    private final CacheLevel l1d;
    private final CacheLevel l2;    // null = no L2, direct to memory
    private final Memory memory;
    private final int memoryLatency;

    public CacheHierarchy(CacheConfig l1iCfg, CacheConfig l1dCfg,
            CacheConfig l2Cfg, int memoryLatency, Memory memory) {
        this.l1i = (l1iCfg != null) ? new CacheLevel(l1iCfg) : null;
        this.l1d = (l1dCfg != null) ? new CacheLevel(l1dCfg) : null;
        this.l2 = (l2Cfg != null) ? new CacheLevel(l2Cfg) : null;
        this.memory = memory;
        this.memoryLatency = memoryLatency;
    }

    // ── Instruction fetch (IF stage) ─────────────────────────────────────

    /** Fetch a single instruction word through L1I → [L2] → Memory. */
    public AccessResult fetchInstruction(int address) {
        if (l1i == null) {
            // No instruction cache — read directly from memory
            return new AccessResult(memory.readWord(address), 1);
        }
        return readThrough(l1i, address);
    }

    // ── Data access (MEM stage) ──────────────────────────────────────────

    /** Read a word of data through L1D → [L2] → Memory. */
    public AccessResult readData(int address) {
        if (l1d == null) {
            return new AccessResult(memory.readWord(address), 1);
        }
        return readThrough(l1d, address);
    }

    /** Read a byte of data through L1D → [L2] → Memory. */
    public AccessResult readDataByte(int address) {
        AccessResult wordResult = readData((address / 4) * 4);
        int word = wordResult.data;
        int bytePos = (address % 4) * 8;
        int byteVal = (word >> bytePos) & 0xFF;
        return new AccessResult(byteVal, wordResult.latencyCycles);
    }

    /** Write a word of data through L1D → [L2] → Memory (write-allocate). */
    public AccessResult writeData(int address, int value) {
        if (l1d == null) {
            memory.writeWord(address, value);
            return new AccessResult(0, 1);
        }

        // Single L1D lookup for stats
        CacheLine line = l1d.lookup(address);

        if (line != null) {
            // L1D hit — write directly
            int offset = getBlockOffset(l1d, address);
            line.data[offset] = value;
            line.dirty = true;
            return new AccessResult(0, l1d.getConfig().latency);
        }

        // L1D miss — fetch block (write-allocate), then write
        int latency = l1d.getConfig().latency;
        FetchResult fetch = fetchBlockToL1(l1d, address);
        latency += fetch.latency;

        // Write into the now-loaded L1D block (no stats)
        l1d.writeWordNoStats(address, value);
        return new AccessResult(0, latency);
    }

    /** Write a byte of data through L1D → [L2] → Memory (write-allocate). */
    public AccessResult writeDataByte(int address, int value) {
        if (l1d == null) {
            // Read-modify-write directly to memory
            int wordAddr = (address / 4) * 4;
            int word = memory.readWord(wordAddr);
            int bytePos = (address % 4) * 8;
            word = (word & ~(0xFF << bytePos)) | ((value & 0xFF) << bytePos);
            memory.writeWord(wordAddr, word);
            return new AccessResult(0, 1);
        }

        int wordAddress = (address / 4) * 4;

        // Single L1D lookup for stats
        CacheLine line = l1d.lookup(wordAddress);
        int latency;

        if (line == null) {
            // L1D miss — fetch block first
            latency = l1d.getConfig().latency;
            FetchResult fetch = fetchBlockToL1(l1d, wordAddress);
            latency += fetch.latency;
        } else {
            latency = l1d.getConfig().latency;
        }

        // Read-modify-write the byte (no stats — block is guaranteed present)
        Integer word = l1d.readWordNoStats(wordAddress);
        if (word == null)
            word = 0;
        int bytePos = (address % 4) * 8;
        word = (word & ~(0xFF << bytePos)) | ((value & 0xFF) << bytePos);
        l1d.writeWordNoStats(wordAddress, word);

        return new AccessResult(0, latency);
    }

    // ── Internal: read-through logic ─────────────────────────────────────

    private AccessResult readThrough(CacheLevel l1, int address) {
        int latency = l1.getConfig().latency;

        // Single L1 lookup — this is the ONLY stat-counted L1 access
        Integer val = l1.readWord(address);
        if (val != null) {
            return new AccessResult(val, latency);
        }

        // L1 miss — fetch block from L2/memory
        FetchResult fetch = fetchBlockToL1(l1, address);
        latency += fetch.latency;

        // Extract word from the fetched block (no extra L1 lookup)
        int offset = getBlockOffset(l1, address);
        int data = fetch.blockData[offset];
        return new AccessResult(data, latency);
    }

    // ── Block fetch: [L2] → Memory → install into L1 ────────────────────

    /**
     * Fetch a block from L2 (or memory) and install it into the given L1 cache.
     * If L2 is present, counts exactly ONE L2 access for stats.
     * If L2 is null, goes directly to memory.
     */
    private FetchResult fetchBlockToL1(CacheLevel l1, int address) {
        int blockSizeWords = l1.getBlockSizeWords();
        int blockStart = (address / l1.getConfig().blockSize) * l1.getConfig().blockSize;
        int[] block = new int[blockSizeWords];
        int latency = 0;

        if (l2 != null) {
            // L2 is present — try L2 first
            Integer l2Probe = l2.readWord(blockStart);

            if (l2Probe != null) {
                // L2 hit — read the full block using no-stats methods
                latency = l2.getConfig().latency;
                block[0] = l2Probe;
                for (int i = 1; i < blockSizeWords; i++) {
                    Integer w = l2.readWordNoStats(blockStart + i * 4);
                    block[i] = (w != null) ? w : 0;
                }
            } else {
                // L2 miss — fetch from main memory
                latency = l2.getConfig().latency + memoryLatency;
                for (int i = 0; i < blockSizeWords; i++) {
                    block[i] = memory.readWord(blockStart + i * 4);
                }
                // Install into L2 — build L2-sized block (may differ from L1 block size)
                int l2BlockWords = l2.getBlockSizeWords();
                int l2BlockStart = (address / l2.getConfig().blockSize) * l2.getConfig().blockSize;
                int[] l2Block = new int[l2BlockWords];
                for (int i = 0; i < l2BlockWords; i++) {
                    l2Block[i] = memory.readWord(l2BlockStart + i * 4);
                }
                CacheLevel.EvictionResult l2Evict = l2.insert(address, l2Block);
                if (l2Evict != null) {
                    writeBackToMemory(l2Evict);
                }
            }
        } else {
            // No L2 — fetch directly from main memory
            latency = memoryLatency;
            for (int i = 0; i < blockSizeWords; i++) {
                block[i] = memory.readWord(blockStart + i * 4);
            }
        }

        // Install into L1
        CacheLevel.EvictionResult l1Evict = l1.insert(address, block);
        if (l1Evict != null) {
            if (l2 != null) {
                writeBackToL2(l1Evict);
            } else {
                writeBackToMemory(l1Evict);
            }
        }

        return new FetchResult(block, latency);
    }

    // ── Write-back helpers (no stats) ────────────────────────────────────

    private void writeBackToL2(CacheLevel.EvictionResult eviction) {
        int addr = eviction.address;

        // Try to write into existing L2 block (no stats)
        if (!l2.writeWordNoStats(addr, eviction.data[0])) {
            // L2 miss — install the evicted block into L2
            int[] l2Block = fetchMemoryBlock(l2, addr);
            int l2BlockStart = (addr / l2.getConfig().blockSize) * l2.getConfig().blockSize;
            int l2BlockWords = l2.getBlockSizeWords();
            // Overlay dirty words from L1
            int l1Offset = (addr - l2BlockStart) / 4;
            for (int i = 0; i < eviction.data.length && (l1Offset + i) < l2BlockWords; i++) {
                l2Block[l1Offset + i] = eviction.data[i];
            }
            CacheLevel.EvictionResult l2Evict = l2.insert(addr, l2Block);
            if (l2Evict != null) {
                writeBackToMemory(l2Evict);
            }
            l2.writeWordNoStats(addr, eviction.data[0]);
        }
        // Write remaining words of the dirty L1 block into L2 (no stats)
        for (int i = 1; i < eviction.data.length; i++) {
            l2.writeWordNoStats(addr + i * 4, eviction.data[i]);
        }
    }

    private void writeBackToMemory(CacheLevel.EvictionResult eviction) {
        for (int i = 0; i < eviction.data.length; i++) {
            memory.writeWord(eviction.address + i * 4, eviction.data[i]);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private int getBlockOffset(CacheLevel level, int address) {
        return Math.floorMod(address / 4, level.getBlockSizeWords());
    }

    private int[] fetchMemoryBlock(CacheLevel level, int address) {
        int blockWords = level.getBlockSizeWords();
        int blockStart = (address / level.getConfig().blockSize) * level.getConfig().blockSize;
        int[] block = new int[blockWords];
        for (int i = 0; i < blockWords; i++) {
            block[i] = memory.readWord(blockStart + i * 4);
        }
        return block;
    }

    /** Result of fetching a block from L2/memory. */
    private static class FetchResult {
        final int[] blockData;
        final int latency;

        FetchResult(int[] blockData, int latency) {
            this.blockData = blockData;
            this.latency = latency;
        }
    }

    // ── Stats accessors ──────────────────────────────────────────────────

    /** Returns L1I cache level, or null if not configured. */
    public CacheLevel getL1I() {
        return l1i;
    }

    /** Returns L1D cache level, or null if not configured. */
    public CacheLevel getL1D() {
        return l1d;
    }

    /** Returns L2 cache level, or null if not configured (e.g., trace mode). */
    public CacheLevel getL2() {
        return l2;
    }

    /**
     * Invalidate all L1D cache lines within a physical frame.
     * Must be called when a physical frame is evicted and reassigned
     * to a new virtual page (PIPT correctness).
     */
    public void invalidateFrame(int frameBaseAddr, int frameSizeBytes) {
        if (l1d != null) {
            l1d.invalidateFrameLines(frameBaseAddr, frameSizeBytes);
        }
    }
}
