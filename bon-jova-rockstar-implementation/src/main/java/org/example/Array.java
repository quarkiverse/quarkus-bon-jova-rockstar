package org.example;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import rock.Rockstar;

import java.util.List;

import static org.example.BytecodeGeneratingListener.isNumber;
import static org.example.Constant.coerceNothingIntoType;
import static org.example.Expression.coerceAwayNothing;

public class Array {
    public static final Class<?> TYPE_CLASS = RockstarArray.class;
    static final MethodDescriptor CONSTRUCTOR = MethodDescriptor.ofConstructor(TYPE_CLASS);
    static final MethodDescriptor ADD_METHOD = MethodDescriptor.ofMethod(TYPE_CLASS, "add", void.class, Object.class);
    static final MethodDescriptor ADD_AT_NUMERIC_INDEX_METHOD = MethodDescriptor.ofMethod(TYPE_CLASS, "add", void.class, double.class, Object.class);
    static final MethodDescriptor ADD_AT_INDEX_METHOD = MethodDescriptor.ofMethod(TYPE_CLASS, "add", void.class, Object.class, Object.class);
    private static final MethodDescriptor GET_METHOD = MethodDescriptor.ofMethod(TYPE_CLASS, "get", Object.class, double.class);
    private static final MethodDescriptor MAP_GET_METHOD = MethodDescriptor.ofMethod(TYPE_CLASS, "get", Object.class, Object.class);
    private static final MethodDescriptor REMOVE_METHOD = MethodDescriptor.ofMethod(TYPE_CLASS, "pop", Object.class);
    private static final MethodDescriptor LENGTH_METHOD = MethodDescriptor.ofMethod(TYPE_CLASS, "size", double.class);

    private final Class<?> variableClass;
    private final Variable variable;
    private List<Expression> initialContents;
    private Expression index;
    private Expression placedValue;

    public Array(Rockstar.ArrayStmtContext ctx) {
        this(new Variable(ctx.variable(), TYPE_CLASS));
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
        return method.invokeVirtualMethod(LENGTH_METHOD, arr);
    }

    public ResultHandle read(Expression arrayAccessIndex, BytecodeCreator method, ClassCreator creator) {
        ResultHandle index;
        if (arrayAccessIndex.isNothing()) {
            index = coerceNothingIntoType(method, method.load(0d));
        } else {
            index = arrayAccessIndex.getResultHandle(method, creator);
            // This could still be a null, so do another check
            index = coerceAwayNothing(method, index, method.load(0d));
        }
        // Short circuit this logic if we know we are dealing with a number
        ResultHandle rh = variable.read(method);
        if (isNumber(index)) {
            return method.invokeVirtualMethod(GET_METHOD, rh, index);
        } else {
            return method.invokeVirtualMethod(MAP_GET_METHOD, rh, index);
        }
    }

    // This (badly-named) method covers initialisation and writing
    public ResultHandle toCode(BytecodeCreator method, ClassCreator creator) {

        ResultHandle rh;

        if (variable.isAlreadyWritten()) {
            rh = variable.read(method);
        } else {
            // TODO it would be nice to specify the initial capacity, even if creating a collection to initialise it with is too tricky
            rh = method.newInstance(CONSTRUCTOR);
            variable.write(method, creator, rh);
        }


        if (initialContents != null) {
            for (Expression c : initialContents) {
                method.invokeVirtualMethod(ADD_METHOD, rh, c.getResultHandle(method, creator));
            }
        }

        if (index != null) {
            ResultHandle placedRh = placedValue.getResultHandle(method, creator);

            // Short circuit this logic if we know we are dealing with a number
            ResultHandle indexRh = index.getResultHandle(method, creator);
            if (isNumber(indexRh)) {
                method.invokeVirtualMethod(ADD_AT_NUMERIC_INDEX_METHOD, rh, indexRh, placedRh);
            } else {
                method.invokeVirtualMethod(ADD_AT_INDEX_METHOD, rh, indexRh, placedRh);
            }
        }

        // Return the result handle for ease of testing
        return rh;
    }

    public ResultHandle pop(BytecodeCreator method, ClassCreator creator) {
        ResultHandle arr = variable.read(method);
        return method.invokeVirtualMethod(REMOVE_METHOD, arr);
    }
}
