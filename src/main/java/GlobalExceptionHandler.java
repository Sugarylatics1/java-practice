import com.sun.net.httpserver.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.google.gson.Gson;

public class GlobalExceptionHandler implements HttpHandler {
	private final HttpHandler wrapped;
	private static final Gson gson = new Gson();

	public GlobalExceptionHandler(HttpHandler wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public void handle(HttpExchange ex) throws IOException {
		try {
			wrapped.handle(ex);
		} catch (Exception e) {
			String traceId = UUID.randomUUID().toString();
			ex.getResponseHeaders().set("X-Request-ID", traceId);
			ex.getResponseHeaders().set("Content-Type", "application/json");

			Map<String, Object> err = new HashMap<>();
			err.put("error", "internal_server_error");
			err.put("trace", traceId);

			byte[] out = gson.toJson(err).getBytes();
			ex.sendResponseHeaders(500, out.length);
			ex.getResponseBody().write(out);
			ex.close();

			System.err.println("[" + traceId + "] UNHANDLED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
			e.printStackTrace();
		}
	}
}