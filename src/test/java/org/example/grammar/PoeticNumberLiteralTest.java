package org.example.grammar;

import org.example.ParseHelper;
import org.junit.jupiter.api.Test;
import rock.Rockstar;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PoeticNumberLiteralTest {


    @Test
    public void shouldParseIntegerPoeticNumberLiterals() {
        Rockstar.PoeticNumberLiteralContext ctx = ParseHelper.getPoeticNumberLiteral("My thing is a big bad monster");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(1337, a.getValue());
        assertEquals(int.class, a.getVariableClass());
    }

    @Test
    public void shouldParsePoeticNumberLiteralsContainingZeros() {
        Rockstar.PoeticNumberLiteralContext ctx = ParseHelper.getPoeticNumberLiteral("Tommy was a lovestruck ladykiller");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(100, a.getValue());
        assertEquals(int.class, a.getVariableClass());
    }

    @Test
    public void shouldIgnoreCommas() {
        Rockstar.PoeticNumberLiteralContext ctx = ParseHelper.getPoeticNumberLiteral("My thing is a big, bad, monster");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(1337, a.getValue());
        assertEquals(int.class, a.getVariableClass());
    }

    @Test
    public void shouldIgnoreApostrophes() {
        Rockstar.PoeticNumberLiteralContext ctx = ParseHelper.getPoeticNumberLiteral("My thing is a smokin' gun");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(163, a.getValue());
        assertEquals(int.class, a.getVariableClass());
    }

    @Test
    public void shouldIgnoreSemicolons() {
        Rockstar.PoeticNumberLiteralContext ctx = ParseHelper.getPoeticNumberLiteral("My thing is good; too good for you");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(43433, a.getValue());
        assertEquals(int.class, a.getVariableClass());
    }

    @Test
    public void shouldParseFloatingPointPoeticNumberLiterals() {
        Rockstar.PoeticNumberLiteralContext ctx = ParseHelper.getPoeticNumberLiteral(
                "My dreams were ice. A life unfulfilled; wakin' everybody up, taking booze and pills");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(3.1415926535, a.getValue());
        assertEquals(double.class, a.getVariableClass());

        ctx = ParseHelper.getPoeticNumberLiteral(
                "My dreams were ice A. life unfulfilled; wakin' everybody up, taking booze and pills");
        a = new PoeticNumberLiteral(ctx);
        assertEquals(31.415926535, a.getValue());

        ctx = ParseHelper.getPoeticNumberLiteral(
                "My dreams were ice A life unfulfilled; wakin' everybody. up, taking booze and pills");
        a = new PoeticNumberLiteral(ctx);
        assertEquals(314159.26535, a.getValue());
    }

}
