package cache;

import core.Memory;

/**
 * Two-level cache hierarchy:
 * L1I (instruction) ──→ L2 (unified) ──→ Main Memory
 * L1D (data) ──→ L2 (unified) ──→ Main Memory
 *
 * On a miss at any level, the block is fetched from the next level,
 * and the total access latency is the sum of all levels traversed.
 * Uses write-back: dirty evictions propagate down the hierarchy.
 */
public class CacheHierarchy {

    private final CacheLevel l1i;
    private final CacheLevel l1d;
    private final CacheLevel l2;
    private final Memory memory;
    private final int memoryLatency;

    public CacheHierarchy(CacheConfig l1iCfg, CacheConfig l1dCfg,
            CacheConfig l2Cfg, int memoryLatency, Memory memory) {
        this.l1i = new CacheLevel("L1I", l1iCfg);
        this.l1d = new CacheLevel("L1D", l1dCfg);
        this.l2 = new CacheLevel("L2", l2Cfg);
        this.memory = memory;
        this.memoryLatency = memoryLatency;
    }

    // ── Instruction fetch (IF stage) ─────────────────────────────────────

    /**
     * Fetch a single instruction word through L1I → L2 → Memory.
     */
    public AccessResult fetchInstruction(int address) {
        return readThrough(l1i, address);
    }

    // ── Data access (MEM stage) ──────────────────────────────────────────

    /**
     * Read a word of data through L1D → L2 → Memory.
     */
    public AccessResult readData(int address) {
        return readThrough(l1d, address);
    }

    /**
     * Read a byte of data through L1D → L2 → Memory.
     */
    public AccessResult readDataByte(int address) {
        AccessResult wordResult = readThrough(l1d, (address / 4) * 4);
        int word = wordResult.data;
        int bytePos = (address % 4) * 8;
        int byteVal = (word >> bytePos) & 0xFF;
        return new AccessResult(byteVal, wordResult.latencyCycles);
    }

    /**
     * Write a word of data through L1D → L2 → Memory (write-allocate).
     */
    public AccessResult writeData(int address, int value) {
        int latency = 0;

        // Try L1D hit
        if (l1d.writeWord(address, value)) {
            return new AccessResult(0, l1d.getConfig().latency);
        }

        // L1D miss — need to load the block first (write-allocate)
        latency += l1d.getConfig().latency;
        fetchBlockToL1(l1d, address);
        latency += getL2MemLatency(address);

        // Now write into L1D
        l1d.writeWord(address, value);
        return new AccessResult(0, latency);
    }

    /**
     * Write a byte of data through L1D → L2 → Memory (write-allocate).
     */
    public AccessResult writeDataByte(int address, int value) {
        int wordAddress = (address / 4) * 4;
        int latency = 0;

        // Ensure the block is in L1D
        Integer existing = l1d.readWord(wordAddress);
        if (existing == null) {
            latency += l1d.getConfig().latency;
            fetchBlockToL1(l1d, wordAddress);
            latency += getL2MemLatency(wordAddress);
        } else {
            latency += l1d.getConfig().latency;
        }

        // Read the current word, modify the byte, write back
        int word = l1d.readWord(wordAddress);
        int bytePos = (address % 4) * 8;
        word = (word & ~(0xFF << bytePos)) | ((value & 0xFF) << bytePos);
        l1d.writeWord(wordAddress, word);

        return new AccessResult(0, latency);
    }

    // ── Internal: read-through logic ─────────────────────────────────────

    private AccessResult readThrough(CacheLevel l1, int address) {
        int latency = l1.getConfig().latency;

        // L1 hit?
        Integer val = l1.readWord(address);
        if (val != null) {
            return new AccessResult(val, latency);
        }

        // L1 miss — try L2
        latency += getL2MemLatency(address);
        fetchBlockToL1(l1, address);

        val = l1.readWord(address);
        return new AccessResult(val != null ? val : 0, latency);
    }

    /**
     * Fetch a block from L2 (or memory) and install it into the given L1 cache.
     * Returns the block data.
     */
    private int[] fetchBlockToL1(CacheLevel l1, int address) {
        int blockSizeWords = l1.getBlockSizeWords();
        int blockStart = (address / l1.getConfig().blockSize) * l1.getConfig().blockSize;
        int[] block = new int[blockSizeWords];

        // Try L2
        Integer firstWord = l2.readWord(blockStart);
        if (firstWord != null) {
            // L2 hit — read the full block from L2
            for (int i = 0; i < blockSizeWords; i++) {
                Integer w = l2.readWord(blockStart + i * 4);
                block[i] = (w != null) ? w : 0;
            }
        } else {
            // L2 miss — fetch from main memory
            for (int i = 0; i < blockSizeWords; i++) {
                block[i] = memory.readWord(blockStart + i * 4);
            }
            // Install into L2
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

        // Install into L1
        CacheLevel.EvictionResult l1Evict = l1.insert(address, block);
        if (l1Evict != null) {
            // Write dirty L1 eviction into L2
            writeBackToL2(l1Evict);
        }

        return block;
    }

    /**
     * Returns the additional latency for an L2 lookup (hit or miss to memory).
     */
    private int getL2MemLatency(int address) {
        // Check if block is in L2
        int l2BlockStart = (address / l2.getConfig().blockSize) * l2.getConfig().blockSize;
        Integer probe = l2.readWord(l2BlockStart);
        // Undo the stat change from probing (we just want the latency estimate)
        // Actually the real lookup happens in fetchBlockToL1, so we just return
        // the worst/best case latency based on L2 state.
        // Simpler approach: always charge L2 latency + memory latency on L1 miss.
        // The L2 lookup stats are tracked inside fetchBlockToL1.
        return l2.getConfig().latency + (probe != null ? 0 : memoryLatency);
    }

    private void writeBackToL2(CacheLevel.EvictionResult eviction) {
        // Try to write into L2; if miss, install the block
        int addr = eviction.address;
        if (!l2.writeWord(addr, eviction.data[0])) {
            // L2 miss — install the evicted block into L2
            int l2BlockWords = l2.getBlockSizeWords();
            int l2BlockStart = (addr / l2.getConfig().blockSize) * l2.getConfig().blockSize;

            // Build a full L2 block from memory, then overlay the dirty data
            int[] l2Block = new int[l2BlockWords];
            for (int i = 0; i < l2BlockWords; i++) {
                l2Block[i] = memory.readWord(l2BlockStart + i * 4);
            }
            // Overlay dirty words from L1
            int l1Offset = (addr - l2BlockStart) / 4;
            for (int i = 0; i < eviction.data.length && (l1Offset + i) < l2BlockWords; i++) {
                l2Block[l1Offset + i] = eviction.data[i];
            }
            CacheLevel.EvictionResult l2Evict = l2.insert(addr, l2Block);
            if (l2Evict != null) {
                writeBackToMemory(l2Evict);
            }
            l2.writeWord(addr, eviction.data[0]);
        }
        // Write remaining words of the dirty L1 block into L2
        for (int i = 1; i < eviction.data.length; i++) {
            l2.writeWord(addr + i * 4, eviction.data[i]);
        }
    }

    private void writeBackToMemory(CacheLevel.EvictionResult eviction) {
        for (int i = 0; i < eviction.data.length; i++) {
            memory.writeWord(eviction.address + i * 4, eviction.data[i]);
        }
    }

    // ── Stats accessors ──────────────────────────────────────────────────

    public CacheLevel getL1I() {
        return l1i;
    }

    public CacheLevel getL1D() {
        return l1d;
    }

    public CacheLevel getL2() {
        return l2;
    }
}
