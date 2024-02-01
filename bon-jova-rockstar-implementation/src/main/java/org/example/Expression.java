package org.example;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import rock.Rockstar;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.example.BytecodeGeneratingListener.isBoolean;
import static org.example.BytecodeGeneratingListener.isNumber;
import static org.example.BytecodeGeneratingListener.isObject;
import static org.example.Constant.NOTHING;
import static org.example.Constant.coerceMysteriousIntoType;
import static org.example.Constant.coerceNothingIntoType;

public class Expression {

    private final String text;
    private String function;
    private Class<?> valueClass;
    private Object value;
    private Variable variable;
    private Array arrayAccess;
    private Expression arrayAccessIndex;
    private boolean arrayPop = false;

    private Expression lhe;
    private Expression rhe;
    private List<Expression> extraRhes;

    private List<Expression> params;

    private Operation operation;

    private UnaryOperation unaryOperation;
    private static final MethodDescriptor CONCAT_METHOD = MethodDescriptor.ofMethod(String.class, "concat", String.class, String.class);
    private static final MethodDescriptor COMPARE_TO_METHOD = MethodDescriptor.ofMethod(Comparable.class, "compareTo", "I", Object.class);
    private static final MethodDescriptor EQUALITY_METHOD = MethodDescriptor.ofMethod(Object.class, "equals", "Z", Object.class);
    private static final MethodDescriptor DOUBLE_CREATOR = MethodDescriptor.ofConstructor(Double.class, double.class);
    private static final MethodDescriptor constructor = MethodDescriptor.ofConstructor(BigDecimal.class, double.class);
    private static final MethodDescriptor divide = MethodDescriptor.ofMethod(BigDecimal.class, "divide", BigDecimal.class, BigDecimal.class, MathContext.class);
    private static final MethodDescriptor toDouble = MethodDescriptor.ofMethod(BigDecimal.class, "doubleValue", double.class);

    FieldDescriptor mathContext = FieldDescriptor.of(MathContext.class, "DECIMAL32", MathContext.class);

    enum Context {SCALAR, BOOLEAN, NORMAL}


    public Expression(Rockstar.VariableContext ctx) {
        text = ctx.getText();
        variable = new Variable(ctx);
        value = variable.getVariableName();
        valueClass = variable.getVariableClass();
    }

    public Expression(Rockstar.LiteralContext ctx) {
        text = ctx.getText();
        Literal l = new Literal(ctx);
        value = l.getValue();
        valueClass = l.getValueClass();
    }

    public Expression(Rockstar.ConstantContext ctx) {
        text = ctx.getText();
        Constant c = new Constant(ctx);
        value = c.getValue();
        valueClass = c.getValueClass();
    }

    public Expression(Rockstar.ExpressionContext ctx) {
        text = ctx.getText();
        Rockstar.LiteralContext literal = ctx.literal();
        Rockstar.ConstantContext constant = ctx.constant();
        Rockstar.VariableContext variableContext = ctx.variable();
        Rockstar.FunctionCallContext functionCall = ctx.functionCall();
        if (ctx.extraExpressions() != null) {
            extraRhes = ctx.extraExpressions().expression().stream().map(Expression::new).toList();
        } else {
            extraRhes = Collections.emptyList();
        }

        if (ctx.comparisionOp() != null) {
            // We don't know the answer, but we know the type
            valueClass = boolean.class;
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

            if (ctx.KW_ADD() != null || ctx.KW_WITH() != null || "+".equals(ctx.op.getText())) {
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
            } else if (ctx.KW_AND() != null) {
                if (lhe.getValueClass() == boolean.class && rhe.getValueClass() == boolean.class) {
                    valueClass = boolean.class;
                }
                operation = Operation.CONJUNCTION;
            } else if (ctx.KW_OR() != null) {
                if (lhe.getValueClass() == boolean.class && rhe.getValueClass() == boolean.class) {
                    valueClass = boolean.class;
                }
                operation = Operation.DISJUNCTION;
            } else if (ctx.KW_NOR() != null) {
                if (lhe.getValueClass() == boolean.class && rhe.getValueClass() == boolean.class) {
                    valueClass = boolean.class;
                }
                operation = Operation.JOINT_DENIAL;
            }
        } else if (ctx.KW_NOT() != null) {
            rhe = new Expression(ctx.rhe);
            // Best guess if we can't work out the exact value class
            valueClass = Object.class;
            if (rhe.getValueClass() == boolean.class) {
                valueClass = boolean.class;
            }
            unaryOperation = UnaryOperation.NEGATION;

        } else if (functionCall != null) {
            function = functionCall.functionName.getText();

            params = Stream.of(functionCall.argList().expression().stream().map(Expression::new), functionCall.argList().variable().stream().map(Expression::new), functionCall.argList().literal().stream().map(Expression::new), functionCall.argList().constant().stream().map(Expression::new))
                    .flatMap(Function.identity())
                    .collect(Collectors.toList());

            valueClass = Object.class;
        } else if (ctx.KW_AT() != null) {
            variable = new Variable(variableContext, Array.TYPE_CLASS);
            valueClass = Object.class;
            arrayAccess = new Array(variable);
            arrayAccessIndex = new Expression(ctx.expression(0));
        } else if (ctx.KW_ROLL() != null) {
            variable = new Variable(variableContext, Array.TYPE_CLASS);
            valueClass = Object.class;
            arrayAccess = new Array(variable);
            // TODO clunky! should this knowledge be sent to the array in initialisation?
            arrayPop = true;

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

    private static AssignableResultHandle doComparison(BytecodeCreator method, Checker comparison,
                                                       ResultHandle lrh, ResultHandle rrh) {

        ResultHandle safeLrh = coerceAwayNothing(method, lrh, rrh);
        safeLrh = coerceAwayNothing(method, safeLrh, rrh);

        ResultHandle safeRrh = coerceAwayNothing(method, rrh, lrh);
        safeRrh = coerceAwayNothing(method, safeRrh, lrh);


        ResultHandle equalityCheck = method.invokeInterfaceMethod(
                COMPARE_TO_METHOD,
                safeLrh, safeRrh);
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
        return getResultHandle(method, classCreator, Context.NORMAL);
    }

    public ResultHandle getResultHandle(BytecodeCreator method, ClassCreator classCreator, Context context) {
        ResultHandle handle;
        if (function != null) {
            handle = getHandleForFunction(method, classCreator);
        } else if (operation != null) {
            handle = getHandleForOperation(method, classCreator);
        } else if (unaryOperation != null) {
            handle = getHandleForUnaryOperation(method, classCreator);
        } else if (arrayAccess != null) {
            handle = getHandleForArray(method, classCreator);
        } else if (variable != null) {
            handle = getHandleForVariable(method, context);
        } else {
            // This is a literal
            handle = getHandleForLiteral(method);
        }

        // Now do an extra check if the context was boolean
        if (context == Context.BOOLEAN && !isBoolean(handle)) {
            AssignableResultHandle bool = method.createVariable(boolean.class);
            method.assign(bool, method.load(1));
            ResultHandle falsey = method.load(0);
            BranchResult nullCheck = method.ifNull(handle);
            nullCheck.trueBranch().assign(bool, falsey);
            BytecodeCreator notNull = nullCheck.falseBranch();
            ResultHandle doub = notNull.newInstance(DOUBLE_CREATOR, notNull.load(0d));
            // Be lazy and do a comparison so we don't have to cast
            BytecodeCreator isZero = notNull.ifTrue(
                    notNull.invokeVirtualMethod(EQUALITY_METHOD, doub, handle)).trueBranch();
            isZero.assign(bool, falsey);

            return bool;
        } else {
            return handle;
        }

    }

    private ResultHandle getHandleForLiteral(BytecodeCreator method) {
        ResultHandle answer;
        if (String.class.equals(valueClass)) {
            answer = method.load((String) value);
        } else if (double.class.equals(valueClass)) {
            answer = method.load((double) value);
        } else if (boolean.class.equals(valueClass)) {
            answer = method.load((boolean) value);
        } else if (valueClass == null) {
            answer = method.loadNull();
        } else if (value == NOTHING) {
            answer = method.loadNull();
        } else if (value == null) {
            answer = method.loadNull();
        } else if (value instanceof String) {
            answer = method.load((String) value);
        } else if (value instanceof Double) {
            answer = method.load((double) value);
        } else if (value instanceof Boolean) {
            answer = method.load((Boolean) value);
        } else {
            throw new RuntimeException("Confused expression: Could not interpret type " + valueClass + " with value " + value);
        }
        return answer;
    }

    private ResultHandle getHandleForArray(BytecodeCreator method, ClassCreator creator) {

        if (!arrayPop) {
            return arrayAccess.read(arrayAccessIndex, method, creator);
        } else {
            return arrayAccess.pop(method, creator);
        }
    }

    private ResultHandle getHandleForVariable(BytecodeCreator method, Context context) {
        ResultHandle rh = variable.read(method);
        if (context == Context.NORMAL) {
            return rh;
        } else {
            // For boolean contexts, we also want arrays to go to a number length
            BranchResult arrayCheck = method.ifTrue(method.instanceOf(rh, RockstarArray.class));
            AssignableResultHandle answer = method.createVariable(Object.class);
            BytecodeCreator isArray = arrayCheck.trueBranch();
            isArray.assign(answer, Array.toScalarContext(variable, isArray));
            BytecodeCreator isNotArray = arrayCheck.falseBranch();
            isNotArray.assign(answer, rh);
            return answer;
        }
    }

    private ResultHandle getHandleForUnaryOperation(BytecodeCreator method, ClassCreator classCreator) {
        ResultHandle rrh = rhe.getResultHandle(method, classCreator, Context.BOOLEAN);

        // Do type coercion of rockstar nulls (which are a special type, not null)
        // We need to check the type *before* converting to bytecode, since bytecode does not have the right type

        // TODO this may not be needed since we passed a boolean context
        rrh = coerceFalsyTypes(method, rhe, rrh);

        switch (unaryOperation) {
            case NEGATION -> {
                AssignableResultHandle answer = method.createVariable(boolean.class);
                method.assign(answer, method.load(false));
                method.ifFalse(rrh).trueBranch().assign(answer, method.load(true));
                return answer;
            }
            default -> throw new RuntimeException("Unsupported operation " + operation);
        }
    }

    private ResultHandle getHandleForOperation(BytecodeCreator method, ClassCreator classCreator) {
        ResultHandle lrh = lhe.getResultHandle(method, classCreator, Context.SCALAR);
        ResultHandle rrh = rhe.getResultHandle(method, classCreator, Context.SCALAR);

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

                BytecodeInvoker numericOperation = BytecodeCreator::add;
                BytecodeInvoker stringOperation = (bc, a, b) -> {
                    ResultHandle lsrh = stringify(bc, a);
                    ResultHandle rsrh = stringify(bc, b);
                    return bc.invokeVirtualMethod(
                            CONCAT_METHOD,
                            lsrh, rsrh);
                };
                ResultHandle answer = doOperation(method, lrh, rrh, numericOperation, stringOperation);
                for (Expression extra : extraRhes) {
                    // This could be a fancy reduce with streams, but for works well enough
                    ResultHandle erh = extra.getResultHandle(method, classCreator, Context.SCALAR);

                    erh = coerceFalsyTypes(method, extra, erh);

                    answer = doOperation(method, answer, erh, numericOperation, stringOperation);
                }
                return answer;
            }
            case SUBTRACT -> {

                BytecodeInvoker numericOperation = (bc, a, b) -> {
                    // Handle subtraction by multiplying by -1 and adding
                    ResultHandle negativeRightSide = bc.multiply(bc.load(-1d), b);
                    return bc.add(a, negativeRightSide);
                };
                BytecodeInvoker stringOperation = unsupportedOperation("Subtraction of strings is not possible.");
                ResultHandle answer = doOperation(method, lrh, rrh, numericOperation, stringOperation);

                for (Expression extra : extraRhes) {
                    ResultHandle erh = extra.getResultHandle(method, classCreator, Context.SCALAR);

                    erh = coerceFalsyTypes(method, extra, erh);

                    answer = doOperation(method, answer, erh, numericOperation, stringOperation);
                }
                return answer;

            }
            case MULTIPLY -> {
                BytecodeInvoker numericOperation = BytecodeCreator::multiply;
                BytecodeInvoker stringOperation = unsupportedOperation("Multiplication of strings not yet implemented.");
                ResultHandle answer = doOperation(method, lrh, rrh, numericOperation, stringOperation);
                for (Expression extra : extraRhes) {
                    ResultHandle erh = extra.getResultHandle(method, classCreator, Context.SCALAR);

                    erh = coerceFalsyTypes(method, extra, erh);

                    answer = doOperation(method, answer, erh, numericOperation, stringOperation);
                }
                return answer;
            }
            case DIVIDE -> {
                // Needs Gizmo 1.8, which is not yet accessible in Quarkus extensions
//                return doOperation(method, lrh, rrh, (bc, a, b) -> bc.divide(a, b), (bc, a, b) -> {
//                    bc.throwException(IllegalArgumentException.class, "Divided we fall: Division of strings is not possible.");
//                    return bc.loadNull();
//                });

                // So in the interim, do a work around

                ResultHandle answer = divide(method, lrh, rrh);
                for (Expression extra : extraRhes) {
                    ResultHandle erh = extra.getResultHandle(method, classCreator, Context.SCALAR);

                    erh = coerceFalsyTypes(method, extra, erh);

                    answer = divide(method, answer, erh);
                }
                return answer;
            }
            case CONJUNCTION -> {
                // See https://stackoverflow.com/questions/17052001/binary-expression-in-asm-compiler/17053797#17053797
                // To implement the short circuit, we need several operations
                AssignableResultHandle answer = method.createVariable(boolean.class);
                method.assign(answer, method.load(false));
                BranchResult check1 = method.ifTrue(lrh);
                BytecodeCreator next = check1.trueBranch();
                BranchResult check2 = next.ifTrue(rrh);
                check2.trueBranch().assign(answer, method.load(true));

                return answer;
            }
            case DISJUNCTION -> {
                // To implement the short circuit, we need several operations
                AssignableResultHandle answer = method.createVariable(boolean.class);
                method.assign(answer, method.load(false));
                BranchResult check1 = method.ifTrue(lrh);
                check1.trueBranch().assign(answer, method.load(true));
                BranchResult check2 = check1.falseBranch().ifTrue(rrh);
                check2.trueBranch().assign(answer, method.load(true));

                return answer;
            }
            case JOINT_DENIAL -> {
                // See https://stackoverflow.com/questions/17052001/binary-expression-in-asm-compiler/17053797#17053797
                // To implement the short circuit, we need several operations
                AssignableResultHandle answer = method.createVariable(boolean.class);
                method.assign(answer, method.load(false));
                BranchResult check1 = method.ifFalse(lrh);
                BytecodeCreator next = check1.trueBranch();
                BranchResult check2 = next.ifFalse(rrh);
                check2.trueBranch().assign(answer, method.load(true));

                return answer;
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
                return doComparison(method, method::ifGreaterThanZero,
                        lrh, rrh);
            }
            case LESS_THAN_CHECK -> {
                return doComparison(method, method::ifLessThanZero,
                        lrh, rrh);
            }
            case GREATER_OR_EQUAL_THAN_CHECK -> {
                return doComparison(method, method::ifGreaterEqualZero,
                        lrh, rrh);
            }
            case LESS_OR_EQUAL_THAN_CHECK -> {
                return doComparison(method, method::ifLessEqualZero,
                        lrh, rrh);
            }
            default -> throw new RuntimeException("Unsupported operation " + operation);
        }
    }

    private ResultHandle divide(BytecodeCreator method, ResultHandle lrh, ResultHandle rrh) {
        ResultHandle lbd = method.newInstance(constructor, lrh);
        ResultHandle rbd = method.newInstance(constructor, rrh);
        ResultHandle answer = method.invokeVirtualMethod(divide, lbd, rbd, method.readStaticField(mathContext));
        ResultHandle doubleAnswer = method.invokeVirtualMethod(toDouble, answer);
        return doubleAnswer;
    }

    private static BytecodeInvoker unsupportedOperation(String message) {
        return (bc, a, b) -> {
            bc.throwException(UnsupportedOperationException.class, message);
            return bc.load("nope");
        };
    }

    private static ResultHandle coerceFalsyTypes(BytecodeCreator method, Expression extra, ResultHandle erh) {
        if (extra.isNothing()) {
            erh = coerceNothingIntoType(method, erh);
        }

        if (extra.isMysterious()) {
            erh = coerceMysteriousIntoType(method, erh);
        }
        return erh;
    }

    private ResultHandle getHandleForFunction(BytecodeCreator method, ClassCreator classCreator) {
        List<ResultHandle> args = params.stream()
                .map(v -> v.getResultHandle(method, classCreator))
                .toList();
        Class[] paramClasses = new Class[params.size()];
        Arrays.fill(paramClasses, Object.class);

        MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(classCreator.getClassName(), function, "Ljava/lang/Object;",
                paramClasses);
        ResultHandle[] rhs = args.toArray(new ResultHandle[]{});
        return method.invokeStaticMethod(
                methodDescriptor,
                rhs);
    }

    interface BytecodeInvoker {
        ResultHandle invoke(BytecodeCreator bc, ResultHandle a, ResultHandle b);
    }

    private ResultHandle doOperation(BytecodeCreator method, ResultHandle unsafelrh, ResultHandle unsaferrh, BytecodeInvoker numberCaseOp,
                                     BytecodeInvoker stringCaseOp) {
        // If we know we're working with numbers, do the simplest thing
        if (isNumber(unsafelrh) && isNumber(unsaferrh)) {
            return numberCaseOp.invoke(method, unsafelrh, unsaferrh);
        }
        // Otherwise, do some checking and casting

        // First, check for nulls on both sides
        ResultHandle lrh = coerceAwayNothing(method, unsafelrh, unsaferrh);
        ResultHandle rrh = coerceAwayNothing(method, unsaferrh, unsafelrh);

        AssignableResultHandle answer = method.createVariable(Object.class);
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

    static ResultHandle coerceAwayNothing(BytecodeCreator method, ResultHandle handle, ResultHandle referenceHandle) {
        AssignableResultHandle answer = method.createVariable(Object.class);
        BranchResult nullCheck = method.ifNull(handle);
        BytecodeCreator trueBranch = nullCheck.trueBranch();
        BytecodeCreator falseBranch = nullCheck.falseBranch();
        // TODO this will still fail if both sides are null, but that's maybe fair enough (although Satriani coerces to zero in that case)
        trueBranch.assign(answer, coerceNothingIntoType(trueBranch, referenceHandle));
        falseBranch.assign(answer, handle);

        return answer;
    }

    private ResultHandle stringify(BytecodeCreator method, ResultHandle rh) {


        // We want to do a special toString on numbers, to avoid tacking decimal points onto integers
        ResultHandle toStringed;
        if (isNumber(rh)) {
            // TODO save this in a field and re-use it (in a way that works in unit tests)
            ResultHandle formatterHandle = method.newInstance(MethodDescriptor.ofConstructor(DecimalFormat.class, String.class),
                    method.load("#.#########"));
            toStringed = method
                    .invokeVirtualMethod(
                            MethodDescriptor.ofMethod(DecimalFormat.class, "format", String.class, double.class),
                            formatterHandle, rh);

        } else if (isObject(rh)) {
            // If we really don't know, try and see if the thing is a number
            AssignableResultHandle answer = method.createVariable(String.class);
            TryBlock tryBlock = method.tryBlock();
            ResultHandle formatterHandle = tryBlock.newInstance(MethodDescriptor.ofConstructor(DecimalFormat.class, String.class),
                    tryBlock.load("#.#########"));
            tryBlock.assign(answer, tryBlock
                    .invokeVirtualMethod(
                            MethodDescriptor.ofMethod(DecimalFormat.class, "format", String.class, double.class),
                            formatterHandle, rh));
            CatchBlockCreator catchBlockCreator = tryBlock.addCatch(Throwable.class);
            catchBlockCreator.assign(answer, Gizmo.toString(catchBlockCreator, rh));

            toStringed = answer;
        } else {

            toStringed = Gizmo.toString(method, rh);
        }
        return toStringed;
    }

    private ResultHandle doEqualityCheck(BytecodeCreator method, ResultHandle lrh, ResultHandle rrh) {
        AssignableResultHandle answer = method.createVariable("Z");
        BranchResult nullCheck = method.ifNull(lrh);
        BytecodeCreator isNull = nullCheck.trueBranch();

        // If the left hand side is null, only return true if the right side is null
        BranchResult br = isNull.ifReferencesEqual(lrh, rrh);
        BytecodeCreator trueBranch = br.trueBranch();
        trueBranch
                .assign(answer, trueBranch.load(true));
        BytecodeCreator falseBranch = br.falseBranch();
        falseBranch
                .assign(answer, falseBranch.load(false));

        BytecodeCreator isNotNull = nullCheck.falseBranch();

        isNotNull.assign(answer, isNotNull.invokeVirtualMethod(
                EQUALITY_METHOD,
                lrh, rrh));

        return answer;
    }

    public boolean isNothing() {
        return value == Constant.NOTHING;
    }

    public boolean isMysterious() {
        return valueClass == null;
    }

    private enum Operation {
        ADD, SUBTRACT, MULTIPLY, CONJUNCTION, DISJUNCTION, JOINT_DENIAL, EQUALITY_CHECK, INEQUALITY_CHECK, GREATER_THAN_CHECK,
        LESS_THAN_CHECK, GREATER_OR_EQUAL_THAN_CHECK, LESS_OR_EQUAL_THAN_CHECK, DIVIDE
    }

    private enum UnaryOperation {
        NEGATION
    }
}
