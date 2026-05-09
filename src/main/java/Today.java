import com.sun.net.httpserver.*;
import java.net.*;
import java.time.*;
import java.util.concurrent.*;

public class Today {
    public static void main(String[] args) throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress(8080), 0);
        RateLimiter limiter = new RateLimiter("limits.json");
        PingHandler ping = new PingHandler(limiter);
        HealthHandler health = new HealthHandler(limiter);
        MetricsHandler metrics = new MetricsHandler();
        AuthHandler authForPing = new AuthHandler(ping, "secret123");
        AuthHandler authForHealth = new AuthHandler(health, "secret123");
        AuthHandler authForMetrics = new AuthHandler(metrics, "secret123");
        s.createContext("/ping", new GlobalExceptionHandler(authForPing));
        s.createContext("/health", new GlobalExceptionHandler(authForHealth));
        s.createContext("/metrics",new GlobalExceptionHandler(authForMetrics));
        s.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        s.start();
        System.out.println("Server running on http://localhost:8080");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nDraining & Saving...");
            s.stop(0);
        }));
    }
}
