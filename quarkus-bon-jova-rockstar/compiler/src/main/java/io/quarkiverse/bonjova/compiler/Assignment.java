package io.quarkiverse.bonjova.compiler;

import io.quarkiverse.bonjova.compiler.grammar.PoeticNumberLiteral;
import io.quarkiverse.bonjova.support.Nothing;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import rock.Rockstar;

import static io.quarkiverse.bonjova.support.Nothing.NOTHING;

public class Assignment {
    private final String originalName;
    private final Object value;
    private final Class<?> variableClass;
    private final Variable variable;
    private final String text;
    private Expression expression;

    private Array arrayAccess;

    public Assignment(Rockstar.AssignmentStmtContext ctx) {
        this.text = ctx.getText();

        if (ctx.KW_ROLL() != null) {
            value = null;
            variableClass = Object.class;
            Variable source = new Variable(ctx.variable(0), Array.TYPE_CLASS);
            arrayAccess = new Array(source);

            variable = new Variable(ctx.variable().get(1), variableClass);

        } else {
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
            variable = new Variable(ctx.variable().get(0), variableClass);
        }
        originalName = variable.getVariableName();
        // Variables should 'apply' to future pronouns when used in assignments
        variable.track();

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

    // TODO make argument order consistent across related classes
    public void toCode(Block block) {

        ResultHandle rh;

        if (expression != null) {
            rh = expression.getResultHandle(block);
        } else if (arrayAccess != null) {
            rh = arrayAccess.pop(block);
        } else {
            // TODO it's now really big This code is duplicated in Expression, but it's probably a bit too small to be worth extracting
            Object value = getValue();

            BytecodeCreator method = block.method();

            if (String.class.equals(variableClass)) {
                rh = method.load((String) value);
            } else if (double.class.equals(variableClass)) {
                rh = method.load((double) value);
            } else if (boolean.class.equals(variableClass)) {
                rh = method.load((boolean) value);
            } else if (value == NOTHING) { // We can't coerce here, because we have nothing to use as a reference type
                // TODO at what point should we coerce? is it here, or do we pass nothing around?
                rh = method.readStaticField(FieldDescriptor.of(Nothing.class, "NOTHING", Nothing.class));
            } else if (value instanceof String) {
                rh = method.load((String) value);
            } else if (value instanceof Double) {
                rh = method.load((double) value);
            } else if (value instanceof Boolean) {
                rh = method.load((Boolean) value);
            } else if (value == null) {
                rh = method.loadNull();
            } else {
                throw new RuntimeException("Internal error: unknown type " + variableClass + " for " + value
                        + " used in assigment '" + text + "'");
            }
        }

        variable.write(block, rh);

    }
}
