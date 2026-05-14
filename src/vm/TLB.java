package vm;

/**
 * Fully-associative data TLB with configurable size and replacement policy.
 * Supports LRU and FIFO eviction.
 */
public class TLB {

    private final TLBEntry[] entries;
    private final int numEntries;
    private final boolean useLRU; // true = LRU, false = FIFO
    private long clock = 0;

    // Statistics
    private long hits = 0;
    private long misses = 0;

    public TLB(int numEntries, String replacementPolicy) {
        if (numEntries < 1) {
            throw new IllegalArgumentException("TLB must have at least 1 entry, got: " + numEntries);
        }
        this.numEntries = numEntries;
        this.useLRU = replacementPolicy.equalsIgnoreCase("lru");
        this.entries = new TLBEntry[numEntries];
        for (int i = 0; i < numEntries; i++) {
            entries[i] = new TLBEntry();
        }
    }

    /**
     * Look up a virtual page number in the TLB.
     * Returns the physical frame number on hit, or -1 on miss.
     */
    public int lookup(int vpn) {
        for (int i = 0; i < numEntries; i++) {
            TLBEntry e = entries[i];
            if (e.valid && e.virtualPageNumber == vpn) {
                hits++;
                e.lastUsed = clock++;
                return e.physicalFrameNumber;
            }
        }
        misses++;
        return -1;
    }

    /**
     * Insert a new mapping into the TLB. If full, evict based on policy.
     */
    public void insert(int vpn, int pfn, boolean dirty) {
        // Check if already present (update in-place)
        for (int i = 0; i < numEntries; i++) {
            TLBEntry e = entries[i];
            if (e.valid && e.virtualPageNumber == vpn) {
                e.physicalFrameNumber = pfn;
                e.dirty = dirty;
                e.lastUsed = clock++;
                return;
            }
        }

        // Find an invalid (empty) slot first
        for (int i = 0; i < numEntries; i++) {
            if (!entries[i].valid) {
                fillEntry(entries[i], vpn, pfn, dirty);
                return;
            }
        }

        // All slots full — pick a victim
        int victim = selectVictim();
        fillEntry(entries[victim], vpn, pfn, dirty);
    }

    /**
     * Mark the TLB entry for the given VPN as dirty (for store instructions).
     */
    public void markDirty(int vpn) {
        for (int i = 0; i < numEntries; i++) {
            TLBEntry e = entries[i];
            if (e.valid && e.virtualPageNumber == vpn) {
                e.dirty = true;
                e.lastUsed = clock++; // refresh LRU on store access
                return;
            }
        }
    }

    /**
     * Invalidate the TLB entry for the given VPN (on page eviction).
     */
    public void invalidate(int vpn) {
        for (int i = 0; i < numEntries; i++) {
            TLBEntry e = entries[i];
            if (e.valid && e.virtualPageNumber == vpn) {
                e.valid = false;
                return;
            }
        }
    }

    /**
     * Check if a given VPN has a dirty TLB entry.
     */
    public boolean isDirty(int vpn) {
        for (int i = 0; i < numEntries; i++) {
            TLBEntry e = entries[i];
            if (e.valid && e.virtualPageNumber == vpn) {
                return e.dirty;
            }
        }
        return false;
    }

    private void fillEntry(TLBEntry e, int vpn, int pfn, boolean dirty) {
        e.valid = true;
        e.virtualPageNumber = vpn;
        e.physicalFrameNumber = pfn;
        e.dirty = dirty;
        e.lastUsed = clock;
        e.insertOrder = clock;
        clock++;
    }

    private int selectVictim() {
        int victim = 0;
        if (useLRU) {
            long oldest = Long.MAX_VALUE;
            for (int i = 0; i < numEntries; i++) {
                if (entries[i].lastUsed < oldest) {
                    oldest = entries[i].lastUsed;
                    victim = i;
                }
            }
        } else { // FIFO
            long earliest = Long.MAX_VALUE;
            for (int i = 0; i < numEntries; i++) {
                if (entries[i].insertOrder < earliest) {
                    earliest = entries[i].insertOrder;
                    victim = i;
                }
            }
        }
        return victim;
    }

    // ── Statistics ────────────────────────────────────────────────────────

    public long getHits() {
        return hits;
    }

    public long getMisses() {
        return misses;
    }
}
