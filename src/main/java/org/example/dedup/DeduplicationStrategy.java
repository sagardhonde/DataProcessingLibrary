package org.example.dedup;

import org.example.model.Event;

public interface DeduplicationStrategy {
    boolean isUnique(Event event);
}