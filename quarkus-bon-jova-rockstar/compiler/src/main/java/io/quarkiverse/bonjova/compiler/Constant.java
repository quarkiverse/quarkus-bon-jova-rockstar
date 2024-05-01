package io.quarkiverse.bonjova.compiler;

import io.quarkiverse.bonjova.support.Nothing;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import rock.Rockstar;

import static io.quarkiverse.bonjova.compiler.BytecodeGeneratingListener.isBoolean;
import static io.quarkiverse.bonjova.compiler.BytecodeGeneratingListener.isNumber;
import static io.quarkiverse.bonjova.compiler.BytecodeGeneratingListener.isString;
import static io.quarkiverse.bonjova.compiler.Expression.Operation.ADD;
import static io.quarkiverse.bonjova.support.Nothing.NOTHING;
import static io.quarkiverse.bonjova.support.Nothing.NULL;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class Constant implements ValueHolder {

    public static final String MYSTERIOUS = "mysterious";
    private static final String EMPTY_STRING = "";
    public static final MethodDescriptor COERCE_METHOD = MethodDescriptor.ofMethod(Nothing.class, "coerce", Object.class,
            Object.class);
    public static final MethodDescriptor COERCE_TO_STRING_METHOD = MethodDescriptor.ofMethod(Nothing.class, "coerceToString",
            String.class);
    public static final MethodDescriptor COERCE_TO_NUMBER_METHOD = MethodDescriptor.ofMethod(Nothing.class, "coerceToNumber",
            Double.class);
    public static final MethodDescriptor COERCE_TO_BOOLEAN_METHOD = MethodDescriptor.ofMethod(Nothing.class, "coerceToBoolean",
            Boolean.class);
    public static final MethodDescriptor COERCE_TO_SOMETHING_METHOD = MethodDescriptor.ofMethod(Nothing.class,
            "coerceToSomething", Object.class);
    public static final FieldDescriptor NOTHING_FIELD = FieldDescriptor.of(Nothing.class, "NOTHING", Nothing.class);
    public static final MethodDescriptor COERCE_METHOD_WITH_VISIBLE_NULLS = MethodDescriptor.ofMethod(Nothing.class,
            "coerceWithVisibleNulls", Object.class,
            Object.class);
    private Class<?> valueClass;
    private Object value;

    public Constant(Rockstar.ConstantContext constant) {

        if (constant != null) {
            if (constant
                    .CONSTANT_TRUE() != null) {
                value = TRUE;
                valueClass = boolean.class;
            } else if (constant
                    .CONSTANT_FALSE() != null) {
                value = FALSE;
                valueClass = boolean.class;
            } else if (constant
                    .CONSTANT_EMPTY() != null) {
                value = EMPTY_STRING;
                valueClass = String.class;
            } else if (constant
                    .CONSTANT_NULL() != null) {
                value = NOTHING;
                valueClass = Object.class; // This can't be Nothing, because the value that is currently nothing might become a double or a string
            } else if (constant.CONSTANT_UNDEFINED() != null) {
                value = null;
                valueClass = null;
            }

            valueClass = Object.class;
        }
    }

    public static ResultHandle coerceNothingIntoType(BytecodeCreator method, ResultHandle original,
            ResultHandle referenceHandle, Expression.Operation operation) {
        AssignableResultHandle answer = method.createVariable(Object.class);
        // Don't do instanceof checks on a primitive
        if (isNumber(original) || isBoolean(original)) {
            // TODO could also bypass stuff for strings? except for mysterious
            return original;
        } else {
            ResultHandle checkResult = method.instanceOf(original, Nothing.class);
            BranchResult br = method.ifTrue(checkResult);
            BytecodeCreator isInstance = br.trueBranch();
            ResultHandle nothingHandle = isInstance.checkCast(original, Nothing.class);
            if (operation == ADD) {
                isInstance.assign(answer, isInstance.invokeVirtualMethod(
                        COERCE_METHOD_WITH_VISIBLE_NULLS, nothingHandle, referenceHandle));
            } else {
                isInstance.assign(answer, isInstance.invokeVirtualMethod(
                        COERCE_METHOD, nothingHandle, referenceHandle));
            }

            BytecodeCreator isNotInstance = br.falseBranch();
            // In this case, we can just return the original
            isNotInstance.assign(answer, original);
            return answer;
        }

    }

    public static ResultHandle coerceNothingIntoType(Block block, ResultHandle original,
            Expression.Context context) {
        BytecodeCreator method = block.method();
        AssignableResultHandle answer = method.createVariable(Object.class);
        // Don't do instanceof checks on a primitive
        if (isNumber(original) || isBoolean(original)) {
            // TODO could also bypass stuff for strings? except for mysterious
            return original;
        } else {
            ResultHandle checkResult = method.instanceOf(original, Nothing.class);
            BranchResult br = method.ifTrue(checkResult);
            BytecodeCreator isInstance = br.trueBranch();
            ResultHandle nothingHandle = isInstance.checkCast(original, Nothing.class);
            if (context == Expression.Context.STRING) {
                isInstance.assign(answer, isInstance.invokeVirtualMethod(
                        COERCE_TO_STRING_METHOD, nothingHandle));
            } else if (context == Expression.Context.SCALAR) {
                isInstance.assign(answer, isInstance.invokeVirtualMethod(
                        COERCE_TO_NUMBER_METHOD, nothingHandle));
            } else if (context == Expression.Context.BOOLEAN) {
                isInstance.assign(answer, isInstance.invokeVirtualMethod(
                        COERCE_TO_BOOLEAN_METHOD, nothingHandle));
            } else {
                isInstance.assign(answer, isInstance.invokeVirtualMethod(
                        COERCE_TO_SOMETHING_METHOD, nothingHandle));
            }

            BytecodeCreator isNotInstance = br.falseBranch();
            // In this case, we can just return the original
            isNotInstance.assign(answer, original);
            return answer;
        }

    }

    public static ResultHandle coerceMysteriousIntoType(BytecodeCreator method, ResultHandle referenceHandle) {
        if (isString(referenceHandle)) {
            return method.load(MYSTERIOUS);
        } else {
            return method.loadNull();
        }
    }

    public static ResultHandle coerceMysteriousIntoType(BytecodeCreator method, Expression.Context context) {
        if (context == Expression.Context.STRING) {
            return method.load(MYSTERIOUS);
        } else {
            return method.loadNull();
        }
    }

    public Object getValue() {
        return value;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }

    public ResultHandle getResultHandle(Block block) {
        return getResultHandle(block, Expression.Context.NORMAL);
    }

    public ResultHandle getResultHandle(Block block, Expression.Context context) {
        BytecodeCreator method = block.method();
        // The only options are true or false, empty string, nothing, or null
        ResultHandle answer;
        if (value == TRUE || value == FALSE) {
            answer = method.load((boolean) value);
        } else if (value == EMPTY_STRING) {
            answer = method.load(EMPTY_STRING);
        } else if (value == NOTHING) { // We can't coerce here, because we have nothing to use as a reference type
            if (context == Expression.Context.NORMAL) {
                // TODO at what point should we coerce? is it here, or do we pass nothing around?
                // TODO combine these - we cannot coerce because we do not have a reference type
                answer = method.readStaticField(NOTHING_FIELD);
            } else if (context == Expression.Context.BOOLEAN || context == Expression.Context.SCALAR
                    || context == Expression.Context.STRING) {
                // TODO what about the other contexts?
                answer = coerceNothingIntoType(method, context);
            } else {
                answer = method.readStaticField(NOTHING_FIELD);
            }

        } else if (value == null) {
            answer = method.loadNull();
        } else {
            throw new RuntimeException("Confused constant: Could not interpret type " + valueClass + " with value " + value);
        }
        return answer;
    }

    // TODO change names to make it clear that this is for null, the other is checking
    private ResultHandle coerceNothingIntoType(BytecodeCreator method, Expression.Context context) {
        // TODO consolidate this with the other checking logic
        if (context == Expression.Context.BOOLEAN) {
            return method.load(false);
        } else if (context == Expression.Context.SCALAR) {
            return method.load(0d);
        } // TODO do we need a context for strings?
        else {
            return method.load(NULL);
        }
    }
}
