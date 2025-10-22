package io.quarkiverse.bonjova.compiler;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.WhileLoop;
import rock.Rockstar;

import java.util.Arrays;
import java.util.List;

import static io.quarkiverse.bonjova.compiler.Array.ADD_METHOD;
import static io.quarkiverse.bonjova.compiler.Array.TYPE_CLASS;

public class StringSplit {
    private static final MethodDescriptor SPLIT_METHOD = MethodDescriptor.ofMethod(String.class, "split", String[].class,
            String.class);
    private static final MethodDescriptor LENGTH_METHOD = MethodDescriptor.ofMethod(String.class, "length", int.class);
    private static final MethodDescriptor ADD_ALL_METHOD = MethodDescriptor.ofMethod(TYPE_CLASS, "addAll", void.class,
            List.class);
    private static final MethodDescriptor ASLIST_METHOD = MethodDescriptor.ofMethod(Arrays.class, "asList", List.class,
            Object[].class);
    private static final MethodDescriptor CHAR_AT_METHOD = MethodDescriptor.ofMethod(String.class, "charAt", char.class,
            int.class);

    private final Class<?> variableClass;
    private final Variable variable;
    private final Expression source;
    private Expression delimiter;

    public StringSplit(Rockstar.StringStmtContext ctx) {
        variableClass = TYPE_CLASS;
        // We definitely have one expression ...
        Rockstar.ExpressionContext sourceCtx = ctx.expression().get(0);
        source = new Expression(sourceCtx);

        if (ctx.variable() == null) {
            // If we're splitting in place, the expression had better just be a variable
            if (sourceCtx.variable() == null) {
                throw new RuntimeException("Cannot cut without a variable.");
            }
            variable = new Variable(sourceCtx.variable(), variableClass);
        } else {
            variable = new Variable(ctx.variable(), variableClass);
            // Variables should 'apply' to future pronouns when used in assignments
            variable.track();
        }

        if (ctx.KW_WITH() != null) {
            delimiter = new Expression(ctx.expression().get(1));
        }
    }

    public String getVariableName() {
        return variable.getVariableName();
    }

    public ResultHandle toCode(Block inBlock) {

        BytecodeCreator method = inBlock.method();
        ResultHandle toSplit = source.getResultHandle(inBlock);
        ResultHandle answer = method.newInstance(Array.CONSTRUCTOR);

        if (delimiter != null) {
            ResultHandle delimiterHandle = delimiter.getResultHandle(inBlock);
            ResultHandle splitArray = method.invokeVirtualMethod(SPLIT_METHOD, toSplit, delimiterHandle);
            ResultHandle splitList = method.invokeStaticMethod(ASLIST_METHOD, splitArray);
            method.invokeVirtualMethod(ADD_ALL_METHOD, answer, splitList);
        } else {
            // Doing the lambda in bytecode is hard, so just do a while loop
            // and working with the int stream is also hard ...
            // ... and working with the primitive array is also hard ...
            ResultHandle length = method.invokeVirtualMethod(LENGTH_METHOD, toSplit);

            AssignableResultHandle index = method.createVariable(int.class);
            method.assign(index, method.load(0));

            WhileLoop loop = method.whileLoop(bc -> bc.ifIntegerLessThan(index, length));
            BytecodeCreator block = loop.block();
            ResultHandle charAsString = block.invokeVirtualMethod(CHAR_AT_METHOD, toSplit, index);
            block.invokeVirtualMethod(ADD_METHOD, answer, charAsString);
            block.assign(index, block.increment(index));
        }

        variable.write(inBlock, answer);

        // Return the result handle for ease of testing
        return answer;

    }
}
