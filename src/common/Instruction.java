package common;

public record Instruction(
        Opcode opcode,
        int rd,
        int rs1,
        int rs2,
        int immediate,
        String label
) {
    // Factory for R-type
    public static Instruction rType(Opcode op, int rd, int rs1, int rs2) {
        return new Instruction(op, rd, rs1, rs2, 0, "");
    }

    // Factory for I-type (immediate)
    public static Instruction iType(Opcode op, int rd, int rs1, int imm) {
        return new Instruction(op, rd, rs1, 0, imm, "");
    }

    // Factory for S-type (store) - use rs1, rs2 and immediate (store offset)
    public static Instruction sType(Opcode op, int rs1, int rs2, int imm) {
        return new Instruction(op, 0, rs1, rs2, imm, "");
    }

    // Factory for B-type (branch) - rs1, rs2 and branch offset
    public static Instruction bType(Opcode op, int rs1, int rs2, int imm) {
        return new Instruction(op, 0, rs1, rs2, imm, "");
    }

    // Factory for J-type (jump) - rd and immediate (target)
    public static Instruction jType(Opcode op, int rd, int imm) {
        return new Instruction(op, rd, 0, 0, imm, "");
    }

    // Factory for U-type (system/other) - may carry a funct3 or label
    public static Instruction uType(Opcode op, int rd, String label) {
        return new Instruction(op, rd, 0, 0, 0, label == null ? "" : label);
    }
}
