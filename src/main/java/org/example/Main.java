package org.example;

import org.example.api.EventAggregationService;
import org.example.dedup.InMemoryDeduplicationStrategy;
import org.example.model.AggregatedStatistics;
import org.example.model.Event;
import org.example.service.DefaultEventAggregationService;
import org.example.util.Utility;
import org.example.validation.DefaultEventValidator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Map;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        Main m = new Main();
        m.getAggregatedData();

    }
    public void getAggregatedData(){
        EventAggregationService service = new DefaultEventAggregationService(
                new DefaultEventValidator(),
                new InMemoryDeduplicationStrategy()
        );

        InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream("dataset.ndjson");

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        Stream<Event> events = reader.lines()
                .map(Utility::toEvent);

        Map<String, AggregatedStatistics> result =
                service.aggregate(events);

        result.values().forEach(stats -> {
            System.out.println("------------------------------------------------");
            System.out.println("ID           : " + stats.id());
            System.out.println("Count        : " + stats.count());
            System.out.println("Min Timestamp: " + stats.minTimestamp());
            System.out.println("Max Timestamp: " + stats.maxTimestamp());
            System.out.println("Average      : " + stats.average());
        });


    }
}