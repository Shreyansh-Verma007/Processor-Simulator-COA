# Study Report — `IF_Stage.java`

> **File:** `src/pipeline_stages/IF_Stage.java`
> **Module:** Instruction Fetch (IF) stage of a 5-stage RISC-V pipelined processor simulator
> **Date Updated:** 2026-04-19 *(originally 2026-04-09)*

---

## Table of Contents

1. [Overview](#overview)
2. [Imports & Package Declaration](#imports--package-declaration)
3. [Class Declaration](#class-declaration)
4. [Main Method — `tick()`](#main-method--tick)
   - [Cache-Enabled Path](#cache-enabled-path)
   - [Direct-Fetch Path (Phase 1 Fallback)](#direct-fetch-path-phase-1-fallback)
5. [Cache Miss Stall Mechanism](#cache-miss-stall-mechanism)
6. [Summary Table](#summary-table)
7. [Data-Flow Diagram](#data-flow-diagram)

---

## Overview

`IF_Stage` models the **Instruction Fetch stage** — the first stage of the RISC-V pipeline:

```
**IF** → ID → EX → MEM → WB
```

**Phase 2 update:** `IF_Stage` now supports two fetch modes, selected at runtime via the presence of a `CacheHierarchy`:

| Mode | Behaviour |
|------|-----------|
| **Cache-enabled (Phase 2)** | Fetches through L1I → L2 → Memory; produces a variable-latency `AccessResult`. |
| **Direct-fetch (Phase 1 fallback)** | Fetches directly from the `List<Instruction>` in one cycle — no cache involved. |

The `IF_Stage` itself remains **stateless** — all mode selection is done by checking whether `cache` is `null`.

---

## Imports & Package Declaration

```java
package pipeline_stages;

import cache.AccessResult;
import cache.CacheHierarchy;
import common.Instruction;
import common.InstructionEncoder;
import pipeline_registers.IF_ID;
import java.util.List;
```

| Import | Purpose |
|--------|---------|
| `AccessResult` | Bundles `data` (instruction word) and `latencyCycles` from a cache access. |
| `CacheHierarchy` | Two-level cache hierarchy; `null` means Phase 1 mode. |
| `Instruction` | Decoded RISC-V instruction object. |
| `InstructionEncoder` | Decodes a 32-bit encoded instruction word back into an `Instruction`. Used only in Phase 2 (cache encodes/decodes through memory). |
| `IF_ID` | Output pipeline register to the Decode stage. |

---

## Class Declaration

```java
// Instruction Fetch (IF) — fetches via cache hierarchy when available.
public class IF_Stage {
```

Public, **stateless** class — no instance fields. All state arrives via parameters.

---

## Main Method — `tick()`

### Signature

```java
public IF_ID tick(List<Instruction> program, int pc, CacheHierarchy cache)
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `program` | `List<Instruction>` | The full program (always passed; used in Phase 1 mode). |
| `pc` | `int` | Current Program Counter (byte address, multiple of 4). |
| `cache` | `CacheHierarchy` | Cache hierarchy; `null` selects Phase 1 direct-fetch mode. |

**Returns:** A new `IF_ID` pipeline register carrying the fetched instruction (and optional stall count) to the Decode stage.

---

### Cache-Enabled Path

```java
if (cache != null) {
    AccessResult result = cache.fetchInstruction(pc);
    Instruction instr = InstructionEncoder.decode(result.data);
    if (instr != null) {
        out.instruction = instr;
        out.pc = pc;
        out.isNop = false;
        out.fetchLatencyLeft = result.latencyCycles - 1;
    }
}
```

1. `cache.fetchInstruction(pc)` routes through **L1I → L2 → Memory** and returns an `AccessResult` containing the encoded 32-bit instruction word and the total access latency.
2. `InstructionEncoder.decode(result.data)` converts the raw integer back into an `Instruction` object. This is necessary because the cache operates on raw words (integers), while the pipeline works with `Instruction` objects.
3. `fetchLatencyLeft = latencyCycles - 1`: The current tick counts as 1 cycle, so the remaining stall cycles are `latency - 1`. A value of 0 means no extra stall (L1 hit on a 1-cycle cache would be `1 - 1 = 0`).

---

### Direct-Fetch Path (Phase 1 Fallback)

```java
} else {
    int index = pc / 4;
    if (index >= 0 && index < program.size()) {
        out.instruction = program.get(index);
        out.pc = pc;
        out.isNop = false;
    }
}
```

The original Phase 1 behaviour: converts the PC to a list index (`pc / 4`), bounds-checks, and directly retrieves the `Instruction` object. Always completes in 1 cycle — no stall latency is set.

---

## Cache Miss Stall Mechanism

When the cache path is used and there is a cache miss, `fetchLatencyLeft > 0` is set in `IF_ID`. The `PipelineController` detects this and counts down extra stall cycles:

```java
// PipelineController.java
if (ifId.fetchLatencyLeft > 0) {
    ifStallCycles = ifId.fetchLatencyLeft;
    ifId.fetchLatencyLeft = 0;
}
```

During a stall, the pipeline counter `ifStallCycles` is decremented each cycle and the pipeline is frozen (no new IF/ID/EX/MEM work until the stall resolves). MEM-stage stalls take priority over IF-stage stalls.

---

## Summary Table

| Line(s) | Code | Purpose |
|---------|------|---------|
| `new IF_ID()` | Default NOP output | Default bubble if anything goes wrong |
| `cache != null` | Mode selection | Switch between Phase 2 (cache) and Phase 1 (direct) |
| `cache.fetchInstruction(pc)` | L1I → L2 → Mem fetch | Variable-latency instruction fetch |
| `InstructionEncoder.decode()` | Binary decode | Convert encoded word back to `Instruction` object |
| `out.fetchLatencyLeft = lat - 1` | Stall signal | Communicate extra stall cycles to `PipelineController` |
| `pc / 4` | Index conversion | Byte address → instruction list index (Phase 1) |
| `program.get(index)` | Direct fetch | 1-cycle fetch from instruction list (Phase 1) |

---

## Data-Flow Diagram

```
                    ┌───────────────────────────────────────────┐
                    │             IF_Stage.tick()               │
                    │                                            │
  cache != null? ── yes ──► cache.fetchInstruction(pc)          │
       │                         │                              │
       │                    AccessResult                        │
       │                   { data, latency }                    │
       │                         │                              │
       │                  InstructionEncoder.decode(data)       │
       │                         │                              │
       no ──► pc / 4 ──► program.get(index)                     │
                                 │                              │
                          ┌──────┴────────────────────────────┐ │
                          │         IF/ID Register             │ │
                          │  instruction, pc, isNop,           │ │
                          │  fetchLatencyLeft (cache mode)     │ │
                          └──────────┬─────────────────────────┘ │
                                     │                            │
                                     ▼                            │
                                ID Stage                          │
                    └───────────────────────────────────────────┘
```

---

*End of Report*
