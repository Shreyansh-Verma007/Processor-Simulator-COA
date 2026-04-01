package compiler;

/**
 * A contiguous block of bytes to be written into memory at a given address.
 * Produced by the .data segment directives (.word, .byte, .space, .ascii,
 * .asciiz).
 */
public class DataItem {
    public final int address;
    public final byte[] bytes;

    public DataItem(int address, byte[] bytes) {
        this.address = address;
        this.bytes = bytes;
    }
}
