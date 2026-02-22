package compiler;

import common.Instruction;
import common.Opcode;

import java.util.ArrayList;
import java.util.Map;

public class Parser {
    private Map<String, Integer> symbols;
    private int instrIndex;

    public Parser(Map<String, Integer> symbols) {
        this.symbols = symbols;
        this.instrIndex = 0;
    }

    public Parser() {
        this.symbols = new java.util.HashMap<String, Integer>();
        this.instrIndex = 0;
    }

    ArrayList<Instruction> parse(ArrayList<String> tokens) {
        ArrayList<Instruction> program = new ArrayList<Instruction>();
        instrIndex = 0;

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i).trim();
            if (token.isEmpty())
                continue;

            if (token.endsWith(":"))
                continue;

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

    private Instruction parseInstruction(String line, int idx) {
        line = line.replaceAll(",", " ");
        String[] p = line.trim().split("\\s+");
        if (p.length == 0 || p[0].isEmpty())
            return null;

        String op = p[0].toUpperCase();
        int pc = idx * 4;

        if (op.equals("ADD")) {
            return Instruction.rType(Opcode.ADD, reg(p[1]), reg(p[2]), reg(p[3]));
        } else if (op.equals("SUB")) {
            return Instruction.rType(Opcode.SUB, reg(p[1]), reg(p[2]), reg(p[3]));
        } else if (op.equals("SLL")) {
            return Instruction.rType(Opcode.SLL, reg(p[1]), reg(p[2]), reg(p[3]));
        } else if (op.equals("XOR")) {
            return Instruction.rType(Opcode.XOR, reg(p[1]), reg(p[2]), reg(p[3]));
        } else if (op.equals("SRL")) {
            return Instruction.rType(Opcode.SRL, reg(p[1]), reg(p[2]), reg(p[3]));
        } else if (op.equals("OR")) {
            return Instruction.rType(Opcode.OR, reg(p[1]), reg(p[2]), reg(p[3]));
        } else if (op.equals("AND")) {
            return Instruction.rType(Opcode.AND, reg(p[1]), reg(p[2]), reg(p[3]));
        } else if (op.equals("MUL")) {
            return Instruction.rType(Opcode.MUL, reg(p[1]), reg(p[2]), reg(p[3]));
        } else if (op.equals("DIV")) {
            return Instruction.rType(Opcode.DIV, reg(p[1]), reg(p[2]), reg(p[3]));
        } else if (op.equals("ADDI")) {
            return Instruction.iType(Opcode.ADDI, reg(p[1]), reg(p[2]), imm(p[3], pc));
        } else if (op.equals("XORI")) {
            return Instruction.iType(Opcode.XORI, reg(p[1]), reg(p[2]), imm(p[3], pc));
        } else if (op.equals("ORI")) {
            return Instruction.iType(Opcode.ORI, reg(p[1]), reg(p[2]), imm(p[3], pc));
        } else if (op.equals("ANDI")) {
            return Instruction.iType(Opcode.ANDI, reg(p[1]), reg(p[2]), imm(p[3], pc));
        } else if (op.equals("LI")) {
            if (p.length == 3) {
                return Instruction.iType(Opcode.LI, reg(p[1]), 0, imm(p[2], pc));
            } else {
                return Instruction.iType(Opcode.LI, reg(p[1]), reg(p[2]), imm(p[3], pc));
            }
        } else if (op.equals("LW")) {
            int rd = reg(p[1]);
            if (p[2].contains("(")) {
                return Instruction.iType(Opcode.LW, rd, memReg(p[2]), memOff(p[2]));
            } else {
                return Instruction.iType(Opcode.LW, rd, reg(p[2]), imm(p[3], pc));
            }
        } else if (op.equals("LB")) {
            int rd = reg(p[1]);
            if (p[2].contains("(")) {
                return Instruction.iType(Opcode.LB, rd, memReg(p[2]), memOff(p[2]));
            } else {
                return Instruction.iType(Opcode.LB, rd, reg(p[2]), imm(p[3], pc));
            }
        } else if (op.equals("SW")) {
            int rs2 = reg(p[1]);
            if (p[2].contains("(")) {
                return Instruction.sType(Opcode.SW, memReg(p[2]), rs2, memOff(p[2]));
            } else {
                return Instruction.sType(Opcode.SW, reg(p[2]), rs2, imm(p[3], pc));
            }
        } else if (op.equals("SB")) {
            int rs2 = reg(p[1]);
            if (p[2].contains("(")) {
                return Instruction.sType(Opcode.SB, memReg(p[2]), rs2, memOff(p[2]));
            } else {
                return Instruction.sType(Opcode.SB, reg(p[2]), rs2, imm(p[3], pc));
            }
        } else if (op.equals("BEQ")) {
            return Instruction.bType(Opcode.BEQ, reg(p[1]), reg(p[2]), imm(p[3], pc));
        } else if (op.equals("BNE")) {
            return Instruction.bType(Opcode.BNE, reg(p[1]), reg(p[2]), imm(p[3], pc));
        } else if (op.equals("BLT")) {
            return Instruction.bType(Opcode.BLT, reg(p[1]), reg(p[2]), imm(p[3], pc));
        } else if (op.equals("BGE")) {
            return Instruction.bType(Opcode.BGE, reg(p[1]), reg(p[2]), imm(p[3], pc));
        } else if (op.equals("JAL")) {
            return Instruction.jType(Opcode.JAL, reg(p[1]), imm(p[2], pc));
        } else if (op.equals("ECALL")) {
            return Instruction.uType(Opcode.ECALL, 0, "");
        } else if (op.equals("HALT")) {
            return Instruction.uType(Opcode.HALT, 0, "");
        } else {
            throw new RuntimeException("Unknown instruction: " + op);
        }
    }

    private int reg(String s) {
        s = s.trim();
        if (s.startsWith("x") || s.startsWith("X")) {
            return Integer.parseInt(s.substring(1));
        }
        throw new RuntimeException("Bad register: " + s);
    }

    private int imm(String s, int pc) {
        s = s.trim();
        if (isLabel(s)) {
            Integer addr = symbols.get(s);
            if (addr == null) {
                throw new RuntimeException("Undefined label: " + s);
            }
            return addr - pc;
        }
        return Integer.parseInt(s);
    }

    private int memOff(String s) {
        int lp = s.indexOf('(');
        String off = s.substring(0, lp).trim();
        if (off.isEmpty())
            return 0;
        return Integer.parseInt(off);
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
