package org.example.aggregation;

import org.example.model.AggregatedStatistics;
import org.example.model.Event;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collector;

final public class StatsAccumulator {

    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong minTimestamp = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxTimestamp = new AtomicLong(Long.MIN_VALUE);
    private final DoubleAdder sum = new DoubleAdder();

    void add(Event event) {
        count.incrementAndGet();
        sum.add(event.value());
        
        // Atomic update for min timestamp
        minTimestamp.updateAndGet(current -> Math.min(current, event.timestamp()));
        
        // Atomic update for max timestamp
        maxTimestamp.updateAndGet(current -> Math.max(current, event.timestamp()));
    }

    StatsAccumulator combine(StatsAccumulator other) {
        this.count.addAndGet(other.count.get());
        this.sum.add(other.sum.sum());
        this.minTimestamp.updateAndGet(current -> 
            Math.min(current, other.minTimestamp.get()));
        this.maxTimestamp.updateAndGet(current -> 
            Math.max(current, other.maxTimestamp.get()));
        return this;
    }

    public AggregatedStatistics toImmutable(String id) {
        long finalCount = count.get();
        double average = finalCount == 0 ? 0.0 : sum.sum() / finalCount;
        long finalMin = minTimestamp.get();
        long finalMax = maxTimestamp.get();
        
        return new AggregatedStatistics(
                id,
                finalCount,
                finalMin == Long.MAX_VALUE ? 0 : finalMin,
                finalMax == Long.MIN_VALUE ? 0 : finalMax,
                average
        );
    }

    public static Collector<Event, StatsAccumulator, StatsAccumulator> collector() {
        return Collector.of(
                StatsAccumulator::new,
                StatsAccumulator::add,
                StatsAccumulator::combine,
                Collector.Characteristics.CONCURRENT,
                Collector.Characteristics.UNORDERED
        );
    }
}
