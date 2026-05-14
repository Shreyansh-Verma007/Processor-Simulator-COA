# 🚀 RISC-V Pipeline Simulator

<p align="center">
  <strong>A cycle-accurate, modular 5-stage in-order RISC-V processor simulator</strong><br>
  with full cache hierarchy, virtual memory, hazard resolution, and trace replay<br><br>
  <code>Java</code> · <code>Zero Dependencies</code> · <code>3,500+ Lines</code> · <code>42 Source Files</code> · <code>9 Packages</code>
</p>

---

> **What makes this different?** This isn't a textbook toy — it's a **hardware-faithful simulation** where every cycle is accounted for. Pipeline stalls from load-use hazards, cache miss penalties propagating through a two-level hierarchy, TLB misses triggering page walks, dirty page evictions on physical memory pressure — all modeled with the precision expected of a real RTL design, but expressed in clean, modular Java.

---

## 🎯 Project Highlights

- **Cycle-Accurate Pipeline** — 5-stage IF→ID→EX→MEM→WB with precise stall/flush/drain mechanics
- **Complete Memory Hierarchy** — L1I + L1D + unified L2 caches, all set-associative with LRU/FIFO, write-back write-allocate
- **Full Virtual Memory** — TLB, flat page table, page fault handling, frame allocation, LRU/FIFO page replacement, dirty eviction tracking, swap space with file-backed persistence (`swap.txt`)
- **Branch Prediction** — BTFNT (Backward-Taken, Forward-Not-Taken) static predictor with misprediction recovery
- **Data Forwarding** — EX/MEM → EX and MEM/WB → EX bypass paths, configurable enable/disable for experimentation
- **Trace Replay Engine** — Feed pre-recorded memory access traces through the VM + cache subsystem for workload analysis
- **Two-Pass Assembler** — Full `.data`/`.text` section support, labels, pseudo-instructions, string literals
- **Single Config File** — All parameters (latencies, cache geometry, VM sizes, replacement policies) in one INI-style file
- **Modular Architecture** — Pipeline, cache, VM, compiler, and hazard units are independent packages sharing zero duplicated logic

---

## 📐 System Architecture

### Pipeline Mode — Full Processor Simulation

```
                    ┌──────────────────────────────────────────────────┐
  input.asm ───▶    │  Lexer → Parser → Compiler                      │
                    │         │                                        │
                    │         ▼                                        │
                    │  ┌─────┬─────┬─────┬─────┬─────┐                │
                    │  │ IF  │ ID  │ EX  │ MEM │ WB  │  Pipeline      │
                    │  └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┘                │
                    │     │     │     │     │     │                    │
                    │     ▼     │     │     ▼     ▼                    │
                    │   L1I    HazardUnit  L1D   RegisterFile         │
                    │     │   ForwardingUnit │                         │
                    │     └────────┬─────────┘                         │
                    │              ▼                                    │
                    │         L2 (Unified)                              │
                    │              │                                    │
                    │              ▼                                    │
                    │        Main Memory                                │
                    └──────────────────────────────────────────────────┘
```

### Trace Replay Mode — VM + Cache Workload Analysis

```
                    ┌──────────────────────────────────────────────────┐
  trace.file ───▶   │  TraceParser → TraceSimulator                    │
                    │                    │                              │
                    │         ┌──────────┼──────────┐                  │
                    │         ▼          ▼          ▼                  │
                    │       TLB     RegisterFile  CacheHierarchy       │
                    │         │                   (L1D only)           │
                    │         ▼                      │                 │
                    │    Page Table                   │                 │
                    │    (flat, single-level)         │                 │
                    │         │                       │                 │
                    │         ▼                       ▼                 │
                    │   Frame Allocator ──────▶ Physical Memory        │
                    │   (LRU/FIFO eviction)                            │
                    └──────────────────────────────────────────────────┘
```

> **Key design choice**: Both modes share the **same** `CacheHierarchy`, `RegisterFile`, `Memory`, `Config`, and `Stats` classes — zero code duplication between pipeline and trace paths.

---

## 🏗️ Pipeline Deep Dive

### Stage-by-Stage Breakdown

#### 1️⃣ IF — Instruction Fetch
- Fetches the instruction word from `CacheHierarchy.fetchInstruction(pc)`
- When cache is present: variable-latency fetch through L1I → L2 → Memory
- When cache is absent: direct 1-cycle fetch from instruction list (Phase 1 compatibility)
- On a cache miss, the **entire pipeline freezes** until the fetch completes

#### 2️⃣ ID — Instruction Decode + Branch Prediction
- Decodes opcode, register indices, and immediate values
- Applies **BTFNT static branch prediction**:
  - Backward branches (negative offset) → predicted **TAKEN** (optimizes loops)
  - Forward branches (positive offset) → predicted **NOT TAKEN**
- If predicted taken, **eagerly redirects PC** to the branch target
- Sets up multi-cycle latency countdown for MUL (3c) and DIV (4c)

#### 3️⃣ EX — Execute + Branch Resolution + Forwarding
- Performs ALU computation (`ADD`, `SUB`, `MUL`, `DIV`, shifts, logic ops)
- **Resolves branch conditions** by comparing actual outcome vs. BTFNT prediction
- On misprediction: asserts `branchMispredicted`, provides recovery PC
- **Forwarding priority chain**: EX/MEM → newMEM/WB → oldMEM/WB → register file
- Multi-cycle instructions (MUL, DIV) emit NOP bubbles while counting down

#### 4️⃣ MEM — Memory Access
- Routes `LW`/`LB`/`SW`/`SB` through the cache hierarchy
- Cache hit: returns data with L1D latency
- Cache miss: block is fetched from L2 (or memory), installed in L1D, then data is returned
- Dirty evictions from L1D are written back to L2 (or memory if no L2)
- Without cache: direct memory access (backward compatible)

#### 5️⃣ WB — Write Back
- Writes ALU/load results to the destination register
- Enforces **x0 immutability** (RISC-V convention: x0 is always 0)
- Increments `instructionsRetired` counter for IPC calculation
- Only non-NOP, non-flushed instructions reach this stage

### Pipeline Hazard Resolution

```
┌──────────────────────────────────────────────────────────────────┐
│                    Hazard Detection Matrix                        │
├──────────────────┬───────────────────────────────────────────────┤
│ Hazard Type      │ Resolution Strategy                           │
├──────────────────┼───────────────────────────────────────────────┤
│ Load-Use (RAW)   │ 1-cycle stall (even with forwarding enabled)  │
│ RAW (forwarding) │ EX/MEM or MEM/WB bypass — zero stall penalty  │
│ RAW (no forward) │ Stall until producer reaches WB stage         │
│ Multi-cycle EX   │ Pipeline frozen while MUL/DIV counts down     │
│ Branch mispredict│ 2-cycle flush: squash IF_ID + ID_EX, fix PC   │
│ JAL              │ Flush + redirect to jump target               │
│ IF cache miss    │ Entire pipeline frozen for miss latency        │
│ MEM cache miss   │ Entire pipeline frozen (MEM prioritized)      │
│ Concurrent miss  │ MEM miss served first, then IF miss (serial)  │
└──────────────────┴───────────────────────────────────────────────┘
```

---

## 🗄️ Cache Hierarchy

### Two-Level Set-Associative Design

```
  IF Stage ──▶ L1I ──┐
                      ├──▶ L2 (Unified) ──▶ Main Memory (200 cycles)
 MEM Stage ──▶ L1D ──┘
```

| Property | L1I (default) | L1D (default) | L2 (default) |
|----------|:---:|:---:|:---:|
| Size | 1 KB | 1 KB | 8 KB |
| Block Size | 64 B | 64 B | 64 B |
| Associativity | 2-way | 2-way | 4-way |
| Hit Latency | 5 cycles | 5 cycles | 50 cycles |
| Write Policy | — | Write-back, WA | Write-back |
| Replacement | LRU / FIFO | LRU / FIFO | LRU / FIFO |

**Key implementation details:**
- **L2 is optional** — when set to null (trace mode), L1 misses go directly to main memory
- **Null-safe L1I/L1D** — trace mode uses only L1D; pipeline mode uses both
- **Write-back, write-allocate** — on a write miss, the block is fetched into L1, modified, and marked dirty. Evictions cascade: L1 → L2 → Memory
- **Single-stat-count policy** — each pipeline request counts as exactly ONE L1 access. Internal block fills and write-backs use no-stats methods to avoid inflating counters

---

## 🧠 Virtual Memory Subsystem

### Translation Pipeline

```
  Virtual Address
       │
       ▼
  ┌─────────┐    hit     ┌────────────────┐
  │   TLB   │ ─────────▶ │ Physical Frame │ ──▶ Cache Access (PIPT)
  │ (16 ent)│            └────────────────┘
  └────┬────┘
       │ miss (+10 cycles page walk)
       ▼
  ┌───────────┐   valid   ┌────────────────┐
  │ Page Table│ ────────▶ │ Physical Frame │ ──▶ Insert into TLB
  │  (flat)   │           └────────────────┘
  └─────┬─────┘
        │ invalid (+50 cycles page fault)
        ▼
  ┌──────────────┐
  │ Frame Alloc  │ ──▶ Free frame available? Use it.
  │              │ ──▶ No free frames? Evict via LRU/FIFO
  │              │       └▶ Dirty? Save to swap space
  │              │ ──▶ Page in swap? Restore data to frame
  └──────────────┘
         │
         ▼
  ┌──────────────┐
  │  Swap Space  │ ──▶ In-memory HashMap + swap.txt dump
  │  (swap.txt)  │     Preserves dirty page data across evictions
  └──────────────┘
```

### VM Statistics Tracked

| Metric | Description |
|--------|-------------|
| TLB Hits / Misses | Counts and hit rate for the data TLB |
| Page Walks | Number of page table lookups on TLB miss |
| Page Faults | First-access faults requiring frame allocation |
| Page Evictions | Pages evicted when physical memory is full |
| Dirty Evictions | Evictions of modified pages (require writeback) |
| Swap Outs | Dirty pages saved to swap space on eviction |
| Swap Ins | Pages restored from swap space on re-access |
| Translation Penalty | Total cycles spent on address translation |

---

## 📊 Performance Results

### Pipeline Mode — `input.asm`

> 22-instruction program with loops, branches, loads/stores, and arithmetic

**Config:** L1I=1KB/2-way/5c, L1D=4KB/1-way/1c, L2=8KB/4-way/50c, Mem=200c, Forwarding=ON

```
╔══════════════════════════════════════════════╗
║         Pipeline Simulation Results          ║
╠══════════════════════════════════════════════╣
║  Cycles              : 15,291                ║
║  Instructions Retired: 2,514                 ║
║  IPC                 : 0.164                 ║
║  Stalls              : 12,289                ║
║  Branch Flushes      : 242                   ║
╠══════════════════════════════════════════════╣
║  L1I : 2,758 hits, 2 misses  (MR: 0.001)    ║
║  L1D : 575 hits, 2 misses    (MR: 0.003)    ║
║  L2  : 0 hits, 4 misses      (MR: 1.000)    ║
╚══════════════════════════════════════════════╝
```

### Trace Replay Mode — 10 Traces (~715K instructions each)

**Config:** LRU, 16 DTLB entries, 64 physical frames (256KB), 4KB direct-mapped L1D (1cy hit), 8KB 4-way L2 (50cy hit, 50cy memory), PIPT

| Trace | Cycles | IPC | TLB Hit Rate | Page Faults | Evictions | Dirty Evictions | Swap Out | Swap In | L1D Miss Rate | L1I Miss Rate | L2 Miss Rate |
|:-----:|-------:|:---:|:------------:|:-----------:|:---------:|:---------------:|:--------:|:-------:|:-------------:|:-------------:|:------------:|
| 01 | 37,218,128 | 0.0192 | **100.0%** | 8 | 0 | 0 | 0 | 0 | 100.0% | 0.0% | 100.0% |
| 02 | 37,217,568 | 0.0192 | **100.0%** | 16 | 0 | 0 | 0 | 0 | 100.0% | 0.0% | 100.0% |
| 03 | 40,798,714 | 0.0175 | 0.0% | 17 | 0 | 0 | 0 | 0 | 100.0% | 0.0% | 100.0% |
| 04 | 37,262,236 | 0.0192 | 49.9% | 32 | 0 | 0 | 0 | 0 | 96.9% | 0.0% | 96.4% |
| 05 | 40,638,374 | 0.0176 | 3.6% | 64 | 0 | 0 | 0 | 0 | 100.0% | 0.0% | 99.9% |
| 06 | 22,909,696 | **0.0312** | 0.0% | 357,864 | 357,800 | 107,798 | 107,798 | 107,793 | **0.02%** | 0.0% | 100.0% |
| 07 | 40,775,202 | 0.0176 | 58.4% | 59,900 | 59,836 | 57,100 | 57,100 | 57,069 | 98.1% | 0.0% | 98.6% |
| 08 | 22,910,080 | **0.0312** | 0.0% | 357,870 | 357,806 | 71,269 | 71,269 | 71,222 | **0.02%** | 0.0% | 100.0% |
| 09 | **58,691,664** | 0.0122 | 0.0% | 357,876 | 357,812 | 125,515 | 125,515 | 125,492 | 100.0% | 0.0% | 100.0% |
| 10 | 35,322,154 | 0.0203 | 79.7% | 1,716 | 1,652 | 1,652 | 1,652 | 1,636 | 95.1% | 0.0% | 94.4% |

### Key Observations

**Traces 1–2 (Best TLB, Worst Cache):**
Near-perfect TLB locality (100% hit rate) with only 8–16 unique pages. However, 100% L1D miss rate — the 8-page stride pattern creates systematic cache conflicts in the 4KB direct-mapped L1D, and subsequently thrashes the L2. Every memory access pays the full 100-cycle L2 + memory penalty.

**Traces 6, 8 (Worst VM, Best Cache — The Paradox):**
Maximum page fault pressure — every single L/S instruction triggers a page fault (357K+ total), overwhelming 64 physical frames. **107K–71K dirty evictions** are saved to swap and selectively restored on re-access, ensuring correctness. Yet paradoxically, these traces achieve the **lowest non-translation cache penalty** because after translation, physical addresses map to a small set of cache lines, yielding 99.98% L1D hit rate. The expensive translation is offset by nearly free cache access.

**Trace 9 (Absolute Worst Case):**
Zero TLB hits combined with 100% L1D and L2 miss rates. Every memory operation pays: TLB miss (10 cycles page walk) + page fault (50 cycles) + cache miss (100 cycles). **125K dirty evictions** are swap-saved. Results in the highest total cycle count: **58.7 million cycles** for 715K instructions.

**Trace 7 (Heavy Swap):**
57,100 dirty evictions, all saved to swap with 57,069 restored — meaning nearly every evicted dirty page is re-accessed, validating that the swap round-trip preserves register computation correctness.

**Trace 10 (Best Overall):**
Good TLB locality (79.7%), only 1,716 page faults, and moderate cache reuse. All 1,652 evictions were dirty (saved to swap), with 1,636 restored. Achieves a much lower cycle count of **35.3 million**.

---

## 📜 Supported ISA

| Type | Instructions | Description |
|------|-------------|-------------|
| **R-Type** | `ADD`, `SUB`, `MUL`, `DIV`, `AND`, `OR`, `XOR`, `SLL`, `SRL` | Two-source register arithmetic and logic |
| **I-Type** | `ADDI`, `LI`, `LW`, `LB` | Immediate arithmetic and memory loads |
| **S-Type** | `SW`, `SB` | Memory stores (word and byte) |
| **B-Type** | `BEQ`, `BNE`, `BLT`, `BGE` | Conditional branches with BTFNT prediction |
| **J-Type** | `JAL` | Jump and link (saves return address) |
| **System** | `ECALL`, `HALT` | Register dump and simulation termination |

### Assembler Features

- **Two-pass compilation** with label resolution for forward references
- **`.data` section** support: `.word`, `.byte`, `.half`, `.space`, `.ascii`, `.asciiz`
- **Memory layout**: `.text` at `0x0000`, `.data` at `0x0400`
- **32-bit instruction encoding** for cache-based fetch

---

## ⚙️ Configuration

All parameters live in a **single INI-style config file** — nothing is hardcoded:

```ini
[pipeline]
forwarding_enabled = true

[latencies]
ADD = 1
MUL = 3
DIV = 4

[memory]
virtual_size_bytes = 536870912      # 512 MB virtual address space
physical_size_bytes = 262144        # 64 frames × 4 KB
page_size_bytes = 4096              # 4 KB pages

[vm]
dtlb_entries = 16
tlb_hit_latency = 1
page_walk_latency = 10
page_fault_latency = 50
replacement_policy = lru            # lru or fifo

[cache]
L1I_SIZE = 1024
L1I_BLOCK_SIZE = 64
L1I_ASSOCIATIVITY = 2
L1I_LATENCY = 5
L1D_SIZE = 4096
L1D_BLOCK_SIZE = 64
L1D_ASSOCIATIVITY = 1
L1D_LATENCY = 1
L2_SIZE = 8192
L2_BLOCK_SIZE = 64
L2_ASSOCIATIVITY = 4
L2_LATENCY = 50
MEMORY_LATENCY = 50
REPLACEMENT_POLICY = LRU
```

---

## 📂 Project Structure

```
src/
├── Main.java                        Entry point — pipeline, trace, batch modes
│
├── common/                          Shared infrastructure
│   ├── Config.java                  Unified INI config parser (pipeline + VM + cache)
│   ├── Instruction.java             Instruction record (opcode, rd, rs1, rs2, imm)
│   ├── InstructionEncoder.java      32-bit encode/decode for memory-backed fetch
│   ├── Opcode.java                  21 opcodes with isBranch(), isLoad(), writesBack()
│   └── StatsPrinter.java            Centralized stats formatting for all modes
│
├── compiler/                        Two-pass RISC-V assembler
│   ├── Compiler.java                Label resolution + instruction emission
│   ├── CompilationResult.java       Instructions + data items output
│   ├── DataItem.java                .data segment byte arrays
│   ├── Lexer.java                   Line tokenizer
│   └── Parser.java                  Instruction parser with symbol resolution
│
├── core/                            Processor fundamentals
│   ├── Processor.java               Top-level orchestrator
│   ├── Memory.java                  Configurable word-addressable memory
│   ├── RegisterFile.java            32 registers, x0 hardwired to 0
│   └── Stats.java                   Unified metrics (pipeline + cache + VM)
│
├── cache/                           Memory hierarchy
│   ├── CacheHierarchy.java          L1I → [L2] → Memory (null-safe L2 support)
│   ├── CacheLevel.java              Set-associative cache with LRU/FIFO eviction
│   ├── CacheConfig.java             Immutable cache geometry specification
│   ├── CacheLine.java               Line: valid, dirty, tag, data[], timestamps
│   └── AccessResult.java            Access output: data word + latency cycles
│
├── pipeline_stages/                 5-stage pipeline
│   ├── PipelineController.java      Main simulation loop with stall/flush logic
│   ├── IF_Stage.java                Cache-aware instruction fetch
│   ├── ID_Stage.java                Decode + BTFNT branch prediction
│   ├── EX_Stage.java                ALU + branch resolution + forwarding
│   ├── MEM_Stage.java               Load/store through cache hierarchy
│   └── WB_Stage.java                Register writeback + retirement
│
├── pipeline_registers/              Inter-stage communication
│   ├── IF_ID.java, ID_EX.java       Carry decoded fields + prediction data
│   ├── EX_MEM.java                  Carry ALU results + branch resolution
│   └── MEM_WB.java                  Carry final results for writeback
│
├── hazard/                          Pipeline correctness
│   ├── HazardUnit.java              RAW, load-use, multi-cycle stall detection
│   ├── ForwardingUnit.java          EX/MEM and MEM/WB bypass path logic
│   └── ForwardResult.java           Forwarding decision enum
│
├── trace/                           Trace replay subsystem
│   ├── TraceSimulator.java          VM + cache simulation engine
│   ├── TraceParser.java             L/S/ADD/MUL trace file parser
│   └── TraceInstruction.java        Trace instruction data class
│
└── vm/                              Virtual memory
    ├── VirtualMemoryUnit.java       TLB → PageTable → Fault → Allocate + Swap
    ├── TLB.java                     Fully-associative, LRU/FIFO eviction
    ├── TLBEntry.java                VPN → PFN mapping + dirty bit
    ├── PageTable.java               Flat table indexed by virtual page number
    ├── PageTableEntry.java          Valid, frame, dirty, LRU/FIFO timestamps
    └── TranslationResult.java       Physical address + translation latency
```

---

## 🛠️ Build & Run

### Compile

```bash
javac -d out -sourcepath src src/Main.java
```

### Pipeline Mode

```bash
java -cp out Main input.asm          # runs pipeline with default cache/VM config
```

### Trace Replay

```bash
java -cp out Main --trace phase3_traces/trace01.trace   # single trace
java -cp out Main --trace-all phase3_traces             # batch (all .trace files)
```

### Output Files

| File | Contents |
|------|----------|
| `console.txt` | Cycle-by-cycle pipeline execution log |
| `output.txt` | Final simulation statistics |
| `traces_output/*` | Individual batch trace results |
| `swap.txt` | Swap space dump — pages still resident in swap after simulation |

---

## 📊 Trace Replay Results

Below are the Phase 3 evaluation statistics for all 10 provided trace files under the mandatory hardware configuration (256KB Physical Memory, 16 DTLB, 4KB Direct Mapped L1, No L2).

| Trace | Total Cycles | Instr Retired | IPC | Stalls | TLB Hits | TLB Misses | Page Walks | Page Faults | Evictions | Dirty Evic | Translation Penalty |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `trace01` | 18967166 | 715724 | 0.0377 | 18251442 | 357854 | 8 | 8 | 8 | 0 | 0 | 358342 |
| `trace02` | 18967116 | 715704 | 0.0377 | 18251412 | 357836 | 16 | 16 | 16 | 0 | 0 | 358812 |
| `trace03` | 22547038 | 715752 | 0.0317 | 21831286 | 0 | 357876 | 357876 | 17 | 0 | 0 | 3937486 |
| `trace04` | 20761872 | 715728 | 0.0345 | 20046144 | 178516 | 179348 | 179348 | 32 | 0 | 0 | 2152944 |
| `trace05` | 22418658 | 715732 | 0.0319 | 21702926 | 13010 | 344856 | 344856 | 64 | 0 | 0 | 3809626 |
| `trace06` | 40438632 | 715728 | 0.0177 | 39722904 | 0 | 357864 | 357864 | 357864 | 357800 | 107798 | 21829704 |
| `trace07` | 23451884 | 715736 | 0.0305 | 22736148 | 208880 | 148988 | 148988 | 59900 | 59836 | 57100 | 4842748 |
| `trace08` | 40439310 | 715740 | 0.0177 | 39723570 | 0 | 357870 | 357870 | 357870 | 357806 | 71269 | 21830070 |
| `trace09` | 40439988 | 715752 | 0.0177 | 39724236 | 0 | 357876 | 357876 | 357876 | 357812 | 125515 | 21830436 |
| `trace10` | 19779898 | 715712 | 0.0362 | 19064186 | 285083 | 72773 | 72773 | 1716 | 1652 | 1652 | 1171386 |

---

## ⚖️ Architectural Decisions

| Decision | Rationale |
|----------|-----------|
| **Reverse-order stage ticking** (WB→IF) | Simulates half-cycle write-first/read-second — WB writes are visible to ID in the same cycle |
| **Serialized cache miss handling** | Models a single-ported shared memory bus (MEM miss prioritized over IF miss) |
| **BTFNT in ID, resolution in EX** | Keeps branch prediction simple while allowing 2-cycle recovery on mispredict |
| **3-cycle HALT drain** | Lets in-flight MEM/WB instructions retire gracefully before termination |
| **Null-safe CacheHierarchy** | L2=null makes L1 misses go to memory — eliminates need for separate trace cache class |
| **Flat page table** | O(1) lookup by VPN index; sufficient for 32-bit addresses with 4KB pages |
| **Unified Stats class** | Both pipeline and trace modes write to the same metrics object — consistent reporting |

---

<p align="center">
  <strong>Cycle Accurate · Set-Associative Cache · Virtual Memory · BTFNT Predicted · Trace Replay · Modular</strong>
</p>