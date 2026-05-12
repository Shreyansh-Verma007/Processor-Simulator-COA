package trace;

/**
 * Represents a single instruction parsed from a trace file.
 * Trace format:
 *   L  0xADDR  xRD        — load word from virtual address into register
 *   S  0xADDR  xRS        — store word from register to virtual address
 *   ADD xRD xRS1 xRS2     — rd = rs1 + rs2
 *   MUL xRD xRS1 xRS2     — rd = rs1 * rs2
 */
public class TraceInstruction {

    public enum Type { LOAD, STORE, ADD, MUL }

    public final Type type;
    public final int address;   // virtual address for L/S (0 for ALU ops)
    public final int rd;        // destination register (L, ADD, MUL)
    public final int rs1;       // source register 1 (S uses this as source, ADD/MUL use both)
    public final int rs2;       // source register 2 (ADD, MUL only)

    private TraceInstruction(Type type, int address, int rd, int rs1, int rs2) {
        this.type = type;
        this.address = address;
        this.rd = rd;
        this.rs1 = rs1;
        this.rs2 = rs2;
    }

    /** Create a LOAD instruction: L 0xADDR xRD */
    public static TraceInstruction load(int address, int rd) {
        return new TraceInstruction(Type.LOAD, address, rd, 0, 0);
    }

    /** Create a STORE instruction: S 0xADDR xRS */
    public static TraceInstruction store(int address, int rs) {
        return new TraceInstruction(Type.STORE, address, 0, rs, 0);
    }

    /** Create an ADD instruction: ADD xRD xRS1 xRS2 */
    public static TraceInstruction add(int rd, int rs1, int rs2) {
        return new TraceInstruction(Type.ADD, 0, rd, rs1, rs2);
    }

    /** Create a MUL instruction: MUL xRD xRS1 xRS2 */
    public static TraceInstruction mul(int rd, int rs1, int rs2) {
        return new TraceInstruction(Type.MUL, 0, rd, rs1, rs2);
    }

    @Override
    public String toString() {
        switch (type) {
            case LOAD:
                return "L 0x" + Integer.toHexString(address) + " x" + rd;
            case STORE:
                return "S 0x" + Integer.toHexString(address) + " x" + rs1;
            case ADD:
                return "ADD x" + rd + " x" + rs1 + " x" + rs2;
            case MUL:
                return "MUL x" + rd + " x" + rs1 + " x" + rs2;
            default:
                return "UNKNOWN";
        }
    }
}
