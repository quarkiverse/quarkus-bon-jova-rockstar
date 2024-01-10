package org.example;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import rock.Rockstar;

import static org.example.BytecodeGeneratingListener.isNull;
import static org.example.BytecodeGeneratingListener.isNumber;
import static org.example.Constant.coerceNothingIntoType;

public class Expression {

    private Class<?> valueClass;
    private Object value;
    private Variable variable;

    private Expression lhe;
    private Expression rhe;
    private Operation operation;

    private enum Operation {
        ADD, SUBTRACT, MULTIPLY, EQUALITY_CHECK, INEQUALITY_CHECK, GREATER_THAN_CHECK,
        LESS_THAN_CHECK, GREATER_OR_EQUAL_THAN_CHECK, LESS_OR_EQUAL_THAN_CHECK, DIVIDE
    }


    public Expression(Rockstar.ExpressionContext ctx) {
        if (ctx != null) {
            Rockstar.LiteralContext literal = ctx.literal();
            Rockstar.ConstantContext constant = ctx.constant();
            Rockstar.VariableContext variableContext = ctx.variable();

            if (ctx.comparisionOp() != null) {
                Rockstar.ComparisionOpContext cop = ctx.comparisionOp();
                lhe = new Expression(ctx.lhe);
                rhe = new Expression(ctx.rhe);

                if (cop.KW_LESS() != null) {
                    operation = Operation.LESS_THAN_CHECK;
                } else if (cop.KW_GREATER() != null) {
                    operation = Operation.GREATER_THAN_CHECK;
                } else if (cop.KW_LESS_EQUAL() != null) {
                    operation = Operation.LESS_OR_EQUAL_THAN_CHECK;
                } else if (cop.KW_GREATER_EQUAL() != null) {
                    operation = Operation.GREATER_OR_EQUAL_THAN_CHECK;
                } else if (cop.KW_NOT_EQUAL() != null) {
                    operation = Operation.INEQUALITY_CHECK;
                } else if (cop.KW_IS() != null) {
                    // Do this check last since many other comparisons include an equality check
                    operation = Operation.EQUALITY_CHECK;
                }

            } else if (ctx.contractedComparisionOp() != null) {
                Rockstar.ContractedComparisionOpContext cop = ctx.contractedComparisionOp();
                lhe = new Expression(ctx.lhe);
                rhe = new Expression(ctx.rhe);

                if (cop.APOSTROPHE_S() != null) {
                    operation = Operation.EQUALITY_CHECK;
                } else {
                    throw new RuntimeException("Unknown contracted comparison operation: " + cop.getText());
                }

            } else if (ctx.op != null) {
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
                Literal l = new Literal(literal);
                value = l.getValue();
                valueClass = l.getValueClass();
            } else if (constant != null) {
                Constant c = new Constant(constant);
                value = c.getValue();
                valueClass = c.getValueClass();
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

    public ResultHandle getResultHandle(BytecodeCreator method) {
        if (operation != null) {
            ResultHandle lrh = lhe.getResultHandle(method);
            ResultHandle rrh = rhe.getResultHandle(method);

            // Do type coercion of rockstar nulls (which are a special type, not null)
            // We need to check the type *before* converting to bytecode, since bytecode does not have the right type
            if (lhe.isNothing()) {
                lrh = coerceNothingIntoType(method, rrh);
            }
            if (rhe.isNothing()) {
                rrh = coerceNothingIntoType(method, lrh);
            }

            switch (operation) {
                case ADD -> {
                    if (isNumber(lrh) && isNumber(rrh)) {
                        return method.add(lrh, rrh);
                    } else {
                        ResultHandle lsrh = Gizmo.toString(method, lrh);
                        ResultHandle rsrh = Gizmo.toString(method, rrh);

                        return method.invokeVirtualMethod(
                                MethodDescriptor.ofMethod("java/lang/String", "concat", "Ljava/lang/String;", "Ljava/lang/String;"),
                                lsrh, rsrh);
                    }
                }
                case SUBTRACT -> {
                    // Handle subtraction by multiplying by -1 and adding
                    ResultHandle negativeRightSide = method.multiply(method.load(-1d), rrh);
                    return method.add(lrh, negativeRightSide);
                }
                case MULTIPLY -> {
                    return method.multiply(lrh, rrh);
                }
                case DIVIDE -> {
                    //  return method.divide(lhe.getResultHandle(method), rhe.getResultHandle(method));
                    throw new RuntimeException("Unsupported operation: divided we fall, and all that");
                }
                case EQUALITY_CHECK -> {
                    return doEqualityCheck(method, lrh, rrh);
                }
                case INEQUALITY_CHECK -> {
                    // A boolean negation in bytecode is a bit tricky, since the jvm doesn't really recognise booleans, so do a bitwise xor
                    // to simulate it
                    ResultHandle equalityCheck = doEqualityCheck(method, lrh, rrh);
                    return method.bitwiseXor(equalityCheck, method.load(true));
                }
                case GREATER_THAN_CHECK -> {
                    return doComparison(method, (ResultHandle eq) -> method.ifGreaterThanZero(eq),
                            lrh, rrh);
                }
                case LESS_THAN_CHECK -> {
                    return doComparison(method, (ResultHandle eq) -> method.ifLessThanZero(eq),
                            lrh, rrh);
                }
                case GREATER_OR_EQUAL_THAN_CHECK -> {
                    return doComparison(method, (ResultHandle eq) -> method.ifGreaterEqualZero(eq),
                            lrh, rrh);
                }
                case LESS_OR_EQUAL_THAN_CHECK -> {
                    return doComparison(method, (ResultHandle eq) -> method.ifLessEqualZero(eq),
                            lrh, rrh);
                }
                default -> throw new RuntimeException("Unsupported operation " + operation);
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
            } else if (valueClass == null) {
                return method.loadNull();
            } else if (valueClass == Nothing.class) {
                return method.loadNull();
            }
        }
        throw new RuntimeException("Confused expression: Could not interpret type " + valueClass);
    }

    private ResultHandle doEqualityCheck(BytecodeCreator method, ResultHandle lrh, ResultHandle rrh) {
        // Should we do this check in bytecode instead?
        if (!isNull(lrh)) {
            return method.invokeVirtualMethod(
                    MethodDescriptor.ofMethod("java/lang/Object", "equals", "Z", "Ljava/lang/Object;"),
                    lrh, rrh);
        } else {
            BytecodeCreator scope = method;
            AssignableResultHandle answer = scope.createVariable("Z");
            BranchResult br = method.ifReferencesEqual(lrh, rrh);
            br.trueBranch()
              .assign(answer, scope.load(true));
            br.falseBranch()
              .assign(answer, scope.load(false));
            return answer;
        }
    }

    public boolean isNothing() {
        return value == Constant.NOTHING;
    }

    private static AssignableResultHandle doComparison(BytecodeCreator method, Checker comparison,
                                                       ResultHandle lrh, ResultHandle rrh) {
        ResultHandle equalityCheck = method.invokeInterfaceMethod(
                MethodDescriptor.ofMethod("java/lang/Comparable", "compareTo", "I", "Ljava/lang/Object;"),
                lrh, rrh);
        BranchResult result = comparison.doCheck(equalityCheck);
        AssignableResultHandle answer = method.createVariable("Z");
        BytecodeCreator trueBranch = result.trueBranch();
        trueBranch.assign(answer, method.load(true));
        result.falseBranch()
              .assign(answer, method.load(false));
        return answer;
    }
}
