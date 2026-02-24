package org.example.dedup;

import org.example.model.Event;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryDeduplicationStrategy implements DeduplicationStrategy {

    private final Set<EventKey> seen = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isUnique(Event event) {
        return seen.add(new EventKey(event.id(), event.timestamp()));
    }

    private record EventKey(String id, long timestamp) {}
}