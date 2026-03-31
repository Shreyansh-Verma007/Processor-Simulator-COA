package pipeline_registers;

import common.Opcode;

/**
 * ID/EX Pipeline Register — sits between Instruction Decode and Execute.
 * Carries decoded instruction fields and BTFNT prediction data to the EX stage.
 */
public class ID_EX {

    // Decoded instruction fields
    public Opcode opcode = null;
    public int rd = 0;
    public int rs1 = 0;
    public int rs2 = 0;

    public int immediate = 0;
    public int pc = 0;

    // Multi-cycle execution support
    public int latencyCyclesLeft = 0;

    // Pipeline control
    public boolean isNop = true;

    // BTFNT branch prediction (set by ID stage)
    public boolean branchPredictedTaken = false;
    public int predictedPC = -1; // -1 = no prediction redirect
}
