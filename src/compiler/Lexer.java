package compiler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Lexer {
    ArrayList<String> tokenize(String filePath) throws IOException {
        ArrayList<String> tokens = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//"))
                    continue;
                line = stripComment(line);
                if (line.isEmpty())
                    continue;
                tokens.add(line);
            }
        }
        return tokens;
    }

    /** Strip comments (# or //) while respecting quoted strings. */
    private String stripComment(String line) {
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (!inQuote) {
                if (c == '#') {
                    return line.substring(0, i).trim();
                }
                if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                    return line.substring(0, i).trim();
                }
            }
        }
        return line.trim();
    }
}
