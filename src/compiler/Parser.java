package compiler;

import common.Instruction;
import common.Opcode;

import java.util.ArrayList;
import java.util.Map;

// Parses tokenized assembly lines into Instruction objects.
// Each line is split into parts (opcode + operands) and converted
// to the appropriate instruction type (R, I, S, B, J, U).
public class Parser {
    private final Map<String, Integer> symbols;
    private int instrIndex; // current instruction number

    public Parser(Map<String, Integer> symbols) {
        this.symbols = symbols;
        this.instrIndex = 0;
    }

    // Parse all tokens into a list of Instructions
    ArrayList<Instruction> parse(ArrayList<String> tokens) {
        ArrayList<Instruction> program = new ArrayList<>();
        instrIndex = 0;

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i).trim();
            if (token.isEmpty())
                continue;

            // Skip standalone labels like "loop:"
            if (token.endsWith(":"))
                continue;

            // Handle "label: instruction" on the same line
            String line = token;
            if (token.contains(":")) {
                line = token.substring(token.indexOf(':') + 1).trim();
                if (line.isEmpty())
                    continue;
            }

            Instruction instr = parseInstruction(line, instrIndex);
            if (instr != null) {
                program.add(instr);
                instrIndex++;
            }
        }
        return program;
    }

    // Parse a single assembly line into an Instruction
    private Instruction parseInstruction(String line, int idx) {
        line = line.replaceAll(",", " "); // remove commas
        String[] p = line.trim().split("\\s+"); // split by whitespace
        if (p.length == 0 || p[0].isEmpty())
            return null;

        String op = p[0].toUpperCase();
        int pc = idx * 4; // byte address of this instruction

        if (op.equals("ADD"))
            return Instruction.rType(Opcode.ADD, reg(p[1]), reg(p[2]), reg(p[3]));
        if (op.equals("SUB"))
            return Instruction.rType(Opcode.SUB, reg(p[1]), reg(p[2]), reg(p[3]));
        if (op.equals("MUL"))
            return Instruction.rType(Opcode.MUL, reg(p[1]), reg(p[2]), reg(p[3]));
        if (op.equals("DIV"))
            return Instruction.rType(Opcode.DIV, reg(p[1]), reg(p[2]), reg(p[3]));
        if (op.equals("SLL"))
            return Instruction.rType(Opcode.SLL, reg(p[1]), reg(p[2]), reg(p[3]));
        if (op.equals("SRL"))
            return Instruction.rType(Opcode.SRL, reg(p[1]), reg(p[2]), reg(p[3]));
        if (op.equals("XOR"))
            return Instruction.rType(Opcode.XOR, reg(p[1]), reg(p[2]), reg(p[3]));
        if (op.equals("OR"))
            return Instruction.rType(Opcode.OR, reg(p[1]), reg(p[2]), reg(p[3]));
        if (op.equals("AND"))
            return Instruction.rType(Opcode.AND, reg(p[1]), reg(p[2]), reg(p[3]));

        if (op.equals("ADDI"))
            return Instruction.iType(Opcode.ADDI, reg(p[1]), reg(p[2]), imm(p[3], pc));

        // LI rd, imm — load immediate (pseudo-instruction, 2 operands only)
        if (op.equals("LI"))
            return Instruction.iType(Opcode.LI, reg(p[1]), 0, immAbsolute(p[2]));

        // LW/LB: "LW x1, 0(x2)" or "LW x1, x2, imm"
        if (op.equals("LW")) {
            int rd = reg(p[1]);
            if (p[2].contains("(")) {
                return Instruction.iType(Opcode.LW, rd, memReg(p[2]), memOff(p[2]));
            } else {
                return Instruction.iType(Opcode.LW, rd, reg(p[2]), imm(p[3], pc));
            }
        }
        if (op.equals("LB")) {
            int rd = reg(p[1]);
            if (p[2].contains("(")) {
                return Instruction.iType(Opcode.LB, rd, memReg(p[2]), memOff(p[2]));
            } else {
                return Instruction.iType(Opcode.LB, rd, reg(p[2]), imm(p[3], pc));
            }
        }

        // --- S-Type: op rs2, offset(rs1) ---
        if (op.equals("SW")) {
            int rs2 = reg(p[1]);
            if (p[2].contains("(")) {
                return Instruction.sType(Opcode.SW, memReg(p[2]), rs2, memOff(p[2]));
            } else {
                return Instruction.sType(Opcode.SW, reg(p[2]), rs2, imm(p[3], pc));
            }
        }
        if (op.equals("SB")) {
            int rs2 = reg(p[1]);
            if (p[2].contains("(")) {
                return Instruction.sType(Opcode.SB, memReg(p[2]), rs2, memOff(p[2]));
            } else {
                return Instruction.sType(Opcode.SB, reg(p[2]), rs2, imm(p[3], pc));
            }
        }

        // --- B-Type: op rs1, rs2, label ---
        if (op.equals("BEQ"))
            return Instruction.bType(Opcode.BEQ, reg(p[1]), reg(p[2]), imm(p[3], pc));
        if (op.equals("BNE"))
            return Instruction.bType(Opcode.BNE, reg(p[1]), reg(p[2]), imm(p[3], pc));
        if (op.equals("BLT"))
            return Instruction.bType(Opcode.BLT, reg(p[1]), reg(p[2]), imm(p[3], pc));
        if (op.equals("BGE"))
            return Instruction.bType(Opcode.BGE, reg(p[1]), reg(p[2]), imm(p[3], pc));

        // --- J-Type: JAL rd, label ---
        if (op.equals("JAL"))
            return Instruction.jType(Opcode.JAL, reg(p[1]), imm(p[2], pc));

        // --- U-Type: system instructions ---
        if (op.equals("ECALL"))
            return Instruction.uType(Opcode.ECALL);
        if (op.equals("HALT"))
            return Instruction.uType(Opcode.HALT);

        throw new RuntimeException("Unknown instruction: " + op);
    }

    // Parse register name "x5" → 5 (must be x0–x31)
    private int reg(String s) {
        s = s.trim();
        if (s.startsWith("x") || s.startsWith("X")) {
            int r = Integer.parseInt(s.substring(1));
            if (r < 0 || r > 31)
                throw new RuntimeException("Register out of range (must be x0–x31): " + s);
            return r;
        }
        throw new RuntimeException("Bad register: " + s);
    }

    // Parse immediate value or label name → integer offset
    // Labels are resolved to (label_address - current_pc) for relative jumps
    private int imm(String s, int pc) {
        s = s.trim();
        if (isLabel(s)) {
            Integer addr = symbols.get(s);
            if (addr == null)
                throw new RuntimeException("Undefined label: " + s);
            return addr - pc; // relative offset
        }
        return Integer.parseInt(s);
    }

    // Parse immediate as absolute value (no PC-relative offset)
    // Used by LI where labels should resolve to absolute addresses
    private int immAbsolute(String s) {
        s = s.trim();
        if (isLabel(s)) {
            Integer addr = symbols.get(s);
            if (addr == null)
                throw new RuntimeException("Undefined label: " + s);
            return addr; // absolute address, not relative
        }
        return Integer.parseInt(s);
    }

    // Extract offset from "offset(reg)" format, e.g. "4(x1)" → 4
    private int memOff(String s) {
        int lp = s.indexOf('(');
        String off = s.substring(0, lp).trim();
        return off.isEmpty() ? 0 : Integer.parseInt(off);
    }

    // Extract register number from "offset(reg)" format, e.g. "4(x1)" → 1
    private int memReg(String s) {
        int lp = s.indexOf('(');
        int rp = s.indexOf(')');
        return reg(s.substring(lp + 1, rp).trim());
    }

    // Check if a string looks like a label (starts with letter or underscore)
    private boolean isLabel(String s) {
        if (s == null || s.isEmpty())
            return false;
        char c = s.charAt(0);
        return Character.isLetter(c) || c == '_';
    }
}
