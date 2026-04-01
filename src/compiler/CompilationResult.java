package compiler;

import common.Instruction;

import java.util.ArrayList;
import java.util.List;

public class CompilationResult {
    private final ArrayList<Instruction> instructions;
    private final List<DataItem> dataItems;

    public CompilationResult(ArrayList<Instruction> instructions, List<DataItem> dataItems) {
        this.instructions = instructions;
        this.dataItems = dataItems;
    }

    public ArrayList<Instruction> getInstructions() {
        return instructions;
    }

    /** Data items to be written into memory before simulation starts. */
    public List<DataItem> getDataItems() {
        return dataItems;
    }
}
