package pipeline_stages;

import common.Config;
import common.Opcode;
import core.RegisterFile;
import hazard.ForwardResult;
import hazard.ForwardingUnit;
import pipeline_registers.EX_MEM;
import pipeline_registers.ID_EX;
import pipeline_registers.MEM_WB;

/**
 * Execute (EX) Stage.
 *
 * Performs ALU computation, resolves branch conditions, and detects BTFNT
 * mispredictions by comparing the actual branch outcome against the
 * prediction made in the ID stage.
 */
public class EX_Stage {

    public boolean haltFlag = false;

    private final RegisterFile rf;
    private final Config cfg;

    public EX_Stage(RegisterFile rf, Config cfg) {
        this.rf = rf;
        this.cfg = cfg;
    }

    // ── Main tick ────────────────────────────────────────────────────────

    public EX_MEM tick(ID_EX idEx, EX_MEM prevExMem,
            MEM_WB newMemWb, MEM_WB oldMemWb,
            ForwardingUnit fu) {

        EX_MEM out = new EX_MEM();
        if (idEx.isNop) {
            return out;
        }

        // Resolve operand values
        int a, b;
        if (cfg.isForwardingEnabled()) {
            a = resolveOperandA(idEx, prevExMem, newMemWb, oldMemWb, fu);
            b = resolveOperandB(idEx, prevExMem, newMemWb, oldMemWb, fu);
        } else {
            a = rf.read(idEx.rs1);
            b = rf.read(idEx.rs2);
        }

        // Multi-cycle instruction still counting down — emit a bubble
        if (idEx.latencyCyclesLeft > 0) {
            idEx.latencyCyclesLeft--;
            return out;
        }

        // Populate EX/MEM register
        out.isNop = false;
        out.opcode = idEx.opcode;
        out.rd = idEx.rd;
        out.writeData = b;

        out.aluResult = computeALU(idEx.opcode, a, b, idEx.immediate);
        out.branchTaken = resolveBranch(idEx, a, b, out);

        if (idEx.opcode == Opcode.JAL) {
            out.aluResult = idEx.pc + 4;
        }
        if (idEx.opcode == Opcode.ECALL) {
            rf.dump();
        }
        if (idEx.opcode == Opcode.HALT) {
            haltFlag = true;
        }

        return out;
    }

    // ── Operand forwarding ───────────────────────────────────────────────

    /**
     * Priority: EX/MEM → newMEM/WB → oldMEM/WB → register file.
     */
    private int resolveOperandA(ID_EX idEx, EX_MEM prevExMem,
            MEM_WB newMemWb, MEM_WB oldMemWb,
            ForwardingUnit fu) {
        ForwardResult fa = fu.getForwardA(idEx, prevExMem, newMemWb);
        if (fa == ForwardResult.FROM_EX_MEM)
            return prevExMem.aluResult;
        if (fa == ForwardResult.FROM_MEM_WB)
            return newMemWb.result;
        if (!oldMemWb.isNop && oldMemWb.rd != 0 && oldMemWb.rd == idEx.rs1)
            return oldMemWb.result;
        return rf.read(idEx.rs1);
    }

    private int resolveOperandB(ID_EX idEx, EX_MEM prevExMem,
            MEM_WB newMemWb, MEM_WB oldMemWb,
            ForwardingUnit fu) {
        ForwardResult fb = fu.getForwardB(idEx, prevExMem, newMemWb);
        if (fb == ForwardResult.FROM_EX_MEM)
            return prevExMem.aluResult;
        if (fb == ForwardResult.FROM_MEM_WB)
            return newMemWb.result;
        if (!oldMemWb.isNop && oldMemWb.rd != 0 && oldMemWb.rd == idEx.rs2)
            return oldMemWb.result;
        return rf.read(idEx.rs2);
    }

    // ── ALU ──────────────────────────────────────────────────────────────

    private int computeALU(Opcode op, int a, int b, int imm) {
        switch (op) {
            // R-Type
            case ADD:
                return a + b;
            case SUB:
                return a - b;
            case MUL:
                return a * b;
            case DIV:
                if (b == 0) return -1; // RISC-V: div by zero → -1
                if (a == Integer.MIN_VALUE && b == -1) return Integer.MIN_VALUE; // overflow guard
                return a / b;
            case SLL:
                return a << (b & 0x1F); // RISC-V: only low 5 bits of shift amount
            case SRL:
                return a >>> (b & 0x1F);
            case XOR:
                return a ^ b;
            case OR:
                return a | b;
            case AND:
                return a & b;

            // I-Type
            case ADDI:
                return a + imm;
            case LI:
                return imm;

            // Load / Store — address = base + offset
            case LW:
            case LB:
            case SW:
            case SB:
                return a + imm;

            default:
                return 0; // branches, ECALL, HALT — no ALU result
        }
    }

    // ── Branch resolution + BTFNT misprediction detection ────────────────

    /**
     * Resolves whether a branch is actually taken and computes the jump target.
     * For conditional branches, compares the actual outcome against the BTFNT
     * prediction and populates misprediction fields when they disagree.
     */
    private boolean resolveBranch(ID_EX idEx, int a, int b, EX_MEM out) {
        Opcode op = idEx.opcode;
        int imm = idEx.immediate;
        int pc = idEx.pc;

        boolean taken = evaluateCondition(op, a, b);

        if (taken) {
            out.jumpTarget = pc + imm;
        }

        // Misprediction detection (conditional branches only — JAL has no prediction)
        if (op.isBranch() && taken != idEx.branchPredictedTaken) {
            out.branchMispredicted = true;
            out.branchRecoveryPC = taken ? (pc + imm) : (pc + 4);
        }

        return taken;
    }

    private boolean evaluateCondition(Opcode op, int a, int b) {
        switch (op) {
            case BEQ:
                return a == b;
            case BNE:
                return a != b;
            case BLT:
                return a < b;
            case BGE:
                return a >= b;
            case JAL:
                return true;
            default:
                return false;
        }
    }
}
