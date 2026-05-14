# Study Report — `RegisterFile.java`

> **File:** `src/core/RegisterFile.java`
> **Module:** 32-register RISC-V integer register file
> **Date Generated:** 2026-04-09

---

## Table of Contents

1. [Overview](#overview)
2. [Package Declaration (Line 1)](#package-declaration)
3. [Class Declaration & Fields (Lines 3–5)](#class-declaration--fields)
4. [Constructor (Lines 7–9)](#constructor)
5. [Method — `read()` (Lines 11–13)](#read)
6. [Method — `write()` (Lines 15–18)](#write)
7. [Method — `dump()` (Lines 20–25)](#dump)
8. [Summary Table](#summary-table)

---

## Overview

`RegisterFile` models the RISC-V **integer register file** — 32 general-purpose 32-bit registers (`x0` through `x31`). It enforces the RISC-V architectural rule that **`x0` is hardwired to zero** (reads always return `0`, writes are silently ignored).

This class is shared across the pipeline: the **ID stage** reads from it, the **WB stage** writes to it, and the **EX stage** uses it as a fallback when forwarding doesn't apply.

---

## Package Declaration

```java
// Line 1
package core;
```

---

## Class Declaration & Fields

```java
// Lines 3–5
// 32 integer registers. x0 is hardwired to 0.
public class RegisterFile {
    private int[] regs = new int[32];
```

| Field | Type | Description |
|-------|------|-------------|
| `regs` | `int[32]` | Array storing all 32 register values. Java initialises every element to `0`. |

The array is `private` — all access goes through `read()` and `write()`, which enforce the `x0` invariant.

---

## Constructor

```java
// Lines 7–9
public RegisterFile() {
    regs[2] = 0x0FFF;  // Initialize SP at top of memory
}
```

**Purpose:** Initialises the **stack pointer** (`x2` / `sp` in RISC-V convention) to `0x0FFF` (4095 in decimal).

| Detail | Value |
|--------|-------|
| Register | `x2` (ABI name: `sp`) |
| Initial value | `0x0FFF` = 4095 |
| Rationale | Points to the top of the 4 KB memory (address 4095). The stack grows downward in RISC-V. |

All other registers start at `0` (Java default).

---

## `read()`

```java
// Lines 11–13
public int read(int r) {
    return (r == 0) ? 0 : regs[r];
}
```

**Purpose:** Reads the value of register `xR`.

| Input | Output | Explanation |
|-------|--------|-------------|
| `r == 0` | `0` | RISC-V mandates that `x0` always reads as zero, regardless of what was written. |
| `r != 0` | `regs[r]` | Returns the stored value for registers `x1`–`x31`. |

This is a **ternary expression** — a compact if-else:
```
if (r == 0) return 0;
else return regs[r];
```

---

## `write()`

```java
// Lines 15–18
public void write(int r, int val) {
    if (r != 0)
        regs[r] = val;
}
```

**Purpose:** Writes `val` to register `xR`.

| Input | Action | Explanation |
|-------|--------|-------------|
| `r == 0` | **No operation** | Writes to `x0` are silently discarded — the hardwired zero is never overwritten. |
| `r != 0` | `regs[r] = val` | Stores the value for registers `x1`–`x31`. |

Called exclusively by the **WB stage** (`WB_Stage.tick()`).

---

## `dump()`

```java
// Lines 20–25
public void dump() {
    System.out.println("Register dump (x0-x31):");
    for (int i = 0; i < 32; i++) {
        System.out.printf("  x%-2d = %d%n", i, (i == 0) ? 0 : regs[i]);
    }
}
```

**Purpose:** Prints all 32 register values to `stdout` for debugging. Called by the EX stage when an `ECALL` instruction is executed.

**Output format:**
```
Register dump (x0-x31):
  x0  = 0
  x1  = 42
  x2  = 4095
  ...
  x31 = 0
```

| Format specifier | Meaning |
|-----------------|---------|
| `x%-2d` | Register name, left-aligned in a 2-character field (`x0 `, `x1 `, …, `x31`). |
| `(i == 0) ? 0 : regs[i]` | Enforces the x0 = 0 convention even in the dump output. |

---

## Summary Table

| Method | Visibility | Return | Lines | Purpose |
|--------|-----------|--------|-------|---------|
| `RegisterFile()` | `public` | — | 7–9 | Constructor; sets SP (`x2`) to `0x0FFF` |
| `read()` | `public` | `int` | 11–13 | Read register (x0 always returns 0) |
| `write()` | `public` | `void` | 15–18 | Write register (x0 writes ignored) |
| `dump()` | `public` | `void` | 20–25 | Print all registers to stdout |

---

*End of Report*
