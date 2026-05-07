import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.Executors;
import java.util.UUID;

public class Today {
    private static final String VALID_TOKEN = "secret123";
    private static final String LIMITS_FILE = "limits.json";
    private static final ReentrantLock fileLock = new ReentrantLock();
    private static final ConcurrentHashMap<String, AtomicInteger> rateLimits = new ConcurrentHashMap<>();
    private static final LocalDateTime startTime = LocalDateTime.now();
    private static void addCorsHeaders(HttpExchange ex) {
    	ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    	ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
		ex.getResponseHeaders().add("Access-Control-Expose-Headers","X-Request-ID, Content-Type");
    }
    public static void main(String[] args) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress(8080), 0);
        loadLimits();
        s.createContext("/ping", ex -> {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                addCorsHeaders(ex);
                ex.sendResponseHeaders(204, -1);
                ex.close();
                return;
            }

            addCorsHeaders(ex);

            String ip = ex.getRemoteAddress().getAddress().getHostAddress();
            String auth = ex.getRequestHeaders().getFirst("Authorization");
            int hits = 0;
            int status = 200;

    	    if (!VALID_TOKEN.equals(auth)) {
        	status = 401;
        	String err = "{\"error\":\"unauthorized\",\"message\":\"Missing or invalid token\"}";
        	ex.getResponseHeaders().set("Content-Type", "application/json");
        	ex.sendResponseHeaders(status, err.getBytes().length);
       		ex.getResponseBody().write(err.getBytes());
        	ex.close();
        	return;
    	    }

	    hits = rateLimits.computeIfAbsent(ip, k -> new AtomicInteger()).incrementAndGet();
            saveLimits();

            if (hits > 5) {
        	status = 429;
        	String err = "{\"error\":\"rate_limited\",\"message\":\"Too many requests. Slow down.\"}";
        	ex.getResponseHeaders().set("Content-Type", "application/json");
        	ex.sendResponseHeaders(status, err.getBytes().length);
        	ex.getResponseBody().write(err.getBytes());
        	ex.close();
        	return;
    	    }
    	    String resp = "{\"status\":\"ok\",\"user\":\"jimmir\",\"ip\":\"" + ip + "\",\"hits\":" + hits + "}";
    	    ex.getResponseHeaders().set("Content-Type", "application/json");
    	    ex.sendResponseHeaders(200, resp.getBytes().length);
    	    ex.getResponseBody().write(resp.getBytes());
    	    ex.close();
	}); 

        s.createContext("/health", ex -> {
       	    addCorsHeaders(ex);	    
            long uptime = Duration.between(startTime, LocalDateTime.now()).getSeconds();
            String resp = "{\"status\":\"healthy\",\"uptime_seconds\":" + uptime + "}";
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, resp.getBytes().length);
            ex.getResponseBody().write(resp.getBytes());
            ex.close();
        });

        s.createContext("/metrics", ex -> {
            addCorsHeaders(ex); 
            StringBuilder sb = new StringBuilder();
            for (var e : rateLimits.entrySet()) {
                sb.append("requests_total{ip=\"").append(e.getKey()).append("\"} ")
                  .append(e.getValue().get()).append("\n");
            }
            byte[] out = sb.toString().getBytes();
            ex.getResponseHeaders().set("Content-Type", "text/plain");
            ex.sendResponseHeaders(200, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
	s.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        s.start();
        System.out.println("Server running on http://localhost:8080");
	Runtime.getRuntime().addShutdownHook(new Thread(() -> {
	    System.out.println("\n Signal received. Draining & Saving state ...");
	    saveLimits();
	    s.stop(0);
	    System.out.println("Shutdown complete.");
	}));
    }

    private static void loadLimits() {
        try {
            if (Files.exists(Path.of(LIMITS_FILE))) {
                String json = Files.readString(Path.of(LIMITS_FILE));
                json = json.replaceAll("[{}\"]", "");
                for (String entry : json.split(",")) {
                    if (entry.trim().isEmpty()) continue;
                    String[] kv = entry.split(":");
                    if (kv.length == 2) {
                        String ip = kv[0].trim();
                        int count = Integer.parseInt(kv[1].trim());
                        rateLimits.put(ip, new AtomicInteger(count));
                    }
                }
            }
        } catch (Exception e) { System.err.println("loadLimits: " + e.getMessage()); }
    }

    private static void saveLimits() {
        fileLock.lock();
        try {
            StringBuilder json = new StringBuilder("{");
            for (var e : rateLimits.entrySet()) {
                if (json.length() > 1) json.append(",");
                json.append("\"").append(e.getKey()).append("\":").append(e.getValue().get());
            }
            json.append("}");
            Files.writeString(Path.of(LIMITS_FILE), json.toString());
        } catch (Exception e) { System.err.println("saveLimits: " + e.getMessage()); }
        finally { fileLock.unlock(); }
    }
    private static void logEvent(String level, String ip, String path, int status, int hits) {
		String dateStr = LocalDate.now().toString();
		String ts = LocalDateTime.now().toString();
		String logFile = "log-" + dateStr + ".txt";
		String json = String.format("{\"ts\":\"%s\",\"level\":\"%s\",\"ip\":\"%s\",\"path\":\"%s\",\"status\":%d,\"hits\":%d}%n",ts,level,ip,path,status,hits);
		try(PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
	    	out.print(json);
		} catch(IOException e) { System.err.println("Log failed: " + e.getMessage()); }
    }
    
}
