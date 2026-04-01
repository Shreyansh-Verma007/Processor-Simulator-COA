package cache;

/**
 * A single cache line (block) within a set.
 */
public class CacheLine {
    public boolean valid = false;
    public boolean dirty = false;
    public int tag = -1;
    public int[] data; // block data in words

    // For replacement policies
    public long lastUsed = 0; // LRU: timestamp of last access
    public long insertOrder = 0; // FIFO: timestamp of insertion

    public CacheLine(int blockSizeWords) {
        this.data = new int[blockSizeWords];
    }
}
