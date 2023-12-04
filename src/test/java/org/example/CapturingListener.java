package org.example;

import rock.Rockstar;
import rock.RockstarBaseListener;

/**
 * Used in tests as an alternative to mocking.
 */
public class CapturingListener extends RockstarBaseListener {
    private Rockstar.AssignmentStmtContext assignmentStatement;

    @Override
    public void enterAssignmentStmt(Rockstar.AssignmentStmtContext assignmentStatement) {
        this.assignmentStatement = assignmentStatement;
    }

    public Rockstar.AssignmentStmtContext getAssignmentStatement() {
        return assignmentStatement;
    }
}
