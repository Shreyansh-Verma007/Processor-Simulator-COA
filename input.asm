# ╔══════════════════════════════════════════════════════════════════════════════╗
# ║                    RISC-V PIPELINE SIMULATOR — QUICK REFERENCE             ║
# ╠══════════════════════════════════════════════════════════════════════════════╣
# ║  WORKFLOW:                                                                 ║
# ║    1. Write your assembly code below the reference section                 ║
# ║    2. Run Main.java (no arguments needed)                                  ║
# ║    3. Program output (register dump)  →  console.txt                       ║
# ║    4. Simulation stats (cycles, IPC)  →  output.txt                        ║
# ║    5. Edit this file, re-run Main — outputs are overwritten each time      ║
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
#  │  XOR  rd, rs1, rs2    rd = rs1 ^ rs2   (bitwise XOR)        (1 cycle)   │
#  │  OR   rd, rs1, rs2    rd = rs1 | rs2   (bitwise OR)         (1 cycle)   │
#  │  AND  rd, rs1, rs2    rd = rs1 & rs2   (bitwise AND)        (1 cycle)   │
#  └──────────────────────────────────────────────────────────────────────────┘
#
#  ┌─ I-Type (immediate) ────────────────────────────────────────────────────┐
#  │  ADDI rd, rs1, imm    rd = rs1 + immediate                  (1 cycle)   │
#  │  LI   rd, imm         rd = immediate (pseudo: ADDI rd, x0, imm)         │
#  │  LW   rd, offset(rs1) rd = Memory[rs1 + offset] (load word) (1 cycle)   │
#  │  LB   rd, offset(rs1) rd = Memory[rs1 + offset] (load byte) (1 cycle)   │
#  └──────────────────────────────────────────────────────────────────────────┘
#
#  ┌─ S-Type (store) ────────────────────────────────────────────────────────┐
#  │  SW   rs2, offset(rs1)  Memory[rs1 + offset] = rs2 (store word)         │
#  │  SB   rs2, offset(rs1)  Memory[rs1 + offset] = rs2 (store byte)         │
#  └──────────────────────────────────────────────────────────────────────────┘
#
#  ┌─ B-Type (branches — conditional jumps to labels) ───────────────────────┐
#  │  Syntax: OP rs1, rs2, label                                              │
#  │                                                                          │
#  │  BEQ  rs1, rs2, label   jump if rs1 == rs2                               │
#  │  BNE  rs1, rs2, label   jump if rs1 != rs2                               │
#  │  BLT  rs1, rs2, label   jump if rs1 <  rs2                               │
#  │  BGE  rs1, rs2, label   jump if rs1 >= rs2                               │
#  └──────────────────────────────────────────────────────────────────────────┘
#
#  ┌─ J-Type (unconditional jump) ───────────────────────────────────────────┐
#  │  JAL  rd, label      rd = PC+4, then jump to label (jump & link)         │
#  │                      Use JAL x0, label for a plain jump (no link)        │
#  └──────────────────────────────────────────────────────────────────────────┘
#
#  ┌─ System ────────────────────────────────────────────────────────────────┐
#  │  ECALL               print register dump to console                      │
#  │  HALT                stop the processor                                  │
#  └──────────────────────────────────────────────────────────────────────────┘
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
#   YOUR CODE BELOW — edit and re-run Main.java each time!
# ════════════════════════════════════════════════════════════════════════════

# ---- store array in memory ----
    LI   x10, 100

    LI   x1, 1
    SW   x1, 0(x10)
    LI   x1, 2
    SW   x1, 4(x10)
    LI   x1, 34
    SW   x1, 8(x10)
    LI   x1, 3
    SW   x1, 12(x10)
    LI   x1, 4
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

# ---- initialization ----
    ADDI x1, x10, 40     # pointer to last element
    LI   x3, 10          # loop counter
    LI   x2, 3           # need 3 odd numbers
    LI   x5, 0           # current odd count
    LI   x6, 1           # mask for odd check

loop:

    LW   x7, 0(x1)       # load current value
    AND  x8, x7, x6      # x8 = value & 1

    BEQ  x8, x0, reset_count

# ---- odd number case ----
    ADDI x5, x5, 1
    BEQ  x5, x2, true
    JAL  x0, skip

reset_count:
    LI   x5, 0

skip:
    ADDI x3, x3, -1
    ADDI x1, x1, -4

    BLT  x0, x3, loop

    JAL  x0, false

true:
    LI   x20, 1          # result = TRUE
    JAL  x0, end

false:
    LI   x20, 0          # result = FALSE

end:
    ADDI x0, x0, 0
    ADDI x0, x0, 0
    ADDI x0, x0, 0

    ECALL
    HALT