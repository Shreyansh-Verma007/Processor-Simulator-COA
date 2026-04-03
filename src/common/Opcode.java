package common;

// All supported opcodes for the simulator's custom RISC-V-like ISA.
// Grouped by instruction format type.
public enum Opcode {

    // R-Type: two source registers → one destination register
    ADD, // rd = rs1 + rs2
    SUB, // rd = rs1 - rs2
    MUL, // rd = rs1 * rs2
    DIV, // rd = rs1 / rs2
    SLL, // rd = rs1 << rs2 (shift left logical)
    SRL, // rd = rs1 >>> rs2 (shift right logical, unsigned)
    XOR, // rd = rs1 ^ rs2
    OR, // rd = rs1 | rs2
    AND, // rd = rs1 & rs2

    // I-Type: one source register + immediate → one destination register
    ADDI, // rd = rs1 + immediate
    LW, // rd = Memory[rs1 + immediate] (load word, 32-bit)
    LB, // rd = Memory[rs1 + immediate] (load byte, 8-bit)
    LI, // rd = immediate (load immediate, pseudo-instruction)

    // S-Type: store register value to memory
    SW, // Memory[rs1 + immediate] = rs2 (store word)
    SB, // Memory[rs1 + immediate] = rs2 (store byte)

    // B-Type: conditional branches (compare two registers, jump by offset)
    BEQ, // if rs1 == rs2, jump to PC + offset
    BNE, // if rs1 != rs2, jump to PC + offset
    BLT, // if rs1 < rs2, jump to PC + offset
    BGE, // if rs1 >= rs2, jump to PC + offset

    // J-Type: unconditional jump
    JAL, // rd = PC + 4; jump to PC + offset (jump and link)

    // U-Type: system/control
    ECALL, // system call — dumps registers to console
    HALT; // stop execution

    // ── Utility Methods ──────────────────────────────────────────────────

    public boolean isBranch() {
        return this == BEQ || this == BNE || this == BLT || this == BGE;
    }

    public boolean isLoad() {
        return this == LW || this == LB;
    }

    public boolean isStore() {
        return this == SW || this == SB;
    }

    public boolean writesBack() {
        return !isStore() && !isBranch() && this != ECALL && this != HALT;
    }
}
