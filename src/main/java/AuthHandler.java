import com.sun.net.httpserver.*;
import java.io.*;

public class AuthHandler implements HttpHandler {
    private final HttpHandler next;
    private final String validToken;

    public AuthHandler(HttpHandler next, String validToken) {
        this.next = next;
        this.validToken = validToken;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, authorization, Content-Type");
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return;
        }
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (!validToken.equals(auth)) {
            String err = "{\"error\":\"unauthorized\"}";
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(401, err.getBytes().length);
            ex.getResponseBody().write(err.getBytes());
            ex.close();
            return;
        }
        next.handle(ex);
    }
}
