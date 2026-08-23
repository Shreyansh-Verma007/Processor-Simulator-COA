# RISC-V Pipeline Simulator — Complete Interview Preparation Report

> **Purpose:** This document is your single-source-of-truth for defending every line of the RISC-V Pipeline Simulator project during a **software engineering internship interview**. It assumes the interviewer is a **senior engineer with 5-10 years of experience** who will probe algorithms, design decisions, edge cases, and scaling limitations.

---

# SECTION 1 — Elevator Pitch

## 30-Second Explanation (Screening Call)

> "I built a cycle-accurate, 5-stage in-order RISC-V processor simulator in Java with a full-stack web interface. The Java backend models a complete memory hierarchy — split L1I/L1D caches, a unified L2, virtual memory with TLB and swap space — plus pipeline hazard detection with data forwarding and BTFNT branch prediction. It has four modes: pipeline mode, trace replay mode, batch trace mode, and an HTTP API server mode. The API server exposes 9 REST endpoints so a React/TypeScript/Vite frontend can assemble programs, run simulations, and visualize results in the browser. 44 Java source files, 9 packages, zero external dependencies. The frontend deploys to Vercel, the backend to Heroku via Docker."

## 60-Second Explanation (Tell Me About Your Project)

> "I built this for my Computer Organization and Architecture course to deeply understand how a real processor works at the microarchitectural level. It's a cycle-accurate 5-stage pipeline simulator: Instruction Fetch, Decode, Execute, Memory Access, and Write Back — each modeled as a separate Java class communicating through pipeline register objects.
>
> The interesting engineering is in the hazard resolution: I implemented data forwarding from the EX/MEM and MEM/WB stages back to EX, load-use stall detection, multi-cycle execution for MUL (3 cycles) and DIV (4 cycles), and a BTFNT static branch predictor that eagerly redirects the PC on backward branches and flushes the pipeline on misprediction.
>
> The memory subsystem is the most complex part — a two-level set-associative cache hierarchy (L1I/L1D/L2) with write-back write-allocate policy and LRU/FIFO eviction, plus a full virtual memory unit with a 16-entry fully-associative TLB, flat page table, frame allocation, page replacement, and file-backed swap space. The whole thing uses PIPT (Physically Indexed, Physically Tagged) addressing, so cache lines are invalidated on frame eviction for correctness.
>
> I validated it against 10 trace workloads of ~715K instructions each, seeing IPC range from 0.164 on cache-warm runs down to 0.027 on adversarial traces with 0% TLB hit rate and 357K page faults."

## 2-Minute Explanation (Deep Technical Walkthrough)

> "Let me walk through the full system top to bottom.
>
> **Assembly to machine code:** The input is a RISC-V `.asm` file with `.data` and `.text` sections. A two-pass assembler — Lexer strips comments, Parser resolves labels, Compiler emits instructions — produces a list of `Instruction` records. Each instruction is encoded as a 32-bit integer (5-bit opcode, 5-bit rd, 5-bit rs1, 5-bit rs2, 12-bit immediate) and loaded into a simulated 128KB main memory.
>
> **Pipeline:** The `PipelineController` orchestrates the 5 stages in reverse order each cycle: WB writes back first, then MEM accesses the cache, then EX computes and resolves branches, then ID decodes and applies BTFNT prediction, then IF fetches through the L1I cache. Stages communicate via 4 pipeline register objects (IF/ID, ID/EX, EX/MEM, MEM/WB) which carry decoded fields, ALU results, and control signals.
>
> **Hazard resolution:** The `HazardUnit` detects three kinds of stalls: load-use (always 1-cycle stall even with forwarding), multi-cycle execution (pipeline frozen while MUL/DIV counts down), and RAW hazards without forwarding (stall until producer reaches WB). The `ForwardingUnit` implements two bypass paths: EX/MEM→EX and MEM/WB→EX, with EX/MEM taking priority. Forwarding explicitly excludes load instructions (their data isn't ready until after MEM).
>
> **Cache hierarchy:** `CacheHierarchy` manages L1I, L1D, and an optional L2. On a miss, it fetches the block from the next level down, installs it, and handles dirty evictions cascading through write-back. The key design is the stats policy: each pipeline request counts as exactly ONE L1 access, and internal block fills use `NoStats` methods to avoid inflating counters.
>
> **Virtual memory:** The `VirtualMemoryUnit` handles TLB lookup → page table walk → page fault → frame allocation → swap. On TLB miss, it walks the flat page table (10 cycles). On page fault (page not in physical memory), it allocates a frame (50 cycles). If physical memory is full, it evicts a page via LRU/FIFO, saves dirty data to an in-memory swap space backed by `swap.txt`. On frame eviction, the L1D cache is invalidated for PIPT correctness.
>
> **Trace mode:** The `TraceSimulator` reads pre-recorded memory access traces (L/S/ADD/MUL format), translates virtual addresses through the VM, accesses the L1D cache with physical addresses, and accumulates cycle counts. It reuses the same `HazardUnit`, `Config`, and `Stats` infrastructure as pipeline mode — zero code duplication.
>
> **Web layer:** `ApiServer.java` wraps the simulator in a lightweight HTTP server (`java Main --server`) using Java's built-in `com.sun.net.httpserver` — zero external dependencies. 9 REST endpoints cover status, ASM get/set, pipeline run, console/output/swap file fetch, trace upload, preset trace list, and named trace file fetch. A React + TypeScript + Vite frontend (deployed on Vercel) communicates with this backend via Axios, providing a code editor, real-time stats dashboard, pipeline diagram, trace upload UI, and the About/Architecture documentation pages.
>
> The Java backend is 44 source files across 9 packages, zero dependencies. The frontend is a separate TypeScript project in the `web/` directory."

## Layman Explanation (Non-Technical HR/Recruiter)

> "Imagine a processor is like a factory assembly line with 5 stations — each station does one step of processing an instruction (like 'add two numbers' or 'load data from memory'). I built a software simulation of this entire factory. It models everything: the assembly line itself, the hazards that happen when one station needs the result from another (like a bottleneck), the fast memory caches that sit close to the processor, and even the virtual memory system that the operating system uses to manage limited physical RAM. I tested it with real workloads and measured how efficiently different memory configurations perform — basically, I built a virtual computer inside a computer to study how hardware works."

## Technical Explanation (For Senior Engineer)

> "It's a cycle-accurate 5-stage in-order RISC-V scalar pipeline simulator (IF→ID→EX→MEM→WB) with configurable, precise stall/flush/drain semantics. Hazard resolution uses a `HazardUnit` for load-use and no-forwarding RAW detection, and a `ForwardingUnit` implementing EX/MEM→EX and MEM/WB→EX bypass paths (loads excluded from EX/MEM forwarding — they still need a 1-cycle stall). Branch prediction is static BTFNT with eager PC redirect in ID and 2-instruction flush on misprediction in EX. The memory subsystem is a null-safe 3-level hierarchy: split L1I/L1D + optional unified L2, all set-associative with LRU/FIFO eviction and write-back write-allocate. Dirty evictions cascade L1→L2→memory. Stats are carefully counted: one L1 access per pipeline request, one L2 access on L1 miss — internal fills and writebacks use `NoStats` paths. The trace replay mode layers a VM subsystem on top: 16-entry fully-associative DTLB, flat page table (2^20 entries for 4GB VA space), LRU/FIFO page replacement, in-memory swap HashMap with `swap.txt` persistence, and PIPT cache invalidation on frame eviction via `CacheHierarchy.invalidateFrame()`. Everything is driven by a single `Config` class — latencies, cache geometry, VM sizes, replacement policies — ensuring physical architecture consistency between pipeline and trace modes. Two-pass assembler handles `.data`/`.text` sections, labels, pseudo-instructions, and 32-bit instruction encoding for cache-based fetch. **On top of the simulator core, I added a full-stack web layer: `ApiServer.java` wraps all modes behind 9 REST endpoints using Java's built-in `HttpServer` (zero added dependencies), and a React/TypeScript/Vite single-page app provides a code editor, live stats dashboard, trace upload UI, pipeline diagram, and documentation pages. Backend deploys to Heroku via Docker; frontend deploys to Vercel.** 44 Java files, 9 packages, zero external Java dependencies."

---

# SECTION 2 — High-Level Architecture

## Complete Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ENTRY POINT                                    │
│  Main.java ──→ runPipelineMode() | runTraceMode() | runBatch() | --server   │
└──┬───────────────────────────┬────────────────────────────┬─────────────────┘
   │                           │                            │
   ▼                           ▼                            ▼
┌──────────────┐    ┌────────────────┐        ┌─────────────────────────────┐
│ PIPELINE MODE│    │  TRACE MODE    │        │       API SERVER MODE       │
├──────────────┤    ├────────────────┤        ├─────────────────────────────┤
│ Lexer        │    │ TraceParser    │        │ ApiServer.java              │
│ Parser       │    │                │        │  GET  /api/status           │
│ Compiler     │    │   ↓            │        │  GET  /api/asm              │
│   ↓          │    │ List<TraceInstr│        │  POST /api/asm              │
│ Compilation  │    │   ↓            │        │  POST /api/run              │
│ Result       │    │ TraceSimulator │        │  GET  /api/console          │
│   ↓          │    │                │        │  GET  /api/output           │
│ Processor    │    │   ↓            │        │  GET  /api/swap             │
│   ↓          │    │ executeInstr() │        │  POST /api/trace            │
│ PipelineCtrl │    │                │        │  GET  /api/traces           │
│  ┌───────────┤    │  ┌────────────┤│        │  GET  /api/trace-file       │
│  │ IF_Stage  │    │  │ VMU        ││        └────────────────┬────────────┘
│  │ ID_Stage  │    │  │  TLB       ││                         │
│  │ EX_Stage  │    │  │  PageTable ││                         ▼
│  │ MEM_Stage │    │  │  SwapSpace ││        ┌─────────────────────────────┐
│  │ WB_Stage  │    │  │            ││        │     WEB FRONTEND (React)    │
│  ├───────────┤    │  ├────────────┤│        ├─────────────────────────────┤
│  │ IF_ID     │    │  │ ID_EX      ││        │ SimulatorPage (code editor) │
│  │ ID_EX     │    │  │ EX_MEM     ││        │ TraceReplayPage (upload UI) │
│  │ EX_MEM    │    │  │ MEM_WB     ││        │ ArchitecturePage (diagram)  │
│  │ MEM_WB    │    │  │            ││        │ AboutPage (docs)            │
│  ├───────────┤    │  ├────────────┤│        │ StatsPanel (metrics)        │
│  │ HazardUnit│    │  │ HazardUnit ││        │ SwapPanel (swap viewer)     │
│  │ FwdUnit   │    │  │            ││        │ PipelineDiagram (SVG)       │
│  ├───────────┤    │  ├────────────┤│        └─────────────────────────────┘
│  │ CacheHier.│    │  │ CacheHier. ││
│  │ L1I/L1D   │    │  │ L1D only   ││
│  │ L2 → Mem  │    │  │            ││
│  ├───────────┤    │  ├────────────┤│
│  │ Memory    │    │  │ Memory     ││
│  │ RegFile   │    │  │ Stats      ││
│  │ Stats     │    │  │            ││
└──┴───────────┘    └──┴────────────┘│
         │                     │
         ▼                     ▼
    ┌──────────┐        ┌──────────────┐
    │output.txt│        │traces_output/│   ← results
    │console.txt        │swap.txt      │
    └──────────┘        └──────────────┘
```

Here is a high-level overview of the architecture of your project. It is built as a full-stack application, split into a modern web frontend and a highly optimized, raw Java backend.

The architecture can be broken down into three main layers: The Web Frontend, The API Server, and The Core Processor Simulator.

1. The Web Frontend (User Interface)
Tech Stack: React 18, TypeScript, Vite, TailwindCSS.
Hosting: Deployed statically to Vercel.
Role: This is the interactive dashboard. It provides a Monaco-based code editor for writing assembly, file upload zones for large trace files, and visual dashboards for statistics and pipeline diagrams.
Communication: It talks to the backend via standard HTTP REST calls using Axios. In development, Vite proxies these calls; in production, it calls the live backend URL.
2. The API Server (Backend Gateway)
Tech Stack: Pure Java 17+ (No Spring Boot, no Maven/Gradle dependencies).
Hosting: Deployed via Docker on Railway.
Role: Acts as the bridge between the web UI and the simulator. It uses Java's built-in com.sun.net.httpserver.HttpServer.
Concurrency: It uses a CachedThreadPool to handle multiple people asking for files at the same time. However, to protect the simulator from crashing, it uses an AtomicBoolean flag to lock the simulation — if two people click "Run" at the exact same time, the second person gets a 409 Conflict (Server Busy) error.
3. The Core Simulator (The Engine)
This is where the actual computer architecture happens. It is entirely written in Java and is broken into several subsystems:

A. The Compiler / Assembler
Takes raw text assembly (input.asm), strips comments, resolves branch labels, and separates .data from .text.
Uses the InstructionEncoder to convert the text into raw 32-bit machine code (integers) and places it into the Memory array.
B. The 5-Stage Pipeline (PipelineController)
Orchestrates the classic 5 stages: IF (Fetch) → ID (Decode) → EX (Execute) → MEM (Memory) → WB (Write Back).
Reverse Ticking: It executes the stages in reverse order (WB first, IF last) during every clock cycle. This guarantees mathematical cycle accuracy and ensures no instruction accidentally skips a clock cycle.
C. Hazard & Forwarding Units
Hazard Unit: Watches for things that require the pipeline to freeze (like a load-use hazard or a multi-cycle multiply). If detected, it injects NOP bubbles to stall the pipeline.
Forwarding Unit: Intercepts data from the EX/MEM and MEM/WB registers and feeds it straight back into the ALU to prevent data stalls.
Branch Prediction: Uses BTFNT (Backward Taken, Forward Not Taken) to guess if loops will repeat, flushing the pipeline if it guesses wrong.
D. The Memory & Cache Hierarchy
Caches: Split L1I (Instruction) and L1D (Data) caches, with an optional unified L2 cache.
Null-Safe Routing: Missing cache levels are simply set to null, and the hierarchy dynamically bypasses them without needing duplicate code.
E. Virtual Memory System
Before the caches can even be accessed, virtual addresses must be translated to physical addresses.
TLB: A high-speed cache for recent translations (1-cycle lookup).
Page Table & VMU: Maps virtual pages to 64 physical RAM frames. If RAM gets full, it uses an LRU (Least Recently Used) algorithm to evict a frame to the swap.txt disk file and loads the new page in.
How Data Flows (Example: Clicking "Run")
You write ADD x1, x2, x3 in the React Frontend and click Run.
The frontend sends an HTTP POST request to the API Server.
The server locks the thread and hands the text to the Compiler.
The compiler turns it into binary and puts it in the Memory.
The PipelineController starts ticking cycle-by-cycle.
The instruction flows through the caches, hazards, and stages until it reaches HALT.
The server gathers the results from the Stats object, unlocks the thread, and sends the JSON back to React to draw the graphs!


## End-to-End Data Flow — API Server Mode

```
Step 1: User interacts with React Frontend, writes ASM, and clicks "Run"
    ↓
Step 2: Frontend API calls (Axios)
    → POST /api/asm (saves editor content to input.asm on server)
    → POST /api/run (triggers the simulation on the backend)
    ↓
Step 3: Server ApiServer.java handles /api/run
    → Initiates compilation and pipeline simulation
    ↓
Step 4: Lexer.tokenize("input.asm")
    → Reads file line-by-line, strips comments, returns ArrayList<String>
    ↓
Step 5: Compiler.buildSymbolTable(lines)  [PASS 1]
    → Tracks .data/.text sections, records label → byte_address mappings
    ↓
Step 6: Parser.parseText(lines) + Compiler.parseData(lines)  [PASS 2]
    → Parser: converts text lines to Instruction records, resolves labels/ABIs
    → Compiler: converts .data directives to little-endian byte arrays
    → Returns CompilationResult
    ↓
Step 7: Processor(cfg) constructor
    → Creates Memory, RegisterFile, Stats, and CacheHierarchy
    ↓
Step 8: Processor.run(compilationResult)
    → Loads data items and encoded instructions into memory
    → Calls PipelineController.run()
    ↓
Step 9: PipelineController simulation loop (each cycle)
    9a. Check cache stall counters
    9b. HazardUnit.needsStall() — detect load-use, multi-cycle, RAW
    9c. WB_Stage.tick() — write to register file
    9d. MEM_Stage.tick() — route LW/SW through CacheHierarchy
    9e. EX_Stage.tick() — resolve forwarding, ALU, branch prediction
    9f. ID_Stage.tick() / IF_Stage.tick() — fetch/decode if no stall
    ↓
Step 10: Server generates output files
    → StatsPrinter writes to `output.txt` (stats) and `console.txt` (pipeline log)
    → Return JSON success payload `{"ok": true}` to frontend
    ↓
Step 11: Frontend fetches results
    → GET /api/console, GET /api/output, GET /api/swap
    → Renders pipeline diagram, cache stats, and register state in the UI
```

## Folder/File Structure

```
src/
├── Main.java                         Entry point: dispatches pipeline, trace, batch, and --server modes
├── ApiServer.java                    HTTP API server: 9 REST endpoints, uses java.com.sun.net.httpserver
│
├── common/                           Shared infrastructure (no dependencies on pipeline/trace/vm)
│   ├── Config.java                   Unified config: latencies, forwarding, cache params, VM params
│   ├── Instruction.java              Java record: opcode + rd + rs1 + rs2 + immediate; factory methods per type
│   ├── InstructionEncoder.java       Encode/decode Instruction ↔ 32-bit int for memory storage
│   ├── Opcode.java                   Enum of 21 opcodes with utility: isBranch(), isLoad(), writesBack()
│   └── StatsPrinter.java             Printf-based formatted stats output for both pipeline and trace
│
├── compiler/                         Two-pass RISC-V assembler
│   ├── Compiler.java                 Pass 1: symbol table; Pass 2: emit instructions + data items
│   ├── CompilationResult.java        Container for instructions list + data items list
│   ├── DataItem.java                 Address + byte[] pair for .data segment
│   ├── Lexer.java                    Line tokenizer, comment stripping (respects quoted strings)
│   └── Parser.java                   Instruction parser: ABI names, memory syntax, pseudo-instructions
│
├── core/                             Processor fundamentals
│   ├── Processor.java                Top-level orchestrator: wires memory, registers, cache, pipeline
│   ├── Memory.java                   128KB word-addressable memory with byte-level R/W and bounds checks
│   ├── RegisterFile.java             32 integer registers, x0 hardwired to 0, SP initialized at 0x0FFF
│   └── Stats.java                    All metrics: cycles, stalls, flushes, retired, cache, VM stats
│
├── cache/                            Memory hierarchy
│   ├── CacheHierarchy.java           L1I/L1D → [L2] → Memory; null-safe L2; stats-counted reads/writes
│   ├── CacheLevel.java               Set-associative cache: LRU/FIFO eviction, insert/lookup/invalidate
│   ├── CacheConfig.java              Immutable geometry: size, block, associativity, latency, policy
│   ├── CacheLine.java                Valid, dirty, tag, data[], lastUsed, insertOrder
│   └── AccessResult.java             data + latencyCycles pair returned from every cache/memory access
│
├── pipeline_stages/                  5-stage pipeline implementation
│   ├── PipelineController.java       Main loop: stall/flush/drain logic, cache stall counters
│   ├── IF_Stage.java                 Cache-aware fetch: L1I path or direct-from-list (Phase 1 compat)
│   ├── ID_Stage.java                 Decode + BTFNT: backward branches predicted taken, redirect PC
│   ├── EX_Stage.java                 ALU, branch resolution, forwarding resolution, misprediction detect
│   ├── MEM_Stage.java                Load/store routing through cache hierarchy (LW/LB/SW/SB)
│   └── WB_Stage.java                 Register writeback + instructionsRetired increment
│
├── pipeline_registers/               Inter-stage communication data objects
│   ├── IF_ID.java                    Instruction + PC + isNop + fetchLatencyLeft
│   ├── ID_EX.java                    Decoded fields + latencyCyclesLeft + BTFNT prediction
│   ├── EX_MEM.java                   ALU result + branch resolution + misprediction signals
│   └── MEM_WB.java                   Final result + memLatencyLeft
│
├── hazard/                           Pipeline correctness
│   ├── HazardUnit.java               Detects: multi-cycle stall, load-use, no-forwarding RAW
│   ├── ForwardingUnit.java           EX/MEM→EX and MEM/WB→EX bypass paths (loads excluded from EX/MEM)
│   └── ForwardResult.java            Enum: NONE, FROM_EX_MEM, FROM_MEM_WB
│
├── trace/                            Trace replay subsystem
│   ├── TraceSimulator.java           VM + cache simulation engine with hazard stall tracking
│   ├── TraceParser.java              Line parser: L/S/ADD/MUL/BEQ/BNE/JAL → TraceInstruction
│   └── TraceInstruction.java         Type + address + rd + rs1 + rs2; factory methods per type
│
└── vm/                               Virtual memory subsystem
    ├── VirtualMemoryUnit.java        TLB → PageTable → Fault → Frame alloc → Swap; PIPT invalidation
    ├── TLB.java                      Fully-associative, LRU/FIFO eviction, dirty bit tracking
    ├── TLBEntry.java                 VPN → PFN mapping + valid + dirty + lastUsed + insertOrder
    ├── PageTable.java                Flat table indexed by VPN; map/unmap/findVPNByFrame
    ├── PageTableEntry.java           Valid + frameNumber + dirty + lastUsed + insertOrder
    └── TranslationResult.java        Physical address + total translation latency

web/                                  React + TypeScript + Vite frontend (separate project)
├── src/
│   ├── App.tsx                       Root: Navbar + page routing (simulator/trace/architecture/about)
│   ├── api/client.ts                 Axios API client: getAsm, saveAsm, runSimulation, runTrace, listTraces
│   ├── hooks/useSimulator.ts         Stateful hook: ASM code, run status, output content, backend health
│   ├── pages/
│   │   ├── SimulatorPage.tsx         Split-pane: code editor ↔ console/stats/swap/raw output tabs
│   │   ├── TraceReplayPage.tsx       Trace upload + preset trace list + live stats dashboard
│   │   ├── ArchitecturePage.tsx      Pipeline diagram + instruction set table + memory subsystem docs
│   │   └── AboutPage.tsx             Project hero + feature grid + 3-phase timeline
│   └── components/
│       ├── Navbar.tsx                Side navigation: page links + backend online indicator
│       ├── CodeEditor.tsx            Syntax-highlighted RISC-V ASM editor (textarea-based)
│       ├── StatsPanel.tsx            Parsed stats display: stat cards + cache table + config
│       ├── PipelineDiagram.tsx       SVG 5-stage pipeline visualization with hazard annotations
│       ├── ConsolePanel.tsx          Scrollable terminal for console.txt output
│       ├── SwapPanel.tsx             Parsed swap.txt viewer with VPN→frame table
│       ├── RunButton.tsx             Animated run/loading/done button with elapsed time
│       └── OutputTabs.tsx            Tab bar: Console / Stats / Swap / Raw
├── Dockerfile                        Multi-stage: builds Java backend + serves combined app
├── heroku.yml                        Heroku Docker deployment config
└── vercel.json                       Vercel SPA routing config (rewrites → index.html)
```

## Architectural Layers

| Layer | Responsibility | Key Classes |
|-------|---------------|-------------|
| **Entry / CLI** | Argument parsing, mode dispatch, I/O redirection | `Main.java` |
| **HTTP API Server** | REST endpoints wrapping all simulator modes | `ApiServer.java` |
| **Assembler** | .asm → Instruction list + data items | `Lexer`, `Parser`, `Compiler`, `CompilationResult`, `DataItem` |
| **Pipeline Engine** | Cycle-accurate simulation loop, stage orchestration | `PipelineController`, `IF_Stage` through `WB_Stage` |
| **Inter-Stage Data** | Communication between pipeline stages per cycle | `IF_ID`, `ID_EX`, `EX_MEM`, `MEM_WB` |
| **Hazard Resolution** | Correctness enforcement: stalls and forwarding | `HazardUnit`, `ForwardingUnit`, `ForwardResult` |
| **Cache Hierarchy** | Multi-level set-associative caching | `CacheHierarchy`, `CacheLevel`, `CacheConfig`, `CacheLine`, `AccessResult` |
| **Virtual Memory** | Address translation, paging, swap | `VirtualMemoryUnit`, `TLB`, `TLBEntry`, `PageTable`, `PageTableEntry`, `TranslationResult` |
| **Trace Replay** | Workload analysis without full pipeline | `TraceSimulator`, `TraceParser`, `TraceInstruction` |
| **Shared Infrastructure** | Config, instruction model, stats, output | `Config`, `Instruction`, `InstructionEncoder`, `Opcode`, `Stats`, `StatsPrinter` |
| **Web Frontend** | Browser UI: editor, stats, trace upload, docs | React + TypeScript + Vite (`web/src/`) |

## Design Decisions Summary

1. **Reverse-order stage ticking (WB→MEM→EX→ID→IF)** — Ensures each stage reads the *old* pipeline register values and writes new ones, preventing same-cycle data races without double-buffering
2. **Null-safe L2 / L1I** — `CacheHierarchy` dynamically bypasses missing cache levels; trace mode uses only L1D, pipeline mode uses both L1I and L1D, with no duplicate code paths
3. **`NoStats` methods** — Internal block fills and write-backs don't inflate hit/miss counters; only pipeline-initiated requests count
4. **PIPT cache invalidation on frame eviction** — `CacheHierarchy.invalidateFrame()` wipes all L1D lines in the evicted frame's address range, preventing stale data reads
5. **In-memory swap HashMap** — Dirty page data is stored in a `Map<Integer, int[]>` keyed by VPN; `swap.txt` is a post-simulation dump, not a runtime I/O path
6. **`Instruction` as a Java record** — Immutable, auto-generated equals/hashCode/toString; factory methods (`rType()`, `iType()`, etc.) enforce correct field semantics per instruction format
7. **Shared `Config` / `Stats` / `HazardUnit` across modes** — Trace mode reuses the exact same hazard detection, config parameters, and stats reporting as pipeline mode — zero code duplication
8. **32-bit custom encoding (not standard RISC-V encoding)** — Simplified layout (5-bit opcode + 5+5+5+12) to keep the encoder/decoder trivial; the ISA subset doesn't need full RV32I encoding complexity
9. **`ApiServer` uses Java's built-in `HttpServer` — zero external dependencies** — `com.sun.net.httpserver` ships with the JDK; no Maven/Gradle needed. The server uses a `CachedThreadPool` for concurrent requests and an `AtomicBoolean` running flag to reject concurrent simulation attempts (409 Conflict).
10. **`runPipelinePublic()` made package-accessible** — The pipeline entry point is declared `public static` so `ApiServer.RunHandler` can call it directly without reflection, sharing I/O redirection (stdout → `console.txt`) cleanly.
11. **Frontend/backend decoupling via CORS + proxy** — The Vite dev server proxies `/api` to `localhost:8080`, so the frontend works identically in dev and production. CORS headers (`Access-Control-Allow-Origin: *`) are set on every response so the Vercel-deployed frontend can call the Heroku-hosted backend.

---

# SECTION 3 — Deep Code Walkthrough

## Module: `common/Config.java` — Unified Configuration

### Why It Exists
Every tunable parameter in the entire system — instruction latencies, forwarding toggle, cache geometry for 3 levels, VM sizes, TLB entries, replacement policies — lives in this single class. This ensures pipeline mode and trace mode use identical physical architecture parameters.

### Key Design
- Default cache: L1D only (4KB, direct-mapped, 1-cycle hit), no L1I, no L2, 10-cycle memory latency
- Default VM: 4GB virtual, 256KB physical (64 frames × 4KB pages), 16-entry DTLB, LRU
- Latency map: `HashMap<Opcode, Integer>` — ADD/SUB/shifts = 1 cycle, MUL = 3, DIV = 4
- `hasCacheConfig()` returns true if either L1I or L1D is non-null

### Edge Cases
- Unknown opcode → `getLatency()` returns 1 (safe default)
- L1I set to null → pipeline fetches directly from instruction list (backward compatibility)

### Interview Questions
- *Q: Why use a HashMap for latencies instead of a switch statement in the EX stage?*
  → A: Separation of concerns. The Config owns "how many cycles does ADD take?" — the EX stage shouldn't hardcode that. It also makes the latency configurable without modifying pipeline code.
- *Q: The default Config has L2 size = 0 and L2 = null. What happens if someone sets L2 size to a positive value but forgets to create the CacheConfig?*
  → A: The `DEF_L2_SIZE = 0` is just a default constant. The actual `l2` field is set to `null` in the constructor. The size constant isn't even used to construct a CacheConfig — the code explicitly sets `this.l2 = null`. So the constant is dead code unless someone modifies the constructor.

---

## Module: `common/InstructionEncoder.java` — 32-Bit Encoding

### Why It Exists
When the cache hierarchy is enabled, instructions must be stored *in memory* (not just in a Java `List<Instruction>`). The IF stage needs to fetch raw 32-bit words from the L1I cache and decode them back into `Instruction` objects. This encoder/decoder bridges that gap.

### Bit Layout
```
[31–27] opcode    (5 bits, ordinal of Opcode enum)
[26–22] rd        (5 bits)
[21–17] rs1       (5 bits)
[16–12] rs2       (5 bits)
[11–0]  immediate (12 bits, sign-extended on decode for branches/loads/stores)
```

### Key Detail: Sign Extension
On decode, the 12-bit immediate is sign-extended for branches, ADDI, JAL, loads, and stores (instructions that use PC-relative or signed offsets). The check is `if ((imm & 0x800) != 0) imm |= 0xFFFFF000` — this fills the upper 20 bits with 1s for negative offsets.

### Limitation
12-bit immediate limits branch offsets to ±2048 instructions and data offsets to ±2048 bytes. This is sufficient for the project's test programs but would fail for larger programs. Real RISC-V uses different encoding widths per format (B-type has 13-bit offset, J-type has 21-bit).

### Interview Questions
- *Q: Why use `ordinal()` for the opcode instead of a fixed encoding table?*
  → A: Simpler implementation. Since we control both the encoder and decoder, and they use the same `Opcode` enum, the ordinal is stable. The tradeoff is fragility — if someone reorders the enum constants, all previously encoded instructions break. In a real ISA, you'd use a standardized encoding map.
- *Q: Why only 12 bits for the immediate? Real RISC-V gives B-type 13 bits.*
  → A: This is a simplified custom encoding, not standard RV32I. 12 bits covers our test programs. The simplicity of a uniform field layout (same bit positions for every format) outweighed the need for larger offsets in this academic context.

---

## Module: `pipeline_stages/PipelineController.java` — The Simulation Loop

### Why It Exists
This is the heart of the simulator. It orchestrates the 5 pipeline stages, manages cache stall counters, detects and responds to hazards and branch mispredictions, and enforces termination conditions.

### Key Constants
- `DRAIN_THRESHOLD = 3` — After HALT is encountered, run 3 more cycles to let in-flight instructions drain through the pipeline
- `MAX_CYCLE_LIMIT = 100,000` — Safety valve to prevent infinite loops

### Function: `run()` — Step-by-Step Logic

**Per cycle:**

1. **Cache stall check:** If `memStallCycles > 0` or `ifStallCycles > 0`, decrement the appropriate counter (MEM has priority), count as stall cycle, skip all stage logic, and `continue`
2. **Hazard detection:** `HazardUnit.needsStall()` checks load-use, multi-cycle, and RAW dependencies
3. **Stage ticking in WB→MEM→EX order:**
   - `wbStage.tick(memWb)` — writes back the *previous* MEM/WB register
   - `memStage.tick(exMem)` → produces *new* MEM/WB (may carry `memLatencyLeft`)
   - `exStage.tick(idEx)` → produces *new* EX/MEM (carries branch resolution, misprediction flags)
4. **MEM stall extraction:** If `newMemWb.memLatencyLeft > 0`, transfer it to `memStallCycles` and zero it out
5. **Branch/stall resolution:**
   - Misprediction → flush IF/ID and ID/EX (replace with NOPs), set PC to recovery address
   - JAL → flush and redirect to jump target
   - Stall without misprediction → inject NOP bubble into ID/EX
   - Normal flow → run ID_Stage.tick() to decode, then IF_Stage.tick() to fetch
6. **Termination:** HALT + 3 drain cycles, or pipeline fully drained (all stages NOP), or cycle limit

### Why Reverse Order?
In real hardware, all stages execute simultaneously and latch results at the clock edge. In sequential simulation, ticking WB first means it reads the *old* MEM/WB register. Then MEM reads the *old* EX/MEM and produces the *new* MEM/WB. This prevents the combinational loop where a later stage reads a value that was just written by an earlier stage in the same cycle.

### Edge Cases
- **Concurrent IF and MEM cache misses:** MEM miss is serviced first (its stall counter is decremented before IF's). This correctly models the structural hazard where MEM and IF compete for the cache/memory bus.
- **Stall + misprediction in the same cycle:** The misprediction takes priority (the stall was for an instruction that's about to be flushed anyway).

### Interview Questions
- *Q: Why do you save both `oldMemWb` and `newMemWb`?*
  → A: The EX stage's forwarding logic needs to check *both* the current MEM/WB (data just leaving MEM) and the previous MEM/WB (data that was written back last cycle). `oldMemWb` is the pre-tick value; `newMemWb` is the post-tick value.
- *Q: What happens if a branch misprediction occurs while a cache stall is active?*
  → A: Cache stalls are checked first (the `continue` on line 79 skips all stage logic). The misprediction can only be detected after the EX stage ticks, which requires the cache stall to have fully resolved. So the misprediction is naturally delayed until the stall completes — which is architecturally correct.

---

## Module: `hazard/HazardUnit.java` — Stall Detection

### Why It Exists
Without hazard detection, the pipeline would produce incorrect results whenever a consumer instruction reads a register that a prior instruction hasn't finished writing. This unit detects all cases that *require stalling* (as opposed to forwarding).

### Five Cases Handled

| Case | Condition | Action |
|------|-----------|--------|
| 1. Multi-cycle EX | `idEx.latencyCyclesLeft > 0` | Stall (MUL/DIV still computing) |
| 2. Load-use | ID/EX has a load, and IF/ID uses its `rd` | Stall 1 cycle (load data unavailable until after MEM) |
| 3. RAW (no fwd, EX) | Forwarding OFF, producer in ID/EX | Stall until producer advances |
| 4. RAW (no fwd, MEM) | Forwarding OFF, producer in EX/MEM | Stall until producer advances |
| 5. RAW (no fwd, WB) | Forwarding OFF, producer in MEM/WB | **Not stalled** — register file has internal forwarding (write-first-half, read-second-half) |

### Why Case 5 is Commented Out
The code has an explicit comment explaining the assumption of register file internal forwarding. In real hardware, the register file can be designed so that a write in the first half of the clock cycle is visible to a read in the second half. This means if the producer is in WB (about to write) and the consumer is in ID (about to read), the consumer will see the new value without a stall.

### Interview Questions
- *Q: Why does load-use always stall even with forwarding enabled?*
  → A: A load instruction's data isn't available until the *end* of the MEM stage. Even with forwarding, the earliest the data can be forwarded is from the MEM/WB register — but that's one cycle *after* the EX stage of the consumer instruction needs it. So there's an unavoidable 1-cycle bubble.
- *Q: If forwarding is disabled, how many stall cycles does a RAW hazard cost?*
  → A: It depends on the distance. Producer in EX (1 stage ahead of consumer in ID) → 2 stall cycles. Producer in MEM (2 stages ahead) → 1 stall cycle. Producer in WB → 0 (internal forwarding). The HazardUnit keeps stalling as long as the dependency exists.

---

## Module: `hazard/ForwardingUnit.java` — Data Bypass

### Why It Exists
Without forwarding, every RAW hazard requires stalling until the producer reaches WB and writes to the register file. With forwarding, the ALU result can be bypassed directly from the EX/MEM or MEM/WB register to the EX stage input, eliminating most stalls.

### Forwarding Priority
```
EX/MEM → EX   (highest priority — most recent value)
MEM/WB → EX   (if EX/MEM doesn't match)
oldMEM/WB → EX (checked in EX_Stage, not ForwardingUnit)
Register file   (lowest priority — may be stale)
```

### Key Detail: Loads Excluded from EX/MEM Forwarding
The condition `&& !exMem.opcode.isLoad()` prevents forwarding from EX/MEM when the producer is a load. Load data isn't available until after MEM completes, so EX/MEM only carries the *address* (aluResult), not the loaded *data*. This forces a load-use stall.

### Interview Questions
- *Q: What happens if both EX/MEM and MEM/WB match the same source register?*
  → A: EX/MEM takes priority — it has the *more recent* value. Example: `ADD x1, x2, x3` followed by `SUB x1, x4, x5` followed by `AND x6, x1, x7`. When AND is in EX, both EX/MEM (SUB) and MEM/WB (ADD) have `rd = x1`. We want SUB's result (the latest write), which is in EX/MEM.

---

## Module: `cache/CacheLevel.java` — Set-Associative Cache

### Why It Exists
Models a single level of set-associative cache (used for L1I, L1D, and L2). Handles address decomposition, tag matching, LRU/FIFO victim selection, and dirty eviction.

### Address Decomposition
```
Tag       = address / (blockSize × numSets)
Set Index = (address / blockSize) mod numSets
Offset    = (address / 4) mod blockSizeWords
```
All divisions use `Integer.divideUnsigned` or `Math.floorMod` to handle large unsigned addresses correctly.

### Function: `insert()` — Block Installation with Eviction

1. Compute set index and tag
2. Scan the set for an invalid (empty) slot → fill it, return null (no eviction)
3. If all ways are valid → `selectVictim()` based on policy
4. If victim is dirty → create `EvictionResult(address, data.clone())` for write-back
5. Fill the victim slot with the new block

### LRU vs FIFO
- **LRU:** Victim = line with smallest `lastUsed` timestamp. `lastUsed` is updated on every access (read or write).
- **FIFO:** Victim = line with smallest `insertOrder` timestamp. `insertOrder` is set only on insertion and never updated on access.

Both use a monotonically increasing `clock` counter — no actual timestamps, just logical ordering.

### Function: `invalidateFrameLines()` — PIPT Correctness

When a physical frame is evicted by the VMU, all cache lines mapping to addresses within that frame must be invalidated. Otherwise, the PIPT cache would serve stale data from the old page that previously occupied that frame.

The method iterates over all blocks in the frame (`frameSizeBytes / blockSize`), computes the set and tag for each block address, and invalidates matching lines. Dirty lines are silently discarded — the VMU has already saved the frame's data to swap before calling this.

### Time Complexity
- `lookup()`: O(A) where A = associativity (scan one set)
- `insert()`: O(A) for victim selection
- `invalidateFrameLines()`: O(B × A) where B = blocks per frame (4KB / 64B = 64 blocks for default config)

### Interview Questions
- *Q: Why use `Integer.divideUnsigned` for set index?*
  → A: Physical addresses from the VM can be large unsigned values that exceed `Integer.MAX_VALUE`. Standard Java `/` treats ints as signed, which would produce negative results for addresses ≥ 2^31. `divideUnsigned` treats the bits as an unsigned value.
- *Q: What's the difference between `lookup()` and `lookupNoStats()`?*
  → A: `lookup()` increments the hit or miss counter. `lookupNoStats()` does not. This distinction is critical — when the CacheHierarchy fetches a full block on a miss and installs it, the individual word reads during that fill should not count as additional cache accesses.

---

## Module: `cache/CacheHierarchy.java` — Multi-Level Cache

### Why It Exists
Orchestrates the full cache hierarchy: L1I → [L2] → Memory and L1D → [L2] → Memory. Manages block fetching on misses, dirty eviction cascading, and the stats-counting policy.

### Key Function: `fetchBlockToL1()`

This is the most complex method. On an L1 miss:

1. If L2 exists: probe L2 with a stats-counted `readWord()`
   - L2 hit → read full block using `readWordNoStats()`, latency = L2 latency
   - L2 miss → read from main memory, latency = L2 latency + memory latency, install block into L2
2. If no L2: read directly from memory, latency = memory latency
3. Install the fetched block into L1 via `l1.insert()`
4. If L1 eviction produces a dirty block → write back to L2 (or memory if no L2)

### Write-Back Cascade
Dirty evictions can cascade: L1 eviction → write-back to L2 → if L2 eviction is also dirty → write-back to memory. The `writeBackToL2()` method handles the case where the evicted L1 block must be overlaid onto the existing L2 block (since L2 blocks may be larger than L1 blocks).

### Stats Policy (Critical Interview Point)
Each pipeline request counts as **exactly ONE L1 access** (hit or miss). If L1 misses, **exactly ONE L2 access** is counted. All internal operations (block fills, word reads within a block, write-backs) use `NoStats` methods. This prevents a single load instruction from being counted as 16 L1 accesses (one per word in a 64-byte block).

### Interview Questions
- *Q: What happens if L1 and L2 have different block sizes?*
  → A: The code handles this. When installing into L2 after an L2 miss, it fetches an L2-sized block from memory (which may be larger than the L1 block). When writing back a dirty L1 block to L2, it overlays the L1 block data onto the correct offset within the L2 block. The key calculation is `l1Offset = (addr - l2BlockStart) / 4`.
- *Q: Why is the `invalidateFrame()` method on CacheHierarchy instead of CacheLevel?*
  → A: Because the VMU doesn't know (and shouldn't know) about individual cache levels. It calls `cacheHierarchy.invalidateFrame()`, and the hierarchy delegates to L1D. In future, if L2 also needed invalidation, the hierarchy method would handle both.

---

## Module: `vm/VirtualMemoryUnit.java` — Virtual Memory

### Why It Exists
Translates virtual addresses to physical addresses, manages the illusion of a large virtual address space (4GB) backed by limited physical memory (256KB = 64 frames), and handles page faults with swap space.

### Function: `translateAddress()` — The Core Flow

1. Extract VPN and offset from virtual address (unsigned division)
2. **TLB lookup** — charge `tlbHitLatency` (1 cycle) regardless of hit/miss
3. **TLB hit** → get PFN, mark dirty if store, update PTE's LRU timestamp
4. **TLB miss** → charge `pageWalkLatency` (10 cycles), walk the page table
   - **Page table hit** → update LRU timestamp, get PFN
   - **Page fault** → charge `pageFaultLatency` (50 cycles), allocate frame, restore from swap or zero-fill
5. On TLB miss, insert the mapping into the TLB
6. Compute physical address = PFN × pageSize + offset

### Function: `evictPage()` — Page Replacement

1. Scan ALL valid PTEs (full page table scan) to find the victim with the smallest `lastUsed` (LRU) or `insertOrder` (FIFO)
2. If victim is dirty (checked in both PTE and TLB), save to swap
3. Invalidate in TLB and page table
4. Return the freed frame number

### Swap Space Design
- `Map<Integer, int[]> swapSpace` — VPN → word array of the page's data
- `saveToSwap()`: reads every word from physical memory in the frame's address range, stores in the HashMap
- `restoreOrZeroFrame()`: if the VPN has swap data, write it back to the new frame; otherwise zero-fill
- `writeSwapFile()`: post-simulation dump to `swap.txt` for debugging

### Tradeoff: Full Page Table Scan for Eviction
The eviction algorithm scans all 2^20 = 1,048,576 PTEs to find the victim. This is O(N) where N = number of pages in the virtual address space. An alternative would be to maintain a linked list of mapped pages (O(1) eviction), but the full scan is simpler and sufficient for simulation purposes — it's not modeling real hardware eviction circuitry.

### Interview Questions
- *Q: Why is the TLB hit latency charged even on a TLB miss?*
  → A: In real hardware, you must probe the TLB before you know whether it's a hit or miss. The probe itself costs 1 cycle. On a miss, the page walk latency is *additional*.
- *Q: Why check dirty bits in both the PTE and TLB?*
  → A: A store instruction might dirty a page while the mapping is still in the TLB. The TLB entry gets its dirty bit set, but the PTE might not have been updated yet (the TLB acts as a cache of the PTE). On eviction, we must check both to avoid losing dirty data.
- *Q: The `evictPage()` method scans 1M entries. Isn't that slow?*
  → A: For a simulator, this is fine — each scan is a linear pass through an array. In real hardware, page replacement is handled by OS-level data structures (e.g., a clock algorithm with a hardware-updated reference bit), not a brute-force scan.

---

## Module: `ApiServer.java` — HTTP API Server

### Why It Exists
The simulator originally ran as a CLI tool. To make it accessible in a browser (for demo, assignment submission, and remote evaluation), `ApiServer.java` wraps all four simulator modes behind a lightweight HTTP server. No external library is needed — `com.sun.net.httpserver` ships with the JDK and is sufficient for single-developer usage.

### Endpoints

| Method | Path | Handler | Description |
|--------|------|---------|-------------|
| `GET` | `/api/status` | `StatusHandler` | Returns `{"status":"idle"}` or `{"status":"running"}` |
| `GET` | `/api/asm` | `AsmHandler` | Returns `{"content":"..."}` — full text of `input.asm` |
| `POST` | `/api/asm` | `AsmHandler` | Overwrites `input.asm` with request body |
| `POST` | `/api/run` | `RunHandler` | Runs pipeline mode → returns `{"ok":true}` or `{"error":"..."}` |
| `GET` | `/api/console` | `FileHandler("console.txt")` | Returns `{"content":"..."}` |
| `GET` | `/api/output` | `FileHandler("output.txt")` | Returns `{"content":"..."}` |
| `GET` | `/api/swap` | `FileHandler("swap.txt")` | Returns `{"content":"..."}` |
| `POST` | `/api/trace` | `TraceHandler` | Accepts raw `.trace` bytes, runs TraceSimulator, returns stats text |
| `GET` | `/api/traces` | `TracesListHandler` | Lists `.trace` files in `phase3_traces/` |
| `GET` | `/api/trace-file?name=` | `TraceFileHandler` | Serves a named preset trace file as raw bytes |

### Key Design: Concurrency Guard
```java
private static final AtomicBoolean running = new AtomicBoolean(false);

// In RunHandler / TraceHandler:
if (!running.compareAndSet(false, true)) {
    sendJson(ex, 409, "{\"error\":\"Simulation already running\"}");
    return;
}
```
The `AtomicBoolean` ensures only one simulation runs at a time. Compare-and-set is atomic — no race condition even with concurrent HTTP requests on the `CachedThreadPool`. If a second request arrives while a simulation runs, it immediately gets a 409 Conflict.

### Key Design: Zero-Dep JSON
All JSON is hand-serialized using `escapeJson()` — a simple string replacement for `\`, `"`, `\n`, `\r\n`, and `\t`. No Jackson/Gson needed. This is safe because all values are either numbers (safe) or file content strings (escaped).

### Key Design: Trace Upload Flow
`TraceHandler` reads the raw POST body as bytes, writes to a `File.createTempFile()`, runs `TraceParser.parse()` + `TraceSimulator.run()` with captured stdout, then deletes the temp file. The `capturedOut` pattern reuses `StatsPrinter` — no duplicate stats formatting code.

### Key Design: Port from Environment
```java
private static final int PORT = System.getenv("PORT") != null
    ? Integer.parseInt(System.getenv("PORT")) : 8080;
```
Heroku sets `PORT` dynamically. The server reads this at startup, making the same binary runnable locally (`8080`) and on Heroku (`random port assigned by dyno`).

### Interview Questions
- *Q: Why not use Spring Boot or Quarkus?*
  → A: Zero dependencies is a design goal. `com.sun.net.httpserver` is part of the JDK, so the project compiles and runs with `javac` + `java` — no build tool, no dependency manager. Spring Boot would add 50+ MB of JAR overhead for a server that needs 9 simple routes.
- *Q: What's the risk of the `running` AtomicBoolean approach vs a proper job queue?*
  → A: Requests that arrive during a simulation are immediately rejected (409). A proper queue would let them wait and be served in order. For a demo tool, rejection is acceptable (the frontend shows an "already running" state). For production, you'd want a queue with timeout and cancellation.
- *Q: The TraceHandler writes to a temp file. What happens if the JVM crashes during simulation?*
  → A: `File.deleteOnExit()` is registered as a JVM shutdown hook, so the OS will clean up the temp file on normal or abnormal exit. If the disk fills up during the write, `Files.write()` throws `IOException`, which is caught and returned as a 500 error.
- *Q: Why does `RunHandler` redirect `System.err` to a `ByteArrayOutputStream`?*
  → A: `Main.runPipelinePublic()` and the pipeline stages may write diagnostic messages to `System.err`. The handler captures these to include in the JSON response (`{"stderr":"..."}`) for the frontend to display, without polluting the server's actual stderr.

---

## Module: Web Frontend (`web/src/`) — React + TypeScript + Vite

### Why It Exists
The CLI simulator is powerful but requires Java knowledge to operate. The web frontend makes it accessible to anyone with a browser — useful for live demos, assignment showcases, and collaborative review.

### Technology Stack
- **React 18** — component-based UI with hooks
- **TypeScript** — type-safe API calls and component props
- **Vite** — fast dev server with `/api` proxy, optimized production builds
- **Axios** — HTTP client with configurable base URL (`VITE_API_URL` env var) and 2-minute timeout for simulation requests
- **Lucide React** — icon set (no external CSS framework)
- **CSS custom properties** — all tokens (`--bg-base`, `--accent-cyan`, etc.) defined in `index.css` for a dark-mode glassmorphism theme

### Pages

| Page | Route key | Description |
|------|-----------|-------------|
| `SimulatorPage` | `simulator` | Split-pane: left = RISC-V ASM code editor, right = Console/Stats/Swap/Raw output tabs |
| `TraceReplayPage` | `trace` | Drag-and-drop trace upload + preset trace list + live stats dashboard |
| `ArchitecturePage` | `architecture` | SVG pipeline diagram + instruction set table + hazard/memory subsystem cards |
| `AboutPage` | `about` | Project hero + feature grid + 3-phase project timeline |

### Key Components

**`useSimulator` hook** — Centralizes all simulator state: `asmCode`, `status` (`idle`|`running`|`success`|`error`), `consoleContent`, `outputContent`, `swapContent`, `backendOnline`, `runDurationMs`. The `run()` function: saves ASM via `POST /api/asm`, calls `POST /api/run`, then fetches all three output files in parallel.

**`StatsPanel`** — Parses `output.txt` text with regex to extract numeric fields (`Cycles`, `IPC`, `Branch Flushes`, cache hit/miss counts). Displays them as stat cards and a cache table. This parsing must be robust to output format changes.

**`TraceReplayPage`** — Two modes: (1) file upload via `<input type="file">` or drag-and-drop (sends raw bytes via `POST /api/trace`), (2) preset trace buttons that fetch file bytes via `GET /api/trace-file?name=` and then upload the same way.

**`PipelineDiagram`** — SVG-based diagram showing all 5 pipeline stages, forwarding paths, and cache connections. Static diagram (not cycle-accurate animation).

### Deployment Architecture
```
┌──────────────────────┐         ┌────────────────────────┐
│   Vercel (Frontend)  │  HTTPS  │   Heroku (Backend)     │
│   React + Vite SPA   │ ──────▶ │   java Main --server   │
│   Static files       │         │   PORT from env        │
│   vercel.json: SPA   │         │   Dockerfile:          │
│   routing rewrites   │         │     javac + java       │
└──────────────────────┘         └────────────────────────┘
```
- `vercel.json` rewrites all routes to `index.html` (SPA routing)
- `Dockerfile`: builds Java backend with `javac -d out -sourcepath src src/Main.java`, then runs `java -cp out Main --server`
- `heroku.yml` declares the Docker build/run commands
- `VITE_API_URL` env var in the Vercel project settings points the frontend to the Heroku URL

### Interview Questions
- *Q: Why TypeScript instead of plain JavaScript for the frontend?*
  → A: The API responses have specific shapes (`SimResult`, `TraceResult`, `ParsedTraceStats`). TypeScript interfaces catch mismatches at compile time — e.g., accessing `result.content` when `result.error` is set, or passing wrong props to `StatsPanel`. Without TypeScript, these bugs would only surface at runtime.
- *Q: The `StatsPanel` parses output.txt with regex. What breaks if the output format changes?*
  → A: The regex patterns like `'Cycles\\s*:\\s*([\\d.]+)'` assume specific field names and colon-separated formatting. If `StatsPrinter` changes a label (e.g., "Total Cycles" instead of "Cycles"), the parser silently returns `null` and the dashboard shows `—`. The fix is to either use a structured JSON response from the API or version-stamp the output format.
- *Q: Why not server-side render (Next.js) instead of a Vite SPA?*
  → A: The simulator UI is entirely client-driven — no SEO requirements, no shared state, no initial data fetch needed for the page to render. A SPA is simpler: no server framework to maintain, trivial Vercel deployment (just static files), and instant navigation between pages.
- *Q: What's the `runPipelinePublic` design necessary for the ApiServer?*
  → A: `Main.runPipelineMode()` is private. The `RunHandler` in `ApiServer` calls `Main.runPipelinePublic()` — a `public static` method that does the same work but is accessible outside `Main`. This avoids reflection or code duplication. The method redirects `System.out` to `console.txt` and writes `output.txt` — the same behavior as the CLI.

---

## Module: `trace/TraceSimulator.java` — Trace Replay Engine

### Why It Exists
Trace replay allows workload analysis *without* a full pipeline. Given a pre-recorded sequence of memory accesses and ALU operations, it simulates the VM and cache subsystem to measure TLB hit rates, cache miss rates, page fault counts, and dirty eviction patterns.

### Key Design: Reusing Pipeline Infrastructure
The TraceSimulator creates `ID_EX`, `EX_MEM`, and `MEM_WB` pipeline register objects and uses the *same* `HazardUnit` from the pipeline package to detect data hazards. Each trace instruction is converted to a mock `Instruction` object, and the pipeline registers are advanced each cycle. This means multi-cycle MUL stalls, load-use hazards, and RAW hazards are all correctly accounted for in the trace mode cycle count.

### Per-Instruction Cycle Accounting
- **LOAD:** translation_latency + cache_read_latency. Stalls = total - 1 (1 cycle is "normal execution")
- **STORE:** translation_latency + cache_write_latency. Same stall accounting.
- **ADD:** 1 base cycle. Multi-cycle stalls handled by HazardUnit.
- **MUL:** 1 base cycle + 2 stall cycles from HazardUnit (3-cycle total latency)
- **BRANCH/JUMP:** 2-cycle flush penalty + instruction pipeline flush

### Interview Questions
- *Q: Why does the trace simulator maintain pipeline registers if there's no actual pipeline?*
  → A: To correctly model data hazards. Without tracking which registers are being written and when, the cycle count would undercount stalls. The pipeline registers let the existing HazardUnit detect load-use and multi-cycle dependencies without duplicating that logic.

---

## Module: `compiler/Compiler.java` — Two-Pass Assembler

### Why It Exists
The input `.asm` file contains labels, pseudo-instructions, `.data` directives, and human-readable register names. The compiler transforms this into a list of `Instruction` records and `DataItem` byte arrays that the simulator can execute.

### Pass 1: Symbol Table Construction
- Iterates all lines, tracking whether we're in `.data` or `.text`
- Text labels → mapped to `instrIndex * 4` (byte address)
- Data labels → mapped to `DATA_BASE + accumulated byte offset`
- Validates text segment doesn't overflow into data segment

### Pass 2: Instruction Emission + Data Emission
- **Parser.parseText():** Converts each `.text` line into an `Instruction` using factory methods. Branch labels are resolved to PC-relative offsets (`label_address - pc`). `LA` and `LI` with labels resolve to absolute addresses.
- **Compiler.parseData():** Converts each `.data` directive into a `DataItem`. `.word` emits 4-byte little-endian. `.asciiz` emits null-terminated string bytes.

### Pseudo-Instructions Supported
- `NOP` → `ADDI x0, x0, 0`
- `MV rd, rs` → `ADDI rd, rs, 0`
- `LI rd, imm` → `LI rd, 0, imm` (dedicated LI opcode)
- `LA rd, label` → `LI rd, 0, absolute_address`

### Memory Syntax Parsing
The parser handles both `LW x1, 0(x2)` and `LW x1, x2, 0` syntax using `memOff()` and `memReg()` helper methods that detect the presence of parentheses.

### Interview Questions
- *Q: Why a two-pass assembler instead of single-pass?*
  → A: Forward references. A branch instruction at line 5 might reference a label at line 20. In pass 1, we build the complete symbol table. In pass 2, we resolve all references. Single-pass would require backpatching, which is more complex.
- *Q: What happens if a branch target is outside the 12-bit immediate range?*
  → A: The immediate is truncated to 12 bits by `& 0xFFF` in the encoder. On decode, it's sign-extended back. If the actual offset exceeds ±2048, the truncation produces a wrong target. The assembler doesn't validate this — it's a known limitation.

---

# SECTION 4 — Technology Choices

## Java vs C++ vs Python

| Factor | Java | C++ | Python |
|--------|------|-----|--------|
| OOP modeling of hardware | Excellent (classes map naturally to pipeline stages, cache levels, TLB entries) | Good but manual memory management | Good but slower |
| Performance | ~3× slower than C++ for simulation loops | Fastest | ~50× slower |
| Development speed | Fast (no memory bugs, strong typing) | Slower (segfaults, header files) | Fastest |
| Academic suitability | Standard for CS courses, easy for peers to read | Common but higher barrier | Often used but too slow for large traces |
| Records / immutable data | `record` keyword (Java 16+) | `struct` (manual) | `@dataclass` |
| Built-in HTTP server | `com.sun.net.httpserver` (zero deps) | Requires libmicrohttpd or similar | Flask/FastAPI (easy but adds dep) |

**Why Java?** This is a course project where correctness and readability are more important than raw speed. Java's strong typing catches bugs at compile time. The `record` keyword (used for `Instruction`) provides immutable data classes with zero boilerplate. The JVM's JIT compiler is fast enough to process 715K-instruction traces in seconds. **The built-in `HttpServer` API let us add a web layer without any external dependencies — the same `javac` + `java` command line that builds the simulator also builds the API server.**

## React + TypeScript + Vite vs Alternatives

| Factor | React + Vite | Next.js | Plain HTML/JS |
|--------|-------------|---------|---------------|
| Use case | SPA, client-only, no SSR needed | Full-stack, SSR, SEO | Simple one-pagers |
| TypeScript support | First-class | First-class | Manual setup |
| Dev experience | Fast HMR via Vite, `/api` proxy | Good but heavier | No hot reload |
| Deployment | Vercel static (trivial) | Vercel serverless (more config) | Any CDN |
| Component model | Functional hooks | Same | N/A |

**Why Vite SPA?** The simulator UI is purely client-driven — the backend is the source of truth for simulation state, and all UI updates happen after API calls. No SSR or SEO requirements. Vite's dev proxy (`/api → localhost:8080`) mirrors production exactly (Vercel rewrites → Heroku), so there's no environment-specific code in the frontend.

## Data Structures: HashMap vs Array vs TreeMap

| Decision | Chosen | Alternative | Why |
|----------|--------|-------------|-----|
| Instruction latencies | `HashMap<Opcode, Integer>` | `int[]` indexed by ordinal | HashMap is clearer and more maintainable; O(1) average lookup is fast enough |
| Swap space | `HashMap<Integer, int[]>` | Array indexed by VPN | Only a tiny fraction of pages are ever swapped; a full array for 2^20 entries would waste 4MB of int references |
| Page table | `PageTableEntry[]` indexed by VPN | HashMap | Full array is correct here — every VPN needs a PTE, and array access is O(1) with no hashing overhead |
| Free frames | `LinkedList<Integer>` (Queue) | Stack | FIFO ordering matches the natural frame allocation pattern; queue semantics are clearer |

---

# SECTION 5 — Algorithms

## 1. LRU Replacement (Cache + TLB + Page Table)

**Purpose:** Select the least-recently-used entry for eviction when the container is full.

**Implementation:** Each entry (CacheLine, TLBEntry, PageTableEntry) has a `lastUsed` timestamp set to a monotonically increasing `clock` value on every access. On eviction, scan all valid entries in the set/TLB/page-table and select the one with the smallest `lastUsed`.

**Time Complexity:**
- Cache: O(A) per eviction where A = associativity (typically 1-4)
- TLB: O(E) per eviction where E = number of entries (16)
- Page Table: O(N) per eviction where N = total pages (1,048,576) — this is the bottleneck

**Why a clock counter instead of real timestamps?** Logical ordering is all that's needed. A clock counter is cheaper than calling `System.nanoTime()` every access and provides deterministic, reproducible results.

**Known Limitation:** The page table LRU scan is O(N) with N=2^20. A production OS would maintain a doubly-linked list of mapped pages for O(1) LRU eviction (at the cost of list manipulation on every access).

## 2. FIFO Replacement

**Purpose:** Select the first-inserted entry for eviction (no access-time tracking needed).

**Implementation:** Each entry has an `insertOrder` timestamp set only on insertion (not updated on subsequent accesses). Eviction selects the entry with the smallest `insertOrder`.

**Tradeoff vs LRU:** FIFO is simpler to implement in hardware (no need to update timestamps on every access) but has worse hit rates because it doesn't consider recency. Belady's anomaly: increasing cache size can increase miss rate with FIFO (not with LRU).

## 3. BTFNT Static Branch Prediction

**Purpose:** Predict branch direction without runtime history. Reduces branch misprediction penalty for loop-heavy code.

**Algorithm:**
1. In ID stage: check if the decoded instruction is a branch (`isBranch()`)
2. Check the sign of the immediate (offset):
   - Negative offset (backward branch) → predict TAKEN (most backward branches are loop back-edges)
   - Positive offset (forward branch) → predict NOT TAKEN (most forward branches are early exits or error checks)
3. If predicted taken: eagerly redirect PC to `pc + immediate`
4. In EX stage: compare actual outcome to prediction
5. If misprediction: flush IF/ID and ID/EX, set PC to recovery address

**Misprediction Cost:** 2 cycles (the 2 instructions fetched after the mispredicted branch are squashed).

**Why BTFNT over always-not-taken?** BTFNT correctly predicts the common case of loop back-edges. For a bubble sort with ~250 inner-loop iterations, BTFNT predicts 249 of them correctly (backward branch taken), mispredicting only the loop exit.

## 4. Two-Pass Assembly

**Purpose:** Resolve forward label references in a single source file.

**Algorithm:**
- **Pass 1:** Scan all lines. Track section (.data/.text). For each label, record its byte address in a symbol table HashMap. Data labels get `DATA_BASE + accumulated_offset`. Text labels get `instrIndex * 4`.
- **Pass 2:** Parse instructions. For branches/jumps, resolve label names to PC-relative offsets (`symbol_address - current_pc`). For `LA`/`LI`, resolve to absolute addresses.

**Time Complexity:** O(L) for each pass, where L = number of source lines. Total: O(L).

**Known Limitation:** Forward references within `.data` to other `.data` labels work (the symbol table is built in one complete pass), but `.text` instructions cannot reference labels defined *later* in `.data` — in practice this doesn't matter because `.data` precedes `.text` in standard layout.

## 5. Write-Back Write-Allocate Cache Policy

**Purpose:** Minimize traffic to the next level of the memory hierarchy.

**Algorithm:**
- **Write Hit:** Modify the data in the cache line, mark it dirty. Do NOT write to the next level.
- **Write Miss (Write-Allocate):** Fetch the block from the next level, install it in the cache, then modify the data in the newly installed line.
- **Eviction:** If the evicted line is dirty, write its data back to the next level (write-back).

**Why Write-Back over Write-Through?** Write-through would generate a write to L2 (or memory) on every store instruction. For a bubble sort with ~500 store instructions, that's 500 L2/memory writes. Write-back only writes on eviction, which may be zero if the data stays cached.

---

# SECTION 6 — Every Parameter

## Pipeline Parameters (Config.java)

| Parameter | Default | Purpose | Effect of ↑ | Effect of ↓ | Why This Value |
|-----------|---------|---------|-------------|-------------|----------------|
| `forwardingEnabled` | `true` | Enables EX/MEM and MEM/WB bypass paths | N/A (boolean) | More stalls, lower IPC | Forwarding is standard in modern processors |
| `ADD latency` | 1 cycle | Cycles for integer add in EX | More stalls | N/A (minimum 1) | Simple ALU operation, 1 cycle is standard |
| `MUL latency` | 3 cycles | Cycles for integer multiply | Pipeline frozen longer, lower IPC | Unrealistic for multiply hardware | Realistic for unpipelined multiplier |
| `DIV latency` | 4 cycles | Cycles for integer divide | Pipeline frozen longer | Unrealistic for divider | Division is inherently slower than multiply |
| `DRAIN_THRESHOLD` | 3 | Cycles after HALT to drain pipeline | More unnecessary cycles | Instructions still in pipeline may not complete | 3 covers WB of instruction at EX when HALT is decoded |
| `MAX_CYCLE_LIMIT` | 100,000 | Safety limit for infinite loops | More time before abort | May abort valid long-running programs | Sufficient for all test programs |

## Cache Parameters (Config.java defaults)

| Parameter | Default | Purpose | Effect of ↑ | Effect of ↓ | Why This Value |
|-----------|---------|---------|-------------|-------------|----------------|
| L1I Size | `null` (disabled) | Instruction cache size | More I-cache hits | More misses | Disabled for trace mode compatibility |
| L1D Size | 4096 B | Data cache capacity | Fewer capacity misses | More misses | 4KB is a minimal L1 for academic analysis |
| L1D Block Size | 64 B | Cache line width | Better spatial locality, but more wasted bandwidth | Worse spatial locality | 64B is the industry standard |
| L1D Associativity | 1 (direct-mapped) | Ways per set | Fewer conflict misses | More conflict misses (direct-mapped) | Direct-mapped exposes conflict miss behavior for analysis |
| L1D Latency | 1 cycle | Hit access time | More total cycles | Unrealistic | 1 cycle = ideal, simplifies pipeline timing |
| L2 Size | `null` (disabled) | Unified L2 capacity | Fewer L2 misses | N/A | Disabled per spec for trace mode |
| Memory Latency | 10 cycles | Main memory access time | More penalty per miss | Unrealistic | 10 cycles is a moderate penalty for simulation |

## Virtual Memory Parameters (Config.java)

| Parameter | Default | Purpose | Effect of ↑ | Effect of ↓ | Why This Value |
|-----------|---------|---------|-------------|-------------|----------------|
| `virtualSizeBytes` | 4 GB (4294967296) | Virtual address space | More page table entries (memory cost) | Fewer addressable pages | 32-bit standard |
| `physicalSizeBytes` | 256 KB (262144) | Physical memory | Fewer page faults, fewer evictions | More page faults, more swapping | 64 frames × 4KB — constrained to stress-test VM |
| `pageSizeBytes` | 4096 (4 KB) | Page granularity | Internal fragmentation, fewer pages | More page table entries | 4KB is the standard x86/RISC-V page size |
| `dtlbEntries` | 16 | TLB capacity | Fewer TLB misses | More page walks (10 cycles each) | Small enough to observe TLB thrashing |
| `tlbHitLatency` | 1 cycle | TLB probe cost | Always charged | N/A | 1 cycle is standard for a small TLB |
| `pageWalkLatency` | 10 cycles | Page table lookup cost | Higher translation penalty | Unrealistic | Flat table = 1 memory access ~ 10 cycles |
| `pageFaultLatency` | 50 cycles | Frame allocation cost | Higher fault penalty | Unrealistic | Models OS trap + frame allocation overhead |
| `vmReplacementPolicy` | `"lru"` | Page eviction strategy | N/A (string) | N/A | LRU has best hit rates; FIFO available for comparison |

---

# SECTION 7 — Design Decisions

## 1. In-Order vs Out-of-Order Pipeline
**Decision:** In-order, 5-stage scalar pipeline.
**Rationale:** Models the classic RISC pipeline taught in COA courses. OoO execution (Tomasulo's) would multiply complexity (reservation stations, reorder buffer, common data bus) without serving the project's educational purpose.
**At Scale:** A production simulator would implement OoO to accurately model modern processors. ARM's Cortex-A76 simulator, for example, models hundreds of pipeline stages.

## 2. Static vs Dynamic Branch Prediction
**Decision:** BTFNT (static, no history).
**Rationale:** Static prediction requires zero storage overhead (no branch history table, no pattern history register). BTFNT achieves high accuracy on loop-dominated code because backward branches (loop back-edges) are taken >95% of the time.
**Tradeoff:** Poor accuracy on forward branches in data-dependent code. A 2-bit saturating counter predictor would improve accuracy at the cost of storage and complexity.

## 3. Flat Page Table vs Multi-Level Page Table
**Decision:** Flat (single-level) page table indexed by VPN.
**Rationale:** With a 4GB virtual space and 4KB pages, the flat table has 2^20 entries. Each `PageTableEntry` is a small Java object (~40 bytes with overhead), totaling ~40MB. This is acceptable for a simulator. A multi-level table would save memory for sparse address spaces but add implementation complexity.
**Tradeoff:** The O(2^20) eviction scan is the main cost. A multi-level table with a linked list of mapped pages would give O(1) eviction at the cost of more complex insertion/deletion.

## 4. In-Memory Swap vs Disk I/O
**Decision:** In-memory HashMap for swap space; `swap.txt` is only a post-simulation dump.
**Rationale:** Simulating actual disk I/O would conflate Java I/O latency with simulated cycle counts, making results non-deterministic. The in-memory HashMap gives O(1) swap operations with deterministic behavior.
**Tradeoff:** Unbounded memory usage — a pathological workload could swap millions of pages, consuming gigabytes of JVM heap. A production simulator might cap swap space or model disk bandwidth constraints.

## 5. Custom 32-Bit Encoding vs Standard RV32I
**Decision:** Simplified custom encoding with uniform field layout.
**Rationale:** The encoder/decoder is ~50 lines. Standard RV32I has 6 different encoding formats (R, I, S, B, U, J) with overlapping field positions, requiring ~200 lines. The custom encoding is sufficient because we control both the assembler and the simulator.
**Tradeoff:** Programs compiled with a standard RISC-V toolchain (GCC) cannot run on this simulator. This is acceptable for a course project.

## 6. Write-Back Write-Allocate vs Write-Through No-Allocate
**Decision:** Write-back, write-allocate for all cache levels.
**Rationale:** Minimizes traffic to the next level. With write-through, every SW instruction would generate L2/memory traffic, dramatically increasing cycle counts and obscuring cache hit rate analysis.
**Tradeoff:** More complex eviction logic (must check dirty bits, cascade write-backs). Write-through is simpler but produces unrealistic traffic patterns.

## 7. Reverse-Order Stage Ticking vs Double-Buffering
**Decision:** Tick stages in WB→MEM→EX order with single pipeline register instances.
**Rationale:** Avoids the memory cost and complexity of maintaining "old" and "new" copies of every pipeline register. By processing later stages first, each stage reads the old value and the new value is written for the next cycle.
**Tradeoff:** Requires careful attention to which variable holds the "old" vs "new" value. The `oldMemWb` / `newMemWb` naming in PipelineController is an artifact of this choice.

## 8. Unified Stats Class vs Per-Component Stats
**Decision:** Single `Stats` class with public fields for all metrics (pipeline, cache, VM).
**Rationale:** Both pipeline and trace modes produce the same metrics. A single class with `collectCacheStats()` and direct field assignment simplifies reporting.
**Tradeoff:** Large, flat class with 17+ public fields. A more modular design would group stats by subsystem (PipelineStats, CacheStats, VMStats) and compose them.

---

# SECTION 8 — Possible Improvements

## Performance
1. **Pipelined cache access** — Model non-blocking caches where the pipeline doesn't freeze on a cache miss but instead queues the miss and continues with independent instructions (requires miss status holding registers)
2. **Faster page eviction** — Replace O(N) page table scan with a doubly-linked list of mapped pages for O(1) LRU eviction
3. **Instruction decoding cache** — In pipeline mode, decoded instructions could be cached to avoid re-encoding/decoding on loop iterations
4. **Parallel trace processing** — Batch trace mode could process multiple trace files in parallel using Java's `ForkJoinPool`

## Scalability
1. **Config file parser** — Instead of hardcoded defaults, parse an actual INI-style config file (the README documents this format but the code doesn't implement it)
2. **Larger instruction set** — Add RV32M (multiply extension), RV32F (floating-point), RV32A (atomic) support
3. **Multi-level page table** — Replace flat table with SV32 (2-level) or SV39 (3-level) for realistic virtual memory modeling
4. **Variable-length pipeline** — Allow configurable pipeline depth (7-stage, 10-stage) for architectural space exploration

## Architecture
1. **Out-of-order execution** — Implement Tomasulo's algorithm with reservation stations and a reorder buffer
2. **Dynamic branch prediction** — 2-bit saturating counters, GShare, or tournament predictor
3. **Superscalar** — Dual-issue pipeline fetching and executing 2 instructions per cycle
4. **Write buffer** — Model a store buffer to decouple stores from the pipeline, reducing store-related stalls
5. **MSHR (Miss Status Holding Registers)** — Track outstanding cache misses to support non-blocking cache operation

## Testing & Validation
1. **Automated regression suite** — Unit tests for each module (cache eviction, TLB replacement, hazard detection, encoding round-trip)
2. **Differential testing against Spike** — Compare register state and cycle counts against the official RISC-V ISA simulator
3. **Coverage analysis** — Identify untested code paths (e.g., all branches in `HazardUnit` exercised?)
4. **Randomized instruction generation** — Fuzz the assembler with random valid programs to catch edge cases

## Frontend/UX
1. **Interactive visualization** — Web-based pipeline diagram showing instruction flow, stalls, and forwarding paths per cycle
2. **Step-by-step debugger** — Pause simulation, inspect register/memory state, single-step
3. **Config GUI** — Graphical parameter editor instead of modifying Java source code
4. **Comparative analysis** — Run the same program with different configs and visualize IPC differences

---

# SECTION 9 — Interview Questions

## Easy (30 Questions)

**Q1:** What are the 5 stages of the pipeline?
**A:** Instruction Fetch (IF), Instruction Decode (ID), Execute (EX), Memory Access (MEM), Write Back (WB).

**Q2:** What does `x0` always contain in RISC-V?
**A:** Zero. It's hardwired — writes to x0 are silently discarded. The RegisterFile enforces this with `if (r != 0) regs[r] = val`.

**Q3:** What's the difference between a cache hit and a cache miss?
**A:** A hit means the requested data is found in the cache (fast access at cache latency). A miss means it's not there, and the data must be fetched from the next level (L2 or main memory), paying additional latency.

**Q4:** What does IPC stand for, and how is it calculated?
**A:** Instructions Per Cycle. `IPC = instructionsRetired / totalCycles`. Higher is better. Ideal for a single-issue pipeline is 1.0.

**Q5:** What instruction types does the simulator support?
**A:** R-type (ADD, SUB, MUL, DIV, shifts, logic), I-type (ADDI, LW, LB, LI), S-type (SW, SB), B-type (BEQ, BNE, BLT, BGE), J-type (JAL), System (ECALL, HALT), plus pseudo-instructions (NOP, MV, LA).

**Q6:** What does the `Instruction` Java record contain?
**A:** Five fields: `opcode` (Opcode enum), `rd` (destination register), `rs1` (source 1), `rs2` (source 2), `immediate` (12-bit value). Factory methods enforce correct semantics per type.

**Q7:** What does write-back mean in the cache context?
**A:** Modified data is written only to the cache (marking the line dirty). It's written to the next level only when the line is evicted.

**Q8:** What is a TLB?
**A:** Translation Lookaside Buffer — a small, fast cache that stores recent virtual-to-physical page translations to avoid slow page table walks.

**Q9:** What does BTFNT stand for?
**A:** Backward-Taken, Forward-Not-Taken. A static branch prediction heuristic that predicts backward branches as taken (loop back-edges) and forward branches as not taken.

**Q10:** How many registers does the simulator model?
**A:** 32 integer registers (x0–x31), matching the RISC-V standard.

**Q11:** What is a pipeline register?
**A:** A data buffer between two pipeline stages that holds the intermediate results from one stage for the next stage to consume on the following clock cycle. Examples: IF/ID, ID/EX, EX/MEM, MEM/WB.

**Q12:** What does the `isNop` flag in pipeline registers indicate?
**A:** That the register holds a "bubble" (no valid instruction). This happens after stalls (NOP injected into ID/EX) or flushes (IF/ID and ID/EX zeroed out).

**Q13:** What is set-associativity?
**A:** A cache organization where each memory address maps to a specific set, but can be placed in any way within that set. A 2-way set-associative cache has 2 lines per set.

**Q14:** What is a page fault?
**A:** An event that occurs when a program accesses a virtual page that is not currently mapped to a physical frame. The system must allocate a frame (possibly evicting another page) and map the page.

**Q15:** What does the `Lexer` do?
**A:** Reads the assembly file line by line, strips comments (# and //, respecting quoted strings), removes blank lines, and returns a list of cleaned lines for the parser.

**Q16:** What is the purpose of the `Stats` class?
**A:** Collects all simulation metrics in one place: cycles, stalls, branch flushes, instructions retired, cache hits/misses per level, and VM stats (TLB, page faults, swap).

**Q17:** What does `writesBack()` return for a store instruction?
**A:** `false`. Store instructions write to memory, not to a register. This method is used to determine if the instruction produces a register result (needed for hazard detection and forwarding).

**Q18:** What is the data segment base address?
**A:** `0x0400` (1KB offset from text base at `0x0000`).

**Q19:** What does `Memory.loadProgram()` do?
**A:** Encodes each `Instruction` as a 32-bit integer using `InstructionEncoder.encode()` and writes it to consecutive word addresses in memory, starting at the given base address.

**Q20:** What is the purpose of `swap.txt`?
**A:** A post-simulation dump of the swap space, listing how many pages were swapped out/in and which VPNs are still resident in swap. It's for debugging/verification, not runtime I/O.

**Q21:** How does the simulator handle division by zero?
**A:** Returns -1, matching the RISC-V specification. Additionally guards against `Integer.MIN_VALUE / -1` overflow (returns `MIN_VALUE`).

**Q22:** What is the `AccessResult` class?
**A:** A pair of (data, latencyCycles) returned by every cache/memory access. It tells the caller both the value read and how many cycles the access cost.

**Q23:** What does `hasCacheConfig()` check?
**A:** Returns `true` if either `l1i` or `l1d` is non-null, indicating the cache hierarchy should be used instead of direct memory access.

**Q24:** What is the difference between `readWord` and `readWordNoStats`?
**A:** `readWord` counts the access in hit/miss statistics. `readWordNoStats` does not. The distinction prevents internal block fills from inflating counters.

**Q25:** What does the `ForwardResult` enum represent?
**A:** The source of a forwarded operand value: `NONE` (use register file), `FROM_EX_MEM` (bypass from EX/MEM), or `FROM_MEM_WB` (bypass from MEM/WB).

**Q26:** How does the simulator terminate?
**A:** Three conditions: (1) HALT instruction + 3 drain cycles, (2) PC past end of program + all pipeline registers are NOP, (3) cycle limit (100,000) reached.

**Q27:** What is the stack pointer initialized to?
**A:** `0x0FFF` (set in `RegisterFile` constructor for `x2`/`sp`).

**Q28:** What trace instruction types are supported?
**A:** L (load), S (store), ADD, MUL, BEQ/BNE (branch), JAL (jump).

**Q29:** What does `CacheConfig.getNumSets()` compute?
**A:** `size / (blockSize × associativity)` — the number of sets in the cache.

**Q30:** What does `PageTable.unmapPage()` do?
**A:** Sets the PTE's `valid` flag to false, `frameNumber` to -1, and `dirty` to false — effectively removing the mapping.

---

## Medium (40 Questions)

**Q31:** Why does the pipeline tick stages in reverse order (WB first, IF last)?
**A:** To avoid data races in sequential simulation. If IF ticked first and wrote new data into IF/ID, ID would read the just-written value instead of the previous cycle's value. By ticking WB first, each stage reads its input register before it's overwritten by the preceding stage.

**Q32:** Walk through what happens cycle-by-cycle when a load-use hazard occurs.
**A:** Cycle N: LW x1, 0(x2) enters EX. Cycle N+1: ADD x3, x1, x4 enters ID. HazardUnit detects LW in ID/EX with rd=x1, and ADD in IF/ID needs rs1=x1. Since LW is a load, this is a load-use hazard. A NOP bubble is injected into ID/EX (ADD stays in IF/ID). LW proceeds to MEM. Cycle N+2: LW is in WB (data now available via MEM/WB forwarding), ADD finally enters EX and gets x1's value via forwarding from MEM/WB.

**Q33:** Why does the ForwardingUnit exclude loads from EX/MEM forwarding?
**A:** When a load is in the MEM stage (EX/MEM register), its `aluResult` field contains the *memory address* (base + offset), not the loaded data. The actual data isn't available until the load completes the MEM stage and reaches MEM/WB. Forwarding the address instead of the data would produce incorrect results.

**Q34:** What happens when both an IF cache miss and a MEM cache miss occur simultaneously?
**A:** MEM miss takes priority. The `memStallCycles` counter is decremented first. Only when MEM stall is fully resolved does the IF stall counter begin decrementing. This correctly models a single-ported memory bus where MEM has priority.

**Q35:** Explain the difference between LRU and FIFO replacement in the context of your cache.
**A:** LRU evicts the line that hasn't been accessed for the longest time — `lastUsed` is updated on every read/write. FIFO evicts the line that was inserted first — `insertOrder` is set once at insertion and never updated. LRU adapts to access patterns; FIFO does not. LRU is better in general but requires updating timestamps on every access (more hardware cost). FIFO is simpler but susceptible to Belady's anomaly.

**Q36:** Why is the TLB fully-associative instead of set-associative?
**A:** With only 16 entries, set-associativity would create tiny sets (e.g., 2-way = 8 sets of 2 entries). A working set that maps to the same set would thrash even with 14 unused entries. Full associativity eliminates this pathological case. The hardware cost is acceptable because 16 entries = 16 comparators, which is feasible.

**Q37:** Trace through the address translation for a load to a never-before-seen virtual address when physical memory is full.
**A:** (1) TLB lookup: miss (new address). Charge TLB hit latency (1 cycle). (2) Page walk: miss (page not in table). Charge page walk latency (10 cycles). (3) Page fault. Charge page fault latency (50 cycles). (4) Allocate frame: physical memory full → evict page via LRU. (5) If evicted page is dirty (checked in both PTE and TLB) → save to swap space. (6) Invalidate evicted page in TLB and page table. (7) Invalidate L1D cache lines in the freed frame's address range (PIPT correctness). (8) Check if new page has swap data → if yes, restore from swap; if no, zero-fill frame. (9) Map new page → freed frame in page table. (10) Insert mapping into TLB. Total translation latency: 1 + 10 + 50 = 61 cycles. Then L1D cache access adds additional latency.

**Q38:** Why does the 12-bit immediate get sign-extended only for certain instructions?
**A:** Branch offsets, load/store offsets, ADDI, and JAL all use signed values (they can be negative: backward branches, negative stack offsets). R-type instructions don't use the immediate at all. LI and LA use unsigned absolute values. Sign-extending only for instructions that semantically treat the immediate as signed prevents misinterpreting positive large values as negative.

**Q39:** What would happen if you removed the `x0 != 0` check from the ForwardingUnit?
**A:** Forwarding would fire for instructions that read x0 as a source register, providing a nonzero value from a previous instruction's result instead of the hardwired zero. For example, `ADD x0, x1, x2` followed by `BEQ x0, x3, label` — without the check, x0 would be forwarded as x1+x2 instead of 0, making the branch decision wrong.

**Q40:** Explain the `memLatencyLeft` mechanism in MEM/WB.
**A:** When the MEM stage accesses the cache and gets a miss, the `AccessResult` returns a latency of, say, 11 cycles (1 L1D hit latency + 10 memory latency). MEM_Stage subtracts 1 (for the current cycle tick) and stores `latencyCycles - 1 = 10` in `memWb.memLatencyLeft`. The PipelineController then freezes the entire pipeline for those 10 cycles by setting `memStallCycles = 10` and counting them down, one per cycle.

**Q41:** Why does the Compiler validate that the text segment doesn't overflow into the data segment?
**A:** TEXT_BASE is 0x0000 and DATA_BASE is 0x0400 (1KB). If the program has more than 256 instructions (256 × 4 = 1024 bytes), the text segment would overwrite data addresses. The check `textAddr > DATA_BASE` catches this at compile time with a clear error.

**Q42:** How does the Compiler handle the `LA` pseudo-instruction?
**A:** `LA rd, label` is treated as `LI rd, absolute_address_of_label`. The `immAbsolute()` method looks up the label in the symbol table and returns the absolute address (not a PC-relative offset). This means `LA` loads the data segment address directly into a register.

**Q43:** Why does `CacheLevel.insert()` clone the dirty victim's data array?
**A:** Because the victim's `CacheLine.data` array will be immediately overwritten by `fillLine()` with the new block's data. If we passed a reference instead of a clone, the `EvictionResult` would point to the new data, not the old dirty data that needs to be written back.

**Q44:** What's the difference between `Memory.readWord(addr)` and `Memory.readByte(addr)`?
**A:** `readWord` returns the full 32-bit integer at `data[addr/4]`. `readByte` extracts a single byte from the word: it reads `data[addr/4]`, computes `bytePosition = (addr % 4) * 8`, and shifts/masks to get the specific byte. This models little-endian byte ordering.

**Q45:** Why does the TraceSimulator create "mock" Instruction objects?
**A:** The HazardUnit expects pipeline registers populated from real `Instruction` objects (it checks `ifId.instruction.rs1()`, etc.). By creating mock Instructions that match the trace instruction's register usage, the trace simulator can reuse the existing HazardUnit without modification.

**Q46:** What is the PIPT design, and why does it matter for cache invalidation?
**A:** Physically Indexed, Physically Tagged means the cache uses physical addresses for both indexing (selecting the set) and tagging (identifying the block). When a physical frame is reassigned to a different virtual page, any cache lines that were indexed by addresses in that frame now hold stale data from the *old* page. The `invalidateFrame()` method prevents serving this stale data.

**Q47:** Why does the `evictPage()` method check dirty bits in both the PTE and the TLB?
**A:** Consider: (1) Page is loaded clean. (2) A store instruction sets the TLB entry's dirty bit. (3) The TLB entry is later evicted (TLB is full). (4) When the page is evicted, the PTE might have `dirty=false` if the TLB dirty bit was never written back to the PTE. Checking both ensures no dirty data is lost.

**Q48:** What happens if you run a program without cache enabled (Config has null L1I and L1D)?
**A:** The pipeline operates in "Phase 1 mode": IF_Stage fetches directly from the `List<Instruction>` (no encoding/decoding needed), MEM_Stage reads/writes directly from `Memory`, and all accesses take 1 cycle (no cache latency). This backward compatibility lets you test pipeline logic without the cache subsystem.

**Q49:** Why does `EX_Stage` save `idEx.pc + 4` into `aluResult` for JAL?
**A:** JAL (Jump And Link) saves the return address (PC + 4, i.e., the address of the next sequential instruction) into `rd`. This is how function calls work: `JAL x1, target` jumps to `target` and saves the return address in x1. The caller can later use `JALR x0, x1, 0` to return.

**Q50:** Explain the `parseStringLiteral` method's escape handling.
**A:** It processes the string character by character. When it encounters a backslash, it looks at the next character: `\n` → newline (0x0A), `\t` → tab (0x09), `\0` → null byte (0x00), `\\` → literal backslash, `\"` → literal quote. For `.asciiz`, it appends a null terminator byte at the end. This allows the assembler to handle C-style string constants in the `.data` section.

**Q51:** What would happen if the cache block size was 1 word (4 bytes)?
**A:** Every cache line would hold exactly one word. Spatial locality would provide no benefit — accessing `arr[0]` would not prefetch `arr[1]`. Cache miss rate would increase dramatically. The cache would effectively become a direct-mapped lookup table with no spatial prefetching.

**Q52:** Why does the PipelineController check `pcPastEnd && all stages NOP` for termination?
**A:** After the last instruction is fetched, 4 more cycles are needed for it to flow through ID, EX, MEM, and WB. The condition `pcPastEnd` alone would terminate too early. Checking that all pipeline registers are NOP ensures every fetched instruction has completed.

**Q53:** What's the difference between a branch misprediction and a branch flush for JAL?
**A:** A misprediction is when a conditional branch (BEQ, etc.) was predicted in one direction but resolved in the opposite direction — requiring recovery. A JAL flush is unconditional — JAL always redirects the PC, so the instruction after JAL (already fetched) must always be squashed. Both increment `branchFlushes` and squash 2 instructions.

**Q54:** How does the write-allocate policy work on a store miss?
**A:** On `writeData()`, if L1D lookup misses: (1) charge L1D miss latency, (2) call `fetchBlockToL1()` to bring the entire block from L2/memory into L1D, (3) then write the word into the now-present L1D block using `writeWordNoStats()`. The fetch ensures the rest of the block has correct data (the store only modifies one word).

**Q55:** Why does `fillLine()` set `dirty = false` even though we're installing a fresh block?
**A:** The fresh block contains data from a lower level (L2 or memory). It hasn't been modified by the processor yet. Setting `dirty = false` correctly indicates that evicting this block won't require a write-back — the lower level already has the same data.

**Q56:** What is the significance of `cfg.getL2() == null` in CacheHierarchy?
**A:** It means L1 misses go directly to main memory, skipping L2 entirely. The `fetchBlockToL1()` method branches on `l2 != null` — if null, latency = `memoryLatency`, and blocks are fetched/evicted directly from/to `Memory`. This enables trace mode to run with only L1D.

**Q57:** How does the Compiler handle data labels that are referenced by instructions?
**A:** In Pass 1, data labels are mapped to `DATA_BASE + offset`. In Pass 2, when an instruction like `LA x1, arr` is parsed, `immAbsolute("arr")` looks up `arr` in the symbol table and returns its absolute address (e.g., 0x0400). The instruction becomes `LI x1, 0, 0x0400`.

**Q58:** What is the cycle cost breakdown for a trace LOAD instruction with a TLB miss and L1D miss (no L2)?
**A:** TLB hit latency: 1. Page walk: 10. (Page fault: 50 if new page; 0 if page table hit). Cache access: L1D miss latency (1) + memory latency (10) = 11. Best case (TLB miss, page table hit, L1D miss): 1 + 10 + 11 = 22 cycles. Worst case (TLB miss, page fault, L1D miss): 1 + 10 + 50 + 11 = 72 cycles.

**Q59:** Why does the TraceSimulator charge `cycles - 1` as stalls for load/store?
**A:** 1 cycle is considered "normal execution" (the instruction would take at least 1 cycle even with perfect cache/TLB). Everything beyond that is a penalty (stall). So if translation + cache takes 12 cycles total, 11 are stalls.

**Q60:** What's the relationship between `blockAddress` in CacheLine and the eviction address calculation?
**A:** `blockAddress` is stored but not used in the current eviction address calculation. Instead, the eviction address is reconstructed as `victim.tag * (blockSize * numSets) + set * blockSize`. This is equivalent but computed from the tag and set index. The `blockAddress` field is a leftover that could simplify the reconstruction.

**Q61:** Why does `ID_Stage` compute `latencyCyclesLeft = latency - 1` instead of just `latency`?
**A:** Because the first cycle of execution is the current EX tick. MUL has 3 cycles: the first cycle is when it enters EX (normal tick), and 2 additional cycles of stalling. So `latencyCyclesLeft = 3 - 1 = 2` remaining stall cycles.

**Q62:** What happens if you assemble a program with more than 256 instructions?
**A:** The Compiler's `buildSymbolTable()` method checks `if (textAddr > DATA_BASE)` and throws a RuntimeException with a clear message about text segment overflow. DATA_BASE = 0x0400 = 1024 bytes = 256 instructions.

**Q63:** How does `CacheLevel.getTag()` handle unsigned addresses?
**A:** It uses `Integer.divideUnsigned(address, blockSize * numSets)`. Without unsigned division, addresses above 2^31 would produce negative tags, causing tag mismatches and incorrect cache behavior.

**Q64:** Explain why the `oldMemWb` is checked in `resolveOperandA()`.
**A:** After the ForwardingUnit checks EX/MEM and the current MEM/WB, there's a third forwarding path from the *previous cycle's* MEM/WB (2 instructions ahead of the consumer). This catches the case where the producer has already passed through WB but the register file write hasn't been reflected yet in the same cycle. It's a 3-deep forwarding chain.

**Q65:** What would change if you switched from PIPT to VIVT?
**A:** VIVT (Virtually Indexed, Virtually Tagged) wouldn't require address translation before cache access — the TLB and cache could be accessed in parallel, reducing latency. However, VIVT creates aliasing problems: two different virtual addresses mapping to the same physical address could have separate, inconsistent cache entries. You'd need cache flushing on context switches and alias detection logic.

**Q66:** Why does the Compiler store data in little-endian byte order?
**A:** RISC-V is little-endian by default. When the `.word 9` directive emits bytes, it stores 0x09, 0x00, 0x00, 0x00 at consecutive addresses. The `Memory.readWord()` method reads a full `int` from `data[addr/4]`, which Java stores internally as a 32-bit big-endian value. The byte-level representation doesn't matter for word reads, but it matters for `LB`/`SB` instructions and for `Memory.writeByte()` which uses bit shifting based on `(address % 4) * 8`.

**Q67:** How does the `Lexer.stripComment()` handle comments inside string literals?
**A:** It tracks an `inQuote` boolean that toggles on each unescaped `"` character. When `inQuote` is true, `#` and `//` characters are treated as part of the string, not as comment markers. This correctly handles `.asciiz "Hello # World"`.

**Q68:** What's the purpose of the `DRAIN_THRESHOLD` in PipelineController?
**A:** When HALT is decoded in EX, there may still be instructions in MEM and WB that haven't completed. The drain threshold of 3 gives those instructions 3 additional cycles to flow through the remaining stages and be properly retired.

**Q69:** Why does the `TraceSimulator` use `int pc = 0x400000` (an artificial PC)?
**A:** Trace instructions don't have real PC values — they're just a list of memory accesses and ALU operations. The artificial PC prevents collisions with the data addresses in the traces. It's never actually used for branching (trace mode doesn't simulate instruction fetch).

**Q70:** What would happen to IPC if you doubled the L1D latency from 1 to 2 cycles?
**A:** Every L1D access (every LW/LB/SW/SB) would cost an extra cycle. For a program with many memory accesses (like bubble sort), total cycles would increase significantly while instructions retired stays the same → IPC would drop. For the trace workloads with ~357K memory accesses, the increase would be ~357K additional cycles.

---

## Hard (30 Questions)

**Q71:** Redesign this simulator for a 2-wide superscalar pipeline. What changes?
**A:** (1) IF must fetch 2 instructions per cycle. (2) ID must decode 2 simultaneously, checking for intra-group dependencies. (3) EX needs 2 ALU units. (4) Hazard detection becomes O(n²) — each instruction in the decode pair must check dependencies against every in-flight instruction AND against the other instruction in the pair. (5) Forwarding paths double. (6) Branch prediction must handle the case where a branch is the first of two fetched instructions (the second is potentially useless). (7) The register file needs 4 read ports and 2 write ports.

**Q72:** Your page eviction scans 1M entries. Design an O(1) LRU eviction mechanism.
**A:** Maintain a doubly-linked list of all mapped pages, ordered by access time. On every page access (TLB hit or page table hit), move the accessed page to the tail of the list (O(1) with the doubly-linked list). On eviction, remove the head of the list (the LRU page) — O(1). The trade-off: every memory access now requires a list manipulation (move-to-tail), which adds overhead per access. Use a `HashMap<Integer, ListNode>` for O(1) lookup of the node to move.

**Q73:** The simulator processes traces serially. How would you parallelize it for 100 traces?
**A:** Use Java's `ForkJoinPool` or `ExecutorService` with a thread pool. Each trace is independent (separate TraceSimulator instance, separate Stats, separate VMU). The only shared state is `Config`, which is read-only. Submit 100 trace tasks to the pool and collect results. Bottleneck: memory — each TraceSimulator creates a 128KB Memory + 4MB PageTable. With 100 instances: ~400MB. Use memory-mapped files or lazy page table allocation to reduce footprint.

**Q74:** Implement Tomasulo's algorithm for this processor. What data structures do you need?
**A:** (1) **Reservation Stations** — one per functional unit, holding opcode, operand values or tags, and ready bits. (2) **Reorder Buffer (ROB)** — circular buffer tracking in-order retirement: stores (opcode, destination, value, done bit). (3) **Common Data Bus (CDB)** — broadcasts completed results to all reservation stations and ROB entries that are waiting on that tag. (4) **Register Alias Table (RAT)** — maps architectural registers to ROB entries (for register renaming). The key challenge: the pipeline registers become unnecessary — the ROB handles ordering, and reservation stations handle issue.

**Q75:** Your cache uses a global clock counter for LRU. Can this overflow? What happens?
**A:** The clock is a `long` (64 bits). At 1GHz (10^9 accesses/second), it would take ~292 years to overflow. In practice, simulation runs are millions of cycles — no risk. But if it *did* overflow, `Long.MAX_VALUE + 1 = Long.MIN_VALUE`, which would break comparisons. Fix: periodically normalize timestamps by subtracting the minimum across all entries, or use unsigned comparison.

**Q76:** A student claims your forwarding unit has a bug with back-to-back stores followed by a load to the same address. Analyze.
**A:** Sequence: `SW x1, 0(x5)` → `SW x2, 0(x5)` → `LW x3, 0(x5)`. The load should get x2's value. The ForwardingUnit doesn't handle store-to-load forwarding — it only forwards *register* values, not *memory* values. Store-to-load forwarding is a separate mechanism (load-store queue) not implemented in this simulator. However, this is actually correct for this pipeline: stores write to the cache in MEM, and loads read from the cache in MEM. If both stores complete before the load reaches MEM, the load reads the latest value from the cache. The potential issue is if the load follows the second store too closely and the store hasn't written to the cache yet — but the HazardUnit doesn't detect this (stores don't set `rd`, so no RAW hazard is flagged). This is a valid concern for store-then-load dependencies through memory.

**Q77:** The CacheHierarchy has a `writeBackToL2()` method that's quite complex. Identify the edge case it handles.
**A:** When a dirty L1 block is evicted, and the corresponding block is NOT present in L2 (L2 miss for the write-back). The method must: (1) fetch the L2-sized block from memory, (2) overlay the dirty L1 words onto the correct position within the larger L2 block, (3) install this composite block into L2 (which may itself cause another eviction), (4) THEN write the dirty words. If it just wrote directly, L2 would contain only the dirty words with garbage in the rest of the block.

**Q78:** Your two-pass assembler can't handle forward references within the `.data` section (e.g., `.word label2` where `label2` is defined later in `.data`). How would you fix this?
**A:** Run the symbol table pass (Pass 1) to completion before emitting data (Pass 2). Since the current implementation already does this (Pass 1 builds the full symbol table for both `.text` and `.data`), and `resolveInt()` checks the symbol table for non-numeric values, forward `.data` references actually *do* work. The limitation is self-referential data items, which don't exist in practice.

**Q79:** Critique the `Stats` class from a SOLID perspective.
**A:** **Single Responsibility:** Violated — it holds pipeline stats, cache stats, and VM stats. Should be decomposed into `PipelineStats`, `CacheStats`, `VMStats`. **Open/Closed:** Violated — adding a new metric (e.g., branch prediction accuracy) requires modifying Stats. A better design would use a `Map<String, Long>` or event-based metrics. **Interface Segregation:** Violated — `StatsPrinter.printPipelineStats()` doesn't use VM fields, but they're exposed. **Public mutable fields:** No encapsulation. Any class can modify any counter at any time. In a larger system, this makes debugging very hard.

**Q80:** If this simulator needed to model cache coherence for a dual-core processor, what protocol would you implement and how would it interact with the existing CacheHierarchy?
**A:** MESI protocol. Each cache line gets a 2-bit state: Modified, Exclusive, Shared, Invalid. A bus snooping mechanism would intercept cache requests from the other core. On a read miss by Core B to a block that Core A has in Modified state: Core A writes back to L2, transitions to Shared. On a write by Core B: Core A's copy transitions to Invalid. The `CacheLevel.lookup()` method would need to check the snooping bus *before* returning a hit. The `CacheHierarchy` would need a shared bus object that both cores reference.

**Q81:** The `TraceSimulator.executeInstruction()` method has a complex stall loop with pipeline advancement. Walk through a scenario where a MUL followed immediately by an ADD using MUL's result causes stalls.
**A:** Instruction N: `MUL x5, x1, x2` (latency = 3, so `latencyCyclesLeft = 2`). After N is processed, `idEx` has `opcode=MUL, rd=5, latencyCyclesLeft=2`. Instruction N+1: `ADD x6, x5, x3` (rs1=5). When the HazardUnit checks: `idEx.latencyCyclesLeft > 0` → true → stall. The stall loop runs: cycle 1: `latencyCyclesLeft-- → 1`, advance MEM→WB. Cycle 2: still `latencyCyclesLeft > 0` → stall again, `latencyCyclesLeft-- → 0`. Cycle 3: `latencyCyclesLeft = 0` and `idEx.rd=5 == ifId.rs1=5` but with forwarding enabled, the load-use check doesn't apply to MUL (not a load). Exit stall loop. Total: 2 stall cycles for MUL.

**Q82:** If the Config allowed runtime parsing of INI files (as the README implies), what's the attack surface?
**A:** If the INI parser evaluates expressions or allows environment variable expansion, path traversal or code injection is possible. For integer parameters, `Integer.parseInt()` on malicious input would throw exceptions but not execute code. The main risk is resource exhaustion: `physicalSizeBytes = 999999999999` → OOM. `dtlbEntries = 0` → `IllegalArgumentException` in TLB constructor. `pageSizeBytes = 0` → division by zero in VMU. Mitigation: validate all parameters against reasonable bounds.

**Q83:** Your cache hierarchy doesn't model cache line coherence between L1D and L2 during a load. Is this correct?
**A:** For a single-core system with write-back caches, L1D and L2 are *inclusive* by design: every block in L1D came from L2 (or memory). On a write, L1D is modified (dirty) but L2 is not updated — L2 may hold stale data. This is correct because L1D is the authoritative copy. On L1D eviction, the dirty block is written back to L2, restoring coherence. The only risk is L2 evicting a block that's still in L1D (non-inclusive behavior) — the current implementation doesn't handle this, but since L2 ≥ L1D, it's unlikely in practice.

**Q84:** Analyze the IPC of the bubble sort program. Why is it 0.164?
**A:** The program retires 2,514 instructions in 15,291 cycles. The dominant cost is stalls (12,289), primarily from cache misses. With L1I and L1D configured, the inner loop accesses different memory addresses for array elements, causing compulsory misses that go to L2 (50 cycles each) or memory (200 cycles). Each cache miss freezes the entire pipeline. The nested loop structure (23 elements → ~253 inner iterations) means many LW/SW instructions, each potentially missing in L1D. The 242 branch flushes (from mispredictions at loop boundaries) contribute minimally compared to the cache miss penalties.

**Q85:** Design a non-blocking cache (MSHR) for this simulator. What changes?
**A:** Add a Miss Status Holding Register (MSHR) table to `CacheLevel`. On a miss, instead of immediately fetching the block and stalling, allocate an MSHR entry that records the pending request (address, destination register, waiting instruction). The pipeline continues executing independent instructions. When the fetch completes, the MSHR signals the pipeline to replay the dependent instruction. This requires: (1) an MSHR table with N entries, (2) dependency tracking in the pipeline, (3) instruction replay logic, (4) multiple outstanding misses. The `PipelineController` would need to track multiple pending loads and check MSHR completion each cycle.

**Q86:** Your assembler converts `BEQ x0, x0, inner` to an unconditional backward jump. But BTFNT predicts it as taken (backward). Does this ever mispredict?
**A:** No. `BEQ x0, x0, label` is semantically always-taken (0 always equals 0). BTFNT predicts backward branches as taken. Since the actual outcome is always taken, the prediction is always correct. This is the best case for BTFNT — a known-taken backward branch. The only misprediction for backward branches occurs when the loop eventually exits (e.g., `BGE x3, x2, end` when `x3 >= x2`), which is a backward branch predicted taken but actually not taken.

**Q87:** The flat page table uses 1M entries. With a 4GB virtual space and 32-bit addresses, derive the entry count mathematically.
**A:** Virtual address = 32 bits. Page size = 4KB = 2^12 bytes. Page offset = 12 bits. VPN = 32 - 12 = 20 bits. Number of pages = 2^20 = 1,048,576. Each `PageTableEntry` has: `valid` (1B), `frameNumber` (4B), `dirty` (1B), `lastUsed` (8B), `insertOrder` (8B) = 22 bytes raw + Java object overhead (~40 bytes with header/alignment). Total: ~40MB for the page table array. With 64 physical frames, at most 64 entries are valid at any time.

**Q88:** What invariant must hold between the TLB and the page table?
**A:** For every valid TLB entry with (VPN → PFN), the corresponding page table entry must also be valid with the same frame number. The reverse is NOT required — the page table may have valid entries not in the TLB (TLB is a strict subset). Additionally, if a TLB entry has `dirty = true`, the PTE must also eventually learn this (either immediately or on TLB eviction via a write-back). The current implementation updates both simultaneously on a store hit.

**Q89:** Explain the exact conditions under which the `CacheHierarchy.invalidateFrame()` method is called and why skipping it would produce incorrect results.
**A:** Called in `VirtualMemoryUnit.evictPage()` when a physical frame is freed and reassigned. Without it: (1) Page A maps to frame 5. (2) Cache line at physical address `frame5 + offset` holds Page A's data. (3) Page A is evicted, frame 5 is given to Page B. (4) Page B's data is loaded into frame 5. (5) A subsequent access to a physical address in frame 5 would *hit* in L1D and return Page A's old (stale) data. With invalidation, step 2.5 wipes those cache lines, forcing a miss and a fresh fetch of Page B's data.

**Q90:** Analyze the trace09 worst case: 58.7M cycles for 715K instructions. Derive the per-instruction cost breakdown.
**A:** IPC = 0.0122 → CPI = 82 cycles per instruction. TLB: 0 hits, 357,876 misses → every instruction misses the TLB. Per TLB miss: 1 (probe) + 10 (walk) = 11 cycles base. 357,876 page faults → every TLB miss also faults: +50 cycles = 61 cycles. 357,812 page evictions, 125,515 dirty → heavy swap traffic. Cache: 100% L1D miss rate → every memory access pays L1D (1) + memory (10) = 11 cycles. Combined per load/store: ~61 (translation) + 11 (cache miss) = 72 cycles. Non-memory instructions (ADD/MUL) still pay TLB misses for their memory operands. Total translation penalty: 21,830,436 cycles (matching the stats).

**Q91:** If the `CacheConfig` validation allows `blockSize > size` (which it doesn't — it checks divisibility), what would happen?
**A:** `getNumSets()` would return 0 (size / (blockSize * associativity) < 1, truncated to 0). The `sets` array would be `CacheLine[0][assoc]` — zero sets. Every `getSetIndex()` would compute `floorMod(address / blockSize, 0)` → ArithmeticException (division by zero). The constructor validation prevents this: `size % (blockSize * associativity) != 0` catches all cases where the geometry doesn't divide evenly.

**Q92:** How would you extend the simulator to support exceptions and interrupts?
**A:** (1) Add an exception vector table in memory. (2) On exception (e.g., illegal instruction, division by zero), save PC and status to special registers (CSRs), jump to exception handler address. (3) For interrupts, check an interrupt-pending flag at the end of each cycle; if pending and interrupts are enabled, flush the pipeline and jump to the interrupt handler. (4) Need to model privilege levels (user/supervisor/machine) and CSR registers (mstatus, mepc, mcause). (5) The ECALL instruction currently just dumps registers — it should trigger a supervisor call exception.

**Q93:** Your `writeBackToL2()` method reads from memory to construct the L2 block, even though the dirty L1 data should overwrite part of it. Is this correct?
**A:** Yes. The L2 block is larger than or equal to the L1 block (L2 blockSize ≥ L1 blockSize by design). The dirty L1 block only covers *part* of the L2 block's address range. The rest of the L2 block must contain the correct data from memory. So the method: (1) fetches the full L2-sized block from memory, (2) overlays the dirty L1 words at the correct offset, (3) installs this composite block into L2. This ensures the L2 block is fully populated with correct data.

**Q94:** Explain the race condition risk in the PipelineController's handling of `ifStallCycles`.
**A:** After `IF_Stage.tick()` returns, `ifId.fetchLatencyLeft` is checked. If positive, it's transferred to `ifStallCycles` and zeroed in `ifId`. On the next cycle, the stall loop handles it. The risk: if a branch misprediction *also* occurs on the same cycle as an IF cache miss, the misprediction handler sets `ifId = new IF_ID()` (zeroing the instruction), but `ifStallCycles` has already been set from the old `ifId`. The stall would freeze the pipeline for the remaining cache miss cycles, then resume with a wrong PC. However, looking at the code, the misprediction is detected from `newExMem`, which is computed *before* IF runs. If a misprediction is detected, the `else` block (normal flow with IF/ID) is never reached, so `ifStallCycles` is never set from the flushed instruction. The code is actually safe.

**Q95:** Propose a testing strategy that achieves full branch coverage of the HazardUnit.
**A:** Need test cases for each return path: (1) Multi-cycle stall: MUL instruction in ID/EX with `latencyCyclesLeft > 0`. (2) Load-use with forwarding ON: LW in ID/EX, next instruction reads same rd. (3) RAW with forwarding OFF, producer in ID/EX. (4) RAW with forwarding OFF, producer in EX/MEM. (5) No hazard (no dependency). (6) Dependency on x0 (should NOT stall — x0 is always 0). (7) Store in ID/EX — `writesBack()` returns false, so no stall even if registers match. (8) NOP in IF/ID (early return).

**Q96:** The simulator uses Java's `int` (32-bit signed) for addresses, but virtual addresses are logically unsigned 32-bit values. Where could this cause problems?
**A:** (1) `Integer.divideUnsigned()` is used correctly in VPN calculation and cache addressing. (2) `Memory.readWord(address)` computes `idx = address / 4` — this uses signed division. For addresses ≥ 2^31 (0x80000000+), this would produce negative indices and fail the `inBounds()` check. (3) The default physical memory is 128KB (max address 0x1FFFF), well within signed range. But if someone configured physical memory to 2GB+, addresses would go negative. (4) InstructionEncoder uses `& 0x1F` masking which works correctly regardless of signedness.

**Q97:** Design an experiment to demonstrate Belady's anomaly with your FIFO cache.
**A:** Use a FIFO cache with 3 sets (direct-mapped, 3 sets × 1 way). Access pattern: addresses mapping to sets 0, 1, 2, 3, 0, 1, 2, 3, ... (cyclic with period 4 > 3 sets). Count misses. Then increase to 4 sets. With 4 sets (still cyclic period 4), every access *could* be a hit — but with FIFO, the eviction order might cause more misses than with 3 sets for certain access patterns. The classic Belady sequence is: 1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5. With 3 frames (FIFO): 9 faults. With 4 frames (FIFO): 10 faults.

**Q98:** The trace simulator handles BRANCH instructions with a flat 2-cycle penalty. Is this accurate for a pipelined processor?
**A:** It's a simplification. In the pipeline mode, a taken branch causes a 2-instruction flush (IF/ID and ID/EX). The cost depends on whether the branch was predicted correctly. For trace mode, since there's no instruction fetch simulation and all trace branches are resolved (the trace records the taken path), the 2-cycle penalty approximates the average cost. A more accurate model would apply BTFNT prediction to the branch offset and only charge the penalty on misprediction.

**Q99:** If you had to port this simulator to process traces at 100x the current speed, what's the bottleneck and how would you optimize it?
**A:** The bottleneck is the `evictPage()` O(N) scan of 1M page table entries. For trace09 with 357,876 page evictions, that's 357,876 × 1,048,576 = ~375 billion comparisons. Optimization: (1) Maintain a linked list of mapped pages → O(1) eviction. (2) Use primitive `int[]` arrays instead of `PageTableEntry` objects → eliminate Java object overhead (header, alignment). (3) Pool/reuse objects instead of creating new `MEM_WB`, `EX_MEM` objects every cycle. (4) Use `int` arithmetic instead of `long` for timestamps (safe within simulation scale). (5) Memory-map the trace file instead of line-by-line parsing.

**Q100:** Your simulator doesn't model speculative execution. If it did, what security implication exists?
**A:** Spectre-class vulnerabilities. If the simulator modeled speculative execution beyond branch prediction (speculatively executing loads from untrusted addresses), cache side-channel attacks would be possible: a speculatively executed load would change the cache state (populating a line), and an attacker could probe cache timing to infer the speculated value even after the speculative path is squashed. Modeling this would require: (1) tracking speculative vs committed cache state, (2) rolling back cache changes on misprediction, (3) simulating cache timing side channels.

---

# SECTION 10 — Resume Grilling

## Bullet 1: "Built a cycle-accurate 5-stage pipelined RISC-V simulator across 42 Java source files, supporting 25 instructions (21 native opcodes + 4 pseudo-instructions) with a configurable 3-level cache hierarchy (L1I/L1D/L2), fully-associative 16-entry DTLB, and virtual memory subsystem with swap-space persistence."

**Q:** "You say cycle-accurate. How do you define a 'cycle' in your simulation? Is it the same as a real hardware clock cycle?"
**A:** A cycle in the simulator represents one iteration of the simulation loop where all pipeline stages tick once. It's functionally equivalent to a clock edge in hardware — the PC advances, pipeline registers latch, and one unit of work progresses through each stage. It's "cycle-accurate" in the sense that every stall, bubble, and penalty is counted as an integer number of cycles, matching what a real hardware implementation would produce for the same instruction sequence and configuration.

**Q:** "You claim 42 source files. How did you decide the granularity? Why not 10 files or 100?"
**A:** The granularity follows the hardware structure: each pipeline stage is a class, each pipeline register is a class, each cache/VM component is a class. This maps naturally to how COA textbooks draw block diagrams. 10 files would conflate unrelated logic (e.g., cache + VM in one file). 100 would be over-decomposed (e.g., separate files for each ALU operation). The 42-file count follows the principle of one class per hardware unit.

**Q:** "What makes your 4 pseudo-instructions different from the 21 native opcodes?"
**A:** Pseudo-instructions don't have their own opcode in the Opcode enum. NOP → `ADDI x0, x0, 0`, MV → `ADDI rd, rs, 0`, LA → `LI rd, absolute_address`, LI → has its own opcode but is semantically a pseudo-instruction (it's not in the RISC-V base ISA as a standalone instruction). The assembler translates them during parsing.

## Bullet 2: "Implemented BTFNT static branch prediction with 2-cycle flush on misprediction, data forwarding, and load-use hazard stall logic; characterized IPC across 10 trace workloads (~715K instructions each), ranging from 0.164 on cache-warm workloads to 0.027 on adversarial traces with 0% TLB hit rate."

**Q:** "Why is the misprediction penalty exactly 2 cycles? Could it be 1 or 3?"
**A:** The penalty is 2 because at the time the branch is resolved (EX stage), two younger instructions have already been fetched (one in IF/ID, one in ID/EX). Both must be squashed. This is specific to a 5-stage pipeline where branches are resolved in EX. If the branch were resolved in ID (common in some designs), the penalty would be 1. If resolved in MEM, it would be 3.

**Q:** "You report IPC of 0.027 for trace09. That means each instruction takes ~37 cycles on average. Where do those cycles go?"
**A:** Primarily translation penalties: every instruction triggers a TLB miss (0% TLB hit rate) + page fault = 61 cycles for address translation alone. Plus L1D cache misses (100% miss rate) adding ~11 cycles per memory access. Plus MUL stalls and hazard stalls. The 26M stall cycles out of 26.6M total cycles confirms the pipeline is stalled >97% of the time.

**Q:** "What would the IPC be if you had infinite physical memory (no page faults)?"
**A:** Be honest — I haven't run that specific experiment. But based on the trace06 data where TLB hit rate is 0% but with reduced page faults from different access patterns, the cycle count drops to ~23M. With infinite memory, page faults and evictions would be zero, TLB misses would still cost 11 cycles each (hit latency + walk), and cache misses would still add 11 cycles. Rough estimate: IPC would improve to approximately 0.04-0.05.

## Bullet 3: "Designed configurable LRU/FIFO replacement at all three levels — cache lines, TLB entries, and page table entries — via a single config parameter; measured L1D hit rates from 99.7% on spatial-locality workloads to 0% on stride-8 access patterns, demonstrating direct-mapped conflict miss behavior."

**Q:** "Explain the stride-8 conflict miss pattern. Why does stride-8 specifically cause 0% hit rate?"
**A:** With a 4KB direct-mapped L1D (64 sets × 64B blocks), two addresses map to the same set if they differ by a multiple of 4KB. A stride-8 access pattern (accessing every 8th page) means consecutive accesses map to the same set — each access evicts the previous one, producing 100% miss rate. This is a classic conflict miss pathology of direct-mapped caches that disappears with even 2-way associativity.

**Q:** "You say 'single config parameter' controls replacement at all three levels. Is that actually true?"
**A:** Partially. The Config has one `vmReplacementPolicy` string (for TLB and page table) and one `ReplacementPolicy` enum in CacheConfig (for cache levels). However, the cache policy is set per CacheConfig instance, so theoretically L1D could use LRU while L2 uses FIFO. The VM replacement policy (TLB + page table) is genuinely controlled by a single parameter. My resume bullet slightly overstates this — it's really two parameters that share the same value by default.

## Bullet 4: "Built a trace-driven performance analytics engine reporting IPC, CPI, cache hit/miss rates, branch mispredictions, TLB statistics, and dirty page eviction counts; validated correctness across 10 trace files and a bubble sort program verified against memory dump output."

**Q:** "How did you validate correctness of the bubble sort output?"
**A:** After the pipeline completes, the data segment is dumped from memory. For bubble sort with input `[9,7,5,3,1,2,4,6,8,15,14,13,12,11,10,16,17,17,18,18,18,19,20]`, the dump should show the 23 integers in ascending order `[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,17,18,18,18,19,20]`. I verified this manually by inspecting the `console.txt` memory dump.

**Q:** "How did you validate that cache miss rates are correct, not just plausible?"
**A:** By analyzing the access patterns mathematically. For the direct-mapped L1D with known block size and number of sets, I can predict which addresses conflict. For trace01 with 8 unique pages, the TLB has 16 entries → 100% TLB hit rate (8 < 16). The L1D with 64 sets and direct mapping should show specific conflict patterns based on the stride. If the measured rates match the predicted rates, the implementation is correct.

**Q:** "Did you do differential testing against another simulator?"
**A:** No. The validation was manual: mathematical prediction of expected miss rates, memory dump verification for bubble sort, and consistency checks (e.g., swap-ins ≈ swap-outs for pages that are re-accessed). Differential testing against Spike (the reference RISC-V simulator) would be a valuable improvement.

---

# SECTION 11 — Behavioral Questions

## Biggest Technical Challenge

**Situation:** The cache hierarchy needed to support L1I, L1D, and optional L2 with correct stats counting, dirty eviction cascading, and write-allocate on store misses — while also working correctly when any level was null.

**Task:** Design a `CacheHierarchy` class that handles all combinations: L1I+L1D+L2, L1D-only, no-cache, and ensures each pipeline request counts as exactly one access.

**Action:** I created the `readThrough()` and `fetchBlockToL1()` methods with null checks at every level. The key insight was the `NoStats` methods — `readWordNoStats()` and `writeWordNoStats()` — which let internal operations (block fills, write-backs) use the same cache without inflating counters. I also separated the L1 miss path into a `FetchResult` object that carries both the block data and the accumulated latency.

**Result:** The cache hierarchy correctly handles all 4 configurations with zero code duplication. Stats match mathematical predictions for known access patterns.

## Hardest Bug Fixed

**Situation:** During trace testing, the L1D miss rate was showing 50% when it should have been 100% for stride-8 access patterns.

**Task:** Find why half the expected misses were being counted as hits.

**Action:** I traced through the `writeData()` path and discovered that on a write miss, after `fetchBlockToL1()` installed the block, the subsequent `writeWordNoStats()` was not updating the dirty bit correctly in some cases. More critically, I found that the stats were being double-counted: the initial `lookup()` call counted a miss, and then the `readWord()` call inside the fetch path counted another access (this time a hit, since the block was just installed). I introduced the `NoStats` methods to separate stats-counted pipeline accesses from internal bookkeeping.

**Result:** After the fix, miss rates exactly matched mathematical predictions. This fix also revealed the importance of the stats policy design — without `NoStats`, a single load instruction could generate 16 L1 accesses (one per word in the block fill).

## What I'd Redesign

If starting over, I'd implement a proper INI config file parser instead of hardcoded defaults in `Config.java`. Currently, changing any parameter requires recompiling. I'd also replace the flat page table with a multi-level table and maintain a linked list of mapped pages for O(1) LRU eviction instead of the O(N) scan.

## Proudest Feature

The trace replay mode sharing the same infrastructure as the pipeline mode. Both modes use the same `CacheHierarchy`, `Stats`, `Config`, and `HazardUnit` — zero code duplication. This wasn't an accident; I explicitly refactored the cache and hazard code to be mode-agnostic when adding trace support.

## Time Management

The project was developed across 3 phases: Phase 1 (basic pipeline without cache), Phase 2 (cache hierarchy), Phase 3 (virtual memory + trace replay). I structured each phase as an independent deliverable — Phase 1's direct-memory path still works when cache is null (backward compatibility). This let me test each phase independently without breaking previous functionality.

## What I'd Do Differently

Add automated unit tests from the start. I validated correctness manually (memory dumps, mathematical predictions, trace analysis), but as the codebase grew, regression testing became error-prone. A JUnit suite testing each module (encoding round-trip, hazard detection cases, cache eviction correctness) would have caught bugs faster.

---

# SECTION 12 — Explain to Different Audiences

## HR/Recruiter
"I built a software simulation of a computer processor for my Computer Architecture course. Think of it like a flight simulator, but for CPU hardware. It models how instructions flow through a processor's assembly line, how data is cached for fast access, and how virtual memory works. I used Java, wrote 42 files of code, and validated it against real workloads of 700,000+ instructions each."

## Software Engineer (Peer Level)
"It's a cycle-accurate RISC-V pipeline simulator in Java — 5 stages (IF/ID/EX/MEM/WB) with forwarding, BTFNT branch prediction, a 3-level set-associative cache hierarchy, and a full VM subsystem (TLB, flat page table, swap space). The interesting engineering is in the cache stats policy (one counted access per pipeline request), the null-safe L2 design, and reusing pipeline infrastructure (HazardUnit, pipeline registers) in the trace replay mode to accurately model hazard stalls without code duplication."

## Senior Engineer
"The architecture is intentionally modular: 9 packages mapping to hardware subsystems, with the pipeline stages, cache hierarchy, and VM communicating through well-defined interfaces (pipeline registers, AccessResult, TranslationResult). Design decisions worth noting: reverse-order stage ticking to avoid double-buffering, NoStats methods to decouple pipeline-visible access counting from internal cache operations, and a shared HazardUnit between pipeline and trace modes. The main scalability limitation is the O(N) page eviction scan — I'd replace it with a doubly-linked list in a production setting."

## CTO
"This project demonstrates deep understanding of hardware-software interaction at the microarchitectural level — pipeline hazard resolution, cache coherence (PIPT invalidation), and virtual memory management. The clean modular architecture (9 independent packages, zero external dependencies, shared infrastructure between modes) shows software engineering discipline. The performance analysis across 10 trace workloads shows data-driven characterization of architectural tradeoffs (TLB size vs hit rate, associativity vs conflict misses, physical memory size vs page fault rate)."

## Non-Technical Family Member
"You know how your computer has a processor — that little chip that runs everything? I built a pretend version of that processor using software. It shows exactly how the processor handles instructions step by step, like an assembly line in a factory. I can change the settings — like making the memory faster or slower — and see how it affects the computer's speed. It's like building a miniature model of a car engine to understand how all the parts work together."

## 12-Year-Old Child
"Imagine you have a toy factory with 5 stations. Station 1 grabs a toy instruction card. Station 2 reads what the card says. Station 3 does the work (like adding numbers). Station 4 checks the toy box for parts. Station 5 puts the finished toy on the shelf. My program pretends to be this whole factory! And sometimes, station 4 says 'I don't have that part!' and the whole factory has to wait — that's called a 'cache miss.' I built all of this to figure out how to make the factory work faster."

---

# SECTION 13 — Rapid Fire Revision Notes

## Tech Stack
- **Backend Language:** Java 17+ — OOP maps to hardware, records for Instruction, zero external dependencies
- **Frontend:** React 18 + TypeScript + Vite — `web/` directory, deployed to Vercel
- **Deployment:** Heroku (Docker) for backend, Vercel (static) for frontend
- **Build:** `javac -d out -sourcepath src src/Main.java` — no build tool needed
- **Run (pipeline):** `java -cp out Main input.asm`
- **Run (trace):** `java -cp out Main --trace phase3_traces/trace01.trace`
- **Run (batch):** `java -cp out Main --trace-all phase3_traces`
- **Run (API server):** `java -cp out Main --server` — starts HTTP server on port 8080 (or `$PORT`)
- **Frontend dev:** `cd web && npm run dev` — Vite proxies `/api` → `localhost:8080`

## Architecture Steps (Pipeline Mode)
1. Lexer strips comments → Parser resolves labels → Compiler emits Instructions + DataItems
2. Processor creates Memory, RegisterFile, CacheHierarchy, Stats
3. Program encoded into memory as 32-bit words (InstructionEncoder)
4. PipelineController loops: check cache stalls → HazardUnit → WB→MEM→EX → resolve branch → ID→IF → cycle++
5. Stats collected from CacheLevel objects → printed to output.txt

## Key Parameters
- L1D: 4KB, 64B blocks, direct-mapped, 1-cycle hit
- Memory latency: 10 cycles
- MUL: 3 cycles, DIV: 4 cycles
- TLB: 16 entries, fully-associative, 1-cycle probe, 10-cycle walk, 50-cycle fault
- Physical memory: 256KB (64 frames), page size: 4KB
- Max cycle limit: 100,000, drain threshold: 3

## Common Q&A Pairs
- Why reverse-order ticking? → Avoids same-cycle data races
- Why load-use always stalls? → Data not ready until end of MEM
- Why BTFNT? → Correctly predicts loop back-edges (>95% of backward branches)
- Why NoStats methods? → Block fills shouldn't inflate hit/miss counters
- Why PIPT invalidation? → Prevents stale data when frame is reassigned
- Why flat page table? → Simple O(1) lookup; O(N) eviction is acceptable for simulation
- Why custom encoding? → Simpler than standard RV32I; sufficient for project scope
- Why HashMap for swap? → Only dirty evicted pages are stored; sparse, O(1) access

---

# SECTION 14 — Mock Interview

**Interviewer:** So I see you built a RISC-V processor simulator. Walk me through it at a high level.

**You:** Sure. It's a cycle-accurate 5-stage in-order pipeline simulator in Java with a full memory hierarchy — and a React/TypeScript web frontend that makes it interactive in the browser. The 5 stages — Fetch, Decode, Execute, Memory, Write Back — are each modeled as separate classes that communicate through pipeline register objects. I implemented data forwarding to eliminate most RAW hazard stalls, BTFNT branch prediction to handle control hazards, and a configurable 3-level cache hierarchy with a complete virtual memory subsystem including TLB, page table, and swap space. There are four modes: pipeline mode, trace replay mode, batch trace mode, and an API server mode. The API server exposes 9 REST endpoints via Java's built-in `HttpServer` — no external dependencies — so the React frontend can assemble programs, run simulations, upload traces, and display live stats without any CLI knowledge.

**Interviewer:** Interesting. You mentioned forwarding eliminates "most" RAW stalls. When doesn't it work?

**You:** Load-use hazards. When a load instruction is followed by an instruction that uses the loaded value, forwarding can't help because the load data isn't available until the end of the MEM stage. Even with forwarding, the consumer instruction in EX needs the data one cycle before it exists. So there's an unavoidable 1-cycle stall — the pipeline inserts a bubble and the consumer instruction re-reads the value via MEM/WB forwarding the next cycle.

**Interviewer:** Makes sense. What about the branch predictor — why BTFNT instead of a dynamic predictor?

**You:** BTFNT was a pragmatic choice for the project scope. It has zero storage overhead — no branch history table or pattern registers — and achieves high accuracy on loop-dominated code because backward branches are loop back-edges that are taken over 95% of the time. For my bubble sort benchmark, the inner loop branch is backward and correctly predicted for 249 out of 250 iterations, only mispredicting on the loop exit. A 2-bit saturating counter would improve accuracy on data-dependent forward branches, but the added complexity wasn't justified for this project.

**Interviewer:** Let's go deeper on the cache. Your README mentions a "stats policy" — one counted access per pipeline request. Why does that matter?

**You:** It's critical for accurate miss rate measurement. When the pipeline does a load and gets an L1 miss, I need to fetch a full 64-byte block from L2 or memory — that's 16 words. If each of those word reads during the block fill counted as a separate L1 access, a single load instruction would generate 1 miss + 15 hits, showing a miss rate of 6.25% when it should be 100%. I solved this with `readWordNoStats()` and `writeWordNoStats()` methods that the block fill and write-back paths use. Only the initial pipeline-triggered lookup calls `readWord()`, which increments the counter.

**Interviewer:** Smart. Now, your virtual memory does a full page table scan on eviction — that's O(N) with N over a million. Doesn't that concern you?

**You:** Absolutely. For trace09 with 357,000 page evictions, that's potentially 375 billion comparisons. In the simulator, it's fast enough because it's a tight loop over a primitive array, and JIT compilation optimizes it well. But it's the clear bottleneck for larger workloads. The fix is to maintain a doubly-linked list of mapped pages ordered by access time — on every access, move the page to the tail (O(1) with the linked list), and on eviction, remove the head (O(1)). The trade-off is that every memory access now requires a list manipulation.

**Interviewer:** Good awareness. Let me ask about an edge case: what happens if both the IF stage and the MEM stage have cache misses in the same cycle?

**You:** MEM takes priority. The PipelineController has separate counters — `memStallCycles` and `ifStallCycles`. In the stall-handling code, `memStallCycles` is decremented first. Only when MEM stall is fully resolved does the IF stall begin counting down. This correctly models a single-ported memory bus where data memory access has priority over instruction fetch — which is the standard behavior in most real processors.

**Interviewer:** One more technical question. You mentioned PIPT cache invalidation on frame eviction. Why is that necessary?

**You:** Without it, you get stale data. Here's the scenario: Page A is mapped to frame 5, and data from Page A is cached in L1D with physical address tags referencing frame 5. Now Page A is evicted, and Page B is loaded into frame 5. If we don't invalidate, a subsequent access to the *same physical address* in frame 5 would hit in L1D and return Page A's old data instead of Page B's new data. My `invalidateFrame()` method walks through all cache blocks that fall within the frame's address range and marks them invalid, forcing a fresh fetch from memory on the next access.

**Interviewer:** I notice there's also a web frontend. How does that connect to the Java simulator?

**You:** I added `ApiServer.java` — a lightweight HTTP server using Java's built-in `com.sun.net.httpserver`, which ships with the JDK, so zero external dependencies. It exposes 9 REST endpoints: you can get and set the ASM file, trigger a pipeline simulation, fetch the output files, upload a trace file for replay, or list and fetch preset traces. The React/TypeScript frontend — deployed on Vercel — calls these endpoints via Axios. In dev, Vite proxies `/api` to `localhost:8080`; in production, the `VITE_API_URL` env var points to the Heroku backend. The backend runs in a Docker container on Heroku. One design challenge was preventing concurrent simulations — I use an `AtomicBoolean` running flag with compare-and-set, so a second request while a simulation is running immediately gets a 409 Conflict.

**Interviewer:** Excellent. Last question: if you had unlimited time, what would you add?

**You:** Four things. First, dynamic branch prediction — a 2-bit saturating counter or GShare predictor. The infrastructure is already there (the ID stage sets prediction, EX resolves it); I'd just need the history table. Second, an actual INI config file parser exposed through the web UI — right now all parameters are hardcoded in Config.java, which means recompiling to change any setting. Third, automated regression tests — a JUnit suite testing encoding round-trips, hazard detection edge cases, cache eviction correctness, and golden-file comparison for known traces. Fourth, cycle-accurate pipeline animation in the frontend — instead of a static diagram, show each instruction flowing through the stages in real time with stalls and flushes highlighted, driven by the cycle-by-cycle log in console.txt.

**Interviewer:** Great answers. Thanks for the deep dive — this is clearly a project you understand at every level.

**You:** Thank you. It was one of the most rewarding projects I've worked on — building each layer from scratch, from the assembler through the pipeline to the virtual memory system and finally wrapping it all in a web interface, really cemented my understanding of how hardware and software interact at the microarchitectural level.

---

# SECTION 15 — Resume Bullet-Driven & Deployed Project Questions

Because interviewers often won't look at the raw codebase, they will base their questions on your resume bullets and a live demo of your deployed project. Be prepared to answer these high-level architectural and behavioral questions:

## Deployed Project Questions (Frontend / API)

**Q: Walk me through what happens when I click "Run Simulation" on your frontend.**
**A:** When you click "Run", the React frontend makes an Axios POST request to `/api/run` on the Heroku-hosted Java backend. The backend `ApiServer` receives the request and flips an `AtomicBoolean` lock to prevent concurrent simulations. It invokes the `Compiler` to convert your assembly text into 32-bit machine code, loads it into the `Memory` object, and starts the `PipelineController`. The pipeline runs cycle-by-cycle until it hits a `HALT` instruction. The server then writes the logs to `console.txt` and `output.txt`, flips the lock back to false, and returns an HTTP 200 OK. The frontend then makes subsequent GET requests to fetch those log files and updates the React UI to display the stats dashboard and pipeline diagram.

**Q: Your server uses Java's built-in `HttpServer` with zero dependencies. Why not use Spring Boot?**
**A:** Spring Boot is fantastic, but it's very heavy. It requires Maven or Gradle and downloads hundreds of megabytes of dependencies. My goal for this project was to keep it as raw and close to the metal as possible—both in the simulated processor and in the backend code. Using the JDK's built-in `com.sun.net.httpserver` kept the project lightweight, making it incredibly easy to compile and deploy via a simple Dockerfile on Heroku without any heavy build tools.

**Q: How do you handle multiple users trying to use the simulator at once?**
**A:** The `ApiServer` uses a `CachedThreadPool` to handle incoming HTTP requests concurrently, which is great for serving the static files and trace lists simultaneously. However, the simulation itself modifies shared state (`input.asm`, `output.txt`). To prevent race conditions, I use an `AtomicBoolean` flag called `running`. If User A starts a simulation, it sets `running` to true. If User B clicks run at the same time, the server checks the flag, sees it's true, and immediately returns a `409 Conflict` (Server Busy) error.

## Resume-Driven Questions (Core Architecture)

**Q: Your resume says "Cycle-accurate 5-stage RISC-V simulator". How do you guarantee it's cycle-accurate?**
**A:** I designed the `PipelineController` to tick the 5 stages in *reverse order* (Writeback, Memory, Execute, Decode, Fetch) during every iteration of the `while` loop. This mathematically guarantees that data passed from one stage to the next cannot "skip" ahead in the same clock cycle, mimicking the behavior of physical pipeline latencies and hardware clock edges.

**Q: You mention "Data Forwarding and Hazard Resolution". Can you explain a scenario where your hazard unit saves cycles?**
**A:** Yes! Without data forwarding, a simple RAW (Read-After-Write) hazard—like an `ADD` instruction followed immediately by a `SUB` that needs the result—would require a 2-cycle stall while waiting for the `ADD` to reach the Writeback stage. My `ForwardingUnit` intercepts the result directly from the `EX/MEM` pipeline register and feeds it straight back into the ALU input for the `SUB` instruction, completely eliminating those two stall cycles.

**Q: Your resume highlights a "Virtual Memory Unit with TLB and Page Replacement". Why is a TLB necessary?**
**A:** Every memory access requires translating a Virtual Address to a Physical Address. If we had to walk the Page Table in memory for every translation, it would add massive latency (10 cycles in my simulation) to every single instruction fetch and data load. The TLB (Translation Lookaside Buffer) acts as a high-speed cache (1 cycle latency) for those mappings. By keeping recent translations in the fully-associative TLB, we avoid the 10-cycle penalty on almost all memory accesses.

**Q: You tested this with "715K instruction trace workloads". What was the biggest bottleneck you discovered?**
**A:** The biggest bottleneck was page faults when physical memory (RAM) filled up. On one adversarial trace, the simulator hit 357,000 page faults. Because my page replacement algorithm (LRU) does a brute-force O(N) scan across the entire flat page table to find the victim frame, the simulation slowed down significantly. It proved to me mathematically why modern operating systems use complex, hardware-assisted tree structures and clock algorithms for page replacement rather than simple linear arrays.
