package org.example;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.WhileLoop;
import rock.Rockstar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.example.BytecodeGeneratingListener.isNumber;
import static org.example.Constant.coerceNothingIntoType;
import static org.example.Expression.coerceAwayNothing;

public class Array {
    private static final MethodDescriptor LIST_CONSTRUCTOR = MethodDescriptor.ofConstructor(ArrayList.class);
    private static final MethodDescriptor MAP_CONSTRUCTOR = MethodDescriptor.ofConstructor(HashMap.class);
    static final MethodDescriptor ADD_METHOD = MethodDescriptor.ofMethod(List.class, "add", boolean.class, Object.class);
    static final MethodDescriptor ADD_AT_INDEX_METHOD = MethodDescriptor.ofMethod(List.class, "add", void.class, int.class, Object.class);
    static final MethodDescriptor PUT_METHOD = MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class, Object.class);
    private static final MethodDescriptor GET_METHOD = MethodDescriptor.ofMethod(List.class, "get", Object.class, int.class);
    private static final MethodDescriptor MAP_GET_METHOD = MethodDescriptor.ofMethod(Map.class, "get", Object.class, Object.class);
    private static final MethodDescriptor REMOVE_METHOD = MethodDescriptor.ofMethod(List.class, "remove", Object.class, int.class);
    private static final MethodDescriptor LENGTH_METHOD = MethodDescriptor.ofMethod(List.class, "size", int.class);
    private static final MethodDescriptor DOUBLE_METHOD = MethodDescriptor.ofMethod(Integer.class, "doubleValue", double.class);
    private static final MethodDescriptor INT_METHOD = MethodDescriptor.ofMethod(Double.class, "intValue", int.class);

    private final Class<?> variableClass;
    private final Variable variable;
    private final Variable stringKeyVariable;
    public static final Class<?> TYPE_CLASS = List.class;
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
        stringKeyVariable = new Variable(variable.getVariableName() + "nonNumeric", Map.class);
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
        ResultHandle index;
        if (arrayAccessIndex.isNothing()) {
            index = coerceNothingIntoType(method, method.load(0d));
        } else {
            index = arrayAccessIndex.getResultHandle(method, creator);
            // This could still be a null, so do another check
            index = coerceAwayNothing(method, index, method.load(0d));
        }
        // Short circuit this logic if we know we are dealing with a number
        if (isNumber(index)) {
            ResultHandle rh = variable.read(method);
            ResultHandle intIndex = method.invokeVirtualMethod(INT_METHOD, index);
            return method.invokeInterfaceMethod(GET_METHOD, rh, intIndex);
        } else {
            AssignableResultHandle answer = method.createVariable(Object.class);

            // Now check the type
            BranchResult br = method.ifTrue(method.instanceOf(index, Double.class));
            BytecodeCreator trueBranch = br.trueBranch();
            ResultHandle rh = variable.read(trueBranch);
            ResultHandle intIndex = trueBranch.invokeVirtualMethod(INT_METHOD, index);
            trueBranch.assign(answer, trueBranch.invokeInterfaceMethod(GET_METHOD, rh, intIndex));

            BytecodeCreator falseBranch = br.falseBranch();
            ResultHandle skrh = stringKeyVariable.read(falseBranch);
            falseBranch.assign(answer, falseBranch.invokeInterfaceMethod(MAP_GET_METHOD, skrh, index));

            return answer;
        }
    }

    // This (badly-named) method covers initialisation and writing
    public ResultHandle toCode(BytecodeCreator method, ClassCreator creator) {

        ResultHandle rh, skrh;

        if (variable.isAlreadyWritten()) {
            rh = variable.read(method);
        } else {
            // TODO it would be nice to specify the initial capacity, even if creating a collection to initialise it with is too tricky
            rh = method.newInstance(LIST_CONSTRUCTOR);
            variable.write(method, creator, rh);
        }
        if (stringKeyVariable.isAlreadyWritten()) {
            skrh = stringKeyVariable.read(method);
        } else {
            skrh = method.newInstance(MAP_CONSTRUCTOR);
            stringKeyVariable.write(method, creator, skrh);
        }


        if (initialContents != null) {
            for (Expression c : initialContents) {
                method.invokeInterfaceMethod(ADD_METHOD, rh, c.getResultHandle(method, creator));
            }
        }

        if (index != null) {
            ResultHandle placedRh = placedValue.getResultHandle(method, creator);

            // Short circuit this logic if we know we are dealing with a number
            ResultHandle indexRh = index.getResultHandle(method, creator);
            if (isNumber(indexRh)) {
                writeToNumericIndex(rh, placedRh, indexRh, method);
            } else {


                // Is this a numeric or string index?
                BranchResult br = method.ifTrue(method.instanceOf(indexRh, Double.class));
                BytecodeCreator trueBranch = br.trueBranch();
                // Ensure capacity
                writeToNumericIndex(rh, placedRh, indexRh, trueBranch);

                BytecodeCreator falseBranch = br.falseBranch();
                falseBranch.invokeInterfaceMethod(PUT_METHOD, skrh, indexRh, placedRh);
                // Return the map in this case
                rh = skrh;
            }
        }

        // Return the result handle for ease of testing
        return rh;

    }

    private static void writeToNumericIndex(ResultHandle rh, ResultHandle placedRh, ResultHandle indexRh, BytecodeCreator trueBranch) {
        ResultHandle addIndex = trueBranch.invokeVirtualMethod(INT_METHOD, indexRh);
        ResultHandle length = trueBranch.invokeInterfaceMethod(LENGTH_METHOD, rh);
        AssignableResultHandle currentPosition = trueBranch.createVariable(int.class);
        trueBranch.assign(currentPosition, length);
        WhileLoop loop = trueBranch.whileLoop(bc -> bc.ifIntegerLessThan(currentPosition, addIndex));
        BytecodeCreator block = loop.block();
        block.invokeInterfaceMethod(ADD_METHOD, rh, block.loadNull());
        block.assign(currentPosition, block.increment(currentPosition));

        trueBranch.invokeInterfaceMethod(ADD_AT_INDEX_METHOD, rh, addIndex, placedRh);
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
