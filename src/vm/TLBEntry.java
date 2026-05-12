package vm;

/**
 * A single TLB entry mapping a virtual page number to a physical frame number.
 */
public class TLBEntry {
    public boolean valid = false;
    public int virtualPageNumber = -1;
    public int physicalFrameNumber = -1;
    public boolean dirty = false;

    // For replacement policies
    public long lastUsed = 0;     // LRU: timestamp of last access
    public long insertOrder = 0;  // FIFO: timestamp of insertion
}
