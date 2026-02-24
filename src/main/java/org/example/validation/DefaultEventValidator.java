package org.example.validation;

import org.example.model.Event;
import org.example.validation.EventValidator;

public final class DefaultEventValidator implements EventValidator {

    @Override
    public boolean isValid(Event event) {
        return event != null
                && event.id() != null
                && !Double.isNaN(event.value())
                && event.value() >= 0;
    }
}