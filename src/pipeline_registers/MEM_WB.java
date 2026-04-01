package pipeline_registers;

import common.Opcode;

// MEM/WB Pipeline Register — sits between Memory Access and Write Back.
// Carries the final result to be written into the destination register.
public class MEM_WB {
    public Opcode opcode = null;
    public int rd = 0;
    public int result = 0;
    public boolean isNop = true;
    public int memLatencyLeft = 0; // remaining cycles for cache miss stall
}
