# Study Report — `HazardUnit.java`

> **File:** `src/hazard/HazardUnit.java`
> **Module:** Hazard detection unit for the RISC-V pipeline simulator
> **Date Updated:** 2026-04-19 *(originally 2026-04-09)*

---

## Table of Contents

1. [Overview](#overview)
2. [Imports & Package Declaration](#imports--package-declaration)
3. [Class Declaration](#class-declaration)
4. [Main Method — `needsStall()`](#main-method--needsstall)
   - [Case 1: Multi-Cycle Op](#case-1-multi-cycle-op)
   - [Case 2: Load-Use Hazard](#case-2-load-use-hazard)
   - [Case 3 (EX): RAW Without Forwarding — producer in ID/EX](#case-3-ex-raw-without-forwarding--producer-in-idex)
   - [Case 4 (MEM): RAW Without Forwarding — producer in EX/MEM](#case-4-mem-raw-without-forwarding--producer-in-exmem)
5. [How `writesBack()` and `isLoad()` Work](#how-writesback-and-isload-work)
6. [Summary Table](#summary-table)
7. [Decision Flowchart](#decision-flowchart)

---

## Overview

`HazardUnit` is the pipeline's **stall controller**. Every cycle, `PipelineController` asks it: *"Does the pipeline need to stall this cycle?"*

The unit detects four scenarios:

| # | Hazard | When | Stall? |
|---|--------|------|--------|
| 1 | Multi-cycle op in EX | MUL/DIV counting down | Yes |
| 2 | Load-use | LW/LB in EX, consumer in ID | Yes — 1 cycle (even with forwarding) |
| 3 | RAW no-forward (EX) | Producer in ID/EX (rd == rs1/rs2), forwarding off | Yes |
| 4 | RAW no-forward (MEM) | Producer advanced to EX/MEM, forwarding off | Yes |

---

## Imports & Package Declaration

```java
package hazard;

import common.Config;
import pipeline_registers.EX_MEM;
import pipeline_registers.ID_EX;
import pipeline_registers.IF_ID;
```

| Import | Purpose |
|--------|---------|
| `Config` | `isForwardingEnabled()` flag. |
| `EX_MEM` | Pipeline register of the instruction currently in MEM — needed for Case 4. |
| `ID_EX` | Instruction in EX (potential producer). |
| `IF_ID` | Instruction in ID (potential consumer). |

Note: `Opcode` is **not** directly imported. `isLoad()` and `writesBack()` are called as methods on the `Opcode` enum value itself (e.g., `idEx.opcode.isLoad()`).

---

## Class Declaration

```java
// Detects hazards that require stalling (e.g., load-use, branch, or multi-cycle ops).
public class HazardUnit {
```

Public, stateless class — one public method, no fields.

---

## Main Method — `needsStall()`

### Signature

```java
public boolean needsStall(ID_EX idEx, IF_ID ifId, EX_MEM exMem, Config cfg)
```

| Parameter | Description |
|-----------|-------------|
| `idEx` | Instruction currently in EX (potential producer). |
| `ifId` | Instruction currently in ID (potential consumer). |
| `exMem` | Instruction currently in MEM — needed for no-forward case when producer has moved past EX. |
| `cfg` | Simulator settings — `isForwardingEnabled()`. |

**Returns:** `true` = stall needed; `false` = pipeline can proceed.

---

### Case 1: Multi-Cycle Op

```java
if (!idEx.isNop && idEx.latencyCyclesLeft > 0)
    return true;
```

A multi-cycle instruction (`MUL`, `DIV`) is still counting down in the EX stage. The pipeline freezes until it completes.

---

### Case 2: Load-Use Hazard

```java
if (!idEx.isNop && ifId.instruction != null) {
    if (idEx.opcode.isLoad()) {
        if (idEx.rd != 0 && (idEx.rd == incomingRs1 || idEx.rd == incomingRs2)) {
            return true;
        }
    }
}
```

The instruction in EX is a **load** (`LW` or `LB`) and the instruction in ID reads the loaded value. Even with forwarding, the data isn't available until EX/MEM is produced — always requires exactly one stall cycle.

`isLoad()` is a method on the `Opcode` enum: `return this == LW || this == LB;`

---

### Case 3 (EX): RAW Without Forwarding — producer in ID/EX

```java
if (!cfg.isForwardingEnabled()) {
    if (idEx.rd != 0 && idEx.opcode.writesBack()) {
        if (idEx.rd == incomingRs1 || idEx.rd == incomingRs2) {
            return true;
        }
    }
}
```

Forwarding is **disabled**. Any instruction producing a register value that the consumer in ID needs causes a stall. `writesBack()` is a method on `Opcode`: `return !isStore() && !isBranch() && this != ECALL && this != HALT;`

---

### Case 4 (MEM): RAW Without Forwarding — producer in EX/MEM

```java
if (!cfg.isForwardingEnabled()) {
    if (!exMem.isNop && exMem.rd != 0 && exMem.opcode.writesBack()) {
        if (exMem.rd == incomingRs1 || exMem.rd == incomingRs2) {
            return true;
        }
    }
}
```

The producer has advanced from EX to MEM (now in `EX_MEM`). Without forwarding, it still hasn't written to the register file, so the consumer in ID must still stall. This case must run even when `idEx` is a NOP bubble (inserted by a previous stall).

---

## How `writesBack()` and `isLoad()` Work

Both are **methods on the `Opcode` enum** (not private helpers in `HazardUnit`):

```java
// In Opcode.java:
public boolean isLoad()     { return this == LW || this == LB; }
public boolean writesBack() { return !isStore() && !isBranch()
                                     && this != ECALL && this != HALT; }
```

They are called as `idEx.opcode.isLoad()` and `idEx.opcode.writesBack()`.

---

## Summary Table

| Method | Visibility | Return | Purpose |
|--------|-----------|--------|---------|
| `needsStall(ID_EX, IF_ID, EX_MEM, Config)` | `public` | `boolean` | Decides if pipeline must stall |

---

## Decision Flowchart

```
                needsStall()
                    │
                    ▼
       Multi-cycle op in EX (latencyCyclesLeft > 0)?
                    │
               YES  │  NO
                ▼   │
          return true
                    │
                    ▼
       EX is a load AND ID uses rd?
                    │
               YES  │  NO
                ▼   │
          return true
                    │
                    ▼
       Forwarding disabled?
         ├── YES: EX producer (idEx) conflicts with ID consumer?
         │           └── YES → return true
         └── YES: MEM producer (exMem) conflicts with ID consumer?
                     └── YES → return true
                    │
                    ▼
              return false
```

---

*End of Report*
