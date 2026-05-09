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
import java.time.LocalDateTime;

public class MetricsHandler implements HttpHandler{
    LocalDateTime start = LocalDateTime.now();
    @Override
    public void handle(HttpExchange ex) throws IOException {
        long up = Duration.between(start, LocalDateTime.now()).getSeconds();
        String resp = "# HELP uptime_seconds Server uptime\n" +
                "# TYPE uptime_seconds counter\n" +
                "uptime_seconds " + up + "\n";
        ex.getResponseHeaders().set("Content-Type", "text/plain");
        ex.sendResponseHeaders(200, resp.getBytes().length);
        ex.getResponseBody().write(resp.getBytes());
        ex.close();
    }
}
