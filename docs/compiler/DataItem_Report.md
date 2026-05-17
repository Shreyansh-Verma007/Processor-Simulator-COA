# Study Report — `DataItem.java`

> **File:** `src/compiler/DataItem.java`
> **Module:** Compiler — Data Segment Support
> **Date Generated:** 2026-05-14 *(Phase 2)*

---

## Overview

`DataItem` represents a **contiguous block of bytes** to be written into memory at a given address. Instances are produced by the compiler when parsing `.data` segment directives (`.word`, `.byte`, `.space`, `.ascii`, `.asciiz`).

---

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `address` | `int` | Memory address where the data block starts |
| `bytes` | `byte[]` | Raw byte content to write into memory |

---

*End of Report*
