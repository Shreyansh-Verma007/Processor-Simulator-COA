# Study Report — `Parser.java`

> **File:** `src/compiler/Parser.java`
> **Module:** Assembly instruction parser for the RISC-V simulator
> **Date Generated:** 2026-04-09

---

## Table of Contents

1. [Overview](#overview)
2. [Imports & Package Declaration (Lines 1–7)](#imports--package-declaration)
3. [Class Declaration & Fields (Lines 12–14)](#class-declaration--fields)
4. [Constructors (Lines 16–24)](#constructors)
5. [Method — `parse()` (Lines 27–55)](#parse)
6. [Method — `parseInstruction()` (Lines 58–150)](#parseinstruction)
   - [R-Type Instructions (Lines 67–84)](#r-type-instructions)
   - [I-Type Instructions (Lines 86–109)](#i-type-instructions)
   - [S-Type Instructions (Lines 111–127)](#s-type-instructions)
   - [B-Type Instructions (Lines 129–137)](#b-type-instructions)
   - [J-Type & U-Type Instructions (Lines 139–147)](#j-type--u-type-instructions)
7. [Helper — `reg()` (Lines 153–162)](#reg)
8. [Helper — `imm()` (Lines 166–175)](#imm)
9. [Helper — `immAbsolute()` (Lines 179–188)](#immabsolute)
10. [Helper — `memOff()` (Lines 191–195)](#memoff)
11. [Helper — `memReg()` (Lines 198–202)](#memreg)
12. [Helper — `isLabel()` (Lines 205–210)](#islabel)
13. [Summary Table](#summary-table)
14. [Instruction Type Mapping](#instruction-type-mapping)

---

## Overview

`Parser` is the **second pass** of the assembler. Given a list of cleaned tokens (from the `Lexer`) and a symbol table (from `Compiler.buildSymbolTable()`), it converts each assembly line into an `Instruction` object that the pipeline simulator can execute.

It supports all six RISC-V instruction formats:

| Format | Example | Factory Method |
|--------|---------|----------------|
| R-Type | `ADD x1, x2, x3` | `Instruction.rType()` |
| I-Type | `ADDI x1, x2, 5` | `Instruction.iType()` |
| S-Type | `SW x1, 0(x2)` | `Instruction.sType()` |
| B-Type | `BEQ x1, x2, label` | `Instruction.bType()` |
| J-Type | `JAL x1, label` | `Instruction.jType()` |
| U-Type | `ECALL` / `HALT` | `Instruction.uType()` |

---

## Imports & Package Declaration

```java
// Line 1
package compiler;
```

```java
// Lines 3–7
import common.Instruction;
import common.Opcode;
import java.util.ArrayList;
import java.util.Map;
```

| Import | Purpose |
|--------|---------|
| `Instruction` | Factory class for creating RISC-V instruction objects. |
| `Opcode` | Enum of all supported instruction opcodes. |
| `ArrayList` | Token and instruction lists. |
| `Map` | Symbol table (`label → byte address`). |

---

## Class Declaration & Fields

```java
// Lines 12–14
public class Parser {
    private Map<String, Integer> symbols;
    private int instrIndex;
```

| Field | Type | Description |
|-------|------|-------------|
| `symbols` | `Map<String, Integer>` | Label-to-address mapping from the first pass. Used to resolve label operands to numeric values. |
| `instrIndex` | `int` | Tracks the current instruction number during parsing. Multiplied by 4 to get the byte address (used for PC-relative offset calculations). |

---

## Constructors

```java
// Lines 16–19 — Primary constructor
public Parser(Map<String, Integer> symbols) {
    this.symbols = symbols;
    this.instrIndex = 0;
}
```
Takes a pre-built symbol table. Used by `Compiler.compile()`.

```java
// Lines 21–24 — No-arg constructor
public Parser() {
    this.symbols = new java.util.HashMap<>();
    this.instrIndex = 0;
}
```
Creates an empty symbol table. Useful for testing or simple programs without labels.

---

## `parse()`

```java
// Lines 27–55
ArrayList<Instruction> parse(ArrayList<String> tokens)
```

**Purpose:** Iterates through all tokens and converts each to an `Instruction`.

### Line-by-Line

```java
// Lines 28–29
ArrayList<Instruction> program = new ArrayList<>();
instrIndex = 0;
```
Initialise the output list and reset the instruction counter.

```java
// Lines 31–34
for (int i = 0; i < tokens.size(); i++) {
    String token = tokens.get(i).trim();
    if (token.isEmpty())
        continue;
```
Loop through tokens, skipping empty strings.

```java
// Lines 36–38
// Skip standalone labels like "loop:"
if (token.endsWith(":"))
    continue;
```
Standalone labels (e.g., `loop:`) are already in the symbol table — they don't produce instructions. Skip them.

```java
// Lines 40–46
// Handle "label: instruction" on the same line
String line = token;
if (token.contains(":")) {
    line = token.substring(token.indexOf(':') + 1).trim();
    if (line.isEmpty())
        continue;
}
```
If a token has a label prefix (e.g., `start: ADD x1, x2, x3`), strip the label portion and keep only the instruction part.

```java
// Lines 48–52
Instruction instr = parseInstruction(line, instrIndex);
if (instr != null) {
    program.add(instr);
    instrIndex++;
}
```
Parse the assembly line into an `Instruction`. If successful, add it to the program and advance the index.

```java
// Line 54
return program;
```
Return the complete instruction list.

---

## `parseInstruction()`

```java
// Lines 58–150
private Instruction parseInstruction(String line, int idx)
```

**Purpose:** Parses a single assembly line into an `Instruction` object.

### Preprocessing (Lines 59–65)

```java
line = line.replaceAll(",", " ");     // remove commas
String[] p = line.trim().split("\\s+"); // split by whitespace
if (p.length == 0 || p[0].isEmpty())
    return null;

String op = p[0].toUpperCase();
int pc = idx * 4;                     // byte address of this instruction
```

| Step | Explanation |
|------|-------------|
| Remove commas | Treats `ADD x1, x2, x3` and `ADD x1 x2 x3` identically. |
| Split by whitespace | `p[0]` = opcode, `p[1]`–`p[n]` = operands. |
| `toUpperCase()` | Case-insensitive opcode matching. |
| `idx * 4` | Computes byte address for PC-relative label resolution. |

---

### R-Type Instructions (Lines 67–84)

```java
if (op.equals("ADD"))
    return Instruction.rType(Opcode.ADD, reg(p[1]), reg(p[2]), reg(p[3]));
```

**Format:** `OP rd, rs1, rs2` → `Instruction.rType(opcode, rd, rs1, rs2)`

| Opcode | Line | Operands |
|--------|------|----------|
| `ADD` | 67–68 | `rd, rs1, rs2` |
| `SUB` | 69–70 | `rd, rs1, rs2` |
| `MUL` | 71–72 | `rd, rs1, rs2` |
| `DIV` | 73–74 | `rd, rs1, rs2` |
| `SLL` | 75–76 | `rd, rs1, rs2` |
| `SRL` | 77–78 | `rd, rs1, rs2` |
| `XOR` | 79–80 | `rd, rs1, rs2` |
| `OR` | 81–82 | `rd, rs1, rs2` |
| `AND` | 83–84 | `rd, rs1, rs2` |

All follow the same pattern: 3 register operands.

---

### I-Type Instructions (Lines 86–109)

**ADDI (Line 86–87):**
```java
if (op.equals("ADDI"))
    return Instruction.iType(Opcode.ADDI, reg(p[1]), reg(p[2]), imm(p[3], pc));
```
Format: `ADDI rd, rs1, immediate`

**LI (Lines 89–91):**
```java
if (op.equals("LI"))
    return Instruction.iType(Opcode.LI, reg(p[1]), 0, immAbsolute(p[2]));
```
Pseudo-instruction: `LI rd, immediate` — only 2 operands. `rs1` is hardcoded to `0`. Uses `immAbsolute()` because the immediate is an absolute value, not PC-relative.

**LW / LB (Lines 93–109):**
```java
if (op.equals("LW")) {
    int rd = reg(p[1]);
    if (p[2].contains("(")) {
        return Instruction.iType(Opcode.LW, rd, memReg(p[2]), memOff(p[2]));
    } else {
        return Instruction.iType(Opcode.LW, rd, reg(p[2]), imm(p[3], pc));
    }
}
```
Supports two syntax forms:
| Syntax | Example | Handling |
|--------|---------|----------|
| `offset(base)` | `LW x1, 4(x2)` | Uses `memOff()` and `memReg()` to extract offset and base register |
| `rs1, imm` | `LW x1, x2, 8` | Standard 3-operand form |

`LB` follows the same pattern (Lines 102–109).

---

### S-Type Instructions (Lines 111–127)

```java
if (op.equals("SW")) {
    int rs2 = reg(p[1]);
    if (p[2].contains("(")) {
        return Instruction.sType(Opcode.SW, memReg(p[2]), rs2, memOff(p[2]));
    } else {
        return Instruction.sType(Opcode.SW, reg(p[2]), rs2, imm(p[3], pc));
    }
}
```
Format: `SW rs2, offset(rs1)` → `Instruction.sType(opcode, rs1, rs2, offset)`

Same dual-syntax support as loads. `SB` follows identically (Lines 120–127).

---

### B-Type Instructions (Lines 129–137)

```java
if (op.equals("BEQ"))
    return Instruction.bType(Opcode.BEQ, reg(p[1]), reg(p[2]), imm(p[3], pc));
```
Format: `BEQ rs1, rs2, label` → `Instruction.bType(opcode, rs1, rs2, offset)`

The `imm(p[3], pc)` call resolves the label to a **PC-relative offset** (`label_address − current_pc`).

| Opcode | Line |
|--------|------|
| `BEQ` | 130–131 |
| `BNE` | 132–133 |
| `BLT` | 134–135 |
| `BGE` | 136–137 |

---

### J-Type & U-Type Instructions (Lines 139–147)

**JAL (Lines 139–141):**
```java
if (op.equals("JAL"))
    return Instruction.jType(Opcode.JAL, reg(p[1]), imm(p[2], pc));
```
Format: `JAL rd, label` — PC-relative jump with link.

**ECALL / HALT (Lines 143–147):**
```java
if (op.equals("ECALL"))
    return Instruction.uType(Opcode.ECALL, 0);
if (op.equals("HALT"))
    return Instruction.uType(Opcode.HALT, 0);
```
System instructions with no operands — immediate is `0`.

**Unknown opcode (Line 149):**
```java
throw new RuntimeException("Unknown instruction: " + op);
```
Throws an error at assembly time if the opcode is not recognised.

---

## `reg()`

```java
// Lines 153–162
private int reg(String s) {
    s = s.trim();
    if (s.startsWith("x") || s.startsWith("X")) {
        int r = Integer.parseInt(s.substring(1));
        if (r < 0 || r > 31)
            throw new RuntimeException("Register out of range (must be x0–x31): " + s);
        return r;
    }
    throw new RuntimeException("Bad register: " + s);
}
```
Parses a register name like `x5` or `X12` into its integer index (0–31). Validates the range and throws on invalid input.

---

## `imm()`

```java
// Lines 166–175
private int imm(String s, int pc) {
    s = s.trim();
    if (isLabel(s)) {
        Integer addr = symbols.get(s);
        if (addr == null)
            throw new RuntimeException("Undefined label: " + s);
        return addr - pc;  // relative offset
    }
    return Integer.parseInt(s);
}
```
Parses an immediate operand. If it's a label name, looks up the address in the symbol table and returns the **PC-relative offset** (`addr − pc`). Otherwise, parses it as a plain integer.

---

## `immAbsolute()`

```java
// Lines 179–188
private int immAbsolute(String s) {
    s = s.trim();
    if (isLabel(s)) {
        Integer addr = symbols.get(s);
        if (addr == null)
            throw new RuntimeException("Undefined label: " + s);
        return addr;  // absolute address
    }
    return Integer.parseInt(s);
}
```
Same as `imm()` but returns the **absolute address** (no PC subtraction). Used by `LI` where you want the raw address, not an offset.

---

## `memOff()`

```java
// Lines 191–195
private int memOff(String s) {
    int lp = s.indexOf('(');
    String off = s.substring(0, lp).trim();
    return off.isEmpty() ? 0 : Integer.parseInt(off);
}
```
Extracts the **offset** from the `offset(register)` memory syntax.

| Input | Output |
|-------|--------|
| `"4(x1)"` | `4` |
| `"(x2)"` | `0` (empty offset defaults to 0) |
| `"-8(x3)"` | `-8` |

---

## `memReg()`

```java
// Lines 198–202
private int memReg(String s) {
    int lp = s.indexOf('(');
    int rp = s.indexOf(')');
    return reg(s.substring(lp + 1, rp).trim());
}
```
Extracts the **base register** from the `offset(register)` format.

| Input | Extracted | Result |
|-------|-----------|--------|
| `"4(x1)"` | `"x1"` | `1` |
| `"(x2)"` | `"x2"` | `2` |

---

## `isLabel()`

```java
// Lines 205–210
private boolean isLabel(String s) {
    if (s == null || s.isEmpty())
        return false;
    char c = s.charAt(0);
    return Character.isLetter(c) || c == '_';
}
```
Determines if a string is a **label name** (starts with a letter or underscore) versus a **numeric literal** (starts with a digit or minus sign).

| Input | Result | Reason |
|-------|--------|--------|
| `"loop"` | `true` | Starts with letter |
| `"_end"` | `true` | Starts with underscore |
| `"42"` | `false` | Starts with digit |
| `"-8"` | `false` | Starts with minus |

---

## Summary Table

| Method | Visibility | Return | Lines | Purpose |
|--------|-----------|--------|-------|---------|
| `Parser(Map)` | `public` | — | 16–19 | Constructor with symbol table |
| `Parser()` | `public` | — | 21–24 | No-arg constructor |
| `parse()` | package | `ArrayList<Instruction>` | 27–55 | Parse all tokens into instructions |
| `parseInstruction()` | `private` | `Instruction` | 58–150 | Parse single assembly line |
| `reg()` | `private` | `int` | 153–162 | Parse register name → index |
| `imm()` | `private` | `int` | 166–175 | Parse immediate (PC-relative for labels) |
| `immAbsolute()` | `private` | `int` | 179–188 | Parse immediate (absolute for labels) |
| `memOff()` | `private` | `int` | 191–195 | Extract offset from `off(reg)` syntax |
| `memReg()` | `private` | `int` | 198–202 | Extract register from `off(reg)` syntax |
| `isLabel()` | `private` | `boolean` | 205–210 | Check if string is a label name |

---

## Instruction Type Mapping

| Assembly Syntax | Type | Factory | Operand Fields |
|-----------------|------|---------|----------------|
| `ADD rd, rs1, rs2` | R | `rType` | `rd, rs1, rs2` |
| `ADDI rd, rs1, imm` | I | `iType` | `rd, rs1, imm` |
| `LI rd, imm` | I | `iType` | `rd, 0, imm` |
| `LW rd, off(rs1)` | I | `iType` | `rd, rs1, off` |
| `SW rs2, off(rs1)` | S | `sType` | `rs1, rs2, off` |
| `BEQ rs1, rs2, label` | B | `bType` | `rs1, rs2, offset` |
| `JAL rd, label` | J | `jType` | `rd, offset` |
| `ECALL` / `HALT` | U | `uType` | `0, 0` |

---

*End of Report*
