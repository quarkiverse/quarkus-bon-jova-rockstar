package io.quarkiverse.bonjova.support;

import static java.lang.Boolean.FALSE;

public class Nothing {
    public static final Nothing NOTHING = new Nothing();
    public static final String NULL = "null";
    public static final String EMPTY_STRING = "";
    public static final Double ZERO = Double.valueOf(0d);

    private Nothing() {

    }

    public String toString() {
        return EMPTY_STRING;
    }

    public Object coerce(Object reference) {
        if (reference instanceof Double) {
            return coerceToNumber();
        } else if (reference instanceof String) {
            return toString();
        } else if (reference instanceof Boolean) {
            return coerceToBoolean();
        } else {
            return coerceToSomething();
        }
    }

    public Object coerceWithVisibleNulls(Object reference) {
        if (reference instanceof Double) {
            return coerceToNumber();
        } else if (reference instanceof String) {
            return coerceToString(); // Note that this is not the same as one gets with toString()!
        } else if (reference instanceof Boolean) {
            return coerceToBoolean();
        } else {
            return coerceToSomething();
        }
    }

    public Boolean coerceToBoolean() {
        return FALSE;
    }

    public String coerceToString() {
        return NULL;
    }

    public Double coerceToNumber() {
        return ZERO;
    }

    public Object coerceToSomething() {
        return ZERO;
    }

}
