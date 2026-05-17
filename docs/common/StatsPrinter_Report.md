# Study Report — `StatsPrinter.java`

> **File:** `src/common/StatsPrinter.java`
> **Module:** Common — Centralized Output Formatting
> **Date Generated:** 2026-05-14 *(Phase 3)*

---

## Overview

`StatsPrinter` is a **centralized statistics and configuration printer** used by both pipeline mode and trace replay mode. It eliminates scattered print logic by providing a single class for all formatted output.

**Key characteristics:**
- All methods are `static` — no state, pure formatting
- Accepts a `PrintStream` parameter for flexible output targeting (console, file, etc.)
- Separates concerns: pipeline stats, trace stats, and config sections are independent methods

---

## Output Sections

### Pipeline Mode (`printPipelineStats`)
- Simulation stats: cycles, stalls, branch flushes, IPC
- Cache configuration: L1I, L1D, L2, memory latency, forwarding toggle
- Cache statistics: hits, misses, miss rates per level

### Trace Replay Mode (`printTraceStats`)
- Execution stats: total cycles, instructions retired, IPC, stalls
- Virtual memory stats: TLB hits/misses/hit rate, page walks, page faults, evictions, swap ins/outs, translation penalty cycles
- Cache statistics: L1D hits, misses, miss rate

### Shared (`printConfigSection`)
- VM configuration: virtual/physical memory sizes, page size, DTLB entries
- Latencies: TLB hit, page walk, page fault
- Replacement policy, forwarding, L1D cache config

---

## Summary Table

| Method | Visibility | Return | Purpose |
|--------|-----------|--------|---------|
| `printPipelineStats(out, stats, cfg)` | `public static` | `void` | Print Phase 1/2 pipeline stats |
| `printTraceHeader(out, tracePath, configPath, instrCount)` | `public static` | `void` | Print trace replay header |
| `printTraceStats(out, stats)` | `public static` | `void` | Print full trace simulation stats |
| `printConfigSection(out, cfg)` | `public static` | `void` | Print VM + cache configuration |

---

*End of Report*
