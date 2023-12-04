package org.example;

import org.junit.jupiter.api.Test;
import rock.Rockstar;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssignmentTest {

    @Test
    public void shouldParseVariableName() {
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
    public void shouldParseIntegerLiterals() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is 5");
        Assignment a = new Assignment(ctx);
        assertEquals(5, a.getValue());
    }

    @Test
    public void shouldParsePoeticNumberLiterals() {
        Rockstar.AssignmentStmtContext ctx = ParseHelper.getAssignment("My thing is a big bad monster");
        Assignment a = new Assignment(ctx);
        assertEquals(1337, a.getValue());
    }

}
