import com.sun.net.httpserver.HttpExchange;
import java.util.regex.Pattern;

public class InputValidator {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{8,32}$");

    public static boolean isValidToken(String token) {
        if (token == null || token.isEmpty()) return false;
        return TOKEN_PATTERN.matcher(token).matches();
    }

    public static boolean isPayloadTooLarge(HttpExchange ex) {
        String lenHeader = ex.getRequestHeaders().getFirst("Content-Length");
        if (lenHeader == null) return false;
        try {
            long size = Long.parseLong(lenHeader);
            return size > 1024;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}