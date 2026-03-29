package compiler;

import common.Instruction;

import java.util.ArrayList;

public class CompilationResult {
    private final ArrayList<Instruction> instructions;

    public CompilationResult(ArrayList<Instruction> instructions) {
        this.instructions = instructions;
    }

    public ArrayList<Instruction> getInstructions() {
        return instructions;
    }
}
