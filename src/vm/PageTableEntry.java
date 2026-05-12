package vm;

/**
 * A single entry in the flat page table.
 * Maps a virtual page to a physical frame.
 */
public class PageTableEntry {
    public boolean valid = false;
    public int frameNumber = -1;
    public boolean dirty = false;

    // For page replacement (LRU/FIFO among all mapped pages)
    public long lastUsed = 0;
    public long insertOrder = 0;
}
