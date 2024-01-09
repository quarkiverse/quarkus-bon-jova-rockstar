package org.example;

import org.example.util.ParseHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rock.Rockstar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
Note that even though we're just testing the expressions, they need to be couched in something like output statement to be
recognised.
 */
public class ConditionTest {

    @BeforeEach
    public void clearState() {
        Variable.clearPronouns();
    }

    // We can't execute a condition in isolation (we get verification errors) so the tests here are pretty trivial
   
    @Test
    public void shouldDetectElse() {
        Rockstar.IfStmtContext ctx = ParseHelper.getIf("""                
                If true is right
                Say "he is ok"
                Else say "no way"
                """);
        Condition condition = new Condition(ctx);
        assertTrue(condition.hasElse());

        ctx = ParseHelper.getIf("""                
                If true is right
                Say "he is ok"
                """);
        condition = new Condition(ctx);
        assertFalse(condition.hasElse());
    }
}