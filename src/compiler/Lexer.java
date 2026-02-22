package compiler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Lexer {
    ArrayList<String> tokenize(String filePath) throws IOException {
        ArrayList<String> tokens = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//"))
                continue;
            if (line.contains("#")) {
                line = line.substring(0, line.indexOf("#")).trim();
            }
            if (line.contains("//")) {
                line = line.substring(0, line.indexOf("//")).trim();
            }
            if (line.isEmpty())
                continue;
            tokens.add(line);
        }
        reader.close();
        return tokens;
    }
}
