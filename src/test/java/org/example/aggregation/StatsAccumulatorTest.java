package org.example.aggregation;

import org.example.model.AggregatedStatistics;
import org.example.model.Event;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class StatsAccumulatorTest {

    @Test
    void testEmptyAccumulator() {
        StatsAccumulator accumulator = new StatsAccumulator();
        AggregatedStatistics stats = accumulator.toImmutable("A");

        assertEquals("A", stats.id());
        assertEquals(0, stats.count());
        assertEquals(0, stats.minTimestamp());
        assertEquals(0, stats.maxTimestamp());
        assertEquals(0.0, stats.average(), 0.001);
    }

    @Test
    void testAddSingleEvent() {
        StatsAccumulator accumulator = new StatsAccumulator();
        accumulator.add(new Event("A", 1000L, 10.5));
        AggregatedStatistics stats = accumulator.toImmutable("A");

        assertEquals(1, stats.count());
        assertEquals(1000L, stats.minTimestamp());
        assertEquals(1000L, stats.maxTimestamp());
        assertEquals(10.5, stats.average(), 0.001);
    }

    @Test
    void testAddMultipleEvents() {
        StatsAccumulator accumulator = new StatsAccumulator();
        accumulator.add(new Event("A", 1000L, 10.0));
        accumulator.add(new Event("A", 2000L, 20.0));
        accumulator.add(new Event("A", 3000L, 30.0));
        AggregatedStatistics stats = accumulator.toImmutable("A");

        assertEquals(3, stats.count());
        assertEquals(1000L, stats.minTimestamp());
        assertEquals(3000L, stats.maxTimestamp());
        assertEquals(20.0, stats.average(), 0.001); // (10 + 20 + 30) / 3
    }

    @Test
    void testCombine() {
        StatsAccumulator acc1 = new StatsAccumulator();
        acc1.add(new Event("A", 1000L, 10.0));
        acc1.add(new Event("A", 2000L, 20.0));

        StatsAccumulator acc2 = new StatsAccumulator();
        acc2.add(new Event("A", 3000L, 30.0));
        acc2.add(new Event("A", 4000L, 40.0));

        acc1.combine(acc2);
        AggregatedStatistics stats = acc1.toImmutable("A");

        assertEquals(4, stats.count());
        assertEquals(1000L, stats.minTimestamp());
        assertEquals(4000L, stats.maxTimestamp());
        assertEquals(25.0, stats.average(), 0.001); // (10 + 20 + 30 + 40) / 4
    }

    @Test
    void testMinMaxTimestamp_OutOfOrder() {
        StatsAccumulator accumulator = new StatsAccumulator();
        accumulator.add(new Event("A", 3000L, 30.0));
        accumulator.add(new Event("A", 1000L, 10.0));
        accumulator.add(new Event("A", 2000L, 20.0));
        AggregatedStatistics stats = accumulator.toImmutable("A");

        assertEquals(1000L, stats.minTimestamp());
        assertEquals(3000L, stats.maxTimestamp());
    }

    @Test
    void testCollector() {
        Stream<Event> events = Stream.of(
                new Event("A", 1000L, 10.0),
                new Event("A", 2000L, 20.0),
                new Event("A", 3000L, 30.0)
        );

        StatsAccumulator result = events.collect(StatsAccumulator.collector());
        AggregatedStatistics stats = result.toImmutable("A");

        assertEquals(3, stats.count());
        assertEquals(20.0, stats.average(), 0.001);
    }

    @Test
    void testZeroValue() {
        StatsAccumulator accumulator = new StatsAccumulator();
        accumulator.add(new Event("A", 1000L, 0.0));
        AggregatedStatistics stats = accumulator.toImmutable("A");

        assertEquals(1, stats.count());
        assertEquals(0.0, stats.average(), 0.001);
    }

    @Test
    void testLargeNumbers() {
        StatsAccumulator accumulator = new StatsAccumulator();
        accumulator.add(new Event("A", 1000L, 1_000_000.0));
        accumulator.add(new Event("A", 2000L, 2_000_000.0));
        AggregatedStatistics stats = accumulator.toImmutable("A");

        assertEquals(2, stats.count());
        assertEquals(1_500_000.0, stats.average(), 0.001);
    }
}

