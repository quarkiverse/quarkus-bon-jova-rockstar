package org.example;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import rock.Rockstar;

import java.util.ArrayList;
import java.util.List;

import static org.example.Constant.coerceNothingIntoType;
import static org.example.Expression.coerceAwayNothing;

public class Array {
    private static final MethodDescriptor LIST_CONSTRUCTOR = MethodDescriptor.ofConstructor(ArrayList.class);
    private static final MethodDescriptor ADD_METHOD = MethodDescriptor.ofMethod(ArrayList.class, "add", boolean.class, Object.class);
    private static final MethodDescriptor GET_METHOD = MethodDescriptor.ofMethod(ArrayList.class, "get", Object.class, int.class);
    private static final MethodDescriptor LENGTH_METHOD = MethodDescriptor.ofMethod(ArrayList.class, "size", int.class);
    private static final MethodDescriptor DOUBLE_METHOD = MethodDescriptor.ofMethod(Integer.class, "doubleValue", double.class);
    private static final MethodDescriptor INT_METHOD = MethodDescriptor.ofMethod(Double.class, "intValue", int.class);

    private final Class<?> variableClass;
    private final Variable variable;
    public static final Class<?> TYPE_CLASS = List.class;
    private List<Expression> initialContents;

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
        ResultHandle intLength = method.invokeVirtualMethod(LENGTH_METHOD, arr);
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
        return method.invokeVirtualMethod(GET_METHOD, rh, index);
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
                method.invokeVirtualMethod(ADD_METHOD, rh, c.getResultHandle(method, creator));
            }
        }

        // Return the result handle for ease of testing
        return rh;

    }
}
