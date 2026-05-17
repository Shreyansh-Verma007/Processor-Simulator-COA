package vm;

import common.Config;
import core.Memory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import cache.CacheHierarchy;

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

    private final Memory physicalMemory;

    // Optional cache hierarchy for PIPT invalidation on frame eviction
    private CacheHierarchy cacheHierarchy;

    // Physical frame management
    private final int numFrames;
    private final Queue<Integer> freeFrames;    // free frame pool
    private final boolean useLRU;
    private long clock = 0;

    // Swap space: VPN → saved page data (word array)
    private final Map<Integer, int[]> swapSpace = new HashMap<>();
    private long swapOuts = 0;
    private long swapIns = 0;

    // Statistics
    private long pageWalks = 0;
    private long pageFaults = 0;
    private long pageEvictions = 0;
    private long dirtyEvictions = 0;
    private long totalTranslationPenalty = 0;

    public VirtualMemoryUnit(Config cfg, Memory physicalMemory) {
        this.cfg = cfg;
        this.physicalMemory = physicalMemory;
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
     * Set the cache hierarchy for PIPT frame invalidation.
     * Must be called before running any trace that uses cache.
     */
    public void setCacheHierarchy(CacheHierarchy cache) {
        this.cacheHierarchy = cache;
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

        // Step 1: TLB lookup — always costs tlb_hit_latency (per spec)
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
            // TLB miss — page walk is EXTRA on top of TLB access (per spec)
            latency += cfg.getPageWalkLatency();
            pageWalks++;

            PageTableEntry pte = pageTable.lookup(vpn);

            if (!pte.valid) {
                // Page fault — page not in physical memory
                latency += cfg.getPageFaultLatency();
                pageFaults++;

                // Allocate a frame and restore swap data (or zero-fill)
                int frame = allocateFrame();
                restoreOrZeroFrame(vpn, frame);
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
            saveToSwap(victimVPN, freedFrame);
        }

        // Invalidate in TLB and page table
        tlb.invalidate(victimVPN);
        pageTable.unmapPage(victimVPN);

        return freedFrame;
    }

    /** Save a frame's data to swap space. */
    private void saveToSwap(int vpn, int frame) {
        int pageSizeBytes = cfg.getPageSizeBytes();
        int wordsPerPage = pageSizeBytes / 4;
        int baseAddr = frame * pageSizeBytes;
        int[] pageData = new int[wordsPerPage];
        for (int i = 0; i < wordsPerPage; i++) {
            pageData[i] = physicalMemory.readWord(baseAddr + i * 4);
        }
        swapSpace.put(vpn, pageData);
        swapOuts++;
    }

    /** Restore page data from swap, or zero-fill if fresh page. */
    private void restoreOrZeroFrame(int vpn, int frame) {
        int pageSizeBytes = cfg.getPageSizeBytes();
        int wordsPerPage = pageSizeBytes / 4;
        int baseAddr = frame * pageSizeBytes;
        if (swapSpace.containsKey(vpn)) {
            int[] pageData = swapSpace.remove(vpn);
            for (int i = 0; i < wordsPerPage; i++) {
                physicalMemory.writeWord(baseAddr + i * 4, pageData[i]);
            }
            swapIns++;
        } else {
            for (int i = 0; i < wordsPerPage; i++) {
                physicalMemory.writeWord(baseAddr + i * 4, 0);
            }
        }
    }

    /** Dump swap state to swap.txt for inspection. */
    public void writeSwapFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("swap.txt"))) {
            pw.println("=== Swap Space Dump ===");
            pw.println("Swap Outs (writes): " + swapOuts);
            pw.println("Swap Ins  (reads) : " + swapIns);
            pw.println("Resident in swap  : " + swapSpace.size());
            for (Map.Entry<Integer, int[]> e : swapSpace.entrySet()) {
                pw.println("  VPN " + e.getKey() + ": " + e.getValue().length + " words");
            }
        } catch (IOException e) {
            System.err.println("WARNING: Could not write swap.txt: " + e.getMessage());
        }
    }

    // ── Statistics accessors ─────────────────────────────────────────────

    public long getTlbHits()                  { return tlb.getHits(); }
    public long getTlbMisses()                { return tlb.getMisses(); }
    public long getPageWalks()                { return pageWalks; }
    public long getPageFaults()               { return pageFaults; }
    public long getPageEvictions()             { return pageEvictions; }
    public long getDirtyEvictions()            { return dirtyEvictions; }
    public long getTotalTranslationPenalty()  { return totalTranslationPenalty; }
    public long getSwapOuts()                  { return swapOuts; }
    public long getSwapIns()                   { return swapIns; }
}
