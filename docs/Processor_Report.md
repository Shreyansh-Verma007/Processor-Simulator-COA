# Study Report — `Processor.java`

> **File:** `src/core/Processor.java`
> **Module:** Top-level simulator controller for the RISC-V pipeline simulator
> **Date Updated:** 2026-04-19 *(originally 2026-04-09)*

---

## Table of Contents

1. [Overview](#overview)
2. [Imports & Package Declaration](#imports--package-declaration)
3. [Class Declaration & Fields](#class-declaration--fields)
4. [Constructor](#constructor)
5. [Method — `run(CompilationResult)`](#method--runcompilationresult)
6. [Private Method — `run(List<Instruction>)`](#private-method--runlistinstruction)
7. [Accessor — `getStats()`](#accessor--getstats)
8. [Summary Table](#summary-table)
9. [Architecture Diagram](#architecture-diagram)

---

## Overview

`Processor` is the **top-level facade** that wires together all core simulator components. It provides the API used by `Main.java` to run a program and retrieve performance statistics.

**Phase 2 update:** `Processor` now accepts a `Config` object in its constructor (instead of creating one internally). If the config contains a cache configuration, it builds a `CacheHierarchy` and passes it to `PipelineController`. The old accessor methods (`getRegister`, `getMemory`, `preload`, `dumpMemory`) have been removed — `Processor` now exposes only `run()` and `getStats()`.

---

## Imports & Package Declaration

```java
package core;

import cache.CacheHierarchy;
import common.Config;
import common.Instruction;
import compiler.CompilationResult;
import pipeline_stages.PipelineController;
import java.util.List;
```

| Import | Purpose |
|--------|---------|
| `CacheHierarchy` | Two-level cache built from `Config` if cache config is present. |
| `Config` | Holds instruction latencies, forwarding flag, and cache parameters. |
| `Instruction` | Decoded RISC-V instruction. |
| `CompilationResult` | Output of `Compiler` — contains instruction list and `.data` items. |
| `PipelineController` | The simulation engine. |

---

## Class Declaration & Fields

```java
// Top-level simulator controller.
public class Processor {
    private final Memory mem;
    private final RegisterFile rf;
    private final Config cfg;
    private final Stats stats;
    private CacheHierarchy cache;
```

| Field | Type | Description |
|-------|------|-------------|
| `mem` | `Memory` | 128 KB simulated RAM (backing store for both data and cache hierarchy). |
| `rf` | `RegisterFile` | 32 × 32-bit register file. |
| `cfg` | `Config` | Injected at construction — not created internally. |
| `stats` | `Stats` | Accumulates simulation metrics; populated by the pipeline run. |
| `cache` | `CacheHierarchy` | Built from `cfg` cache parameters if present; `null` in Phase 1 / no-cache mode. |

---

## Constructor

```java
public Processor(Config cfg) {
    this.mem   = new Memory();
    this.rf    = new RegisterFile();
    this.cfg   = cfg;
    this.stats = new Stats();

    // Build cache hierarchy if configured
    if (cfg.hasCacheConfig()) {
        this.cache = new CacheHierarchy(
                cfg.getL1I(), cfg.getL1D(), cfg.getL2(),
                cfg.getMainMemoryLatency(), mem);
    }
}
```

`cfg.hasCacheConfig()` returns `true` when all three cache level configs (L1I, L1D, L2) are non-null. By default (from `Config` constructor), a full cache hierarchy is always created.

---

## Method — `run(CompilationResult)`

```java
public void run(CompilationResult result) {
    if (result.getDataItems() != null && !result.getDataItems().isEmpty()) {
        mem.loadDataItems(result.getDataItems());
    }
    run(result.getInstructions());
}
```

The primary public entry point called by `Main.java`. It:
1. **Loads `.data` segment items** from the compilation result into RAM (byte-level writes via `Memory.loadDataItems()`).
2. Delegates to the private `run(List<Instruction>)`.

---

## Private Method — `run(List<Instruction>)`

```java
private void run(List<Instruction> program) {
    new PipelineController().run(program, mem, rf, cfg, stats, cache);
}
```

Creates a fresh `PipelineController` and starts the simulation. All state lives in the shared objects (`mem`, `rf`, `stats`) — the controller itself is stateless.

`cache` may be `null` (no-cache / Phase 1 mode), in which case `PipelineController` uses direct memory access.

---

## Accessor — `getStats()`

```java
public Stats getStats() {
    return stats;
}
```

Returns the `Stats` object after simulation completes. Used by `Main.java` to write performance metrics to `output.txt`.

---

## Summary Table

| Method | Visibility | Return | Purpose |
|--------|-----------|--------|---------|
| `Processor(Config)` | `public` | — | Construct simulator, optionally build cache |
| `run(CompilationResult)` | `public` | `void` | Primary entry — load data + run pipeline |
| `run(List<Instruction>)` | `private` | `void` | Delegate to `PipelineController` |
| `getStats()` | `public` | `Stats` | Retrieve performance metrics after run |

---

## Architecture Diagram

```
          Main.java
               │
               ▼  new Processor(cfg)
        ┌──────────────────────┐
        │       Processor       │  ← Top-level facade
        │                       │
        │  ┌─────────────────┐  │
        │  │  Memory (128KB) │  │  ← backing store
        │  └─────────────────┘  │
        │  ┌─────────────────┐  │
        │  │  RegisterFile   │  │  ← x0–x31
        │  └─────────────────┘  │
        │  ┌─────────────────┐  │
        │  │  Config         │  │  ← latencies, forwarding, cache params
        │  └─────────────────┘  │
        │  ┌─────────────────┐  │
        │  │  Stats          │  │  ← cycles, stalls, IPC, cache hits
        │  └─────────────────┘  │
        │  ┌─────────────────┐  │
        │  │ CacheHierarchy  │  │  ← L1I, L1D, L2 (or null = Phase 1)
        │  └─────────────────┘  │
        └────────┬──────────────┘
                 │  run()
                 ▼
        PipelineController
        (IF → ID → EX → MEM → WB)
```

---

*End of Report*
