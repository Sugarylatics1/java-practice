import com.sun.net.httpserver.*;
import java.io.*;
import java.util.UUID;

public class PingHandler implements HttpHandler {
    private final RateLimiter rateLimiter;

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
            String err = "{\"error\":\"rate_limited\",\"trace\":\"" + traceId + "\"}";
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(429, err.getBytes().length);
            ex.getResponseBody().write(err.getBytes());
            ex.close();
            System.out.println("[" + traceId + "] 429 " + ip);
            return;
        }

        String resp = "{\"status\":\"ok\",\"trace\":\"" + traceId + "\",\"hits\":" + hits + "}";
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, resp.getBytes().length);
        ex.getResponseBody().write(resp.getBytes());
        ex.close();
        System.out.println("[" + traceId + "] 200 " + ip + " hits:" + hits);
    }
}
