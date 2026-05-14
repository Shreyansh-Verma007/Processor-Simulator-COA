# Study Report — `AccessResult.java`

> **File:** `src/cache/AccessResult.java`
> **Module:** Cache – value object returned by every cache/memory access
> **Date Generated:** 2026-04-19

---

## Table of Contents

1. [Overview](#overview)
2. [Package Declaration](#package-declaration)
3. [Class Declaration & Fields](#class-declaration--fields)
4. [Constructor](#constructor)
5. [Usage in the Pipeline](#usage-in-the-pipeline)

---

## Overview

`AccessResult` is an **immutable value object** returned by every read/write method on `CacheHierarchy`. It bundles two pieces of information into a single return value:

| Field | Meaning |
|-------|---------|
| `data` | The word read from the cache/memory hierarchy (0 for writes). |
| `latencyCycles` | Total number of cycles consumed by this access (hit or miss penalty). |

Because Java methods can only return one value, `AccessResult` avoids the need to pass latency back via mutable parameters or separate calls.

---

## Package Declaration

```java
package cache;
```

---

## Class Declaration & Fields

```java
public class AccessResult {
    public final int data;
    public final int latencyCycles;
    ...
}
```

Both fields are declared `final` — the object is immutable after construction.

| Field | Type | Description |
|-------|------|-------------|
| `data` | `int` | The 32-bit word read from the cache hierarchy. Zero for store operations (no meaningful data returned). |
| `latencyCycles` | `int` | Total access latency in cycles, representing the sum of all cache levels traversed (hit latency, plus any miss penalties at L1, L2, or main memory). |

---

## Constructor

```java
public AccessResult(int data, int latencyCycles) {
    this.data = data;
    this.latencyCycles = latencyCycles;
}
```

A simple all-fields constructor. Validation is left to the caller (typically `CacheHierarchy`).

---

## Usage in the Pipeline

`AccessResult` is used in two pipeline stages:

### IF_Stage (Instruction Fetch)
```java
AccessResult result = cache.fetchInstruction(pc);
Instruction instr = InstructionEncoder.decode(result.data);
out.fetchLatencyLeft = result.latencyCycles - 1;
```
The `-1` accounts for the fact that the current tick already counts as one cycle.

### MEM_Stage (Memory Access)
```java
AccessResult r = cache.readData(exMem.aluResult);
out.result = r.data;
out.memLatencyLeft = r.latencyCycles - 1;
```
Any remaining cycles are stored in `MEM_WB.memLatencyLeft` and converted to pipeline stalls by `PipelineController`.

---

*End of Report*
