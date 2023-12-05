package org.example;

import org.junit.jupiter.api.Test;
import rock.Rockstar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AssignmentTest {

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
        String name = "my thing";
        assertEquals(name, a.getVariableName());
    }

    @Test
    public void shouldGenerateAJavaCompliantVariableName() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is 6");
        Assignment a = new Assignment(ctx);
        // Variable names should be normalised to lower case
        String name = "my_thing";
        assertEquals(name, a.getNormalisedVariableName());
    }

    @Test
    public void shouldParseVariableNameWithProperVariableAssignment() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("Doctor Feelgood is 6");
        Assignment a = new Assignment(ctx);
        // Variable names should be normalised to lower case
        assertEquals("doctor feelgood", a.getVariableName());
        assertEquals("doctor_feelgood", a.getNormalisedVariableName());
    }

    @Test
    public void shouldParseIntegerLiterals() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is 5");
        Assignment a = new Assignment(ctx);
        assertEquals(5, a.getValue());
        assertEquals(int.class, a.getVariableClass());
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
        assertEquals(1337, a.getValue());
        assertEquals(int.class, a.getVariableClass());
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
    Rockstar makes a distinction between `null` and `undefined`. Javascript also does this,
    but since Java does not, we will ignore that for the moment.
     */
    @Test
    public void shouldParseUndefinedConstants() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is mysterious");
        Assignment a = new Assignment(ctx);
        assertNull(a.getValue());
        // Not great, but the best we can do
    }

    @Test
    public void shouldParseNullConstants() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is nothing");
        Assignment a = new Assignment(ctx);
        assertNull(a.getValue());

        ctx = ParseHelper.getAssignment("My thing is nobody");
        a = new Assignment(ctx);
        assertNull(a.getValue());

        ctx = ParseHelper.getAssignment("My thing is nowhere");
        a = new Assignment(ctx);
        assertNull(a.getValue());

        ctx = ParseHelper.getAssignment("My thing is gone");
        a = new Assignment(ctx);
        assertNull(a.getValue());
    }

    @Test
    public void shouldHandleAssignmentUsingPut() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("Put 123 into X");
        Assignment a = new Assignment(ctx);
        assertEquals(123, a.getValue());
    }

    @Test
    public void shouldHandleAssignmentOfConstantsUsingPut() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("Put lies into X");
        Assignment a = new Assignment(ctx);
        assertEquals(false, a.getValue());
    }

}
