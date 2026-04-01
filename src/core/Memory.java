package core;

import compiler.DataItem;
import common.Instruction;
import common.InstructionEncoder;

import java.util.List;

// Simulated main memory (configurable size, default 128 KB).
public class Memory {
    private static final int DEFAULT_SIZE_BYTES = 131072; // 128 KB
    private final int sizeBytes;
    private final int numWords;
    private final int[] data;

    public Memory() {
        this(DEFAULT_SIZE_BYTES);
    }

    public Memory(int sizeBytes) {
        this.sizeBytes = sizeBytes;
        this.numWords = sizeBytes / 4;
        this.data = new int[numWords];
    }

    private boolean inBounds(int wordIndex) {
        return wordIndex >= 0 && wordIndex < numWords;
    }

    public int readWord(int address) {
        int idx = address / 4;
        if (!inBounds(idx)) {
            System.err.println("WARNING: readWord out of bounds at address " + address);
            return 0;
        }
        return data[idx];
    }

    public void writeWord(int address, int value) {
        int idx = address / 4;
        if (!inBounds(idx)) {
            System.err.println("WARNING: writeWord out of bounds at address " + address);
            return;
        }
        data[idx] = value;
    }

    public int readByte(int address) {
        int idx = address / 4;
        if (!inBounds(idx)) {
            System.err.println("WARNING: readByte out of bounds at address " + address);
            return 0;
        }
        int word = data[idx];
        int bytePosition = (address % 4) * 8;
        return (word >> bytePosition) & 0xFF;
    }

    public void writeByte(int address, int value) {
        int wordIndex = address / 4;
        if (!inBounds(wordIndex)) {
            System.err.println("WARNING: writeByte out of bounds at address " + address);
            return;
        }
        int bytePosition = (address % 4) * 8;
        data[wordIndex] = (data[wordIndex] & ~(0xFF << bytePosition))
                | ((value & 0xFF) << bytePosition);
    }

    public void preload(int address, int[] values) {
        for (int i = 0; i < values.length; i++) {
            int idx = (address / 4) + i;
            if (inBounds(idx)) {
                data[idx] = values[i];
            }
        }
    }

    /**
     * Encode a program (list of Instructions) into memory starting at the given
     * address.
     * Each instruction is encoded as a 32-bit word.
     */
    public void loadProgram(List<Instruction> program, int startAddress) {
        for (int i = 0; i < program.size(); i++) {
            int addr = startAddress + i * 4;
            int idx = addr / 4;
            if (inBounds(idx)) {
                data[idx] = InstructionEncoder.encode(program.get(i));
            }
        }
    }

    /**
     * Write compiled data items (from the .data segment) into memory.
     */
    public void loadDataItems(List<DataItem> items) {
        for (DataItem item : items) {
            for (int i = 0; i < item.bytes.length; i++) {
                writeByte(item.address + i, item.bytes[i] & 0xFF);
            }
        }
    }

    public int getSizeBytes() {
        return sizeBytes;
    }
}
