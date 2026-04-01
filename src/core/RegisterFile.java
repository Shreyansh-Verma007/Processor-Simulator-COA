package core;

// 32 integer registers. x0 is hardwired to 0.
public class RegisterFile {
    private final int[] regs = new int[32];

    public RegisterFile() {
        regs[2] = 0x0FFF; // Initialize SP at top of memory
    }

    public int read(int r) {
        return (r == 0) ? 0 : regs[r];
    }

    public void write(int r, int val) {
        if (r != 0)
            regs[r] = val;
    }

    public void dump() {
        System.out.println("Register dump (x0-x31):");
        for (int i = 0; i < 32; i++) {
            System.out.printf("  x%-2d = %d%n", i, (i == 0) ? 0 : regs[i]);
        }
    }
}
