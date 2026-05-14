# Study Report — `VirtualMemoryUnit.java`

> **File:** `src/vm/VirtualMemoryUnit.java`
> **Module:** Virtual Memory — Translation Orchestrator
> **Date Generated:** 2026-05-12 *(Phase 3)*

---

## Table of Contents

1. [Overview](#overview)
2. [Package & Imports](#package--imports)
3. [Class Fields](#class-fields)
4. [Constructor](#constructor)
5. [Method — `translateAddress()`](#translateaddress)
6. [Method — `allocateFrame()`](#allocateframe)
7. [Method — `evictPage()`](#evictpage)
8. [Translation Flow Diagram](#translation-flow-diagram)
9. [Latency Model](#latency-model)
10. [Statistics](#statistics)
11. [Summary Table](#summary-table)

---

## Overview

`VirtualMemoryUnit` is the **central orchestrator** for virtual-to-physical address translation. It coordinates:

1. **TLB lookup** — fast path for cached translations
2. **Page table walk** — on TLB miss, consults the flat page table
3. **Page fault handling** — on first access to unmapped pages, allocates frames
4. **Frame allocation** — manages a free frame pool
5. **Page replacement** — LRU/FIFO eviction when physical memory is full
6. **Dirty page tracking** — counts dirty evictions/writebacks

---

## Package & Imports

```java
package vm;
import common.Config;
import java.util.LinkedList;
import java.util.Queue;
```

---

## Class Fields

```java
private final TLB tlb;
private final PageTable pageTable;
private final Config cfg;
private final int numFrames;
private final Queue<Integer> freeFrames;
private final boolean useLRU;
private long clock = 0;

// Statistics
private int pageWalks, pageFaults, pageEvictions, dirtyEvictions;
private long totalTranslationPenalty;
```

| Field | Description |
|-------|-------------|
| `tlb` | The data TLB instance |
| `pageTable` | The flat page table |
| `numFrames` | Total physical frames = `physicalSizeBytes / pageSizeBytes` |
| `freeFrames` | Queue of available frame numbers |
| `useLRU` | Replacement policy flag |
| `clock` | Monotonic counter for LRU/FIFO timestamps |

---

## Constructor

```java
public VirtualMemoryUnit(Config cfg)
```

Initializes the TLB, page table, and free frame pool. The free frame list is populated with frame numbers 0 through `numFrames - 1`.

---

## `translateAddress()`

```java
public TranslationResult translateAddress(int virtualAddress, boolean isStore)
```

The main translation method. Steps:

1. **Extract VPN and offset** from the virtual address using `Integer.divideUnsigned()` for 32-bit correctness
2. **TLB lookup** — charge `tlb_hit_latency`
   - **Hit:** Update dirty bit if store, update LRU timestamp, return physical address
   - **Miss:** Proceed to step 3
3. **Page table walk** — charge `page_walk_latency`, increment `pageWalks`
   - **PTE valid:** Update LRU timestamp
   - **PTE invalid:** Trigger page fault (step 4)
4. **Page fault** — charge `page_fault_latency`, increment `pageFaults`, allocate frame
5. **Insert into TLB** — cache the new mapping
6. **Return** `TranslationResult(physicalAddress, totalLatency)`

---

## `allocateFrame()`

```java
private int allocateFrame()
```

Attempts to dequeue a free frame from the pool. If the pool is empty, calls `evictPage()` to free a frame.

---

## `evictPage()`

```java
private int evictPage()
```

Selects a victim page for eviction using the configured policy:

- **LRU:** Scans all valid PTEs, picks the one with smallest `lastUsed` timestamp
- **FIFO:** Scans all valid PTEs, picks the one with smallest `insertOrder` timestamp

On eviction:
1. Increments `pageEvictions`
2. Checks dirty bit (both PTE and TLB) — if dirty, increments `dirtyEvictions`
3. Invalidates the victim in both TLB and page table
4. Returns the freed frame number

---

## Translation Flow Diagram

```
Virtual Address
      │
      ▼
  ┌───────┐
  │  TLB  │──── Hit ──→ Physical Address (1 cycle)
  └───┬───┘
      │ Miss
      ▼
  ┌─────────────┐
  │ Page Table   │──── Valid ──→ Insert TLB → Physical Address (+10 cycles)
  └──────┬──────┘
         │ Invalid (Page Fault)
         ▼
  ┌──────────────┐
  │ Allocate     │──── Free frame? ──→ Map page, insert TLB (+50 cycles)
  │ Frame        │
  └──────┬───────┘
         │ No free frames
         ▼
  ┌──────────────┐
  │ Evict Page   │──→ Free frame, check dirty → Map page, insert TLB
  │ (LRU/FIFO)   │
  └──────────────┘
```

---

## Latency Model

| Scenario | Total Translation Latency |
|----------|--------------------------|
| TLB hit | `tlb_hit_latency` (1 cycle) |
| TLB miss, PT hit | `tlb_hit_latency + page_walk_latency` (11 cycles) |
| TLB miss, page fault | `tlb_hit_latency + page_walk_latency + page_fault_latency` (61 cycles) |

---

## Statistics

| Accessor | Returns |
|----------|---------|
| `getTlbHits()` | TLB hit count (from `TLB` instance) |
| `getTlbMisses()` | TLB miss count (from `TLB` instance) |
| `getPageWalks()` | Number of page table walks performed |
| `getPageFaults()` | Number of page faults (first access to unmapped page) |
| `getPageEvictions()` | Number of pages evicted from physical memory |
| `getDirtyEvictions()` | Number of dirty pages evicted (requiring writeback) |
| `getTotalTranslationPenalty()` | Cumulative translation latency cycles |

---

## Summary Table

| Method | Visibility | Return | Purpose |
|--------|-----------|--------|---------|
| `VirtualMemoryUnit(cfg)` | `public` | — | Constructor |
| `translateAddress(va, isStore)` | `public` | `TranslationResult` | Full translation pipeline |
| `allocateFrame()` | `private` | `int` | Get a free frame (or evict) |
| `evictPage()` | `private` | `int` | LRU/FIFO page replacement |

---

*End of Report*
