# Study Report — `Memory.java`

> **File:** `src/core/Memory.java`
> **Module:** Simulated main memory for the RISC-V pipeline simulator
> **Date Updated:** 2026-04-19 *(originally 2026-04-09)*

---

## Table of Contents

1. [Overview](#overview)
2. [Package & Imports](#package--imports)
3. [Class Fields & Constructors](#class-fields--constructors)
4. [Helper — `inBounds()`](#helper--inbounds)
5. [Method — `readWord()`](#readword)
6. [Method — `writeWord()`](#writeword)
7. [Method — `readByte()`](#readbyte)
8. [Method — `writeByte()`](#writebyte)
9. [Method — `loadProgram()`](#loadprogram)
10. [Method — `loadDataItems()`](#loaddataitems)
11. [Summary Table](#summary-table)
12. [Memory Layout Diagram](#memory-layout-diagram)

---

## Overview

`Memory` simulates **main memory** used as the backing store for the entire pipeline simulator. In Phase 2, it serves triple duty:

1. **Data memory** — stores program data accessed via `LW`/`SW`/`LB`/`SB`.
2. **Instruction store** — the encoded binary program is loaded here so the cache hierarchy can fetch instructions as raw 32-bit words.
3. **Cache backing store** — `CacheHierarchy` reads and writes through to `Memory` on L2 misses and dirty evictions.

**Phase 2 change:** The old `preload()` method has been split into `loadProgram()` (for instruction binary encoding) and `loadDataItems()` (for `.data` segment initialisation from `CompilationResult`). The `dump()` method has been removed. Memory is now **128 KB** by default (was 4 KB in Phase 1).

---

## Package & Imports

```java
package core;

import common.Instruction;
import common.InstructionEncoder;
import compiler.DataItem;
import java.util.List;
```

| Import | Purpose |
|--------|---------|
| `Instruction` | Instruction objects passed to `loadProgram()`. |
| `InstructionEncoder` | Encodes each `Instruction` to a 32-bit integer for storage. |
| `DataItem` | Holds a byte-level memory initialisation record from the compiler's `.data` segment. |

---

## Class Fields & Constructors

```java
// Simulated main memory (configurable size, default 128 KB).
public class Memory {
    private static final int DEFAULT_SIZE_BYTES = 131072;  // 128 KB
    private final int numWords;
    private final int[] data;
```

| Field | Description |
|-------|-------------|
| `DEFAULT_SIZE_BYTES` | `131072` bytes = 128 KB |
| `numWords` | `sizeBytes / 4` — total number of 32-bit words |
| `data` | The backing integer array; `int[numWords]`, Java-initialised to 0 |

### Constructors

```java
public Memory() {
    this(DEFAULT_SIZE_BYTES);   // 128 KB
}

public Memory(int sizeBytes) {
    this.numWords = sizeBytes / 4;
    this.data = new int[numWords];
}
```

The default constructor creates a 128 KB memory. A parameterised constructor allows smaller memories for testing.

---

## Helper — `inBounds()`

```java
private boolean inBounds(int wordIndex) {
    return wordIndex >= 0 && wordIndex < numWords;
}
```

Bounds check used by all read/write methods. Prevents `ArrayIndexOutOfBoundsException`. Compares based on **word index**, not byte address.

---

## `readWord()`

```java
public int readWord(int address) {
    int idx = address / 4;
    if (!inBounds(idx)) {
        System.err.println("WARNING: readWord out of bounds at address " + address);
        return 0;
    }
    return data[idx];
}
```

Reads a 32-bit word. Used by MEM stage (LW) and by `CacheHierarchy` during block fills.

---

## `writeWord()`

```java
public void writeWord(int address, int value) {
    int idx = address / 4;
    if (!inBounds(idx)) {
        System.err.println("WARNING: writeWord out of bounds at address " + address);
        return;
    }
    data[idx] = value;
}
```

Writes a 32-bit word. Used by MEM stage (SW) and by `CacheHierarchy` during dirty write-backs.

---

## `readByte()`

```java
public int readByte(int address) {
    int idx = address / 4;
    ...
    int word = data[idx];
    int bytePosition = (address % 4) * 8;
    return (word >> bytePosition) & 0xFF;
}
```

Reads one byte using **little-endian** extraction. `address % 4` → byte offset (0–3); `× 8` → bit position. The `& 0xFF` mask zero-extends the result to 32 bits.

---

## `writeByte()`

```java
public void writeByte(int address, int value) {
    ...
    int bytePosition = (address % 4) * 8;
    data[wordIndex] = (data[wordIndex] & ~(0xFF << bytePosition))
            | ((value & 0xFF) << bytePosition);
}
```

Single-byte **read-modify-write**: clears the target byte, then ORs in the new value. The other 3 bytes in the word are preserved.

Used by MEM stage (SB) and `loadDataItems()`.

---

## `loadProgram()`

```java
public void loadProgram(List<Instruction> program, int startAddress) {
    for (int i = 0; i < program.size(); i++) {
        int addr = startAddress + i * 4;
        int idx = addr / 4;
        if (inBounds(idx)) {
            data[idx] = InstructionEncoder.encode(program.get(i));
        }
    }
}
```

**Purpose:** Encodes each `Instruction` object as a 32-bit integer and writes it into the `data[]` array. Called by `PipelineController` at the start of simulation (Phase 2 only) so that the instruction cache (L1I) can fetch binary instruction words.

`InstructionEncoder.encode()` converts the instruction fields (opcode, rd, rs1, rs2, immediate) into a compact 32-bit representation. The reverse `InstructionEncoder.decode()` is used by `IF_Stage` when retrieving instructions from the cache.

---

## `loadDataItems()`

```java
public void loadDataItems(List<DataItem> items) {
    for (DataItem item : items) {
        for (int i = 0; i < item.bytes.length; i++) {
            writeByte(item.address + i, item.bytes[i] & 0xFF);
        }
    }
}
```

**Purpose:** Initialises the `.data` segment in memory from compiler output. Each `DataItem` has a target byte address and a byte array. Called by `Processor.run(CompilationResult)` before the pipeline starts.

`item.bytes[i] & 0xFF` ensures unsigned byte handling before the `writeByte` call.

---

## Summary Table

| Method | Visibility | Return | Purpose |
|--------|-----------|--------|---------|
| `Memory()` | `public` | — | Default 128 KB memory |
| `Memory(int sizeBytes)` | `public` | — | Custom-size memory |
| `inBounds(wordIndex)` | `private` | `boolean` | Bounds check |
| `readWord(address)` | `public` | `int` | Read 32-bit word |
| `writeWord(address, value)` | `public` | `void` | Write 32-bit word |
| `readByte(address)` | `public` | `int` | Read one byte (little-endian) |
| `writeByte(address, value)` | `public` | `void` | Write one byte (read-modify-write) |
| `loadProgram(program, start)` | `public` | `void` | Encode + store instruction binary for cache fetch |
| `loadDataItems(items)` | `public` | `void` | Write `.data` segment bytes from compiler output |

---

## Memory Layout Diagram

```
Address (byte)    Word Index     Contents
  0x00000            0          Instruction 0 (encoded)   ← loadProgram() writes here
  0x00004            1          Instruction 1 (encoded)
    ...
  0x????             ?          .data items               ← loadDataItems() writes here
    ...
  0x1FFFC         32767         data[32767]  ← Last valid word (128 KB)
  0x20000         32768         ❌ OUT OF BOUNDS

Total: 131072 bytes = 32768 words

Each word (little-endian layout):
  ┌────────┬────────┬────────┬────────┐
  │ Byte 3 │ Byte 2 │ Byte 1 │ Byte 0 │
  │ [31:24]│ [23:16]│ [15:8] │ [7:0]  │
  └────────┴────────┴────────┴────────┘
```

---

*End of Report*
