package compiler;

import common.Instruction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Two-pass compiler for RISC-V assembly with .data / .text section support.
 *
 * Memory layout:
 * 0x00000 — text segment (instructions, 4 bytes each)
 * 0x10000 — data segment (.word, .byte, .space, .ascii, .asciiz)
 */
public class Compiler {

    static final int TEXT_BASE = 0x00000;
    static final int DATA_BASE = 0x0400; // 1 KB offset

    public CompilationResult compile(String path) throws IOException {
        Lexer lexer = new Lexer();
        ArrayList<String> lines = lexer.tokenize(path);

        // Pass 1 — build full symbol table (both text and data labels)
        Map<String, Integer> symbols = buildSymbolTable(lines);

        // Pass 2 — emit instructions (.text) and data items (.data)
        Parser parser = new Parser(symbols);
        ArrayList<Instruction> instructions = parser.parseText(lines);
        List<DataItem> dataItems = parseData(lines, symbols);

        return new CompilationResult(instructions, dataItems);
    }

    // ── Pass 1: symbol table ─────────────────────────────────────────────

    private Map<String, Integer> buildSymbolTable(ArrayList<String> lines) {
        Map<String, Integer> symbols = new HashMap<>();
        boolean inData = false;
        int textAddr = TEXT_BASE;
        int dataAddr = DATA_BASE;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty())
                continue;

            // Section directives
            if (line.equalsIgnoreCase(".data")) {
                inData = true;
                continue;
            }
            if (line.equalsIgnoreCase(".text")) {
                inData = false;
                continue;
            }

            // Extract label if present
            String rest = line;
            if (line.contains(":")) {
                int colon = line.indexOf(':');
                String label = line.substring(0, colon).trim();
                rest = line.substring(colon + 1).trim();
                if (inData) {
                    symbols.put(label, dataAddr);
                } else {
                    symbols.put(label, textAddr);
                }
            }

            if (rest.isEmpty())
                continue;

            if (inData) {
                // Advance dataAddr by the size of this directive
                dataAddr += dataDirectiveSize(rest);
            } else {
                // Skip directives inside .text (shouldn't be many)
                if (!rest.startsWith(".")) {
                    textAddr += 4;
                }
            }
        }
        return symbols;
    }

    /**
     * Compute how many bytes a data directive occupies.
     */
    private int dataDirectiveSize(String rest) {
        String[] parts = rest.split("\\s+", 2);
        String directive = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1].trim() : "";

        switch (directive) {
            case ".word":
                return 4 * countArgs(args);
            case ".half":
                return 2 * countArgs(args);
            case ".byte":
                return countArgs(args);
            case ".space":
            case ".zero":
                try {
                    return Integer.parseInt(args.trim());
                } catch (Exception e) {
                    return 0;
                }
            case ".ascii":
                return parseStringLiteral(args, false).length;
            case ".asciiz":
            case ".string":
                return parseStringLiteral(args, true).length;
            case ".align":
            case ".globl":
            case ".global":
                return 0;
            default:
                return 0;
        }
    }

    private int countArgs(String args) {
        if (args == null || args.trim().isEmpty())
            return 0;
        return args.split(",").length;
    }

    // ── Pass 2: data items ────────────────────────────────────────────────

    private List<DataItem> parseData(ArrayList<String> lines, Map<String, Integer> symbols) {
        List<DataItem> items = new ArrayList<>();
        boolean inData = false;
        int dataAddr = DATA_BASE;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty())
                continue;

            if (line.equalsIgnoreCase(".data")) {
                inData = true;
                continue;
            }
            if (line.equalsIgnoreCase(".text")) {
                inData = false;
                continue;
            }

            if (!inData)
                continue;

            // Strip label
            String rest = line;
            if (line.contains(":")) {
                rest = line.substring(line.indexOf(':') + 1).trim();
            }
            if (rest.isEmpty())
                continue;

            String[] parts = rest.split("\\s+", 2);
            String directive = parts[0].toLowerCase();
            String args = parts.length > 1 ? parts[1].trim() : "";

            byte[] bytes = emitDataDirective(directive, args, symbols);
            if (bytes != null && bytes.length > 0) {
                items.add(new DataItem(dataAddr, bytes));
                dataAddr += bytes.length;
            }
        }
        return items;
    }

    private byte[] emitDataDirective(String directive, String args, Map<String, Integer> symbols) {
        switch (directive) {
            case ".word": {
                String[] vals = args.split(",");
                byte[] out = new byte[4 * vals.length];
                for (int i = 0; i < vals.length; i++) {
                    int v = resolveInt(vals[i].trim(), symbols);
                    // little-endian
                    out[i * 4] = (byte) (v);
                    out[i * 4 + 1] = (byte) (v >> 8);
                    out[i * 4 + 2] = (byte) (v >> 16);
                    out[i * 4 + 3] = (byte) (v >> 24);
                }
                return out;
            }
            case ".half": {
                String[] vals = args.split(",");
                byte[] out = new byte[2 * vals.length];
                for (int i = 0; i < vals.length; i++) {
                    int v = resolveInt(vals[i].trim(), symbols);
                    out[i * 2] = (byte) (v);
                    out[i * 2 + 1] = (byte) (v >> 8);
                }
                return out;
            }
            case ".byte": {
                String[] vals = args.split(",");
                byte[] out = new byte[vals.length];
                for (int i = 0; i < vals.length; i++) {
                    out[i] = (byte) resolveInt(vals[i].trim(), symbols);
                }
                return out;
            }
            case ".space":
            case ".zero": {
                try {
                    int n = Integer.parseInt(args.trim());
                    return new byte[n]; // zero-initialized
                } catch (Exception e) {
                    return new byte[0];
                }
            }
            case ".ascii":
                return parseStringLiteral(args, false);
            case ".asciiz":
            case ".string":
                return parseStringLiteral(args, true);
            case ".align":
            case ".globl":
            case ".global":
                return new byte[0];
            default:
                return new byte[0];
        }
    }

    private int resolveInt(String s, Map<String, Integer> symbols) {
        s = s.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return Integer.parseUnsignedInt(s.substring(2), 16);
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Integer v = symbols.get(s);
            if (v == null)
                throw new RuntimeException("Undefined symbol in .data: " + s);
            return v;
        }
    }

    /**
     * Parse a quoted string literal into bytes.
     * Handles escapes: \n \t \r \\ \" \0
     */
    private byte[] parseStringLiteral(String args, boolean nullTerminate) {
        args = args.trim();
        if (args.startsWith("\""))
            args = args.substring(1);
        if (args.endsWith("\""))
            args = args.substring(0, args.length() - 1);

        List<Byte> bytes = new ArrayList<>();
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (c == '\\' && i + 1 < args.length()) {
                char next = args.charAt(++i);
                switch (next) {
                    case 'n':
                        bytes.add((byte) '\n');
                        break;
                    case 't':
                        bytes.add((byte) '\t');
                        break;
                    case 'r':
                        bytes.add((byte) '\r');
                        break;
                    case '\\':
                        bytes.add((byte) '\\');
                        break;
                    case '"':
                        bytes.add((byte) '"');
                        break;
                    case '0':
                        bytes.add((byte) 0);
                        break;
                    default:
                        bytes.add((byte) next);
                }
            } else {
                bytes.add((byte) c);
            }
        }
        if (nullTerminate)
            bytes.add((byte) 0);

        byte[] result = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++)
            result[i] = bytes.get(i);
        return result;
    }
}
