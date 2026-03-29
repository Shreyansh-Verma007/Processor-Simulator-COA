package pipeline_stages;

import common.Config;
import common.Opcode;
import core.RegisterFile;
import hazard.ForwardResult;
import hazard.ForwardingUnit;
import pipeline_registers.EX_MEM;
import pipeline_registers.ID_EX;
import pipeline_registers.MEM_WB;

// Execute Stage
public class EX_Stage {
    public boolean haltFlag = false; // set to true when ECALL/HALT is seen
    private RegisterFile rf; // reference to register file for re-reads after stalls
    private Config cfg;

    public EX_Stage(RegisterFile rf, Config cfg) {
        this.rf = rf;
        this.cfg = cfg;
    }

    public EX_MEM tick(ID_EX idEx, EX_MEM prevExMem,
            MEM_WB newMemWb, MEM_WB oldMemWb,
            ForwardingUnit fu) {
        EX_MEM out = new EX_MEM();
        if (idEx.isNop)
            return out; // nothing to execute

        int a, b;
        if (cfg.isForwardingEnabled()) {
            a = resolveOperandA(idEx, prevExMem, newMemWb, oldMemWb, fu);
            b = resolveOperandB(idEx, prevExMem, newMemWb, oldMemWb, fu);
        } else {
            // No forwarding — read from register file (stalls ensure correctness)
            a = rf.read(idEx.rs1);
            b = rf.read(idEx.rs2);
        }
        idEx.valA = a;
        idEx.valB = b;

        // === Multi-cycle instruction still counting down ===
        if (idEx.latencyCyclesLeft > 0) {
            idEx.latencyCyclesLeft--;
            return out; // output a bubble — instruction not finished yet
        }

        out.isNop = false;
        out.opcode = idEx.opcode;
        out.rd = idEx.rd;
        out.writeData = b; // for SW/SB in MEM stage

        out.aluResult = computeALU(idEx.opcode, a, b, idEx.immediate, idEx.pc);

        out.branchTaken = resolveBranch(idEx.opcode, a, b, idEx.immediate, idEx.pc, out);

        if (idEx.opcode == Opcode.JAL) {
            out.aluResult = idEx.pc + 4; // return address
        }

        if (idEx.opcode == Opcode.ECALL) {
            rf.dump();
        }

        if (idEx.opcode == Opcode.HALT) {
            haltFlag = true;
        }

        return out;
    }

    // Resolve operand A with forwarding priority: EX/MEM > newMEM/WB > oldMEM/WB >
    // register file
    private int resolveOperandA(ID_EX idEx, EX_MEM prevExMem, MEM_WB newMemWb, MEM_WB oldMemWb, ForwardingUnit fu) {
        ForwardResult fa = fu.getForwardA(idEx, prevExMem, newMemWb);
        if (fa == ForwardResult.FROM_EX_MEM)
            return prevExMem.aluResult;
        if (fa == ForwardResult.FROM_MEM_WB)
            return newMemWb.result;
        // Check the retiring MEM/WB
        if (!oldMemWb.isNop && oldMemWb.rd != 0 && oldMemWb.rd == idEx.rs1)
            return oldMemWb.result;
        // No forwarding — read from register file (x0 always returns 0)
        return rf.read(idEx.rs1);
    }

    // Resolve operand B with same priority chain
    private int resolveOperandB(ID_EX idEx, EX_MEM prevExMem, MEM_WB newMemWb, MEM_WB oldMemWb, ForwardingUnit fu) {
        ForwardResult fb = fu.getForwardB(idEx, prevExMem, newMemWb);
        if (fb == ForwardResult.FROM_EX_MEM)
            return prevExMem.aluResult;
        if (fb == ForwardResult.FROM_MEM_WB)
            return newMemWb.result;
        if (!oldMemWb.isNop && oldMemWb.rd != 0 && oldMemWb.rd == idEx.rs2)
            return oldMemWb.result;
        return rf.read(idEx.rs2);
    }

    // ALU: performs the actual computation based on the opcode.
    private int computeALU(Opcode op, int a, int b, int imm, int pc) {
        // R-Type: two register operands
        if (op == Opcode.ADD)
            return a + b;
        if (op == Opcode.SUB)
            return a - b;
        if (op == Opcode.MUL)
            return a * b;
        if (op == Opcode.DIV)
            return (b != 0) ? a / b : -1; // RISC-V spec: div by zero → -1
        if (op == Opcode.SLL)
            return a << b; // shift left
        if (op == Opcode.SRL)
            return a >>> b; // shift right (unsigned)
        if (op == Opcode.XOR)
            return a ^ b;
        if (op == Opcode.OR)
            return a | b;
        if (op == Opcode.AND)
            return a & b;

        // I-Type: register + immediate
        if (op == Opcode.ADDI)
            return a + imm;
        if (op == Opcode.LI)
            return imm; // load immediate value

        // Load/Store: compute memory address = base register + offset
        if (op == Opcode.LW || op == Opcode.LB)
            return a + imm;
        if (op == Opcode.SW || op == Opcode.SB)
            return a + imm;

        return 0; // branches, ECALL, HALT don't produce ALU results
    }

    // Decides whether a branch is taken and computes the jump target.
    private boolean resolveBranch(Opcode op, int a, int b, int imm, int pc, EX_MEM out) {
        boolean taken = false;

        if (op == Opcode.BEQ)
            taken = (a == b); // branch if equal
        else if (op == Opcode.BNE)
            taken = (a != b); // branch if not equal
        else if (op == Opcode.BLT)
            taken = (a < b); // branch if less than
        else if (op == Opcode.BGE)
            taken = (a >= b); // branch if greater or equal
        else if (op == Opcode.JAL)
            taken = true; // always jump

        if (taken) {
            out.jumpTarget = pc + imm; // target = current PC + offset
        }
        return taken;
    }
}
