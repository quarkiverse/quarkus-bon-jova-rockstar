package io.quarkiverse.bonjova.compiler;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

public interface ValueHolder {

    ResultHandle getResultHandle(BytecodeCreator method);

    ResultHandle getResultHandle(BytecodeCreator method, Expression.Context context);
}
