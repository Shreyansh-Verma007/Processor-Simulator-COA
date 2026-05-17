# Study Report — `PageTableEntry.java`

> **File:** `src/vm/PageTableEntry.java`
> **Module:** Virtual Memory — Page Table Entry
> **Date Generated:** 2026-05-14 *(Phase 3)*

---

## Overview

`PageTableEntry` represents a **single entry in the flat page table**, mapping a virtual page to a physical frame. It includes metadata for validity, dirty tracking, and replacement policy ordering.

---

## Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `valid` | `boolean` | `false` | Whether this page is currently mapped to a frame |
| `frameNumber` | `int` | `-1` | Physical frame number (-1 if unmapped) |
| `dirty` | `boolean` | `false` | True if page has been written to since last load |
| `lastUsed` | `long` | `0` | Timestamp of last access (used by LRU replacement) |
| `insertOrder` | `long` | `0` | Timestamp of insertion (used by FIFO replacement) |

---

*End of Report*
