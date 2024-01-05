package org.example;

import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import org.antlr.v4.runtime.tree.TerminalNode;
import rock.Rockstar;

public class Expression {

    private Class<?> valueClass;
    private Object value;
    private Variable variable;

    public Expression(Rockstar.ExpressionContext ctx) {
        if (ctx != null) {
            Rockstar.LiteralContext literal = ctx.literal();
            Rockstar.ConstantContext constant = ctx.constant();
            Rockstar.VariableContext variableContext = ctx.variable();

            if (literal != null) {
                if (literal
                        .NUMERIC_LITERAL() != null) {

                    TerminalNode num = literal
                            .NUMERIC_LITERAL();
                    //  Numbers in Rockstar are double-precision floating point numbers, stored according to the IEEE 754 standard.
                    // ... so we don't need to worry about integers
                    value = Double.parseDouble(num.getText());

                    valueClass = double.class;

                } else if (literal
                        .STRING_LITERAL() != null) {
                    value = literal
                            .STRING_LITERAL()
                            .getText()
                            .replaceAll("\"", "");
                    // Strip out the quotes around literals (doing it in the listener rather than the lexer is simpler, and apparently
                    // idiomatic-ish)
                    valueClass = String.class;
                }

            } else if (constant != null) {
                if (constant
                        .CONSTANT_TRUE() != null) {
                    value = true;
                    valueClass = boolean.class;
                } else if (constant
                        .CONSTANT_FALSE() != null) {
                    value = false;
                    valueClass = boolean.class;
                } else if (constant
                        .CONSTANT_EMPTY() != null) {
                    value = "";
                    valueClass = String.class;
                }


            } else if (variableContext != null) {
                variable = new Variable(variableContext);
                value = variable.getVariableName();
                // A somewhat arbitrary choice, but at least it's a marker
                valueClass = Variable.class;

            }
        }
    }


    public Object getValue() {
        return value;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }

    public ResultHandle getResultHandle(MethodCreator method) {
        if (variable != null) {
            return variable.read(method);
        } else {
            // This is a literal
            if (String.class.equals(valueClass)) {
                return method.load((String) value);
            } else if (double.class.equals(valueClass)) {
                return method.load((double) value);
            } else if (boolean.class.equals(valueClass)) {
                return method.load((boolean) value);
            }
        }
        throw new RuntimeException("Could not interpret type " + valueClass);
    }
}
