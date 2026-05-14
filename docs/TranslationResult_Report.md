# Study Report — `TranslationResult.java`

> **File:** `src/vm/TranslationResult.java`
> **Module:** Virtual Memory — Translation Output
> **Date Generated:** 2026-05-14 *(Phase 3)*

---

## Overview

`TranslationResult` is an **immutable data class** that encapsulates the result of a virtual-to-physical address translation performed by the `VirtualMemoryUnit`. It contains both the translated physical address and the total latency penalty incurred during translation.

---

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `physicalAddress` | `int` | The translated physical memory address |
| `latencyCycles` | `int` | Total latency (TLB hit/miss + page walk + page fault penalty) |

---

## Latency Breakdown

The `latencyCycles` value may include any combination of:

| Scenario | Latency Components |
|----------|-------------------|
| TLB Hit | `TLB_HIT_LATENCY` only |
| TLB Miss, Page Table Hit | `TLB_HIT_LATENCY + PAGE_WALK_LATENCY` |
| TLB Miss, Page Fault | `TLB_HIT_LATENCY + PAGE_WALK_LATENCY + PAGE_FAULT_LATENCY` |

---

*End of Report*
