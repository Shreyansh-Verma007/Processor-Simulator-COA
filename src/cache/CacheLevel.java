package cache;

/**
 * A single level of set-associative cache.
 *
 * Supports LRU and FIFO replacement policies, write-back with dirty bits,
 * and tracks hit/miss statistics.
 */
public class CacheLevel {

    private final CacheConfig config;
    private final CacheLine[][] sets; // [set][way]
    private final int numSets;
    private final int blockSizeWords;

    // Stats
    private int hits = 0;
    private int misses = 0;

    // Global clock for LRU/FIFO ordering
    private long clock = 0;

    public CacheLevel(CacheConfig config) {
        this.config = config;
        this.numSets = config.getNumSets();
        this.blockSizeWords = config.blockSize / 4;
        this.sets = new CacheLine[numSets][config.associativity];

        for (int s = 0; s < numSets; s++) {
            for (int w = 0; w < config.associativity; w++) {
                sets[s][w] = new CacheLine(blockSizeWords);
            }
        }
    }

    // ── Address decomposition ────────────────────────────────────────────

    private int getBlockOffset(int address) {
        return Math.floorMod(address / 4, blockSizeWords);
    }

    private int getSetIndex(int address) {
        return Math.floorMod(Integer.divideUnsigned(address, config.blockSize), numSets);
    }

    private int getTag(int address) {
        return Integer.divideUnsigned(address, config.blockSize * numSets);
    }

    // ── Lookup ───────────────────────────────────────────────────────────

    /**
     * Look up an address in this cache level.
     * Returns the CacheLine on hit (and updates LRU timestamp), or null on miss.
     */
    public CacheLine lookup(int address) {
        return findLine(address, true);
    }

    /**
     * Read a single word from this cache level.
     * Returns Integer value on hit, null on miss.
     */
    public Integer readWord(int address) {
        CacheLine line = lookup(address);
        if (line == null)
            return null;
        return line.data[getBlockOffset(address)];
    }

    // ── Internal methods (no stats) — for block fills and write-backs ───

    CacheLine lookupNoStats(int address) {
        return findLine(address, false);
    }

    private CacheLine findLine(int address, boolean updateStats) {
        int set = getSetIndex(address);
        int tag = getTag(address);

        for (int w = 0; w < config.associativity; w++) {
            CacheLine line = sets[set][w];
            if (line.valid && line.tag == tag) {
                if (updateStats)
                    hits++;
                line.lastUsed = clock++;
                return line;
            }
        }
        if (updateStats)
            misses++;
        return null;
    }

    /** Read a word without counting stats. */
    public Integer readWordNoStats(int address) {
        CacheLine line = lookupNoStats(address);
        if (line == null)
            return null;
        return line.data[getBlockOffset(address)];
    }

    /** Write a word without counting stats (write-back). */
    public boolean writeWordNoStats(int address, int value) {
        CacheLine line = lookupNoStats(address);
        if (line == null)
            return false;
        line.data[getBlockOffset(address)] = value;
        line.dirty = true;
        return true;
    }

    // ── Insertion / Eviction ─────────────────────────────────────────────

    /**
     * Insert a block into this cache level. If the set is full, evict a line
     * based on the configured replacement policy.
     *
     * @param address   the byte address that triggered the miss
     * @param blockData the full block of data to insert (word array)
     * @return the evicted CacheLine if it was dirty (needs write-back), else null
     */
    public EvictionResult insert(int address, int[] blockData) {
        int set = getSetIndex(address);
        int tag = getTag(address);

        // Find an invalid (empty) slot first
        for (int w = 0; w < config.associativity; w++) {
            CacheLine line = sets[set][w];
            if (!line.valid) {
                fillLine(line, tag, blockData);
                return null; // no eviction
            }
        }

        // Set is full — pick a victim
        int victimWay = selectVictim(set);
        CacheLine victim = sets[set][victimWay];

        EvictionResult result = null;
        if (victim.dirty) {
            // Need to write back the evicted block
            int evictedAddress = victim.tag * (config.blockSize * numSets)
                    + set * config.blockSize;
            result = new EvictionResult(evictedAddress, victim.data.clone());
        }

        fillLine(victim, tag, blockData);
        return result;
    }

    private void fillLine(CacheLine line, int tag, int[] blockData) {
        line.valid = true;
        line.dirty = false;
        line.tag = tag;
        System.arraycopy(blockData, 0, line.data, 0, blockSizeWords);
        line.lastUsed = clock;
        line.insertOrder = clock;
        clock++;
    }

    private int selectVictim(int set) {
        int victim = 0;
        if (config.policy == CacheConfig.ReplacementPolicy.LRU) {
            long oldest = Long.MAX_VALUE;
            for (int w = 0; w < config.associativity; w++) {
                if (sets[set][w].lastUsed < oldest) {
                    oldest = sets[set][w].lastUsed;
                    victim = w;
                }
            }
        } else { // FIFO
            long earliest = Long.MAX_VALUE;
            for (int w = 0; w < config.associativity; w++) {
                if (sets[set][w].insertOrder < earliest) {
                    earliest = sets[set][w].insertOrder;
                    victim = w;
                }
            }
        }
        return victim;
    }

    // ── Eviction result ──────────────────────────────────────────────────

    public static class EvictionResult {
        public final int address;
        public final int[] data;

        public EvictionResult(int address, int[] data) {
            this.address = address;
            this.data = data;
        }
    }

    // ── Stats ────────────────────────────────────────────────────────────

    public int getHits() {
        return hits;
    }

    public int getMisses() {
        return misses;
    }

    public CacheConfig getConfig() {
        return config;
    }

    public int getBlockSizeWords() {
        return blockSizeWords;
    }
}
