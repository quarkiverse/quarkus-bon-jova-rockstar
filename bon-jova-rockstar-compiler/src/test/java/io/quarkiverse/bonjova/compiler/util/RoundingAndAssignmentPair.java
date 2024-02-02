package io.quarkiverse.bonjova.compiler.util;

import rock.Rockstar;

public record RoundingAndAssignmentPair(Rockstar.AssignmentStmtContext assignmentStmtContext,
                                        Rockstar.RoundingStmtContext roundingStmtContext) {
}
