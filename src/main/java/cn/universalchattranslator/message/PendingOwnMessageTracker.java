package cn.universalchattranslator.message;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.LongSupplier;

public final class PendingOwnMessageTracker {
    private final int maximumSize;
    private final long ttlMillis;
    private final LongSupplier clock;
    private final Deque<Entry> entries = new ArrayDeque<>();

    public PendingOwnMessageTracker() {
        this(100, Duration.ofSeconds(30), System::currentTimeMillis);
    }

    PendingOwnMessageTracker(int maximumSize, Duration ttl, LongSupplier clock) {
        this.maximumSize = maximumSize;
        this.ttlMillis = ttl.toMillis();
        this.clock = clock;
    }

    public synchronized void track(String translatedMessage) {
        pruneExpired();
        while (entries.size() >= maximumSize) entries.removeFirst();
        entries.addLast(new Entry(
                MessageClassifier.normalizeForComparison(translatedMessage), clock.getAsLong() + ttlMillis));
    }

    public synchronized boolean consumeIfTracked(String message) {
        pruneExpired();
        String normalized = MessageClassifier.normalizeForComparison(message);
        Iterator<Entry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().normalizedMessage().equals(normalized)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    public synchronized int size() {
        pruneExpired();
        return entries.size();
    }

    private void pruneExpired() {
        long now = clock.getAsLong();
        while (!entries.isEmpty() && entries.peekFirst().expiresAtMillis() <= now) entries.removeFirst();
    }

    private record Entry(String normalizedMessage, long expiresAtMillis) {
    }
}
