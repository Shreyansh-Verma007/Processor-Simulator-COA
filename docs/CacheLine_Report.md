# Study Report — `CacheLine.java`

> **File:** `src/cache/CacheLine.java`
> **Module:** Cache – single cache line (block) within a set
> **Date Generated:** 2026-04-19

---

## Table of Contents

1. [Overview](#overview)
2. [Package Declaration](#package-declaration)
3. [Class Declaration & Fields](#class-declaration--fields)
4. [Constructor](#constructor)
5. [Summary Table](#summary-table)

---

## Overview

`CacheLine` is the fundamental data unit stored inside every set of a `CacheLevel`.
Each instance represents **one block (line)** of cached memory, consisting of:
- **Control bits** (`valid`, `dirty`)
- **Tag** — the high-order address bits used to identify which memory block is stored here
- **Data array** — the raw word values in this block
- **Replacement-policy timestamps** — `lastUsed` (LRU) and `insertOrder` (FIFO)

A newly created `CacheLine` is **invalid** by default (no real data, `valid = false`).

---

## Package Declaration

```java
package cache;
```

---

## Class Declaration & Fields

```java
public class CacheLine {
    public boolean valid = false;
    public boolean dirty = false;
    public int tag = -1;
    public int[] data;          // block data in words (one int per 4 bytes)

    // Replacement policy metadata
    public long lastUsed    = 0; // LRU: timestamp of last access
    public long insertOrder = 0; // FIFO: timestamp of insertion
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `valid` | `boolean` | `false` | `true` when the line holds real (cached) data. |
| `dirty` | `boolean` | `false` | `true` when the line has been written but not yet flushed to the next level (write-back). |
| `tag` | `int` | `-1` | High-order address bits used for tag comparison during lookup. |
| `data` | `int[]` | allocated | Array of `blockSizeWords` words — the cached block content. |
| `lastUsed` | `long` | `0` | Timestamp of the most recent access; used by LRU replacement. |
| `insertOrder` | `long` | `0` | Timestamp of insertion into the cache; used by FIFO replacement. |

---

## Constructor

```java
public CacheLine(int blockSizeWords) {
    this.data = new int[blockSizeWords];
}
```

Allocates the `data` array with `blockSizeWords` entries (one `int` per 4-byte word). All other fields keep their default values (`valid = false`, `dirty = false`, `tag = -1`).

---

## Summary Table

| Field / Member | Role |
|----------------|------|
| `valid` | Guards all cache lookups — invalid lines are invisible to the rest of the cache |
| `dirty` | Determines whether an eviction must propagate data downward |
| `tag` | Matched against the tag bits of an incoming address |
| `data[]` | Stores the actual word data for the block |
| `lastUsed` | Updated on every access for LRU ordering |
| `insertOrder` | Set once at insertion for FIFO ordering |

---

*End of Report*
