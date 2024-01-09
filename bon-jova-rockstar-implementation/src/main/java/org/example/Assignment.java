package org.example;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import org.example.grammar.PoeticNumberLiteral;
import rock.Rockstar;

public class Assignment {
    private final String originalName;
    private final Object value;
    private final Class<?> variableClass;

    private Expression expression;
    private final Variable variable;

    public Assignment(Rockstar.AssignmentStmtContext ctx) {
        variable = new Variable(ctx.variable());
        originalName = variable.getVariableName();
        // Variables should 'apply' to future pronouns when used in assignments
        variable.track();

        if (ctx.expression() != null) {
            expression = new Expression(ctx.expression());
            value = expression.getValue();
            variableClass = expression.getValueClass();
        } else {
            // The literals and constant could be in an expression, or top-level
            // (which is a bit annoying and perhaps is something that should be optimised in the grammar)
            Rockstar.LiteralContext literalContext = ctx.literal();
            Rockstar.ConstantContext constantContext = ctx.constant();

            if (literalContext != null) {
                Literal literal = new Literal(literalContext);
                value = literal.getValue();
                variableClass = literal.getValueClass();
            } else if (constantContext != null) {
                Constant constant = new Constant(constantContext);
                value = constant.getValue();
                variableClass = constant.getValueClass();
            } else {
                if (ctx.poeticStringLiteral() != null) {
                    value = ctx.poeticStringLiteral()
                               .getText();
                    variableClass = String.class;

                } else if (ctx.poeticNumberLiteral() != null) {
                    PoeticNumberLiteral lit = new PoeticNumberLiteral(ctx.poeticNumberLiteral());
                    value = lit.getValue();
                    variableClass = lit.getVariableClass();
                } else {
                    value = null;
                    variableClass = Object.class;
                }
            }
        }
    }

    public String getVariableName() {
        return originalName;
    }

    public Object getValue() {
        return value;
    }

    public Class<?> getVariableClass() {
        return variableClass;
    }

    public void toCode(ClassCreator creator, BytecodeCreator method) {

        FieldDescriptor field = variable.getField(creator, method, getVariableClass());

        ResultHandle rh;

        if (expression != null) {
            rh = expression.getResultHandle(method);
        } else {
            // This code is duplicated in Expression, but it's probably a bit too small to be worth extracting
            Object value = getValue();

            if (String.class.equals(variableClass)) {
                rh = method.load((String) value);
            } else if (double.class.equals(variableClass)) {
                rh = method.load((double) value);
            } else if (boolean.class.equals(variableClass)) {
                rh = method.load((boolean) value);
            } else {
                throw new RuntimeException("Internal error: unknown type " + value);
            }
        }

        method.writeStaticField(field, rh);

    }
}
