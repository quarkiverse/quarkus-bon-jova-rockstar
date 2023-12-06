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

}
