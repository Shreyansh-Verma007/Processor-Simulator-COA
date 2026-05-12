package vm;

/**
 * Flat (single-level) page table for 32-bit virtual addresses.
 * Indexed by virtual page number (VPN).
 */
public class PageTable {

    private final PageTableEntry[] entries;
    private final int numPages;

    public PageTable(int virtualSizeBytes, int pageSizeBytes) {
        this.numPages = virtualSizeBytes / pageSizeBytes;
        this.entries = new PageTableEntry[numPages];
        for (int i = 0; i < numPages; i++) {
            entries[i] = new PageTableEntry();
        }
    }

    /**
     * Look up the PTE for a given virtual page number.
     * Returns the entry (which may be invalid — caller must check).
     */
    public PageTableEntry lookup(int vpn) {
        if (vpn < 0 || vpn >= numPages) {
            throw new IllegalArgumentException(
                    "VPN " + vpn + " out of range [0, " + numPages + ")");
        }
        return entries[vpn];
    }

    /**
     * Create a mapping from VPN to a physical frame.
     */
    public void mapPage(int vpn, int frameNumber, long timestamp) {
        PageTableEntry pte = entries[vpn];
        pte.valid = true;
        pte.frameNumber = frameNumber;
        pte.dirty = false;
        pte.lastUsed = timestamp;
        pte.insertOrder = timestamp;
    }

    /**
     * Remove the mapping for a given VPN (on page eviction).
     */
    public void unmapPage(int vpn) {
        PageTableEntry pte = entries[vpn];
        pte.valid = false;
        pte.frameNumber = -1;
        pte.dirty = false;
    }

    /**
     * Find the VPN that is currently mapped to a given frame number.
     * Returns -1 if no page maps to that frame.
     */
    public int findVPNByFrame(int frameNumber) {
        for (int i = 0; i < numPages; i++) {
            if (entries[i].valid && entries[i].frameNumber == frameNumber) {
                return i;
            }
        }
        return -1;
    }

    public int getNumPages() {
        return numPages;
    }
}
