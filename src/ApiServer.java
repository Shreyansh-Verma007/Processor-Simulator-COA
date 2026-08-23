import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight HTTP API server for the RISC-V Pipeline Simulator.
 * Uses Java's built-in HttpServer — zero external dependencies.
 *
 * Start with: java Main --server
 *
 * Endpoints:
 *   GET  /api/status   → { "status": "idle"|"running" }
 *   GET  /api/asm      → { "content": "<input.asm text>" }
 *   POST /api/asm      → Save body to input.asm
 *   POST /api/run      → Run pipeline simulation, returns JSON result
 *   GET  /api/console  → { "content": "<console.txt text>" }
 *   GET  /api/output   → { "content": "<output.txt text>" }
 *   GET  /api/swap     → { "content": "<swap.txt text>" }
 *   POST /api/trace    → Upload .trace file bytes, run trace replay, return stats
 *   GET  /api/traces   → List available .trace files in phase3_traces/
 */
public class ApiServer {

    // Heroku sets PORT dynamically; fall back to 8080 for local dev
    private static final int PORT = System.getenv("PORT") != null
            ? Integer.parseInt(System.getenv("PORT"))
            : 8080;
    private static final AtomicBoolean running = new AtomicBoolean(false);

    public static void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/asm", new AsmHandler());
        server.createContext("/api/run", new RunHandler());
        server.createContext("/api/console", new FileHandler("console.txt"));
        server.createContext("/api/output", new FileHandler("output.txt"));
        server.createContext("/api/swap", new FileHandler("swap.txt"));
        server.createContext("/api/trace", new TraceHandler());
        server.createContext("/api/traces", new TracesListHandler());
        server.createContext("/api/trace-file", new TraceFileHandler());

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.err.println("=== RISC-V Simulator API Server ===");
        System.err.println("Listening on http://localhost:" + PORT);
        System.err.println("Press Ctrl+C to stop.");
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    private static void addCors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        addCors(ex);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n")
                .replace("\t", "\\t");
    }

    private static String readFile(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                addCors(ex);
                ex.sendResponseHeaders(204, -1);
                return;
            }
            String status = running.get() ? "running" : "idle";
            sendJson(ex, 200, "{\"status\":\"" + status + "\"}");
        }
    }

    static class AsmHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            if ("GET".equals(ex.getRequestMethod())) {
                String content = readFile("input.asm");
                sendJson(ex, 200, "{\"content\":\"" + escapeJson(content) + "\"}");
            } else if ("POST".equals(ex.getRequestMethod())) {
                String body = readBody(ex);
                try {
                    Files.write(Paths.get("input.asm"), body.getBytes(StandardCharsets.UTF_8));
                    sendJson(ex, 200, "{\"ok\":true}");
                } catch (IOException e) {
                    sendJson(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else {
                sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    static class RunHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equals(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"POST required\"}");
                return;
            }

            if (!running.compareAndSet(false, true)) {
                sendJson(ex, 409, "{\"error\":\"Simulation already running\"}");
                return;
            }

            // Read assembly code from POST body
            String asmCode = readBody(ex);
            if (asmCode == null || asmCode.isBlank()) {
                running.set(false);
                sendJson(ex, 400, "{\"error\":\"Empty assembly code\"}");
                return;
            }

            // Write to a per-request temp file so concurrent users don't overwrite each other
            java.io.File tmpAsm = null;
            try {
                tmpAsm = java.io.File.createTempFile("sim_", ".asm");
                tmpAsm.deleteOnExit();
                Files.write(tmpAsm.toPath(), asmCode.getBytes(java.nio.charset.StandardCharsets.UTF_8));

                // Capture stderr for any error messages
                ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
                PrintStream origErr = System.err;
                System.setErr(new PrintStream(errBuf, true, java.nio.charset.StandardCharsets.UTF_8));

                try {
                    Main.runPipelinePublic(tmpAsm.getAbsolutePath());
                    System.setErr(origErr);
                    String errText = errBuf.toString(java.nio.charset.StandardCharsets.UTF_8);
                    sendJson(ex, 200,
                        "{\"ok\":true,\"stderr\":\"" + escapeJson(errText) + "\"}");
                } catch (Exception e) {
                    System.setErr(origErr);
                    sendJson(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            } finally {
                if (tmpAsm != null) tmpAsm.delete();
                running.set(false);
            }
        }
    }

    static class FileHandler implements HttpHandler {
        private final String filePath;

        FileHandler(String filePath) {
            this.filePath = filePath;
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            String content = readFile(filePath);
            sendJson(ex, 200, "{\"content\":\"" + escapeJson(content) + "\"}");
        }
    }

    // ── Trace Replay Handler ─────────────────────────────────────────────────

    /**
     * POST /api/trace
     * Body: raw bytes of the .trace file
     * Returns: { "ok": true, "content": "<stats text>" }
     */
    static class TraceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equals(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"POST required\"}");
                return;
            }

            if (!running.compareAndSet(false, true)) {
                sendJson(ex, 409, "{\"error\":\"Simulation already running\"}");
                return;
            }

            try {
                // Read uploaded trace bytes and save to a temp file
                byte[] traceBytes;
                try (InputStream is = ex.getRequestBody()) {
                    traceBytes = is.readAllBytes();
                }
                if (traceBytes.length == 0) {
                    sendJson(ex, 400, "{\"error\":\"Empty trace file\"}" );
                    return;
                }

                java.io.File tmpFile = java.io.File.createTempFile("upload_", ".trace");
                tmpFile.deleteOnExit();
                Files.write(tmpFile.toPath(), traceBytes);

                // Run trace simulator and capture output to a string
                ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
                try (PrintStream capturedOut = new PrintStream(outBuf, true, StandardCharsets.UTF_8)) {
                    common.Config cfg = new common.Config();
                    java.util.List<trace.TraceInstruction> instructions = trace.TraceParser.parse(tmpFile.getPath());
                    trace.TraceSimulator simulator = new trace.TraceSimulator(cfg);
                    simulator.run(instructions);

                    common.StatsPrinter.printTraceHeader(capturedOut, tmpFile.getName(), "default (Config.java)", instructions.size());
                    common.StatsPrinter.printTraceStats(capturedOut, simulator.getStats());
                    common.StatsPrinter.printConfigSection(capturedOut, cfg);
                }

                tmpFile.delete();

                String result = outBuf.toString(StandardCharsets.UTF_8);
                sendJson(ex, 200, "{\"ok\":true,\"content\":\"" + escapeJson(result) + "\"}");

            } catch (Exception e) {
                sendJson(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            } finally {
                running.set(false);
            }
        }
    }

    /**
     * GET /api/traces
     * Returns list of .trace files available in phase3_traces/ directory.
     * { "files": ["trace01.trace", "trace02.trace", ...] }
     */
    static class TracesListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            java.io.File dir = new java.io.File("phase3_traces");
            StringBuilder sb = new StringBuilder("{\"files\":");
            if (dir.isDirectory()) {
                java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".trace"));
                if (files != null) java.util.Arrays.sort(files);
                sb.append("[");
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        if (i > 0) sb.append(",");
                        sb.append("\"").append(escapeJson(files[i].getName())).append("\"");
                    }
                }
                sb.append("]");
            } else {
                sb.append("[]");
            }
            sb.append("}");
            sendJson(ex, 200, sb.toString());
        }
    }

    /**
     * GET /api/trace-file?name=trace01.trace
     * Serves the raw bytes of a named .trace file from phase3_traces/.
     * Used by the frontend to run preset trace files.
     */
    static class TraceFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }

            // Parse ?name= query param
            String query = ex.getRequestURI().getQuery();
            String name = null;
            if (query != null) {
                for (String part : query.split("&")) {
                    if (part.startsWith("name=")) {
                        name = java.net.URLDecoder.decode(part.substring(5), StandardCharsets.UTF_8);
                        break;
                    }
                }
            }
            if (name == null || name.contains("..") || name.contains("/") || name.contains("\\")) {
                sendJson(ex, 400, "{\"error\":\"Invalid file name\"}");
                return;
            }

            java.io.File file = new java.io.File("phase3_traces", name);
            if (!file.exists() || !file.isFile()) {
                sendJson(ex, 404, "{\"error\":\"File not found: " + escapeJson(name) + "\"}");
                return;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            ex.getResponseHeaders().set("Content-Type", "application/octet-stream");
            ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + name + "\"");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
