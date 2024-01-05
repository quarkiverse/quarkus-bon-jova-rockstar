package org.example;

import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import org.antlr.v4.runtime.tree.TerminalNode;
import rock.Rockstar;

public class Expression {

    private Class<?> valueClass;
    private Object value;
    private Variable variable;

    private Expression lhe;
    private Expression rhe;
    private Operation operation;

    private enum Operation {ADD, SUBTRACT, MULTIPLY, DIVIDE}


    public Expression(Rockstar.ExpressionContext ctx) {
        if (ctx != null) {
            Rockstar.LiteralContext literal = ctx.literal();
            Rockstar.ConstantContext constant = ctx.constant();
            Rockstar.VariableContext variableContext = ctx.variable();

            if (ctx.op != null) {
                lhe = new Expression(ctx.lhe);
                rhe = new Expression(ctx.rhe);

                if (ctx.KW_ADD() != null || "+".equals(ctx.op.getText())) {
                    operation = Operation.ADD;
                } else if (ctx.KW_SUBTRACT() != null || "-".equals(ctx.op.getText())) {
                    operation = Operation.SUBTRACT;
                } else if (ctx.KW_MULTIPLY() != null || "*".equals(ctx.op.getText())) {
                    operation = Operation.MULTIPLY;
                } else if (ctx.KW_DIVIDE() != null || "/".equals(ctx.op.getText())) {
                    operation = Operation.DIVIDE;
                }
            }

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
        if (operation != null) {
            if (operation == Operation.ADD) {
                return method.add(lhe.getResultHandle(method), rhe.getResultHandle(method));
            } else if (operation == Operation.SUBTRACT) {
                // Handle subtraction by multiplying by -1 and adding
                ResultHandle negativeRightSide = method.multiply(method.load(-1d), rhe.getResultHandle(method));
                return method.add(lhe.getResultHandle(method), negativeRightSide);
            } else if (operation == Operation.MULTIPLY) {
                return method.multiply(lhe.getResultHandle(method), rhe.getResultHandle(method));
            } else if (operation == Operation.DIVIDE) {
                //  return method.divide(lhe.getResultHandle(method), rhe.getResultHandle(method));
                throw new RuntimeException("Unsupported operation: divided we fall, and all that");
            }
        } else if (variable != null) {
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
