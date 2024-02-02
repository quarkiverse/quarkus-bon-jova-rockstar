package io.quarkiverse.bonjova.compiler;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import rock.Rockstar;

public class Rounding {
    private static final MethodDescriptor ROUND_METHOD = MethodDescriptor.ofMethod("java/lang/Math", "round", "J", "D");
    private static final MethodDescriptor FLOOR_METHOD = MethodDescriptor.ofMethod("java/lang/Math", "floor", "D", "D");
    private static final MethodDescriptor CEIL_METHOD = MethodDescriptor.ofMethod("java/lang/Math", "ceil", "D", "D");
    private static final MethodDescriptor toDouble = MethodDescriptor.ofMethod(Long.class, "doubleValue", double.class);

    Variable variable;
    final RoundingMethod roundingMethod;

    private enum RoundingMethod {ROUND, UP, DOWN}


    public Rounding(Rockstar.RoundingStmtContext ctx) {
        variable = new Variable(ctx.variable());
        if (ctx.KW_DOWN() != null) {
            roundingMethod = RoundingMethod.DOWN;
        } else if (ctx.KW_UP() != null) {
            roundingMethod = RoundingMethod.UP;
        } else {
            roundingMethod = RoundingMethod.ROUND;
        }
    }

    public ResultHandle toCode(BytecodeCreator method, ClassCreator creator) {
        ResultHandle variableContents = variable.read(method);

        ResultHandle answer = null;

        switch (roundingMethod) {
            case ROUND -> {
                ResultHandle rounded = method.invokeStaticMethod(ROUND_METHOD, variableContents);
                answer = method.invokeVirtualMethod(toDouble, rounded);
            }
            case DOWN -> {
                answer = method.invokeStaticMethod(FLOOR_METHOD, variableContents);
            }
            case UP -> {
                answer = method.invokeStaticMethod(CEIL_METHOD, variableContents);
            }
        }


        // Write the update back to the variable
        if (variable != null) {
            variable.write(method, creator, answer);
        }

        // Also return the result handle, for ease of testing
        return answer;
    }
}
