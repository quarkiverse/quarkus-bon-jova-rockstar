package org.example;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
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

        // The literals and constant could be in an expression, or top-level
        Rockstar.LiteralContext literalContext = ctx.expression() != null ? ctx.expression()
                                                                               .literal() : ctx.literal();
        Rockstar.ConstantContext constantContext = ctx.expression() != null ? ctx.expression()
                                                                                 .constant() : ctx.constant();

        // Don't bother instantiating an expression, since we may need to go direct to a literal, and since
        // (in the reference implementation, at least) assignment to a variable isn't possible; variable names are interpreted as poetic
        // number literals

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

    public String getVariableName() {
        return originalName;
    }

    public Object getValue() {
        return value;
    }

    public Class<?> getVariableClass() {
        return variableClass;
    }

    public void toCode(ClassCreator creator, MethodCreator method) {

        FieldDescriptor field = variable.getField(creator, method, getVariableClass());

        // This code is duplicated in Expression, but it's probably a bit too small to be worth extracting
        Object value = getValue();
        if (String.class.equals(variableClass)) {
            method.writeStaticField(field, method.load((String) value));
        } else if (double.class.equals(variableClass)) {
            method.writeStaticField(field, method.load((double) value));
        } else if (boolean.class.equals(variableClass)) {
            method.writeStaticField(field, method.load((boolean) value));
        }
    }
}
