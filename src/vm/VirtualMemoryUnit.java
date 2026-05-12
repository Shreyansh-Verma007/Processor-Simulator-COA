package vm;

import common.Config;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Orchestrates the virtual memory translation flow:
 *   TLB lookup → Page Table walk → Page Fault handling → Frame allocation.
 *
 * Manages physical frame allocation, page replacement (LRU/FIFO),
 * and dirty page writeback tracking.
 */
public class VirtualMemoryUnit {

    private final TLB tlb;
    private final PageTable pageTable;
    private final Config cfg;

    // Physical frame management
    private final int numFrames;
    private final Queue<Integer> freeFrames;    // free frame pool
    private final boolean useLRU;
    private long clock = 0;

    // Statistics
    private int pageWalks = 0;
    private int pageFaults = 0;
    private int pageEvictions = 0;
    private int dirtyEvictions = 0;
    private long totalTranslationPenalty = 0;

    public VirtualMemoryUnit(Config cfg) {
        this.cfg = cfg;
        int pageSizeBytes = cfg.getPageSizeBytes();
        this.numFrames = cfg.getPhysicalSizeBytes() / pageSizeBytes;
        this.useLRU = cfg.getVmReplacementPolicy().equalsIgnoreCase("lru");

        this.tlb = new TLB(cfg.getDtlbEntries(), cfg.getVmReplacementPolicy());
        this.pageTable = new PageTable(cfg.getVirtualSizeBytes(), pageSizeBytes);

        // Initialize free frame list
        this.freeFrames = new LinkedList<>();
        for (int i = 0; i < numFrames; i++) {
            freeFrames.add(i);
        }
    }

    /**
     * Translate a virtual address to a physical address.
     *
     * @param virtualAddress the 32-bit virtual address
     * @param isStore        true if this is a store (sets dirty bits)
     * @return TranslationResult with physical address and latency
     */
    public TranslationResult translateAddress(int virtualAddress, boolean isStore) {
        int pageSizeBytes = cfg.getPageSizeBytes();
        // Use unsigned division for 32-bit virtual addresses
        int vpn = Integer.divideUnsigned(virtualAddress, pageSizeBytes);
        int offset = Integer.remainderUnsigned(virtualAddress, pageSizeBytes);
        int latency = 0;

        // Step 1: TLB lookup
        latency += cfg.getTlbHitLatency();
        int pfn = tlb.lookup(vpn);

        if (pfn >= 0) {
            // TLB hit
            if (isStore) {
                tlb.markDirty(vpn);
                // Also update page table dirty bit
                PageTableEntry pte = pageTable.lookup(vpn);
                if (pte.valid) {
                    pte.dirty = true;
                }
            }
            // Update page table LRU timestamp
            PageTableEntry pte = pageTable.lookup(vpn);
            if (pte.valid) {
                pte.lastUsed = clock++;
            }
        } else {
            // TLB miss — need page walk
            latency += cfg.getPageWalkLatency();
            pageWalks++;

            PageTableEntry pte = pageTable.lookup(vpn);

            if (!pte.valid) {
                // Page fault — page not in physical memory
                latency += cfg.getPageFaultLatency();
                pageFaults++;

                // Allocate a frame
                int frame = allocateFrame();
                pageTable.mapPage(vpn, frame, clock++);
                pte = pageTable.lookup(vpn); // re-fetch after mapping
            } else {
                // Page table hit — update LRU
                pte.lastUsed = clock++;
            }

            pfn = pte.frameNumber;

            if (isStore) {
                pte.dirty = true;
            }

            // Insert into TLB
            tlb.insert(vpn, pfn, isStore || pte.dirty);
        }

        totalTranslationPenalty += latency;

        int physicalAddress = pfn * pageSizeBytes + offset;
        return new TranslationResult(physicalAddress, latency);
    }

    /**
     * Allocate a physical frame. If no free frames, evict a page.
     */
    private int allocateFrame() {
        if (!freeFrames.isEmpty()) {
            return freeFrames.poll();
        }

        // Physical memory is full — need to evict a page
        return evictPage();
    }

    /**
     * Evict a page using the configured replacement policy (LRU or FIFO).
     * Returns the freed frame number.
     */
    private int evictPage() {
        pageEvictions++;

        // Find victim: scan all valid PTEs, pick one with smallest lastUsed (LRU)
        // or smallest insertOrder (FIFO)
        int victimVPN = -1;
        long victimTimestamp = Long.MAX_VALUE;

        int numPages = pageTable.getNumPages();
        for (int vpn = 0; vpn < numPages; vpn++) {
            PageTableEntry pte = pageTable.lookup(vpn);
            if (!pte.valid) continue;

            long ts = useLRU ? pte.lastUsed : pte.insertOrder;
            if (ts < victimTimestamp) {
                victimTimestamp = ts;
                victimVPN = vpn;
            }
        }

        if (victimVPN == -1) {
            throw new IllegalStateException("No page to evict but no free frames!");
        }

        PageTableEntry victimPTE = pageTable.lookup(victimVPN);
        int freedFrame = victimPTE.frameNumber;

        // Check dirty: both PTE and TLB may have dirty info
        boolean isDirty = victimPTE.dirty || tlb.isDirty(victimVPN);
        if (isDirty) {
            dirtyEvictions++;
        }

        // Invalidate in TLB and page table
        tlb.invalidate(victimVPN);
        pageTable.unmapPage(victimVPN);

        return freedFrame;
    }

    // ── Statistics accessors ─────────────────────────────────────────────

    public int getTlbHits() {
        return tlb.getHits();
    }

    public int getTlbMisses() {
        return tlb.getMisses();
    }

    public int getPageWalks() {
        return pageWalks;
    }

    public int getPageFaults() {
        return pageFaults;
    }

    public int getPageEvictions() {
        return pageEvictions;
    }

    public int getDirtyEvictions() {
        return dirtyEvictions;
    }

    public long getTotalTranslationPenalty() {
        return totalTranslationPenalty;
    }
}
