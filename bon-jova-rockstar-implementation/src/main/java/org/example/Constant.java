package org.example;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;
import rock.Rockstar;

import static org.example.BytecodeGeneratingListener.isBoolean;
import static org.example.BytecodeGeneratingListener.isNumber;
import static org.example.BytecodeGeneratingListener.isString;

public class Constant {

    public static final Nothing NOTHING = new Nothing();

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
                valueClass = Nothing.class;
            } else if (constant.CONSTANT_UNDEFINED() != null) {
                value = null;
                valueClass = null;
            }
        }
    }


    public Object getValue() {
        return value;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }

    public static ResultHandle coerceNothingIntoType(BytecodeCreator method, ResultHandle referenceHandle) {
        if (isNumber(referenceHandle)) {
            return method.load(0d);
        } else if (isBoolean(referenceHandle)) {
            return method.load(false);
        } else if (isString(referenceHandle)) {
            return method.load("");
        } else {
            return method.loadNull();
        }
    }
}
