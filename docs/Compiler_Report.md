# Study Report — `Compiler.java`

> **File:** `src/compiler/Compiler.java`
> **Module:** Top-level assembler orchestrator for the RISC-V simulator
> **Date Generated:** 2026-04-09

---

## Table of Contents

1. [Overview](#overview)
2. [Imports & Package Declaration (Lines 1–8)](#imports--package-declaration)
3. [Class Declaration (Line 10)](#class-declaration)
4. [Method — `compile()` (Lines 11–18)](#compile)
5. [Method — `buildSymbolTable()` (Lines 20–43)](#buildsymboltable)
6. [Summary Table](#summary-table)
7. [Assembly Pipeline Diagram](#assembly-pipeline-diagram)

---

## Overview

`Compiler` orchestrates the **two-pass assembly** of a RISC-V assembly source file:

1. **Pass 1 (Lexer + Symbol table):** Tokenise the source file and build a symbol table mapping labels to byte addresses.
2. **Pass 2 (Parser):** Parse each token into an `Instruction` object, resolving label references using the symbol table.

Despite its name, this is an **assembler**, not a compiler — it translates assembly mnemonics directly into simulator instruction objects.

---

## Imports & Package Declaration

```java
// Line 1
package compiler;
```

```java
// Lines 3–8
import common.Instruction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
```

| Import | Purpose |
|--------|---------|
| `Instruction` | Decoded RISC-V instruction object. |
| `IOException` | Thrown if the source file cannot be read. |
| `ArrayList` | Token list and instruction list. |
| `HashMap` | Concrete implementation of the symbol table. |
| `Map` | Interface type for the symbol table. |

---

## Class Declaration

```java
// Line 10
public class Compiler {
```
A stateless class with two methods — the public `compile` entry point and the private `buildSymbolTable` helper.

---

## `compile()`

```java
// Lines 11–18
public CompilationResult compile(String path) throws IOException {
    Lexer lexer = new Lexer();
    ArrayList<String> tokens = lexer.tokenize(path);
    Map<String, Integer> symbols = buildSymbolTable(tokens);
    Parser parser = new Parser(symbols);
    ArrayList<Instruction> program = parser.parse(tokens);
    return new CompilationResult(program);
}
```

**Purpose:** The main entry point — takes a file path and returns a `CompilationResult` containing the assembled program.

### Line-by-Line

| Line | Code | Step |
|------|------|------|
| 12 | `new Lexer()` | Create the lexer (tokeniser). |
| 13 | `lexer.tokenize(path)` | **Pass 1a:** Read the source file and split into cleaned tokens (lines stripped of comments and whitespace). |
| 14 | `buildSymbolTable(tokens)` | **Pass 1b:** Scan tokens to find labels and record their byte addresses. |
| 15 | `new Parser(symbols)` | Create the parser with the symbol table for label resolution. |
| 16 | `parser.parse(tokens)` | **Pass 2:** Parse each token into an `Instruction` object, resolving labels to numeric offsets. |
| 17 | `new CompilationResult(program)` | Wrap the instruction list in the result container. |

---

## `buildSymbolTable()`

```java
// Lines 20–43
private Map<String, Integer> buildSymbolTable(ArrayList<String> tokens)
```

**Purpose:** Scans all tokens to find labels and maps each label name to its **byte address** (instruction index × 4).

### Line-by-Line

```java
// Lines 21–22
Map<String, Integer> symbols = new HashMap<>();
int instrIndex = 0;
```
- `symbols` — the label-to-address map.
- `instrIndex` — tracks the current instruction number (not counting labels).

---

```java
// Lines 24–27
for (String token : tokens) {
    String t = token.trim();
    if (t.isEmpty())
        continue;
```
Iterates through each token. Skips empty strings.

---

```java
// Lines 29–31
if (t.endsWith(":")) {
    String label = t.substring(0, t.length() - 1).trim();
    symbols.put(label, instrIndex * 4);
}
```
**Case 1: Standalone label** (e.g., `loop:`).

- Extracts the label name by removing the trailing `:`.
- Maps it to `instrIndex * 4` (byte address).
- **Does NOT increment `instrIndex`** — a standalone label does not represent an instruction.

---

```java
// Lines 32–36
} else if (t.contains(":")) {
    int idx = t.indexOf(':');
    String label = t.substring(0, idx).trim();
    symbols.put(label, instrIndex * 4);
    instrIndex++;
}
```
**Case 2: Label + instruction on same line** (e.g., `start: ADD x1, x2, x3`).

- Extracts the label portion before the `:`.
- Maps it to the current byte address.
- **Increments `instrIndex`** — this line also contains an instruction.

---

```java
// Lines 37–39
} else {
    instrIndex++;
}
```
**Case 3: Plain instruction** (no label).

- Just increments the instruction counter.

---

```java
// Line 42
return symbols;
```
Returns the completed symbol table, which the `Parser` uses for label resolution.

---

### Symbol Table Example

```asm
      LI x1, 5        # instrIndex = 0, address = 0x0000
loop: ADD x2, x2, x1  # instrIndex = 1, address = 0x0004  ← "loop" → 4
      ADDI x1, x1, -1 # instrIndex = 2, address = 0x0008
      BNE x1, x0, loop # instrIndex = 3, address = 0x000C
done:                  # (standalone label)                ← "done" → 16
      HALT             # instrIndex = 4, address = 0x0010
```

Symbol table: `{ "loop" → 4, "done" → 16 }`

---

## Summary Table

| Method | Visibility | Return | Lines | Purpose |
|--------|-----------|--------|-------|---------|
| `compile()` | `public` | `CompilationResult` | 11–18 | Orchestrate full assembly pipeline |
| `buildSymbolTable()` | `private` | `Map<String, Integer>` | 20–43 | First pass: map labels to byte addresses |

---

## Assembly Pipeline Diagram

```
  Source File (.asm)
       │
       ▼
  ┌──────────┐
  │  Lexer   │  tokenize() — read file, strip comments
  └────┬─────┘
       │  ArrayList<String> tokens
       ▼
  ┌──────────────────┐
  │ buildSymbolTable │  First pass — find labels, compute addresses
  └────┬─────────────┘
       │  Map<String, Integer> symbols
       ▼
  ┌──────────┐
  │  Parser  │  parse() — convert tokens to Instructions
  └────┬─────┘
       │  ArrayList<Instruction> program
       ▼
  ┌────────────────────┐
  │ CompilationResult  │  Wrapper for the instruction list
  └────────────────────┘
```

---

*End of Report*
