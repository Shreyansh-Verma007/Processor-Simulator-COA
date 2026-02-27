package pipeline_registers;

import common.Opcode;

// EX/MEM Pipeline Register — sits between Execute and Memory Access.
// Carries the ALU result and control signals to the MEM stage.
public class EX_MEM {
    public Opcode opcode = null;
    public int rd = 0;
    public int aluResult = 0;
    public int writeData = 0;
    public boolean isNop = true;
    public boolean branchTaken = false;
    public int jumpTarget = 0;
}
