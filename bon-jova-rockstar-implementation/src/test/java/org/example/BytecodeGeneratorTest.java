package org.example;

import io.quarkus.gizmo.TestClassLoader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BytecodeGeneratorTest {

    @Test
    public void shouldIgnoreComments() {
        String program = "Shout 1\n(Shout 2)\nShout 3\n";
        String output = compileAndLaunch(program);
        assertEquals("1\n3\n", output);
    }

    @Test
    public void shouldHandleSimpleStringLiterals() {
        String program = """
                Shout "Hello San Francisco"
                                            """;
        String output = compileAndLaunch(program);
        assertEquals("Hello San Francisco\n", output);
    }

    @Test
    public void shouldHandleSimpleNumericLiterals() {
        String program = "Shout 31";
        String output = compileAndLaunch(program);
        assertEquals("31\n", output);

        // Output should be rounded even of entered with decimals
        program = "Shout 34.0";
        output = compileAndLaunch(program);
        assertEquals("34\n", output);

        program = "Shout 34.2";
        output = compileAndLaunch(program);

        var expected = BigDecimal.valueOf(34.2);
        assertEquals("%.1f%n".formatted(expected), output);
    }

    /*
    Simple variables are valid identifiers that are not language keywords. A simple variable name must contain only letters, and cannot
    contain spaces.
     */
    @Test
    public void shouldHandleSimpleVariableNames() {
        String program = """
                Variable is "Hello San Francisco"
                Shout Variable
                                            """;
        String output = compileAndLaunch(program);

        assertEquals("Hello San Francisco\n", output);
    }

    /*
     Common variables consist of one of the keywords a , an , the , my , your or our followed by whitespace and a unique variable name,
     which must contain only lowercase ASCII letters a-z. The keyword is part of the variable name, so a boy is a different variable from
      the boy . Common variables are case-insensitive.
     */
    @Test
    public void shouldHandleCommonVariableNames() {
        String program = """
                My thing is true
                Shout my thing
                                            """;
        String output = compileAndLaunch(program);

        assertEquals("true\n", output);
    }

    /*
    Proper variables are multi-word proper nouns - words that aren't language keywords, each starting with an uppercase letter, separated
     by spaces. (Single-word variables are always simple variables.) Whilst some developers may use this feature to create variables with
      names like Customer ID , Tax Rate or Distance In KM , we recommend you favour idiomatic variable names such as
Doctor Feelgood , Mister Crowley , Tom Sawyer , and Billie Jean .
(Although not strictly idiomatic, Eleanor Rigby , Peggy Sue , Black Betty , and Johnny B Goode would also all be valid variable
names in Rockstar.)
     */
    @Test
    public void shouldHandleProperVariableNames() {
        String program = """
                Doctor Feelgood is a good fellow
                Shout Doctor Feelgood
                Shout Doctor FeelGOOD
                                            """;
        String output = compileAndLaunch(program);

        assertEquals("146\n146\n", output);
    }

    @Test
    public void shouldTreatVariableNamesAsCaseInsensitive() {
        String program = """
                time is an illusion
                Shout time
                Shout tIMe
                Shout TIMe
                                            """;
        String output = compileAndLaunch(program);

        assertEquals("28\n28\n28\n", output);
    }

    @Test
    public void shouldAllowAllUpperCaseVariableNames() {
        String program = """
                TIME is an illusion
                Shout time
                Shout tIMe
                Shout TIMe
                                            """;
        String output = compileAndLaunch(program);

        assertEquals("28\n28\n28\n", output);
    }

    @Test
    public void shouldUseApostrophesForAssignmentAndIgnoreOtherApostrophes() {
        String program = """
                The fire's burning Tommy's feet 
                Shout the fire
                                            """;
        String output = compileAndLaunch(program);

        assertEquals("764\n", output);
    }

    @Test
    public void shouldHandleNothingInAVariable() {
        String program = """
                My world is nothing 
                Shout my world
                                            """;
        String output = compileAndLaunch(program);

        assertEquals("\n", output);
    }

    @Test
    public void shouldHandleNothingInALiteral() {
        String program = """
                Shout nothing
                                            """;
        String output = compileAndLaunch(program);

        assertEquals("\n", output);
    }

    @Test
    public void shouldNotBeSensitiveToWhitespaceAtBeginningAndEndOfLines() {
        String program = """
                Time is an illusion
                  Shout time
                    Shout time
                Shout time        
                  
                Shout time
                                            """;
        String output = compileAndLaunch(program);

        assertEquals("28\n28\n28\n28\n", output);
    }

    // This is taken from the sample of 99 bottles on the try it out site
    @Test
    public void shouldNotBeSensitiveToWhitespaceInTheMiddleOfLines() {
        String program = """
                Your heart  says bottles of beer on the wall
                Say  it with your heart
                                                            """;
        String output = compileAndLaunch(program);

        assertTrue(output.matches("bottles of beer on the wallbottles of beer on the wall\n"));
    }

    @Test
    public void shouldNotBeSensitiveToLotsOfWhitespaceInTheMiddleOfLines() {
        String program = """
                Your heart   says bottles of beer on the wall
                Say  it  with   your heart
                                                            """;
        String output = compileAndLaunch(program);

        assertTrue(output.matches("bottles of beer on the wallbottles of beer on the wall\n"));
    }

    @Test
    public void shouldNotBeSensitiveToWhitespaceInCommonVariableNames() {
        String program = """
                Your heart says bottles of beer on the wall
                Say it with your    heart
                                                            """;
        String output = compileAndLaunch(program);

        assertTrue(output.matches("bottles of beer on the wallbottles of beer on the wall\n"));
    }


    // This is taken from the sample of 99 bottles on the try it out site
    // The reference implementation attaches the leading whitespace to the string literal, rather than ignoring it
    @Test
    public void shouldIncludeWhitespaceInPoeticStringLiteralDeclarations() {
        String program = """
                Your heart says  bottles of beer on the wall
                Say it with your heart
                                                            """;
        String output = compileAndLaunch(program);

        // the reference implementation attaches the leading whitespace to the string literal, rather than ignoring it
        assertEquals(" bottles of beer on the wall bottles of beer on the wall\n", output);
    }

    /*
 Common variables consist of one of the keywords a , an , the , my , your or our followed by whitespace and a unique variable name,
 which must contain only lowercase ASCII letters a-z. The keyword is part of the variable name, so a boy is a different variable from
  the boy . Common variables are case-insensitive.
 */
    @Test
    public void shouldHandleVariableAssignmentToBoolean() {
        String program = """
                My thing is true
                Shout my thing
                                            """;
        String output = compileAndLaunch(program);

        assertEquals("true\n", output);
    }

    /*
        Common variables consist of one of the keywords a , an , the , my , your or our followed by whitespace and a unique variable name,
        which must contain only lowercase ASCII letters a-z. The keyword is part of the variable name, so a boy is a different variable from
        the boy . Common variables are case-insensitive.
    */
    @Test
    public void shouldHandleVariableAssignmentToInteger() {
        String program = """
                My thing is 5
                Shout my thing
                                            """;
        String output = compileAndLaunch(program);

        // Even though is stored as a double internally, we want (and the spec expects) an integer-format output
        assertEquals("5\n", output);
    }

    @Test
    public void shouldHandleVariableAssignmentToFloatingPoint() {
        String program = """
                My thing is 4.89
                Shout my thing
                                            """;
        String output = compileAndLaunch(program);

        var expected = BigDecimal.valueOf(4.89);
        assertEquals("%.2f%n".formatted(expected), output);
    }

    /*
     * This is the starting example on https://codewithrockstar.com/online
     * It exercises variables, poetic number literals, and console output
     */
    @Test
    public void shouldHandleVariableAssignmentToPoeticNumberLiterals() {
        String program = """
                Rockstar is a big bad monster
                Shout Rockstar
                """;
        String output = compileAndLaunch(program);
        String leet = "1337\n";

        assertEquals(leet, output);
    }

    // As long as the next symbol is not a Literal Word, the rest of the line is treated as a decimal number in which the values of consecutive digits are given by the lengths of the subsequent barewords, up until the end of the line.
    @Test
    public void shouldHandleNoInPoeticLiterals() {
        String program = """
                The tide is low. A ball flung-about, beach abandoned, no soiree beats the shore
                                             Whisper the tide
                                """;
        assertEquals("3.141592654\n", compileAndLaunch(program));

    }

    /*
     * Put 123 into X will assign the value 123 to the variable X
     */
    @Test
    public void shouldHandlePutIntoVariableAssignment() {
        String program = """
                X is 60
                Put 123 into X
                Shout X
                """;
        String output = compileAndLaunch(program);
        assertEquals("123\n", output);
    }

    /*
     * Put "Hello San Francisco" into the message will assign the value "Hello San Francisco" to the variable the message Let my balance
     */
    @Test
    public void shouldHandlePutVariableAssignmentForStrings() {
        String program = """
                The message is empty
                Put "Hello San Francisco" into the message
                Shout the message
                """;
        String output = compileAndLaunch(program);
        assertEquals("Hello San Francisco\n", output);
    }

    /*
     *  Let my balance be 1000000 will store the value 1000000 in the variable my balance
     */
    @Test
    public void shouldHandleLetVariableAssignment() {
        String program = """
                Let my balance be 1000000
                Shout my balance
                """;
        String output = compileAndLaunch(program);
        assertEquals("1000000\n", output);
    }

    @Test
    public void shouldHandleVariableReassignment() {
        String program = """
                My thing is 5
                My thing is 8
                Shout my thing
                                            """;
        String output = compileAndLaunch(program);

        assertEquals("8\n", output);

        program = """
                My thing is "hello"
                My thing is "goodbye"
                Shout my thing
                                            """;
        output = compileAndLaunch(program);

        assertEquals("goodbye\n", output);
    }

    @Test
    public void shouldIgnoreLogicalOperatorsInAssignment() {
        String program = """
                Anticipation is ok
                Fear is wrong
                Hope is anticipation and fear
                Say hope
                """;
        assertEquals("234\n", compileAndLaunch(program));
// Surprise! Satriani says 234 for this, rather than a logical operation
    }

    @Nested
    @DisplayName("List arithmetic")
    class ListArithmetic {
        // Rockstar operators support a list of expressions on the right-hand side of the operator. (Imagine explaining in English that, say, “the restaurant bill is the food, plus the drinks, the service, and the tax” - same idea.)
        @Test
        public void shouldHandleNumericListsInAdditionAssignments() {

            String program = """
                    Let X be 1 with 2, 3, 4
                    Say X
                    """;
            assertEquals("10\n", compileAndLaunch(program));

        }

        @Test
        public void shouldHandleNumericListsInSubtractionAssignments() {

            String program = """
                    Let X be 1 without 2, 3, 4
                    Say X
                    """;
            assertEquals("-8\n", compileAndLaunch(program));
        }

        @Test
        public void shouldHandleNumericListsInDivisionAssignments() {

            String program = """
                    Let X be 480 over 2, 3, 4
                    Say X
                    """;
            assertEquals("20\n", compileAndLaunch(program));
        }

        @Test
        public void shouldHandleStringListsInAdditionAssignments() {

            String program = """
                    Let Y be "foo" with "bar", "baz"
                    Say Y
                    """;
            assertEquals("foobarbaz\n", compileAndLaunch(program));

        }

        @Test
        public void shouldHandleStringListsAndExtraWordsInAdditionAssignments() {

            String program = """
                    Let Y be "foo" with "bar", and "baz"
                    Say Y
                    """;
            assertEquals("foobarbaz\n", compileAndLaunch(program));

        }

        @Test
        public void shouldHandleNumericListsInMultiplicationAssignments() {

            String program = """
                    Let X be 5 times 2, 2, 2
                    Say X
                    """;
            assertEquals("40\n", compileAndLaunch(program));

        }

        @Disabled("Multiplication of strings not yet implemented")
        @Test
        public void shouldHandleStringListsInMultiplicationAssignments() {

            String program = """
                    Let X be "foo" times 2, 2, 2
                    Say X
                    """;
            assertEquals("foofoofoofoofoofoofoofoo\n", compileAndLaunch(program));

        }

        // List arithmetic is only possible where the result type supports further operations.
        //       Let X be 2 times "foo", "bar" - is mysterious (because 2 * foo = "foofoo", and "foofoo" * "bar" is undefined)
        @Disabled("Multiplication of strings not yet implemented")
        @Test
        public void shouldGracefullyHandleListsInStringMultiplicationAssignments() {
            String program = """
                    Let X be 2 times "foo", "bar"
                    Say X
                    """;
            assertEquals("mysterious\n", compileAndLaunch(program));
        }
    }

    @Test
    public void shouldHandleStringLiteralPronounReferences() {
        String program = """
                The message is "pass"
                say it
                """;

        String output = compileAndLaunch(program);
        assertEquals("pass\n", output);
    }

    @Test
    public void shouldHandleNumericLiteralPronounReferences() {
        String program = """
                Gina is 25
                say it
                Shout it
                Whisper it
                Scream it
                                """;

        String output = compileAndLaunch(program);
        assertEquals("25\n25\n25\n25\n", output);
    }

    @Test
    public void shouldHandleOperationsOnPronounReferences() {
        String program = """
                The message is "pass"
                say it
                say it plus "!"
                """;
        String output = compileAndLaunch(program);

        assertEquals("pass\n" +
                "pass!\n", output);

    }

    @Test
    public void shouldHandleRoundingOnPronounReferences() {
        String program = """
                My heart is on fire. Aflame with desire.
                Turn it up.
                Shout it.
                """;
        String output = compileAndLaunch(program);

        assertEquals("25\n", output);

    }


    @Test
    public void shouldHandleIncrementForSimpleCase() {
        String program = """
                My world is 0
                Build my world up
                Build my world up
                Say my world
                """;
        String output = compileAndLaunch(program);

        assertEquals("2\n", output);
    }

    @Test
    public void shouldHandleIncrementForRepetitionWithCommasCase() {
        String program = """
                My world is 0
                Build my world up, up, up
                Say my world
                """;
        String output = compileAndLaunch(program);

        assertEquals("3\n", output);
    }

    @Test
    public void shouldHandleIncrementForRepetitionWithoutCommasCase() {
        String program = """
                My world is 0
                Build my world up, up up
                Say my world
                """;
        String output = compileAndLaunch(program);

        assertEquals("3\n", output);
    }


    @Test
    public void shouldHandleIncrementForPronounCase() {
        String program = """
                My world is 1
                Build it up
                Build it up
                Say my world
                """;
        String output = compileAndLaunch(program);

        assertEquals("3\n", output);

    }

    @Test
    public void shouldHandleIncrementForDecimalCase() {
        String program = """
                My world is 3.141
                Build my world up
                Build my world up
                Say my world
                """;
        String output = compileAndLaunch(program);

        var expected = BigDecimal.valueOf(5.141);
        assertEquals("%.3f%n".formatted(expected), output);
    }

    @Test
    public void shouldHandleDecrementForSimpleCase() {
        String program = """
                The walls is 0
                Knock the walls down
                Knock the walls down
                Shout the walls
                """;
        String output = compileAndLaunch(program);

        assertEquals("-2\n", output);
    }

    @Test
    public void shouldHandleDecrementForPronounCase() {
        String program = """
                The walls is 1
                Knock them down
                Knock them down
                Shout the walls
                """;
        String output = compileAndLaunch(program);

        assertEquals("-1\n", output);
    }

    @Test
    public void shouldHandleDecrementForDecimalCase() {
        String program = """
                The walls is 1.42
                Knock the walls down
                Knock the walls down
                Shout the walls
                """;
        String output = compileAndLaunch(program);

        // Floating points are hard! Satriani also gives this as -0.5800000000000001, and a simple "1.42 - 2.0" also gives that result
        // Here, because we have to round to handle integers correctly, we can set a maximum precision that's shorter than the point
        // where floats go weird
        var expected = BigDecimal.valueOf(-0.58);
        assertEquals("%.2f%n".formatted(expected), output);
    }

    @Test
    public void shouldHandleDecrementForRepetitionWithCommasCase() {
        String program = """
                My world is 4
                Knock my world down, down, down
                Say my world
                """;
        String output = compileAndLaunch(program);

        assertEquals("1\n", output);
    }

    @Test
    public void shouldHandleDecrementForRepetitionWithoutCommasCase() {
        String program = """
                My world is 4
                Knock my world down down, down
                Say my world
                """;
        String output = compileAndLaunch(program);

        assertEquals("1\n", output);
    }

    @Test
    public void shouldHandleIncrementForStringCase() {
        String program = """
                My world is "hello"
                Build my world up
                Build my world up
                Say my world
                """;
        String output = compileAndLaunch(program);

        assertEquals("hello11\n", output);
    }

    @Test
    public void shouldHandleDecrementForStringCase() {
        String program = """
                My world is "hello"
                Knock my world down
                Knock my world down
                Say my world
                """;
        String output = compileAndLaunch(program);

        assertEquals("NaN\n", output);
    }

    @Test
    public void shouldHandleIncrementStartingWithNull() {
        String program = """
                Desire is large
                My world is nothing
                Until my world is Desire,
                Build my world up
                shout my world
                """;
        assertEquals("1\n2\n3\n4\n5\n", compileAndLaunch(program));
    }

    @Test
    public void shouldHandleDecrementStartingWithNull() {
        String program = """
                Desire is -5
                My world is nothing
                Until my world is Desire,
                Knock my world down
                shout my world
                """;
        assertEquals("-1\n-2\n-3\n-4\n-5\n", compileAndLaunch(program));
    }

    // No need for decrement for a string, it's NaN in Satriani

    // Rockstar supports the infix arithmetic operators +, -, * and /
    @Test
    public void shouldHandleSimpleAdditionOfVariables() {
        String program = """
                My world is 4
                Soil is 1
                Say my world + soil
                """;
        String output = compileAndLaunch(program);

        // Counter-intuitive, but what Satriani does
        assertEquals("5\n", output);
    }


    @Test
    public void shouldTreatAdditionInVariablesAsPoeticStringLiterals() {
        String program = """
                My world is 4
                Soil is 1
                My universe is my world + soil
                Say my universe
                """;
        String output = compileAndLaunch(program);

        // Counter-intuitive, but what Satriani does - on the right side of a variable assignment, values are poetic string literals
        // The hyphen (-) is counted as a letter – so you can use terms like ‘all-consuming’ (13 letters > 3) and ‘power-hungry’ (12
        // letters > 2) instead of having to think of 12- and 13-letter words.
        //         The semi-colon, comma, apostrophe and any other non-alphabetical characters are ignored.
        assertEquals("254\n", output);
    }


    @Test
    public void shouldHandleArithmeticWithVariablesAndPut() {

        String program = """
                Your heart is love
                The whole is happy
                My hands is awaiting
                Put the whole of your heart into my hands
                Say my hands
                """;
        String output = compileAndLaunch(program);

        assertEquals("20\n", output);
    }

    @Test
    public void shouldHandleInitializeVariableOnAssignment() {

        // No declaration of my hands before put-ing into it
        String program = """
                Your heart is love
                The whole is happy
                Put the whole of your heart into my hands
                Say my hands
                """;
        String output = compileAndLaunch(program);

        assertEquals("20\n", output);
    }

    @Test
    public void shouldHandleAreAssignments() {
        String program = """
                Your heart is true
                Your stories are ok
                If your heart ain't wrong and your stories ain't lies
                Say "You're a trustworthy friend"
                Else
                Whisper "Would not recommend"
                                """;
        assertEquals("You're a trustworthy friend\n", compileAndLaunch(program));

    }

    @Test
    public void shouldHandleDivisionWithVariablesAndPut() {

        String program = """
                My heart is so full
                The moon is aflame
                Let my heart be my heart over the moon
                Whisper my heart
                """;
        String output = compileAndLaunch(program);

        assertEquals("4\n", output);
    }

    @Test
    public void shouldHandleDivisionThatResultsInRepeatingDecimals() {

        String program = """
                My heart is full
                The moon is aflame
                Let my heart be my heart over the moon
                Whisper my heart
                """;
        String output = compileAndLaunch(program);

        var expected = BigDecimal.valueOf(2.0 / 3);
        assertEquals("%.7f%n".formatted(expected), output);
    }

    @Test
    public void shouldHandleComparisons() {
        String program = """
                Say 1 is 2
                """;
        String output = compileAndLaunch(program);

        assertEquals("false\n", output);
    }

    @Nested
    @DisplayName("Conditionals and flow control")
    class FlowControl {
        @Test
        public void shouldHandleConditionalsForTrueCase() {
            // Positive case
            String program = """
                    Tommy is a big bad monster
                    If Tommy is 1337
                    Say "he is bad"
                    Say "end line"
                    """;
            String output = compileAndLaunch(program);

            assertEquals("he is bad\nend line\n", output);
        }

        @Test
        public void shouldHandleConditionalsForAintCheck() {
            // Positive case
            String program = """
                    Tommy is a big bad monster
                    If Tommy ain't 1337
                    Say "main case"
                    Else
                    Say "else case"
                    """;
            String output = compileAndLaunch(program);

            assertEquals("else case\n", output);
        }

        @Test
        public void shouldHandleConditionalsForFalseCase() {
            // Negative case
            String program = """
                     Tommy is a big bad monster
                     If Tommy is 133333373737777
                     Say "he is very bad"
                    """;
            String output = compileAndLaunch(program);

            assertEquals("", output);
        }


        @Test
        public void shouldHandleIfElseConditionalsForTrueBranch() {
            String program = """
                    Tommy is a big bad monster
                    If Tommy is 1337
                    Say "he is bad"
                    Say "this is still the true branch"
                    Else
                    Say "he is good"
                    """;
            String output = compileAndLaunch(program);

            assertEquals("he is bad\nthis is still the true branch\n", output);
        }

        @Test
        public void shouldHandleIfElseConditionalsForFalseBranch() {
            String program = """
                    Tommy is a big bad monster
                    If Tommy is 13337737373337
                    Say "this is the true branch"
                    Else
                    Say "this is the false branch"
                    """;
            String output = compileAndLaunch(program);

            assertEquals("this is the false branch\n", output);
        }

        @Test
        public void shouldHandleConsecutiveConditionals() {
            String program = """
                    Midnight takes your heart and your soul
                    Give back 0
                                    
                    My world is nothing
                    Fire is ice
                                    
                    If Midnight taking my world, Fire is nothing
                    Shout "FizzBuzz!"
                                    
                    If Midnight taking my world, Fire is nothing
                    Shout "Fizz!"
                    """;

            assertEquals("FizzBuzz!\nFizz!\n", compileAndLaunch(program));
        }

        @Test
        public void shouldHandleWhileLoops() {
            String program = """
                    Tommy was shy
                    While Tommy ain't 0,
                    Knock Tommy down
                    Say Tommy
                    """;
            String output = compileAndLaunch(program);

            String expected = """
                    2
                    1
                    0
                    """;
            assertEquals(expected, output);
        }

        @Test
        public void shouldWhileLoopsInAFunction() {
            String program = """
                    Midnight takes your heart and your soul
                    While your heart is as high as your soul
                    Put your heart without your soul into your heart

                    Give back your heart

                    My world is "got this far"
                    Whisper my world
                                    """;

            assertEquals("got this far\n", compileAndLaunch(program));
        }

        @Test
        public void shouldHandleUntilLoops() {
            String program = """
                    Tommy was nice
                    Until Tommy is 0,
                    Knock Tommy down
                    Say Tommy
                    """;
            String output = compileAndLaunch(program);

            String expected = """
                    3
                    2
                    1
                    0
                    """;
            assertEquals(expected, output);
        }

        @Test
        public void shouldHandleBreakInConditional() {
            String program = """
                    Tommy is a big bad monster
                    If Tommy is 1337
                    Say "he is bad"
                    Break
                    Say "this is after the break"
                    Else
                    Say "he is good"
                    """;
            String output = compileAndLaunch(program);
            assertEquals("he is bad\n", output);

            program = """
                    Tommy is a big bad monster
                    If Tommy is 1337
                    Say "he is bad"
                    Break it down
                    Say "this is after the break"
                    Else
                    Say "he is good"
                    """;
            output = compileAndLaunch(program);
            assertEquals("he is bad\n", output);
        }

        @Test
        public void shouldHandleBreakInWhileLoops() {
            String program = """
                    Tommy was shy
                    While Tommy ain't 0,
                    Say Tommy
                    Say "before the break"
                    Knock Tommy down
                    Break it down
                    Say "after the break"
                    """;
            String output = compileAndLaunch(program);

            assertEquals("3\nbefore the break\n", output);
        }

        @Test
        public void shouldHandleContinueInWhileLoops() {
            String program = """
                    Tommy was shy
                    While Tommy ain't 0,
                    Say Tommy
                    Say "before the break"
                    Knock Tommy down
                    Take it to the top
                    Say "after the break"
                    """;
            String output = compileAndLaunch(program);

            String expected = """
                    3
                    before the break
                    2
                    before the break
                    1
                    before the break
                    """;
            assertEquals(expected, output);
        }

        @Test
        public void shouldHandleContinueInIfs() {
            String program = """
                    Tommy was shy
                    If Tommy ain't 0,
                    Say Tommy
                    Say "before the break"
                    Knock Tommy down
                    Take it to the top
                    Say "after the break\"
                    """;
            String output = compileAndLaunch(program);

            String expected = """
                    3
                    before the break
                                    """;
            assertEquals(expected, output);
        }

        @Test
        public void shouldHandleLooseContinues() {
            String program = """
                    Say "before the continue"
                    Take it to the top
                    Say "after the continue"
                    """;
            String output = compileAndLaunch(program);

            assertEquals("before the continue\n", output);
        }
    }

    @Test
    public void shouldReadFromStdin() {
        String program = """
                Listen to your voices
                Listen to your heart
                Shout your voices
                Whisper your heart
                """;
        String output = compileAndLaunch(program, "Tommy", "Gina");

        assertEquals("Tommy\nGina\n", output);
    }

    // Functions are called using the ‘taking’ keyword and must have at least one argument.
    //

    @Nested
    class Functions {

        @Test
        public void shouldHandleSingleArgumentFunctionsOnStrings() {
            String program = """
                    Midnight takes your heart
                    Give back your heart
                                           
                    Ice is "nice"
                    Say Midnight taking ice
                    """;
            String output = compileAndLaunch(program);

            // Function returns argument, which is 4
            assertEquals("nice\n", output);
        }

        @Test
        public void shouldHandleReturnInFunction() {
            String program = """
                    Midnight takes your heart
                    Return your heart
                                           
                    Ice is "nice"
                    Say Midnight taking ice
                    """;
            String output = compileAndLaunch(program);

            // Function returns argument, which is 4
            assertEquals("nice\n", output);
        }

        @Test
        public void shouldHandleSendInFunction() {
            String program = """
                    Midnight takes your heart
                    Send back your heart
                                           
                    Ice is "nice"
                    Say Midnight taking ice
                    """;
            String output = compileAndLaunch(program);

            // Function returns argument, which is 4
            assertEquals("nice\n", output);
        }

        @Test
        public void shouldHandleGiveWithoutBackInFunction() {
            String program = """
                    Midnight takes your heart
                    Give your heart
                                           
                    Ice is "nice"
                    Say Midnight taking ice
                    """;
            String output = compileAndLaunch(program);

            // Function returns argument, which is 4
            assertEquals("nice\n", output);
        }

        @Test
        public void shouldHandleTrailingBackInFunction() {
            String program = """
                    Midnight takes your heart
                    Give your heart back
                                           
                    Ice is "nice"
                    Say Midnight taking ice
                    """;
            String output = compileAndLaunch(program);

            // Function returns argument, which is 4
            assertEquals("nice\n", output);
        }

        @Test
        public void shouldHandleOperationsInSingleArgumentFunctionsOnStrings() {
            String program = """
                    Midnight takes your heart
                    Give back your heart plus " was the param"
                                           
                    Ice is "nice"
                    Say Midnight taking ice
                    """;
            String output = compileAndLaunch(program);

            // Function returns argument, which is 4
            assertEquals("nice was the param\n", output);
        }

        @Test
        public void shouldHandleSingleArgumentFunctionsOnNumbers() {
            String program = """
                    Midnight takes your heart
                    Give back your heart
                                           
                    Ice is nice
                    Say Midnight taking ice
                    """;
            String output = compileAndLaunch(program);

            // Function returns argument, which is 4
            assertEquals("4\n", output);
        }

        @Test
        public void shouldHandleMathematicalOperationsInSingleArgumentFunctionsOnNumbers() {
            String program = """
                    Midnight takes your heart
                    Give back your heart plus 2
                                           
                    Ice is nice
                    Say Midnight taking ice
                    """;
            String output = compileAndLaunch(program);

            // Function returns argument, which is 4
            assertEquals("6\n", output);
        }

        @Test
        public void shouldHandleMultiArgumentFunctionsOnStringVariables() {
            String program = """
                    Midnight takes your heart and your soul
                    Give back your heart plus your soul
                                           
                    Carol is "nice"
                    Bob is "less nice"
                    Say Midnight taking Carol, Bob
                    """;
            String output = compileAndLaunch(program);

            // Function multiplies arguments, 4*3 is 12
            assertEquals("niceless nice\n", output);

            program = """
                    Midnight takes your heart and your soul
                    Give back your heart plus your soul
                                           
                    Carol says Morning
                    Bob says Evening
                    Say Midnight taking Carol & Bob
                    """;

            assertEquals("MorningEvening\n", compileAndLaunch(program));

            program = """
                    Midnight takes your heart and your soul
                    Give back your heart plus your soul
                                           
                    Ice is "nicest"
                    Fire is "hottest"
                    Say Midnight taking ice 'n' fire
                    """;

            assertEquals("nicesthottest\n", compileAndLaunch(program));
        }

        @Test
        public void shouldHandleMultiArgumentFunctionsOnStringLiterals() {
            String program = """
                    Midnight takes your heart and your soul
                    Give back your heart plus your soul
                                           
                    Say Midnight taking "one", "two"
                    """;
            String output = compileAndLaunch(program);

            // Function multiplies arguments, 4*3 is 12
            assertEquals("onetwo\n", output);
        }

        @Test
        public void shouldHandleMultiArgumentFunctionsOnStringMixOfLiteralsAndVariables() {
            String program = """
                    Midnight takes your heart and your soul
                    Give back your heart plus your soul
                                           
                    Thing is "three"
                    Say Midnight taking "one", thing
                    """;
            String output = compileAndLaunch(program);

            // Function multiplies arguments, 4*3 is 12
            assertEquals("onethree\n", output);
        }

        @Test
        public void shouldHandleMultiArgumentFunctionsOnStringMixOfLiteralsAndConstants() {
            String program = """
                    Midnight takes your heart and your soul
                    Give back your heart plus your soul
                                           
                    Thing is "three"
                    Say Midnight taking "one", right
                    """;
            String output = compileAndLaunch(program);

            // Function multiplies arguments, 4*3 is 12
            assertEquals("onetrue\n", output);
        }

        @Test
        public void shouldHandleMultiplicationInMultiArgumentFunctionsOnNumbers() {
            String program = """
                    Midnight takes your heart and your soul
                    Give back your heart of your soul
                                           
                    Ice is nice
                    Fire is hot
                    Say Midnight taking ice, fire
                    """;
            String output = compileAndLaunch(program);

            // Function multiplies arguments, 4*3 is 12
            assertEquals("12\n", output);

            // Multiple arguments are separated with one of the following: , & , and 'n'.

            program = """
                    Midnight takes your heart and your soul
                    Give back your heart of your soul
                                           
                    Ice is nicer
                    Fire is hotter
                    Say Midnight taking ice & fire
                    """;

            assertEquals("30\n", compileAndLaunch(program));

            program = """
                    Midnight takes your heart and your soul
                    Give back your heart of your soul
                                           
                    Ice is nicest
                    Fire is hottest
                    Say Midnight taking ice 'n' fire
                    """;

            assertEquals("42\n", compileAndLaunch(program));
        }

        @Test
        public void shouldHandleSubtractionInMultiArgumentFunctionsOnNumbers() {
            String program = """
                    Midnight takes your heart and your soul
                    Give back your heart without your soul
                                           
                    Ice is nice
                    Fire is hot
                    Say Midnight taking ice, fire
                    """;
            String output = compileAndLaunch(program);

            // Function multiplies arguments, 4*3 is 12
            assertEquals("1\n", output);
        }

        @Test
        public void shouldHandleAdditionInMultiArgumentFunctionsOnNothings() {
            String program = """
                    Midnight takes your heart and your soul
                    Give back your heart plus your soul
                                           
                    Ice is nothing
                    Fire is hot
                    Say Midnight taking ice, fire
                    """;
            String output = compileAndLaunch(program);

            // Function multiplies arguments, 4*3 is 12
            assertEquals("3\n", output);
        }

        @Test
        public void shouldHandleSubtractionInMultiArgumentFunctionsOnNothings() {
            String program = """
                    Midnight takes your heart and your soul
                    Give back your heart minus your soul
                                           
                    Ice is nothing
                    Fire is hot
                    Say Midnight taking ice, fire
                    """;
            String output = compileAndLaunch(program);

            // Function multiplies arguments, 4*3 is 12
            assertEquals("-3\n", output);
        }

        @Test
        public void shouldHandleThreeArgumentFunctionsOnStrings() {
            String program = """
                    Midnight takes your heart and your soul and my hat
                    Give back your heart plus your soul plus my hat
                                           
                    Carol is "nice"
                    Bob is "less nice"
                    Hair is "frizzy"
                    Say Midnight taking Carol, Bob, Hair
                    """;
            String output = compileAndLaunch(program);

            assertEquals("niceless nicefrizzy\n", output);
        }

        @Test
        public void shouldHandleFunctionInvocationsMixedWithOtherExpressionsInAddition() {
            String program = """
                    Midnight takes your heart and your soul
                    Give back your heart
                            
                            
                    My world is 0
                    Fire is ice
                    Hate is water
                    shout Midnight taking my world, Fire is 0 plus Midnight taking my world, Hate is 0
                    """;

            assertEquals("false\n", compileAndLaunch(program));

            program = """
                    Midnight takes your heart and your soul
                    Give back your heart plus " concat " plus your soul
                            
                            
                    My world is 0
                    Fire is ice
                    Hate is water
                    shout Midnight taking my world, Fire plus Midnight taking my world, Hate
                    """;

            assertEquals("0 concat 30 concat 5\n", compileAndLaunch(program));
        }

        @Test
        public void shouldHandleFunctionInvocationsMixedWithOtherExpressionsInConjunction() {
            String program = """
                    Midnight takes your heart and your soul
                    Give back your heart
                            
                    My world is 0
                    Fire is ice
                    Hate is water
                    shout Midnight taking my world, Fire is 0 plus Midnight taking my world, Hate is 0
                    """;

            assertEquals("false\n", compileAndLaunch(program));

            // A slightly harder version, with nulls
            program = """
                    Midnight takes your heart and your soul
                    Give back your heart minus your soul
                            
                    My world is nothing
                    Fire is ice
                    Hate is water
                    shout Midnight taking my world, Fire is nothing and Midnight taking my world, Hate is nothing
                    """;
            assertEquals("false\n", compileAndLaunch(program));

            // Another hard version, with null coming back from the function
            program = """
                    Midnight takes your heart and your soul
                    Give back your heart
                            
                    My world is nothing
                    Fire is ice
                    Hate is water
                    shout Midnight taking my world, Fire is nothing and Midnight taking my world, Hate is nothing
                    """;
            assertEquals("true\n", compileAndLaunch(program));

        }
    }

    @Nested
    class Strings {

        @Disabled("Needs support for changing the type of a variable")
        @Test
        public void shouldSplitStringsInPlace() {
            String program = """
                    My life is "misery"
                    Cut my life
                    Say my life
                    """;
            assertEquals("6\n", compileAndLaunch(program));
        }

        @Test
        public void shouldSplitStringsIntoCharArrays() {
            String program = """
                    My life is "one two"
                    Cut my life into pieces
                    Say pieces
                    """;
            assertEquals("7\n", compileAndLaunch(program));
        }

        @Test
        public void shouldSplitStringsIntoCharArraysAndThenReadThem() {
            String program = """
                        delimiter is ";"
                        line is "hi; bye"
                        split line into segments with delimiter
                        let stationName be segments at 0
                        say stationName
                    """;

            assertEquals("hi\n", compileAndLaunch(program));

        }

        @Disabled("Needs support for changing the type of a variable")
        @Test
        public void shouldSplitStringsInPlaceWithDelimiter() {
            String program = """
                    Your cake is "chocolate"
                    My knife is "o"
                    Cut your cake with my knife
                    """;

            assertEquals("3\n", compileAndLaunch(program));

        }

        @Test
        public void shouldSplitStringsUsingADelimiter() {
            String program = """
                    Your deceit is " "
                    My heart is "a string with spaces"
                    Shatter my heart into pieces with your deceit
                    Say pieces
                    """;

            assertEquals("4\n", compileAndLaunch(program));

        }
    }

    @Nested
    class Arrays {
        @Test
        public void shouldInitialiseAnArrayWithRock() {
            String program = """
                    Rock the array
                    Say the array
                    """;
            String output = compileAndLaunch(program);

            assertEquals("0\n", output);
        }

        @Test
        public void shouldOutputArrayLength() {
            String program = """
                    Rock 1, 2, 3, 4, 8 into arr
                    Say arr
                    """;
            String output = compileAndLaunch(program);

            assertEquals("5\n", output);
        }

        // Returning an array in a scalar context will return the current length of the array
        @Test
        public void shouldUseLengthOfArrayInAScalarContext() {
            String program = """
                    rock 5, 4, 2 into me
                    say me
                    say me times 2
                    say me plus 8
                                        """;
            String output = compileAndLaunch(program);

            assertEquals("3\n6\n11\n", output);
        }

        @Test
        public void shouldUseLengthOfArrayWhenConcatenatingToStrings() {
            String program = """
                    rock 1, 3 into arr
                    say arr + "length"
                                        """;
            String output = compileAndLaunch(program);

            assertEquals("2length\n", output);
        }

        @Test
        public void shouldUseLengthOfArrayInAScalarContextEvenIfArrayIsWrappedInAVariable() {
            String program = """
                    rock 5, 4, 2 into me
                    let you be me
                    say you times 2
                    say you plus 8
                                        """;
            String output = compileAndLaunch(program);

            assertEquals("6\n11\n", output);
        }

        @Test
        public void shouldIncreaseLengthWhenAppendingToArray() {
            String program = """
                    rock arr
                    rock 1 into arr
                    rock 5 into arr
                    rock 8 into arr
                    say arr
                                                            """;
            String output = compileAndLaunch(program);

            assertEquals("3\n", output);
        }

        @Test
        public void shouldPutContentInRightPlaceWhenAppendingToArray() {
            String program = """
                    rock arr
                    rock 1 into arr
                    rock 5 into arr
                    rock 8 into arr
                    say arr at 1
                    say arr at 2
                    say arr at 0
                                                            """;
            String output = compileAndLaunch(program);

            assertEquals("5\n8\n1\n", output);
        }

        @Test
        public void shouldBeAbleToUseWithInArrayAssignments() {
            String program = """
                    rock lines with 7
                    say lines
                                        """;


            assertEquals("1\n", compileAndLaunch(program));
        }

        @Test
        public void shouldBeAbleToUseListsOfWithInArrayAssignments() {
            String program = """
                    push lines with 1, 4, 8, 16
                    say lines
                                        """;


            assertEquals("4\n", compileAndLaunch(program));
        }

        @Test
        public void shouldBeAbleToAccessArrayUsingExpressions() {
            String program = """
                    rock arr
                    rock 1 into arr
                    rock 5 into arr
                    rock 8 into arr
                    fred is 1
                    jane is 0
                    say arr at 1 + 1
                    say arr at jane
                    say arr at fred
                                                            """;
            String output = compileAndLaunch(program);

            assertEquals("8\n1\n5\n", output);
        }

        @Test
        public void shouldBeAbleToAccessArrayUsingNothingExpressions() {
            String program = """
                    rock arr
                    rock 1 into arr
                    rock 5 into arr
                    rock 8 into arr
                    say arr at nothing
                    jane is nothing
                    say arr at jane
                                                            """;
            String output = compileAndLaunch(program);

            assertEquals("1\n1\n", output);
        }

        @Test
        public void shouldAccessFirstElementOfTheArrayUsingPop() {
            String program = """
                    rock 1 into lines
                    rock 4 into lines
                    rock 8 into lines
                    let line be pop lines
                    say line
                    let line be pop lines
                    say line
                                        """;


            assertEquals("1\n4\n", compileAndLaunch(program));
        }

        @Test
        public void shouldBeAbleToPopPastTheEndOfArray() {
            String program = """
                    rock 1 into lines
                    rock 4 into lines
                    rock 8 into lines
                    let line be pop lines
                    say line
                    let line be pop lines
                    say line
                    let line be pop lines
                    say line
                    let line be pop lines
                    say line
                                        """;


            String output = compileAndLaunch(program);
            assertTrue(output.startsWith("1\n4\n8\n"), output);
        }

        @Disabled("Needs better mysterious support")
        @Test
        public void shouldAccessFirstElementOfTheArrayUsingPopAndOutputMysterious() {
            String program = """
                    rock 1 into lines
                    rock 4 into lines
                    rock 8 into lines
                    let line be pop lines
                    say line
                    let line be pop lines
                    say line
                    let line be pop lines
                    say line
                    let line be pop lines
                    say line
                                        """;
            assertEquals("1\n4\n8\nmysterious\n", compileAndLaunch(program));
        }

        // Arrays are zero-based, and dynamically allocated when values are assigned using numeric indexes.
        @Test
        public void shouldSetTheArrayLengthOnWritingToAnIndex() {
            String program = """
                    Let the array at 0 be "zero"
                    Let the array at 1 be "one"
                    Let the array at 255 be "big"
                    Shout the array
                    Shout the array at 0
                    Shout the array at 255
                                                            """;
            assertEquals("256\nzero\nbig\n", compileAndLaunch(program));
        }

        @Test
        public void shouldNotOverWriteExistingContentsWhenPopulatingArrayOnInitialisationAtALargeIndex() {
            String program = """
                    Rock 1, 2, 3 into arr
                    Let arr at 8 be 7
                    Say arr at 2
                    Say arr at 8
                    Say arr
                                        """;
            assertEquals("3\n7\n9\n", compileAndLaunch(program));

        }

        // Rockstar also supports a special roll x into y syntax for removing the first element from an array and assigning it to a variable
        @Test
        public void shouldSupportAssignmentUsingRollIntoVariables() {
            String program = """
                    Rock the list with 4, 5, 6
                    Roll the list into foo
                    Roll the list into bar
                    Roll the list into baz
                    Shout foo
                    Shout bar
                    Shout baz
                    """;
            assertEquals("4\n5\n6\n", compileAndLaunch(program));

        }

        @Test
        public void shouldSupportNonNumericKeys() {
            String program = """
                    let my array at "some_key" be "some_value"
                    Shout my array at "some_key"
                    """;
            assertEquals("some_value\n", compileAndLaunch(program));

        }

        @Test
        public void shouldSupportMixOfNumericAndNonNumericKeys() {
            // Non-numeric keys are not counted in the length, but numeric keys are
            String program = """
                    Let my array at "some_key" be "some_value"
                    Shout my array
                    Let my array at 7 be "some other value"
                    Shout my array
                                        """;
            assertEquals("0\n8\n", compileAndLaunch(program));
        }

        @Test
        public void shouldSupportMixOfNumericAndNonNumericKeysEvenWhenArrayIsAssignedToAnotherVariable() {
            // Non-numeric keys are not counted in the length, but numeric keys are
            String program = """
                    Let my array at "some_key" be "some_value"
                    Shout my array at "some_key"
                    Let my array at 7 be "some other value"
                    Shout my array
                    Shout my array at 7
                    Let copy be my array
                    Shout copy at 7
                    Shout copy at "some_key"
                                        """;
            assertEquals("some_value\n" +
                    "8\n" +
                    "some other value\n" +
                    "some other value\n" +
                    "some_value\n", compileAndLaunch(program));
        }
    }

    @Nested
    class Casting {
        @Test
        public void shouldCastDoubleToString() {
            String program = """
                    Let X be "123.45"
                    Cast X
                        (X now contains the numeric value 123.45)
                    Say X
                    Say X + 2 (casting really shows when doing math)
                    """;
            assertEquals("123.45\n125.45\n", compileAndLaunch(program));
        }

        @Test
        public void shouldCastDoubleToStringUsingBurnAlias() {
            String program = """
                    Let X be "123.45"
                    Burn X
                        (X now contains the numeric value 123.45)
                    Say X
                    Say X + 2 (casting really shows when doing math)
                    """;
            assertEquals("123.45\n125.45\n", compileAndLaunch(program));
        }

        @Disabled("Not yet implemented")
        @Test
        public void shouldCastStringToDoubleWithConversionBases() {
            String program = """
                    Let X be "ff"
                    Cast X with 16
                        (X now contains the numeric value 255 - OxFF)
                    Say X
                    Say X + 2
                                       
                                        
                    Cast "12345" into result
                        (result now contains the number 12345)
                    Cast "aa" into result with 16
                        (result now contains the number 170 - 0xAA)
                                        
                    Cast 65 into result
                        (result now contains the string "A" - ASCII code 65)
                                        
                    Cast 1046 into result
                        (result now contains the Cyrillic letter "Ж" - Unicode code point 1046)
                    """;
            assertEquals("255\n257\n", compileAndLaunch(program));
        }

        @Disabled("Not yet implemented")
        @Test
        public void shouldCastIntoAVariable() {
            String program = """             
                    Cast "12345" into result
                        (result now contains the number 12345)
                    Say result
                    Say result + 2
                    Cast "aa" into result with 16
                        (result now contains the number 170 - 0xAA)
                    Say result
                    Say result + 2
                    """;
            assertEquals("12345\n12347\n170\n172\n", compileAndLaunch(program));
        }

        @Disabled("Not yet implemented")
        @Test
        public void shouldCastNumbersIntoStringsAsUnicode() {
            String program = """       
                    Cast 65 into result
                        (result now contains the string "A" - ASCII code 65)
                    Say result
                    Say result + 2
                                        
                    Cast 1046 into result
                        (result now contains the Cyrillic letter "Ж" - Unicode code point 1046)
                    Say result
                    """;
            assertEquals("A\nA2\nЖ\n", compileAndLaunch(program));
        }
    }

    @Test
    public void shouldUpdateTypeOnNewUsage() {
        String program = """
                my world is "octopus"
                            my world is nothing
                            Build my world up
                            Build my world up
                            say my world
                """;
        assertEquals("2\n", compileAndLaunch(program));
    }

    private String compileAndLaunch(String program, String... args) {
        // Save the current System.out for later restoration
        PrintStream originalOut = System.out;
        String className = "rock.soundcheck";

        TestClassLoader loader = new TestClassLoader(this.getClass().getClassLoader());

        try {
            new BytecodeGenerator().generateBytecode(new ByteArrayInputStream(program.getBytes(StandardCharsets.UTF_8)), className,
                    loader);
            Class<?> clazz = loader.loadClass(className);
            Method main = clazz.getMethod("main", String[].class);

            // Capture stdout since that's what the test will validate
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(outputStream);
            System.setOut(printStream);

            // We need to wrap the String[] in an Object[] because it's nested varargs
            main.invoke(null, new Object[]{args});

            // Get the captured output as a string
            return outputStream.toString();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 IOException e) {
            throw new RuntimeException(e);
        } finally {
            // Restore the original System.out
            System.setOut(originalOut);
        }
    }
}
