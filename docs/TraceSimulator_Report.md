# Study Report — `TraceSimulator.java`

> **File:** `src/trace/TraceSimulator.java`
> **Module:** Trace Replay — Main Simulation Engine
> **Date Generated:** 2026-05-12 *(Phase 3)*

---

## Table of Contents

1. [Overview](#overview)
2. [Package & Imports](#package--imports)
3. [Class Fields](#class-fields)
4. [Constructor](#constructor)
5. [Method — `run()`](#run)
6. [Method — `executeLoad()`](#executeload)
7. [Method — `executeStore()`](#executestore)
8. [Method — `executeAdd()`](#executeadd)
9. [Method — `executeMul()`](#executemul)
10. [Method — `printStats()`](#printstats)
11. [Latency Model](#latency-model)
12. [Summary Table](#summary-table)

---

## Overview

`TraceSimulator` is the **main simulation engine for trace replay mode**. It reads pre-parsed `TraceInstruction` objects and simulates their execution with:

- **Virtual memory translation** (TLB → page table → page fault)
- **L1D data cache** using physical addresses (PIPT)
- **Simple latency accumulation** — no pipeline, each instruction's latency is added to the cycle count

This is a separate execution path from the existing 5-stage pipeline. It is invoked via `java Main --trace <trace_file> <config_file>`.

---

## Package & Imports

```java
package trace;
import cache.CacheConfig;
import common.Config;
import core.Memory;
import core.Stats;
import vm.TranslationResult;
import vm.VirtualMemoryUnit;
```

---

## Class Fields

```java
private final Config cfg;
private final Stats stats;
private final VirtualMemoryUnit vmu;
private TraceDataCache dataCache;
private Memory physicalMemory;
private final int[] registers = new int[32];
```

| Field | Description |
|-------|-------------|
| `cfg` | Configuration object with VM and cache parameters |
| `stats` | Shared statistics object |
| `vmu` | Virtual memory unit for address translation |
| `dataCache` | L1D-only cache (no L2 in trace mode) |
| `physicalMemory` | Physical memory sized to `physical_size_bytes` |
| `registers` | Simple 32-register file for ALU operations |

---

## Constructor

```java
public TraceSimulator(Config cfg)
```

Initializes the VM unit, physical memory, and L1D cache. The cache is created using `TraceDataCache` (L1D-only, direct to memory on miss — no L2).

---

## `run()`

```java
public void run(List<TraceInstruction> instructions)
```

Main simulation loop. Iterates through all trace instructions, dispatching each to the appropriate execute method. After all instructions complete, collects cache and VM statistics into the `Stats` object.

---

## `executeLoad()`

```java
private void executeLoad(TraceInstruction instr)
```

1. Translates virtual address → physical address via `VirtualMemoryUnit`
2. Reads from L1D cache using the physical address (PIPT)
3. Writes loaded value to the destination register
4. Accumulates: `cycles += translation_latency + cache_latency`

---

## `executeStore()`

```java
private void executeStore(TraceInstruction instr)
```

1. Translates virtual address → physical address (marks page dirty)
2. Writes register value to L1D cache using the physical address
3. Accumulates: `cycles += translation_latency + cache_latency`

---

## `executeAdd()`

```java
private void executeAdd(TraceInstruction instr)
```

Computes `rd = rs1 + rs2` and charges the configured ADD latency (default: 1 cycle). No memory access or translation.

---

## `executeMul()`

```java
private void executeMul(TraceInstruction instr)
```

Computes `rd = rs1 * rs2` and charges the configured MUL latency (default: 3 cycles). Extra cycles counted as stalls.

---

## `printStats()`

```java
public void printStats(PrintStream out)
```

Prints all simulation statistics in a formatted report:

```
=== Trace Replay Simulation Stats ===

--- Execution ---
Total Cycles              : 73004328
Instructions Retired      : 715724
IPC                       : 0.0098
Stalls                    : 72288604

--- Virtual Memory ---
TLB Hits                  : 357854
TLB Misses                : 8
TLB Hit Rate              : 1.0000
Page Walks                : 8
Page Faults               : 8
Page Evictions            : 0
Dirty Evictions           : 0
Translation Penalty Cycles: 358342

--- Cache Statistics ---
L1D Hits                  : 0
L1D Misses                : 357862
L1D Miss Rate             : 1.0000
```

---

## Latency Model

| Instruction | Total Cycles |
|-------------|-------------|
| `L` (Load) | `translation_latency + cache_latency` |
| `S` (Store) | `translation_latency + cache_latency` |
| `ADD` | `cfg.getLatency(ADD)` = 1 cycle |
| `MUL` | `cfg.getLatency(MUL)` = 3 cycles |

Where:
- `translation_latency` = TLB hit (1) or TLB miss + page walk (11) or + page fault (61)
- `cache_latency` = L1D hit (1) or L1D miss (1 + MEMORY_LATENCY)

---

## Summary Table

| Method | Visibility | Return | Purpose |
|--------|-----------|--------|---------|
| `TraceSimulator(cfg)` | `public` | — | Constructor |
| `run(instructions)` | `public` | `void` | Execute all trace instructions |
| `executeLoad(instr)` | `private` | `void` | Translate + cache read |
| `executeStore(instr)` | `private` | `void` | Translate + cache write |
| `executeAdd(instr)` | `private` | `void` | ALU add (1 cycle) |
| `executeMul(instr)` | `private` | `void` | ALU multiply (3 cycles) |
| `printStats(out)` | `public` | `void` | Print formatted statistics |
| `getStats()` | `public` | `Stats` | Access stats object |

---

*End of Report*
