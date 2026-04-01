package compiler;

import common.Instruction;
import common.Opcode;

import java.util.ArrayList;
import java.util.Map;

// Parses the .text section of tokenized assembly lines into Instruction objects.
// Skips .data section lines and directives.
public class Parser {
    private final Map<String, Integer> symbols;
    private int instrIndex;

    public Parser(Map<String, Integer> symbols) {
        this.symbols = symbols;
        this.instrIndex = 0;
    }

    /** Parse all lines, emitting only instructions from the .text section. */
    ArrayList<Instruction> parseText(ArrayList<String> lines) {
        ArrayList<Instruction> program = new ArrayList<>();
        instrIndex = 0;
        boolean inData = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty())
                continue;

            // Section switches
            if (line.equalsIgnoreCase(".data")) {
                inData = true;
                continue;
            }
            if (line.equalsIgnoreCase(".text")) {
                inData = false;
                continue;
            }

            // Skip entire data section
            if (inData)
                continue;

            // Strip label prefix (standalone "loop:" or inline "loop: ADD ...")
            if (line.contains(":")) {
                int colon = line.indexOf(':');
                line = line.substring(colon + 1).trim();
                if (line.isEmpty())
                    continue;
            }

            // Skip assembler directives in text section (.globl main, .align 2, etc.)
            if (line.startsWith("."))
                continue;

            Instruction instr = parseInstruction(line, instrIndex);
            if (instr != null) {
                program.add(instr);
                instrIndex++;
            }
        }
        return program;
    }

    // ── Instruction parsing ───────────────────────────────────────────────

    private Instruction parseInstruction(String line, int idx) {
        line = line.replaceAll(",", " ");
        String[] p = line.trim().split("\\s+");
        if (p.length == 0 || p[0].isEmpty())
            return null;

        String op = p[0].toUpperCase();
        int pc = idx * 4; // byte address of this instruction

        switch (op) {
            // R-Type
            case "ADD":
                return Instruction.rType(Opcode.ADD, reg(p[1]), reg(p[2]), reg(p[3]));
            case "SUB":
                return Instruction.rType(Opcode.SUB, reg(p[1]), reg(p[2]), reg(p[3]));
            case "MUL":
                return Instruction.rType(Opcode.MUL, reg(p[1]), reg(p[2]), reg(p[3]));
            case "DIV":
                return Instruction.rType(Opcode.DIV, reg(p[1]), reg(p[2]), reg(p[3]));
            case "SLL":
                return Instruction.rType(Opcode.SLL, reg(p[1]), reg(p[2]), reg(p[3]));
            case "SRL":
                return Instruction.rType(Opcode.SRL, reg(p[1]), reg(p[2]), reg(p[3]));
            case "XOR":
                return Instruction.rType(Opcode.XOR, reg(p[1]), reg(p[2]), reg(p[3]));
            case "OR":
                return Instruction.rType(Opcode.OR, reg(p[1]), reg(p[2]), reg(p[3]));
            case "AND":
                return Instruction.rType(Opcode.AND, reg(p[1]), reg(p[2]), reg(p[3]));

            // I-Type ALU
            case "ADDI":
                return Instruction.iType(Opcode.ADDI, reg(p[1]), reg(p[2]), imm(p[3], pc));

            // LI rd, imm — pseudo, 2 operands. Also handles "LI rd, label" (data addr)
            case "LI":
                return Instruction.iType(Opcode.LI, reg(p[1]), 0, immAbsolute(p[2]));

            // LA rd, label — load address of a data label (pseudo = LI with data addr)
            case "LA":
                return Instruction.iType(Opcode.LI, reg(p[1]), 0, immAbsolute(p[2]));

            // Loads
            case "LW": {
                int rd = reg(p[1]);
                return p[2].contains("(")
                        ? Instruction.iType(Opcode.LW, rd, memReg(p[2]), memOff(p[2]))
                        : Instruction.iType(Opcode.LW, rd, reg(p[2]), imm(p[3], pc));
            }
            case "LB": {
                int rd = reg(p[1]);
                return p[2].contains("(")
                        ? Instruction.iType(Opcode.LB, rd, memReg(p[2]), memOff(p[2]))
                        : Instruction.iType(Opcode.LB, rd, reg(p[2]), imm(p[3], pc));
            }

            // Stores
            case "SW": {
                int rs2 = reg(p[1]);
                return p[2].contains("(")
                        ? Instruction.sType(Opcode.SW, memReg(p[2]), rs2, memOff(p[2]))
                        : Instruction.sType(Opcode.SW, reg(p[2]), rs2, imm(p[3], pc));
            }
            case "SB": {
                int rs2 = reg(p[1]);
                return p[2].contains("(")
                        ? Instruction.sType(Opcode.SB, memReg(p[2]), rs2, memOff(p[2]))
                        : Instruction.sType(Opcode.SB, reg(p[2]), rs2, imm(p[3], pc));
            }

            // Branches
            case "BEQ":
                return Instruction.bType(Opcode.BEQ, reg(p[1]), reg(p[2]), imm(p[3], pc));
            case "BNE":
                return Instruction.bType(Opcode.BNE, reg(p[1]), reg(p[2]), imm(p[3], pc));
            case "BLT":
                return Instruction.bType(Opcode.BLT, reg(p[1]), reg(p[2]), imm(p[3], pc));
            case "BGE":
                return Instruction.bType(Opcode.BGE, reg(p[1]), reg(p[2]), imm(p[3], pc));

            // Jump
            case "JAL":
                return Instruction.jType(Opcode.JAL, reg(p[1]), imm(p[2], pc));

            // NOP pseudo — ADDI x0, x0, 0
            case "NOP":
                return Instruction.iType(Opcode.ADDI, 0, 0, 0);

            // MV pseudo — ADDI rd, rs, 0
            case "MV":
                return Instruction.iType(Opcode.ADDI, reg(p[1]), reg(p[2]), 0);

            // System
            case "ECALL":
                return Instruction.uType(Opcode.ECALL);
            case "HALT":
                return Instruction.uType(Opcode.HALT);

            default:
                throw new RuntimeException("Unknown instruction: " + op + " (line: " + line + ")");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private int reg(String s) {
        s = s.trim();
        if (s.startsWith("x") || s.startsWith("X")) {
            int r = Integer.parseInt(s.substring(1));
            if (r < 0 || r > 31)
                throw new RuntimeException("Register out of range (x0-x31): " + s);
            return r;
        }
        // ABI names
        switch (s.toLowerCase()) {
            case "zero":
                return 0;
            case "ra":
                return 1;
            case "sp":
                return 2;
            case "gp":
                return 3;
            case "tp":
                return 4;
            case "t0":
                return 5;
            case "t1":
                return 6;
            case "t2":
                return 7;
            case "s0":
            case "fp":
                return 8;
            case "s1":
                return 9;
            case "a0":
                return 10;
            case "a1":
                return 11;
            case "a2":
                return 12;
            case "a3":
                return 13;
            case "a4":
                return 14;
            case "a5":
                return 15;
            case "a6":
                return 16;
            case "a7":
                return 17;
            case "s2":
                return 18;
            case "s3":
                return 19;
            case "s4":
                return 20;
            case "s5":
                return 21;
            case "s6":
                return 22;
            case "s7":
                return 23;
            case "s8":
                return 24;
            case "s9":
                return 25;
            case "s10":
                return 26;
            case "s11":
                return 27;
            case "t3":
                return 28;
            case "t4":
                return 29;
            case "t5":
                return 30;
            case "t6":
                return 31;
        }
        throw new RuntimeException("Bad register: " + s);
    }

    private int imm(String s, int pc) {
        s = s.trim();
        if (isLabel(s)) {
            Integer addr = symbols.get(s);
            if (addr == null)
                throw new RuntimeException("Undefined label: " + s);
            return addr - pc; // PC-relative
        }
        return Integer.parseInt(s);
    }

    private int immAbsolute(String s) {
        s = s.trim();
        if (s.startsWith("0x") || s.startsWith("0X"))
            return Integer.parseUnsignedInt(s.substring(2), 16);
        if (isLabel(s)) {
            Integer addr = symbols.get(s);
            if (addr == null)
                throw new RuntimeException("Undefined label: " + s);
            return addr; // absolute address (used for la/li with data labels)
        }
        return Integer.parseInt(s);
    }

    private int memOff(String s) {
        int lp = s.indexOf('(');
        String off = s.substring(0, lp).trim();
        return off.isEmpty() ? 0 : Integer.parseInt(off);
    }

    private int memReg(String s) {
        int lp = s.indexOf('(');
        int rp = s.indexOf(')');
        return reg(s.substring(lp + 1, rp).trim());
    }

    private boolean isLabel(String s) {
        if (s == null || s.isEmpty())
            return false;
        char c = s.charAt(0);
        return Character.isLetter(c) || c == '_';
    }
}
