# Study Report — `InstructionEncoder.java`

> **File:** `src/common/InstructionEncoder.java`
> **Module:** Common — Binary Encoding/Decoding
> **Date Generated:** 2026-05-14 *(Phase 1)*

---

## Overview

`InstructionEncoder` encodes and decodes `Instruction` objects as **32-bit integers** for storage in the simulator's byte-addressable memory. This allows instructions to be fetched from memory as raw words, matching real hardware behavior.

---

## Bit Layout (32 bits)

| Bits | Field | Width | Encoding |
|------|-------|-------|----------|
| `[31–27]` | opcode | 5 bits | Unsigned (`Opcode.ordinal()`) |
| `[26–22]` | rd | 5 bits | Unsigned register index |
| `[21–17]` | rs1 | 5 bits | Unsigned register index |
| `[16–12]` | rs2 | 5 bits | Unsigned register index |
| `[11–0]` | immediate | 12 bits | Sign-extended for branches/ADDI/JAL |

---

## Summary Table

| Method | Visibility | Return | Purpose |
|--------|-----------|--------|---------|
| `encode(instr)` | `public static` | `int` | Encode `Instruction` → 32-bit word |
| `decode(word)` | `public static` | `Instruction` | Decode 32-bit word → `Instruction` |

---

## Sign Extension

The `decode` method applies **sign extension** on the 12-bit immediate only for PC-relative instructions (`BEQ`, `BNE`, `BLT`, `BGE`, `ADDI`, `JAL`). Load/store/LI instructions treat the immediate as an unsigned offset (0–4095).

---

*End of Report*
