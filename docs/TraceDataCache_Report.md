# Study Report — `TraceDataCache.java`

> **File:** `src/trace/TraceDataCache.java`
> **Module:** Trace Replay — L1D-Only Data Cache
> **Date Generated:** 2026-05-12 *(Phase 3)*

---

## Overview

`TraceDataCache` provides a **simple L1 data cache** for trace replay mode. Unlike the full `CacheHierarchy` (which requires L1I, L1D, and L2), this class implements L1D-only with direct-to-memory miss handling.

**Key characteristics:**
- Uses `CacheLevel` internally for set-associative cache logic
- On miss: fetches block from `Memory`, installs into L1D, writes back dirty evictions
- Write-allocate, write-back policy
- No L2 level (per Phase 3 spec: "no L2 cache")

---

## Latency Model

| Event | Latency |
|-------|---------|
| L1D Hit | `L1D_LATENCY` (1 cycle) |
| L1D Miss | `L1D_LATENCY + MEMORY_LATENCY` (201 cycles) |

---

## Summary Table

| Method | Visibility | Return | Purpose |
|--------|-----------|--------|---------|
| `TraceDataCache(l1dCfg, memLatency, memory)` | `public` | — | Constructor |
| `read(address)` | `public` | `Result` | Read word (hit/miss handling) |
| `write(address, value)` | `public` | `Result` | Write word (write-allocate) |
| `getHits()` | `public` | `int` | L1D hit count |
| `getMisses()` | `public` | `int` | L1D miss count |

---

*End of Report*
