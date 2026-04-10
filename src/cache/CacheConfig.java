package cache;

/**
 * Immutable configuration for a single cache level.
 * All parameters are validated at construction time.
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
        if (size <= 0 || blockSize <= 0 || associativity <= 0 || latency <= 0) {
            throw new IllegalArgumentException(
                    "Cache parameters must be positive: size=" + size
                            + " blockSize=" + blockSize + " assoc=" + associativity
                            + " latency=" + latency);
        }
        if (size % (blockSize * associativity) != 0) {
            throw new IllegalArgumentException(
                    "Cache size (" + size + ") must be divisible by blockSize*associativity ("
                            + blockSize + "*" + associativity + "=" + (blockSize * associativity) + ")");
        }
        this.size = size;
        this.blockSize = blockSize;
        this.associativity = associativity;
        this.latency = latency;
        this.policy = policy;
    }

    public int getNumSets() {
        return size / (blockSize * associativity);
    }

    @Override
    public String toString() {
        return size + "B, " + blockSize + "B blocks, " + associativity + "-way, "
                + latency + "-cycle, " + policy;
    }
}
