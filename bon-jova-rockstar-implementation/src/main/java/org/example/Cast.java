package org.example;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import rock.Rockstar;

public class Cast {
    private static final MethodDescriptor VALUE_OF_METHOD = MethodDescriptor.ofMethod(Double.class, "valueOf", Double.class, String.class);
    private static final MethodDescriptor DOUBLE_FROM_INTEGER_METHOD = MethodDescriptor.ofMethod(Integer.class, "doubleValue", double.class);
    private static final MethodDescriptor RADIX_VALUE_OF_METHOD = MethodDescriptor.ofMethod(Integer.class, "parseInt", int.class, String.class, int.class);
    private static final MethodDescriptor TO_CHARS_METHOD = MethodDescriptor.ofMethod(Character.class, "toChars", char[].class, int.class);
    private static final MethodDescriptor INTVALUE_METHOD = MethodDescriptor.ofMethod(Double.class, "intValue", int.class);
    private static final MethodDescriptor STRING_FROM_CHARS_METHOD = MethodDescriptor.ofConstructor(String.class, char[].class);

    private final Rockstar.CastStmtContext ctx;

    public Cast(Rockstar.CastStmtContext ctx) {
        // For now, just save the ctx rather than unpacking it here
        this.ctx = ctx;
    }

    public ResultHandle toCode(BytecodeCreator method, ClassCreator creator) {

        Rockstar.ExpressionContext sourceExpression = ctx.expression().get(0);
        ResultHandle oldVal = new Expression(sourceExpression).getResultHandle(method, creator);

        // Handle casting things that aren't strings
        ResultHandle isString = method.instanceOf(oldVal, String.class);
        BranchResult br = method.ifTrue(isString);
        AssignableResultHandle newVal = method.createVariable(Object.class);

        BytecodeCreator isStringBranch = br.trueBranch();
        if (ctx.KW_WITH() != null) {
            //Satriani gives a NaN if the 'with' isn't 16, so this is doing better than it (although we ignore floating points because priorities)
            ResultHandle intRadix = isStringBranch.invokeVirtualMethod(INTVALUE_METHOD, new Expression(ctx.expression(1)).getResultHandle(method, creator));
            ResultHandle parsedInteger = isStringBranch.invokeStaticMethod(RADIX_VALUE_OF_METHOD, oldVal, intRadix);
            ResultHandle resultHandle = isStringBranch.invokeVirtualMethod(DOUBLE_FROM_INTEGER_METHOD, parsedInteger);
            isStringBranch.assign(newVal, resultHandle);
        } else {
            isStringBranch.assign(newVal, isStringBranch.invokeStaticMethod(VALUE_OF_METHOD, oldVal));
        }

        BytecodeCreator isNumberBranch = br.falseBranch();
        isNumberBranch.assign(newVal, oldVal);
        ResultHandle oldInt = isNumberBranch.invokeVirtualMethod(INTVALUE_METHOD, oldVal);
        ResultHandle newChars = isNumberBranch.invokeStaticMethod(TO_CHARS_METHOD, oldInt);
        isNumberBranch.assign(newVal, isNumberBranch.newInstance(STRING_FROM_CHARS_METHOD, newChars));

        // TODO nice error for cases where it isn't a number or string

        Variable newVar;
        if (ctx.KW_INTO() != null) {
            newVar = new Variable(ctx.variable(), double.class);
        } else {
            // If we don't have a variable to cast into, failure is appropriate
            Rockstar.VariableContext variableCtx = sourceExpression.variable();
            if (variableCtx == null) {
                method.throwException(IllegalArgumentException.class, "Nothing to cast into.");
                return null;
            } else {
                Variable oldVar = new Variable(variableCtx);
                newVar = oldVar;
            }
        }
        newVar.write(method, creator, newVal);

        return newVal;
    }
}


