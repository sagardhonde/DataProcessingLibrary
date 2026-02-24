package org.example.api;


import org.example.model.AggregatedStatistics;
import org.example.model.Event;

import java.util.Map;
import java.util.stream.Stream;

public interface EventAggregationService {
    Map<String, AggregatedStatistics> aggregate(Stream<Event> events);
}