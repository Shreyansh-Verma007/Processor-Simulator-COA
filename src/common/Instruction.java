package common;

public record Instruction(
        Opcode opcode,
        int rd,
        int rs1,
        int rs2,
        int immediate) {
    // R-type
    public static Instruction rType(Opcode op, int rd, int rs1, int rs2) {
        return new Instruction(op, rd, rs1, rs2, 0);
    }

    // I-type (immediate)
    public static Instruction iType(Opcode op, int rd, int rs1, int imm) {
        return new Instruction(op, rd, rs1, 0, imm);
    }

    // S-type (store)
    public static Instruction sType(Opcode op, int rs1, int rs2, int imm) {
        return new Instruction(op, 0, rs1, rs2, imm);
    }

    // B-type (branch)
    public static Instruction bType(Opcode op, int rs1, int rs2, int imm) {
        return new Instruction(op, 0, rs1, rs2, imm);
    }

    // J-type
    public static Instruction jType(Opcode op, int rd, int imm) {
        return new Instruction(op, rd, 0, 0, imm);
    }

    // U-type (system: ECALL, HALT)
    public static Instruction uType(Opcode op, int rd) {
        return new Instruction(op, rd, 0, 0, 0);
    }
}
