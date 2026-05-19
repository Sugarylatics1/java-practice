import java.util.concurrent.ConcurrentHashMap;
import java.util.Deque;
import java.util.ArrayDeque;

public class RateLimiter {
    private final long windowMs;
    private final int maxHits;
    private final ConcurrentHashMap<String, Deque<Long>> windows = new ConcurrentHashMap<>();
    public RateLimiter(long windowMs, int maxHits){
        this.windowMs = windowMs;
        this.maxHits = maxHits;
    }
    public boolean isAllowed(String ip) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = windows.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (deque) {
            while(!deque.isEmpty() && now - deque.peekFirst() > windowMs){
                deque.pollFirst();
            }
            if (deque.size() >= maxHits) return false;
            deque.addLast(now);
            return true;
        }
    }
}
