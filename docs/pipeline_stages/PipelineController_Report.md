# Study Report — `PipelineController.java`

> **File:** `src/pipeline_stages/PipelineController.java`
> **Module:** Pipeline orchestrator for a 5-stage RISC-V pipelined processor simulator
> **Date Updated:** 2026-04-19 *(originally 2026-04-09)*

---

## Table of Contents

1. [Overview](#overview)
2. [Imports & Package Declaration](#imports--package-declaration)
3. [Class Declaration](#class-declaration)
4. [Constants](#constants)
5. [Main Method — `run()`](#main-method--run)
   - [Signature & Parameters](#signature--parameters)
   - [Initialisation](#initialisation)
   - [Simulation Loop](#simulation-loop)
     - [Phase 0: Cache Stall Drain](#phase-0-cache-stall-drain)
     - [Phase 1: Hazard Detection](#phase-1-hazard-detection)
     - [Phase 2: WB, MEM, EX Stages](#phase-2-wb-mem-ex-stages)
     - [Phase 3: MEM Cache Stall Check](#phase-3-mem-cache-stall-check)
     - [Phase 4: Branch / Stall Resolution](#phase-4-branch--stall-resolution)
     - [Phase 5: Cycle Counting & Termination](#phase-5-cycle-counting--termination)
6. [Cache Stall Priority](#cache-stall-priority)
7. [Summary Table](#summary-table)
8. [Pipeline Timing Diagram](#pipeline-timing-diagram)
9. [Data-Flow Diagram](#data-flow-diagram)

---

## Overview

`PipelineController` is the **central orchestrator** of the simulator. It:

1. **Instantiates** all five pipeline stages, hazard/forwarding units, and (optionally) loads the program into memory for cache-based fetch.
2. **Runs a cycle-accurate simulation loop** that, on every clock cycle:
   - Drains cache stall cycles (MEM priority over IF).
   - Detects data hazards / multi-cycle stalls.
   - Executes stages in **reverse order** (WB → MEM → EX) to correctly model latch behaviour.
   - Handles **BTFNT branch mispredictions** and **JAL unconditional jumps** with pipeline flushes.
   - Tracks statistics (cycles, stalls, flushes, retired instructions).
3. **Collects cache stats** from `CacheHierarchy` after the loop exits.
4. **Drains the pipeline** after a HALT instruction before terminating.

**Phase 2 update:** A `CacheHierarchy` parameter is now accepted. When non-null, the program is first encoded into `Memory` (so the instruction cache can fetch real binary words), and both IF and MEM stages receive the cache object for variable-latency access. Two stall counters (`ifStallCycles`, `memStallCycles`) handle cache-miss cycles before and separately from data-hazard stalls.

---

## Imports & Package Declaration

```java
package pipeline_stages;

import cache.CacheHierarchy;
import common.Config;
import common.Instruction;
import common.Opcode;
import core.Memory;
import core.RegisterFile;
import core.Stats;
import hazard.ForwardingUnit;
import hazard.HazardUnit;
import pipeline_registers.EX_MEM;
import pipeline_registers.ID_EX;
import pipeline_registers.IF_ID;
import pipeline_registers.MEM_WB;
import java.util.List;
```

| Import | Purpose |
|--------|---------|
| `CacheHierarchy` | Two-level cache (L1I, L1D, L2 → Memory); `null` = Phase 1 mode. |
| `Config` | Instruction latencies, forwarding flag, cache configuration. |
| `Instruction` | Decoded RISC-V instruction. |
| `Opcode` | Used to detect JAL for unconditional-jump flush. |
| `Memory` | Simulated RAM — also backing store for cache hierarchy. |
| `RegisterFile` | 32 × 32-bit register file. |
| `Stats` | Performance metrics accumulator. |
| `ForwardingUnit` | Resolves forwarding paths in EX stage. |
| `HazardUnit` | Detects data hazards that require stalling. |
| `EX_MEM`, `ID_EX`, `IF_ID`, `MEM_WB` | Four inter-stage pipeline registers. |

---

## Class Declaration

```java
public class PipelineController {
```

Single public class with one public method (`run`). The entire simulation is self-contained within this method.

---

## Constants

```java
private static final int DRAIN_THRESHOLD = 3;
private static final int MAX_CYCLE_LIMIT = 100_000;
```

| Constant | Value | Purpose |
|----------|-------|---------|
| `DRAIN_THRESHOLD` | 3 | Cycles to drain pipeline after HALT is detected in EX. Allows instructions in MEM and WB to complete. |
| `MAX_CYCLE_LIMIT` | 100,000 | Safety net — abort simulation if cycle count exceeds this (e.g., infinite-loop bug). |

---

## Main Method — `run()`

### Signature & Parameters

```java
public void run(List<Instruction> program, Memory mem, RegisterFile rf,
        Config cfg, Stats stats, CacheHierarchy cache)
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `program` | `List<Instruction>` | Assembled program (used by IF stage in Phase 1, and for PC bounds check in Phase 2). |
| `mem` | `Memory` | Shared RAM — cache backing store in Phase 2, direct data memory in Phase 1. |
| `rf` | `RegisterFile` | 32×32-bit register file. |
| `cfg` | `Config` | Forwarding flag, instruction latencies. |
| `stats` | `Stats` | Performance metrics accumulator. |
| `cache` | `CacheHierarchy` | Two-level cache — `null` means Phase 1 direct-memory mode. |

---

### Initialisation

```java
if (cache != null) {
    mem.loadProgram(program, 0);  // Encode instructions as binary words for cache-based fetch
}

IF_ID  ifId  = new IF_ID();
ID_EX  idEx  = new ID_EX();
EX_MEM exMem = new EX_MEM();
MEM_WB memWb = new MEM_WB();

IF_Stage  ifStage  = new IF_Stage();
ID_Stage  idStage  = new ID_Stage();
EX_Stage  exStage  = new EX_Stage(rf, cfg);
MEM_Stage memStage = new MEM_Stage();
WB_Stage  wbStage  = new WB_Stage();

HazardUnit    hazard    = new HazardUnit();
ForwardingUnit forwarding = new ForwardingUnit();

int pc          = 0x0000;
int drainCycles = 0;
int ifStallCycles  = 0;   // remaining IF cache-miss stall cycles
int memStallCycles = 0;   // remaining MEM cache-miss stall cycles
```

When `cache != null`, `mem.loadProgram()` encodes each `Instruction` as a 32-bit word into `Memory`. This is necessary because the L1I cache fetches from `Memory` and returns raw integers, which `IF_Stage` then decodes back to `Instruction` objects via `InstructionEncoder.decode()`.

`EX_Stage` now takes both `rf` and `cfg` (Phase 2 change — cfg is used to check `isForwardingEnabled()`).

---

### Simulation Loop

```java
while (true) { ... }
```

Exits via `break` when the pipeline is drained or the safety limit is hit.

---

#### Phase 0: Cache Stall Drain

```java
if (memStallCycles > 0 || ifStallCycles > 0) {
    if (memStallCycles > 0) memStallCycles--;
    else                    ifStallCycles--;
    stats.cycles++;
    stats.stalls++;
    if (stats.cycles > MAX_CYCLE_LIMIT) { ... break; }
    continue;   // skip all normal pipeline work this cycle
}
```

If either cache stall counter is non-zero, the entire pipeline is frozen: no instruction advances, and the loop restarts. MEM stalls take priority over IF stalls. Each cache stall cycle increments both `cycles` and `stalls`.

---

#### Phase 1: Hazard Detection

```java
boolean stall          = hazard.needsStall(idEx, ifId, exMem, cfg);
boolean isMultiCycleStall = (idEx.latencyCyclesLeft > 0);
```

`needsStall()` now also receives `exMem` (Phase 2 change) to detect no-forwarding RAW hazards where the producer has moved from EX→MEM.

---

#### Phase 2: WB, MEM, EX Stages

```java
MEM_WB oldMemWb = memWb;
wbStage.tick(memWb, rf, stats);
MEM_WB newMemWb = memStage.tick(exMem, mem, cache);   // Phase 2: cache passed
EX_MEM newExMem = exStage.tick(idEx, exMem, newMemWb, oldMemWb, forwarding);
```

Stages execute in **reverse order** (WB → MEM → EX) to model simultaneous hardware latching without data races. `MEM_Stage` now receives `cache` for variable-latency access.

---

#### Phase 3: MEM Cache Stall Check

```java
if (newMemWb.memLatencyLeft > 0) {
    memStallCycles = newMemWb.memLatencyLeft;
    newMemWb.memLatencyLeft = 0;
}
memWb = newMemWb;
exMem = newExMem;
```

After MEM executes, if a cache miss occurred, the extra latency in `memLatencyLeft` is transferred to `memStallCycles`. This stall will freeze the pipeline on subsequent iterations until the cache access completes.

---

#### Phase 4: Branch / Stall Resolution

```java
if (!newExMem.isNop && newExMem.branchMispredicted) {
    // BTFNT misprediction flush
    pc = newExMem.branchRecoveryPC;
    ifId = new IF_ID();
    idEx = new ID_EX();
    stats.branchFlushes++;
    if (stall) stats.stalls++;

} else if (!newExMem.isNop && newExMem.branchTaken
        && newExMem.opcode == Opcode.JAL) {
    // JAL unconditional jump flush
    pc = newExMem.jumpTarget;
    ifId = new IF_ID();
    idEx = new ID_EX();
    stats.branchFlushes++;
    if (stall) stats.stalls++;

} else if (stall) {
    if (!isMultiCycleStall) {
        idEx = new ID_EX();   // insert NOP bubble
    }
    stats.stalls++;

} else {
    // Normal operation
    idEx = idStage.tick(ifId, cfg);

    // BTFNT: backward branch redirect
    if (idEx.predictedPC != -1) {
        pc = idEx.predictedPC;
    }

    ifId = ifStage.tick(program, pc, cache);   // Phase 2: cache passed
    pc += 4;

    // IF cache stall check
    if (ifId.fetchLatencyLeft > 0) {
        ifStallCycles = ifId.fetchLatencyLeft;
        ifId.fetchLatencyLeft = 0;
    }
}
```

**Key Phase 2 changes vs Phase 1:**
1. **BTFNT misprediction** (`branchMispredicted`) is now detected separately from JAL — the EX stage sets `branchRecoveryPC` to either the branch target or `pc + 4` depending on the actual outcome vs prediction.
2. **JAL** is still flushed explicitly (it has no prediction to check — it's always taken).
3. **IF stage** now passes `cache` and checks `fetchLatencyLeft` after each fetch to initiate IF stall cycles.
4. Stall counting accounts for the case where a flush coincides with a pending data-hazard stall.

---

#### Phase 5: Cycle Counting & Termination

```java
stats.cycles++;

if (exStage.haltFlag && ++drainCycles >= DRAIN_THRESHOLD) break;

boolean pcPastEnd = (pc / 4) >= program.size();
if (pcPastEnd && ifId.isNop && idEx.isNop && exMem.isNop && memWb.isNop) break;

if (stats.cycles > MAX_CYCLE_LIMIT) { ...; break; }
```

Three exit conditions:
1. **HALT drain:** After a HALT/ECALL reaches EX (`haltFlag = true`), count 3 more cycles and exit.
2. **Clean drain:** PC has passed the end of the program **and** all pipeline registers are NOPs — the pipeline is empty.
3. **Safety limit:** 100,000-cycle guard.

After loop exit:
```java
stats.collectCacheStats(cache);
```

---

## Cache Stall Priority

```
Priority: MEM stall > IF stall > data-hazard stall > normal
```

On any given cycle, at most one of `memStallCycles` or `ifStallCycles` is decremented. This correctly models that a memory access that misses in cache is more critical to wait for than an instruction fetch miss, since it blocks the write-back of a result.

---

## Summary Table

| Phase | Code | Purpose |
|-------|------|---------|
| Init | `mem.loadProgram(program, 0)` | Encode program for cache-based fetch (Phase 2 only) |
| 0 | `memStallCycles/ifStallCycles` | Drain cache miss stall cycles (freeze all stages) |
| 1 | `hazard.needsStall()` | Detect data hazard stalls |
| 2 | `wbStage/memStage/exStage.tick()` | Execute WB→MEM→EX (reverse order) |
| 3 | `newMemWb.memLatencyLeft` | Capture MEM cache miss stall cycles |
| 4a | `branchMispredicted` | Flush on BTFNT misprediction |
| 4b | `JAL` flush | Flush on unconditional jump |
| 4c | `stall` path | Insert NOP bubble for data hazard |
| 4d | Normal | ID decode, IF fetch + PC advance |
| 4d | `fetchLatencyLeft` | Capture IF cache miss stall cycles |
| 5 | `stats.cycles++` | Count cycle |
| 5 | `drainCycles`, `pcPastEnd` | Termination conditions |
| Post | `stats.collectCacheStats(cache)` | Snapshot cache hit/miss counts |

---

## Pipeline Timing Diagram

Example: Normal execution followed by a BTFNT misprediction at cycle 4.

```
Cycle:    1     2     3     4     5     6     7
        ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┐
Instr1: │ IF  │ ID  │ EX  │ MEM │ WB  │     │     │
Instr2: │     │ IF  │ ID* │ EX  │ MEM │ WB  │     │  *predicted taken (BTFNT)
Instr3: │     │     │ IF  │ ID  │─FLUSH──── │     │  ← misprediction in EX
Instr4: │     │     │     │ IF  │─FLUSH──── │     │  ← wrong-path, flushed
BrTgt:  │     │     │     │     │ IF  │ ID  │ EX  │  ← correct target
        └─────┴─────┴─────┴─────┴─────┴─────┴─────┘
```

---

## Data-Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                   PipelineController.run()                       │
│                                                                  │
│  ┌──────────────────────────────────────────────┐               │
│  │  Phase 0: Cache Stall Counters               │               │
│  │  memStallCycles > 0 → freeze, cycles++       │               │
│  │  ifStallCycles > 0  → freeze, cycles++       │               │
│  └──────────────────────────────────────────────┘               │
│                         │                                        │
│                         ▼                                        │
│   WB ◄─ MEM/WB ◄─ MEM ◄─ EX/MEM ◄─ EX ◄─ ID/EX ◄─ ID ◄─ IF   │
│    ↑                    ↑                                  ↑     │
│    │     (cache miss)   │                    (cache miss)  │     │
│    │    memStallCycles  │                   ifStallCycles  │     │
│    │                    │                                  │     │
│   RF ◄─────────── Forwarding ──────────────────────────────     │
│                         │                                        │
│   HazardUnit ──── stall? ──► freeze IF/ID, insert NOP bubble    │
│                                                                  │
│   EX stage ─── branchMispredicted? ──► redirect PC, flush 2     │
│             ─── JAL taken?          ──► redirect PC, flush 2     │
│             ─── HALT?              ──► drain 3 cycles, break     │
└─────────────────────────────────────────────────────────────────┘
```

---

*End of Report*
