# Study Report — `ID_Stage.java`

> **File:** `src/pipeline_stages/ID_Stage.java`
> **Module:** Instruction Decode (ID) stage of a 5-stage RISC-V pipelined processor simulator
> **Date Updated:** 2026-04-19 *(originally 2026-04-09)*

---

## Table of Contents

1. [Overview](#overview)
2. [Imports & Package Declaration](#imports--package-declaration)
3. [Class Declaration](#class-declaration)
4. [Main Method — `tick()`](#main-method--tick)
5. [BTFNT Branch Prediction](#btfnt-branch-prediction)
6. [Summary Table](#summary-table)
7. [Data-Flow Diagram](#data-flow-diagram)

---

## Overview

`ID_Stage` models the **Instruction Decode stage** of a classic 5-stage RISC-V pipeline:

```
IF → **ID** → EX → MEM → WB
```

Its responsibilities are:
- **Decode the instruction** fetched by the IF stage — extract opcode, register indices, immediate, and PC.
- **Determine multi-cycle latency** for complex operations (e.g., MUL, DIV) from the simulator configuration.
- **Apply BTFNT static branch prediction** — backward branches are predicted taken; forward branches are predicted not taken.
- **Package everything** into the `ID_EX` pipeline register for the Execute stage.

> **Phase 2 change:** The ID stage no **longer reads the register file** (`rf` parameter was removed). Operand values (`valA`, `valB`) are now resolved by the EX stage using the forwarding unit. `ID_EX` carries only register *indices* (`rs1`, `rs2`), not values.

---

## Imports & Package Declaration

```java
package pipeline_stages;

import common.Config;
import common.Instruction;
import pipeline_registers.ID_EX;
import pipeline_registers.IF_ID;
```

| Import | Purpose |
|--------|---------|
| `Config` | Holds per-opcode latencies (`getLatency()`). |
| `Instruction` | Decoded instruction object with accessor methods (`opcode()`, `rd()`, `rs1()`, `rs2()`, `immediate()`). |
| `ID_EX` | Output pipeline register to the Execute stage. |
| `IF_ID` | Input pipeline register from the Fetch stage. |

Note: `RegisterFile` is **not** imported — operand reads have been moved to the EX stage.

---

## Class Declaration

```java
public class ID_Stage {
```

A public, **stateless** class — no instance fields.

---

## Main Method — `tick()`

### Signature

```java
public ID_EX tick(IF_ID ifId, Config cfg)
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `ifId` | `IF_ID` | Fetched instruction and PC from IF stage. |
| `cfg` | `Config` | Simulator configuration — used to look up instruction latency. |

**Returns:** A new `ID_EX` pipeline register.

### Walkthrough

```java
ID_EX out = new ID_EX();
if (ifId.isNop || ifId.instruction == null)
    return out;   // guard: emit bubble if nothing to decode
```

```java
Instruction instr = ifId.instruction;

out.isNop    = false;
out.opcode   = instr.opcode();
out.rd       = instr.rd();
out.rs1      = instr.rs1();
out.rs2      = instr.rs2();
out.immediate = instr.immediate();
out.pc       = ifId.pc;
```

**Multi-cycle latency:**
```java
int latency = cfg.getLatency(instr.opcode());
out.latencyCyclesLeft = (latency > 1) ? latency - 1 : 0;
```

The EX stage will decrement `latencyCyclesLeft` each cycle, emitting bubbles until it reaches 0.

| Op | Configured Latency | `latencyCyclesLeft` |
|----|-------------------|---------------------|
| `ADD`, `ADDI`, etc. | 1 | 0 |
| `MUL` | 3 | 2 |
| `DIV` | 4 | 3 |

---

## BTFNT Branch Prediction

BTFNT = **Backward Taken, Forward Not Taken** — a static branch prediction scheme.

```java
if (out.opcode.isBranch() && out.immediate < 0) {
    out.branchPredictedTaken = true;
    out.predictedPC = out.pc + out.immediate;
}
```

| Direction | Prediction | Action |
|-----------|-----------|--------|
| Backward branch (`immediate < 0`) | **TAKEN** | `predictedPC` = `pc + immediate`; PC redirected by `PipelineController` |
| Forward branch (`immediate >= 0`) | **NOT TAKEN** | No redirect; `branchPredictedTaken = false`, `predictedPC = -1` |

The EX stage checks whether the actual branch outcome matches `branchPredictedTaken`. If not, it sets `EX_MEM.branchMispredicted` and the pipeline controller flushes the wrong-path instructions.

---

## Summary Table

| Line | Code | Purpose |
|------|------|---------|
| `new ID_EX()` | Default NOP output | Safe return if no valid instruction |
| `isNop \|\| null` guard | Early return | Skip decoding for bubbles or end-of-program |
| `opcode, rd, rs1, rs2` | Decode fields | Extract instruction fields |
| `immediate` | Extract immediate | Branch offsets, load/store offsets, constants |
| `pc = ifId.pc` | Pass-through PC | Needed by EX for branch target and JAL link |
| `latencyCyclesLeft` | Multi-cycle support | Countdown for MUL/DIV |
| `branchPredictedTaken` | BTFNT prediction | Signal to EX for misprediction detection |
| `predictedPC` | BTFNT redirect | PC to use if backward branch predicted taken |

---

## Data-Flow Diagram

```
     ┌───────────────────────────────────┐
     │          IF/ID Register           │
     │   instruction, pc, isNop          │
     └──────────┬────────────────────────┘
                │
                ▼
     ┌───────────────────────────────────────────────┐
     │              ID_Stage.tick()                   │
     │                                                 │
     │  1. Guard: NOP / null check                    │
     │  2. Extract opcode, rd, rs1, rs2               │
     │  3. Extract immediate                           │
     │  4. Pass through PC                            │
     │  5. Compute latencyCyclesLeft (multi-cycle)    │
     │  6. BTFNT: backward branch? predict taken      │
     └──────────┬────────────────────────────────────┘
                │
                ▼
     ┌────────────────────────────────────────────────┐
     │              ID/EX Register                     │
     │  opcode, rd, rs1, rs2, immediate, pc,           │
     │  latencyCyclesLeft,                             │
     │  branchPredictedTaken, predictedPC              │
     └───────────────────────────────────────────────┘
                │
    ┌───────────┴───────────┐
    ▼                       ▼
 EX Stage               Hazard Unit
(ALU, forwarding,    (stall detection)
 BTFNT comparison)
```

---

*End of Report*
