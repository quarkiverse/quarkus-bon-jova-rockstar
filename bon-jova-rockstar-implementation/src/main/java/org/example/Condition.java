package org.example;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;
import rock.Rockstar;

public class Condition {
    private final boolean hasElse;
    Expression expression;

    public Condition(Rockstar.IfStmtContext ctx) {
        expression = new Expression(ctx.expression());
        hasElse = ctx.KW_ELSE() != null;
    }

    public BranchResult toCode(BytecodeCreator main) {
        ResultHandle evaluated = expression.getResultHandle(main);
        BranchResult conditional = main.ifTrue(evaluated);
        return conditional;
    }

    public boolean hasElse() {
        return hasElse;
    }
}
