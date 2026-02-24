# Data Processing Library

A library for processing event streams and calculating aggregated statistics. Handles out-of-order events, duplicates, and invalid data while being thread-safe and memory-efficient.

## What it does

Takes a stream of events and returns aggregated stats per event ID:
- Count of valid events
- Min and max timestamps
- Average value

Events can arrive out of order, have duplicates (same id + timestamp), or be invalid (NaN, negative values). The library filters these out and processes everything in parallel.

## Requirements

- Java 17+
- Maven (for building)

## Build and Run

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Run specific test
mvn test -Dtest=DefaultEventAggregationServiceTest

# Run the app
mvn exec:java -Dexec.mainClass="org.example.Main"
```

## How it works

The main entry point is `DefaultEventAggregationService`. You pass it a stream of events, and it returns a map of aggregated statistics.

```java
EventAggregationService service = new DefaultEventAggregationService(
    new DefaultEventValidator(),
    new InMemoryDeduplicationStrategy()
);

Stream<Event> events = ...; // your event stream
Map<String, AggregatedStatistics> result = service.aggregate(events);
```

The processing pipeline:
1. Validates events (filters out null, NaN, negative values)
2. Deduplicates (same id + timestamp = duplicate)
3. Groups by ID
4. Calculates stats (count, min/max timestamps, average)

## Design decisions

### Thread safety

Since we use parallel streams, multiple threads might process events for the same ID. I used atomic operations to handle this:

- `AtomicLong` for count and timestamps
- `DoubleAdder` for the sum (better for concurrent additions)
- `updateAndGet()` for atomic min/max updates

The `StatsAccumulator` collector is marked as `CONCURRENT`, so it can be safely shared across threads. Deduplication uses `ConcurrentHashMap.newKeySet()` which is thread-safe.

**Trade-off**: Slightly more memory overhead from atomic types, but it's negligible and worth it for correctness.

### Memory efficiency

The stream is processed lazily - we don't load everything into memory at once. Only the aggregated results are materialized.

Deduplication stores unique (id, timestamp) pairs in memory. Memory usage is O(U) where U is the number of unique pairs. For most use cases this is fine, but if you have billions of unique pairs, you might need a different approach (disk-based storage, bounded cache, or Bloom filter).

### Deduplication

Duplicates are identified by the combination of (id, timestamp), not just id. This means:
- Same ID with different timestamps = both counted
- Same ID with same timestamp = only first one counted

The `InMemoryDeduplicationStrategy` keeps state across calls. If you call `aggregate()` multiple times with the same strategy instance, it remembers events from previous calls. If you want independent processing, create a new strategy instance for each call.

### Validation

Events are considered invalid if:
- Event is null
- ID is null
- Value is NaN
- Value is negative

Invalid events are filtered out early, before deduplication. Zero values are valid.

## Assumptions

- Streams are finite (not infinite)
- Streams might be too large to fit in memory
- Events can arrive out of order
- Same (id, timestamp) pairs can appear multiple times
- We have enough RAM for unique (id, timestamp) pairs
- Library will be used with parallel streams
- Deduplication state is in-memory only (not persisted)

## Performance

- Time: O(N) where N is number of events
- Space: O(U) where U is unique (id, timestamp) pairs
- Parallel processing scales with CPU cores
- Thread-safe operations ensure correctness

## Testing

Tests cover:
- Empty streams
- Single events
- Duplicates
- Invalid events
- Out-of-order events
- Parallel processing
- Thread safety

Run with `mvn test`.
