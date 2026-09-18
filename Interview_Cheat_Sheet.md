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
├── railway.yml                        Railway Docker deployment config
└── vercel.json                       Vercel SPA routing config (rewrites → index.html)
```
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
11. **Frontend/backend decoupling via CORS + proxy** — The Vite dev server proxies `/api` to `localhost:8080`, so the frontend works identically in dev and production. CORS headers (`Access-Control-Allow-Origin: *`) are set on every response so the Vercel-deployed frontend can call the Railway-hosted backend.


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

Here is a detailed breakdown of SSR and SEO, how they are connected, and exactly how to talk about them in an interview.

### 1. SEO (Search Engine Optimization)
**What it is:** SEO is the practice of making your website rank higher on search engines like Google. 
**How it works:** Google uses automated bots (called "web crawlers" or "spiders") to constantly browse the internet. When a bot visits a website, it reads the HTML code to figure out what the page is about (looking at titles, headers, and text). If the bot understands the content, Google can show that page to users searching for relevant keywords.
**The Problem with SPAs:** As we discussed with the Vite SPA, the server initially sends an almost empty HTML file (usually just `<div id="root"></div>`), and then React uses JavaScript to draw the actual buttons, text, and images. While Google's bots have gotten better at running JavaScript, they are still fundamentally text-readers. If a bot reads the initial empty HTML file before React has time to draw the page, it assumes your website has zero content and won't rank it on Google.

### 2. SSR (Server-Side Rendering)
**What it is:** SSR is a technique designed to solve the SEO problem for React applications. Frameworks like **Next.js** are built specifically for this.
**How it works:** Instead of sending an empty HTML file to the browser and making the user's browser do the heavy lifting of running React to draw the page (which is called **CSR - Client-Side Rendering**), the server does the work. 
When a user (or a Google bot) requests a page, the server runs the React code, generates the fully populated, final HTML, and sends *that* to the browser.
**The Result:** 
* The Google bot instantly sees a page full of text and links, giving you perfect SEO.
* The user instantly sees the fully rendered page on their screen without waiting for JavaScript to load and execute.

---

### How to use this in your interview
If the interviewer asks, *"Why did you use Vite (Client-Side Rendering) instead of Next.js (Server-Side Rendering)?"*

You can give this very strong, senior-level engineering answer:

> *"I evaluated both, but it comes down to the use-case. Next.js and Server-Side Rendering are essential for e-commerce sites or blogs where SEO is critical—you need Google's web crawlers to instantly read your HTML to rank your products. 
> 
> However, my processor simulator is an interactive tool, not a content website. Users don't need to find individual simulation results via Google search. Because SEO wasn't a requirement, I didn't need the heavy backend infrastructure and complexity that comes with Server-Side Rendering. Instead, I chose a Vite SPA (Client-Side Rendering), which is much simpler to deploy, requires less server compute, and offloads the UI rendering entirely to the user's browser, which is perfectly suited for a highly interactive web app."*

**Why Vite SPA?** The simulator UI is purely client-driven — the backend is the source of truth for simulation state, and all UI updates happen after API calls. No SSR or SEO requirements. Vite's dev proxy (`/api → localhost:8080`) mirrors production exactly (Vercel rewrites → Railway), so there's no environment-specific code in the frontend.

Not exactly! In SSR, there is still a load time, but it changes *where* the loading happens and how the user *perceives* the speed. 

If an interviewer asks you this, it's a great opportunity to show deep frontend knowledge by explaining the difference between **Seeing** the page and **Interacting** with the page.

Here is the exact breakdown of how load times differ between a Vite SPA (Client-Side) and Next.js (Server-Side):

### 1. Time to First Byte (The initial server response)
* **Vite SPA (Faster):** The server just grabs a static, empty `index.html` file and fires it back to the browser instantly. 
* **SSR (Slower):** When you request a page, the server has to wake up, run the React code, query the database, and build the full HTML file from scratch *before* sending anything back. So the server response time is actually slower in SSR.

### 2. First Contentful Paint (When the user sees the UI)
* **SSR (Faster):** As soon as the browser receives that fully-built HTML from the server, it instantly paints the text, images, and layout on the screen. The user *sees* the website almost immediately.
* **Vite SPA (Slower):** The browser receives the empty HTML instantly, but the screen stays totally blank (or shows a loading spinner) while it downloads a massive JavaScript bundle, parses it, and runs React to finally draw the UI.

### 3. Hydration (The catch with SSR!)
This is the "gotcha" of SSR. In SSR, the user sees the fully rendered page very quickly. **But they can't click anything yet.** 

Because the server only sent HTML, the buttons don't have any JavaScript attached to them. While the user is looking at the page, the browser is secretly downloading the React JavaScript bundle in the background and "attaching" it to the HTML elements to make them interactive. This process is called **Hydration**. 

If the user tries to click a "Buy" button before hydration finishes, nothing happens. The page looks ready, but it's temporarily frozen.

### Summary for an Interview
> *"SSR doesn't eliminate load time, it just shifts the work to the server. SSR gives you a much faster 'First Contentful Paint'—so the user sees the page instantly—but it suffers from 'Hydration' delay, where the page is visible but not yet interactive. A Vite SPA takes slightly longer to show the initial UI, but once it loads, it is instantly interactive and subsequent navigations are blazing fast."*

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


# SECTION 15 — Resume Bullet-Driven & Deployed Project Questions

Because interviewers often won't look at the raw codebase, they will base their questions on your resume bullets and a live demo of your deployed project. Be prepared to answer these high-level architectural and behavioral questions:

## Deployed Project Questions (Frontend / API)

**Q: Walk me through what happens when I click "Run Simulation" on your frontend.**
**A:** When you click "Run", the React frontend makes an Axios POST request to `/api/run` on the Railway-hosted Java backend. The backend `ApiServer` receives the request and flips an `AtomicBoolean` lock to prevent concurrent simulations. It invokes the `Compiler` to convert your assembly text into 32-bit machine code, loads it into the `Memory` object, and starts the `PipelineController`. The pipeline runs cycle-by-cycle until it hits a `HALT` instruction. The server then writes the logs to `console.txt` and `output.txt`, flips the lock back to false, and returns an HTTP 200 OK. The frontend then makes subsequent GET requests to fetch those log files and updates the React UI to display the stats dashboard and pipeline diagram.

**Q: Your server uses Java's built-in `HttpServer` with zero dependencies. Why not use Spring Boot?**
**A:** Spring Boot is fantastic, but it's very heavy. It requires Maven or Gradle and downloads hundreds of megabytes of dependencies. My goal for this project was to keep it as raw and close to the metal as possible—both in the simulated processor and in the backend code. Using the JDK's built-in `com.sun.net.httpserver` kept the project lightweight, making it incredibly easy to compile and deploy via a simple Dockerfile on Railway without any heavy build tools.

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
