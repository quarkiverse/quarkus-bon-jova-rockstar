package org.example;

import rock.Rockstar;

public class Constant {
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
            }
        }
    }


    public Object getValue() {
        return value;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }
}
