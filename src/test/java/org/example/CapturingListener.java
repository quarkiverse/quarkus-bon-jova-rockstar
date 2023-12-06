package org.example;

import org.antlr.v4.runtime.RuleContext;
import rock.Rockstar;
import rock.RockstarBaseListener;

/**
 * Used in tests as an alternative to mocking.
 */
public class CapturingListener extends RockstarBaseListener {
    private Rockstar.AssignmentStmtContext assignmentStatement;
    private RuleContext poeticNumberLiteral;

    @Override
    public void enterAssignmentStmt(Rockstar.AssignmentStmtContext assignmentStatement) {
        this.assignmentStatement = assignmentStatement;
    }

    @Override
    public void enterPoeticNumberLiteral(Rockstar.PoeticNumberLiteralContext poeticNumberLiteral) {
        this.poeticNumberLiteral = poeticNumberLiteral;
    }


    public Rockstar.AssignmentStmtContext getAssignmentStatement() {
        return assignmentStatement;
    }

    public RuleContext getPoeticNumberLiteral() {
        return poeticNumberLiteral;
    }
}
