package org.example;

import org.example.util.DynamicClassLoader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BytecodeGeneratorTest {


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
        assertEquals("34.2\n", output);
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
    public void shouldNotBeSensitiveToWhitespace() {
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

        assertEquals("4.89\n", output);
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

    @Disabled("Needs support for plus")
    @Test
    public void shouldHandleOperationsOnPronounReferences() {
        String program = """
                The message is "pass"
                say it
                say it plus "!"
                """;
        String output = compileAndLaunch(program);

        assertEquals("pass\n" +
                     "pass!", output);

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

        assertEquals("5.141\n", output);
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
        assertEquals("-0.58\n", output);
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

    @Disabled("Not yet supported")
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

    @Disabled("Division not working")
    @Test
    public void shouldHandleDivisionWithVariablesAndPut() {

        String program = """
                My heart is full
                The moon is aflame
                My heart over the moon
                Whisper my heart
                """;
        String output = compileAndLaunch(program);

        assertEquals("4\n", output);
    }

    private String compileAndLaunch(String program) {
        // Save the current System.out for later restoration
        PrintStream originalOut = System.out;

        DynamicClassLoader loader = new DynamicClassLoader();

        try {
            new BytecodeGenerator().generateBytecode(new ByteArrayInputStream(program.getBytes(StandardCharsets.UTF_8)), "whatever",
                    loader);
            Class<?> clazz = loader.findClass("whatever");
            Method main = clazz.getMethod("main", String[].class);

            // Capture stdout since that's what the test will validate
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(outputStream);
            System.setOut(printStream);

            main.invoke(null, (Object) null);

            // Get the captured output as a string
            return outputStream.toString();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            // Restore the original System.out
            System.setOut(originalOut);
        }
    }
}
