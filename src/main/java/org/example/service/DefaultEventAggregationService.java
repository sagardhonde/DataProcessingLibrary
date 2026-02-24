package org.example.service;

import org.example.api.EventAggregationService;
import org.example.aggregation.StatsAccumulator;
import org.example.dedup.DeduplicationStrategy;
import org.example.model.AggregatedStatistics;
import org.example.model.Event;
import org.example.validation.EventValidator;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DefaultEventAggregationService implements EventAggregationService {

    private final EventValidator validator;
    private final DeduplicationStrategy deduplicationStrategy;

    public DefaultEventAggregationService(EventValidator validator,
                                          DeduplicationStrategy deduplicationStrategy) {
        this.validator = Objects.requireNonNull(validator);
        this.deduplicationStrategy = Objects.requireNonNull(deduplicationStrategy);
    }

    @Override
    public Map<String, AggregatedStatistics> aggregate(Stream<Event> events) {
        Objects.requireNonNull(events, "Event stream must not be null");

        return events
                .parallel()
                .filter(validator::isValid)
                .filter(deduplicationStrategy::isUnique)
                .collect(Collectors.groupingByConcurrent(
                        Event::id,
                        StatsAccumulator.collector()
                ))
                .entrySet()
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> e.getValue().toImmutable(e.getKey())
                ));
    }
}