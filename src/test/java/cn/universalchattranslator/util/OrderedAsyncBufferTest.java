package cn.universalchattranslator.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class OrderedAsyncBufferTest {
    @Test
    void releasesConcurrentResultsInSubmissionOrder() throws Exception {
        OrderedAsyncBuffer<String> buffer = new OrderedAsyncBuffer<>();
        CompletableFuture<String> a = new CompletableFuture<>();
        CompletableFuture<String> b = new CompletableFuture<>();
        CompletableFuture<String> c = new CompletableFuture<>();
        List<String> released = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);

        for (CompletableFuture<String> future : List.of(a, b, c)) {
            buffer.submit(future, Duration.ofSeconds(1), result -> {
                synchronized (released) { released.add(result.value()); }
                latch.countDown();
            });
        }
        b.complete("B");
        c.complete("C");
        assertTrue(released.isEmpty());
        a.complete("A");

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(List.of("A", "B", "C"), released);
    }

    @Test
    void failureAndTimeoutConsumeTheirSequence() throws Exception {
        OrderedAsyncBuffer<String> buffer = new OrderedAsyncBuffer<>();
        CompletableFuture<String> never = new CompletableFuture<>();
        CompletableFuture<String> second = new CompletableFuture<>();
        CompletableFuture<String> third = new CompletableFuture<>();
        List<String> released = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);

        buffer.submit(never, Duration.ofMillis(40), result -> {
            released.add(result.succeeded() ? result.value() : "timeout");
            latch.countDown();
        });
        buffer.submit(second, Duration.ofSeconds(1), result -> {
            released.add(result.succeeded() ? result.value() : "failed");
            latch.countDown();
        });
        buffer.submit(third, Duration.ofSeconds(1), result -> {
            released.add(result.value());
            latch.countDown();
        });
        second.completeExceptionally(new IllegalStateException("broken"));
        third.complete("C");

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(List.of("timeout", "failed", "C"), released);
    }
}
