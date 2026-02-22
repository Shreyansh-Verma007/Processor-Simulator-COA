package compiler;

import common.Instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CompilationResult {
    ArrayList<Instruction> instructions;
    Map<String, Integer> symbolMap;

    public CompilationResult(ArrayList<Instruction> instructions, Map<String, Integer> symbolMap) {
        this.instructions = instructions;
        this.symbolMap = symbolMap == null ? new HashMap<>() : symbolMap;
    }

    public ArrayList<Instruction> getInstructions() {
        return instructions;
    }

    public Map<String, Integer> getSymbolMap() {
        return symbolMap;
    }
}
