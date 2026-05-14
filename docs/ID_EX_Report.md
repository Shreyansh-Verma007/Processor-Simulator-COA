# Study Report — `ID_EX.java`

> **File:** `src/pipeline_registers/ID_EX.java`
> **Module:** Pipeline Registers — ID/EX
> **Date Generated:** 2026-05-14 *(Phase 1)*

---

## Overview

`ID_EX` is the **pipeline register between Instruction Decode and Execute** stages. It carries decoded instruction fields, multi-cycle execution support, and BTFNT branch prediction data to the EX stage.

---

## Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `opcode` | `Opcode` | `null` | Decoded opcode |
| `rd` | `int` | `0` | Destination register index |
| `rs1` | `int` | `0` | Source register 1 value (read from register file) |
| `rs2` | `int` | `0` | Source register 2 value (read from register file) |
| `immediate` | `int` | `0` | Immediate/offset value |
| `pc` | `int` | `0` | Program counter of the instruction |
| `latencyCyclesLeft` | `int` | `0` | Remaining cycles for multi-cycle execution |
| `isNop` | `boolean` | `true` | True if this register holds a pipeline bubble |
| `branchPredictedTaken` | `boolean` | `false` | BTFNT prediction: was branch predicted taken? |
| `predictedPC` | `int` | `-1` | Predicted target PC (-1 = no prediction redirect) |

---

*End of Report*
