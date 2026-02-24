package org.example.util;

import org.example.model.Event;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Utility {
    public static final ObjectMapper objectMapper = new ObjectMapper();

    public static Event toEvent(String jsonLine) {
        try {
            return objectMapper.readValue(jsonLine, Event.class);
        } catch (Exception e) {
            return new Event(null, 0, Double.NaN);
        }
    }
}
