# Study Report — `Config.java`

> **File:** `src/common/Config.java`
> **Module:** Configuration — Instruction latencies, cache parameters, VM settings
> **Date Updated:** 2026-05-12 *(Phase 3 — INI sections + VM parameters)*

---

## Overview

`Config` is the **centralized configuration class** for the simulator. It manages instruction latencies, forwarding settings, cache parameters, and virtual memory settings.

**Phase 3 update:** `Config` now supports **INI-style sectioned configuration files** in addition to the original flat `KEY=VALUE` format. New sections include `[pipeline]`, `[latencies]`, `[memory]`, `[vm]`, and `[cache]`.

---

## Configuration Formats

### INI-Style (Phase 3)

```ini
[pipeline]
forwarding_enabled = true

[latencies]
ADD = 1
MUL = 3

[memory]
virtual_size_bytes = 536870912
physical_size_bytes = 262144
page_size_bytes = 4096

[vm]
dtlb_entries = 16
tlb_hit_latency = 1
page_walk_latency = 10
page_fault_latency = 50
replacement_policy = lru

[cache]
L1D_SIZE = 4096
L1D_BLOCK_SIZE = 64
L1D_ASSOCIATIVITY = 1
L1D_LATENCY = 1
MEMORY_LATENCY = 200
```

### Flat Format (Phase 2 compatible)

```properties
L1I_SIZE = 1024
L1I_BLOCK_SIZE = 64
# ... etc
FORWARDING_ENABLED = true
```

Both formats are automatically detected by `loadCacheConfig()`.

---

## Virtual Memory Parameters (Phase 3)

| Parameter | Default | Description |
|-----------|---------|-------------|
| `virtual_size_bytes` | 65,536 | Total virtual address space in bytes |
| `physical_size_bytes` | 16,384 | Total physical memory in bytes |
| `page_size_bytes` | 4,096 | Page size in bytes |
| `dtlb_entries` | 4 | Number of fully-associative TLB entries |
| `tlb_hit_latency` | 1 | Cycles for TLB hit |
| `page_walk_latency` | 10 | Extra cycles for page table walk |
| `page_fault_latency` | 50 | Extra cycles for page fault |
| `replacement_policy` | "fifo" | Replacement policy: "lru" or "fifo" |

---

## Key Methods

| Method | Return | Purpose |
|--------|--------|---------|
| `loadCacheConfig(path)` | `void` | Load config from file (auto-detects INI vs flat) |
| `getLatency(opcode)` | `int` | Get instruction latency |
| `isForwardingEnabled()` | `boolean` | Forwarding toggle |
| `getL1I()`, `getL1D()`, `getL2()` | `CacheConfig` | Cache level configs |
| `getMainMemoryLatency()` | `int` | Memory latency |
| `getVirtualSizeBytes()` | `int` | Virtual memory size |
| `getPhysicalSizeBytes()` | `int` | Physical memory size |
| `getPageSizeBytes()` | `int` | Page size |
| `getDtlbEntries()` | `int` | TLB entry count |
| `getTlbHitLatency()` | `int` | TLB hit latency |
| `getPageWalkLatency()` | `int` | Page walk latency |
| `getPageFaultLatency()` | `int` | Page fault latency |
| `getVmReplacementPolicy()` | `String` | VM replacement policy |

---

*End of Report*
