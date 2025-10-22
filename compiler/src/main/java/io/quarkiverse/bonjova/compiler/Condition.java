package io.quarkiverse.bonjova.compiler;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.ResultHandle;
import rock.Rockstar;

public class Condition {
    private final boolean hasElse;
    Expression expression;

    public Condition(Rockstar.IfStmtContext ctx) {
        expression = new Expression(ctx.expression());
        hasElse = ctx.KW_ELSE() != null;
    }

    public BranchResult toCode(Block block) {
        ResultHandle evaluated = expression.getResultHandle(block, Expression.Context.BOOLEAN);
        BranchResult conditional = block.method().ifTrue(evaluated);
        return conditional;
    }

    public boolean hasElse() {
        return hasElse;
    }
}
