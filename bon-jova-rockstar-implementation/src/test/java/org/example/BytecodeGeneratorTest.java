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

    @Disabled("Repeated increments in the same statement not supported by grammar, yet")
    @Test
    public void shouldHandleIncrementForRepetitionCase() {
        String program = """
                My world is 0
                Build my world up, up up
                Say my world
                """;
        String output = compileAndLaunch(program);

        assertEquals("2\n", output);
    }


    @Test
    public void shouldHandleIncrementForPronounCase() {
        String program = """
                My world is 0
                Build it up
                Build it up
                Say my world
                """;
        String output = compileAndLaunch(program);

        assertEquals("2\n", output);

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
