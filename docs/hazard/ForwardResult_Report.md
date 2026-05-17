# Study Report — `ForwardResult.java`

> **File:** `src/hazard/ForwardResult.java`
> **Module:** Forwarding result enum for the RISC-V pipeline simulator's hazard subsystem
> **Date Generated:** 2026-04-09

---

## Table of Contents

1. [Overview](#overview)
2. [Package Declaration (Line 1)](#package-declaration)
3. [Enum Declaration & Values (Lines 5–9)](#enum-declaration--values)
4. [Usage in the Pipeline](#usage-in-the-pipeline)

---

## Overview

`ForwardResult` is a simple **enum** that acts as a return type for the `ForwardingUnit`. It tells the EX stage **where** to read an operand value from when a data hazard is detected.

In a 5-stage pipeline, an instruction in the EX stage may need a value that is still being computed by a preceding instruction. Rather than stalling, the forwarding (bypass) network can supply the value directly from a later pipeline register.

---

## Package Declaration

```java
// Line 1
package hazard;
```
Places the enum in the `hazard` package alongside `ForwardingUnit` and `HazardUnit`.

---

## Enum Declaration & Values

```java
// Lines 3–4 (comments)
// Indicates where a forwarded operand value should come from.
// Used by ForwardingUnit to tell EX_Stage which value to use.
```
Documentation comments explaining the purpose.

```java
// Line 5
public enum ForwardResult {
```
Declares a public enum — it has exactly three constants and no methods.

---

### Enum Constants (Lines 6–8)

```java
NONE,          // no forwarding needed — read from register file
FROM_EX_MEM,   // forward from EX/MEM register (1 cycle old)
FROM_MEM_WB    // forward from MEM/WB register (2 cycles old)
```

| Constant | Value Source | Latency | When Used |
|----------|-------------|---------|-----------|
| `NONE` | Register file | N/A | No data hazard exists — the register file has the correct, up-to-date value. |
| `FROM_EX_MEM` | `EX_MEM.aluResult` | 1 cycle ago | The producing instruction is one stage ahead (in MEM). Its ALU result is available in the EX/MEM pipeline register. |
| `FROM_MEM_WB` | `MEM_WB.result` | 2 cycles ago | The producing instruction is two stages ahead (in WB). Its result (ALU or memory load) is available in the MEM/WB pipeline register. |

---

## Usage in the Pipeline

```
ForwardingUnit.getForwardA() / getForwardB()
        │
        ▼
   ForwardResult
        │
        ├── NONE ──────────► EX_Stage reads from RegisterFile
        ├── FROM_EX_MEM ───► EX_Stage reads from EX_MEM.aluResult
        └── FROM_MEM_WB ──► EX_Stage reads from MEM_WB.result
```

The `EX_Stage.resolveOperandA()` and `resolveOperandB()` methods use this enum in an if-chain to select the correct data source.

---

### Why Not Forward Loads from EX/MEM?

Notice that the `ForwardingUnit` **excludes** `LW`/`LB` from `FROM_EX_MEM` forwarding. This is because load instructions don't have their data until the **MEM stage completes** — the EX/MEM register only contains the computed address, not the loaded value. This is the classic **load-use hazard**, which requires a 1-cycle stall even with forwarding.

---

*End of Report*
