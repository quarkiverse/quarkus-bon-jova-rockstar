package io.quarkiverse.bonjova.compiler;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RockstarArrayTest {

    @Test
    public void shouldAddAndGet() {
        RockstarArray ra = new RockstarArray();
        String thing = "thing";
        ra.add(thing);
        assertEquals(thing, ra.get(0));
    }

    @Test
    public void shouldAddAndGetAtAnIndexNearTheBeginning() {
        RockstarArray ra = new RockstarArray();
        String thing = "thing";
        ra.add(0, thing);
        assertEquals(thing, ra.get(0));

        String otherThing = "another thing";
        ra.add(1, otherThing);
        assertEquals(otherThing, ra.get(1));
        assertEquals(thing, ra.get(0));
        ra.add(0, otherThing);
        assertEquals(otherThing, ra.get(0));
    }

    @Test
    public void shouldIncreaseSizeOnAddition() {
        RockstarArray ra = new RockstarArray();
        assertEquals(0, ra.size());
        String thing = "thing";
        ra.add(0, thing);
        assertEquals(1, ra.size());

        String otherThing = "another thing";
        ra.add(1, otherThing);
        assertEquals(2, ra.size());

    }

    @Test
    public void shouldAddAndGetAtAnIndexThatHasNotYetBeenInitialised() {
        RockstarArray ra = new RockstarArray();
        String thing = "thing";
        double index = 10;
        ra.add(index, thing);
        assertEquals(thing, ra.get(index));
        assertEquals(index + 1, ra.size());

    }

    @Test
    public void shouldAddAndGetUsingNonNumericKeys() {
        RockstarArray ra = new RockstarArray();
        String key = "key";
        String thing = "thing";
        ra.add(key, thing);
        assertEquals(thing, ra.get(key));
    }

    @Test
    public void shouldNotCountNonNumericKeysInSize() {
        RockstarArray ra = new RockstarArray();
        String key = "key";
        String thing = "thing";
        ra.add(key, thing);
        assertEquals(thing, ra.get(key));
        assertEquals(0, ra.size());
    }

    @Test
    public void shouldTreatDoubleKeysAsNumericForGetting() {
        RockstarArray ra = new RockstarArray();
        String thing = "thing";
        ra.add(thing);
        assertEquals(thing, ra.get(Double.valueOf(0d)));
    }

    @Test
    public void shouldTreatRoundTextNumbersKeysAsNumericForGetting() {
        RockstarArray ra = new RockstarArray();
        String thing = "thing";
        ra.add(thing);
        assertEquals(thing, ra.get("0"));
    }

    @Test
    public void shouldTreatNonRoundTextNumbersKeysAsNumericForGetting() {
        RockstarArray ra = new RockstarArray();
        String thing = "thing";
        ra.add(thing);
        assertNull(ra.get("0.4"));
    }

    @Test
    public void shouldTreatTextNumbersKeysAsNumericForPutting() {
        RockstarArray ra = new RockstarArray();
        String thing = "thing";
        ra.add("0", thing);
        assertEquals(thing, ra.get(0));
        assertEquals(1, ra.size());
    }

    @Test
    public void shouldTreatLargeTextNumbersKeysAsNumericForPutting() {
        RockstarArray ra = new RockstarArray();
        String thing = "thing";
        ra.add(thing);
        ra.add("4", thing);
        assertEquals(thing, ra.get("4"));
        assertEquals(5, ra.size());
    }

    @Test
    public void shouldTreatObjectNumbersAsNumericKeys() {
        RockstarArray ra = new RockstarArray();
        Double key = 0d;
        String thing = "thing";
        ra.add(key, thing);
        assertEquals(thing, ra.get(key));
        assertEquals(1, ra.size());
    }


    @Test
    public void shouldTreatNonRoundTextNumbersKeysAsNumericForPutting() {
        RockstarArray ra = new RockstarArray();
        String thing = "thing";
        ra.add("0.4", thing);
        // It should be in there
        assertEquals(thing, ra.get("0.4"));
        // ... but in the map
        assertEquals(0, ra.size());
    }

    @Test
    public void shouldSupportPoppingOnAddition() {
        RockstarArray ra = new RockstarArray();
        assertEquals(0, ra.size());
        String thing = "thing";
        ra.add(0, thing);
        assertEquals(1, ra.size());

        String otherThing = "another thing";
        ra.add(1, otherThing);
        assertEquals(2, ra.size());

        assertEquals(thing, ra.pop());
        assertEquals(1, ra.size());
    }

    @Test
    public void shouldSupportPopFirstInFirstOut() {
        RockstarArray ra = new RockstarArray();
        assertEquals(0, ra.size());
        String thing = "thing";
        ra.add(0, thing);
        assertEquals(1, ra.size());

        String otherThing = "another thing";
        ra.add(1, otherThing);
        assertEquals(2, ra.size());

        assertEquals(thing, ra.pop());
        assertEquals(1, ra.size());
        assertEquals(otherThing, ra.pop());
        assertEquals(0, ra.size());
    }

    @Test
    public void shouldSupportPoppingPastTheEndOfTheArray() {
        RockstarArray ra = new RockstarArray();
        assertEquals(0, ra.size());
        String thing = "thing";
        ra.add(thing);

        assertEquals(thing, ra.pop());
        assertNull(ra.pop());
        assertNull(ra.pop());
    }

    @Test
    public void shouldSupportAddAll() {
        RockstarArray ra = new RockstarArray();
        ra.add(1);
        assertEquals(1, ra.size());
        List list = List.of(3, 5, 8, 9);
        ra.addAll(list);
        assertEquals(5, ra.size());
        assertEquals(8, ra.get(3));
    }

    @Test
    public void shouldTolerateAddingNull() {
        RockstarArray ra = new RockstarArray();
        ra.add(null);
        assertEquals(1, ra.size());
    }

    @Test
    public void shouldTolerateGettingPastTheEndOfTheArray() {
        RockstarArray ra = new RockstarArray();
        List list = List.of(3, 5, 8, 9);
        ra.addAll(list);
        assertEquals(null, ra.get(9));
    }

    @Test
    public void shouldTolerateGettingPastTheEndOfTheArrayForObjectKeys() {
        RockstarArray ra = new RockstarArray();
        List list = List.of(3, 5, 8, 9);
        ra.addAll(list);
        assertEquals(null, ra.get(Double.valueOf(9)));
    }

    @Test
    public void shouldJoinWithoutADelimiter() {
        RockstarArray ra = new RockstarArray();
        List list = List.of(3, 5, 8, 9);
        ra.addAll(list);
        assertEquals("3589", ra.join());
    }

    @Test
    public void shouldJoinWithADelimiter() {
        RockstarArray ra = new RockstarArray();
        List list = List.of(3, 5, 8, 9);
        ra.addAll(list);
        assertEquals("3-5-8-9", ra.join("-"));
    }


}