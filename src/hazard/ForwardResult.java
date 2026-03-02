package hazard;

// Indicates where a forwarded operand value should come from.
// Used by ForwardingUnit to tell EX_Stage which value to use.
public enum ForwardResult {
    NONE, // no forwarding needed — read from register file
    FROM_EX_MEM, // forward from EX/MEM register (1 cycle old)
    FROM_MEM_WB // forward from MEM/WB register (2 cycles old)
}
