# Study Report — `ForwardingUnit.java`

> **File:** `src/hazard/ForwardingUnit.java`
> **Module:** Data forwarding (bypass) logic for the RISC-V pipeline simulator
> **Date Updated:** 2026-04-19 *(originally 2026-04-09)*

---

## Table of Contents

1. [Overview](#overview)
2. [Imports & Package Declaration](#imports--package-declaration)
3. [Class Declaration](#class-declaration)
4. [Method — `getForwardA()`](#getforwarda)
5. [Method — `getForwardB()`](#getforwardb)
6. [How EX vs MEM Forwarding Differs](#how-ex-vs-mem-forwarding-differs)
7. [Summary Table](#summary-table)
8. [Data-Flow Diagram](#data-flow-diagram)

---

## Overview

`ForwardingUnit` implements the **data-forwarding (bypass) network** of the pipeline. When an instruction in EX needs a value still being held in the MEM or WB pipeline register (not yet written to the register file), the forwarding unit routes the value directly from the pipeline register, eliminating most RAW stalls.

---

## Imports & Package Declaration

```java
package hazard;

import pipeline_registers.EX_MEM;
import pipeline_registers.ID_EX;
import pipeline_registers.MEM_WB;
```

| Import | Purpose |
|--------|---------|
| `EX_MEM` | Source for EX-to-EX forwarding (1 cycle old). |
| `ID_EX` | Current instruction in EX (consumer). |
| `MEM_WB` | Source for MEM-to-EX forwarding (2 cycles old). |

Note: `Opcode` is **not imported** — write-back checks are done via `opcode.writesBack()` and `opcode.isLoad()`, which are methods on the `Opcode` enum itself.

---

## Class Declaration

```java
// Handles data hazards via forwarding/bypassing.
public class ForwardingUnit {
```

A public, **stateless** class with two public methods. There are **no private helpers** — filtering logic is delegated to `Opcode` enum methods.

---

## `getForwardA()`

```java
public ForwardResult getForwardA(ID_EX idEx, EX_MEM exMem, MEM_WB memWb)
```

Determines whether operand A (`rs1`) of the instruction in EX needs to be forwarded.

### Priority 1 — Forward from EX/MEM (1 cycle old)

```java
if (!exMem.isNop && exMem.rd != 0
        && exMem.rd == idEx.rs1
        && exMem.opcode.writesBack() && !exMem.opcode.isLoad()) {
    return ForwardResult.FROM_EX_MEM;
}
```

| Condition | Reason |
|-----------|--------|
| `!exMem.isNop` | Real instruction in MEM |
| `exMem.rd != 0` | Not x0 (hardwired zero) |
| `exMem.rd == idEx.rs1` | Source matches destination |
| `exMem.opcode.writesBack()` | Instruction produces a register result |
| `!exMem.opcode.isLoad()` | **Load data not yet available here** — only the address was computed in EX |

If all pass → read `EX_MEM.aluResult`.

### Priority 2 — Forward from MEM/WB (2 cycles old)

```java
if (!memWb.isNop && memWb.rd != 0
        && memWb.rd == idEx.rs1
        && memWb.opcode.writesBack()) {
    return ForwardResult.FROM_MEM_WB;
}
```

No load exclusion here — by the time an instruction reaches MEM/WB, load data has been read from cache/memory and `MEM_WB.result` holds the final value.

If all pass → read `MEM_WB.result`.

### No hazard
```java
return ForwardResult.NONE;
```

Register file value is correct — no forwarding needed.

---

## `getForwardB()`

```java
public ForwardResult getForwardB(ID_EX idEx, EX_MEM exMem, MEM_WB memWb)
```

**Identical logic to `getForwardA()`**, but checks `idEx.rs2` instead of `idEx.rs1`. Same priority order: EX/MEM → MEM/WB → NONE.

---

## How EX vs MEM Forwarding Differs

| Scenario | EX/MEM forward? | MEM/WB forward? |
|----------|---------------|----------------|
| ALU result (ADD, ADDI, etc.) | ✅ `writesBack()` ✅ not load | ✅ |
| Load (`LW`, `LB`) | ❌ `isLoad()` prevents this | ✅ data available after MEM |
| Store (`SW`, `SB`) | ❌ `writesBack()` is false | ❌ |
| Branch, ECALL, HALT | ❌ `writesBack()` is false | ❌ |

This table explains why load-use hazards require a 1-cycle stall even with forwarding — the data cannot be forwarded from EX/MEM (address only), and the MEM/WB result isn't produced until the next cycle.

---

## Summary Table

| Method | Visibility | Return Type | Purpose |
|--------|-----------|-------------|---------|
| `getForwardA(ID_EX, EX_MEM, MEM_WB)` | `public` | `ForwardResult` | Check if `rs1` needs forwarding |
| `getForwardB(ID_EX, EX_MEM, MEM_WB)` | `public` | `ForwardResult` | Check if `rs2` needs forwarding |

---

## Data-Flow Diagram

```
  Instruction in EX stage (consumer)
  ┌──────────────────────┐
  │       ID/EX           │
  │   rs1        rs2      │
  └───┬──────────┬───────┘
      │          │
      ▼          ▼
 getForwardA  getForwardB
      │          │
      │  Compare rd fields:
      │  ┌─────────────────────────────────────────┐
      ├─►│  EX/MEM.rd == rs? && writesBack && !load │──► FROM_EX_MEM
      │  │  MEM/WB.rd == rs? && writesBack           │──► FROM_MEM_WB
      │  │  Neither                                   │──► NONE
      │  └─────────────────────────────────────────┘
      │
      ▼
  ForwardResult → EX_Stage.resolveOperandA/B()
```

---

*End of Report*
