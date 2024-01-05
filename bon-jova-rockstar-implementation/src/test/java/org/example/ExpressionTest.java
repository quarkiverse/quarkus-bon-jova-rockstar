package org.example;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import org.example.util.ParseHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rock.Rockstar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
Note that even though we're just testing the expressions, they need to be couched in something like output statement to be
recognised.
 */
public class ExpressionTest {

    @BeforeEach
    public void clearState() {
        Variable.clearPronouns();
    }

    @Test
    public void shouldParseIntegerLiterals() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 5");
        Expression a = new Expression(ctx);
        // The number should be stored as a double, even though it was entered as an integer
        assertEquals(5d, a.getValue());
        assertEquals(double.class, a.getValueClass());
    }

    @Test
    public void shouldParseIntegerLiteralsAsVariables() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("my thing is 5\nshout my thing");
        Expression a = new Expression(ctx);
        // The 'value' is the variable name
        assertEquals("my thing", a.getValue());
        // A bit clunky, but finding an optimum value is non-obvious
        assertEquals(Variable.class, a.getValueClass());
    }

    /* Numbers in Rockstar are double-precision floating point numbers, stored according to the IEEE 754 standard.*/
    @Test
    public void shouldParseFloatingPointLiterals() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 3.141");
        Expression a = new Expression(ctx);
        assertEquals(3.141, a.getValue());
        assertEquals(double.class, a.getValueClass());
    }

    /*
empty , silent , and silence are aliases for the empty string ( "" ).
 */
    @Test
    public void shouldParseEmptyStringAliases() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout silence");
        Expression a = new Expression(ctx);
        assertEquals("", a.getValue());
        assertEquals(String.class, a.getValueClass());

        ctx = ParseHelper.getExpression("shout silent");
        a = new Expression(ctx);
        assertEquals("", a.getValue());

        ctx = ParseHelper.getExpression("shout empty");
        a = new Expression(ctx);
        assertEquals("", a.getValue());
    }


    @Test
    public void shouldParseBooleanLiteralsForTrueCase() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout true");
        Expression a = new Expression(ctx);
        assertEquals(true, a.getValue());
        assertEquals(boolean.class, a.getValueClass());

        ctx = ParseHelper.getExpression("shout right");
        a = new Expression(ctx);
        assertEquals(true, a.getValue());

        ctx = ParseHelper.getExpression("shout ok");
        a = new Expression(ctx);
        assertEquals(true, a.getValue());

        ctx = ParseHelper.getExpression("shout yes");
        a = new Expression(ctx);
        assertEquals(true, a.getValue());
    }

    @Test
    public void shouldParseBooleanLiteralsForFalseCase() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout false");
        Expression a = new Expression(ctx);
        assertEquals(false, a.getValue());
        assertEquals(boolean.class, a.getValueClass());

        ctx = ParseHelper.getExpression("shout lies");
        a = new Expression(ctx);
        assertEquals(false, a.getValue());

        ctx = ParseHelper.getExpression("shout wrong");
        a = new Expression(ctx);
        assertEquals(false, a.getValue());

        ctx = ParseHelper.getExpression("shout no");
        a = new Expression(ctx);
        assertEquals(false, a.getValue());
    }

    @Test
    public void shouldParseNullConstants() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout nothing");
        Expression a = new Expression(ctx);
        assertNull(a.getValue());

        ctx = ParseHelper.getExpression("shout nobody");
        a = new Expression(ctx);
        assertNull(a.getValue());

        ctx = ParseHelper.getExpression("shout nowhere");
        a = new Expression(ctx);
        assertNull(a.getValue());

        ctx = ParseHelper.getExpression("shout gone");
        a = new Expression(ctx);
        assertNull(a.getValue());
    }

    @Test
    public void shouldCreateResultHandlesOfTheCorrectTypeForStrings() {
        ClassCreator creator = ClassCreator.builder()
                                           .className("holder")
                                           .build();
        MethodCreator main = creator.getMethodCreator("main", void.class, String[].class);

        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout \"hello\"");
        ResultHandle handle = new Expression(ctx).getResultHandle(main);

        // We can't interrogate the type directly, so read it from the string
        assertTrue(handle.toString()
                         .contains("type='Ljava/lang/String;'"), handle.toString());

    }

    @Test
    public void shouldCreateResultHandlesOfTheCorrectTypeForNumbers() {
        ClassCreator creator = ClassCreator.builder()
                                           .className("holder")
                                           .build();
        MethodCreator main = creator.getMethodCreator("main", void.class, String[].class);

        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 5");
        ResultHandle handle = new Expression(ctx).getResultHandle(main);

        // We can't interrogate the type directly, so read it from the string
        assertTrue(handle.toString()
                         .contains("type='D'"), handle.toString());

    }

    @Test
    public void shouldCreateResultHandlesOfTheCorrectTypeForBooleans() {
        ClassCreator creator = ClassCreator.builder()
                                           .className("holder")
                                           .build();
        MethodCreator main = creator.getMethodCreator("main", void.class, String[].class);

        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout ok");
        ResultHandle handle = new Expression(ctx).getResultHandle(main);

        // We can't interrogate the type directly, so read it from the string
        assertTrue(handle.toString()
                         .contains("type='Z'"), handle.toString());

    }
}
