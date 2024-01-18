package org.example;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import org.example.util.ParseHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rock.Rockstar;

import static org.example.Constant.NOTHING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AssignmentTest {

    @BeforeEach
    public void clearState() {
        Variable.clearPronouns();
    }

    @Test
    public void shouldParseVariableNameWithSimpleVariableAssignment() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("Thing is 6");
        Assignment a = new Assignment(ctx);
        // Variable names should be normalised to lower case
        String name = "thing";
        assertEquals(name, a.getVariableName());
    }

    @Test
    public void shouldParseVariableNameWithCommonVariableAssignment() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is 6");
        Assignment a = new Assignment(ctx);
        // Variable names should be normalised to lower case
        String name = "my__thing";
        assertEquals(name, a.getVariableName());
    }

    @Test
    public void shouldParseVariableNameWithProperVariableAssignment() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("Doctor Feelgood is 6");
        Assignment a = new Assignment(ctx);
        // Variable names should be normalised to lower case
        assertEquals("doctor__feelgood", a.getVariableName());
    }

    @Test
    public void shouldParseIntegerLiterals() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is 5");
        Assignment a = new Assignment(ctx);
        // The number should be stored as a double, even though it was entered as an integer
        assertEquals(5d, a.getValue());
        assertEquals(double.class, a.getVariableClass());
    }

    /* Numbers in Rockstar are double-precision floating point numbers, stored according to the IEEE 754 standard.*/
    @Test
    public void shouldParseFloatingPointLiterals() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is 3.141");
        Assignment a = new Assignment(ctx);
        assertEquals(3.141, a.getValue());
        assertEquals(double.class, a.getVariableClass());
    }

    @Test
    public void shouldParseStringLiterals() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is \"Yes hello\"");
        Assignment a = new Assignment(ctx);
        assertEquals("Yes hello", a.getValue());
        assertEquals(String.class, a.getVariableClass());
    }

    /*
    empty , silent , and silence are aliases for the empty string ( "" ).
     */
    @Test
    public void shouldParseEmptyStringAliases() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is silence");
        Assignment a = new Assignment(ctx);
        assertEquals("", a.getValue());
        assertEquals(String.class, a.getVariableClass());

        ctx = ParseHelper.getAssignment("My thing is silent");
        a = new Assignment(ctx);
        assertEquals("", a.getValue());

        ctx = ParseHelper.getAssignment("My thing is empty");
        a = new Assignment(ctx);
        assertEquals("", a.getValue());
    }

    @Test
    public void shouldParseIntegerPoeticNumberLiterals() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is a big bad monster");
        Assignment a = new Assignment(ctx);
        assertEquals(1337d, a.getValue());
        assertEquals(double.class, a.getVariableClass());
    }

    /*
    A poetic string literal assignment starts with a variable name, followed by one of the keywords say , says or said followed by a
    single space. The rest of the line up to the \n terminator is treated as an unquoted string literal.
     */
    @Test
    public void shouldParsePoeticStringLiterals() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing says cannot buy love");
        Assignment a = new Assignment(ctx);
        assertEquals("cannot buy love", a.getValue());

        ctx = ParseHelper.getAssignment("My thing say why");
        a = new Assignment(ctx);
        assertEquals("why", a.getValue());
        assertEquals(String.class, a.getVariableClass());

        ctx = ParseHelper.getAssignment("My thing said still cannot buy love");
        a = new Assignment(ctx);
        assertEquals("still cannot buy love", a.getValue());
    }

    @Test
    public void shouldParseBooleanLiteralsForTrueCase() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is true");
        Assignment a = new Assignment(ctx);
        assertEquals(true, a.getValue());
        assertEquals(boolean.class, a.getVariableClass());

        ctx = ParseHelper.getAssignment("My thing is right");
        a = new Assignment(ctx);
        assertEquals(true, a.getValue());

        ctx = ParseHelper.getAssignment("My thing is ok");
        a = new Assignment(ctx);
        assertEquals(true, a.getValue());

        ctx = ParseHelper.getAssignment("My thing is yes");
        a = new Assignment(ctx);
        assertEquals(true, a.getValue());
    }

    @Test
    public void shouldParseBooleanLiteralsForFalseCase() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is false");
        Assignment a = new Assignment(ctx);
        assertEquals(false, a.getValue());
        assertEquals(boolean.class, a.getVariableClass());

        ctx = ParseHelper.getAssignment("My thing is lies");
        a = new Assignment(ctx);
        assertEquals(false, a.getValue());

        ctx = ParseHelper.getAssignment("My thing is wrong");
        a = new Assignment(ctx);
        assertEquals(false, a.getValue());

        ctx = ParseHelper.getAssignment("My thing is no");
        a = new Assignment(ctx);
        assertEquals(false, a.getValue());
    }

    /*
    Rockstar makes a distinction between `null` and `undefined`. The undefined is closer to Java's null.
     */
    @Test
    public void shouldParseUndefinedConstants() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is mysterious");
        Assignment a = new Assignment(ctx);
        assertNull(a.getValue());
    }

    /*
 Rockstar makes a distinction between `null` and `undefined`. The null class is equal to 0 and false, so it can't be represented by a
 Java null.
  */
    @Test
    public void shouldParseNullConstants() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is nothing");
        Assignment a = new Assignment(ctx);
        assertEquals(Nothing.class, a.getVariableClass());
        assertEquals(NOTHING, a.getValue());

        ctx = ParseHelper.getAssignment("My thing is nobody");
        assertEquals(Nothing.class, a.getVariableClass());
        assertEquals(NOTHING, a.getValue());

        ctx = ParseHelper.getAssignment("My thing is nowhere");
        assertEquals(Nothing.class, a.getVariableClass());
        assertEquals(NOTHING, a.getValue());

        ctx = ParseHelper.getAssignment("My thing is gone");
        assertEquals(Nothing.class, a.getVariableClass());
        assertEquals(NOTHING, a.getValue());
    }

    @Test
    public void shouldHandleAssignmentUsingPut() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("Put 123 into X");
        Assignment a = new Assignment(ctx);
        assertEquals(123d, a.getValue());
    }

    @Test
    public void shouldHandleAssignmentToSimpleVariablesUsingApostrophe() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("Janie's got a gun");
        assertEquals(313d, new Assignment(ctx).getValue());

        ctx = ParseHelper.getAssignment("Union's been on strike");
        assertEquals(426d, new Assignment(ctx).getValue());

        ctx = ParseHelper.getAssignment("We're here to see the show");
        assertEquals(42334d, new Assignment(ctx).getValue());
    }

    @Test
    public void shouldHandleAssignmentToCommonVariablesUsingApostrophe() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("The fire's burning");
        assertEquals(7d, new Assignment(ctx).getValue());
    }

    @Test
    public void shouldHandleAssignmentToProperVariablesUsingApostrophe() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("Doctor Feelgood's mad");
        assertEquals(3d, new Assignment(ctx).getValue());
    }

    @Test
    public void shouldHandleAssignmentOfConstantsUsingPut() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("Put lies into X");
        Assignment a = new Assignment(ctx);
        assertEquals(false, a.getValue());
    }

    // Assignment to a variable should be interpreted as a poetic number literal, not the variable.
    // "A poetic number literal begins with a variable name, followed by the keyword is, or the aliases are, was or were. As long as the
    // next symbol is not a Literal Word, the rest of the line is treated as a decimal number in which the values of consecutive digits
    // are given by the lengths of the subsequent barewords, up until the end of the line. "
    // Validated by comparison to Satriani.
    @Test
    public void shouldInterpretAssignmentToVariablesAsPoeticNumberLiterals() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is \"hello\"\nEverything is my thing");
        Assignment a = new Assignment(ctx);
        assertEquals(25d, a.getValue());
        assertEquals(double.class, a.getVariableClass());
    }

    @Test
    public void shouldHandleInitializeVariableOnAssignment() {

        // No declaration of my hands before put-ing into it
        String program = """
                Put 5 of 6 into my hands
                """;
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment(program);
        Assignment a = new Assignment(ctx);
        assertEquals(double.class, a.getVariableClass());

    }

    @Test
    public void shouldWriteToClass() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is \"hello\"\nEverything is my thing");
        Assignment a = new Assignment(ctx);

        ClassCreator creator = ClassCreator.builder()
                .className("holder")
                .build();
        MethodCreator method = creator.getMethodCreator("main", void.class, String[].class);

        a.toCode(creator, method);
        // It's hard to make many sensible assertions without making a mock and asserting about to the calls, and that's
        // awfully close to just writing the code down twice, so just be happy if we get here without explosions

    }

}
