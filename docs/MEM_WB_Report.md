# Study Report — `MEM_WB.java`

> **File:** `src/pipeline_registers/MEM_WB.java`
> **Module:** Pipeline Registers — MEM/WB
> **Date Generated:** 2026-05-14 *(Phase 1)*

---

## Overview

`MEM_WB` is the **pipeline register between Memory Access and Write Back** stages. It carries the final result value to be written into the destination register, along with a cache miss stall counter.

---

## Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `opcode` | `Opcode` | `null` | Instruction opcode |
| `rd` | `int` | `0` | Destination register index |
| `result` | `int` | `0` | Final value to write back (ALU result or loaded data) |
| `isNop` | `boolean` | `true` | True if this register holds a pipeline bubble |
| `memLatencyLeft` | `int` | `0` | Remaining cycles for L1D/L2 cache miss stall |

---

*End of Report*
