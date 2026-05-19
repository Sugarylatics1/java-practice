import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.*;
import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class PingHandler implements HttpHandler {
    private final RateLimiter rateLimiter;
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();
    private static final List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
    public PingHandler(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }
    public static long getP95() {
        if (latencies.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        int idx = (int) (sorted.size() * 0.95);
        return sorted.get(idx);
    }


    @Override
    public void handle(HttpExchange ex) throws IOException {
        long startNs = System.nanoTime();
        String ip = ex.getRemoteAddress().getAddress().getHostAddress();
        String traceId = UUID.randomUUID().toString();
        ex.getResponseHeaders().set("X-Request-ID", traceId);

        if (!rateLimiter.isAllowed(ip)) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "rate_limited");
            err.put("trace", traceId);
            
            ex.getResponseHeaders().set("Content-Type", "application/json");
            byte[] out = gson.toJson(err).getBytes();
            ex.sendResponseHeaders(429, out.length);
            ex.getResponseBody().write(out);
            ex.close();
            System.out.println("[" + traceId + "] 429 " + ip);
            logRequest(traceId, ip, 429, startNs);
            return;

        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        resp.put("trace", traceId);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        byte[] out = gson.toJson(resp).getBytes();
        ex.sendResponseHeaders(200, out.length);
        ex.getResponseBody().write(out);
        ex.close();
        System.out.println("[" + traceId + "] 200 " + ip);
        logRequest(traceId, ip, 200, startNs);
    }

    private void logRequest(String traceId, String ip, int status, long startNs) {
        try {
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
            latencies.add(latencyMs);
            if (latencies.size() > 1000) latencies.remove(0);
            String logLine = String.format("[%s] %s %s latency_ms:%d%n", traceId, ip, status, latencyMs);
            Files.writeString(Path.of("app.log"), logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println(logLine.trim());
        } catch (IOException e) {
            System.out.println("Failed to write log: " + e.getMessage());
        }
    }
}
