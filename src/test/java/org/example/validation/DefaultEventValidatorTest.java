package org.example.validation;

import org.example.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultEventValidatorTest {

    private DefaultEventValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DefaultEventValidator();
    }

    @Test
    void testValidEvent() {
        Event event = new Event("A", 1000L, 10.5);
        assertTrue(validator.isValid(event));
    }

    @Test
    void testNullEvent() {
        assertFalse(validator.isValid(null));
    }

    @Test
    void testNullId() {
        Event event = new Event(null, 1000L, 10.5);
        assertFalse(validator.isValid(event));
    }

    @Test
    void testNaNValue() {
        Event event = new Event("A", 1000L, Double.NaN);
        assertFalse(validator.isValid(event));
    }

    @Test
    void testNegativeValue() {
        Event event = new Event("A", 1000L, -5.0);
        assertFalse(validator.isValid(event));
    }

    @Test
    void testZeroValue() {
        Event event = new Event("A", 1000L, 0.0);
        assertTrue(validator.isValid(event));
    }

    @Test
    void testPositiveInfinity() {
        Event event = new Event("A", 1000L, Double.POSITIVE_INFINITY);
        // Positive infinity is >= 0, so it should be valid
        assertTrue(validator.isValid(event));
    }

    @Test
    void testNegativeInfinity() {
        Event event = new Event("A", 1000L, Double.NEGATIVE_INFINITY);
        assertFalse(validator.isValid(event));
    }

    @Test
    void testEmptyStringId() {
        Event event = new Event("", 1000L, 10.5);
        // Empty string is not null, so it should be valid
        assertTrue(validator.isValid(event));
    }

    @Test
    void testVeryLargeValue() {
        Event event = new Event("A", 1000L, Double.MAX_VALUE);
        assertTrue(validator.isValid(event));
    }
}

