package org.example.service;

import org.example.dedup.InMemoryDeduplicationStrategy;
import org.example.model.AggregatedStatistics;
import org.example.model.Event;
import org.example.validation.DefaultEventValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DefaultEventAggregationServiceTest {

    private DefaultEventAggregationService service;

    @BeforeEach
    void setUp() {
        service = new DefaultEventAggregationService(
                new DefaultEventValidator(),
                new InMemoryDeduplicationStrategy()
        );
    }

    @Test
    void testEmptyStream() {
        Stream<Event> emptyStream = Stream.empty();
        Map<String, AggregatedStatistics> result = service.aggregate(emptyStream);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSingleElement() {
        Event event = new Event("A", 1000L, 10.5);
        Map<String, AggregatedStatistics> result = service.aggregate(Stream.of(event));

        assertEquals(1, result.size());
        assertTrue(result.containsKey("A"));

        AggregatedStatistics stats = result.get("A");
        assertEquals("A", stats.id());
        assertEquals(1, stats.count());
        assertEquals(1000L, stats.minTimestamp());
        assertEquals(1000L, stats.maxTimestamp());
        assertEquals(10.5, stats.average(), 0.001);
    }

    @Test
    void testDuplicates_SameIdAndTimestamp() {
        Event event1 = new Event("A", 1000L, 10.0);
        Event event2 = new Event("A", 1000L, 20.0); // Duplicate: same id and timestamp
        Event event3 = new Event("A", 1000L, 30.0); // Duplicate: same id and timestamp

        Map<String, AggregatedStatistics> result = service.aggregate(
                Stream.of(event1, event2, event3)
        );

        assertEquals(1, result.size());
        AggregatedStatistics stats = result.get("A");
        assertEquals(1, stats.count()); // Only first occurrence counted
        assertEquals(10.0, stats.average(), 0.001); // Only first event's value
        assertEquals(1000L, stats.minTimestamp());
        assertEquals(1000L, stats.maxTimestamp());
    }

    @Test
    void testSameId_DifferentTimestamps() {
        Event event1 = new Event("A", 1000L, 10.0);
        Event event2 = new Event("A", 2000L, 20.0);
        Event event3 = new Event("A", 3000L, 30.0);

        Map<String, AggregatedStatistics> result = service.aggregate(
                Stream.of(event1, event2, event3)
        );

        assertEquals(1, result.size());
        AggregatedStatistics stats = result.get("A");
        assertEquals(3, stats.count());
        assertEquals(20.0, stats.average(), 0.001); // (10 + 20 + 30) / 3
        assertEquals(1000L, stats.minTimestamp());
        assertEquals(3000L, stats.maxTimestamp());
    }

    @Test
    void testMultipleIds() {
        Event event1 = new Event("A", 1000L, 10.0);
        Event event2 = new Event("B", 2000L, 20.0);
        Event event3 = new Event("C", 3000L, 30.0);

        Map<String, AggregatedStatistics> result = service.aggregate(
                Stream.of(event1, event2, event3)
        );

        assertEquals(3, result.size());
        assertTrue(result.containsKey("A"));
        assertTrue(result.containsKey("B"));
        assertTrue(result.containsKey("C"));

        assertEquals(1, result.get("A").count());
        assertEquals(1, result.get("B").count());
        assertEquals(1, result.get("C").count());
    }

    @Test
    void testInvalidEvents_NaN() {
        Event valid = new Event("A", 1000L, 10.0);
        Event invalidNaN = new Event("A", 2000L, Double.NaN);

        Map<String, AggregatedStatistics> result = service.aggregate(
                Stream.of(valid, invalidNaN)
        );

        assertEquals(1, result.size());
        AggregatedStatistics stats = result.get("A");
        assertEquals(1, stats.count()); // Only valid event counted
        assertEquals(10.0, stats.average(), 0.001);
    }

    @Test
    void testInvalidEvents_NegativeValue() {
        Event valid = new Event("A", 1000L, 10.0);
        Event invalidNegative = new Event("A", 2000L, -5.0);

        Map<String, AggregatedStatistics> result = service.aggregate(
                Stream.of(valid, invalidNegative)
        );

        assertEquals(1, result.size());
        AggregatedStatistics stats = result.get("A");
        assertEquals(1, stats.count()); // Only valid event counted
        assertEquals(10.0, stats.average(), 0.001);
    }

    @Test
    void testInvalidEvents_NullId() {
        Event valid = new Event("A", 1000L, 10.0);
        Event invalidNullId = new Event(null, 2000L, 15.0);

        Map<String, AggregatedStatistics> result = service.aggregate(
                Stream.of(valid, invalidNullId)
        );

        assertEquals(1, result.size());
        AggregatedStatistics stats = result.get("A");
        assertEquals(1, stats.count()); // Only valid event counted
    }

    @Test
    void testInvalidEvents_NullEvent() {
        Event valid = new Event("A", 1000L, 10.0);
        Event nullEvent = null;

        // Note: Stream.of() doesn't allow null, so we use a list
        List<Event> events = Arrays.asList(valid, nullEvent);
        Map<String, AggregatedStatistics> result = service.aggregate(events.stream());

        assertEquals(1, result.size());
        AggregatedStatistics stats = result.get("A");
        assertEquals(1, stats.count());
    }

    @Test
    void testOutOfOrderEvents() {
        // Events arrive out of order
        Event event1 = new Event("A", 3000L, 30.0);
        Event event2 = new Event("A", 1000L, 10.0);
        Event event3 = new Event("A", 2000L, 20.0);

        Map<String, AggregatedStatistics> result = service.aggregate(
                Stream.of(event1, event2, event3)
        );

        assertEquals(1, result.size());
        AggregatedStatistics stats = result.get("A");
        assertEquals(3, stats.count());
        assertEquals(20.0, stats.average(), 0.001); // (30 + 10 + 20) / 3
        assertEquals(1000L, stats.minTimestamp()); // Correctly finds min
        assertEquals(3000L, stats.maxTimestamp()); // Correctly finds max
    }

    @Test
    void testComplexScenario_MixedValidInvalidAndDuplicates() {
        Event valid1 = new Event("A", 1000L, 10.0);
        Event duplicate = new Event("A", 1000L, 15.0); // Duplicate
        Event valid2 = new Event("A", 2000L, 20.0);
        Event invalidNaN = new Event("A", 3000L, Double.NaN);
        Event invalidNegative = new Event("A", 4000L, -5.0);
        Event valid3 = new Event("B", 1000L, 5.0);
        Event duplicateB = new Event("B", 1000L, 10.0); // Duplicate

        Map<String, AggregatedStatistics> result = service.aggregate(
                Stream.of(valid1, duplicate, valid2, invalidNaN, invalidNegative, valid3, duplicateB)
        );

        assertEquals(2, result.size());

        AggregatedStatistics statsA = result.get("A");
        assertEquals(2, statsA.count()); // valid1 and valid2 (duplicate and invalid filtered)
        assertEquals(15.0, statsA.average(), 0.001); // (10 + 20) / 2
        assertEquals(1000L, statsA.minTimestamp());
        assertEquals(2000L, statsA.maxTimestamp());

        AggregatedStatistics statsB = result.get("B");
        assertEquals(1, statsB.count()); // Only first occurrence of B
        assertEquals(5.0, statsB.average(), 0.001);
    }

    @Test
    void testZeroValue() {
        Event event = new Event("A", 1000L, 0.0);
        Map<String, AggregatedStatistics> result = service.aggregate(Stream.of(event));

        AggregatedStatistics stats = result.get("A");
        assertEquals(1, stats.count());
        assertEquals(0.0, stats.average(), 0.001);
    }

    @Test
    void testLargeValues() {
        Event event1 = new Event("A", 1000L, 1_000_000.0);
        Event event2 = new Event("A", 2000L, 2_000_000.0);

        Map<String, AggregatedStatistics> result = service.aggregate(
                Stream.of(event1, event2)
        );

        AggregatedStatistics stats = result.get("A");
        assertEquals(2, stats.count());
        assertEquals(1_500_000.0, stats.average(), 0.001);
    }

    @Test
    void testVeryLargeTimestamp() {
        long largeTimestamp = Long.MAX_VALUE - 1000;
        Event event1 = new Event("A", 1000L, 10.0);
        Event event2 = new Event("A", largeTimestamp, 20.0);

        Map<String, AggregatedStatistics> result = service.aggregate(
                Stream.of(event1, event2)
        );

        AggregatedStatistics stats = result.get("A");
        assertEquals(2, stats.count());
        assertEquals(1000L, stats.minTimestamp());
        assertEquals(largeTimestamp, stats.maxTimestamp());
    }

    @Test
    void testNullStream() {
        assertThrows(NullPointerException.class, () -> {
            service.aggregate(null);
        });
    }

    @Test
    void testParallelProcessing_Correctness() {
        // Create many events to ensure parallel processing
        List<Event> events = java.util.stream.IntStream.range(0, 1000)
                .mapToObj(i -> new Event("A", 1000L + i, i * 1.0))
                .toList();

        Map<String, AggregatedStatistics> result = service.aggregate(events.stream());

        assertEquals(1, result.size());
        AggregatedStatistics stats = result.get("A");
        assertEquals(1000, stats.count());

        // Average should be (0 + 1 + 2 + ... + 999) / 1000 = 499.5
        double expectedAverage = (0 + 999) * 1000.0 / 2.0 / 1000.0;
        assertEquals(expectedAverage, stats.average(), 0.001);
        assertEquals(1000L, stats.minTimestamp());
        assertEquals(1999L, stats.maxTimestamp());
    }

    @Test
    void testParallelProcessing_MultipleIds() {
        // Create events for multiple IDs in parallel
        List<Event> events = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            events.add(new Event("A", 1000L + i, i * 1.0));
            events.add(new Event("B", 2000L + i, i * 2.0));
            events.add(new Event("C", 3000L + i, i * 3.0));
        }

        Map<String, AggregatedStatistics> result = service.aggregate(events.stream());

        assertEquals(3, result.size());
        assertEquals(100, result.get("A").count());
        assertEquals(100, result.get("B").count());
        assertEquals(100, result.get("C").count());
    }

    @Test
    void testAverageCalculation_Precision() {
        Event event1 = new Event("A", 1000L, 1.0);
        Event event2 = new Event("A", 2000L, 2.0);
        Event event3 = new Event("A", 3000L, 3.0);

        Map<String, AggregatedStatistics> result = service.aggregate(
                Stream.of(event1, event2, event3)
        );

        AggregatedStatistics stats = result.get("A");
        // (1 + 2 + 3) / 3 = 2.0
        assertEquals(2.0, stats.average(), 0.0001);
    }

    @Test
    void testMinMaxTimestamp_EdgeCases() {
        Event event1 = new Event("A", Long.MAX_VALUE, 10.0);
        Event event2 = new Event("A", Long.MIN_VALUE, 20.0);
        Event event3 = new Event("A", 0L, 30.0);

        Map<String, AggregatedStatistics> result = service.aggregate(
                Stream.of(event1, event2, event3)
        );

        AggregatedStatistics stats = result.get("A");
        assertEquals(3, stats.count());
        assertEquals(Long.MIN_VALUE, stats.minTimestamp());
        assertEquals(Long.MAX_VALUE, stats.maxTimestamp());
    }
}

