# Study Report — `CacheLevel.java`

> **File:** `src/cache/CacheLevel.java`
> **Module:** Cache – single level of set-associative cache
> **Date Generated:** 2026-04-19

---

## Table of Contents

1. [Overview](#overview)
2. [Package Declaration](#package-declaration)
3. [Fields](#fields)
4. [Constructor](#constructor)
5. [Address Decomposition](#address-decomposition)
6. [Lookup Methods](#lookup-methods)
7. [No-Stats Internal Methods](#no-stats-internal-methods)
8. [Insertion & Eviction](#insertion--eviction)
9. [Inner Class — `EvictionResult`](#inner-class--evictionresult)
10. [Statistics Accessors](#statistics-accessors)
11. [Summary Table](#summary-table)
12. [Set-Associative Cache Address Layout](#set-associative-cache-address-layout)

---

## Overview

`CacheLevel` implements one level of a **set-associative cache** with configurable:
- Number of sets and ways (from `CacheConfig`)
- Replacement policy (LRU or FIFO)
- Write-back with dirty-bit evictions
- Hit/miss statistics tracking

It is used internally by `CacheHierarchy` (one instance each for L1I, L1D, and L2).

---

## Package Declaration

```java
package cache;
```

---

## Fields

```java
private final CacheConfig config;
private final CacheLine[][] sets;   // [set][way]
private final int numSets;
private final int blockSizeWords;

private int hits   = 0;
private int misses = 0;
private long clock = 0;
```

| Field | Description |
|-------|-------------|
| `config` | Immutable configuration (size, blockSize, associativity, latency, policy). |
| `sets` | 2D array of `CacheLine` objects: `[numSets][associativity]`. |
| `numSets` | Derived from `config.getNumSets()`. |
| `blockSizeWords` | `config.blockSize / 4` — number of `int` words per block. |
| `hits` / `misses` | Accumulated access statistics. |
| `clock` | Monotonically increasing counter used for LRU and FIFO timestamp ordering. |

---

## Constructor

```java
public CacheLevel(CacheConfig config) {
    this.config = config;
    this.numSets = config.getNumSets();
    this.blockSizeWords = config.blockSize / 4;
    this.sets = new CacheLine[numSets][config.associativity];
    for (int s = 0; s < numSets; s++)
        for (int w = 0; w < config.associativity; w++)
            sets[s][w] = new CacheLine(blockSizeWords);
}
```

Pre-allocates all `CacheLine` objects with their data arrays. All lines start invalid.

---

## Address Decomposition

Every byte address is split into three fields:

```
| tag bits | set index bits | block offset bits |
```

```java
private int getBlockOffset(int address) {
    return Math.floorMod(address / 4, blockSizeWords);
}

private int getSetIndex(int address) {
    return Math.floorMod(Integer.divideUnsigned(address, config.blockSize), numSets);
}

private int getTag(int address) {
    return Integer.divideUnsigned(address, config.blockSize * numSets);
}
```

| Method | Formula | Purpose |
|--------|---------|---------|
| `getBlockOffset` | `(address/4) mod blockSizeWords` | Word index within a block |
| `getSetIndex` | `(address/blockSize) mod numSets` | Which set to search |
| `getTag` | `address / (blockSize × numSets)` | Tag for comparison against stored tags |

`Integer.divideUnsigned` and `Math.floorMod` are used to correctly handle all address values without sign-extension issues.

---

## Lookup Methods

### `lookup(int address)` — public, counts stats

```java
public CacheLine lookup(int address) {
    return findLine(address, true);
}
```

Looks up `address` and returns the matching `CacheLine` on hit (increments `hits` and updates `lastUsed`), or `null` on miss (increments `misses`).

### `readWord(int address)` — public, counts stats

```java
public Integer readWord(int address) {
    CacheLine line = lookup(address);
    if (line == null) return null;
    return line.data[getBlockOffset(address)];
}
```

Convenience wrapper: returns the integer word at `address` from a hit line, or `null` on miss.

### Private `findLine(address, updateStats)`

```java
private CacheLine findLine(int address, boolean updateStats) {
    int set = getSetIndex(address);
    int tag = getTag(address);
    for (int w = 0; w < config.associativity; w++) {
        CacheLine line = sets[set][w];
        if (line.valid && line.tag == tag) {
            if (updateStats) hits++;
            line.lastUsed = clock++;
            return line;
        }
    }
    if (updateStats) misses++;
    return null;
}
```

The `updateStats` flag is used to distinguish pipeline-initiated accesses (stat-counted) from internal block-fill and write-back operations (no stats).

---

## No-Stats Internal Methods

These package-private methods are called by `CacheHierarchy` during block fills and write-backs. They **do not count** hits or misses, preventing double-counting.

```java
CacheLine lookupNoStats(int address)        // find line without counting
Integer   readWordNoStats(int address)      // read word without counting
boolean   writeWordNoStats(int address, int value) // write word without counting
```

`writeWordNoStats` returns `true` if the line was found and written (sets `dirty = true`), or `false` if the line wasn't present.

---

## Insertion & Eviction

```java
public EvictionResult insert(int address, int[] blockData)
```

Installs a new block into the cache set for `address`.

**Steps:**
1. Search for an **invalid (empty) slot** — if found, fill it with no eviction.
2. If all ways are valid (set full), **select a victim** via the replacement policy.
3. If the victim is **dirty**, return an `EvictionResult` with the evicted address and data (caller must write it back).
4. **Fill the chosen line** with `tag`, `blockData`, and fresh timestamps.

### Victim selection — `selectVictim(int set)`

```java
// LRU: pick way with smallest lastUsed timestamp
// FIFO: pick way with smallest insertOrder timestamp
```

Both policies iterate all ways and find the one with the minimum relevant timestamp.

### `fillLine(CacheLine, tag, blockData)`

```java
private void fillLine(CacheLine line, int tag, int[] blockData) {
    line.valid = true;
    line.dirty = false;
    line.tag   = tag;
    System.arraycopy(blockData, 0, line.data, 0, blockSizeWords);
    line.lastUsed    = clock;
    line.insertOrder = clock;
    clock++;
}
```

Copies the block data and updates both timestamps (both are set to `clock` at insertion; `lastUsed` is subsequently updated on each hit, while `insertOrder` stays fixed).

---

## Inner Class — `EvictionResult`

```java
public static class EvictionResult {
    public final int   address;
    public final int[] data;

    public EvictionResult(int address, int[] data) {
        this.address = address;
        this.data    = data;
    }
}
```

Returned by `insert()` when a dirty line is evicted. Carries the **byte address** of the evicted block's start and a copy of its data words. `CacheHierarchy` uses this to propagate the write-back to the next cache level or main memory.

---

## Statistics Accessors

```java
public int getHits()           { return hits;   }
public int getMisses()         { return misses; }
public CacheConfig getConfig() { return config; }
public int getBlockSizeWords() { return blockSizeWords; }
```

`getHits()` and `getMisses()` are read by `Stats.collectCacheStats()` at the end of simulation.

---

## Summary Table

| Method | Visibility | Stats | Returns | Purpose |
|--------|-----------|-------|---------|---------|
| `lookup(addr)` | public | ✓ | `CacheLine` or null | Tag-match search |
| `readWord(addr)` | public | ✓ | `Integer` or null | Read a word through cache |
| `lookupNoStats(addr)` | package | ✗ | `CacheLine` or null | Internal search (block fill / write-back) |
| `readWordNoStats(addr)` | package | ✗ | `Integer` or null | Internal read |
| `writeWordNoStats(addr, val)` | package | ✗ | `boolean` | Internal write (write-back) |
| `insert(addr, data)` | public | ✗ | `EvictionResult` or null | Block installation with optional eviction |
| `getHits()` | public | — | `int` | Hit count for stats |
| `getMisses()` | public | — | `int` | Miss count for stats |

---

## Set-Associative Cache Address Layout

```
Address (32-bit):
┌────────────────────┬─────────────────┬──────────────────────┐
│      Tag bits      │  Set index bits  │  Block offset bits   │
│  (high-order)      │  (middle)        │  (low-order)         │
└────────────────────┴─────────────────┴──────────────────────┘

Example — L1I: 1024 B, 64 B blocks, 2-way:
  numSets = 1024 / (64 × 2) = 8
  blockSizeWords = 64 / 4 = 16 words
  offset bits = 4 bits (log2(16))
  set bits    = 3 bits (log2(8))
  tag bits    = 25 bits (32 - 4 - 3)
```

---

*End of Report*
