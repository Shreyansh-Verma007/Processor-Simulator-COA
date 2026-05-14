# Study Report — `Stats.java`

> **File:** `src/core/Stats.java`
> **Module:** Simulation performance metrics for the RISC-V pipeline simulator
> **Date Updated:** 2026-05-12 *(Phase 3 — VM statistics added)*

---

## Table of Contents

1. [Overview](#overview)
2. [Package Declaration](#package-declaration)
3. [Class Declaration & Fields](#class-declaration--fields)
   - [Pipeline Metrics](#pipeline-metrics)
   - [Cache Metrics (Phase 2)](#cache-metrics-phase-2)
4. [Method — `getIPC()`](#getipc)
5. [Method — `getMissRate()`](#getmissrate)
6. [Method — `collectCacheStats()`](#collectcachestats)
7. [Summary Table](#summary-table)
8. [Where Each Metric Is Updated](#where-each-metric-is-updated)
9. [Example Output](#example-output)

---

## Overview

`Stats` is a **data class** that accumulates all performance metrics during simulation. It is shared across all pipeline components and written to `output.txt` at the end of execution.

**Phase 2 update:** In addition to the original pipeline metrics, `Stats` now tracks detailed **per-level cache hit/miss counts** and exposes a `getMissRate()` helper and `collectCacheStats()` to populate them from the `CacheHierarchy` at the end of simulation.

**Phase 3 update:** Added **virtual memory statistics** fields: TLB hits/misses, page walks, page faults, page evictions, dirty evictions, and total translation penalty cycles. These are populated by the `TraceSimulator` at the end of trace replay.

---

## Package Declaration

```java
package core;
import cache.CacheLevel;
```

---

## Class Declaration & Fields

### Pipeline Metrics

```java
public int cycles              = 0;
public int stalls              = 0;
public int branchFlushes       = 0;
public int instructionsRetired = 0;
```

All pipeline metric fields are `public` for direct access from pipeline components.

| Field | Description |
|-------|-------------|
| `cycles` | Total clock cycles elapsed. Incremented every iteration of the `PipelineController` main loop (including cache-stall cycles). |
| `stalls` | Data hazard and multi-cycle instruction stall cycles. Incremented by `PipelineController`. Also incremented for branch flushes that coincide with a stall. |
| `branchFlushes` | Cycles where a branch misprediction or taken branch (JAL) flush occurred. |
| `instructionsRetired` | Instructions that completed WB stage. Incremented by `WB_Stage.tick()`. |

---

### Cache Metrics (Phase 2)

```java
public int l1iHits   = 0, l1iMisses = 0;
public int l1dHits   = 0, l1dMisses = 0;
public int l2Hits    = 0, l2Misses  = 0;
```

Six counters — two (hits, misses) for each of the three cache levels. They are **zero by default** and only populated at the very end of simulation by `collectCacheStats()`.

| Field Pair | Level | Counts |
|-----------|-------|--------|
| `l1iHits`, `l1iMisses` | L1 Instruction | Accesses from IF stage only |
| `l1dHits`, `l1dMisses` | L1 Data | Accesses from MEM stage only |
| `l2Hits`, `l2Misses` | L2 Unified | Accesses from L1I or L1D misses |

---

### Virtual Memory Metrics (Phase 3)

```java
public int tlbHits    = 0;
public int tlbMisses  = 0;
public int pageWalks  = 0;
public int pageFaults = 0;
public int pageEvictions  = 0;
public int dirtyEvictions = 0;
public long totalTranslationPenaltyCycles = 0;
```

These fields are populated by `TraceSimulator.run()` after the trace replay completes, by reading from the `VirtualMemoryUnit` statistics accessors.

| Field | Description |
|-------|-------------|
| `tlbHits` | Number of TLB lookups that found a valid mapping |
| `tlbMisses` | Number of TLB lookups that did not find a mapping |
| `pageWalks` | Number of page table walks performed (= TLB misses) |
| `pageFaults` | Number of accesses to unmapped pages (required frame allocation) |
| `pageEvictions` | Number of pages evicted from physical memory |
| `dirtyEvictions` | Number of evicted pages that had been written to (require writeback) |
| `totalTranslationPenaltyCycles` | Cumulative translation overhead across all L/S instructions |

---

## `getIPC()`

```java
public double getIPC() {
    return cycles == 0 ? 0 : (double) instructionsRetired / cycles;
}
```

Computes **Instructions Per Cycle**. Guards against division-by-zero when cycles = 0.

| IPC | Interpretation |
|-----|----------------|
| 1.0 | Ideal — one instruction completed per cycle |
| < 1.0 | Inefficiency from stalls, flushes, or cache misses |

---

## `getMissRate()`

```java
public double getMissRate(int hits, int misses) {
    int total = hits + misses;
    return total == 0 ? 0.0 : (double) misses / total;
}
```

General-purpose miss rate calculator. Called by `Main.java` for each cache level:

```java
s.getMissRate(s.l1iHits, s.l1iMisses)  // L1I miss rate
s.getMissRate(s.l1dHits, s.l1dMisses)  // L1D miss rate
s.getMissRate(s.l2Hits,  s.l2Misses)   // L2 miss rate
```

Returns 0.0 if no accesses occurred (defensive against division by zero).

---

## `collectCacheStats()`

```java
public void collectCacheStats(cache.CacheHierarchy hierarchy) {
    if (hierarchy == null) return;
    CacheLevel l1i = hierarchy.getL1I();
    CacheLevel l1d = hierarchy.getL1D();
    CacheLevel l2  = hierarchy.getL2();
    l1iHits   = l1i.getHits();
    l1iMisses = l1i.getMisses();
    l1dHits   = l1d.getHits();
    l1dMisses = l1d.getMisses();
    l2Hits    = l2.getHits();
    l2Misses  = l2.getMisses();
}
```

Called by `PipelineController.run()` after the simulation loop exits:

```java
// At end of PipelineController.run():
stats.collectCacheStats(cache);
```

If `hierarchy` is `null` (Phase 1 / no-cache mode), the method returns immediately and all cache counters stay at zero.

---

## Summary Table

| Method | Return | Purpose |
|--------|--------|---------|
| `getIPC()` | `double` | Instructions per cycle |
| `getMissRate(hits, misses)` | `double` | Miss rate for any cache level |
| `collectCacheStats(hierarchy)` | `void` | Populate cache hit/miss fields from hierarchy |

---

## Where Each Metric Is Updated

| Metric | Updated By | Trigger |
|--------|-----------|---------| 
| `cycles` | `PipelineController` / `TraceSimulator` | Every clock cycle |
| `stalls` | `PipelineController` / `TraceSimulator` | Hazard stalls, cache miss stalls, MUL extra cycles |
| `branchFlushes` | `PipelineController` | Branch misprediction or JAL detected in EX |
| `instructionsRetired` | `WB_Stage.tick()` / `TraceSimulator` | Non-NOP instruction reaches WB / each trace instruction |
| `l1i*`, `l1d*`, `l2*` | `collectCacheStats()` | Once, after simulation loop ends |
| `tlbHits`, `tlbMisses` | `TraceSimulator.run()` | Once, after trace replay completes |
| `pageWalks`, `pageFaults` | `TraceSimulator.run()` | Once, from `VirtualMemoryUnit` |
| `pageEvictions`, `dirtyEvictions` | `TraceSimulator.run()` | Once, from `VirtualMemoryUnit` |
| `totalTranslationPenaltyCycles` | `TraceSimulator.run()` | Once, from `VirtualMemoryUnit` |

---

## Example Output — Pipeline Mode

Written to `output.txt` by `Main.java`:

```
=== Simulation Stats ===
Cycles             : 42
Stalls             : 5
Branch Flushes     : 2
Instructions Retired: 20
IPC                : 0.476

--- Cache Configuration ---
L1I  : 1024B, 64B blocks, 2-way, 5-cycle, LRU
L1D  : 1024B, 64B blocks, 2-way, 5-cycle, LRU
L2   : 8192B, 64B blocks, 4-way, 50-cycle, LRU
Memory Latency: 200 cycles
Forwarding    : enabled

--- Cache Statistics ---
L1I  : 18 hits,  2 misses, miss rate 0.100
L1D  : 15 hits,  5 misses, miss rate 0.250
L2   :  7 hits,  0 misses, miss rate 0.000
```

## Example Output — Trace Replay Mode (Phase 3)

```
=== Trace Replay Simulation Stats ===

--- Execution ---
Total Cycles              : 73004328
Instructions Retired      : 715724
IPC                       : 0.0098
Stalls                    : 72288604

--- Virtual Memory ---
TLB Hits                  : 357854
TLB Misses                : 8
TLB Hit Rate              : 1.0000
Page Walks                : 8
Page Faults               : 8
Page Evictions            : 0
Dirty Evictions           : 0
Translation Penalty Cycles: 358342

--- Cache Statistics ---
L1D Hits                  : 0
L1D Misses                : 357862
L1D Miss Rate             : 1.0000
```

---

*End of Report*
