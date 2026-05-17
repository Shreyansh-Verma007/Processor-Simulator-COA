# Study Report — `Opcode.java`

> **File:** `src/common/Opcode.java`
> **Module:** Common — ISA Definition
> **Date Generated:** 2026-05-14 *(Phase 1)*

---

## Overview

`Opcode` is a Java **enum** that defines all supported opcodes for the simulator's custom RISC-V-like ISA. Instructions are grouped by their encoding format type.

---

## Supported Instructions

| Format | Opcode | Operation |
|--------|--------|-----------|
| **R-Type** | `ADD` | `rd = rs1 + rs2` |
| | `SUB` | `rd = rs1 - rs2` |
| | `MUL` | `rd = rs1 * rs2` |
| | `DIV` | `rd = rs1 / rs2` |
| | `SLL` | `rd = rs1 << rs2` (shift left logical) |
| | `SRL` | `rd = rs1 >>> rs2` (shift right logical) |
| | `XOR` | `rd = rs1 ^ rs2` |
| | `OR` | `rd = rs1 \| rs2` |
| | `AND` | `rd = rs1 & rs2` |
| **I-Type** | `ADDI` | `rd = rs1 + immediate` |
| | `LW` | `rd = Memory[rs1 + imm]` (load word) |
| | `LB` | `rd = Memory[rs1 + imm]` (load byte) |
| | `LI` | `rd = immediate` (pseudo-instruction) |
| **S-Type** | `SW` | `Memory[rs1 + imm] = rs2` (store word) |
| | `SB` | `Memory[rs1 + imm] = rs2` (store byte) |
| **B-Type** | `BEQ` | Branch if `rs1 == rs2` |
| | `BNE` | Branch if `rs1 != rs2` |
| | `BLT` | Branch if `rs1 < rs2` |
| | `BGE` | Branch if `rs1 >= rs2` |
| **J-Type** | `JAL` | `rd = PC + 4`; jump to `PC + offset` |
| **U-Type** | `ECALL` | System call (dump registers) |
| | `HALT` | Stop execution |

---

## Utility Methods

| Method | Return | Purpose |
|--------|--------|---------|
| `isBranch()` | `boolean` | True for BEQ, BNE, BLT, BGE |
| `isLoad()` | `boolean` | True for LW, LB |
| `isStore()` | `boolean` | True for SW, SB |
| `writesBack()` | `boolean` | True for all except stores, branches, ECALL, HALT |

---

*End of Report*
