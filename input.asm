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
LW x4, 0(x0)
BNE x4, x0, LABEL     # bnez $a0, LABEL

LABEL:
add x0 x0 x0

ECALL
HALT