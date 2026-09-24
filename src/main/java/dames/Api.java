package dames;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Serveur HTTP minimal pour les tests de charge (voir mesures.py) : {@code GET /api/move?depth=5}.
 * Lancer : {@code mvn -q exec:java -Dexec.mainClass=dames.Api} (port 8080, ou un autre passé en argument).
 */
public final class Api {
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/api/move", Api::handleMove);
        server.start();
        System.out.println("Serveur sur http://localhost:" + port);
    }

    /** Coup du bot depuis la position initiale (?depth=5) : une requête courte, pour les tests de charge (Vegeta). */
    private static void handleMove(HttpExchange ex) throws IOException {
        int depth = Main.parseDepth(query(ex).get("depth"));
        byte[] body = String.valueOf(new Bot(depth).chooseMove(Board.initial(), Color.WHITE)).getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, body.length);
        ex.getResponseBody().write(body);
        ex.close();
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
}
