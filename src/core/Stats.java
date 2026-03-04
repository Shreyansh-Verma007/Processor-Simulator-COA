package core;

// Simulation performance metrics.
public class Stats {
    public int cycles = 0;
    public int stalls = 0;
    public int branchFlushes = 0;
    public int instructionsRetired = 0;

    public double getIPC() {
        return cycles == 0 ? 0 : (double) instructionsRetired / cycles;
    }

    public void printSummary() {
        System.out.println("\n=== Simulation Complete ===");
        System.out.println("Cycles             : " + cycles);
        System.out.println("Stalls             : " + stalls);
        System.out.println("Branch Flushes     : " + branchFlushes);
        System.out.println("Instructions Retired: " + instructionsRetired);
        System.out.printf("IPC                : %.3f%n", getIPC());
    }
}
