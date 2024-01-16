package org.example;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import org.example.util.DynamicClassLoader;
import org.example.util.ParseHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;

import java.lang.reflect.InvocationTargetException;

import static org.example.Constant.NOTHING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        // Pre-initialise the variable so there's enough information about types
        Rockstar.VariableContext vctx = ParseHelper.getVariable("my thing is 5");
        Variable variable = new Variable(vctx, double.class);

        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout my thing");
        Expression a = new Expression(ctx);
        // The 'value' is the variable name
        assertEquals("my__thing", a.getValue());
        assertEquals(double.class, a.getValueClass());
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
        assertEquals(NOTHING, a.getValue());

        ctx = ParseHelper.getExpression("shout nobody");
        a = new Expression(ctx);
        assertEquals(NOTHING, a.getValue());

        ctx = ParseHelper.getExpression("shout nowhere");
        a = new Expression(ctx);
        assertEquals(NOTHING, a.getValue());

        ctx = ParseHelper.getExpression("shout gone");
        a = new Expression(ctx);
        assertEquals(NOTHING, a.getValue());
    }

    @Test
    public void shouldHandleSimpleAddition() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 3 + 6", 0);
        Expression a = new Expression(ctx);
        assertNull(a.getValue());
        double answer = (double) execute(a);
        assertEquals(9d, answer);
    }

    @Test
    public void shouldHandleAdditionWithAliases() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 3 plus 6", 0);
        Expression a = new Expression(ctx);
        assertNull(a.getValue());
        double answer = (double) execute(a);
        assertEquals(9d, answer);

        ctx = ParseHelper.getExpression("shout 8 with 5", 0);
        a = new Expression(ctx);
        answer = (double) execute(a);
        assertEquals(13d, answer);
    }

    @Test
    public void shouldHandleStringAddition() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say \"rock\" plus \"roll\"", 0);
        Expression e = new Expression(ctx);
        String answer = (String) execute(e);
        assertEquals("rockroll", answer);
    }

    @Test
    public void shouldFormatNumbersCorrectlyOnStringAddition() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say 99 plus \" red balloons\"", 0);
        Expression e = new Expression(ctx);
        String answer = (String) execute(e);
        assertEquals("99 red balloons", answer);

        ctx = ParseHelper.getExpression("say \"blink\" plus 42", 0);
        e = new Expression(ctx);
        answer = (String) execute(e);
        assertEquals("blink42", answer);
    }

    @Test
    public void shouldHandleSimpleSubtraction() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 3 - 6", 0);
        Expression a = new Expression(ctx);
        assertNull(a.getValue());
        double answer = (double) execute(a);
        assertEquals(-3d, answer);
    }

    @Test
    public void shouldHandleSubtractionWithAliases() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 3 minus 6", 0);
        Expression a = new Expression(ctx);
        assertNull(a.getValue());
        double answer = (double) execute(a);
        assertEquals(-3d, answer);

        ctx = ParseHelper.getExpression("shout 8 without 5", 0);
        a = new Expression(ctx);
        answer = (double) execute(a);
        assertEquals(3d, answer);
    }

    @Test
    public void shouldHandleSimpleMultiplication() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 3 * 6", 0);
        Expression a = new Expression(ctx);
        assertNull(a.getValue());
        double answer = (double) execute(a);
        assertEquals(18d, answer);
    }

    @Test
    public void shouldHandleMultiplicationWithAliases() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 3 times 6", 0);
        Expression a = new Expression(ctx);
        assertNull(a.getValue());
        double answer = (double) execute(a);
        assertEquals(18d, answer);

        ctx = ParseHelper.getExpression("shout 8 of 5", 0);
        a = new Expression(ctx);
        answer = (double) execute(a);
        assertEquals(40d, answer);
    }

    // This is a whole section of implementation, but handle some simple cases
    @Nested
    @DisplayName("Types of operations on types")
    class TypeTests {
        @Test
        public void shouldInferASuitableTypeForMultiplicationOfTwoNumbers() {
            Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 3 times 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            assertEquals(double.class, a.getValueClass());
        }

        // String <times> Number => String gets repeated <Number> times
        @Test
        public void shouldInferASuitableTypeForMultiplicationOfANumberAndAString() {
            Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout \"hello\" times 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            assertEquals(String.class, a.getValueClass());
        }

        @Test
        public void shouldInferASuitableTypeForSubtractionOfTwoNumbers() {
            Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 3 minus 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            assertEquals(double.class, a.getValueClass());
        }

        @Test
        public void shouldInferASuitableTypeForAdditionOfTwoNumbers() {
            Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 3 plus 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            assertEquals(double.class, a.getValueClass());
        }

        // String <times> Number => String gets repeated <Number> times
        @Test
        public void shouldInferASuitableTypeForAdditionOfANumberAndAString() {
            Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout \"hello\" plus 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            assertEquals(String.class, a.getValueClass());
        }
    }

    @Disabled("See https://github.com/holly-cummins/bon-jova-rockstar-implementation/issues/23")
    @Test
    public void shouldHandleSimpleDivision() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 24/6", 0);
        Expression a = new Expression(ctx);
        assertNull(a.getValue());
        double answer = (double) execute(a);
        assertEquals(4, answer);
    }

    @Disabled("See https://github.com/holly-cummins/bon-jova-rockstar-implementation/issues/23")
    @Test
    public void shouldHandleDivisionWithAliases() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 3 over 6", 0);
        Expression a = new Expression(ctx);
        assertNull(a.getValue());
        double answer = (double) execute(a);
        assertEquals(0.5, answer);

        ctx = ParseHelper.getExpression("shout 40 between 5", 0);
        a = new Expression(ctx);
        answer = (double) execute(a);
        assertEquals(5d, answer);
    }

    @Test
    public void shouldHandleComparisonOfNumbers() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say 1 is 2", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("shout 1 is 1", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldHandleComparisonOfBooleans() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say ok is right", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("shout right is wrong", 0);
        assertFalse((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldHandleComparisonOfStrings() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say \"life\" is \"life\"", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say \"live\" is \"life\"", 0);
        assertFalse((boolean) execute(new Expression(ctx)));
    }

    @Disabled("This also fails to parse in the reference implementation")
    @Test
    public void shouldHandleAintComparisonOfStrings() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say 123 ain’t \"all that\"", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say \"Tommy\" ain’t \"Tommy\"", 0);
        assertFalse((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldHandleAintComparisonOfStringsWithNoApostrophe() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say \"Tommy\" aint \"nobody\"", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say \"Tommy\" aint \"Tommy\"", 0);
        assertFalse((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldHandleContractionSComparison() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say 123's 123", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 123's 124", 0);
        assertFalse((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldHandleGreaterThanComparison() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say 5 is greater than 10", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 2 is stronger than 20", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 20 is stronger than 2", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 20 is higher than 2", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 20 is bigger than 2", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldReturnFalseForGreaterThanComparisonOfEqualThings() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say 5 is greater than 5", 0);
        assertFalse((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldHandleGreaterThanOrEqualComparison() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say 5 is as great as 10", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 2 is as strong as 20", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 20 is as strong as 2", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 20 is as high as 2", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 20 is as big as 2", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        // Equality cases

        ctx = ParseHelper.getExpression("say 5 is as great as 5", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 2 is as strong as 2", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 6 is as high as 6", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 7 is as big as 7", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldHandleLessThanComparison() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say 15 is less than 10", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 22 is lower than 20", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 2 is weaker than 22", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 20 is smaller than 22", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldReturnFalseForLessThanComparisonOfEqualThings() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say 5 is less than 5", 0);
        assertFalse((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldHandleLessThanOrEqualComparison() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say 15 is as low as 10", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 22 is as little as 20", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 2 is as weak as 22", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 20 is as small as 22", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        // Equality cases
        ctx = ParseHelper.getExpression("say 15 is as low as 15", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 22 is as little as 22", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 2 is as weak as 2", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 3 is as small as 3", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }


    /*
   "02" < "10" is true because the lexicographical comparison between 0 and 1 shows that the first string is less than the second string.
     */
    @Test
    public void shouldHandleLexographicalComparison() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say \"02\" is less than \"10\"", 0);
        assertTrue((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say \"02\" is greater than \"10\"", 0);
        assertFalse((boolean) execute(new Expression(ctx)));
    }

    /* "1" is 1 evaluates to true because "1" gets converted to the number 1 */
    @Disabled("Needs casting support")
    @Test
    public void shouldHandleCastingInComparison() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say \"1\" is 1", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }

    /**
     * True < 10 is an error because 10 gets coerced into True due to the comparison with a boolean and there are no allowed
     * ordering comparisons between booleans.
     */
    @Test
    public void shouldNotAllowComparisonBetweenBooleanAndNumbers() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say true is less than 10", 0);
        assertThrows(Throwable.class, () -> execute(new Expression(ctx)));
    }

    /*    "2" ain't Mysterious evaluates to true because all types are non equal to mysterious, besides mysterious itself.
     */
    @Test
    public void shouldFindAnythingExceptMysteriousIsNotMysterious() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say \"2\" ain't Mysterious", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }

    /*
   all types are non equal to mysterious, besides mysterious itself
     */
    @Test
    public void shouldHaveMysteriousEqualToItself() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say mysterious ain't Mysterious", 0);
        assertFalse((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldTreatNothingAsEqualToFalse() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say wrong ain't nothing", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say right ain't nothing", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldTreatNothingAsEqualToZero() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say 0 ain't nothing", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ParseHelper.getExpression("say nothing ain't 0", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say 0 is nothing", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }

    // The spec is a bit vague on this, so this is based on observation in Satriani
    // (note that on concatenation, null is "null", but I haven't implemented that
    @Test
    public void shouldTreatNothingAsEqualToEmptyStringForComparisons() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say \"\" ain't nothing", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say \"\" is nothing", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldHaveNothingEqualToItself() {
        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("say nothing ain't gone", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = ParseHelper.getExpression("say nothing is gone", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }


    @Test
    public void shouldCreateResultHandlesOfTheCorrectTypeForStrings() {
        MethodCreator main;
        try (ClassCreator creator = ClassCreator.builder()
                                                .className("holder")
                                                .build()) {
            main = creator.getMethodCreator("main", void.class, String[].class);
        }

        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout \"hello\"");
        ResultHandle handle = new Expression(ctx).getResultHandle(main);

        // We can't interrogate the type directly, so read it from the string
        assertTrue(handle.toString()
                         .contains("type='Ljava/lang/String;'"), handle.toString());

    }

    @Test
    public void shouldCreateResultHandlesOfTheCorrectTypeForNumbers() {
        MethodCreator main;
        try (ClassCreator creator = ClassCreator.builder()
                                                .className("holder")
                                                .build()) {
            main = creator.getMethodCreator("main", void.class, String[].class);
        }

        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout 5");
        ResultHandle handle = new Expression(ctx).getResultHandle(main);

        // We can't interrogate the type directly, so read it from the string
        assertTrue(handle.toString()
                         .contains("type='D'"), handle.toString());

    }

    @Test
    public void shouldCreateResultHandlesOfTheCorrectTypeForBooleans() {
        MethodCreator main;
        try (ClassCreator creator = ClassCreator.builder()
                                                .className("holder")
                                                .build()) {
            main = creator.getMethodCreator("main", void.class, String[].class);
        }

        Rockstar.ExpressionContext ctx = ParseHelper.getExpression("shout ok");
        ResultHandle handle = new Expression(ctx).getResultHandle(main);

        // We can't interrogate the type directly, so read it from the string
        assertTrue(handle.toString()
                         .contains("type='Z'"), handle.toString());

    }

    private Object execute(Expression a) {
        DynamicClassLoader cl = new DynamicClassLoader();

        // The auto-close on this triggers the write
        try (ClassCreator creator = ClassCreator.builder()
                                                .classOutput(cl)
                                                .className("com.MyTest")
                                                .build()) {

            MethodCreator method = creator.getMethodCreator("method", Object.class)
                                          .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
            ResultHandle rh = a.getResultHandle(method);
            method.returnValue(rh);
        }

        try {
            Class<?> clazz = cl.loadClass("com.MyTest");
            return clazz.getMethod("method")
                        .invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Test error: " + e);
        }
    }
}