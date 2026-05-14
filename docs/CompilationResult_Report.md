# Study Report — `CompilationResult.java`

> **File:** `src/compiler/CompilationResult.java`
> **Module:** Compilation output container for the RISC-V assembler
> **Date Generated:** 2026-04-09

---

## Table of Contents

1. [Overview](#overview)
2. [Imports & Package Declaration (Lines 1–5)](#imports--package-declaration)
3. [Class Declaration & Field (Lines 7–8)](#class-declaration--field)
4. [Constructor (Lines 10–12)](#constructor)
5. [Accessor — `getInstructions()` (Lines 14–16)](#getinstructions)
6. [Summary Table](#summary-table)

---

## Overview

`CompilationResult` is a simple **data-transfer object (DTO)** that wraps the output of the assembler. It holds the list of parsed `Instruction` objects produced by the `Compiler` and provides a single getter for downstream consumers (`Main.java`, `Processor`).

This class follows the **immutable wrapper pattern** — the list is set once via the constructor and exposed through a read-only accessor.

---

## Imports & Package Declaration

```java
// Line 1
package compiler;
```

```java
// Lines 3–5
import common.Instruction;
import java.util.ArrayList;
```

| Import | Purpose |
|--------|---------|
| `Instruction` | The decoded RISC-V instruction object. |
| `ArrayList` | Concrete list type used to store the instruction sequence. |

---

## Class Declaration & Field

```java
// Lines 7–8
public class CompilationResult {
    private final ArrayList<Instruction> instructions;
```

| Field | Type | Modifier | Description |
|-------|------|----------|-------------|
| `instructions` | `ArrayList<Instruction>` | `private final` | The assembled program as an ordered list of instructions. `final` ensures the reference cannot be reassigned after construction. |

---

## Constructor

```java
// Lines 10–12
public CompilationResult(ArrayList<Instruction> instructions) {
    this.instructions = instructions;
}
```
Stores the instruction list produced by the `Parser`. Called by `Compiler.compile()` as the final step of the assembly pipeline.

---

## `getInstructions()`

```java
// Lines 14–16
public ArrayList<Instruction> getInstructions() {
    return instructions;
}
```
Returns the instruction list. Used by `Main.java` to pass the program to `Processor.run()`.

---

## Summary Table

| Method | Visibility | Return | Lines | Purpose |
|--------|-----------|--------|-------|---------|
| `CompilationResult(ArrayList)` | `public` | — | 10–12 | Constructor; stores instruction list |
| `getInstructions()` | `public` | `ArrayList<Instruction>` | 14–16 | Returns the assembled program |

---

*End of Report*
