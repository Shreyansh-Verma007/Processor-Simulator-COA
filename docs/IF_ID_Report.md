# Study Report — `IF_ID.java`

> **File:** `src/pipeline_registers/IF_ID.java`
> **Module:** Pipeline Registers — IF/ID
> **Date Generated:** 2026-05-14 *(Phase 1)*

---

## Overview

`IF_ID` is the **pipeline register between the Instruction Fetch and Instruction Decode** stages. It holds the fetched instruction, its program counter, and a cache miss stall counter.

---

## Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `instruction` | `Instruction` | `null` | The fetched instruction object |
| `pc` | `int` | `0` | Program counter of the fetched instruction |
| `isNop` | `boolean` | `true` | True if this register holds a pipeline bubble |
| `fetchLatencyLeft` | `int` | `0` | Remaining cycles for L1I cache miss stall |

---

*End of Report*
