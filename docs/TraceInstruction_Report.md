# Study Report — `TraceInstruction.java`

> **File:** `src/trace/TraceInstruction.java`
> **Module:** Trace Replay — Instruction Model
> **Date Generated:** 2026-05-14 *(Phase 3)*

---

## Overview

`TraceInstruction` represents a **single instruction parsed from a trace file**. Unlike the pipeline's `Instruction` record, this class models the simplified Phase 3 trace format which supports only four operation types: `LOAD`, `STORE`, `ADD`, and `MUL`.

---

## Trace File Format

```
L  0xADDR  xRD        — load word from virtual address into register
S  0xADDR  xRS        — store word from register to virtual address
ADD xRD xRS1 xRS2     — rd = rs1 + rs2
MUL xRD xRS1 xRS2     — rd = rs1 * rs2
```

---

## Fields

| Field | Type | Used By | Description |
|-------|------|---------|-------------|
| `type` | `Type` enum | All | `LOAD`, `STORE`, `ADD`, or `MUL` |
| `address` | `int` | L, S | Virtual address (0 for ALU ops) |
| `rd` | `int` | L, ADD, MUL | Destination register |
| `rs1` | `int` | S, ADD, MUL | Source register 1 |
| `rs2` | `int` | ADD, MUL | Source register 2 |

---

## Factory Methods

| Method | Type | Parameters |
|--------|------|-----------|
| `load(address, rd)` | `LOAD` | Virtual address, destination register |
| `store(address, rs)` | `STORE` | Virtual address, source register |
| `add(rd, rs1, rs2)` | `ADD` | Destination and two source registers |
| `mul(rd, rs1, rs2)` | `MUL` | Destination and two source registers |

---

*End of Report*
