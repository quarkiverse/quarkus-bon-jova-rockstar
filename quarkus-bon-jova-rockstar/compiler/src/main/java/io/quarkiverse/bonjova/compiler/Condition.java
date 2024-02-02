package io.quarkiverse.bonjova.compiler;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ResultHandle;
import rock.Rockstar;

public class Condition {
    private final boolean hasElse;
    Expression expression;

    public Condition(Rockstar.IfStmtContext ctx) {
        expression = new Expression(ctx.expression());
        hasElse = ctx.KW_ELSE() != null;
    }

    public BranchResult toCode(BytecodeCreator main, ClassCreator classCreator) {
        ResultHandle evaluated = expression.getResultHandle(main, classCreator, Expression.Context.BOOLEAN);
        BranchResult conditional = main.ifTrue(evaluated);
        return conditional;
    }

    public boolean hasElse() {
        return hasElse;
    }
}
