package trace;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a trace file into a list of TraceInstructions.
 *
 * Expected format (one instruction per line):
 *   L 0x1000 x5
 *   S 0x1004 x6
 *   ADD x7 x5 x6
 *   MUL x8 x7 x9
 *
 * Lines starting with '#' or empty lines are ignored.
 */
public class TraceParser {

    /**
     * Parse an entire trace file and return the list of instructions.
     */
    public static List<TraceInstruction> parse(String path) throws IOException {
        List<TraceInstruction> instructions = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }
                instructions.add(parseLine(line, lineNum));
            }
        }

        if (instructions.isEmpty()) {
            System.err.println("WARNING: Trace file '" + path + "' contains no instructions.");
        }

        return instructions;
    }

    private static TraceInstruction parseLine(String line, int lineNum) {
        String[] parts = line.split("\\s+");
        String op = parts[0].toUpperCase();

        try {
            switch (op) {
                case "L":
                    // L 0xADDR xRD
                    int loadAddr = parseAddress(parts[1]);
                    int loadRd = parseRegister(parts[2]);
                    return TraceInstruction.load(loadAddr, loadRd);

                case "S":
                    // S 0xADDR xRS
                    int storeAddr = parseAddress(parts[1]);
                    int storeRs = parseRegister(parts[2]);
                    return TraceInstruction.store(storeAddr, storeRs);

                case "ADD":
                    // ADD xRD xRS1 xRS2
                    int addRd = parseRegister(parts[1]);
                    int addRs1 = parseRegister(parts[2]);
                    int addRs2 = parseRegister(parts[3]);
                    return TraceInstruction.add(addRd, addRs1, addRs2);

                case "MUL":
                    // MUL xRD xRS1 xRS2
                    int mulRd = parseRegister(parts[1]);
                    int mulRs1 = parseRegister(parts[2]);
                    int mulRs2 = parseRegister(parts[3]);
                    return TraceInstruction.mul(mulRd, mulRs1, mulRs2);

                case "BEQ":
                case "BNE":
                    // BEQ xRS1 xRS2 0xADDR
                    int bRs1 = parseRegister(parts[1]);
                    int bRs2 = parseRegister(parts[2]);
                    int bAddr = parseAddress(parts[3]);
                    return TraceInstruction.branch(bRs1, bRs2, bAddr);

                case "JAL":
                    // JAL xRD 0xADDR
                    int jRd = parseRegister(parts[1]);
                    int jAddr = parseAddress(parts[2]);
                    return TraceInstruction.jump(jRd, jAddr);

                default:
                    throw new IllegalArgumentException(
                            "Unknown instruction '" + op + "' at line " + lineNum);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(
                    "Malformed trace line " + lineNum + ": " + line);
        }
    }

    /**
     * Parse a hex address string like "0x1000" or "0x1ABC".
     * Also supports decimal if no "0x" prefix.
     */
    private static int parseAddress(String s) {
        s = s.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return Integer.parseUnsignedInt(s.substring(2), 16);
        }
        return Integer.parseInt(s);
    }

    /**
     * Parse a register string like "x5" and return the register number.
     */
    private static int parseRegister(String s) {
        s = s.trim().toLowerCase();
        if (s.startsWith("x")) {
            int n = Integer.parseInt(s.substring(1));
            if (n < 0 || n > 31) {
                throw new IllegalArgumentException("Register out of range (x0-x31): " + s);
            }
            return n;
        }
        throw new IllegalArgumentException("Invalid register: " + s);
    }
}
