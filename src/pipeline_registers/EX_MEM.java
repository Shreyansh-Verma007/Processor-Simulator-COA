package pipeline_registers;

import common.Opcode;

/**
 * EX/MEM Pipeline Register — sits between Execute and Memory Access.
 * Carries ALU results, branch resolution, and misprediction signals to MEM
 * stage.
 */
public class EX_MEM {

    // Instruction identity
    public Opcode opcode = null;
    public int rd = 0;

    // ALU output
    public int aluResult = 0;
    public int writeData = 0; // rs2 value for store instructions

    // Pipeline control
    public boolean isNop = true;

    // Branch resolution (set by EX stage)
    public boolean branchTaken = false;
    public int jumpTarget = 0;

    // BTFNT misprediction (set by EX stage)
    public boolean branchMispredicted = false;
    public int branchRecoveryPC = 0;
}
