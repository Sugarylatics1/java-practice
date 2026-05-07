import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;
import com.google.gson.Gson;

public class GlobalExceptionHandler implements HttpHandler {
	private final HttpHandler wrapped:
	private static final Gson gson = new Gson();
	public GlobalExceptionHandler(HttpHandler wrapped) {
		this.wrapped = wrapped;
	}
	@Override
	public void handle(HttpExchange ex) throws IOException {
	
	}
}1
