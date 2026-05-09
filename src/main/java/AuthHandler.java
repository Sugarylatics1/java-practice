import com.sun.net.httpserver.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;


public class AuthHandler implements HttpHandler {
    private final HttpHandler next;
    private final String validToken;
    private static final Gson gson = new Gson();

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
        if (!InputValidator.isValidToken(auth)) {
            sendJsonError(ex, 400, "invalid_input", "Token must be 8-32 alphanumeric chars");
            return;
        }

        if (InputValidator.isPayloadTooLarge(ex)) {
            sendJsonError(ex, 413, "payload_too_large", "Max 1024 bytes allowed");
            return;
        }

        if (!validToken.equals(auth)) {
            sendJsonError(ex, 401, "unauthorized", "Invalid token");
            return;
        }

        next.handle(ex);
    }

    private void sendJsonError(HttpExchange ex, int status, String error, String message) throws IOException {
        Map<String, String> err = new HashMap<>();
        err.put("error", error);
        err.put("message", message);
        String json = gson.toJson(err);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        byte[] out = json.getBytes();
        ex.sendResponseHeaders(status, out.length);
        ex.getResponseBody().write(out);
        ex.close();
    }
}
