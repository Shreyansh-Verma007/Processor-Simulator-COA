# ╔══════════════════════════════════════════════════════════════════════════════╗
# ║                    RISC-V PIPELINE SIMULATOR — QUICK REFERENCE               ║
# ╠══════════════════════════════════════════════════════════════════════════════╣
# ║  WORKFLOW:                                                                   ║
# ║    1. Write your assembly code below the reference section                   ║
# ║    2. Run Main.java (no arguments needed)                                    ║
# ║    3. Program output (register dump)  →  console.txt                         ║
# ║    4. Simulation stats (cycles, IPC)  →  output.txt                          ║
# ║    5. Edit this file, re-run Main — outputs are overwritten each time        ║
# ╚══════════════════════════════════════════════════════════════════════════════╝
#
# ─────────────────────────────── REGISTERS ───────────────────────────────────
#   x0       = always 0 (hardwired, writes are ignored)
#   x1       = general purpose (often return address)
#   x2 (SP)  = stack pointer (initialized to 0x0FFF = 4095)
#   x3–x31   = general purpose
#   Total: 32 registers, each 32-bit
#
# ──────────────────────────────── MEMORY ─────────────────────────────────────
#   Total size : 4 KB (4096 bytes = 1024 words)
#   Addresses  : 0 to 4095
#   Word size  : 4 bytes (word-aligned addresses must be multiples of 4)
#   Layout:
#     0–1023     .text (instructions)
#     1024+      .data (arrays, variables) — use addresses >= 100 for data
#     ~4095      stack (grows downward from SP)
#
# ─────────────────────────── INSTRUCTION SET ─────────────────────────────────
#
#  ┌─ R-Type (register-register) ─────────────────────────────────────────────┐
#  │  Syntax: OP rd, rs1, rs2                                                 │
#  │                                                                          │
#  │  ADD  rd, rs1, rs2    rd = rs1 + rs2                         (1 cycle)   │
#  │  SUB  rd, rs1, rs2    rd = rs1 - rs2                         (1 cycle)   │
#  │  MUL  rd, rs1, rs2    rd = rs1 * rs2                         (3 cycles)  │
#  │  DIV  rd, rs1, rs2    rd = rs1 / rs2                         (4 cycles)  │
#  │  SLL  rd, rs1, rs2    rd = rs1 << rs2  (shift left logical)  (1 cycle)   │
#  │  SRL  rd, rs1, rs2    rd = rs1 >>> rs2 (shift right logical) (1 cycle)   │
#  │  XOR  rd, rs1, rs2    rd = rs1 ^ rs2   (bitwise XOR)        (1 cycle)    │
#  │  OR   rd, rs1, rs2    rd = rs1 | rs2   (bitwise OR)         (1 cycle)    │
#  │  AND  rd, rs1, rs2    rd = rs1 & rs2   (bitwise AND)        (1 cycle)    │
#  └──────────────────────────────────────────────────────────────────────────┘
#
#  ┌─ I-Type (immediate) ────────────────────────────────────────────────────┐
#  │  ADDI rd, rs1, imm    rd = rs1 + immediate                  (1 cycle)   │
#  │  LI   rd, imm         rd = immediate (pseudo: ADDI rd, x0, imm)         │
#  │  LW   rd, offset(rs1) rd = Memory[rs1 + offset] (load word) (1 cycle)   │
#  │  LB   rd, offset(rs1) rd = Memory[rs1 + offset] (load byte) (1 cycle)   │
#  └─────────────────────────────────────────────────────────────────────────┘
#
#  ┌─ S-Type (store) ────────────────────────────────────────────────────────┐
#  │  SW   rs2, offset(rs1)  Memory[rs1 + offset] = rs2 (store word)         │
#  │  SB   rs2, offset(rs1)  Memory[rs1 + offset] = rs2 (store byte)         │
#  └─────────────────────────────────────────────────────────────────────────┘
#
#  ┌─ B-Type (branches — conditional jumps to labels) ───────────────────────┐
#  │  Syntax: OP rs1, rs2, label                                             │
#  │                                                                         │
#  │  BEQ  rs1, rs2, label   jump if rs1 == rs2                              │
#  │  BNE  rs1, rs2, label   jump if rs1 != rs2                              │
#  │  BLT  rs1, rs2, label   jump if rs1 <  rs2                              │
#  │  BGE  rs1, rs2, label   jump if rs1 >= rs2                              │
#  └─────────────────────────────────────────────────────────────────────────┘
#
#  ┌─ J-Type (unconditional jump) ───────────────────────────────────────────┐
#  │  JAL  rd, label      rd = PC+4, then jump to label (jump & link)        │
#  │                      Use JAL x0, label for a plain jump (no link)       │
#  └─────────────────────────────────────────────────────────────────────────┘
#
#  ┌─ System ────────────────────────────────────────────────────────────────┐
#  │  ECALL               print register dump to console                     │
#  │  HALT                stop the processor                                 │
#  └─────────────────────────────────────────────────────────────────────────┘
#
# ───────────────────────────── SYNTAX NOTES ──────────────────────────────────
#   - Labels: end with colon, e.g.  my_loop:
#   - Comments: start with #
#   - Registers: x0 through x31 (case-insensitive: x1 or X1)
#   - Immediates: decimal integers (e.g. 42, -5)
#   - Memory access: offset(register), e.g. 0(x2), 8(x10)
#   - Use ECALL before HALT to see register values in console.txt
#   - Use NOP as: ADDI x0, x0, 0
#   - Add 3 NOPs before ECALL to ensure all results are written back
#
# ════════════════════════════════════════════════════════════════════════════
#   CODE BELOW 
# ════════════════════════════════════════════════════════════════════════════

# ---- store array in memory ----
    LI   x10, 100

    LI   x1, 12
    SW   x1, 0(x10)
    LI   x1, 5
    SW   x1, 4(x10)
    LI   x1, 34
    SW   x1, 8(x10)
    LI   x1, 3
    SW   x1, 12(x10)
    LI   x1, 45
    SW   x1, 16(x10)
    LI   x1, 5
    SW   x1, 20(x10)
    LI   x1, 6
    SW   x1, 24(x10)
    LI   x1, 23
    SW   x1, 28(x10)
    LI   x1, 9
    SW   x1, 32(x10)
    LI   x1, 11
    SW   x1, 36(x10)
    LI   x1, 1
    SW   x1, 40(x10)

# ---- bubble sort initialization ----
    LI   x11, 11         # array size (n)

outer_loop:
    ADDI x11, x11, -1    # n--
    BLT  x11, x0, sorted # if n < 0, done

    LI   x12, 0          # j = 0

inner_loop:
    BGE  x12, x11, outer_loop # if j >= n, end inner loop

    # address calculation: addr = x10 + j * 4
    ADD  x13, x12, x12   # x13 = j * 2
    ADD  x13, x13, x13   # x13 = j * 4
    ADD  x13, x13, x10   # addr = offset + base

    LW   x14, 0(x13)     # a = arr[j]
    LW   x15, 4(x13)     # b = arr[j+1]

    BGE  x15, x14, no_swap # if b >= a, skip swap

    # swap
    SW   x15, 0(x13)
    SW   x14, 4(x13)

no_swap:
    ADDI x12, x12, 1     # j++
    JAL  x0, inner_loop

sorted:
    # Load sorted array into registers to verify in dump
    LW   x20, 0(x10)
    LW   x21, 4(x10)
    LW   x22, 8(x10)
    LW   x23, 12(x10)
    LW   x24, 16(x10)
    LW   x25, 20(x10)
    LW   x26, 24(x10)
    LW   x27, 28(x10)
    LW   x28, 32(x10)
    LW   x29, 36(x10)
    LW   x30, 40(x10)

    # NOPs to drain pipeline before dump
    ADDI x0, x0, 0
    ADDI x0, x0, 0
    ADDI x0, x0, 0

    ECALL
    HALT