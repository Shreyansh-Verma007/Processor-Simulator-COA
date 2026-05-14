package common;

/**
 * Encodes/decodes Instruction objects as 32-bit integers for storage in memory.
 *
 * Bit layout (32 bits total):
 * [31–27] opcode (5 bits, unsigned)
 * [26–22] rd (5 bits, unsigned)
 * [21–17] rs1 (5 bits, unsigned)
 * [16–12] rs2 (5 bits, unsigned)
 * [11–0] immediate (12 bits, sign-extended)
 */
public class InstructionEncoder {

    public static int encode(Instruction instr) {
        int opcodeIdx = instr.opcode().ordinal() & 0x1F;
        int rd = instr.rd() & 0x1F;
        int rs1 = instr.rs1() & 0x1F;
        int rs2 = instr.rs2() & 0x1F;
        int imm = instr.immediate() & 0xFFF;

        return (opcodeIdx << 27)
                | (rd << 22)
                | (rs1 << 17)
                | (rs2 << 12)
                | imm;
    }

    public static Instruction decode(int word) {
        int opcodeIdx = (word >>> 27) & 0x1F;
        int rd = (word >>> 22) & 0x1F;
        int rs1 = (word >>> 17) & 0x1F;
        int rs2 = (word >>> 12) & 0x1F;
        int imm = word & 0xFFF;

        Opcode[] opcodes = Opcode.values();
        if (opcodeIdx >= opcodes.length) {
            throw new IllegalArgumentException(
                "Invalid opcode index in encoded word: " + opcodeIdx);
        }

        Opcode op = opcodes[opcodeIdx];

        // Sign-extend 12-bit immediate for instructions that use signed offsets:
        // branches (PC-relative), ADDI, JAL, and load/store (base+offset addressing)
        if (op.isBranch() || op == Opcode.ADDI || op == Opcode.JAL
                || op.isLoad() || op.isStore()) {
            if ((imm & 0x800) != 0) {
                imm |= 0xFFFFF000;
            }
        }

        return new Instruction(op, rd, rs1, rs2, imm);
    }
}
