# Study Report — `WB_Stage.java`

> **File:** `src/pipeline_stages/WB_Stage.java`
> **Module:** Write-Back (WB) stage of a 5-stage RISC-V pipelined processor simulator
> **Date Updated:** 2026-04-19 *(originally 2026-04-09)*

---

## Table of Contents

1. [Overview](#overview)
2. [Imports & Package Declaration](#imports--package-declaration)
3. [Class Declaration](#class-declaration)
4. [Main Method — `tick()`](#main-method--tick)
5. [How Write-Back Decision Works](#how-write-back-decision-works)
6. [Summary Table](#summary-table)
7. [Data-Flow Diagram](#data-flow-diagram)

---

## Overview

`WB_Stage` models the **Write-Back stage** — the final stage of the RISC-V pipeline:

```
IF → ID → EX → MEM → **WB**
```

Its responsibilities are:
- **Write the instruction's result** back to the destination register in the register file.
- **Count retired instructions** by incrementing the statistics counter.
- **Suppress writes** for instructions that do not produce a register result (stores, branches, ECALL, HALT).

---

## Imports & Package Declaration

```java
package pipeline_stages;

import core.RegisterFile;
import core.Stats;
import pipeline_registers.MEM_WB;
```

| Import | Purpose |
|--------|---------|
| `RegisterFile` | Written to here (WB stage is the sole writer). |
| `Stats` | `instructionsRetired` is incremented here. |
| `MEM_WB` | Input pipeline register carrying the final result from MEM stage. |

Note: `Opcode` is **not imported** — the write-back decision is made via `memWb.opcode.writesBack()`, a method on the `Opcode` enum.

---

## Class Declaration

```java
// Write Back (WB)
public class WB_Stage {
```

Public, **stateless** class — no instance fields, **no private helpers**.

---

## Main Method — `tick()`

### Signature

```java
public void tick(MEM_WB memWb, RegisterFile rf, Stats stats)
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `memWb` | `MEM_WB` | Result, opcode, destination register, and NOP flag from MEM stage. |
| `rf` | `RegisterFile` | The shared register file to write the result into. |
| `stats` | `Stats` | Performance counters — `instructionsRetired` incremented here. |

**Returns:** `void` — end of pipeline.

### Walkthrough

```java
if (memWb.isNop)
    return;   // guard: skip bubbles
```

```java
// Only write back for instructions that produce a register result
if (memWb.rd != 0 && memWb.opcode.writesBack()) {
    rf.write(memWb.rd, memWb.result);
}
stats.instructionsRetired++;
```

Two conditions must hold for a register write:

| Condition | Reason |
|-----------|--------|
| `memWb.rd != 0` | RISC-V hardwires `x0` to zero — writes are silently discarded |
| `memWb.opcode.writesBack()` | Only opcodes that produce a register result are written back |

`stats.instructionsRetired++` counts every non-NOP instruction reaching WB, including stores and branches (they complete here even though they don't write a register).

---

## How Write-Back Decision Works

The check `memWb.opcode.writesBack()` calls the `writesBack()` method defined directly on the `Opcode` enum:

```java
// In Opcode.java:
public boolean writesBack() {
    return !isStore() && !isBranch() && this != ECALL && this != HALT;
}
```

| Category | `writesBack()` | Opcodes |
|----------|---------------|---------|
| Arithmetic / logical | ✅ `true` | ADD, SUB, MUL, DIV, SLL, SRL, XOR, OR, AND |
| Immediate ops | ✅ `true` | ADDI, LI |
| Loads | ✅ `true` | LW, LB |
| Jump | ✅ `true` | JAL (stores return address) |
| Stores | ❌ `false` | SW, SB |
| Branches | ❌ `false` | BEQ, BNE, BLT, BGE |
| System | ❌ `false` | ECALL, HALT |

---

## Summary Table

| Line | Code | Purpose |
|------|------|---------|
| `if (isNop) return` | Guard | Skip pipeline bubbles |
| `rd != 0 && opcode.writesBack()` | Conditional | Check if register write is needed |
| `rf.write(rd, result)` | Write | Commit result to register file |
| `stats.instructionsRetired++` | Count | Track completed instructions (for IPC) |

---

## Data-Flow Diagram

```
     ┌─────────────────────────────────────┐
     │          MEM/WB Register             │
     │     result, opcode, rd, isNop        │
     └──────────┬──────────────────────────┘
                │
                ▼
     ┌─────────────────────────────────────┐
     │        WB_Stage.tick()               │
     │                                      │
     │  1. NOP guard                        │
     │  2. opcode.writesBack() && rd ≠ 0?   │
     │     YES → rf.write(rd, result)       │
     │  3. stats.instructionsRetired++      │
     └──────────┬──────────────────────────┘
                │
       ┌────────┴────────┐
       ▼                 ▼
  Register File       Stats
  (x1 – x31)      (IPC tracking)
```

---

*End of Report*
