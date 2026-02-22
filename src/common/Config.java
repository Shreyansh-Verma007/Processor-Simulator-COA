package common;

import java.util.HashMap;
import java.util.Map;

// Configures how many clock cycles each instruction takes to execute.
// Most instructions take 1 cycle. MUL takes 3, DIV takes 4.
// This is used by the pipeline to simulate multi-cycle stalls.
public class Config {
    private Map<Opcode, Integer> latencies;
    private boolean forwardingEnabled;

    public Config() {
        this.latencies = new HashMap<>();
        this.forwardingEnabled = true;

        // R-Type: most are 1 cycle, MUL and DIV are multi-cycle
        latencies.put(Opcode.ADD, 1);
        latencies.put(Opcode.SUB, 1);
        latencies.put(Opcode.MUL, 3); // multiplication takes 3 cycles
        latencies.put(Opcode.DIV, 4); // division takes 4 cycles
        latencies.put(Opcode.SLL, 1);
        latencies.put(Opcode.SRL, 1);
        latencies.put(Opcode.XOR, 1);
        latencies.put(Opcode.OR, 1);
        latencies.put(Opcode.AND, 1);

        // I-Type
        latencies.put(Opcode.ADDI, 1);
        latencies.put(Opcode.LW, 1);
        latencies.put(Opcode.LB, 1);
        latencies.put(Opcode.LI, 1);

        // S-Type
        latencies.put(Opcode.SW, 1);
        latencies.put(Opcode.SB, 1);

        // B-Type
        latencies.put(Opcode.BEQ, 1);
        latencies.put(Opcode.BNE, 1);
        latencies.put(Opcode.BLT, 1);
        latencies.put(Opcode.BGE, 1);

        // J-Type
        latencies.put(Opcode.JAL, 1);

        // U-Type
        latencies.put(Opcode.ECALL, 1);
        latencies.put(Opcode.HALT, 1);
    }

    // Returns how many cycles an instruction takes. Defaults to 1 if not found.
    public int getLatency(Opcode op) {
        Integer lat = latencies.get(op);
        return lat == null ? 1 : lat;
    }

    // Whether data forwarding is enabled (bypasses hazards without stalling)
    public boolean isForwardingEnabled() {
        return forwardingEnabled;
    }

    public void setForwardingEnabled(boolean enabled) {
        this.forwardingEnabled = enabled;
    }
}
