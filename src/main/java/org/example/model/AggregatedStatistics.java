package org.example.model;

public record AggregatedStatistics(
        String id,
        long count,
        long minTimestamp,
        long maxTimestamp,
        double average
) {}