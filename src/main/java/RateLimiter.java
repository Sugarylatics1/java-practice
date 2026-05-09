import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

public class RateLimiter {
    private final String file;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, HitData> map = new ConcurrentHashMap<>();
    static class HitData {
        AtomicInteger hits;
        long lastReset;
        HitData(int hits, long lastReset) {
            this.hits = new AtomicInteger(hits);
            this.lastReset = lastReset;
        }
    }

    public RateLimiter(String file) {
        this.file = file;
        load();
    }

    public int recordHit(String ip) {
        HitData data = map.computeIfAbsent(ip, k -> new HitData(0, System.currentTimeMillis()));
        long now = System.currentTimeMillis();
        if (now - data.lastReset > 30_000) {
            data.hits.set(0);
            data.lastReset = now;
        }
        int currentHits = data.hits.incrementAndGet();
        save();
        return currentHits;
    }

    private void load() {
        try {
            if (Files.exists(Path.of(file))) {
                String content = Files.readString(Path.of(file)).replaceAll("[{}\"]", "");
                for (String entry : content.split(",")) {
                    if (entry.trim().isEmpty()) continue;
                    String[] kv = entry.split("=");
                    if (kv.length == 2) {
                        String ip = kv[0].trim();
                        String[] vals = kv[1].split("\\|"); // Split hits and time
                        map.put(ip, new HitData(Integer.parseInt(vals[0]), Long.parseLong(vals[1])));
                    }
                }
            }
        } catch (Exception e) { System.err.println("Load failed: " + e.getMessage()); }
    }

    private void save() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder("{");
            for (var e : map.entrySet()) {
                if (sb.length() > 1) sb.append(",");
                sb.append("\"").append(e.getKey()).append("\"=\"")
                        .append(e.getValue().hits.get()).append("|")
                        .append(e.getValue().lastReset).append("\"");
            }
            Files.writeString(Path.of(file), sb + "}");
        } catch (Exception e) { System.err.println("Save failed: " + e.getMessage()); }
        finally { lock.unlock(); }
    }
}
