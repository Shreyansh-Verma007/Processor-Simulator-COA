# Study Report — `Main.java`

> **File:** `src/Main.java`
> **Module:** Application Entry Point
> **Date Generated:** 2026-05-14 *(Phase 3)*

---

## Overview

`Main` is the **entry point** for the RISC-V pipeline simulator. It supports three execution modes via command-line arguments, routing to the appropriate simulation engine.

**Execution Modes:**
- **Pipeline Mode** (default): Compiles and runs a `.asm` file through the 5-stage pipeline (Phase 1/2)
- **Single Trace Mode** (`--trace`): Replays a single `.trace` file through the trace simulator (Phase 3)
- **Batch Trace Mode** (`--trace-all`): Replays all `.trace` files in a directory, writing individual results to the `traces_output/` directory

---

## Usage

```
Pipeline mode : java Main [input.asm]
Single trace  : java Main --trace <trace_file>
Batch traces  : java Main --trace-all <trace_dir>
```

---

## Output Files

| Mode | Output File | Contents |
|------|------------|----------|
| Pipeline | `console.txt` | Cycle-by-cycle pipeline output |
| Pipeline | `output.txt` | Final stats (cycles, stalls, IPC, cache) |
| Single Trace | `traces_output/*` | Trace replay stats (VM + cache + execution) |
| Batch Trace | `traces_output/*` | Individual stats for all trace files |

---

## Summary Table

| Method | Visibility | Return | Purpose |
|--------|-----------|--------|---------|
| `main(args)` | `public static` | `void` | Entry point — dispatches to correct mode |
| `runTraceMode(args)` | `private static` | `void` | Single trace replay execution |
| `runBatchTraceMode(args)` | `private static` | `void` | Batch trace replay over a directory |
| `runPipelineMode(args)` | `private static` | `void` | Full pipeline simulation (Phase 1/2) |

---

## Key Design Decisions

- **Unified Config:** All modes share the same `Config` class.
- **StatsPrinter delegation:** All formatted output goes through `StatsPrinter` — no inline print logic.
- **Pipeline output redirect:** `System.out` is redirected to `console.txt` in pipeline mode to separate cycle logs from stats.

---

*End of Report*
