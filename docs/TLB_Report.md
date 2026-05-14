# Study Report — `TLB.java`

> **File:** `src/vm/TLB.java`
> **Module:** Virtual Memory — Data Translation Lookaside Buffer
> **Date Generated:** 2026-05-12 *(Phase 3)*

---

## Table of Contents

1. [Overview](#overview)
2. [Package Declaration](#package-declaration)
3. [Class Fields](#class-fields)
4. [Constructor](#constructor)
5. [Method — `lookup()`](#lookup)
6. [Method — `insert()`](#insert)
7. [Method — `markDirty()`](#markdirty)
8. [Method — `invalidate()`](#invalidate)
9. [Replacement Policy — `selectVictim()`](#selectvictim)
10. [Statistics](#statistics)
11. [Summary Table](#summary-table)

---

## Overview

`TLB` implements a **fully-associative data TLB** (Translation Lookaside Buffer) for the virtual memory subsystem. It caches recent virtual-to-physical page mappings to avoid expensive page table lookups on every memory access.

**Key characteristics:**
- Fully-associative (all entries checked on every lookup)
- Configurable number of entries (specified via `vm_config.txt`)
- Supports both **LRU** and **FIFO** replacement policies
- Tracks hits and misses for statistics

---

## Package Declaration

```java
package vm;
```

---

## Class Fields

```java
private final TLBEntry[] entries;
private final int numEntries;
private final boolean useLRU;      // true = LRU, false = FIFO
private long clock = 0;

// Statistics
private int hits = 0;
private int misses = 0;
```

| Field | Description |
|-------|-------------|
| `entries` | Array of `TLBEntry` objects — the TLB storage |
| `numEntries` | Number of TLB entries (from `dtlb_entries` in config) |
| `useLRU` | Whether to use LRU (true) or FIFO (false) for replacement |
| `clock` | Monotonically increasing counter for LRU/FIFO ordering |
| `hits`, `misses` | Access counters for statistics |

---

## Constructor

```java
public TLB(int numEntries, String replacementPolicy)
```

Creates a TLB with the specified number of entries. All entries start invalid. The replacement policy string is compared case-insensitively against "lru".

---

## `lookup()`

```java
public int lookup(int vpn)
```

Searches all entries for a matching virtual page number. Returns the physical frame number on hit (-1 on miss). On hit, updates the `lastUsed` timestamp for LRU tracking and increments the hit counter. On miss, increments the miss counter.

**Complexity:** O(n) where n = number of TLB entries (fully-associative scan).

---

## `insert()`

```java
public void insert(int vpn, int pfn, boolean dirty)
```

Inserts a new VPN→PFN mapping. The method first checks if the VPN already exists (update in-place), then looks for an empty slot, and finally evicts a victim using the configured replacement policy.

---

## `markDirty()`

```java
public void markDirty(int vpn)
```

Sets the dirty bit on the TLB entry for the given VPN. Called by `VirtualMemoryUnit` on store instructions to track which pages have been written to.

---

## `invalidate()`

```java
public void invalidate(int vpn)
```

Marks the TLB entry for the given VPN as invalid. Called when a page is evicted from physical memory to ensure stale mappings are not used.

---

## `selectVictim()`

```java
private int selectVictim()
```

Selects a victim entry for eviction:
- **LRU:** Selects the entry with the smallest `lastUsed` timestamp (least recently accessed).
- **FIFO:** Selects the entry with the smallest `insertOrder` timestamp (earliest inserted).

---

## Statistics

| Accessor | Returns |
|----------|---------|
| `getHits()` | Number of TLB hits |
| `getMisses()` | Number of TLB misses |

---

## Summary Table

| Method | Visibility | Return | Purpose |
|--------|-----------|--------|---------|
| `TLB(numEntries, policy)` | `public` | — | Constructor |
| `lookup(vpn)` | `public` | `int` | Look up VPN, return PFN or -1 |
| `insert(vpn, pfn, dirty)` | `public` | `void` | Insert/update mapping |
| `markDirty(vpn)` | `public` | `void` | Set dirty bit for store ops |
| `invalidate(vpn)` | `public` | `void` | Remove mapping on page eviction |
| `isDirty(vpn)` | `public` | `boolean` | Check if VPN is dirty in TLB |
| `getHits()` | `public` | `int` | Hit counter |
| `getMisses()` | `public` | `int` | Miss counter |

---

*End of Report*
