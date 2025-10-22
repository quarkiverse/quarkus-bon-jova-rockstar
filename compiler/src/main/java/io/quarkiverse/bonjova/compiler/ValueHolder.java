package io.quarkiverse.bonjova.compiler;

import io.quarkus.gizmo.ResultHandle;

public interface ValueHolder {

    ResultHandle getResultHandle(Block block);

    ResultHandle getResultHandle(Block block, Expression.Context context);
}
