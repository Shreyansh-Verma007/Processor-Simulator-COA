# Documentation Index — RISC-V Pipeline Simulator

> **Project:** `Processor-Simulator-COA`
> **Last Updated:** 2026-08-17
> **Total Reports:** 43 across 10 packages

All reports are organized by Java package, mirroring the `src/` source layout.

---

## Table of Contents

| Package | Path | Description | Reports |
|---------|------|-------------|---------|
| [main](#-main) | `src/` | Application entry point | 1 |
| [cache](#-cache) | `src/cache/` | L1I, L1D, L2 cache hierarchy | 5 |
| [common](#-common) | `src/common/` | Shared types, config, encoding | 5 |
| [compiler](#-compiler) | `src/compiler/` | RISC-V assembler (lexer → parser → encoder) | 5 |
| [core](#-core) | `src/core/` | Memory, registers, stats, processor shell | 4 |
| [hazard](#-hazard) | `src/hazard/` | Hazard detection & data forwarding | 3 |
| [pipeline\_registers](#-pipeline_registers) | `src/pipeline_registers/` | Inter-stage latch structs | 4 |
| [pipeline\_stages](#-pipeline_stages) | `src/pipeline_stages/` | IF/ID/EX/MEM/WB stage logic & controller | 6 |
| [trace](#-trace) | `src/trace/` | Phase 3 trace-replay simulator | 4 |
| [vm](#-vm) | `src/vm/` | Virtual memory, TLB, page tables | 6 |

---

## 📁 main

> **Source:** `src/Main.java` — Application entry point; dispatches to Pipeline, Trace, or Batch-Trace mode.

| Report | Java File | Description |
|--------|-----------|-------------|
| [Main\_Report.md](main/Main_Report.md) | `Main.java` | Entry point; CLI arg parsing, mode dispatch, output routing |

---

## 📁 cache

> **Source:** `src/cache/` — Two-level (L1I + L1D + shared L2) cache hierarchy backed by `Memory`.

| Report | Java File | Description |
|--------|-----------|-------------|
| [CacheHierarchy\_Report.md](cache/CacheHierarchy_Report.md) | `CacheHierarchy.java` | Top-level cache controller; routes IF/MEM accesses through L1 → L2 → Memory |
| [CacheLevel\_Report.md](cache/CacheLevel_Report.md) | `CacheLevel.java` | Generic set-associative cache level (configurable ways, sets, block size, policy) |
| [CacheConfig\_Report.md](cache/CacheConfig_Report.md) | `CacheConfig.java` | Immutable configuration for one cache level (size, associativity, latency) |
| [CacheLine\_Report.md](cache/CacheLine_Report.md) | `CacheLine.java` | Single cache block (tag, data words, valid, dirty flags) |
| [AccessResult\_Report.md](cache/AccessResult_Report.md) | `AccessResult.java` | Return type from cache operations: `{ data, latencyCycles }` |

---

## 📁 common

> **Source:** `src/common/` — Shared infrastructure used across all packages.

| Report | Java File | Description |
|--------|-----------|-------------|
| [Config\_Report.md](common/Config_Report.md) | `Config.java` | Global simulator configuration (forwarding, latencies, cache params) |
| [Opcode\_Report.md](common/Opcode_Report.md) | `Opcode.java` | Enum of all supported RISC-V opcodes |
| [Instruction\_Report.md](common/Instruction_Report.md) | `Instruction.java` | Decoded instruction: opcode, rd, rs1, rs2, immediate |
| [InstructionEncoder\_Report.md](common/InstructionEncoder_Report.md) | `InstructionEncoder.java` | Encodes `Instruction → int` and decodes `int → Instruction` (for cache word storage) |
| [StatsPrinter\_Report.md](common/StatsPrinter_Report.md) | `StatsPrinter.java` | Formats and writes final simulation stats to file |

---

## 📁 compiler

> **Source:** `src/compiler/` — Three-phase RISC-V assembler: Lexer → Parser → Compiler.

| Report | Java File | Description |
|--------|-----------|-------------|
| [Compiler\_Report.md](compiler/Compiler_Report.md) | `Compiler.java` | Top-level assembler: orchestrates lexing, parsing, label resolution, encoding |
| [Parser\_Report.md](compiler/Parser_Report.md) | `Parser.java` | Converts token stream into `Instruction` list and `.data` directives |
| [Lexer\_Report.md](compiler/Lexer_Report.md) | `Lexer.java` | Tokenizes raw RISC-V assembly source text |
| [CompilationResult\_Report.md](compiler/CompilationResult_Report.md) | `CompilationResult.java` | Output struct: `{ instructions, dataItems }` |
| [DataItem\_Report.md](compiler/DataItem_Report.md) | `DataItem.java` | Byte block for a `.data` segment entry (`address + byte[]`) |

---

## 📁 core

> **Source:** `src/core/` — Low-level hardware primitives shared by all simulation modes.

| Report | Java File | Description |
|--------|-----------|-------------|
| [Processor\_Report.md](core/Processor_Report.md) | `Processor.java` | Top-level simulation orchestrator; wires together all components |
| [Memory\_Report.md](core/Memory_Report.md) | `Memory.java` | Word-addressable simulated RAM; backing store for cache and VM |
| [RegisterFile\_Report.md](core/RegisterFile_Report.md) | `RegisterFile.java` | 32 × 32-bit general-purpose register file (x0 hardwired to 0) |
| [Stats\_Report.md](core/Stats_Report.md) | `Stats.java` | Performance metrics: cycles, stalls, IPC, cache hit/miss counters |

---

## 📁 hazard

> **Source:** `src/hazard/` — Data hazard detection and forwarding logic.

| Report | Java File | Description |
|--------|-----------|-------------|
| [HazardUnit\_Report.md](hazard/HazardUnit_Report.md) | `HazardUnit.java` | Detects RAW hazards; signals stall to `PipelineController` |
| [ForwardingUnit\_Report.md](hazard/ForwardingUnit_Report.md) | `ForwardingUnit.java` | Resolves forwarding paths (EX→EX, MEM→EX, WB→EX) |
| [ForwardResult\_Report.md](hazard/ForwardResult_Report.md) | `ForwardResult.java` | Return struct: `{ rs1Value, rs2Value }` after forwarding is applied |

---

## 📁 pipeline\_registers

> **Source:** `src/pipeline_registers/` — Inter-stage latch structs that carry data between pipeline stages each cycle.

| Report | Java File | Stage | Key Fields |
|--------|-----------|-------|------------|
| [IF\_ID\_Report.md](pipeline_registers/IF_ID_Report.md) | `IF_ID.java` | IF → ID | `instruction`, `pc`, `fetchLatencyLeft` |
| [ID\_EX\_Report.md](pipeline_registers/ID_EX_Report.md) | `ID_EX.java` | ID → EX | `opcode`, `rd`, `rs1`, `rs2`, `immediate`, `latencyCyclesLeft`, `predictedPC` |
| [EX\_MEM\_Report.md](pipeline_registers/EX_MEM_Report.md) | `EX_MEM.java` | EX → MEM | `aluResult`, `writeData`, `branchTaken`, `jumpTarget`, `branchMispredicted`, `branchRecoveryPC` |
| [MEM\_WB\_Report.md](pipeline_registers/MEM_WB_Report.md) | `MEM_WB.java` | MEM → WB | `result`, `rd`, `memLatencyLeft` |

---

## 📁 pipeline\_stages

> **Source:** `src/pipeline_stages/` — Functional models of each pipeline stage plus the central simulation controller.

| Report | Java File | Description |
|--------|-----------|-------------|
| [PipelineController\_Report.md](pipeline_stages/PipelineController_Report.md) | `PipelineController.java` | Central simulation loop; orchestrates all stages, hazards, branches, and cache stalls |
| [IF\_Stage\_Report.md](pipeline_stages/IF_Stage_Report.md) | `IF_Stage.java` | Instruction Fetch — direct list (Phase 1) or cache-based (Phase 2) |
| [ID\_Stage\_Report.md](pipeline_stages/ID_Stage_Report.md) | `ID_Stage.java` | Instruction Decode — reads register file, computes BTFNT prediction |
| [EX\_Stage\_Report.md](pipeline_stages/EX_Stage_Report.md) | `EX_Stage.java` | Execute — ALU operations, branch resolution, multi-cycle support, forwarding |
| [MEM\_Stage\_Report.md](pipeline_stages/MEM_Stage_Report.md) | `MEM_Stage.java` | Memory Access — load/store via cache (Phase 2) or direct memory (Phase 1) |
| [WB\_Stage\_Report.md](pipeline_stages/WB_Stage_Report.md) | `WB_Stage.java` | Write Back — writes result to register file |

---

## 📁 trace

> **Source:** `src/trace/` — Phase 3 trace-replay simulator for memory-access trace files.

| Report | Java File | Description |
|--------|-----------|-------------|
| [TraceSimulator\_Report.md](trace/TraceSimulator_Report.md) | `TraceSimulator.java` | Replays a `.trace` file through VM + L1D cache; collects stats |
| [TraceParser\_Report.md](trace/TraceParser_Report.md) | `TraceParser.java` | Parses `.trace` file lines into `TraceInstruction` objects |
| [TraceInstruction\_Report.md](trace/TraceInstruction_Report.md) | `TraceInstruction.java` | Represents one parsed trace entry: `{ type, address, value }` |
| [TraceDataCache\_Report.md](trace/TraceDataCache_Report.md) | `TraceDataCache.java` ⚠️ | L1D-only write-back cache for trace mode (source file missing — see note below) |

> ⚠️ **Missing Source:** `src/trace/TraceDataCache.java` is referenced in `Pipeline_Simulator_Report.md` and `Meeting_Minute.md` (added during Phase 3 to fix a compilation error), but the file does not exist in the current repository. The documentation report is retained for reference. The class should be recreated from its documented interface.

---

## 📁 vm

> **Source:** `src/vm/` — Virtual memory subsystem: address translation, TLB, and page tables.

| Report | Java File | Description |
|--------|-----------|-------------|
| [VirtualMemoryUnit\_Report.md](vm/VirtualMemoryUnit_Report.md) | `VirtualMemoryUnit.java` | Top-level VM controller; translates virtual addresses and manages page faults |
| [TLB\_Report.md](vm/TLB_Report.md) | `TLB.java` | Translation Lookaside Buffer — fully-associative, LRU eviction |
| [PageTable\_Report.md](vm/PageTable_Report.md) | `PageTable.java` | Software page table mapping virtual page numbers to physical frames |
| [TLBEntry\_Report.md](vm/TLBEntry_Report.md) | `TLBEntry.java` | Single TLB entry: `{ vpn, pfn, valid }` |
| [PageTableEntry\_Report.md](vm/PageTableEntry_Report.md) | `PageTableEntry.java` | Single page table entry: `{ pfn, present }` |
| [TranslationResult\_Report.md](vm/TranslationResult_Report.md) | `TranslationResult.java` | Return type from address translation: `{ physicalAddress, tlbHit, pageFault }` |

---

## Coverage Summary

| Package | Source Files | Reports | Status |
|---------|-------------|---------|--------|
| `main` | 1 | 1 | ✅ Complete |
| `cache` | 5 | 5 | ✅ Complete |
| `common` | 5 | 5 | ✅ Complete |
| `compiler` | 5 | 5 | ✅ Complete |
| `core` | 4 | 4 | ✅ Complete |
| `hazard` | 3 | 3 | ✅ Complete |
| `pipeline_registers` | 4 | 4 | ✅ Complete |
| `pipeline_stages` | 6 | 6 | ✅ Complete |
| `trace` | 3 *(+1 missing)* | 4 | ⚠️ `TraceDataCache.java` source missing |
| `vm` | 6 | 6 | ✅ Complete |
| **Total** | **42 (+1 missing)** | **43** | **9/10 packages fully sourced** |

---

*Generated 2026-08-17 | Processor-Simulator-COA*
