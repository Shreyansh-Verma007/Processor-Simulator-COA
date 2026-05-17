# Study Report — `Instruction.java`

> **File:** `src/common/Instruction.java`
> **Module:** Common — Instruction Representation
> **Date Generated:** 2026-05-14 *(Phase 1)*

---

## Overview

`Instruction` is a Java **class** that represents a single decoded RISC-V instruction. It provides static factory methods for each instruction format type to ensure correct field mapping.

**Class Fields:**
- `opcode` — the `Opcode` enum value
- `rd` — destination register index
- `rs1` — source register 1 index
- `rs2` — source register 2 index
- `immediate` — immediate/offset value

---

## Factory Methods

| Method | Format | Parameters | Zeroed Fields |
|--------|--------|-----------|---------------|
| `rType(op, rd, rs1, rs2)` | R-Type | All registers | `immediate = 0` |
| `iType(op, rd, rs1, imm)` | I-Type | rd, rs1, immediate | `rs2 = 0` |
| `sType(op, rs1, rs2, imm)` | S-Type | rs1, rs2, immediate | `rd = 0` |
| `bType(op, rs1, rs2, imm)` | B-Type | rs1, rs2, offset | `rd = 0` |
| `jType(op, rd, imm)` | J-Type | rd, offset | `rs1 = 0, rs2 = 0` |
| `uType(op)` | U-Type | Opcode only | All fields zeroed |

---

*End of Report*
