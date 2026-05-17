# Study Report — `EX_MEM.java`

> **File:** `src/pipeline_registers/EX_MEM.java`
> **Module:** Pipeline Registers — EX/MEM
> **Date Generated:** 2026-05-14 *(Phase 1)*

---

## Overview

`EX_MEM` is the **pipeline register between Execute and Memory Access** stages. It carries ALU results, branch resolution signals, and BTFNT misprediction data to the MEM stage.

---

## Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `opcode` | `Opcode` | `null` | Instruction opcode |
| `rd` | `int` | `0` | Destination register index |
| `aluResult` | `int` | `0` | ALU computation output |
| `writeData` | `int` | `0` | rs2 value for store instructions |
| `isNop` | `boolean` | `true` | True if this register holds a pipeline bubble |
| `branchTaken` | `boolean` | `false` | Whether branch was actually taken (resolved by EX) |
| `jumpTarget` | `int` | `0` | Computed branch/jump target address |
| `branchMispredicted` | `boolean` | `false` | BTFNT: misprediction detected by EX |
| `branchRecoveryPC` | `int` | `0` | Correct PC to recover to on misprediction |

---

*End of Report*
