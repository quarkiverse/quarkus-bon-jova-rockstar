package org.example;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import rock.Rockstar;

public class Input {
    private static int argIndex;
    private final String originalName;
    private final Class<?> variableClass;
    private final Variable variable;

    public Input(Rockstar.InputStmtContext ctx) {

        // Input always comes in as a string
        variableClass = String.class;
        variable = new Variable(ctx.variable(), variableClass);
        originalName = variable.getVariableName();
        // Variables should 'apply' to future pronouns when used in assignments
        variable.track();

        argIndex++;
    }

    public static void clearState() {
        argIndex = -1;
        // Since we use variables, clear its state as well
        Variable.clearState();
    }

    public String getVariableName() {
        return originalName;
    }

    public Class<?> getVariableClass() {
        return variableClass;
    }

    public Variable toCode(ClassCreator creator, BytecodeCreator method, MethodCreator main) {

        ResultHandle args = main.getMethodParam(0);
        AssignableResultHandle answer = method.createVariable(String.class);
        TryBlock tryBlock = method.tryBlock();
        ResultHandle rh = tryBlock.readArrayValue(args, argIndex);
        tryBlock.assign(answer, rh);
        CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
        catchBlock.assign(answer, catchBlock.loadNull());

        variable.write(method, creator, answer);
        return variable;
    }
}
