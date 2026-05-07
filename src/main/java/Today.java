import com.sun.net.httpserver.*;
import java.net.*;
import java.time.*;
import java.util.concurrent.*;

public class Today {
    public static void main(String[] args) throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress(8080), 0);
        RateLimiter limiter = new RateLimiter("limits.json");
        PingHandler ping = new PingHandler(limiter);
        AuthHandler auth = new AuthHandler(ping, "secret123");
        s.createContext("/ping", auth);
        LocalDateTime start = LocalDateTime.now();
        s.createContext("/health", ex -> {
            ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, authorization, Content-Type");
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                ex.close();
                return;
            }
            
            long up = Duration.between(start, LocalDateTime.now()).getSeconds();
            String resp = "{\"status\":\"healthy\",\"uptime\":" + up + "}";
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, resp.getBytes().length);
            ex.getResponseBody().write(resp.getBytes());
            ex.close();
        });

        s.createContext("/metrics", ex -> {
            ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, authorization, Content-Type");
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                ex.close();
                return;
            }
            long up = Duration.between(start, LocalDateTime.now()).getSeconds();
            String resp = "# HELP uptime_seconds Server uptime\n" +
                         "# TYPE uptime_seconds counter\n" +
                         "uptime_seconds " + up + "\n";
            ex.getResponseHeaders().set("Content-Type", "text/plain");
            ex.sendResponseHeaders(200, resp.getBytes().length);
            ex.getResponseBody().write(resp.getBytes());
            ex.close();
        });

        s.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        s.start();
        System.out.println("Server running on http://localhost:8080");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nDraining & Saving...");
            s.stop(0);
        }));
    }
}
