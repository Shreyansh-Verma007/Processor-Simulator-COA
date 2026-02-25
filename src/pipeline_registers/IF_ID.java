package pipeline_registers;

import common.Instruction;

// IF/ID Pipeline Register — sits between Instruction Fetch and Instruction Decode.
// Holds the fetched instruction and its PC address.
public class IF_ID {
    public Instruction instruction = null;
    public int pc = 0;
    public boolean isNop = true;
}
