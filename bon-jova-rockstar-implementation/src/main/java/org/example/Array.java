package org.example;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.WhileLoop;
import rock.Rockstar;

import java.util.ArrayList;
import java.util.List;

import static org.example.Constant.coerceNothingIntoType;
import static org.example.Expression.coerceAwayNothing;

public class Array {
    private static final MethodDescriptor LIST_CONSTRUCTOR = MethodDescriptor.ofConstructor(ArrayList.class);
    static final MethodDescriptor ADD_METHOD = MethodDescriptor.ofMethod(List.class, "add", boolean.class, Object.class);
    static final MethodDescriptor ADD_AT_INDEX_METHOD = MethodDescriptor.ofMethod(List.class, "add", void.class, int.class, Object.class);
    private static final MethodDescriptor GET_METHOD = MethodDescriptor.ofMethod(List.class, "get", Object.class, int.class);
    private static final MethodDescriptor REMOVE_METHOD = MethodDescriptor.ofMethod(List.class, "remove", Object.class, int.class);
    private static final MethodDescriptor LENGTH_METHOD = MethodDescriptor.ofMethod(List.class, "size", int.class);
    private static final MethodDescriptor DOUBLE_METHOD = MethodDescriptor.ofMethod(Integer.class, "doubleValue", double.class);
    private static final MethodDescriptor INT_METHOD = MethodDescriptor.ofMethod(Double.class, "intValue", int.class);

    private final Class<?> variableClass;
    private final Variable variable;
    public static final Class<?> TYPE_CLASS = List.class;
    private List<Expression> initialContents;
    private Expression index;
    private Expression placedValue;

    public Array(Rockstar.ArrayStmtContext ctx) {
        // TODO two variables, or an intermediate class to handle the map/list duality?
        // Or just always use a map, and track our index?
        variableClass = TYPE_CLASS;
        variable = new Variable(ctx.variable(), variableClass);
        // Variables should 'apply' to future pronouns when used in assignments
        variable.track();

        if (ctx.list() != null) {
            initialContents = ctx.list().expression().stream().map(Expression::new).toList();
        }

        if (ctx.KW_LET() != null && ctx.KW_AT() != null) {
            index = new Expression(ctx.expression(0));
            placedValue = new Expression(ctx.expression(1));
        }
    }

    public Array(Variable variable) {
        this.variable = variable;
        variableClass = variable.getVariableClass();
    }

    public String getVariableName() {
        return variable.getVariableName();
    }

    public static ResultHandle toScalarContext(Variable variable, BytecodeCreator method) {
        ResultHandle arr = variable.read(method);
        ResultHandle intLength = method.invokeInterfaceMethod(LENGTH_METHOD, arr);
        // We work in doubles everywhere, so convert to a double; use doubleValue not a cast, since the dynamic invocation actually gave us an Integer
        return method.invokeVirtualMethod(DOUBLE_METHOD, intLength);
    }

    public ResultHandle read(Expression arrayAccessIndex, BytecodeCreator method, ClassCreator creator) {
        ResultHandle rh = variable.read(method);
        ResultHandle index;
        if (arrayAccessIndex.isNothing()) {
            index = coerceNothingIntoType(method, method.load(0d));
        } else {
            index = arrayAccessIndex.getResultHandle(method, creator);
            // This could still be a null, so do another check
            index = coerceAwayNothing(method, index, method.load(0d));

        }
        index = method.invokeVirtualMethod(INT_METHOD, index);
        return method.invokeInterfaceMethod(GET_METHOD, rh, index);
    }

    public ResultHandle toCode(BytecodeCreator method, ClassCreator creator) {

        // TODO it would be nice to specify the initial capacity, even if creating a collection to initialise it with is too tricky
        ResultHandle rh;

        if (variable.isAlreadyDefined()) {
            rh = variable.read(method);
        } else {
            rh = method.newInstance(LIST_CONSTRUCTOR);
            // TODO refactor so getting the field is private
            FieldDescriptor field = variable.getField(creator, method);
            method.writeStaticField(field, rh);
        }

        if (initialContents != null) {
            for (Expression c : initialContents) {
                method.invokeInterfaceMethod(ADD_METHOD, rh, c.getResultHandle(method, creator));
            }
        }

        if (index != null) {
            // Ensure capacity
            ResultHandle addIndex = method.invokeVirtualMethod(INT_METHOD, index.getResultHandle(method, creator));
            ResultHandle length = method.invokeInterfaceMethod(LENGTH_METHOD, rh);
            AssignableResultHandle currentPosition = method.createVariable(int.class);
            method.assign(currentPosition, length);
            WhileLoop loop = method.whileLoop(bc -> bc.ifIntegerLessThan(currentPosition, addIndex));
            BytecodeCreator block = loop.block();
            block.invokeInterfaceMethod(ADD_METHOD, rh, block.loadNull());
            block.assign(currentPosition, block.increment(currentPosition));

            method.invokeInterfaceMethod(ADD_AT_INDEX_METHOD, rh, addIndex, placedValue.getResultHandle(method, creator));
        }

        // Return the result handle for ease of testing
        return rh;

    }

    public ResultHandle pop(BytecodeCreator method, ClassCreator creator) {
        ResultHandle arr = variable.read(method);
        ResultHandle index = method.load(0);
        AssignableResultHandle answer = method.createVariable(Object.class);
        ResultHandle intLength = method.invokeInterfaceMethod(LENGTH_METHOD, arr);
        BranchResult br = method.ifGreaterThanZero(intLength);
        // Deleting the element returns it, so it acts as a pop
        BytecodeCreator trueBranch = br.trueBranch();
        trueBranch.assign(answer, trueBranch.invokeInterfaceMethod(REMOVE_METHOD, arr, index));
        BytecodeCreator falseBranch = br.falseBranch();
        falseBranch.assign(answer, falseBranch.loadNull()); // This should be mysterious
        return answer;
    }
}
