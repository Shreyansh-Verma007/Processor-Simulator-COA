# Study Report — `CacheHierarchy.java`

> **File:** `src/cache/CacheHierarchy.java`
> **Module:** Cache – two-level cache hierarchy orchestrator
> **Date Generated:** 2026-04-19

---

## Table of Contents

1. [Overview](#overview)
2. [Package & Imports](#package--imports)
3. [Fields & Constructor](#fields--constructor)
4. [Public API — Fetch & Access Methods](#public-api--fetch--access-methods)
5. [Internal Logic — `readThrough()`](#internal-logic--readthrough)
6. [Block Fetch — `fetchBlockToL1()`](#block-fetch--fetchblocktol1)
7. [Write-Back Helpers](#write-back-helpers)
8. [Inner Class — `FetchResult`](#inner-class--fetchresult)
9. [Stats Accessors](#stats-accessors)
10. [Cache Access Flow Diagram](#cache-access-flow-diagram)
11. [Statistics Counting Policy](#statistics-counting-policy)
12. [Summary Table](#summary-table)

---

## Overview

`CacheHierarchy` manages a **two-level cache hierarchy**:

```
IF stage:    L1I (instruction) ──→ L2 (unified) ──→ Main Memory
MEM stage:   L1D (data)        ──→ L2 (unified) ──→ Main Memory
```

Key design decisions:
- **Write-back with write-allocate:** Dirty lines propagate downward on eviction; stores that miss in L1D fetch the block first before writing.
- **Variable latency:** Each access returns an `AccessResult` with the total latency in cycles (used by the pipeline to insert cache-miss stall cycles).
- **Strict stats counting:** Each pipeline request counts as exactly **one L1 access** (hit or miss). If L1 misses, exactly **one L2 access** is counted. Internal fills and write-backs use no-stats methods to avoid double-counting.

---

## Package & Imports

```java
package cache;
import core.Memory;
```

`Memory` is the backing store representing main memory (128 KB by default).

---

## Fields & Constructor

```java
private final CacheLevel l1i;
private final CacheLevel l1d;
private final CacheLevel l2;
private final Memory memory;
private final int memoryLatency;
```

| Field | Description |
|-------|-------------|
| `l1i` | L1 instruction cache (separate from data). |
| `l1d` | L1 data cache (separate from instructions). |
| `l2` | Unified L2 cache (shared by L1I and L1D). |
| `memory` | Simulated main memory — last resort on L2 miss. |
| `memoryLatency` | Main memory access latency in cycles (e.g., 200). |

```java
public CacheHierarchy(CacheConfig l1iCfg, CacheConfig l1dCfg,
        CacheConfig l2Cfg, int memoryLatency, Memory memory) {
    this.l1i = new CacheLevel(l1iCfg);
    this.l1d = new CacheLevel(l1dCfg);
    this.l2  = new CacheLevel(l2Cfg);
    this.memory = memory;
    this.memoryLatency = memoryLatency;
}
```

---

## Public API — Fetch & Access Methods

### Instruction Fetch

```java
public AccessResult fetchInstruction(int address)
```

Called by `IF_Stage`. Reads through **L1I → L2 → Memory**. Returns `AccessResult` with the fetched instruction word and total latency.

### Data Reads

```java
public AccessResult readData(int address)      // full word (LW)
public AccessResult readDataByte(int address)  // single byte (LB)
```

Both route through **L1D → L2 → Memory**. `readDataByte` reads the aligned word and extracts the correct byte:
```java
int bytePos = (address % 4) * 8;
int byteVal = (word >> bytePos) & 0xFF;
```

### Data Writes

```java
public AccessResult writeData(int address, int value)      // full word (SW)
public AccessResult writeDataByte(int address, int value)  // single byte (SB)
```

Both implement **write-allocate** write-back:
1. `lookup(address)` — one stat-counted L1D access.
2. **Hit:** Write directly into the L1D block, mark `dirty = true`. Latency = L1D hit latency.
3. **Miss:** `fetchBlockToL1()` brings the block into L1D first, then writes. Latency = L1D + L2/memory latency.

`writeDataByte` additionally performs a read-modify-write within the word.

---

## Internal Logic — `readThrough()`

```java
private AccessResult readThrough(CacheLevel l1, int address) {
    int latency = l1.getConfig().latency;
    Integer val = l1.readWord(address);     // ONE stat-counted L1 access
    if (val != null) {
        return new AccessResult(val, latency);   // L1 hit
    }
    // L1 miss — fetch block
    FetchResult fetch = fetchBlockToL1(l1, address);
    latency += fetch.latency;
    int offset = getBlockOffset(l1, address);
    return new AccessResult(fetch.blockData[offset], latency);
}
```

This is the shared read path for both instruction fetch and data reads. The total latency is accumulated: `L1 latency + (L2 latency)` on L1 miss, or `L1 + L2 + memory` on L2 miss.

---

## Block Fetch — `fetchBlockToL1()`

```java
private FetchResult fetchBlockToL1(CacheLevel l1, int address)
```

Fetches a full block and installs it into L1. Called only on L1 miss.

**Steps:**
1. `l2.readWord(blockStart)` — ONE stat-counted L2 access.
2. **L2 hit:** Read remaining words of block using no-stats methods. `latency = L2 latency`.
3. **L2 miss:** Read all words from main memory. `latency = L2 latency + memoryLatency`. Then install the full L2-sized block into L2 (evicting L2 victim if needed → `writeBackToMemory`).
4. **Install into L1:** `l1.insert(address, block)`. If L1 eviction is dirty → `writeBackToL2`.
5. Return `FetchResult(blockData, latency)`.

---

## Write-Back Helpers

### `writeBackToL2(EvictionResult eviction)`

Propagates a dirty L1 eviction up to L2:
- If L2 already has a block at that address: write-words directly (no stats).
- If L2 doesn't have the block: fetch from memory, overlay dirty L1 words, insert into L2 (possibly evicting → `writeBackToMemory`).

### `writeBackToMemory(EvictionResult eviction)`

Simply writes each word of the evicted block to `Memory`:
```java
for (int i = 0; i < eviction.data.length; i++) {
    memory.writeWord(eviction.address + i * 4, eviction.data[i]);
}
```

---

## Inner Class — `FetchResult`

```java
private static class FetchResult {
    final int[] blockData;   // full block of words fetched from L2/memory
    final int   latency;     // cycles consumed by this fetch
}
```

Used internally to pass the fetched block and its latency cost back to `fetchBlockToL1`.

---

## Stats Accessors

```java
public CacheLevel getL1I()  { return l1i; }
public CacheLevel getL1D()  { return l1d; }
public CacheLevel getL2()   { return l2;  }
```

Expose the three `CacheLevel` objects so `Stats.collectCacheStats()` can read their hit/miss counts at the end of simulation.

---

## Cache Access Flow Diagram

### Read Path (LW or instruction fetch)

```
pipeline request
      │
      ▼
  L1 lookup ──── HIT ────→ return(data, L1_latency)
      │
    MISS
      │
      ▼
  L2 lookup ──── HIT ────→ read block (no-stats)
      │                      install into L1
      │                      return(data, L1+L2_latency)
    MISS
      │
      ▼
  main memory read
    install block into L2
    install block into L1
    return(data, L1+L2+MEM_latency)
```

### Write Path (SW or SB) — write-allocate

```
pipeline request
      │
      ▼
  L1 lookup ──── HIT ────→ write to L1, mark dirty
      │                      return(0, L1_latency)
    MISS
      │
      ▼
  fetchBlockToL1()
  (same flow as read path)
      │
      ▼
  write word to L1 (no stats)
  return(0, L1+fetch_latency)
```

---

## Statistics Counting Policy

| Access | L1 count | L2 count |
|--------|----------|----------|
| L1 hit | 1 | 0 |
| L1 miss, L2 hit | 1 (miss) | 1 (hit) |
| L1 miss, L2 miss | 1 (miss) | 1 (miss) |
| Internal block fill | 0 | 0 |
| Write-back propagation | 0 | 0 |

The `readWord` / `writeWord` (stat-version) methods are called exactly once per pipeline request. All subsequent accesses during fills and write-backs use the `*NoStats` variants.

---

## Summary Table

| Method | Stage | Hierarchy path | Stats |
|--------|-------|---------------|-------|
| `fetchInstruction(addr)` | IF | L1I → L2 → Mem | L1I + L2 |
| `readData(addr)` | MEM | L1D → L2 → Mem | L1D + L2 |
| `readDataByte(addr)` | MEM | L1D → L2 → Mem | L1D + L2 |
| `writeData(addr, val)` | MEM | L1D → L2 → Mem | L1D + L2 |
| `writeDataByte(addr, val)` | MEM | L1D → L2 → Mem | L1D + L2 |

---

*End of Report*
