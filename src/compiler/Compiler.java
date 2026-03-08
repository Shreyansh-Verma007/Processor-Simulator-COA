package compiler;

import common.Instruction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Compiler {
    public CompilationResult compile(String path) throws IOException {
        Lexer lexer = new Lexer();
        ArrayList<String> tokens = lexer.tokenize(path);
        Map<String, Integer> symbols = buildSymbolTable(tokens);
        Parser parser = new Parser(symbols);
        ArrayList<Instruction> program = parser.parse(tokens);
        return new CompilationResult(program);
    }

    private Map<String, Integer> buildSymbolTable(ArrayList<String> tokens) {
        Map<String, Integer> symbols = new HashMap<>();
        int instrIndex = 0;

        for (String token : tokens) {
            String t = token.trim();
            if (t.isEmpty())
                continue;

            if (t.endsWith(":")) {
                String label = t.substring(0, t.length() - 1).trim();
                symbols.put(label, instrIndex * 4);
            } else if (t.contains(":")) {
                int idx = t.indexOf(':');
                String label = t.substring(0, idx).trim();
                symbols.put(label, instrIndex * 4);
                instrIndex++;
            } else {
                instrIndex++;
            }
        }

        return symbols;
    }
}
