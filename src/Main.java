import compiler.CompilationResult;
import compiler.Compiler;
import core.Processor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

// Entry point for the RISC-V pipeline simulator.
// Loads assembly from input.asm, runs simulation, and saves results.
public class Main {
    public static void main(String[] args) throws Exception {
        String path = (args.length > 0) ? args[0] : "input.asm";

        System.out.println("=== RISC-V Pipeline Simulator ===");
        System.out.println("Loading: " + path);

        // Compile assembly
        CompilationResult result = new Compiler().compile(path);
        System.out.println("Compiled " + result.getInstructions().size() + " instructions.\n");

        // Set up output capture for both console and log file
        PrintStream originalOut = System.out;
        Processor processor = new Processor();
        try (FileOutputStream fileOut = new FileOutputStream("console.txt")) {
            System.setOut(new PrintStream(new OutputStream() {
                public void write(int b) throws IOException {
                    originalOut.write(b);
                    fileOut.write(b);
                }

                public void write(byte[] b, int off, int len) throws IOException {
                    originalOut.write(b, off, len);
                    fileOut.write(b, off, len);
                }

                    public void flush() throws IOException {
                    originalOut.flush();
                    fileOut.flush();
                }
            }));

            // Main simulation loop
            processor.run(result.getInstructions());

            System.setOut(originalOut);
        }

        processor.getStats().printSummary();

        // Save stats to file
        try (PrintStream statsOut = new PrintStream(new FileOutputStream("output.txt"))) {
            statsOut.println("=== Simulation Stats ===");
            statsOut.println("Cycles             : " + processor.getStats().cycles);
            statsOut.println("Stalls             : " + processor.getStats().stalls);
            statsOut.println("Branch Flushes     : " + processor.getStats().branchFlushes);
            statsOut.println("Instructions Retired: " + processor.getStats().instructionsRetired);
            statsOut.printf("IPC                : %.3f%n", processor.getStats().getIPC());
        }

        System.out.println("\n✔ Results saved to console.txt and output.txt");

        // Dump the processor's actual memory (where SW instructions wrote data)
        //processor.dumpMemory(0, 4095);
    }
}
