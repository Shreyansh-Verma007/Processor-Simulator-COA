package pipeline_stages;

import cache.AccessResult;
import cache.CacheHierarchy;
import common.Opcode;
import core.Memory;
import pipeline_registers.EX_MEM;
import pipeline_registers.MEM_WB;

// Memory Access (MEM) — routes through cache hierarchy when available.
public class MEM_Stage {

    public MEM_WB tick(EX_MEM exMem, Memory mem, CacheHierarchy cache) {
        MEM_WB out = new MEM_WB();
        if (exMem.isNop)
            return out;

        out.isNop = false;
        out.opcode = exMem.opcode;
        out.rd = exMem.rd;

        Opcode op = exMem.opcode;

        if (cache != null) {
            // Access through cache hierarchy
            if (op == Opcode.LW) {
                AccessResult r = cache.readData(exMem.aluResult);
                out.result = r.data;
                out.memLatencyLeft = r.latencyCycles - 1;
            } else if (op == Opcode.LB) {
                AccessResult r = cache.readDataByte(exMem.aluResult);
                out.result = r.data;
                out.memLatencyLeft = r.latencyCycles - 1;
            } else if (op == Opcode.SW) {
                AccessResult r = cache.writeData(exMem.aluResult, exMem.writeData);
                out.memLatencyLeft = r.latencyCycles - 1;
            } else if (op == Opcode.SB) {
                AccessResult r = cache.writeDataByte(exMem.aluResult, exMem.writeData);
                out.memLatencyLeft = r.latencyCycles - 1;
            } else {
                out.result = exMem.aluResult;
            }
        } else {
            // Direct memory access (Phase 1 backward compatibility)
            if (op == Opcode.LW) {
                out.result = mem.readWord(exMem.aluResult);
            } else if (op == Opcode.LB) {
                out.result = mem.readByte(exMem.aluResult);
            } else if (op == Opcode.SW) {
                mem.writeWord(exMem.aluResult, exMem.writeData);
            } else if (op == Opcode.SB) {
                mem.writeByte(exMem.aluResult, exMem.writeData);
            } else {
                out.result = exMem.aluResult;
            }
        }
        return out;
    }
}
