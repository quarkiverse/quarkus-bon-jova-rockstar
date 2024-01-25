package org.example;

import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import org.antlr.v4.runtime.tree.TerminalNode;
import rock.Rockstar;

public class Literal {
    private Class<?> valueClass;
    private Object value;

    public Literal(Rockstar.LiteralContext literal) {
        if (literal != null) {
            if (literal.NUMERIC_LITERAL() != null) {

                TerminalNode num = literal
                        .NUMERIC_LITERAL();
                //  Numbers in Rockstar are double-precision floating point numbers, stored according to the IEEE 754 standard.
                // ... so we don't need to worry about integers
                value = Double.parseDouble(num.getText());

                valueClass = double.class;

            } else if (literal.STRING_LITERAL() != null) {
                value = literal
                        .STRING_LITERAL()
                        .getText()
                        .replaceAll("\"", "");
                // Strip out the quotes around literals (doing it in the listener rather than the lexer is simpler, and apparently
                // idiomatic-ish)
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

    public ResultHandle getResultHandle(MethodCreator method) {
        // This code is duplicated in Expression and assignment; we should check if it can be consolidated
        Object value = getValue();

        if (String.class.equals(valueClass)) {
            return method.load((String) value);
        } else if (double.class.equals(valueClass)) {
            return method.load((double) value);
        } else if (boolean.class.equals(valueClass)) {
            return method.load((boolean) value);
        } else {
            throw new RuntimeException("Internal error: unknown type " + value);
        }
    }
}
