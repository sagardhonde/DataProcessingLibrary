package org.example.validation;

import org.example.model.Event;

public interface EventValidator {
    boolean isValid(Event event);
}