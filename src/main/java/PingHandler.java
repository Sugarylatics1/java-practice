import com.sun.net.httpserver.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;

public class PingHandler implements HttpHandler {
    private final RateLimiter rateLimiter;
    private static final Gson gson = new Gson();

    public PingHandler(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String ip = ex.getRemoteAddress().getAddress().getHostAddress();
        String traceId = UUID.randomUUID().toString();
        ex.getResponseHeaders().set("X-Request-ID", traceId);

        int hits = rateLimiter.recordHit(ip);

        if (hits > 5) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "rate_limited");
            err.put("trace", traceId);
            
            ex.getResponseHeaders().set("Content-Type", "application/json");
            byte[] out = gson.toJson(err).getBytes();
            ex.sendResponseHeaders(429, out.length);
            ex.getResponseBody().write(out);
            ex.close();
            System.out.println("[" + traceId + "] 429 " + ip);
            return;
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        resp.put("trace", traceId);
        resp.put("hits", hits);

        ex.getResponseHeaders().set("Content-Type", "application/json");
        byte[] out = gson.toJson(resp).getBytes();
        ex.sendResponseHeaders(200, out.length);
        ex.getResponseBody().write(out);
        ex.close();
        System.out.println("[" + traceId + "] 200 " + ip);
	String logLine = String.format("[%s] %s %s hits:%d%n", traceId, ip, hits > 5 ? "429" : "200", hits);
        Files.writeString(Path.of("app.log"), logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
