package core;

// Simulated 4 KB main memory (1024 words).
public class Memory {
    private static final int SIZE_BYTES = 4096; // 4 KB total
    private static final int NUM_WORDS = SIZE_BYTES / 4; // 1024 words
    private int[] data = new int[NUM_WORDS];

    private boolean inBounds(int wordIndex) {
        return wordIndex >= 0 && wordIndex < NUM_WORDS;
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

    public void dump(int address, int count) {
        System.out.println("Memory dump starting at address " + address + ":");
        for (int i = 0; i < count; i++) {
            int addr = address + i * 4;
            int idx = addr / 4;
            if (inBounds(idx)) {
                System.out.println("  [" + addr + "] = " + data[idx]);
            }
        }
    }
}
