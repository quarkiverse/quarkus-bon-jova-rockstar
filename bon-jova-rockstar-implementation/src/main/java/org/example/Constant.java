package org.example;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import rock.Rockstar;

import static org.example.BytecodeGeneratingListener.isBoolean;
import static org.example.BytecodeGeneratingListener.isNumber;
import static org.example.BytecodeGeneratingListener.isObject;
import static org.example.BytecodeGeneratingListener.isString;

public class Constant {

    public static final Object NOTHING = new Nothing(); // TODO this should be the inner class

    private Class<?> valueClass;
    private Object value;

    public Constant(Rockstar.ConstantContext constant) {

        if (constant != null) {
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
            } else if (constant
                    .CONSTANT_NULL() != null) {
                value = NOTHING;
                valueClass = Object.class; // This can't be Nothing, because the value that is currently nothing might become a double or a string
            } else if (constant.CONSTANT_UNDEFINED() != null) {
                value = null;
                valueClass = null;
            }
        }
    }

    public static ResultHandle coerceNothingIntoType(BytecodeCreator method, ResultHandle referenceHandle) {
        if (isNumber(referenceHandle)) {
            return method.load(0d);
        } else if (isBoolean(referenceHandle)) {
            return method.load(false);
        } else if (isString(referenceHandle)) {
            return method.load("null");
        } else if (isObject(referenceHandle)) {
            // Do a dynamic coercion
            AssignableResultHandle answer = method.createVariable(Object.class);

            // If the reference is null, casts will all work, but we don't want to coerc, just return null
            BranchResult br = method.ifNull(referenceHandle);
            BytecodeCreator trueBranch = br.trueBranch();
            trueBranch.assign(answer, method.loadNull());

            BytecodeCreator falseBranch = br.falseBranch();
            TryBlock doubleTryBlock = falseBranch.tryBlock();
            doubleTryBlock.checkCast(referenceHandle, Double.class);
            doubleTryBlock.assign(answer, doubleTryBlock.load(0d));
            CatchBlockCreator doubleCatchBlock = doubleTryBlock.addCatch(ClassCastException.class);


            TryBlock booleanTryBlock = doubleCatchBlock.tryBlock();
            booleanTryBlock.checkCast(referenceHandle, boolean.class);
            booleanTryBlock.assign(answer, booleanTryBlock.load(false));
            CatchBlockCreator booleanCatchBlock = booleanTryBlock.addCatch(ClassCastException.class);

            TryBlock stringTryBlock = booleanCatchBlock.tryBlock();
            stringTryBlock.checkCast(referenceHandle, boolean.class);
            stringTryBlock.assign(answer, stringTryBlock.load(""));
            CatchBlockCreator stringCatchBlock = stringTryBlock.addCatch(ClassCastException.class);

            // We should alsmost never get to this point in the executed code
            stringCatchBlock.assign(answer, stringTryBlock.loadNull());

            return answer;

        } else {
            return method.loadNull();
        }
    }

    public static ResultHandle coerceMysteriousIntoType(BytecodeCreator method, ResultHandle referenceHandle) {
        if (isString(referenceHandle)) {
            return method.load("mysterious");
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
}
