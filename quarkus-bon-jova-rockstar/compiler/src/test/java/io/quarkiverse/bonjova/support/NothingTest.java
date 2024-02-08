package io.quarkiverse.bonjova.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NothingTest {

    Nothing nothing = Nothing.NOTHING;

    @Test
    public void shouldImplementToString() {
        assertEquals("", nothing.toString());
    }

    @Test
    public void shouldCoerceToFalseInBooleanContext() {
        assertEquals(false, nothing.coerce(true));
    }

    @Test
    public void shouldCoerceToEmptyTextInStringContext() {
        assertEquals("", nothing.coerce("whatever"));
    }

    @Test
    public void shouldCoerceToEmptyStringInNumberContext() {
        assertEquals(0d, nothing.coerce(42d));
    }

    // Spec is ambiguous, but Satriani coerces to zero
    @Test
    public void shouldCoerceToZeroInAmbiguousContext() {
        assertEquals(0d, nothing.coerce(null));
    }

}