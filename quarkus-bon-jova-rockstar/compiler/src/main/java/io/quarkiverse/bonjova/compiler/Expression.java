package io.quarkiverse.bonjova.compiler;

import io.quarkiverse.bonjova.support.RockstarArray;
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

import static io.quarkiverse.bonjova.compiler.BytecodeGeneratingListener.isBoolean;
import static io.quarkiverse.bonjova.compiler.BytecodeGeneratingListener.isNumber;
import static io.quarkiverse.bonjova.compiler.BytecodeGeneratingListener.isObject;
import static io.quarkiverse.bonjova.compiler.Constant.coerceMysteriousIntoType;
import static io.quarkiverse.bonjova.compiler.Constant.coerceNothingIntoType;
import static io.quarkiverse.bonjova.support.Nothing.NOTHING;

public class Expression {

    public static final MethodDescriptor DECIMAL_FORMAT_METHOD = MethodDescriptor.ofMethod(DecimalFormat.class, "format",
            String.class, double.class);

    private static final MethodDescriptor BOOLEAN_VALUE_OF_METHOD = MethodDescriptor.ofMethod(Boolean.class, "booleanValue",
            boolean.class);
    private static final MethodDescriptor CONCAT_METHOD = MethodDescriptor.ofMethod(String.class, "concat", String.class,
            String.class);
    private static final MethodDescriptor COMPARE_TO_METHOD = MethodDescriptor.ofMethod(Comparable.class, "compareTo", "I",
            Object.class);
    private static final MethodDescriptor EQUALITY_METHOD = MethodDescriptor.ofMethod(Object.class, "equals", "Z",
            Object.class);
    private static final MethodDescriptor DOUBLE_CREATOR = MethodDescriptor.ofConstructor(Double.class, double.class);
    private static final MethodDescriptor constructor = MethodDescriptor.ofConstructor(BigDecimal.class, double.class);
    private static final MethodDescriptor divide = MethodDescriptor.ofMethod(BigDecimal.class, "divide", BigDecimal.class,
            BigDecimal.class, MathContext.class);
    private static final MethodDescriptor toDouble = MethodDescriptor.ofMethod(BigDecimal.class, "doubleValue", double.class);

    FieldDescriptor mathContext = FieldDescriptor.of(MathContext.class, "DECIMAL32", MathContext.class);
    private final String text;
    private String function;
    private Class<?> valueClass;
    private Object value;

    private ValueHolder valueHolder;
    // TODO can we get rid of this field?
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

    enum Context {
        SCALAR,
        BOOLEAN,
        STRING,
        NOT_OBJECT,
        NORMAL
    }

    public Expression(Rockstar.VariableContext ctx) {
        text = ctx.getText();
        variable = new Variable(ctx);
        value = variable.getVariableName();
        valueClass = variable.getVariableClass();
        valueHolder = variable;
    }

    public Expression(Rockstar.LiteralContext ctx) {
        text = ctx.getText();
        Literal l = new Literal(ctx);
        value = l.getValue();
        valueClass = l.getValueClass();
        valueHolder = l;
    }

    public Expression(Rockstar.ConstantContext ctx) {
        text = ctx.getText();
        Constant c = new Constant(ctx);
        value = c.getValue();
        valueClass = c.getValueClass();
        valueHolder = c;
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
            function = Variable.getNormalisedVariableName(functionCall.functionName.getText());

            params = Stream
                    .of(functionCall.argList().expression().stream().map(Expression::new),
                            functionCall.argList().variable().stream().map(Expression::new),
                            functionCall.argList().literal().stream().map(Expression::new),
                            functionCall.argList().constant().stream().map(Expression::new))
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
            valueHolder = l;
        } else if (constant != null) {
            Constant c = new Constant(constant);
            value = c.getValue();
            valueClass = c.getValueClass();
            valueHolder = c;
        } else if (variableContext != null) {
            variable = new Variable(variableContext);
            value = variable.getVariableName();
            valueClass = variable.getVariableClass();
            valueHolder = variable;
        }
    }

    private AssignableResultHandle doComparison(BytecodeCreator method, Checker comparison,
            ResultHandle lrh, ResultHandle rrh, ClassCreator classCreator) {

        ResultHandle safeLrh = coerceAwayNothing(method, lrh, rrh);

        ResultHandle safeRrh = coerceAwayNothing(method, rrh, lrh);

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
            // This is a literal or constant
            handle = getHandleForValueHolder(method, context);
        }

        // Now do an extra check if the context was boolean
        if (context == Context.BOOLEAN && !isBoolean(handle)) {
            AssignableResultHandle bool = method.createVariable(boolean.class);
            method.assign(bool, method.load(1));

            // This could still be a boolean, so check
            BytecodeCreator notBoolean;
            if (!isNumber(handle)) {
                ResultHandle isBoolean = method.instanceOf(handle, Boolean.class);
                BranchResult branchResult = method.ifTrue(isBoolean);
                BytecodeCreator booleanB = branchResult.trueBranch();
                // The boxing is needed to avoid verify errors
                booleanB.assign(bool,
                        booleanB.invokeVirtualMethod(BOOLEAN_VALUE_OF_METHOD, booleanB.checkCast(handle, Boolean.class)));

                // If not, check if this is 0, which we should convert to false
                notBoolean = branchResult.falseBranch();
            } else {
                notBoolean = method;
            }

            ResultHandle falsey = method.load(0);
            BranchResult nullCheck = notBoolean.ifNull(handle);
            nullCheck.trueBranch().assign(bool, falsey);
            BytecodeCreator notNull = nullCheck.falseBranch();
            ResultHandle doub = notNull.newInstance(DOUBLE_CREATOR, notNull.load(0d));
            BytecodeCreator isZero = notNull.ifTrue(
                    notNull.invokeVirtualMethod(EQUALITY_METHOD, doub, handle)).trueBranch();
            isZero.assign(bool, falsey);

            return bool;
        } else {
            return handle;
        }

    }

    private ResultHandle getHandleForValueHolder(BytecodeCreator method) {
        return valueHolder.getResultHandle(method);
    }

    private ResultHandle getHandleForValueHolder(BytecodeCreator method, Context context) {
        return valueHolder.getResultHandle(method, context);
    }

    private ResultHandle getHandleForArray(BytecodeCreator method, ClassCreator creator) {

        if (!arrayPop) {
            return arrayAccess.read(arrayAccessIndex, method, creator);
        } else {
            return arrayAccess.pop(method, creator);
        }
    }

    private ResultHandle getHandleForVariable(BytecodeCreator method, Context context) {
        ResultHandle rh = variable.getResultHandle(method);
        if (context == Context.NORMAL) {
            return rh;
        } else {
            if (context == Context.BOOLEAN) {
                // Coerce nothings in a boolean context
                rh = coerceNothingIntoType(method, rh, Context.BOOLEAN);
            }

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
        rrh = coerceFalsyTypes(method, rhe, rrh, classCreator);

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
        // This context isn't scalar, exactly, it's not-object - it could be boolean or string or number
        ResultHandle lrh = lhe.getResultHandle(method, classCreator, Context.NOT_OBJECT);
        ResultHandle rrh = rhe.getResultHandle(method, classCreator, Context.NOT_OBJECT);

        // Do type coercion of rockstar nulls (which are a special type, not null)

        lrh = coerceNothingIntoType(method, lrh, rrh, operation);
        rrh = coerceNothingIntoType(method, rrh, lrh, operation);

        switch (operation) {
            case ADD -> {

                BytecodeInvoker numericOperation = BytecodeCreator::add;
                BytecodeInvoker stringOperation = (bc, a, b) -> bc.invokeVirtualMethod(
                        CONCAT_METHOD,
                        a, b);
                ResultHandle answer = doOperation(method, lrh, rrh, numericOperation, stringOperation, classCreator);
                for (Expression extra : extraRhes) {
                    // This could be a fancy reduce with streams, but for works well enough
                    ResultHandle erh = extra.getResultHandle(method, classCreator, Context.SCALAR);

                    erh = coerceFalsyTypes(method, extra, erh, classCreator);

                    answer = doOperation(method, answer, erh, numericOperation, stringOperation, classCreator);
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
                ResultHandle answer = doOperation(method, lrh, rrh, numericOperation, stringOperation, classCreator);

                for (Expression extra : extraRhes) {
                    ResultHandle erh = extra.getResultHandle(method, classCreator, Context.SCALAR);

                    erh = coerceFalsyTypes(method, extra, erh, classCreator);

                    answer = doOperation(method, answer, erh, numericOperation, stringOperation, classCreator);
                }
                return answer;

            }
            case MULTIPLY -> {
                BytecodeInvoker numericOperation = BytecodeCreator::multiply;
                BytecodeInvoker stringOperation = unsupportedOperation("Multiplication of strings not yet implemented.");
                ResultHandle answer = doOperation(method, lrh, rrh, numericOperation, stringOperation, classCreator);
                for (Expression extra : extraRhes) {
                    ResultHandle erh = extra.getResultHandle(method, classCreator, Context.SCALAR);

                    erh = coerceFalsyTypes(method, extra, erh, classCreator);

                    answer = doOperation(method, answer, erh, numericOperation, stringOperation, classCreator);
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

                    erh = coerceFalsyTypes(method, extra, erh, classCreator);

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
                        lrh, rrh, classCreator);
            }
            case LESS_THAN_CHECK -> {
                return doComparison(method, method::ifLessThanZero,
                        lrh, rrh, classCreator);
            }
            case GREATER_OR_EQUAL_THAN_CHECK -> {
                return doComparison(method, method::ifGreaterEqualZero,
                        lrh, rrh, classCreator);
            }
            case LESS_OR_EQUAL_THAN_CHECK -> {
                return doComparison(method, method::ifLessEqualZero,
                        lrh, rrh, classCreator);
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

    private ResultHandle coerceFalsyTypes(BytecodeCreator method, Expression extra, ResultHandle erh,
            ClassCreator creator) {
        // TODO consolidate this with the other coerce method, do not call different ones on different paths
        if (extra.isNothing()) {
            erh = coerceNothingIntoType(method, extra.getResultHandle(method, creator), erh, operation);
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

        MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(classCreator.getClassName(), function,
                Object.class,
                paramClasses);
        ResultHandle[] rhs = args.toArray(new ResultHandle[] {});
        return method.invokeStaticMethod(
                methodDescriptor,
                rhs);
    }

    interface BytecodeInvoker {
        ResultHandle invoke(BytecodeCreator bc, ResultHandle a, ResultHandle b);
    }

    private ResultHandle doOperation(BytecodeCreator method, ResultHandle unsafelrh, ResultHandle unsaferrh,
            BytecodeInvoker numberCaseOp,
            BytecodeInvoker stringCaseOp, ClassCreator classCreator) {
        // If we know we're working with numbers, do the simplest thing
        if (isNumber(unsafelrh) && isNumber(unsaferrh)) {
            return numberCaseOp.invoke(method, unsafelrh, unsaferrh);
        }
        // Otherwise, do some checking and casting

        // First, check for nulls on both sides
        ResultHandle lrh = coerceAwayNothing(method, unsafelrh, unsaferrh);
        ResultHandle rrh = coerceAwayNothing(method, unsaferrh, unsafelrh);

        AssignableResultHandle answer = method.createVariable(Object.class);

        // Do some more checking for primitive types before trying to cast
        // Use 0 as equivalent to false
        ResultHandle lrhIsString = isBoolean(lrh) || isNumber(lrh) ? method.load(0) : method.instanceOf(lrh, String.class);

        ResultHandle rrhIsString = isBoolean(rrh) || isNumber(rrh) ? method.load(0) : method.instanceOf(rrh, String.class);
        ResultHandle someString = method.bitwiseOr(lrhIsString, rrhIsString);

        BranchResult br = method.ifTrue(someString);
        BytecodeCreator stringCase = br.trueBranch();
        ResultHandle lsrh = stringify(stringCase, lrh);
        ResultHandle rsrh = stringify(stringCase, rrh);
        ResultHandle thing = stringCaseOp.invoke(stringCase, lsrh, rsrh);
        stringCase.assign(answer, thing);

        BytecodeCreator otherCase = br.falseBranch();
        // Which way we go with the conversions depends on the operation - here we know it's a numeric or string context
        // TODO bring this logic and the logic to convert arrays to numbers from getHandleForVariable into the same place, maybe a general coerce method

        ResultHandle dlrh = coerceBooleanIntoNumber(otherCase, lrh);
        ResultHandle drrh = coerceBooleanIntoNumber(otherCase, rrh);

        ResultHandle added = numberCaseOp.invoke(otherCase, dlrh, drrh);
        otherCase.assign(answer, added);

        return answer;
    }

    // This is barely needed, since boolean in bytecode is represented as an integer, but Boolean will need attention
    private ResultHandle coerceBooleanIntoNumber(BytecodeCreator method, ResultHandle rh) {
        AssignableResultHandle answer = method.createVariable(double.class);
        if (isNumber(rh)) {
            return rh;
        } else {
            if (isBoolean(rh)) {
                BranchResult booleanTest = method.ifTrue(rh);
                booleanTest.trueBranch().assign(answer, method.load(1d));
                booleanTest.falseBranch().assign(answer, method.load(0d));
                return answer;
            } else {
                ResultHandle isBoolean = method.instanceOf(rh, Boolean.class);
                BranchResult br = method.ifTrue(isBoolean);
                BytecodeCreator falseBranch = br.falseBranch();
                falseBranch.assign(answer, rh); // Nothing to see here, move along

                BytecodeCreator booleanCase = br.trueBranch();
                BranchResult booleanTest = booleanCase.ifTrue(rh);
                booleanTest.trueBranch().assign(answer, method.load(1d));
                booleanTest.falseBranch().assign(answer, method.load(0d));

                return answer;

            }
        }
    }

    ResultHandle coerceAwayNothing(BytecodeCreator method, ResultHandle handle, ResultHandle referenceHandle) {
        // TODO call the method in Constant instead
        return Constant.coerceNothingIntoType(method, handle, referenceHandle, operation);
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
                            DECIMAL_FORMAT_METHOD,
                            formatterHandle, rh);

        } else if (isObject(rh)) {
            // If we really don't know, try and see if the thing is a number
            AssignableResultHandle answer = method.createVariable(String.class);
            BranchResult br = method.ifNull(rh);
            BytecodeCreator isNull = br.trueBranch();

            isNull.assign(answer, coerceMysteriousIntoType(isNull, Context.STRING));
            BytecodeCreator isNotNull = br.falseBranch();

            TryBlock tryBlock = isNotNull.tryBlock();
            ResultHandle formatterHandle = tryBlock.newInstance(
                    MethodDescriptor.ofConstructor(DecimalFormat.class, String.class),
                    tryBlock.load("#.#########"));
            tryBlock.assign(answer, tryBlock
                    .invokeVirtualMethod(
                            DECIMAL_FORMAT_METHOD,
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
        AssignableResultHandle answer = method.createVariable(boolean.class);
        method.assign(answer, method.load(true));
        BranchResult nullCheck = method.ifNull(lrh);
        BytecodeCreator isNull = nullCheck.trueBranch();

        // If the left hand side is null (ie mysterious), only return true if the right side is null
        BranchResult br = isNull.ifReferencesEqual(lrh, rrh);
        BytecodeCreator trueBranch = br.trueBranch();
        trueBranch
                .assign(answer, trueBranch.load(true));
        BytecodeCreator falseBranch = br.falseBranch();
        falseBranch
                .assign(answer, falseBranch.load(false));

        BytecodeCreator isNotNull = nullCheck.falseBranch();

        // Now do a check for nothing - we treat it as an empty string, NOT null, if the other side is a string

        isNotNull.assign(answer, isNotNull.invokeVirtualMethod(
                EQUALITY_METHOD,
                lrh, rrh));
        return answer;
    }

    public boolean isNothing() {
        return value == NOTHING;
    }

    public boolean isMysterious() {
        return valueClass == null;
    }

    enum Operation {
        ADD,
        SUBTRACT,
        MULTIPLY,
        CONJUNCTION,
        DISJUNCTION,
        JOINT_DENIAL,
        EQUALITY_CHECK,
        INEQUALITY_CHECK,
        GREATER_THAN_CHECK,
        LESS_THAN_CHECK,
        GREATER_OR_EQUAL_THAN_CHECK,
        LESS_OR_EQUAL_THAN_CHECK,
        DIVIDE
    }

    private enum UnaryOperation {
        NEGATION
    }
}
