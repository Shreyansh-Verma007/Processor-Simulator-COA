package cache;

/**
 * A single cache line (block) within a set.
 */
public class CacheLine {
    public boolean valid = false;
    public boolean dirty = false;
    public int tag = -1;
    public final int[] data;

    // Byte address of the block start when valid. Stored so eviction can produce
    // the correct write-back address without reconstructing it from (tag, set).
    public long blockAddress = 0;

    // For replacement policies
    public long lastUsed = 0;
    public long insertOrder = 0;

    public CacheLine(int blockSizeWords) {
        this.data = new int[blockSizeWords];
    }
}
