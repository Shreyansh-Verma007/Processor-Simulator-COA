package cache;

/**
 * Configuration for a single cache level.
 */
public class CacheConfig {
    public final int size; // total cache size in bytes
    public final int blockSize; // block (line) size in bytes
    public final int associativity; // number of ways per set
    public final int latency; // access latency in cycles (on hit)
    public final ReplacementPolicy policy;

    public enum ReplacementPolicy {
        LRU, FIFO
    }

    public CacheConfig(int size, int blockSize, int associativity,
            int latency, ReplacementPolicy policy) {
        this.size = size;
        this.blockSize = blockSize;
        this.associativity = associativity;
        this.latency = latency;
        this.policy = policy;
    }

    public int getNumSets() {
        return size / (blockSize * associativity);
    }
}
