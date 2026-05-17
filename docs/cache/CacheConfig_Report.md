# Study Report — `CacheConfig.java`

> **File:** `src/cache/CacheConfig.java`
> **Module:** Cache – immutable configuration for a single cache level
> **Date Generated:** 2026-04-19

---

## Table of Contents

1. [Overview](#overview)
2. [Package Declaration](#package-declaration)
3. [Enum — `ReplacementPolicy`](#enum--replacementpolicy)
4. [Class Declaration & Fields](#class-declaration--fields)
5. [Constructor & Validation](#constructor--validation)
6. [Method — `getNumSets()`](#method--getnumsets)
7. [Method — `toString()`](#method--tostring)
8. [Summary Table](#summary-table)
9. [Default Values (from `Config.java`)](#default-values-from-configjava)

---

## Overview

`CacheConfig` is an **immutable configuration descriptor** for a single level of cache (L1I, L1D, or L2). It holds all geometric and timing parameters and validates them at construction time. `CacheConfig` objects are created by `Config.java` and passed to `CacheLevel` instances inside `CacheHierarchy`.

---

## Package Declaration

```java
package cache;
```

---

## Enum — `ReplacementPolicy`

```java
public enum ReplacementPolicy {
    LRU, FIFO
}
```

| Value | Strategy |
|-------|----------|
| `LRU` | Least Recently Used — evict the line that was accessed least recently (tracked by `CacheLine.lastUsed`). |
| `FIFO` | First In, First Out — evict the line that was inserted earliest (tracked by `CacheLine.insertOrder`). |

---

## Class Declaration & Fields

```java
public class CacheConfig {
    public final int size;          // total cache size in bytes
    public final int blockSize;     // block (line) size in bytes
    public final int associativity; // number of ways per set
    public final int latency;       // access latency in cycles (on hit)
    public final ReplacementPolicy policy;
    ...
}
```

All fields are `public final` — the object is fully immutable after construction.

| Field | Type | Description |
|-------|------|-------------|
| `size` | `int` | Total capacity of this cache level in **bytes** (e.g., 1024 for 1 KB). |
| `blockSize` | `int` | Size of one cache block in **bytes** (e.g., 64 bytes = 16 words). |
| `associativity` | `int` | Number of ways per set (1 = direct-mapped, >1 = set-associative). |
| `latency` | `int` | Hit latency in **cycles** — additional cache miss cycles are computed separately. |
| `policy` | `ReplacementPolicy` | Replacement policy for eviction decisions (`LRU` or `FIFO`). |

---

## Constructor & Validation

```java
public CacheConfig(int size, int blockSize, int associativity,
        int latency, ReplacementPolicy policy) {
```

**Validation rules** (throws `IllegalArgumentException` on failure):
1. All of `size`, `blockSize`, `associativity`, `latency` must be **positive** (> 0).
2. `size` must be **exactly divisible** by `blockSize × associativity` — this ensures an integer number of sets.

```
numSets = size / (blockSize * associativity)
```

If either check fails, a descriptive message is thrown before any fields are assigned.

---

## Method — `getNumSets()`

```java
public int getNumSets() {
    return size / (blockSize * associativity);
}
```

Computes the number of sets in this cache level.

**Example:** 1024 B cache, 64 B blocks, 2-way → `1024 / (64 × 2)` = **8 sets**.

---

## Method — `toString()`

```java
@Override
public String toString() {
    return size + "B, " + blockSize + "B blocks, " + associativity
            + "-way, " + latency + "-cycle, " + policy;
}
```

Returns a human-readable summary. Used by `Main.java` to print the cache configuration to `output.txt`.

**Example:** `"1024B, 64B blocks, 2-way, 5-cycle, LRU"`

---

## Summary Table

| Method | Return | Description |
|--------|--------|-------------|
| `CacheConfig(...)` | — | Constructor with parameter validation |
| `getNumSets()` | `int` | Number of sets = size / (blockSize × assoc) |
| `toString()` | `String` | Human-readable one-line summary |

---

## Default Values (from `Config.java`)

| Parameter | L1I | L1D | L2 |
|-----------|-----|-----|----|
| `size` | 1024 B | 1024 B | 8192 B |
| `blockSize` | 64 B | 64 B | 64 B |
| `associativity` | 2-way | 2-way | 4-way |
| `latency` | 5 cycles | 5 cycles | 50 cycles |
| `policy` | LRU | LRU | LRU |

Main memory latency (not a cache level): **200 cycles**.

---

*End of Report*
