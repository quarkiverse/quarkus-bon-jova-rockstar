package org.example;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import rock.Rockstar;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.BytecodeGeneratingListener.isNull;
import static org.example.BytecodeGeneratingListener.isNumber;
import static org.example.Constant.coerceMysteriousIntoType;
import static org.example.Constant.coerceNothingIntoType;

public class Expression {

    private String function;
    private Class<?> valueClass;
    private Object value;
    private Variable variable;

    private Expression lhe;
    private Expression rhe;

    private List<Expression> params;

    private Operation operation;

    public Expression(Rockstar.ExpressionContext ctx) {
        if (ctx != null) {
            Rockstar.LiteralContext literal = ctx.literal();
            Rockstar.ConstantContext constant = ctx.constant();
            Rockstar.VariableContext variableContext = ctx.variable();
            Rockstar.FunctionCallContext functionCall = ctx.functionCall();

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
                // Best guess if we can't work out the exact value class
                valueClass = Object.class;

                if (ctx.KW_ADD() != null || "+".equals(ctx.op.getText())) {
                    operation = Operation.ADD;
                    if (lhe.getValueClass() == double.class && rhe.getValueClass() == double.class) {
                        valueClass = double.class;
                    } else if (lhe.getValueClass() == String.class || rhe.getValueClass() == String.class) {
                        valueClass = String.class;
                    }
                } else if (ctx.KW_SUBTRACT() != null || "-".equals(ctx.op.getText())) {
                    operation = Operation.SUBTRACT;
                    if (lhe.getValueClass() == double.class && rhe.getValueClass() == double.class) {
                        valueClass = double.class;
                    }
                } else if (ctx.KW_MULTIPLY() != null || "*".equals(ctx.op.getText())) {
                    operation = Operation.MULTIPLY;

                    if (lhe.getValueClass() == double.class && rhe.getValueClass() == double.class) {
                        valueClass = double.class;
                    } else if (lhe.getValueClass() == String.class || rhe.getValueClass() == String.class) {
                        valueClass = String.class;
                    }

                } else if (ctx.KW_DIVIDE() != null || "/".equals(ctx.op.getText())) {
                    if (lhe.getValueClass() == double.class && rhe.getValueClass() == double.class) {
                        valueClass = double.class;
                    }
                    operation = Operation.DIVIDE;
                }
            } else if (functionCall != null) {
                function = functionCall.functionName.getText();

                params = functionCall.argList()
                        .expression()
                        .stream()
                        .map(Expression::new)
                        .collect(Collectors.toList());
                valueClass = Object.class;
            } else if (literal != null) {
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
                valueClass = variable.getVariableClass();

            }
        }
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

    public Object getValue() {
        return value;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }

    public ResultHandle getResultHandle(BytecodeCreator method, ClassCreator classCreator) {
        if (function != null) {
            List<ResultHandle> args = params.stream()
                    .map(v -> v.getResultHandle(method, classCreator))
                    .collect(Collectors.toList());
            Class[] paramClasses = new Class[params.size()];
            Arrays.fill(paramClasses, Object.class);

            MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(classCreator.getClassName(), function, "Ljava/lang/Object;",
                    paramClasses);
            ResultHandle[] rhs = args.toArray(new ResultHandle[]{});
            return method.invokeStaticMethod(
                    methodDescriptor,
                    rhs);
        }
        if (operation != null) {
            ResultHandle lrh = lhe.getResultHandle(method, classCreator);
            ResultHandle rrh = rhe.getResultHandle(method, classCreator);

            // Do type coercion of rockstar nulls (which are a special type, not null)
            // We need to check the type *before* converting to bytecode, since bytecode does not have the right type
            if (lhe.isNothing()) {
                lrh = coerceNothingIntoType(method, rrh);
            }
            if (rhe.isNothing()) {
                rrh = coerceNothingIntoType(method, lrh);
            }

            if (lhe.isMysterious()) {
                lrh = coerceMysteriousIntoType(method, rrh);
            }
            if (rhe.isMysterious()) {
                rrh = coerceMysteriousIntoType(method, lrh);
            }

            switch (operation) {
                case ADD -> {

                    return doOperation(method, lrh, rrh, (bc, a, b) -> {
                        return bc.add(a, b);
                    }, (bc, a, b) -> {
                        ResultHandle lsrh = stringify(bc, a);
                        ResultHandle rsrh = stringify(bc, b);
                        return bc.invokeVirtualMethod(
                                MethodDescriptor.ofMethod("java/lang/String", "concat", "Ljava/lang/String;", "Ljava/lang/String;"),
                                lsrh, rsrh);
                    });
                }
                case SUBTRACT -> {

                    return doOperation(method, lrh, rrh, (bc, a, b) -> {
                        // Handle subtraction by multiplying by -1 and adding
                        ResultHandle negativeRightSide = bc.multiply(bc.load(-1d), b);
                        return bc.add(a, negativeRightSide);
                    }, (bc, a, b) -> {
                        bc.throwException(UnsupportedOperationException.class, "Subtraction of strings is not possible.");
                        return bc.load("nope");
                    });

                }
                case MULTIPLY -> {
                    return doOperation(method, lrh, rrh, (bc, a, b) -> bc.multiply(a, b), (bc, a, b) -> {// TODO of should be implemented
                        bc.throwException(UnsupportedOperationException.class, "Multiplication of strings not yet implemented.");
                        return bc.load("not yet");
                    });
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

    interface BytecodeInvoker {
        ResultHandle invoke(BytecodeCreator bc, ResultHandle a, ResultHandle b);
    }

    private ResultHandle doOperation(BytecodeCreator method, ResultHandle lrh, ResultHandle rrh, BytecodeInvoker numberCaseOp,
                                     BytecodeInvoker stringCaseOp) {
        // If we know we're working with numbers, do the simplest thing
        if (isNumber(lrh) && isNumber(rrh)) {
            return numberCaseOp.invoke(method, lrh, rrh);
        }
        // Otherwise, do some checking and casting
        AssignableResultHandle answer = method.createVariable(Object.class);
        // We want to do a special toString on numbers, to avoid tacking decimals onto integers
        TryBlock tryBlock = method.tryBlock();
        ResultHandle castlrh = tryBlock.checkCast(lrh, Double.class);
        ResultHandle castrrh = tryBlock.checkCast(rrh, Double.class);

        MethodDescriptor toDouble = MethodDescriptor.ofMethod("java/lang/Double", "doubleValue", double.class);
        ResultHandle dlrh = tryBlock.invokeVirtualMethod(toDouble, castlrh);
        ResultHandle drrh = tryBlock.invokeVirtualMethod(toDouble, castrrh);

        ResultHandle added = numberCaseOp.invoke(tryBlock, dlrh, drrh);

        tryBlock.assign(answer, added);

        CatchBlockCreator catchBlock = tryBlock.addCatch(ClassCastException.class);
        ResultHandle lsrh = stringify(catchBlock, lrh);
        ResultHandle rsrh = stringify(catchBlock, rrh);
        ResultHandle thing = stringCaseOp.invoke(catchBlock, lsrh, rsrh);
        catchBlock.assign(answer, thing);

        return answer;
    }

    private ResultHandle stringify(BytecodeCreator method, ResultHandle rh) {


        // We want to do a special toString on numbers, to avoid tacking decimal points onto integers
        final ResultHandle toStringed;
        if (isNumber(rh)) {
            // TODO save this in a field and re-use it (in a way that works in unit tests)
            ResultHandle formatterHandle = method.newInstance(MethodDescriptor.ofConstructor(DecimalFormat.class, String.class),
                    method.load("#.#########"));
            toStringed = method
                    .invokeVirtualMethod(
                            MethodDescriptor.ofMethod(DecimalFormat.class, "format", String.class, double.class),
                            formatterHandle, rh);

        } else {
            toStringed = Gizmo.toString(method, rh);
        }
        return toStringed;
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

    public boolean isMysterious() {
        return valueClass == null;
    }

    private enum Operation {
        ADD, SUBTRACT, MULTIPLY, EQUALITY_CHECK, INEQUALITY_CHECK, GREATER_THAN_CHECK,
        LESS_THAN_CHECK, GREATER_OR_EQUAL_THAN_CHECK, LESS_OR_EQUAL_THAN_CHECK, DIVIDE
    }
}
