package common;

// Opcode enum - exactly as defined in Reference.txt
// R-Type (opcode=000): ADD, SUB, SLL, XOR, SRL, OR, AND, MUL, DIV
// I-Type (opcode=001): ADDI, XORI, ORI, ANDI, LB, LW, LI
// S-Type (opcode=100): SB, SW
// B-Type (opcode=011): BEQ, BNE, BLT, BGE
// J-Type (opcode=010): JAL
// U-Type (opcode=101): ECALL, HALT
public enum Opcode {
    // R-Type
    ADD,
    SUB,
    SLL,
    XOR,
    SRL,
    OR,
    AND,
    MUL,
    DIV,

    // I-Type
    ADDI,
    XORI,
    ORI,
    ANDI,
    LB,
    LW,
    LI,

    // S-Type
    SB,
    SW,

    // B-Type
    BEQ,
    BNE,
    BLT,
    BGE,

    // J-Type
    JAL,

    // U-Type
    ECALL,
    HALT
}
