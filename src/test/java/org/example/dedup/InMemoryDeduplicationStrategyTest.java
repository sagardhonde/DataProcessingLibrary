package org.example.dedup;

import org.example.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryDeduplicationStrategyTest {

    private InMemoryDeduplicationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new InMemoryDeduplicationStrategy();
    }

    @Test
    void testFirstOccurrence_IsUnique() {
        Event event = new Event("A", 1000L, 10.0);
        assertTrue(strategy.isUnique(event));
    }

    @Test
    void testDuplicate_SameIdAndTimestamp() {
        Event event1 = new Event("A", 1000L, 10.0);
        Event event2 = new Event("A", 1000L, 20.0); // Same id and timestamp

        assertTrue(strategy.isUnique(event1)); // First occurrence
        assertFalse(strategy.isUnique(event2)); // Duplicate
    }

    @Test
    void testSameId_DifferentTimestamp_IsUnique() {
        Event event1 = new Event("A", 1000L, 10.0);
        Event event2 = new Event("A", 2000L, 20.0); // Different timestamp

        assertTrue(strategy.isUnique(event1));
        assertTrue(strategy.isUnique(event2)); // Different timestamp, so unique
    }

    @Test
    void testSameTimestamp_DifferentId_IsUnique() {
        Event event1 = new Event("A", 1000L, 10.0);
        Event event2 = new Event("B", 1000L, 20.0); // Different id

        assertTrue(strategy.isUnique(event1));
        assertTrue(strategy.isUnique(event2)); // Different id, so unique
    }

    @Test
    void testMultipleDuplicates() {
        Event event1 = new Event("A", 1000L, 10.0);
        Event event2 = new Event("A", 1000L, 20.0);
        Event event3 = new Event("A", 1000L, 30.0);

        assertTrue(strategy.isUnique(event1));
        assertFalse(strategy.isUnique(event2));
        assertFalse(strategy.isUnique(event3));
    }

    @Test
    void testMultipleIds() {
        Event event1 = new Event("A", 1000L, 10.0);
        Event event2 = new Event("B", 1000L, 20.0);
        Event event3 = new Event("C", 1000L, 30.0);

        assertTrue(strategy.isUnique(event1));
        assertTrue(strategy.isUnique(event2));
        assertTrue(strategy.isUnique(event3));
    }

    @Test
    void testDuplicateAfterDifferentEvent() {
        Event event1 = new Event("A", 1000L, 10.0);
        Event event2 = new Event("B", 2000L, 20.0);
        Event event3 = new Event("A", 1000L, 30.0); // Duplicate of event1

        assertTrue(strategy.isUnique(event1));
        assertTrue(strategy.isUnique(event2));
        assertFalse(strategy.isUnique(event3)); // Duplicate
    }

    @Test
    void testConcurrentAccess_ThreadSafety() throws InterruptedException {
        int numThreads = 10;
        int eventsPerThread = 100;
        Thread[] threads = new Thread[numThreads];
        boolean[] results = new boolean[numThreads * eventsPerThread];
        int index = 0;

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < eventsPerThread; j++) {
                    Event event = new Event("A", (long) (threadId * eventsPerThread + j), 10.0);
                    results[threadId * eventsPerThread + j] = strategy.isUnique(event);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // All events should be unique (different timestamps)
        for (boolean result : results) {
            assertTrue(result);
        }
    }

    @Test
    void testConcurrentAccess_Duplicates() throws InterruptedException {
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        boolean[] results = new boolean[numThreads];

        // All threads try to add the same event
        Event duplicateEvent = new Event("A", 1000L, 10.0);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                results[threadId] = strategy.isUnique(duplicateEvent);
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Only one thread should get true (first to add), others should get false
        int trueCount = 0;
        for (boolean result : results) {
            if (result) {
                trueCount++;
            }
        }
        assertEquals(1, trueCount, "Only one thread should successfully add the duplicate event");
    }
}

