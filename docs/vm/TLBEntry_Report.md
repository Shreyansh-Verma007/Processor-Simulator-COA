# Study Report — `TLBEntry.java`

> **File:** `src/vm/TLBEntry.java`
> **Module:** Virtual Memory — TLB Entry
> **Date Generated:** 2026-05-14 *(Phase 3)*

---

## Overview

`TLBEntry` represents a **single TLB entry** mapping a virtual page number to a physical frame number. It is used by the fully-associative `TLB` class and includes metadata for dirty tracking and replacement policy ordering.

---

## Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `valid` | `boolean` | `false` | Whether this TLB entry is currently active |
| `virtualPageNumber` | `int` | `-1` | The virtual page number (tag) |
| `physicalFrameNumber` | `int` | `-1` | The mapped physical frame number |
| `dirty` | `boolean` | `false` | True if any store has been performed through this mapping |
| `lastUsed` | `long` | `0` | Timestamp of last access (LRU replacement) |
| `insertOrder` | `long` | `0` | Timestamp of insertion (FIFO replacement) |

---

*End of Report*
