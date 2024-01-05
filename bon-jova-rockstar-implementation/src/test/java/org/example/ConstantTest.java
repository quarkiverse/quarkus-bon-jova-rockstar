package org.example;

import org.example.util.ParseHelper;
import org.junit.jupiter.api.Test;
import rock.Rockstar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ConstantTest {


    /*
    empty , silent , and silence are aliases for the empty string ( "" ).
     */
    @Test
    public void shouldParseEmptyStringAliases() {
        Rockstar.ConstantContext ctx = ParseHelper.getConstant("life is silence");
        Constant a = new Constant(ctx);
        assertEquals("", a.getValue());
        assertEquals(String.class, a.getValueClass());

        ctx = ParseHelper.getConstant("life is silent");
        a = new Constant(ctx);
        assertEquals("", a.getValue());

        ctx = ParseHelper.getConstant("life is empty");
        a = new Constant(ctx);
        assertEquals("", a.getValue());
    }


    @Test
    public void shouldParseBooleanConstantsForTrueCase() {
        Rockstar.ConstantContext ctx = ParseHelper.getConstant("life is true");
        Constant a = new Constant(ctx);
        assertEquals(true, a.getValue());
        assertEquals(boolean.class, a.getValueClass());

        ctx = ParseHelper.getConstant("life is right");
        a = new Constant(ctx);
        assertEquals(true, a.getValue());

        ctx = ParseHelper.getConstant("life is ok");
        a = new Constant(ctx);
        assertEquals(true, a.getValue());

        ctx = ParseHelper.getConstant("life is yes");
        a = new Constant(ctx);
        assertEquals(true, a.getValue());
    }

    @Test
    public void shouldParseBooleanConstantsForFalseCase() {
        Rockstar.ConstantContext ctx = ParseHelper.getConstant("life is false");
        Constant a = new Constant(ctx);
        assertEquals(false, a.getValue());
        assertEquals(boolean.class, a.getValueClass());

        ctx = ParseHelper.getConstant("life is lies");
        a = new Constant(ctx);
        assertEquals(false, a.getValue());

        ctx = ParseHelper.getConstant("life is wrong");
        a = new Constant(ctx);
        assertEquals(false, a.getValue());

        ctx = ParseHelper.getConstant("life is no");
        a = new Constant(ctx);
        assertEquals(false, a.getValue());
    }

    /*
    Rockstar makes a distinction between `null` and `undefined`. Javascript also does this,
    but since Java does not, we will ignore that for the moment.
     */
    @Test
    public void shouldParseUndefinedConstants() {
        Rockstar.ConstantContext ctx = ParseHelper.getConstant("life is mysterious");
        Constant a = new Constant(ctx);
        assertNull(a.getValue());
        // Not great, but the best we can do
    }

    @Test
    public void shouldParseNullConstants() {
        Rockstar.ConstantContext ctx = ParseHelper.getConstant("life is nothing");
        Constant a = new Constant(ctx);
        assertNull(a.getValue());

        ctx = ParseHelper.getConstant("life is nobody");
        a = new Constant(ctx);
        assertNull(a.getValue());

        ctx = ParseHelper.getConstant("life is nowhere");
        a = new Constant(ctx);
        assertNull(a.getValue());

        ctx = ParseHelper.getConstant("life is gone");
        a = new Constant(ctx);
        assertNull(a.getValue());
    }

}
