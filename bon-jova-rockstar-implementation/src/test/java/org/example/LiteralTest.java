package org.example;

import org.example.util.ParseHelper;
import org.junit.jupiter.api.Test;
import rock.Rockstar;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
Note: The programs here need some context outside the literal, such as an assignment or an output statement, to be recognised by the
grammar.
 Note also that poetic string and poetic literals are not handled by the literal class. */
public class LiteralTest {

    @Test
    public void shouldParseIntegerLiterals() {
        Rockstar.LiteralContext ctx = ParseHelper.getLiteral("thing is 5");
        Literal a = new Literal(ctx);
        // The number should be stored as a double, even though it was entered as an integer
        assertEquals(5d, a.getValue());
        assertEquals(double.class, a.getValueClass());
    }

    /* Numbers in Rockstar are double-precision floating point numbers, stored according to the IEEE 754 standard.*/
    @Test
    public void shouldParseFloatingPointLiterals() {
        Rockstar.LiteralContext ctx = ParseHelper.getLiteral("thing is 3.141");
        Literal a = new Literal(ctx);
        assertEquals(3.141, a.getValue());
        assertEquals(double.class, a.getValueClass());
    }

    @Test
    public void shouldParseNegativeLiterals() {
        Rockstar.LiteralContext ctx = ParseHelper.getLiteral("thing is -5");
        Literal a = new Literal(ctx);
        // The number should be stored as a double, even though it was entered as an integer
        assertEquals(-5d, a.getValue());
        assertEquals(double.class, a.getValueClass());
    }

    @Test
    public void shouldParseStringLiterals() {
        Rockstar.LiteralContext ctx = ParseHelper.getLiteral("thing is \"Yes hello\"");
        Literal a = new Literal(ctx);
        assertEquals("Yes hello", a.getValue());
        assertEquals(String.class, a.getValueClass());
    }
}
