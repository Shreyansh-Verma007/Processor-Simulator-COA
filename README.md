# 🚀 RISC-V Pipeline Simulator

**Phase 2 – Cache & Pipeline Integration**

A modular, cycle-accurate 5-stage in-order RISC-V pipeline simulator written in Java.  
Designed with clean architectural separation between compilation, core processor, pipeline stages, hazard resolution, and a variable-latency two-level cache hierarchy.

---

## 🧠 Architectural Overview

This simulator models a classic **5-stage RISC-V pipeline** with **BTFNT Branch Prediction** and a complete **Two-Level Set-Associative Cache Hierarchy**:

```
IF → ID → EX → MEM → WB
│         │          │
L1I       BTFNT      L1D
│         predict    │
L2 ◄────────────────►L2
│                     │
▼                     ▼
      Main Memory
```

### Core Components

| Component | File | Responsibility |
|-----------|------|----------------|
| `Processor` | `core/Processor.java` | Top-level orchestrator — builds cache hierarchy, dispatches to pipeline |
| `PipelineController` | `pipeline_stages/PipelineController.java` | Cycle loop, stall arbitration, branch flush handling |
| `HazardUnit` | `hazard/HazardUnit.java` | Detects RAW hazards, load-use conditions, multi-cycle stalls |
| `ForwardingUnit` | `hazard/ForwardingUnit.java` | Resolves data dependencies via EX/MEM → MEM/WB bypassing |
| `CacheHierarchy` | `cache/CacheHierarchy.java` | L1I, L1D, and unified L2 cache with write-back policy |
| `CacheLevel` | `cache/CacheLevel.java` | Single set-associative cache level (LRU / FIFO eviction) |
| `RegisterFile` | `core/RegisterFile.java` | 32-register architectural state (x0 hardwired to 0) |
| `Memory` | `core/Memory.java` | 128 KB main memory (.text at `0x0000`, .data at `0x0400`) |
| `Stats` | `core/Stats.java` | Collects cycles, stalls, flushes, IPC, cache hit/miss rates |
| `Config` | `common/Config.java` | Instruction latencies, cache parameters, forwarding toggle |

---

## 🏗️ Pipeline Stages

### 1️⃣ IF – Instruction Fetch
- Fetches instructions through **L1I cache** → L2 → main memory
- Variable latency: L1I hit = `L1I_LATENCY` cycles, miss adds L2 + memory latency
- Falls back to direct list fetch when cache is disabled

### 2️⃣ ID – Instruction Decode
- Decodes opcode, registers, and immediates
- Applies **BTFNT static branch prediction** (backward = taken, forward = not taken)
- Sets up multi-cycle latency countdown (MUL = 3 cycles, DIV = 4)

### 3️⃣ EX – Execute
- ALU computation for all R/I-type instructions
- Branch condition evaluation and misprediction detection
- Operand forwarding resolution (EX/MEM → newMEM/WB → oldMEM/WB → register file)

### 4️⃣ MEM – Memory Access
- Load/Store through **L1D cache** → L2 → main memory
- Variable latency with write-allocate, write-back policy
- Supports `LW`, `LB`, `SW`, `SB`

### 5️⃣ WB – Write Back
- Writes results to `RegisterFile`
- Enforces x0 immutability
- Increments `instructionsRetired` counter

---

## 🛠️ Build & Run

### Compile
```bash
javac -sourcepath src -d bin src/Main.java
```

### Run
```bash
# With default cache configuration
java -cp bin Main input.asm

# With custom cache configuration file
java -cp bin Main input.asm cache_config.txt
```

### Output Files
- **`output.txt`** — Simulation stats, active cache config, hit/miss rates
- **`console.txt`** — Register dumps (ECALL) and compilation info

---

## ⚙️ Configuration

### Cache Configuration File

Create a `cache_config.txt` with `KEY = VALUE` pairs (lines starting with `#` are comments):

```properties
# ── L1 Instruction Cache ──
L1I_SIZE          = 1024    # Total size in bytes
L1I_BLOCK_SIZE    = 64      # Block (cache line) size in bytes
L1I_ASSOCIATIVITY = 2       # Number of ways per set
L1I_LATENCY       = 5       # Hit latency in cycles

# ── L1 Data Cache ──
L1D_SIZE          = 1024
L1D_BLOCK_SIZE    = 64
L1D_ASSOCIATIVITY = 2
L1D_LATENCY       = 5

# ── L2 Unified Cache ──
L2_SIZE           = 8192
L2_BLOCK_SIZE     = 64
L2_ASSOCIATIVITY  = 4
L2_LATENCY        = 50

# ── Main Memory & Policies ──
MEMORY_LATENCY     = 200     # Main memory access latency in cycles
REPLACEMENT_POLICY = LRU     # LRU or FIFO
FORWARDING_ENABLED = true    # true or false
```

### Instruction Latencies (in `Config.java`)

| Instruction | Cycles |
|-------------|--------|
| ADD, SUB, ADDI, LI, SLL, SRL, XOR, OR, AND | 1 |
| MUL | 3 |
| DIV | 4 |
| LW, LB, SW, SB | 1 (+ cache access latency) |
| BEQ, BNE, BLT, BGE, JAL | 1 |

### Latency Model

| Scenario | Total Latency |
|----------|---------------|
| L1 hit | `L1_LATENCY` |
| L1 miss → L2 hit | `L1_LATENCY + L2_LATENCY` |
| L1 miss → L2 miss | `L1_LATENCY + L2_LATENCY + MEMORY_LATENCY` |

### Cache Policies

**Replacement Policies:**
- **LRU (Least Recently Used)** - Evicts the block that hasn't been accessed for the longest time.
- **FIFO (First In, First Out)** - Evicts the block that was inserted earliest, regardless of subsequent accesses.

**Write Policies:**
- **Write-Back:** Dirty blocks are only written to the next level of memory when they are evicted.
- **Write-Allocate:** On a store miss, the block is fetched into the cache first, then modified locally.

---

## 📊 Sample Output (`output.txt`)

```
=== Simulation Stats ===
Cycles             : 4449
Stalls             : 3823
Branch Flushes     : 23
Instructions Retired: 576
IPC                : 0.129

--- Cache Configuration ---
L1I  : 1024B, 64B blocks, 2-way, 5-cycle, LRU
L1D  : 1024B, 64B blocks, 2-way, 5-cycle, LRU
L2   : 8192B, 64B blocks, 4-way, 50-cycle, LRU
Memory Latency: 200 cycles
Forwarding    : enabled

--- Cache Statistics ---
L1I  : 601 hits, 2 misses, miss rate 0.003
L1D  : 154 hits, 1 misses, miss rate 0.006
L2   : 0 hits, 3 misses, miss rate 1.000
```

---

## 📜 Supported Instructions

| Type | Instructions |
|------|-------------|
| 🧮 R-Type | `ADD`, `SUB`, `MUL`, `DIV`, `SLL`, `SRL`, `XOR`, `OR`, `AND` |
| 🔢 I-Type | `ADDI` |
| 🏷️ Pseudo | `LI`, `LA`, `MV`, `NOP` |
| 💾 Memory | `LW`, `LB`, `SW`, `SB` |
| 🌿 Branch | `BEQ`, `BNE`, `BLT`, `BGE` (with BTFNT prediction) |
| 🔀 Jump | `JAL` |
| 🛑 System | `ECALL`, `HALT` |

### Registers
Both numeric (`x0`–`x31`) and ABI names (`zero`, `ra`, `sp`, `a0`–`a7`, `t0`–`t6`, `s0`–`s11`) are supported.

### Data Directives (`.data` section)
`.word`, `.half`, `.byte`, `.space`, `.zero`, `.ascii`, `.asciiz`, `.string`, `.align`, `.globl`

---

## ⚠️ Hazard Handling

### Data Hazards (RAW)
- **With forwarding enabled:** EX/MEM → MEM/WB bypass paths resolve most RAW hazards. Load-use hazards still incur a 1-cycle stall.
- **With forwarding disabled:** All RAW hazards from EX and MEM stages produce pipeline stalls.

### Control Hazards
- **BTFNT prediction:** Backward branches predicted taken, forward branches predicted not taken (applied in ID stage).
- **Misprediction penalty:** 2-cycle flush (IF_ID and ID_EX registers squashed).
- **JAL:** Always flushes 1 cycle (no prediction, resolved in EX).

### Cache Miss Stalls
- **MEM priority:** If both IF and MEM have cache misses in the same cycle, MEM (data) stall is served first, then IF (instruction).
- **Pipeline freeze:** During cache stalls, the entire pipeline is frozen — no stage advances.

---

## 📂 Project Structure

```
src/
├── Main.java                    Entry point
├── cache/
│   ├── AccessResult.java        Cache access result (data + latency)
│   ├── CacheConfig.java         Immutable cache-level config with validation
│   ├── CacheHierarchy.java      L1I → L2, L1D → L2 → Memory routing
│   ├── CacheLevel.java          Set-associative cache (LRU/FIFO eviction)
│   └── CacheLine.java           Single cache line (valid, dirty, tag, data)
├── common/
│   ├── Config.java              Latencies, cache defaults, forwarding toggle
│   ├── Instruction.java         Immutable instruction record (Java record)
│   ├── InstructionEncoder.java  32-bit encode/decode for memory storage
│   └── Opcode.java              All supported opcodes with utility methods
├── compiler/
│   ├── CompilationResult.java   Instructions + data items from assembly
│   ├── Compiler.java            Two-pass compiler (.data + .text)
│   ├── DataItem.java            Bytes to write at a memory address
│   ├── Lexer.java               Line tokenizer (strips comments)
│   └── Parser.java              Assembly → Instruction parser
├── core/
│   ├── Memory.java              128 KB byte-addressable memory
│   ├── Processor.java           Top-level simulator controller
│   ├── RegisterFile.java        32 integer registers (x0 = 0)
│   └── Stats.java               Performance + cache metrics
├── hazard/
│   ├── ForwardResult.java       Enum: NONE, FROM_EX_MEM, FROM_MEM_WB
│   ├── ForwardingUnit.java      Data bypass path resolution
│   └── HazardUnit.java          Stall detection (RAW, load-use, multi-cycle)
├── pipeline_registers/
│   ├── IF_ID.java               Instruction + PC + fetch latency
│   ├── ID_EX.java               Decoded fields + BTFNT prediction state
│   ├── EX_MEM.java              ALU result + branch resolution
│   └── MEM_WB.java              Final result + memory latency
└── pipeline_stages/
    ├── IF_Stage.java            Fetch via L1I cache or direct list
    ├── ID_Stage.java            Decode + BTFNT prediction
    ├── EX_Stage.java            ALU + forwarding + branch misprediction
    ├── MEM_Stage.java           Load/Store via L1D cache
    ├── WB_Stage.java            Register write-back
    └── PipelineController.java  Cycle orchestration + stall/flush logic
```

---

## ⚖️ Architectural Assumptions

1. **Half-cycle write-back:** Stages tick in reverse order (WB→MEM→EX→ID→IF), so WB writes to the register file before ID reads in the same cycle.
2. **BTFNT prediction:** Applied in ID stage. Backward branches redirect PC immediately; mispredictions are caught in EX and cause a 2-cycle flush.
3. **Cache miss serialization:** Concurrent IF + MEM misses are serialized (MEM first). Models a shared L2 with single-port memory arbitration.
4. **Write-back, write-allocate:** On a store miss, the block is fetched into cache, then modified. Dirty evictions propagate down the hierarchy.
5. **HALT drain:** After HALT is detected in EX, 3 drain cycles allow MEM and WB to retire trailing instructions.
6. **x0 immutability:** WB stage rejects all writes to x0.
7. **Forwarding precedence:** EX/MEM > MEM/WB when both match the same source register.
8. **64-byte blocks = 16 instructions:** Block size of 64 bytes holds 16 four-byte encoded instructions.

---

## 🎯 Design Philosophy

- **SOLID principles:** Single responsibility per class, consolidated defaults (DRY), private internals, validated config
- **Hardware-accurate:** Cycle-precise simulation, proper stall/flush semantics
- **Configurable:** All cache parameters, replacement policy, forwarding toggle, instruction latencies
- **Observable:** `output.txt` prints both the active configuration and resulting statistics
- **Extensible:** Adding new instructions or cache levels requires no structural redesign