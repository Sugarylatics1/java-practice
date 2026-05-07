import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

public class RateLimiter {
    private final String file;
    private final ReentrantLock lock = new ReentrantLock();
    private final ConcurrentHashMap<String, AtomicInteger> map = new ConcurrentHashMap<>();

    public RateLimiter(String file) {
        this.file = file;
        load();
    }

    public int recordHit(String ip) {
        int hits = map.computeIfAbsent(ip, k -> new AtomicInteger()).incrementAndGet();
        save();
        return hits;
    }

    private void load() {
        try {
            if (Files.exists(Path.of(file))) {
                String json = Files.readString(Path.of(file)).replaceAll("[{}\"]", "");
                for (String entry : json.split(",")) {
                    if (entry.trim().isEmpty()) continue;
                    String[] kv = entry.split(":");
                    if (kv.length == 2) map.put(kv[0].trim(), new AtomicInteger(Integer.parseInt(kv[1].trim())));
                }
            }
        } catch (Exception e) { System.err.println("Load failed: " + e.getMessage()); }
    }

    private void save() {
        lock.lock();
        try {
            StringBuilder json = new StringBuilder("{");
            for (var e : map.entrySet()) {
                if (json.length() > 1) json.append(",");
                json.append("\"").append(e.getKey()).append("\":").append(e.getValue().get());
            }
            Files.writeString(Path.of(file), json + "}");
        } catch (Exception e) { System.err.println("Save failed: " + e.getMessage()); }
        finally { lock.unlock(); }
    }
}
