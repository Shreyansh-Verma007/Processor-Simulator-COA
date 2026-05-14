# Study Report — `TraceParser.java`

> **File:** `src/trace/TraceParser.java`
> **Module:** Trace Replay — Trace File Parser
> **Date Generated:** 2026-05-12 *(Phase 3)*

---

## Overview

`TraceParser` is a **static utility class** that reads a trace file and converts it into a list of `TraceInstruction` objects. Supports `L`, `S`, `ADD`, `MUL` instructions. Comments (`#`, `//`) and empty lines are ignored.

---

## Trace Format

```
L 0x10000000 x5      # Load word from VA into x5
S 0x10005000 x5      # Store x5 to VA
ADD x6 x5 x4         # x6 = x5 + x4
MUL x8 x6 x5         # x8 = x6 * x5
```

---

## Summary Table

| Method | Visibility | Return | Purpose |
|--------|-----------|--------|---------|
| `parse(path)` | `public static` | `List<TraceInstruction>` | Parse entire trace file |
| `parseLine(line, lineNum)` | `private static` | `TraceInstruction` | Parse one line |
| `parseAddress(s)` | `private static` | `int` | Parse hex/decimal address |
| `parseRegister(s)` | `private static` | `int` | Parse `x<N>` register |

---

*End of Report*
