package io.quarkiverse.bonjova.compiler;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import org.antlr.v4.runtime.ParserRuleContext;

public record Block(ParserRuleContext ctx, BytecodeCreator method, ClassCreator creator, VariableScope variables,
        Block parent) {

    // The ctx is not strictly necessary, but very useful for debugging

}
