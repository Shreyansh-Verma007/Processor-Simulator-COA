# Study Report — `MEM_Stage.java`

> **File:** `src/pipeline_stages/MEM_Stage.java`
> **Module:** Memory Access (MEM) stage of a 5-stage RISC-V pipelined processor simulator
> **Date Updated:** 2026-04-19 *(originally 2026-04-09)*

---

## Table of Contents

1. [Overview](#overview)
2. [Imports & Package Declaration](#imports--package-declaration)
3. [Class Declaration](#class-declaration)
4. [Main Method — `tick()`](#main-method--tick)
   - [Cache-Enabled Path](#cache-enabled-path)
   - [Direct Memory Path (Phase 1 Fallback)](#direct-memory-path-phase-1-fallback)
5. [Cache Miss Stall Mechanism](#cache-miss-stall-mechanism)
6. [Summary Table](#summary-table)
7. [Data-Flow Diagram](#data-flow-diagram)
8. [Why Stores Don't Set `isNop`](#why-stores-dont-set-isnop)

---

## Overview

`MEM_Stage` models the **Memory Access stage** of the RISC-V pipeline:

```
IF → ID → EX → **MEM** → WB
```

**Phase 2 update:** `MEM_Stage` now supports two access modes selected at runtime:

| Mode | Behaviour |
|------|-----------|
| **Cache-enabled (Phase 2)** | Routes all loads/stores through `CacheHierarchy` (L1D → L2 → Memory); sets `memLatencyLeft` on cache miss. |
| **Direct memory (Phase 1 fallback)** | Accesses `Memory` directly in one cycle — no cache involved. |

---

## Imports & Package Declaration

```java
package pipeline_stages;

import cache.AccessResult;
import cache.CacheHierarchy;
import common.Opcode;
import core.Memory;
import pipeline_registers.EX_MEM;
import pipeline_registers.MEM_WB;
```

| Import | Purpose |
|--------|---------|
| `AccessResult` | Return type from cache accesses — bundles `data` and `latencyCycles`. |
| `CacheHierarchy` | Two-level cache; `null` triggers Phase 1 direct-memory mode. |
| `Opcode` | Distinguishes LW/LB (loads), SW/SB (stores), and pass-through instructions. |
| `Memory` | Simulated RAM used in Phase 1 direct-access mode. |
| `EX_MEM` | Input pipeline register — carries ALU result and write data from EX stage. |
| `MEM_WB` | Output pipeline register — carries result and possible stall count to WB stage. |

---

## Class Declaration

```java
// Memory Access (MEM) — routes through cache hierarchy when available.
public class MEM_Stage {
```

Public, **stateless** class — no instance fields.

---

## Main Method — `tick()`

### Signature

```java
public MEM_WB tick(EX_MEM exMem, Memory mem, CacheHierarchy cache)
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `exMem` | `EX_MEM` | Input pipeline register: `aluResult` (effective address), `opcode`, `rd`, `writeData` (for stores). |
| `mem` | `Memory` | Shared RAM — used only in Phase 1 mode. |
| `cache` | `CacheHierarchy` | Cache hierarchy — `null` selects Phase 1 mode. |

**Returns:** A new `MEM_WB` pipeline register carrying the result (and optional stall count) to the Write-Back stage.

---

### Cache-Enabled Path

```java
if (cache != null) {
    if (op == Opcode.LW) {
        AccessResult r = cache.readData(exMem.aluResult);
        out.result = r.data;
        out.memLatencyLeft = r.latencyCycles - 1;
    } else if (op == Opcode.LB) {
        AccessResult r = cache.readDataByte(exMem.aluResult);
        out.result = r.data;
        out.memLatencyLeft = r.latencyCycles - 1;
    } else if (op == Opcode.SW) {
        AccessResult r = cache.writeData(exMem.aluResult, exMem.writeData);
        out.memLatencyLeft = r.latencyCycles - 1;
    } else if (op == Opcode.SB) {
        AccessResult r = cache.writeDataByte(exMem.aluResult, exMem.writeData);
        out.memLatencyLeft = r.latencyCycles - 1;
    } else {
        out.result = exMem.aluResult;   // pass-through (non-memory ops)
    }
}
```

Each memory operation calls the corresponding `CacheHierarchy` method:

| Instruction | Cache Method | Returns |
|-------------|-------------|---------|
| `LW` | `readData(addr)` | 32-bit word from L1D/L2/Mem |
| `LB` | `readDataByte(addr)` | 8-bit byte (zero-extended) from L1D/L2/Mem |
| `SW` | `writeData(addr, val)` | Latency only (write-allocate into L1D) |
| `SB` | `writeDataByte(addr, val)` | Latency only (read-modify-write in L1D) |

`memLatencyLeft = latencyCycles - 1`: the `-1` accounts for the current tick. If the access hits in L1D (5-cycle default), `memLatencyLeft = 4` extra stall cycles. On a full miss to main memory (200 cycles), the stall will be much larger.

Note: Unlike the Phase 1 implementation, stores in Phase 2 do **not** set `out.isNop = true` because `MEM_WB.isNop` defaults to `false` when the instruction is real, and `WB_Stage` uses `opcode.writesBack()` to determine whether to write back.

---

### Direct Memory Path (Phase 1 Fallback)

```java
} else {
    if (op == Opcode.LW) {
        out.result = mem.readWord(exMem.aluResult);
    } else if (op == Opcode.LB) {
        out.result = mem.readByte(exMem.aluResult);
    } else if (op == Opcode.SW) {
        mem.writeWord(exMem.aluResult, exMem.writeData);
    } else if (op == Opcode.SB) {
        mem.writeByte(exMem.aluResult, exMem.writeData);
    } else {
        out.result = exMem.aluResult;
    }
}
```

The original Phase 1 behaviour: direct single-cycle `Memory` access with no latency tracking.

---

## Cache Miss Stall Mechanism

When `memLatencyLeft > 0` is set in `MEM_WB`, `PipelineController` captures it as a MEM stall:

```java
// PipelineController.java
if (newMemWb.memLatencyLeft > 0) {
    memStallCycles = newMemWb.memLatencyLeft;
    newMemWb.memLatencyLeft = 0;
}
```

Each cycle during the stall, `memStallCycles` is decremented and the pipeline is frozen. MEM stalls take priority over IF stalls.

---

## Summary Table

| Line(s) | Code | Purpose |
|---------|------|---------|
| `new MEM_WB()` | Default NOP output | Bubble if incoming instruction is NOP |
| `exMem.isNop` guard | Early return | Skip processing for pipeline bubbles |
| `cache != null` | Mode selection | Phase 2 (cache) vs Phase 1 (direct) |
| `cache.readData()` | LW via L1D→L2→Mem | Load word through cache |
| `cache.readDataByte()` | LB via L1D→L2→Mem | Load byte through cache |
| `cache.writeData()` | SW via L1D→L2→Mem | Store word (write-allocate, write-back) |
| `cache.writeDataByte()` | SB via L1D→L2→Mem | Store byte (read-modify-write in L1D) |
| `out.memLatencyLeft = lat - 1` | Stall signal | Communicate extra latency to controller |
| `out.result = exMem.aluResult` | Pass-through | Non-memory instructions forward ALU result |

---

## Data-Flow Diagram

```
        ┌───────────────────────────────────────────────────┐
        │             EX/MEM Register                        │
        │  aluResult (addr), writeData, opcode, rd           │
        └───────────────────────────────────────────────────┘
                          │
                          ▼
        ┌───────────────────────────────────────────────────┐
        │             MEM_Stage.tick()                       │
        │                                                     │
        │  cache != null?                                     │
        │    yes → CacheHierarchy.[read/write]Data(addr)     │
        │             → AccessResult{data, latency}          │
        │    no  → mem.readWord/readByte/writeWord/writeByte │
        │                                                     │
        │  LW/LB → out.result = loaded value                 │
        │  SW/SB → (side effect: write to cache/mem)         │
        │  Other → out.result = aluResult (pass-through)     │
        │                                                     │
        │  memLatencyLeft = latency - 1  (cache mode only)   │
        └───────────────────────────────────────────────────┘
                          │
                          ▼
        ┌───────────────────────────────────────────────────┐
        │             MEM/WB Register                        │
        │     result, opcode, rd, isNop, memLatencyLeft      │
        └───────────────────────────────────────────────────┘
                          │
                          ▼
                      WB Stage
              (writes result to register file)
```

---

## Why Stores Don't Set `isNop`

In Phase 1, stores explicitly set `out.isNop = true` to prevent WB from writing to a register. In Phase 2, this is no longer needed: the `WB_Stage` checks `opcode.writesBack()` before performing any write-back, and SW/SB return `false` from that method — so they naturally skip the register write without needing the NOP flag.

---

*End of Report*
