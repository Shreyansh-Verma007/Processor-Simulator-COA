package core;

import cache.CacheLevel;

// Simulation performance metrics.
public class Stats {
    public int cycles = 0;
    public int stalls = 0;
    public int branchFlushes = 0;
    public int instructionsRetired = 0;

    // Cache stats (populated at end of simulation from CacheHierarchy)
    public int l1iHits = 0, l1iMisses = 0;
    public int l1dHits = 0, l1dMisses = 0;
    public int l2Hits = 0, l2Misses = 0;

    public double getIPC() {
        return cycles == 0 ? 0 : (double) instructionsRetired / cycles;
    }

    public double getMissRate(int hits, int misses) {
        int total = hits + misses;
        return total == 0 ? 0.0 : (double) misses / total;
    }

    public void collectCacheStats(cache.CacheHierarchy hierarchy) {
        if (hierarchy == null)
            return;
        CacheLevel l1i = hierarchy.getL1I();
        CacheLevel l1d = hierarchy.getL1D();
        CacheLevel l2 = hierarchy.getL2();
        l1iHits = l1i.getHits();
        l1iMisses = l1i.getMisses();
        l1dHits = l1d.getHits();
        l1dMisses = l1d.getMisses();
        l2Hits = l2.getHits();
        l2Misses = l2.getMisses();
    }
}
