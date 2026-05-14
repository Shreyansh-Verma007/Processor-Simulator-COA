# Study Report — `Lexer.java`

> **File:** `src/compiler/Lexer.java`
> **Module:** Tokeniser (lexical analyser) for the RISC-V assembler
> **Date Generated:** 2026-04-09

---

## Table of Contents

1. [Overview](#overview)
2. [Imports & Package Declaration (Lines 1–6)](#imports--package-declaration)
3. [Class Declaration (Line 8)](#class-declaration)
4. [Method — `tokenize()` (Lines 9–29)](#tokenize)
5. [Summary Table](#summary-table)
6. [Processing Example](#processing-example)

---

## Overview

`Lexer` performs **lexical analysis** (tokenisation) on a RISC-V assembly source file. It reads the file line by line, strips comments and whitespace, and produces a list of clean, non-empty tokens — one per logical assembly line.

This is the first stage of the assembly pipeline: `Lexer → Symbol Table → Parser`.

---

## Imports & Package Declaration

```java
// Line 1
package compiler;
```

```java
// Lines 3–6
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
```

| Import | Purpose |
|--------|---------|
| `BufferedReader` | Efficient line-by-line file reading. |
| `FileReader` | Opens the source file by path. |
| `IOException` | Propagated if the file cannot be read. |
| `ArrayList` | Stores the output token list. |

---

## Class Declaration

```java
// Line 8
public class Lexer {
```
A stateless class with a single package-private method.

---

## `tokenize()`

```java
// Lines 9–29
ArrayList<String> tokenize(String filePath) throws IOException
```

**Visibility:** Package-private (no `public` modifier) — only accessible within the `compiler` package (called by `Compiler`).

**Purpose:** Reads the assembly file, strips comments and blanks, and returns a list of cleaned lines.

---

### Line-by-Line Walkthrough

```java
// Line 10
ArrayList<String> tokens = new ArrayList<>();
```
Initialises the output list.

---

```java
// Line 11
try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
```
Opens the file using **try-with-resources** — the `BufferedReader` is automatically closed when the block exits, even on exceptions.

---

```java
// Lines 12–13
String line;
while ((line = reader.readLine()) != null) {
```
Reads lines one at a time until end-of-file (`readLine()` returns `null`).

---

```java
// Line 14
line = line.trim();
```
Strips leading and trailing whitespace from the line.

---

```java
// Lines 15–16
if (line.isEmpty() || line.startsWith("#") || line.startsWith("//"))
    continue;
```
**Skip entire-line comments and blank lines:**

| Condition | Skips |
|-----------|-------|
| `line.isEmpty()` | Blank lines |
| `line.startsWith("#")` | Hash-style comments (common in assembly) |
| `line.startsWith("//")` | C-style line comments |

---

```java
// Lines 17–19
if (line.contains("#")) {
    line = line.substring(0, line.indexOf("#")).trim();
}
```
**Strip inline hash comments.** If a `#` appears mid-line (e.g., `ADD x1, x2, x3 # addition`), everything from `#` onwards is removed.

---

```java
// Lines 20–22
if (line.contains("//")) {
    line = line.substring(0, line.indexOf("//")).trim();
}
```
**Strip inline C-style comments.** Same logic for `//` comments.

---

```java
// Lines 23–24
if (line.isEmpty())
    continue;
```
**Second empty check.** After stripping inline comments, the remaining line might be empty (e.g., a line that was just `# comment`). Skip if so.

---

```java
// Line 25
tokens.add(line);
```
The cleaned, non-empty line is added to the token list.

---

```java
// Lines 27–28
}
return tokens;
```
Returns the complete list of clean tokens.

---

## Summary Table

| Line(s) | Purpose |
|---------|---------|
| 10 | Create output list |
| 11 | Open file with try-with-resources |
| 14 | Trim whitespace |
| 15–16 | Skip blank lines and full-line comments |
| 17–19 | Strip inline `#` comments |
| 20–22 | Strip inline `//` comments |
| 23–24 | Skip lines that became empty after comment stripping |
| 25 | Add cleaned line to token list |

---

## Processing Example

**Input file (`input.asm`):**
```asm
# Program: simple addition
LI x1, 5        # load 5 into x1
LI x2, 10       // load 10 into x2

# Compute sum
ADD x3, x1, x2
HALT
```

**Output tokens:**
```
["LI x1, 5", "LI x2, 10", "ADD x3, x1, x2", "HALT"]
```

Blank lines and all comments (full-line and inline) are stripped. Each token is one clean assembly line ready for the symbol table builder and parser.

---

*End of Report*
