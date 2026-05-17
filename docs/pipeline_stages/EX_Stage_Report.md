# Study Report — `EX_Stage.java`

> **File:** `src/pipeline_stages/EX_Stage.java`
> **Module:** Execute (EX) stage of a 5-stage RISC-V pipelined processor simulator
> **Date Updated:** 2026-04-19 *(originally 2026-04-09)*

---

## Table of Contents

1. [Overview](#overview)
2. [Imports & Package Declaration](#imports--package-declaration)
3. [Class Declaration & Fields](#class-declaration--fields)
4. [Constructor](#constructor)
5. [Main Method — `tick()`](#main-method--tick)
6. [Operand Resolution — `resolveOperandA()` / `resolveOperandB()`](#operand-resolution)
7. [ALU Computation — `computeALU()`](#computealu)
8. [Branch Resolution — `resolveBranch()`](#resolvebranch)
9. [Summary Table of Methods](#summary-table-of-methods)
10. [Data-Flow Diagram](#data-flow-diagram)

---

## Overview

`EX_Stage` models the **Execute stage** of a classic 5-stage RISC-V pipeline:

```
IF → ID → **EX** → MEM → WB
```

Its responsibilities are:
- **Resolve source operands** using data-forwarding (or the register file when forwarding is off).
- **Perform ALU operations** (arithmetic, logical, shifts, address computation).
- **Evaluate branch/jump conditions**, detect BTFNT mispredictions, and compute jump targets.
- **Handle multi-cycle (latency) instructions** by counting down remaining cycles.
- **Detect ECALL/HALT** to trigger pipeline drain.

---

## Imports & Package Declaration

```java
package pipeline_stages;

import common.Config;
import common.Opcode;
import core.RegisterFile;
import hazard.ForwardResult;
import hazard.ForwardingUnit;
import pipeline_registers.EX_MEM;
import pipeline_registers.ID_EX;
import pipeline_registers.MEM_WB;
```

| Import | Purpose |
|--------|---------|
| `Config` | Used to check `isForwardingEnabled()` — if false, operands are read from the register file directly. |
| `Opcode` | Enum of every supported instruction, plus utility methods (`isBranch()`, `writesBack()`, etc.). |
| `RegisterFile` | Fallback when forwarding cannot supply a value; also used for `ECALL` dump. |
| `ForwardResult` | Enum (`FROM_EX_MEM`, `FROM_MEM_WB`, `NONE`) indicating which pipeline register to forward from. |
| `ForwardingUnit` | Decides which forwarding path to use. |
| `ID_EX` | Input pipeline register from the Decode stage. |
| `EX_MEM` | Output pipeline register to the Memory stage. |
| `MEM_WB` | Used to read forwarded values for the 3-deep forwarding chain. |

---

## Class Declaration & Fields

```java
public class EX_Stage {
    public boolean haltFlag = false;
    private final RegisterFile rf;
    private final Config cfg;
```

| Field | Description |
|-------|-------------|
| `haltFlag` | Set to `true` when HALT or ECALL is encountered. Read by `PipelineController` to initiate drain. |
| `rf` | Reference to the shared register file — used as the final fallback for operand resolution and for `ECALL` dump. |
| `cfg` | Configuration — specifically `isForwardingEnabled()`. |

---

## Constructor

```java
public EX_Stage(RegisterFile rf, Config cfg) {
    this.rf  = rf;
    this.cfg = cfg;
}
```

Both `rf` and `cfg` are injected at construction time by `PipelineController`.

---

## Main Method — `tick()`

### Signature

```java
public EX_MEM tick(ID_EX idEx, EX_MEM prevExMem,
        MEM_WB newMemWb, MEM_WB oldMemWb,
        ForwardingUnit fu)
```

| Parameter | Description |
|-----------|-------------|
| `idEx` | The ID/EX pipeline register carrying the decoded instruction. |
| `prevExMem` | Previous cycle's EX/MEM register — used for EX-to-EX forwarding. |
| `newMemWb` | Newly computed MEM/WB register — used for MEM-to-EX forwarding. |
| `oldMemWb` | Retiring MEM/WB register — 3-deep forwarding catch before register file write. |
| `fu` | Forwarding unit instance. |

**Returns:** A new `EX_MEM` pipeline register.

### Walkthrough

```java
EX_MEM out = new EX_MEM();
if (idEx.isNop) return out;   // guard: nothing to execute
```

**Operand resolution:**
```java
int a, b;
if (cfg.isForwardingEnabled()) {
    a = resolveOperandA(idEx, prevExMem, newMemWb, oldMemWb, fu);
    b = resolveOperandB(idEx, prevExMem, newMemWb, oldMemWb, fu);
} else {
    a = rf.read(idEx.rs1);
    b = rf.read(idEx.rs2);
}
```

When forwarding is disabled, operands are read directly from the register file (data hazard stalls are managed by `HazardUnit` instead).

**Multi-cycle countdown:**
```java
if (idEx.latencyCyclesLeft > 0) {
    idEx.latencyCyclesLeft--;
    return out;   // emit bubble while counting down
}
```

**JAL return address override:**
```java
if (idEx.opcode == Opcode.JAL) {
    out.aluResult = idEx.pc + 4;  // return address stored as ALU result
}
```

**ECALL / HALT:**
```java
if (idEx.opcode == Opcode.ECALL) rf.dump();
if (idEx.opcode == Opcode.HALT)  haltFlag = true;
```

---

## Operand Resolution

### Forwarding Priority (highest → lowest)

| Priority | Source | EX_Stage reads from |
|----------|--------|---------------------|
| 1 | `EX/MEM` (1 cycle old) | `prevExMem.aluResult` |
| 2 | `MEM/WB` (2 cycles old) | `newMemWb.result` |
| 3 | Old `MEM/WB` (3 cycles old) | `oldMemWb.result` (manual check) |
| 4 | Register file | `rf.read(rs1/rs2)` |

The third-deep check is done manually (not via `ForwardingUnit`) by comparing `oldMemWb.rd` against the source register.

If `cfg.isForwardingEnabled()` is false, the entire forwarding chain is skipped and operands come from the register file.

---

## `computeALU()`

```java
private int computeALU(Opcode op, int a, int b, int imm)
```

Note: Unlike the old version, **PC is no longer a parameter** — JAL's return address (`pc + 4`) is computed outside this method using `idEx.pc + 4` directly in `tick()`.

### Instruction Breakdown

#### R-Type

| Opcode | Expression |
|--------|-----------|
| `ADD` | `a + b` |
| `SUB` | `a - b` |
| `MUL` | `a * b` |
| `DIV` | `(b != 0) ? a / b : -1` |
| `SLL` | `a << b` |
| `SRL` | `a >>> b` |
| `XOR` | `a ^ b` |
| `OR`  | `a \| b` |
| `AND` | `a & b` |

#### I-Type

| Opcode | Expression |
|--------|-----------|
| `ADDI` | `a + imm` |
| `LI`   | `imm` |

#### Load / Store (address calculation)

| Opcode | Expression |
|--------|-----------|
| `LW`, `LB`, `SW`, `SB` | `a + imm` |

#### Default
Returns `0` for branches, ECALL, HALT — these do not produce an ALU result.

---

## `resolveBranch()`

```java
private boolean resolveBranch(ID_EX idEx, int a, int b, EX_MEM out)
```

Evaluates branch/jump conditions and implements **BTFNT misprediction detection**:

```java
boolean taken = evaluateCondition(op, a, b);
if (taken) out.jumpTarget = pc + imm;

// Misprediction detection (conditional branches only; JAL has no prediction)
if (op.isBranch() && taken != idEx.branchPredictedTaken) {
    out.branchMispredicted = true;
    out.branchRecoveryPC = taken ? (pc + imm) : (pc + 4);
}
```

The `EX_MEM.branchMispredicted` flag is checked by `PipelineController` to decide whether to flush the pipeline and redirect the PC.

### Branch Conditions

| Opcode | Condition |
|--------|-----------|
| `BEQ` | `a == b` |
| `BNE` | `a != b` |
| `BLT` | `a < b` |
| `BGE` | `a >= b` |
| `JAL` | always `true` |

---

## Summary Table of Methods

| Method | Visibility | Return | Purpose |
|--------|-----------|--------|---------|
| `EX_Stage(RegisterFile, Config)` | `public` | — | Constructor |
| `tick(ID_EX, EX_MEM, MEM_WB, MEM_WB, ForwardingUnit)` | `public` | `EX_MEM` | Main per-cycle entry point |
| `resolveOperandA(…)` | `private` | `int` | Resolve `rs1` via forwarding chain |
| `resolveOperandB(…)` | `private` | `int` | Resolve `rs2` via forwarding chain |
| `computeALU(Opcode, int, int, int)` | `private` | `int` | Perform ALU computation |
| `resolveBranch(ID_EX, int, int, EX_MEM)` | `private` | `boolean` | Evaluate branch + BTFNT detection |
| `evaluateCondition(Opcode, int, int)` | `private` | `boolean` | Check BEQ/BNE/BLT/BGE/JAL condition |

---

## Data-Flow Diagram

```
        ┌─────────────────────────────────────┐
        │            ID/EX Register            │
        │  rs1, rs2, rd, opcode, imm, pc       │
        └──────┬────────────────┬─────────────┘
               │                │
       resolveA(forwarding)  resolveB(forwarding)
               │                │
               ▼                ▼
           ┌───────┐        ┌───────┐
           │  a    │        │   b   │
           └───┬───┘        └───┬───┘
               │                │
               ▼                ▼
         ┌───────────────────────┐
         │     computeALU()      │──► aluResult
         └───────────────────────┘
                      │
                      ▼
         ┌───────────────────────┐
         │   resolveBranch()     │──► branchTaken, jumpTarget
         │   (+ BTFNT detect)    │──► branchMispredicted, branchRecoveryPC
         └───────────────────────┘
                      │
                      ▼
        ┌─────────────────────────────────────┐
        │           EX/MEM Register            │
        │  aluResult, branchTaken, jumpTarget, │
        │  branchMispredicted, branchRecoveryPC│
        │  writeData, rd, opcode               │
        └─────────────────────────────────────┘
```

---

*End of Report*
