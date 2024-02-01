package org.example;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TestClassLoader;
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
import static org.junit.jupiter.api.Assertions.*;

/*
Note that even though we're just testing the expressions, they need to be couched in something like output statement to be
recognised.
 */
public class ExpressionTest {

    @BeforeEach
    public void clearState() {
        Variable.clearState();
    }

    @Test
    public void shouldParseIntegerLiterals() {
        Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout 5");
        Expression a = new Expression(ctx);
        // The number should be stored as a double, even though it was entered as an integer
        assertEquals(5d, a.getValue());
//     @Disabled("type chaos")        assertEquals(double.class, a.getValueClass());
    }

    @Test
    public void shouldParseIntegerLiteralsAsVariables() {
        // Pre-initialise the variable so there's enough information about types
        Rockstar.VariableContext vctx = new ParseHelper().getVariable("my thing is 5");
        Variable variable = new Variable(vctx, double.class);

        Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout my thing");
        Expression a = new Expression(ctx);
        // The 'value' is the variable name
        assertEquals("my__thing", a.getValue());
//     @Disabled("type chaos")        assertEquals(double.class, a.getValueClass());
    }

    /* Numbers in Rockstar are double-precision floating point numbers, stored according to the IEEE 754 standard.*/
    @Test
    public void shouldParseFloatingPointLiterals() {
        Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout 3.141");
        Expression a = new Expression(ctx);
        assertEquals(3.141, a.getValue());
//     @Disabled("type chaos")        assertEquals(double.class, a.getValueClass());
    }

    /*
empty , silent , and silence are aliases for the empty string ( "" ).
 */
    @Test
    public void shouldParseEmptyStringAliases() {
        Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout silence");
        Expression a = new Expression(ctx);
        assertEquals("", a.getValue());
//     @Disabled("type chaos")        assertEquals(String.class, a.getValueClass());

        ctx = new ParseHelper().getExpression("shout silent");
        a = new Expression(ctx);
        assertEquals("", a.getValue());

        ctx = new ParseHelper().getExpression("shout empty");
        a = new Expression(ctx);
        assertEquals("", a.getValue());
    }


    @Test
    public void shouldParseBooleanLiteralsForTrueCase() {
        Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout true");
        Expression a = new Expression(ctx);
        assertEquals(true, a.getValue());
//     @Disabled("type chaos")        assertEquals(boolean.class, a.getValueClass());

        ctx = new ParseHelper().getExpression("shout right");
        a = new Expression(ctx);
        assertEquals(true, a.getValue());

        ctx = new ParseHelper().getExpression("shout ok");
        a = new Expression(ctx);
        assertEquals(true, a.getValue());

        ctx = new ParseHelper().getExpression("shout yes");
        a = new Expression(ctx);
        assertEquals(true, a.getValue());
    }

    @Test
    public void shouldParseBooleanLiteralsForFalseCase() {
        Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout false");
        Expression a = new Expression(ctx);
        assertEquals(false, a.getValue());
//     @Disabled("type chaos")        assertEquals(boolean.class, a.getValueClass());

        ctx = new ParseHelper().getExpression("shout lies");
        a = new Expression(ctx);
        assertEquals(false, a.getValue());

        ctx = new ParseHelper().getExpression("shout wrong");
        a = new Expression(ctx);
        assertEquals(false, a.getValue());

        ctx = new ParseHelper().getExpression("shout no");
        a = new Expression(ctx);
        assertEquals(false, a.getValue());
    }

    @Test
    public void shouldParseNullConstants() {
        Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout nothing");
        Expression a = new Expression(ctx);
        assertEquals(NOTHING, a.getValue());

        ctx = new ParseHelper().getExpression("shout nobody");
        a = new Expression(ctx);
        assertEquals(NOTHING, a.getValue());

        ctx = new ParseHelper().getExpression("shout nowhere");
        a = new Expression(ctx);
        assertEquals(NOTHING, a.getValue());

        ctx = new ParseHelper().getExpression("shout gone");
        a = new Expression(ctx);
        assertEquals(NOTHING, a.getValue());

        // The value of nothing is context-dependent, but on its own it should stay null, so that other higher-level expressions and
        // output statements can do the right thing
        assertEquals(null, execute(a));

    }

    @Nested
    class Truthiness {
        @Test
        public void shouldReturnFalseForFalse() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say lies");
            Expression a = new Expression(ctx);

            assertEquals(false, execute(a, Expression.Context.BOOLEAN));
        }

        @Test
        public void shouldReturnFalseForNothing() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say nothing");
            Expression a = new Expression(ctx);

            assertEquals(false, execute(a, Expression.Context.BOOLEAN));
        }

        @Test
        public void shouldReturnFalseForMysterious() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say mysterious");
            Expression a = new Expression(ctx);

            assertEquals(false, execute(a, Expression.Context.BOOLEAN));
        }

        @Test
        public void shouldReturnFalseForZero() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say 0");
            Expression a = new Expression(ctx);

            assertEquals(false, execute(a, Expression.Context.BOOLEAN));
        }

        @Test
        public void shouldReturnTrueForTrue() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say ok");
            Expression a = new Expression(ctx);

            assertEquals(true, execute(a, Expression.Context.BOOLEAN));
        }

        @Test
        public void shouldReturnTrueForAString() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say \"hello\"");
            Expression a = new Expression(ctx);

            assertEquals(true, execute(a, Expression.Context.BOOLEAN));
        }

        @Test
        public void shouldReturnTrueForANumber() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say 42");
            Expression a = new Expression(ctx);

            assertEquals(true, execute(a, Expression.Context.BOOLEAN));
        }
    }


    @Nested
    @DisplayName("Arithmetic Operations")
    class Operations {

        @Test
        public void shouldHandleSimpleAddition() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout 3 + 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            double answer = (double) execute(a);
            assertEquals(9d, answer);
        }

        @Test
        public void shouldHandleAdditionWithAliases() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout 3 plus 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            double answer = (double) execute(a);
            assertEquals(9d, answer);

            ctx = new ParseHelper().getExpression("shout 8 with 5", 0);
            a = new Expression(ctx);
            answer = (double) execute(a);
            assertEquals(13d, answer);
        }

        @Test
        public void shouldHandleStringAddition() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say \"rock\" plus \"roll\"", 0);
            Expression e = new Expression(ctx);
            String answer = (String) execute(e);
            assertEquals("rockroll", answer);
        }

        @Test
        public void shouldFormatNumbersCorrectlyOnStringAddition() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say 99 plus \" red balloons\"", 0);
            Expression e = new Expression(ctx);
            String answer = (String) execute(e);
            assertEquals("99 red balloons", answer);

            ctx = new ParseHelper().getExpression("say \"blink\" plus 42", 0);
            e = new Expression(ctx);
            answer = (String) execute(e);
            assertEquals("blink42", answer);
        }

        @Test
        public void shouldHandleStringAdditionWithNull() {
            // Nothing coerces to "null" when added to a string (which is a bit confusing since on its own it's "")
            // String <plus> Null => Convert the null to "null"
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout nothing plus \" points\"", 0);
            // Make sure we have the right expression
            assertEquals("nothing plus \" points\"", ctx.getText());
            Expression a = new Expression(ctx);
            assertEquals("null points", execute(a));

            // Now swap the order
            ctx = new ParseHelper().getExpression("shout  \"points \" plus nothing", 0);
            assertEquals("\"points \" plus nothing", ctx.getText());
            a = new Expression(ctx);
            assertEquals("points null", execute(a));
        }

        @Test
        public void shouldHandleNumericAdditionWithNull() {
            // Nothing coerces to 0 when added to a number
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout nothing plus 42", 0);
            Expression a = new Expression(ctx);
            // Make sure we have the right expression
            assertEquals("nothing plus 42", ctx.getText());
            assertEquals(42d, execute(a));

            // Now swap the order
            ctx = new ParseHelper().getExpression("shout 42 plus nothing", 0);
            assertEquals("42 plus nothing", ctx.getText());
            a = new Expression(ctx);
            assertEquals(42d, execute(a));
        }

        @Test
        @Disabled("type chaos and also mysterious support")
        public void shouldHandleStringAdditionWithMysterious() {
            // String <plus> Mysterious => Convert the mysterious to "mysterious"
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout mysterious plus \" ways\"", 0);
            // Make sure we have the right expression
            assertEquals("mysterious plus \" ways\"", ctx.getText());
            Expression a = new Expression(ctx);
            assertEquals("mysterious ways", execute(a));
        }

        @Disabled("Spec is vague, so not worth worrying about")
        @Test
        public void shouldHandleNumericAdditionWithMysterious() {
            // Spec is vague on this, so using Satriani as a reference
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout mysterious plus 16");
            Expression a = new Expression(ctx);
            assertEquals("mysterious16", execute(a));
        }

        @Test
        public void shouldHandleSimpleSubtraction() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout 3 - 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            double answer = (double) execute(a);
            assertEquals(-3d, answer);
        }

        @Test
        public void shouldHandleSubtractionWithAliases() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout 3 minus 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            double answer = (double) execute(a);
            assertEquals(-3d, answer);

            ctx = new ParseHelper().getExpression("shout 8 without 5", 0);
            a = new Expression(ctx);
            answer = (double) execute(a);
            assertEquals(3d, answer);
        }

        @Test
        public void shouldHandleSimpleMultiplication() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout 3 * 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            double answer = (double) execute(a);
            assertEquals(18d, answer);
        }

        @Test
        public void shouldHandleMultiplicationWithAliases() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout 3 times 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            double answer = (double) execute(a);
            assertEquals(18d, answer);

            ctx = new ParseHelper().getExpression("shout 8 of 5", 0);
            a = new Expression(ctx);
            answer = (double) execute(a);
            assertEquals(40d, answer);
        }

        @Test
        public void shouldHandleSimpleDivision() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout 24/6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            double answer = (double) execute(a);
            assertEquals(4, answer);
        }

        @Test
        public void shouldHandleDivisionWithAliases() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout 3 over 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            double answer = (double) execute(a);
            assertEquals(0.5, answer);

            ctx = new ParseHelper().getExpression("shout 40 between 5", 0);
            a = new Expression(ctx);
            answer = (double) execute(a);
            assertEquals(8d, answer);
        }
    }

    @Nested
    @DisplayName("Logical Operations")
    class LogicalOperations {

        @Test
        public void shouldHandleNegatingBooleans() {
            // Tests the disjunction
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout not ok", 0);
            assertEquals(false, (boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("shout not wrong", 0);
            assertEquals(true, (boolean) execute(new Expression(ctx)));
        }

        @Test
        public void shouldHandleAndingBooleans() {
            // Tests the conjunction
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout true and true", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            boolean answer = (boolean) execute(a);
            assertEquals(true, answer);

            ctx = new ParseHelper().getExpression("shout true and false", 0);
            a = new Expression(ctx);
            assertNull(a.getValue());
            answer = (boolean) execute(a);
            assertEquals(false, answer);
        }

        @Test
        public void shouldHandleOringBooleans() {
            // Tests the disjunction
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout true or true", 0);
            assertEquals(true, execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("shout false or false", 0);
            assertEquals(false, execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("shout true or false", 0);
            assertEquals(true, execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("shout false or true", 0);
            assertEquals(true, execute(new Expression(ctx)));
        }

        @Test
        public void shouldHandleNoringBooleans() {
            // Tests the disjunction
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout true nor true", 0);
            assertEquals(false, (boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("shout false nor false", 0);
            assertEquals(true, (boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("shout true nor false", 0);
            assertEquals(false, (boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("shout false nor true", 0);
            assertEquals(false, (boolean) execute(new Expression(ctx)));
        }

        // false and 1 over 0 is false and does not produce an error for dividing by zero.
        @Disabled("short circuits still not working properly (or even short circuiting")
        @Test
        public void shouldShortCircuitLogicalOperations() {
            // Tests the conjunction
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout false and 1 over 0", 0);
            assertEquals(false, (boolean) execute(new Expression(ctx)));

            // Tests the disjunction
            ctx = new ParseHelper().getExpression("shout true or 1 over 0", 0);
            assertEquals(true, (boolean) execute(new Expression(ctx)));


            // Tests the joint denial
            ctx = new ParseHelper().getExpression("shout true nor 1 over 0", 0);
            assertEquals(false, (boolean) execute(new Expression(ctx)));
        }
    }


    @Nested
    @DisplayName("Comparisons")
    class Comparisons {
        @Test
        public void shouldHandleComparisonOfNumbers() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say 1 is 2", 0);
            assertFalse((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("shout 1 is 1", 0);
            assertTrue((boolean) execute(new Expression(ctx)));
        }

        @Test
        public void shouldHandleComparisonOfBooleans() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say ok is right", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("shout right is wrong", 0);
            assertFalse((boolean) execute(new Expression(ctx)));
        }

        @Test
        public void shouldHandleComparisonOfStrings() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say \"life\" is \"life\"", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say \"live\" is \"life\"", 0);
            assertFalse((boolean) execute(new Expression(ctx)));
        }

        @Disabled("This also fails to parse in the reference implementation")
        @Test
        public void shouldHandleAintComparisonOfStrings() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say 123 ain’t \"all that\"", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say \"Tommy\" ain’t \"Tommy\"", 0);
            assertFalse((boolean) execute(new Expression(ctx)));
        }

        @Test
        public void shouldHandleAintComparisonOfStringsWithNoApostrophe() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say \"Tommy\" aint \"nobody\"", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say \"Tommy\" aint \"Tommy\"", 0);
            assertFalse((boolean) execute(new Expression(ctx)));
        }

        @Test
        public void shouldHandleContractionSComparison() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say 123's 123", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 123's 124", 0);
            assertFalse((boolean) execute(new Expression(ctx)));
        }

        @Test
        public void shouldHandleGreaterThanComparison() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say 5 is greater than 10", 0);
            assertFalse((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 2 is stronger than 20", 0);
            assertFalse((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 20 is stronger than 2", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 20 is higher than 2", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 20 is bigger than 2", 0);
            assertTrue((boolean) execute(new Expression(ctx)));
        }

        @Test
        public void shouldReturnFalseForGreaterThanComparisonOfEqualThings() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say 5 is greater than 5", 0);
            assertFalse((boolean) execute(new Expression(ctx)));
        }

        @Test
        public void shouldHandleGreaterThanOrEqualComparison() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say 5 is as great as 10", 0);
            assertFalse((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 2 is as strong as 20", 0);
            assertFalse((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 20 is as strong as 2", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 20 is as high as 2", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 20 is as big as 2", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            // Equality cases

            ctx = new ParseHelper().getExpression("say 5 is as great as 5", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 2 is as strong as 2", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 6 is as high as 6", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 7 is as big as 7", 0);
            assertTrue((boolean) execute(new Expression(ctx)));
        }

        @Test
        public void shouldHandleLessThanComparison() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say 15 is less than 10", 0);
            assertFalse((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 22 is lower than 20", 0);
            assertFalse((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 2 is weaker than 22", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 20 is smaller than 22", 0);
            assertTrue((boolean) execute(new Expression(ctx)));
        }

        @Test
        public void shouldReturnFalseForLessThanComparisonOfEqualThings() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say 5 is less than 5", 0);
            assertFalse((boolean) execute(new Expression(ctx)));
        }

        @Test
        public void shouldHandleLessThanOrEqualComparison() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say 15 is as low as 10", 0);
            assertFalse((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 22 is as little as 20", 0);
            assertFalse((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 2 is as weak as 22", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 20 is as small as 22", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            // Equality cases
            ctx = new ParseHelper().getExpression("say 15 is as low as 15", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 22 is as little as 22", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 2 is as weak as 2", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say 3 is as small as 3", 0);
            assertTrue((boolean) execute(new Expression(ctx)));
        }

        /*
       "02" < "10" is true because the lexicographical comparison between 0 and 1 shows that the first string is less than the second string.
         */
        @Test
        public void shouldHandleLexographicalComparison() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say \"02\" is less than \"10\"", 0);
            assertTrue((boolean) execute(new Expression(ctx)));

            ctx = new ParseHelper().getExpression("say \"02\" is greater than \"10\"", 0);
            assertFalse((boolean) execute(new Expression(ctx)));
        }

        /* "1" is 1 evaluates to true because "1" gets converted to the number 1 */
        @Disabled("Needs casting support")
        @Test
        public void shouldHandleCastingInComparison() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say \"1\" is 1", 0);
            assertTrue((boolean) execute(new Expression(ctx)));
        }

        /**
         * True < 10 is an error because 10 gets coerced into True due to the comparison with a boolean and there are no allowed
         * ordering comparisons between booleans.
         */
        @Test
        public void shouldNotAllowComparisonBetweenBooleanAndNumbers() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say true is less than 10", 0);
            assertThrows(Throwable.class, () -> execute(new Expression(ctx)));
        }
    }

    /*    "2" ain't Mysterious evaluates to true because all types are non equal to mysterious, besides mysterious itself.
     */
    @Test
    public void shouldFindAnythingExceptMysteriousIsNotMysterious() {
        Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say \"2\" ain't Mysterious", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }

    /*
   all types are non equal to mysterious, besides mysterious itself
     */
    @Test
    public void shouldHaveMysteriousEqualToItself() {
        Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say mysterious ain't Mysterious", 0);
        assertFalse((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldTreatNothingAsEqualToFalse() {
        Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say wrong ain't nothing", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = new ParseHelper().getExpression("say right ain't nothing", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldTreatNothingAsEqualToZero() {
        Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say 0 ain't nothing", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        new ParseHelper().getExpression("say nothing ain't 0", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = new ParseHelper().getExpression("say 0 is nothing", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }

    // The spec is a bit vague on this, so this is based on observation in Satriani
    @Disabled("Unspecified in spec, hard to implement")
    @Test
    public void shouldTreatNothingAsEqualToEmptyStringForComparisons() {
        Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say \"\" ain't nothing", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = new ParseHelper().getExpression("say \"\" is nothing", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldHaveNothingEqualToItself() {
        Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("say nothing ain't gone", 0);
        assertFalse((boolean) execute(new Expression(ctx)));

        ctx = new ParseHelper().getExpression("say nothing is gone", 0);
        assertTrue((boolean) execute(new Expression(ctx)));
    }

    @Test
    public void shouldCreateResultHandlesOfTheCorrectTypeForStrings() {
        try (ClassCreator creator = ClassCreator.builder()
                .className("holder")
                .build()) {
            MethodCreator main = creator.getMethodCreator("main", void.class, String[].class);


            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout \"hello\"");
            ResultHandle handle = new Expression(ctx).getResultHandle(main, creator);

            // We can't interrogate the type directly, so read it from the string
            assertTrue(handle.toString()
                    .contains("type='Ljava/lang/String;'"), handle.toString());
        }
    }

    @Test
    public void shouldCreateResultHandlesOfTheCorrectTypeForNumbers() {
        try (ClassCreator creator = ClassCreator.builder()
                .className("holder")
                .build()) {
            MethodCreator main = creator.getMethodCreator("main", void.class, String[].class);


            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout 5");
            ResultHandle handle = new Expression(ctx).getResultHandle(main, creator);

            // We can't interrogate the type directly, so read it from the string
            assertTrue(handle.toString()
                    .contains("type='D'"), handle.toString());

        }
    }

    @Test
    public void shouldCreateResultHandlesOfTheCorrectTypeForBooleans() {
        try (ClassCreator creator = ClassCreator.builder()
                .className("holder")
                .build()) {
            MethodCreator main = creator.getMethodCreator("main", void.class, String[].class);


            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout ok");
            ResultHandle handle = new Expression(ctx).getResultHandle(main, creator);

            // We can't interrogate the type directly, so read it from the string
            assertTrue(handle.toString()
                    .contains("type='Z'"), handle.toString());

        }
    }

    /* This is a pretty limited test because functions depend on multi-line context to work. All we can really test is the return type. */
    @Test
    public void shouldHandleFunctionCalls() {
        // The parameter needs to be a literal so the expression can be self-contained in a single context
        String program = """
                Midnight takes your heart
                Give back "does not really matter as we cannot test execution in this kind of unit test"
                                      
                Ice is nice
                Say Midnight taking "ice"
                """;

        Rockstar.ExpressionContext ctx = new ParseHelper().getExpression(program, 1);
        TestClassLoader cl = new TestClassLoader(this.getClass().getClassLoader().getParent());
        try (ClassCreator creator = ClassCreator.builder()
                .classOutput(cl)
                .className("com.MyTest")
                .build()) {

            MethodCreator method = creator.getMethodCreator("method", Object.class)
                    .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
            ResultHandle handle = new Expression(ctx).getResultHandle(method, creator);

            // We can't really know the return type of a function, so go with Object
            assertTrue(handle.toString()
                    .contains("type='Ljava/lang/Object;'"), handle.toString());
        }
    }

    // This is a whole section of implementation, but handle some simple cases
    @Nested
    @DisplayName("Types of operations on types")
    @Disabled("type chaos")
    class TypeTests {
        @Test
        public void shouldInferASuitableTypeForMultiplicationOfTwoNumbers() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout 3 times 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            assertEquals(double.class, a.getValueClass());
        }

        // String <times> Number => String gets repeated <Number> times
        @Test
        public void shouldInferASuitableTypeForMultiplicationOfANumberAndAString() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout \"hello\" times 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            assertEquals(String.class, a.getValueClass());
        }

        @Test
        public void shouldInferASuitableTypeForSubtractionOfTwoNumbers() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout 3 minus 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            assertEquals(double.class, a.getValueClass());
        }

        @Test
        public void shouldInferASuitableTypeForAdditionOfTwoNumbers() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout 3 plus 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            assertEquals(double.class, a.getValueClass());
        }

        // String <times> Number => String gets repeated <Number> times
        @Test
        public void shouldInferASuitableTypeForAdditionOfANumberAndAString() {
            Rockstar.ExpressionContext ctx = new ParseHelper().getExpression("shout \"hello\" plus 6", 0);
            Expression a = new Expression(ctx);
            assertNull(a.getValue());
            assertEquals(String.class, a.getValueClass());
        }
    }

    private Object execute(Expression a) {
        return execute(a, Expression.Context.NORMAL);
    }

    private Object execute(Expression a, Expression.Context context) {
        TestClassLoader cl = new TestClassLoader(this.getClass().getClassLoader().getParent());

        // The auto-close on this triggers the write
        try (ClassCreator creator = ClassCreator.builder()
                .classOutput(cl)
                .className("com.MyTest")
                .build()) {

            MethodCreator method = creator.getMethodCreator("method", Object.class)
                    .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
            ResultHandle rh = a.getResultHandle(method, creator, context);
            method.returnValue(rh);
        }

        try {
            Class<?> clazz = cl.loadClass("com.MyTest");
            return clazz.getMethod("method")
                    .invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException("Test error: " + e);
        }
    }
}