package org.example;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.WhileLoop;
import rock.Rockstar;

import java.util.Arrays;
import java.util.List;

import static org.example.Array.ADD_METHOD;
import static org.example.Array.TYPE_CLASS;

public class StringSplit {
    private static final MethodDescriptor SPLIT_METHOD = MethodDescriptor.ofMethod(String.class, "split", String[].class, String.class);
    private static final MethodDescriptor LENGTH_METHOD = MethodDescriptor.ofMethod(String.class, "length", int.class);

    private static final MethodDescriptor ASLIST_METHOD = MethodDescriptor.ofMethod(Arrays.class, "asList", List.class, Object[].class);
    private static final MethodDescriptor CHAR_AT_METHOD = MethodDescriptor.ofMethod(String.class, "charAt", char.class, int.class);

    private final Class<?> variableClass;
    private final Variable variable;
    private Expression source;
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

    public ResultHandle toCode(BytecodeCreator method, ClassCreator creator) {


        ResultHandle toSplit = source.getResultHandle(method, creator);
        ResultHandle answer;
        if (delimiter != null) {
            ResultHandle delimiterHandle = delimiter.getResultHandle(method, creator);
            ResultHandle splitArray = method.invokeVirtualMethod(SPLIT_METHOD, toSplit, delimiterHandle);
            answer = method.invokeStaticMethod(ASLIST_METHOD, splitArray);
        } else {
            // Doing the lambda in bytecode is hard, so just do a while loop
            // and working with the int stream is also hard ...
            // ... and working with the primitive array is also hard ...
            ResultHandle length = method.invokeVirtualMethod(LENGTH_METHOD, toSplit);

            answer = Gizmo.newArrayList(method);

            AssignableResultHandle index = method.createVariable(int.class);
            method.assign(index, method.load(0));
            WhileLoop loop = method.whileLoop(bc -> bc.ifIntegerLessThan(index, length));
            BytecodeCreator block = loop.block();
            ResultHandle charAsString = block.invokeVirtualMethod(CHAR_AT_METHOD, toSplit, index);

            block.invokeInterfaceMethod(ADD_METHOD, answer, charAsString);
            block.assign(index, block.increment(index));
        }

        if (!variable.isAlreadyDefined()) {
            // TODO refactor so getting the field is private
            variable.getField(creator, method);
        }

        variable.write(method, answer);

        // Return the result handle for ease of testing
        return answer;

    }
}


