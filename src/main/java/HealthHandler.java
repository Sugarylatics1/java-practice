import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.*;
import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;

public class HealthHandler implements HttpHandler {
    private final RateLimiter rateLimiter;
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    public HealthHandler(RateLimiter rateLimiter) {this.rateLimiter = rateLimiter;}
    LocalDateTime start = LocalDateTime.now();
    @Override
    public void handle(HttpExchange ex) throws IOException {
        String traceId = UUID.randomUUID().toString();
        String ip = ex.getRemoteAddress().getAddress().getHostAddress();
        ex.getResponseHeaders().set("X-TraceId", traceId);
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
        long up = Duration.between(start, LocalDateTime.now()).getSeconds();
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "Healthy");
        resp.put("uptime", up);
        resp.put("traceId", traceId);
        byte[] out = gson.toJson(resp).getBytes();
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, out.length);
        ex.getResponseBody().write(out);
        ex.close();
    }
}
