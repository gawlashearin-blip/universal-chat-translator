package cn.universalchattranslator.util;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class OrderedAsyncBuffer<T> {
    private final Map<Long, Completed<T>> completed = new HashMap<>();
    private long nextSequence;
    private long nextToRelease;
    private boolean draining;

    public long submit(CompletableFuture<T> future, Duration timeout, Consumer<Outcome<T>> consumer) {
        final long sequence;
        synchronized (this) {
            sequence = nextSequence++;
        }
        future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((value, error) -> complete(sequence, Outcome.of(value, error), consumer));
        return sequence;
    }

    private void complete(long sequence, Outcome<T> outcome, Consumer<Outcome<T>> consumer) {
        synchronized (this) {
            completed.put(sequence, new Completed<>(outcome, consumer));
            if (draining || !completed.containsKey(nextToRelease)) return;
            draining = true;
        }

        while (true) {
            Completed<T> ready;
            synchronized (this) {
                ready = completed.remove(nextToRelease);
                if (ready == null) {
                    draining = false;
                    return;
                }
                nextToRelease++;
            }
            try {
                ready.consumer().accept(ready.outcome());
            } catch (RuntimeException ignored) {
                // A consumer must not permanently block later completed operations.
            }
        }
    }

    public record Outcome<T>(T value, Throwable error) {
        static <T> Outcome<T> of(T value, Throwable error) {
            return new Outcome<>(value, error);
        }

        public boolean succeeded() {
            return error == null;
        }
    }

    private record Completed<T>(Outcome<T> outcome, Consumer<Outcome<T>> consumer) {
    }
}
