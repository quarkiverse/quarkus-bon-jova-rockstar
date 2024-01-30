package org.example;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import org.example.util.ParseHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import rock.Rockstar;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class VariableTest {

    // The variable test doesn't need to care about the class we pass in
    private static final Class defaultVariableClass = Object.class;

    @BeforeEach
    public void clearState() {
        Variable.clearState();
    }

    @Test
    public void shouldParseIntegerLiteralsAsVariables() {
        Rockstar.VariableContext ctx = new ParseHelper().getVariable("my thing is 5\nshout my thing");
        Variable a = new Variable(ctx, double.class);
        // The 'value' is the variable name
        assertEquals("my__thing", a.getVariableName());

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
        Rockstar.VariableContext ctx = new ParseHelper().getVariable(program);
        Variable v = new Variable(ctx, String.class);

        assertEquals("variable", v.getVariableName());
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
        Rockstar.VariableContext ctx = new ParseHelper().getVariable(program);
        Variable v = new Variable(ctx, boolean.class);
        assertEquals("my__thing", v.getVariableName());
    }

    @Test
    public void shouldIgnoreExtraWhitespaceInCommonVariableNames() {
        String program = """
                My             thing is true
                Shout my   thing
                                            """;
        Rockstar.VariableContext ctx = new ParseHelper().getVariable(program);
        Variable v = new Variable(ctx, boolean.class);
        assertEquals("my__thing", v.getVariableName());
    }

    @Disabled("Lies cannot be used in a variable name since it is a boolean alias")
    @Test
    public void shouldAllowLiesVariableNames() {
        String program = """
                Your lies is "hi"
                Shout your lies
                                            """;
        Rockstar.VariableContext ctx = new ParseHelper().getVariable(program);
        Variable v = new Variable(ctx, boolean.class);
        assertEquals("my__thing", v.getVariableName());
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
        Rockstar.VariableContext ctx = new ParseHelper().getVariable(program);
        Variable v = new Variable(ctx, double.class);
        assertEquals("doctor__feelgood", v.getVariableName());
    }

    @Test
    public void shouldTreatVariableNamesAsCaseInsensitive() {
        String program = """
                tIMe is an illusion
                                            """;
        Rockstar.VariableContext ctx = new ParseHelper().getVariable(program);
        Variable v = new Variable(ctx, defaultVariableClass);
        assertEquals("time", v.getVariableName());
    }

    @Test
    public void shouldAllowAllUpperCaseVariableNames() {
        String program = """
                TIME is an illusion
                                            """;
        Rockstar.VariableContext ctx = new ParseHelper().getVariable(program);
        Variable v = new Variable(ctx, defaultVariableClass);
        assertEquals("time", v.getVariableName());
    }

    /*
 Common variables consist of one of the keywords a , an , the , my , your or our followed by whitespace and a unique variable name,
 which must contain only lowercase ASCII letters a-z. The keyword is part of the variable name, so a boy is a different variable from
  the boy . Common variables are case-insensitive.
 */
    @Test
    public void shouldMultiWordVariableNames() {
        String program = """
                My thing is true
                Shout my thing
                                            """;
        Rockstar.VariableContext ctx = new ParseHelper().getVariable(program);
        Variable v = new Variable(ctx, defaultVariableClass);
        assertEquals("my__thing", v.getVariableName());
    }


    /*
     * This is the starting example on https://codewithrockstar.com/online
     * It exercises variables, poetic number literals, and console output
     */
    @Test
    public void shouldHandleVariableAssignmentToPoeticNumberLiterals() {
        String program = "Rockstar is a big bad monster";
        Rockstar.VariableContext ctx = new ParseHelper().getVariable(program);
        Variable v = new Variable(ctx, defaultVariableClass);

        assertEquals("rockstar", v.getVariableName());
    }

    /*
     * Put 123 into X will assign the value 123 to the variable X
     */
    @Test
    public void shouldHandlePutIntoVariableAssignment() {
        String program = """
                Put 123 into X
                Shout X
                """;
        Rockstar.VariableContext ctx = new ParseHelper().getVariable(program);
        Variable v = new Variable(ctx, defaultVariableClass);
        assertEquals("x", v.getVariableName());
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
        Rockstar.VariableContext ctx = new ParseHelper().getVariable(program);
        Variable v = new Variable(ctx, defaultVariableClass);
        assertEquals("my__balance", v.getVariableName());
    }


    @Test
    public void shouldHandleStringLiteralPronounReferences() {

        // This is a bit of a convoluted test, because the thin skeleton we're using for creating the variable does invoke other parts of
        // the parse logic, such as assignments

        String program = "The message is \"pass\"";
        Rockstar.VariableContext ctx = new ParseHelper().getVariable(program);
        Variable v = new Variable(ctx, defaultVariableClass);
        v.track();

        program = "shout it";

        ctx = new ParseHelper().getVariable(program);
        v = new Variable(ctx, defaultVariableClass);

        // Now go again and make sure we can interpret the pronoun
        assertEquals("the__message", v.getVariableName());
    }

    @Test
    public void shouldRoundTripValuesThroughWritingAndReading() {
        ClassCreator creator = ClassCreator.builder()
                .className("holder")
                .build();
        MethodCreator method = creator.getMethodCreator("main", void.class, String[].class);

        Rockstar.VariableContext ctx = new ParseHelper().getVariable("johnny is \"nice\"");
        // The class here needs to match the class of what we load into the result handle
        Variable variable = new Variable(ctx, String.class);
        String className = "soundcheck";
        ResultHandle writtenValue = method.load(className);
        variable.write(method, creator, writtenValue);
        ResultHandle readValue = variable.read(method);

        // Ignore the number in our comparison
        readValue.setNo(0);
        writtenValue.setNo(0);

        // There is not an equals implementation beyond ==, so just compare the strings
        assertEquals(writtenValue.toString(), readValue.toString());

    }

    @Test
    public void shouldCreateAndReuseFields() {
        ClassCreator creator = ClassCreator.builder()
                .className("holder")
                .build();
        MethodCreator method = creator.getMethodCreator("main", void.class, String[].class);
        Set<FieldDescriptor> fields = creator.getExistingFields();
        assertEquals(0, fields.size());

        Rockstar.VariableContext ctx = new ParseHelper().getVariable("fred is 5");
        Variable variable = new Variable(ctx, defaultVariableClass);

        variable.write(method, creator, method.load(0));
        fields = creator.getExistingFields();
        assertEquals(1, fields.size());
        FieldDescriptor field1 = fields.iterator().next();


        // Now a second variable instance with the same name should return the same field
        ctx = new ParseHelper().getVariable("fred is 8");
        variable = new Variable(ctx, defaultVariableClass);

        variable.write(method, creator, method.load(1));
        fields = creator.getExistingFields();
        assertEquals(1, fields.size());
        FieldDescriptor field2 = fields.iterator().next();

        assertSame(field1, field2);
    }

    @Test
    public void shouldGenerateAJavaCompliantVariableName() {
        ClassCreator creator = ClassCreator.builder()
                .className("holder")
                .build();
        MethodCreator method = creator.getMethodCreator("main", void.class, String[].class);

        Rockstar.VariableContext ctx = new ParseHelper().getVariable("My thing is 6");
        Variable variable = new Variable(ctx, defaultVariableClass);

        variable.write(method, creator, method.load(2));
        assertEquals("my__thing", creator.getExistingFields().iterator().next().getName());
    }
}
