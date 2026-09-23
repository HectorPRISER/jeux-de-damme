package dames;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Sert dans le navigateur exactement ce qui s'affiche dans le terminal en mode
 * bot vs bot ({@link Main#runBotVsBot}) : plateau, coups, métriques, ligne par
 * ligne, via Server-Sent Events (aucune dépendance ajoutée — {@code com.sun.net.httpserver}
 * fourni par le JDK). Une nouvelle partie recommence automatiquement à la fin
 * de la précédente, tant que la page reste ouverte.
 *
 * Lancer : {@code mvn -q exec:java -Dexec.mainClass=dames.Api}, puis ouvrir
 * http://localhost:8080.
 */
public final class Api {
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/api/stream", Api::handleStream);
        server.createContext("/", Api::handleStatic);
        server.start();
        System.out.println("Front sur http://localhost:" + port);
    }

    /** ?white=b (noirs jouent le minimax, blancs par défaut) &depth=5 (profondeur, défaut 5). */
    private static void handleStream(HttpExchange ex) throws IOException {
        Map<String, String> q = query(ex);
        Color strategicColor = "b".equalsIgnoreCase(q.get("white")) ? Color.BLACK : Color.WHITE;
        int depth = parseDepth(q.get("depth"));

        ex.getResponseHeaders().add("Content-Type", "text/event-stream; charset=utf-8");
        ex.getResponseHeaders().add("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, 0);

        try (PrintStream out = new PrintStream(new SseOutputStream(ex.getResponseBody()), true, StandardCharsets.UTF_8)) {
            while (!out.checkError()) {
                Main.runBotVsBot(strategicColor, depth, out, 500);
                out.println();
                out.println("Nouvelle partie dans 3 secondes...");
                sleepQuiet(3000);
            }
        } finally {
            ex.close();
        }
    }

    private static int parseDepth(String value) {
        if (value == null) return 5;
        try {
            int depth = Integer.parseInt(value);
            return depth > 0 ? depth : 5;
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    private static void sleepQuiet(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        String resource = "web" + path;
        InputStream in = Api.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            byte[] body = "not found".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, body.length);
            ex.getResponseBody().write(body);
            ex.close();
            return;
        }
        byte[] bytes = in.readAllBytes();
        ex.getResponseHeaders().add("Content-Type", contentType(path));
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static String contentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        return "application/octet-stream";
    }

    private static Map<String, String> query(HttpExchange ex) {
        Map<String, String> map = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null) return map;
        for (String part : raw.split("&")) {
            int eq = part.indexOf('=');
            if (eq < 0) continue;
            String key = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
            map.put(key, value);
        }
        return map;
    }

    /** Convertit un flux de texte (écrit ligne par ligne par {@code println}/{@code printf}) en évènements SSE. */
    private static final class SseOutputStream extends OutputStream {
        private final OutputStream sink;
        private final StringBuilder line = new StringBuilder();

        SseOutputStream(OutputStream sink) {
            this.sink = sink;
        }

        @Override
        public void write(int b) throws IOException {
            char c = (char) b;
            if (c == '\n') {
                flushLine();
            } else if (c != '\r') {
                line.append(c);
            }
        }

        private void flushLine() throws IOException {
            byte[] data = ("data: " + line + "\n\n").getBytes(StandardCharsets.UTF_8);
            line.setLength(0);
            sink.write(data);
            sink.flush();
        }
    }
}
