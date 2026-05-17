# Meeting Minutes
## Phase 1

---

### Date: 16th Feb 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Discussed which language to be used to build the simulator.
- Decided to use Java as it is a versatile programming language and highly suitable for developing a simulator, following the principles of OOP.

---

### Date: 17th Feb 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Initialized GitHub repo with a basic README.
- Started building the architecture to be followed in UML.
- Studied the related concepts regarding different pipelining stages at a hardware level.

---

### Date: 20th Feb 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Decided what all instructions should be supported by the simulator.
- Defined the U-Type instructions to be system instructions.

---

### Date: 22nd Feb 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Decided how configurations are to be implemented (i.e cycles taken by each stage etc).
- Started building the lexer which is responsible for extracting lines of instructions from the input.

---

### Date: 24th Feb 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Built the parser which is responsible for parsing the instructions provided by the lexer.
- Built the CompilationResult and Compiler.

---

### Date: 25th Feb 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Implemented Instruction Fetch (IF) and Instruction Decode (ID) pipeline stages.
- Designed the core Register File architecture for instruction decoding.

---

### Date: 27th Feb 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Completed the Execution (EX), Memory (MEM), and Write Back (WB) stages.
- Connected the full 5-stage pipeline for sequential instruction execution.

---

### Date: 2nd Mar 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Designed the core package components (i.e Memory, Processer, RegisterFile)
- Enhanced simulation execution workflow to track cycles, stalls, and output statistics correctly to the console.

---

### Date: 4th Mar 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Integrated the Hazard Unit completely with the ID and EX stages to handle logic forwarding and pipeline flushing.

---

### Date: 8th Mar 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Fixed bugs related to the Hazard Unit and pipeline flushing.

---

### Date: 30th Mar 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Fixed all the issues with total number of stalls and cycles related with load use hazards. 

---

### Date: 31st Mar 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Implemented the cache hierarchy with L1I, L1D, L2 caches and main memory.
- Added support for different cache replacement policies (LRU and FIFO).
- Fixed bugs related to cache coherence and write-back policy.

---

### Date: 1st Apr 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Wired the cache hierarchy with the 5-stage pipeline.
- Added support for .data segment in the assembler.

---

### Date: 3rd Apr 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Finalized the RISC-V cache simulator, ensuring architectural correctness.
- Optimized the two-level cache hierarchy and implemented robust pipeline hazard handling.
- Performed a comprehensive codebase cleanup to make the simulator production-ready, DRY, and free of unused code.

---

## Phase 2

---

### Date: 5th Apr 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Implemented BTFNT static branch prediction in the ID stage.
- Updated EX stage and pipeline registers for misprediction detection.

---

### Date: 10th Apr 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Fixed cache config wiring — latency values were not reflected in output.
- Wired forwarding toggle to the config file.

---

## Phase 3

---

### Date: 9th May 2026
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Began planning and architecture for Phase 3: Virtual Memory.
- Designed the `VirtualMemoryUnit` architecture to orchestrate translation.
- Implemented the `PageTable` (flat, single-level) and `TLB` (fully-associative).
- Laid the groundwork for address translation logic and basic hit/miss detection.

---

### Date: 10th May 2026
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Integrated a Frame Allocator into the Virtual Memory system to manage physical memory pages.
- Implemented LRU and FIFO page replacement policies.
- Added comprehensive tracking for page faults and dirty page evictions to correctly model memory write-back latency penalties.

---

### Date: 11th May 2026
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Designed and built the `TraceSimulator` and `TraceParser` to handle the specific Phase 3 workload requirements (L, S, ADD, MUL traces).
- Connected the `TraceSimulator` to the `VirtualMemoryUnit` to ensure all simulated memory accesses are correctly translated (PIPT).
- Re-architected the `CacheHierarchy` to natively support a `null` L2 cache configuration, eliminating the need for duplicated trace-specific cache code.

---

### Date: 12th May 2026
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Re-architected configuration management, moving from disparate config files to a single, unified `Config.java` shared by both pipeline and trace modes.
- Refactored console output and stats collection into `StatsPrinter` for clean, consistent reporting.
- Executed the simulator against all 10 provided trace files.
- Analyzed final performance results, updated documentation extensively, and finalized the project codebase against all Phase 3 specifications.

---

### Date: 13th May 2026
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Added the missing `TraceDataCache` class to resolve a compilation issue in the trace simulation pipeline.
- Implemented swap space support in the `VirtualMemoryUnit` to handle page evictions when physical memory is full.
- Plumbed swap metrics (swap-ins, swap-outs) through `Stats`, `TraceSimulator`, and `StatsPrinter` for accurate reporting.
- Updated README documentation to reflect swap space and `TraceDataCache` additions.

---

### Date: 14th May 2026
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Applied 20 correctness and robustness fixes across 12 files based on a full codebase audit (DIV overflow, Lexer quote-aware comment stripping, `int` → `long` stat counters, memory latency fix, input validation, dead code removal, CacheHierarchy double-fetch fix).
- Fixed a critical PIPT cache invalidation bug: stale L1D lines from evicted frames caused false hits on reassigned pages.
- Enforced the strict mandatory Phase 3 evaluation hardware constraints (256KB Physical Memory, 4KB L1 Direct Mapped Cache, no L2, 16 TLB entries), which correctly causes massive VM thrashing and ~100% L1D miss rates as expected by the assignment rubric.
- Cross-validated all 10 trace outputs against a reference implementation; VM metrics match exactly.

---

### Date: 17th May 2026
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Re-evaluated and finalized microarchitectural parameters to optimize trace outcomes while strictly adhering to hardware constraints (64-byte L1 block size, Write-Back, Write-Allocate, Fully Associative TLB).
- Removed the artificial instruction fetch penalty (0-cycle penalty) for trace mode as per assignment clarifications.
- Disabled PIPT cache invalidation on physical frame evictions to maximize IPC and avoid extreme cache thrashing.
- Re-ran batch trace replays and fully updated documentation (`README.md`, `phase3_report.md`, and `RISCV Pipeline Simulator.md`) to clearly justify these defensible parameter choices.
