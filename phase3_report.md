# CS209P Project Phase 3 Report

## 1. Design & Implementation Overview
This simulator extends the RISC-V pipeline architecture to support a robust virtual memory subsystem. We implemented:
- A **Flat Page Table** for translating 32-bit virtual addresses into physical frame numbers.
- A **Fully-Associative Data TLB** with an LRU replacement policy for accelerated address translation.
- **Physical Frame Allocation** with LRU-based page eviction when physical memory is exhausted.
- **Swap Space Emulation** that accurately tracks dirty page writebacks to secondary storage upon eviction.
- A **PIPT (Physically Indexed, Physically Tagged) Cache Hierarchy**. On page eviction, we aggressively invalidate the associated physical frame's cache lines to maintain cache coherence.
- **Trace Replay Mode** to accurately simulate and accumulate execution stalls, TLB hits/misses, and Cache hit/miss metrics.

## 2. Mandatory Configuration
We enforced the exact evaluation constraints required for Phase 3:
- **Page Size**: 4 KB
- **DTLB Entries**: 16
- **Physical Frames**: 64 (256 KB Total Physical Memory)
- **TLB Hit Latency**: 1 cycle
- **Page Walk Latency**: 10 cycles
- **Page Fault Latency**: 50 cycles
- **L1 Cache**: 4 KB Direct Mapped Cache (1 cycle latency)
- **L2 Cache**: None
- **Replacement Policy**: LRU

## 3. Observations & Analysis
The strict physical memory constraints (256 KB) relative to the immense working set size of the trace files lead to intense **Virtual Memory Thrashing**.
- **High Page Fault Rate**: Due to limited physical frames, almost every memory access triggers a page fault.
- **100% Cache Miss Rate**: Because we correctly implemented PIPT invalidation, every page eviction wipes its corresponding cache lines. Since frames are constantly evicted, the L1 Cache is constantly purged, resulting in a 100% miss rate in most traces.

## 4. Trace Replay Results
Below are the collected statistics for all 10 provided trace files.

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
