package trace;

import cache.CacheConfig;
import cache.CacheLevel;
import core.Memory;

/**
 * Simple L1D data cache for trace replay mode.
 * Write-back, write-allocate. On miss, fetches the full block from
 * physical memory and inserts it; dirty evictions write back to memory.
 */
public class TraceDataCache {

    private final CacheLevel cache;
    private final int memoryLatency;
    private final Memory memory;
    private final int blockSizeWords;

    public TraceDataCache(CacheConfig config, int memoryLatency, Memory memory) {
        this.cache = new CacheLevel(config);
        this.memoryLatency = memoryLatency;
        this.memory = memory;
        this.blockSizeWords = config.blockSize / 4;
    }

    /** Result of a cache read or write. */
    public static class Result {
        public final int data;
        public final int latency;

        public Result(int data, int latency) {
            this.data = data;
            this.latency = latency;
        }
    }

    /** Read a word through the cache. */
    public Result read(int address) {
        Integer val = cache.readWord(address);
        if (val != null) {
            // Hit
            return new Result(val, cache.getConfig().latency);
        }
        // Miss — fetch block from memory, insert into cache
        int latency = memoryLatency;
        int[] block = fetchBlock(address);
        CacheLevel.EvictionResult evict = cache.insert(address, block);
        if (evict != null) {
            writeBackBlock(evict.address, evict.data);
        }
        // Re-read from cache (now present)
        Integer result = cache.readWordNoStats(address);
        return new Result(result != null ? result : 0, latency);
    }

    /** Write a word through the cache (write-allocate). */
    public Result write(int address, int value) {
        // Try to write in-place
        if (cache.writeWordNoStats(address, value)) {
            // Hit — update stats manually via a lookup
            cache.lookup(address);
            return new Result(0, cache.getConfig().latency);
        }
        // Miss — record the miss in stats, then fetch block from memory
        cache.recordMiss();
        int latency = memoryLatency;
        int[] block = fetchBlock(address);
        CacheLevel.EvictionResult evict = cache.insert(address, block);
        if (evict != null) {
            writeBackBlock(evict.address, evict.data);
        }
        cache.writeWordNoStats(address, value);
        return new Result(0, latency);
    }

    /** Fetch a full cache block from physical memory. */
    private int[] fetchBlock(int address) {
        int blockSize = blockSizeWords * 4;
        int blockStart = (address / blockSize) * blockSize;
        int[] block = new int[blockSizeWords];
        for (int i = 0; i < blockSizeWords; i++) {
            block[i] = memory.readWord(blockStart + i * 4);
        }
        return block;
    }

    /** Write an evicted dirty block back to physical memory. */
    private void writeBackBlock(int address, int[] data) {
        for (int i = 0; i < data.length; i++) {
            memory.writeWord(address + i * 4, data[i]);
        }
    }

    public int getHits()   { return cache.getHits(); }
    public int getMisses() { return cache.getMisses(); }
}
