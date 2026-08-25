package cn.universalchattranslator.message;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class PendingOwnMessageTrackerTest {
    @Test
    void consumesMatchingMessageOnceAndNormalizesWhitespaceAndCase() {
        long[] now = {1000};
        PendingOwnMessageTracker tracker = new PendingOwnMessageTracker(100, Duration.ofSeconds(30), () -> now[0]);
        tracker.track("Hello   World");

        assertTrue(tracker.consumeIfTracked(" hello world "));
        assertFalse(tracker.consumeIfTracked("hello world"));
    }

    @Test
    void expiresAfterThirtySecondsAndCapsAtOneHundred() {
        long[] now = {1000};
        PendingOwnMessageTracker tracker = new PendingOwnMessageTracker(100, Duration.ofSeconds(30), () -> now[0]);
        for (int i = 0; i < 101; i++) tracker.track("message-" + i);
        assertEquals(100, tracker.size());
        assertFalse(tracker.consumeIfTracked("message-0"));

        now[0] += 30_000;
        assertEquals(0, tracker.size());
    }
}
