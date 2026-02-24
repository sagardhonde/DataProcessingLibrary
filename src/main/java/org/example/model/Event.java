package org.example.model;

public record Event(
        String id,
        long timestamp,
        double value
) {}