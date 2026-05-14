package core;

import cache.CacheLevel;

// Simulation performance metrics — shared between pipeline and trace modes.
public class Stats {
    public long cycles = 0;
    public long stalls = 0;
    public long branchFlushes = 0;
    public long instructionsRetired = 0;

    // Cache stats (populated at end of simulation from CacheHierarchy)
    public long l1iHits = 0, l1iMisses = 0;
    public long l1dHits = 0, l1dMisses = 0;
    public long l2Hits = 0, l2Misses = 0;

    // Virtual Memory stats (populated by TraceSimulator)
    public long tlbHits = 0;
    public long tlbMisses = 0;
    public long pageWalks = 0;
    public long pageFaults = 0;
    public long pageEvictions = 0;
    public long dirtyEvictions = 0;
    public long swapOuts = 0;
    public long swapIns = 0;
    public long totalTranslationPenaltyCycles = 0;

    public double getIPC() {
        return cycles == 0 ? 0 : (double) instructionsRetired / cycles;
    }

    public double getMissRate(long hits, long misses) {
        long total = hits + misses;
        return total == 0 ? 0.0 : (double) misses / total;
    }

    /**
     * Collect hit/miss stats from the cache hierarchy.
     * Handles null hierarchy and null individual cache levels gracefully.
     */
    public void collectCacheStats(cache.CacheHierarchy hierarchy) {
        if (hierarchy == null)
            return;

        CacheLevel l1i = hierarchy.getL1I();
        CacheLevel l1d = hierarchy.getL1D();
        CacheLevel l2 = hierarchy.getL2();

        if (l1i != null) {
            l1iHits = l1i.getHits();
            l1iMisses = l1i.getMisses();
        }
        if (l1d != null) {
            l1dHits = l1d.getHits();
            l1dMisses = l1d.getMisses();
        }
        if (l2 != null) {
            l2Hits = l2.getHits();
            l2Misses = l2.getMisses();
        }
    }
}
