# Study Report — `PageTable.java`

> **File:** `src/vm/PageTable.java`
> **Module:** Virtual Memory — Flat Single-Level Page Table
> **Date Generated:** 2026-05-12 *(Phase 3)*

---

## Table of Contents

1. [Overview](#overview)
2. [Package Declaration](#package-declaration)
3. [Class Fields](#class-fields)
4. [Constructor](#constructor)
5. [Method — `lookup()`](#lookup)
6. [Method — `mapPage()`](#mappage)
7. [Method — `unmapPage()`](#unmappage)
8. [Method — `findVPNByFrame()`](#findvpnbyframe)
9. [Summary Table](#summary-table)
10. [Page Table Layout Diagram](#page-table-layout-diagram)

---

## Overview

`PageTable` implements a **flat (single-level) page table** indexed by virtual page number (VPN). It maps virtual pages to physical frames and is used by `VirtualMemoryUnit` during address translation when a TLB miss occurs.

**Key characteristics:**
- Array-based, O(1) lookup by VPN
- Sized to `virtualSizeBytes / pageSizeBytes` entries
- Each entry stores: valid bit, frame number, dirty bit, LRU/FIFO timestamps

---

## Package Declaration

```java
package vm;
```

---

## Class Fields

```java
private final PageTableEntry[] entries;
private final int numPages;
```

| Field | Description |
|-------|-------------|
| `entries` | Array of `PageTableEntry`, indexed by VPN |
| `numPages` | Total number of virtual pages = `virtualSizeBytes / pageSizeBytes` |

---

## Constructor

```java
public PageTable(int virtualSizeBytes, int pageSizeBytes)
```

Creates a page table sized for the virtual address space. All entries start invalid (no pages mapped).

**Example:** 512 MB virtual / 4 KB pages = 131,072 entries.

---

## `lookup()`

```java
public PageTableEntry lookup(int vpn)
```

Returns the `PageTableEntry` for the given VPN. The caller must check `pte.valid` to determine if the page is mapped. Throws `IllegalArgumentException` if VPN is out of range.

---

## `mapPage()`

```java
public void mapPage(int vpn, int frameNumber, long timestamp)
```

Creates a mapping from VPN to a physical frame. Sets `valid = true`, `dirty = false`, and records the timestamp for replacement tracking.

---

## `unmapPage()`

```java
public void unmapPage(int vpn)
```

Removes the mapping for a VPN. Sets `valid = false` and `frameNumber = -1`. Called when a page is evicted from physical memory.

---

## `findVPNByFrame()`

```java
public int findVPNByFrame(int frameNumber)
```

Reverse lookup: finds which VPN is currently mapped to the given frame. Returns -1 if no mapping exists. Used during page eviction to identify the victim page.

**Complexity:** O(n) scan through all entries.

---

## Summary Table

| Method | Visibility | Return | Purpose |
|--------|-----------|--------|---------|
| `PageTable(virtualSize, pageSize)` | `public` | — | Constructor |
| `lookup(vpn)` | `public` | `PageTableEntry` | Get PTE for VPN |
| `mapPage(vpn, frame, ts)` | `public` | `void` | Create VPN→frame mapping |
| `unmapPage(vpn)` | `public` | `void` | Remove mapping |
| `findVPNByFrame(frame)` | `public` | `int` | Reverse lookup: frame→VPN |
| `getNumPages()` | `public` | `int` | Total virtual page count |

---

## Page Table Layout Diagram

```
VPN     PageTableEntry
  0  →  [ valid=false, frame=-1, dirty=false ]
  1  →  [ valid=true,  frame=3,  dirty=false ]   ← mapped to frame 3
  2  →  [ valid=true,  frame=0,  dirty=true  ]   ← dirty (was written)
  3  →  [ valid=false, frame=-1, dirty=false ]
  ...
  N-1 → [ valid=false, frame=-1, dirty=false ]

N = virtualSizeBytes / pageSizeBytes
```

---

*End of Report*
