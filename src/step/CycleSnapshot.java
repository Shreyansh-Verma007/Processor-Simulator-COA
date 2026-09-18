package step;

import common.Opcode;
import pipeline_registers.EX_MEM;
import pipeline_registers.ID_EX;
import pipeline_registers.IF_ID;
import pipeline_registers.MEM_WB;

import java.util.List;

/**
 * A snapshot of the full pipeline state after one clock cycle.
 * Serialized to JSON by ApiServer and sent to the frontend.
 */
public class CycleSnapshot {

    public long cycle;
    public boolean done;
    public boolean stall;
    public boolean flush;
    public String hazardType; // "NONE", "LOAD_USE", "RAW", "BRANCH_FLUSH", "CACHE_STALL"
    public int pc;

    // One entry per stage: IF, ID, EX, MEM, WB
    public StageState[] stages = new StageState[5];

    // All 32 register values
    public int[] registers;

    // Running statistics
    public long totalCycles;
    public long totalStalls;
    public long totalFlushes;
    public long instructionsRetired;

    public static class StageState {
        public String name;       // "IF", "ID", "EX", "MEM", "WB"
        public String label;      // Human-readable instruction, e.g. "ADD x3, x1, x2"
        public boolean isNop;
        public boolean isStall;
        public boolean isFlush;
        public int instrPc;       // PC of the instruction in this stage
    }

    /**
     * Converts the snapshot to a compact JSON string (no external lib needed).
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"cycle\":").append(cycle).append(",");
        sb.append("\"done\":").append(done).append(",");
        sb.append("\"stall\":").append(stall).append(",");
        sb.append("\"flush\":").append(flush).append(",");
        sb.append("\"hazardType\":\"").append(esc(hazardType)).append("\",");
        sb.append("\"pc\":").append(pc).append(",");

        // Stages array
        sb.append("\"stages\":[");
        for (int i = 0; i < stages.length; i++) {
            StageState s = stages[i];
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"name\":\"").append(esc(s.name)).append("\",");
            sb.append("\"label\":\"").append(esc(s.label)).append("\",");
            sb.append("\"isNop\":").append(s.isNop).append(",");
            sb.append("\"isStall\":").append(s.isStall).append(",");
            sb.append("\"isFlush\":").append(s.isFlush).append(",");
            sb.append("\"instrPc\":").append(s.instrPc);
            sb.append("}");
        }
        sb.append("],");

        // Registers
        sb.append("\"registers\":[");
        for (int i = 0; i < registers.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(registers[i]);
        }
        sb.append("],");

        // Stats
        sb.append("\"totalCycles\":").append(totalCycles).append(",");
        sb.append("\"totalStalls\":").append(totalStalls).append(",");
        sb.append("\"totalFlushes\":").append(totalFlushes).append(",");
        sb.append("\"instructionsRetired\":").append(instructionsRetired);

        sb.append("}");
        return sb.toString();
    }

    /**
     * Builds a human-readable label for an instruction in a pipeline register.
     */
    public static String labelForOpcode(Opcode op, int rd, int rs1, int rs2, int imm, int instrPc) {
        if (op == null) return "NOP";
        switch (op) {
            case ADD: return "ADD x" + rd + ", x" + rs1 + ", x" + rs2;
            case SUB: return "SUB x" + rd + ", x" + rs1 + ", x" + rs2;
            case MUL: return "MUL x" + rd + ", x" + rs1 + ", x" + rs2;
            case DIV: return "DIV x" + rd + ", x" + rs1 + ", x" + rs2;
            case SLL: return "SLL x" + rd + ", x" + rs1 + ", x" + rs2;
            case SRL: return "SRL x" + rd + ", x" + rs1 + ", x" + rs2;
            case XOR: return "XOR x" + rd + ", x" + rs1 + ", x" + rs2;
            case OR:  return "OR x"  + rd + ", x" + rs1 + ", x" + rs2;
            case AND: return "AND x" + rd + ", x" + rs1 + ", x" + rs2;
            case ADDI: return "ADDI x" + rd + ", x" + rs1 + ", " + imm;
            case LW:   return "LW x" + rd + ", " + imm + "(x" + rs1 + ")";
            case LB:   return "LB x" + rd + ", " + imm + "(x" + rs1 + ")";
            case LI:   return "LI x" + rd + ", " + imm;
            case SW:   return "SW x" + rs2 + ", " + imm + "(x" + rs1 + ")";
            case SB:   return "SB x" + rs2 + ", " + imm + "(x" + rs1 + ")";
            case BEQ:  return "BEQ x" + rs1 + ", x" + rs2 + ", " + imm;
            case BNE:  return "BNE x" + rs1 + ", x" + rs2 + ", " + imm;
            case BLT:  return "BLT x" + rs1 + ", x" + rs2 + ", " + imm;
            case BGE:  return "BGE x" + rs1 + ", x" + rs2 + ", " + imm;
            case JAL:  return "JAL x" + rd + ", " + imm;
            case ECALL: return "ECALL";
            case HALT:  return "HALT";
            default:    return op.name();
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
