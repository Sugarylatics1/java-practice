import com.google.gson.Gson;
import com.sun.net.httpserver.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class StatsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
        Map<String, Long> stats = new HashMap<>();
        stats.put("p95_latency_ms", PingHandler.getP95());

        String json = new Gson().toJson(stats);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        byte[] out = json.getBytes();
        ex.sendResponseHeaders(200, out.length);
        ex.getResponseBody().write(out);
        ex.close();
    }
}