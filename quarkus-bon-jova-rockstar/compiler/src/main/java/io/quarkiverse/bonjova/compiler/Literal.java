package io.quarkiverse.bonjova.compiler;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;
import org.antlr.v4.runtime.tree.TerminalNode;
import rock.Rockstar;

public class Literal implements ValueHolder {
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
            // TODO
            valueClass = Object.class;
        }
    }

    public Object getValue() {
        return value;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }

    public ResultHandle getResultHandle(Block block) {
        return getResultHandle(block, value, valueClass);
    }

    public ResultHandle getResultHandle(Block block, Expression.Context content) {
        // No context makes a difference to things we can define as literals
        return getResultHandle(block);
    }

    static ResultHandle getResultHandle(Block block, Object value, Class<?> valueClass) {
        BytecodeCreator method = block.method();
        ResultHandle answer;
        // We do not need to handle nothing
        // TODO do we need to handle mysterious? I don't think so?
        if (String.class.equals(valueClass)) {
            answer = method.load((String) value);
        } else if (double.class.equals(valueClass)) {
            answer = method.load((double) value);
        } else if (boolean.class.equals(valueClass)) {
            answer = method.load((boolean) value);
        } else if (valueClass == null) {
            answer = method.loadNull();
        } else if (value == null) {
            answer = method.loadNull(); // TODO is this needed?
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
}
