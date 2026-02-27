package pipeline_registers;

import common.Opcode;

// ID/EX Pipeline Register — sits between Instruction Decode and Execute.
// Contains all decoded information needed by the EX stage to execute the instruction.
public class ID_EX {
    public Opcode opcode = null;
    public int rd = 0, rs1 = 0, rs2 = 0;
    public int valA = 0, valB = 0;
    public int immediate = 0;
    public int pc = 0;
    public int latencyCyclesLeft = 0;
    public boolean isNop = true;
}
