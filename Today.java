import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;

public class Today {
    private static final String VALID_TOKEN = "secret123";
    private static final String LIMITS_FILE = "limits.json";
    private static final ReentrantLock fileLock = new ReentrantLock();
    private static final ConcurrentHashMap<String, AtomicInteger> rateLimits = new ConcurrentHashMap<>();
    private static final LocalDateTime startTime = LocalDateTime.now();

    public static void main(String[] args) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress(8080), 0);
        loadLimits();

        s.createContext("/ping", ex -> {
            String ip = ex.getRemoteAddress().getAddress().getHostAddress();
            String auth = ex.getRequestHeaders().getFirst("Authorization");

            if (!VALID_TOKEN.equals(auth)) {
                ex.sendResponseHeaders(401, 0); ex.close(); return;
            }

            int hits = rateLimits.computeIfAbsent(ip, k -> new AtomicInteger()).incrementAndGet();
            saveLimits();

            if (hits > 5) {
                ex.sendResponseHeaders(429, 0); ex.close(); return;
            }

            String resp = "{\"status\":\"ok\",\"user\":\"jimmir\",\"ip\":\"" + ip + "\",\"hits\":" + hits + "}";
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, resp.getBytes().length);
            ex.getResponseBody().write(resp.getBytes());
            ex.close();

            try (PrintWriter log = new PrintWriter(new FileWriter("log.txt", true))) {
                log.println(LocalDateTime.now() + " " + ip + " /ping " + auth + " hits:" + hits);
            }
        });

        s.createContext("/health", ex -> {
            long uptime = Duration.between(startTime, LocalDateTime.now()).getSeconds();
            String resp = "{\"status\":\"healthy\",\"uptime_seconds\":" + uptime + "}";
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, resp.getBytes().length);
            ex.getResponseBody().write(resp.getBytes());
            ex.close();
        });

        s.createContext("/metrics", ex -> {
            StringBuilder sb = new StringBuilder();
            for (var e : rateLimits.entrySet()) {
                sb.append("requests_total{ip=\"").append(e.getKey()).append("\"} ")
                  .append(e.getValue().get()).append("\n");
            }
            byte[] out = sb.toString().getBytes();
            ex.getResponseHeaders().set("Content-Type", "text/plain");
            ex.sendResponseHeaders(200, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });

        s.start();
        System.out.println("🌐 Server running on http://localhost:8080");
    }

    private static void loadLimits() {
        try {
            if (Files.exists(Path.of(LIMITS_FILE))) {
                String json = Files.readString(Path.of(LIMITS_FILE));
                json = json.replaceAll("[{}\"]", "");
                for (String entry : json.split(",")) {
                    if (entry.trim().isEmpty()) continue;
                    String[] kv = entry.split(":");
                    if (kv.length == 2) {
                        String ip = kv[0].trim();
                        int count = Integer.parseInt(kv[1].trim());
                        rateLimits.put(ip, new AtomicInteger(count));
                    }
                }
            }
        } catch (Exception e) { System.err.println("⚠️ loadLimits: " + e.getMessage()); }
    }

    private static void saveLimits() {
        fileLock.lock();
        try {
            StringBuilder json = new StringBuilder("{");
            for (var e : rateLimits.entrySet()) {
                if (json.length() > 1) json.append(",");
                json.append("\"").append(e.getKey()).append("\":").append(e.getValue().get());
            }
            json.append("}");
            Files.writeString(Path.of(LIMITS_FILE), json.toString());
        } catch (Exception e) { System.err.println("⚠️ saveLimits: " + e.getMessage()); }
        finally { fileLock.unlock(); }
    }
}
