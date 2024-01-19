package org.example;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import org.antlr.v4.runtime.ParserRuleContext;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;
import rock.RockstarBaseListener;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class BytecodeGeneratingListener extends RockstarBaseListener {

    private MethodCreator main;

    private final ClassCreator creator;
    private final FieldDescriptor formatter;
    private final Stack<Block> blocks = new Stack<>();
    // For some constructs, we may want to create a scope but not switch to it until the next statement list; this stack is a convenient
    // place to store them
    private final Stack<BytecodeCreator> controlScopes = new Stack<>();
    private static final MethodDescriptor STRING_CONCAT = MethodDescriptor.ofMethod("java/lang/String", "concat", "Ljava/lang/String;", "Ljava/lang/String;");
    private BytecodeCreator currentCreator;
    // For things like loops, break and continue need to jump to the top of the loop, which may include several intermediary scopes
    private BytecodeCreator targetScopeForJumps;


    // Not strictly necessary, but very useful for debugging
    record Block(ParserRuleContext ctx, BytecodeCreator scope) {
    }


    public BytecodeGeneratingListener(ClassCreator creator) {
        super();
        MethodCreator clinit = creator.getMethodCreator(MethodDescriptor.CLINIT, void.class);
        clinit.setModifiers(ACC_PUBLIC + ACC_STATIC + ACC_FINAL);
        ResultHandle formatterInstance = clinit.newInstance(MethodDescriptor.ofConstructor(DecimalFormat.class, String.class),
                clinit.load("#.#########"));
        formatter = creator.getFieldCreator("formatter", DecimalFormat.class)
                .setModifiers(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC + ACC_FINAL)
                .getFieldDescriptor();
        clinit.writeStaticField(formatter, formatterInstance);
        clinit.returnVoid();
        this.creator = creator;
    }

    // Ensure we don't get cross-talk between programs for the statics
    @Override
    public void enterProgram(Rockstar.ProgramContext ctx) {
        Variable.clearState();
        Input.setStdIn();

        main = creator.getMethodCreator("main", void.class, String[].class);
        main.setModifiers(ACC_PUBLIC + ACC_STATIC);

        enterBlock(main, ctx);
    }

    @Override
    public void exitProgram(Rockstar.ProgramContext ctx) {
        while (!blocks.isEmpty()) {
            currentCreator.returnVoid();
            currentCreator = blocks.pop().scope;
        }
    }

    // It would be nice to get rid of this, but when we get an expression, we don't always know what the type of thing in the result
    // handle is
    public static boolean isNumber(ResultHandle value) {
        // ResultHandle knows the type, but it's private
        // Doing an instanceof check on a primitive tends to blow up, and it clutters the output code, so cheat
        // Take advantage of the toString format of ResultHandle
        return value.toString()
                .contains("type='D'");
    }

    public static boolean isBoolean(ResultHandle value) {
        // ResultHandle knows the type, but it's private
        // Doing an instanceof check on a primitive tends to blow up, and it clutters the output code, so cheat
        // Take advantage of the toString format of ResultHandle
        return value.toString()
                .contains("type='Z'");
    }

    public static boolean isString(ResultHandle value) {
        // ResultHandle knows the type, but it's private
        // Doing an instanceof check on a primitive tends to blow up, and it clutters the output code, so cheat
        // Take advantage of the toString format of ResultHandle
        return value.toString()
                .contains("type='Ljava/lang/String;'");
    }

    public static boolean isObject(ResultHandle value) {
        // ResultHandle knows the type, but it's private
        // Doing an instanceof check on a primitive tends to blow up, and it clutters the output code, so cheat
        // Take advantage of the toString format of ResultHandle
        return value.toString()
                .contains("type='Ljava/lang/Object;'");
    }

    @Override
    public void enterAssignmentStmt(Rockstar.AssignmentStmtContext ctx) {

        Assignment assignment = new Assignment(ctx);
        assignment.toCode(creator, currentCreator);
    }

    @Override
    public void enterIncrementStmt(Rockstar.IncrementStmtContext ctx) {

        int count = ctx.ups()
                .KW_UP()
                .size();

        ResultHandle one = currentCreator.load(1d);

        for (int i = 0; i < count; i++) {

            Variable variable = new Variable(ctx.variable());
            ResultHandle value = variable.read(currentCreator);

            // This intermediate variable is useful to give a bit of flexibility about types
            AssignableResultHandle incremented = currentCreator.createVariable(Object.class);
            // TODO have this be a method on the Nothing object and store nulls as Nothings
            // TODO on mysterious, this will pass when it should fail

            // See if we can assign the value to a Double - if we can, it is either a number, or null
            TryBlock tryBlock = currentCreator.tryBlock();
            {
                AssignableResultHandle casted = tryBlock.createVariable(Double.class);
                tryBlock.assign(casted, tryBlock.checkCast(value, Double.class));
                MethodDescriptor toDouble = MethodDescriptor.ofMethod("java/lang/Double", "doubleValue", double.class);
                ResultHandle primitive = tryBlock.invokeVirtualMethod(toDouble, casted);

                ResultHandle added = tryBlock.add(primitive, one);
                tryBlock.assign(incremented, added);
                variable.write(tryBlock, incremented);
            }

            // Casting a null to a Double works, casting to a double gives an NPE
            CatchBlockCreator nullCase = tryBlock.addCatch(NullPointerException.class);
            {
                AssignableResultHandle coerced = currentCreator.createVariable(double.class);
                //  We could call   nullCase.assign(coerced, coerceNothingIntoType(nullCase, one)); but it is barely necessary, we know we're going to end up with 1.0
                nullCase.assign(coerced, nullCase.load(0d));
                ResultHandle addedCoerced = nullCase.add(coerced, one);
                nullCase.assign(incremented, addedCoerced);
            }

            if (!isNumber(value)) {
                // If not, do the string case; having bytecode that does string manipulations on variables the compiler knows are numbers upsets the verifier
                CatchBlockCreator stringCase = tryBlock.addCatch(ClassCastException.class);
                {
                    // This must be a string
                    // TODO unless it is a boolean ...
                    ResultHandle constant = stringCase.load("1");
                    AssignableResultHandle castToString = stringCase.createVariable(String.class);
                    stringCase.assign(castToString, stringCase.checkCast(value, String.class));
                    ResultHandle concat = stringCase.invokeVirtualMethod(
                            STRING_CONCAT, castToString,
                            constant);
                    stringCase.assign(incremented, concat);
                }
            }

            variable.write(currentCreator, incremented);
        }
    }

    @Override
    public void enterDecrementStmt(Rockstar.DecrementStmtContext ctx) {

        int count = ctx.downs()
                .KW_DOWN()
                .size();

        ResultHandle minusOne = currentCreator.load(-1d);

        for (int i = 0; i < count; i++) {

            Variable variable = new Variable(ctx.variable());
            ResultHandle value = variable.read(currentCreator);

            // This intermediate variable is useful to give a bit of flexibility about types
            AssignableResultHandle incremented = currentCreator.createVariable(Object.class);
            // TODO have this be a method on the Nothing object and store nulls as Nothings
            // TODO on mysterious, this will pass when it should fail

            // See if we can assign the value to a Double - if we can, it is either a number, or null
            TryBlock tryBlock = currentCreator.tryBlock();
            {
                AssignableResultHandle casted = tryBlock.createVariable(Double.class);
                tryBlock.assign(casted, tryBlock.checkCast(value, Double.class));
                MethodDescriptor toDouble = MethodDescriptor.ofMethod("java/lang/Double", "doubleValue", double.class);
                ResultHandle primitive = tryBlock.invokeVirtualMethod(toDouble, casted);

                ResultHandle added = tryBlock.add(primitive, minusOne);
                tryBlock.assign(incremented, added);
                variable.write(tryBlock, incremented);
            }

            // Casting a null to a Double works, casting to a double gives an NPE
            CatchBlockCreator nullCase = tryBlock.addCatch(NullPointerException.class);
            {
                AssignableResultHandle coerced = currentCreator.createVariable(double.class);
                //  We could call   nullCase.assign(coerced, coerceNothingIntoType(nullCase, one)); but it is barely necessary, we know we're going to end up with 1.0
                nullCase.assign(coerced, nullCase.load(0d));
                ResultHandle addedCoerced = nullCase.add(coerced, minusOne);
                nullCase.assign(incremented, addedCoerced);
            }

            if (!isNumber(value)) {
                // If not, do the string case; having bytecode that does string manipulations on variables the compiler knows are numbers upsets the verifier
                CatchBlockCreator stringCase = tryBlock.addCatch(ClassCastException.class);
                {
                    // This must be a string
                    // TODO unless it is a boolean ...
                    // We can't decrement a string, and the types go a bit weird, so just use a string NaN
                    stringCase.assign(incremented, stringCase.load("NaN"));
                }
            }

            variable.write(currentCreator, incremented);
        }
    }

    @Override
    public void enterOutputStmt(Rockstar.OutputStmtContext ctx) {
        Expression expression = new Expression(ctx.expression());
        ResultHandle value = expression.getResultHandle(currentCreator, creator);

        // TODO refactor this conditional formatting into a class-level method?
        // We want to do a special toString on numbers, to avoid tacking decimals onto integers
        TryBlock tryBlock = currentCreator.tryBlock();
        ResultHandle casttoDouble = tryBlock.checkCast(value, Double.class);

        ResultHandle df = tryBlock.readStaticField(formatter);

        ResultHandle formatted = tryBlock
                .invokeVirtualMethod(
                        MethodDescriptor.ofMethod(DecimalFormat.class, "format", String.class, double.class),
                        df, casttoDouble);
        Gizmo.systemOutPrintln(tryBlock, formatted);

        CatchBlockCreator catchBlock = tryBlock.addCatch(ClassCastException.class);
        ResultHandle toStringed = Gizmo.toString(catchBlock, value);
        Gizmo.systemOutPrintln(catchBlock, toStringed);

        // When printed on its own, null is ""
        CatchBlockCreator nullCatchBlock = tryBlock.addCatch(NullPointerException.class);
        Gizmo.systemOutPrintln(nullCatchBlock, nullCatchBlock.load(""));
    }

    @Override
    public void enterInputStmt(Rockstar.InputStmtContext ctx) {
        Input input = new Input(ctx);
        input.toCode(creator, currentCreator, main);

    }

    @Override
    public void enterBreakStmt(Rockstar.BreakStmtContext ctx) {
        if (targetScopeForJumps != null) {
            currentCreator.breakScope(targetScopeForJumps);
        } else {
            currentCreator.breakScope();
        }
    }

    @Override
    public void enterContinueStmt(Rockstar.ContinueStmtContext ctx) {
        if (targetScopeForJumps != null) {
            currentCreator.continueScope(targetScopeForJumps);
        } else {
            // This is rather dodgy, but if we don't have an enclosing loop-y scope, then a continue behaves like a break
            currentCreator.breakScope();
        }
    }

    @Override
    public void enterLoopStmt(Rockstar.LoopStmtContext ctx) {

        Expression expression = new Expression(ctx.expression());

        final Function<BytecodeCreator, BranchResult> fun;

        targetScopeForJumps = currentCreator.createScope();
        enterBlock(targetScopeForJumps, ctx);

        if (ctx.KW_WHILE() != null) {
            fun = (BytecodeCreator method) -> {
                ResultHandle evaluated = expression.getResultHandle(method, creator);
                return method.ifTrue(evaluated);
            };
        } else if (ctx.KW_UNTIL() != null) {
            fun = (BytecodeCreator method) -> {
                ResultHandle evaluated = expression.getResultHandle(method, creator);
                return method.ifFalse(evaluated);
            };
        } else {
            throw new RuntimeException("Could not understand loop " + ctx.getText());
        }
        BytecodeCreator loop = currentCreator.whileLoop(fun)
                .block();
        enterBlock(loop, ctx);
    }

    @Override
    public void exitLoopStmt(Rockstar.LoopStmtContext ctx) {
        // We make two blocks when we enter a while, so we need to exit two blocks on exit
        exitBlock();
        exitBlock();
        targetScopeForJumps = null;
    }

    @Override
    public void enterIfStmt(Rockstar.IfStmtContext ctx) {
        Condition condition = new Condition(ctx);
        BranchResult code = condition.toCode(currentCreator, creator);
        if (condition.hasElse()) {
            controlScopes.push(code.falseBranch());
        }
        controlScopes.push(code.trueBranch());
        enterBlock(currentCreator, ctx);
    }

    @Override
    public void exitIfStmt(Rockstar.IfStmtContext ctx) {
        exitBlock();
    }

    @Override
    public void enterFunctionDeclaration(Rockstar.FunctionDeclarationContext ctx) {
        // A function creator in Gizmo is like a lambda, which is not really what we want, so use methods
        // TODO the scope of these should be local, not global
        List<Rockstar.VariableContext> variableContexts = ctx.paramList()
                .variable();
        final MethodCreator fun;

        // The spec says all functions must take at least one argument
        // In this case passing a class array to the creator confuses it and doesn't get counted as the varargs
        if (variableContexts.size() == 1) {
            fun = creator.getMethodCreator(ctx.functionName.getText(), Object.class, Object.class);
        } else {
            Class<?>[] paramClasses = new Class[variableContexts.size()];
            Arrays.fill(paramClasses, Object.class);
            fun = creator.getMethodCreator(ctx.functionName.getText(), Object.class, paramClasses);
        }
        fun.setModifiers(ACC_PUBLIC + ACC_STATIC);
        enterBlock(fun, ctx);


        List<Variable> variables = variableContexts.stream()
                .map(vctx -> new Variable(vctx, Object.class))
                .collect(Collectors.toList());

        fun.setParameterNames(variables.stream()
                .map(Variable::getVariableName)
                .collect(Collectors.toList())
                .toArray(new String[]{}));
        int i = 0;
        for (Variable v : variables) {
            // TODO this is all wrong, should be a scoped thing, not a global var
            FieldDescriptor field = v.getField(creator, fun);
            v.write(fun, fun.getMethodParam(i));
            i++;
        }
    }

    @Override
    public void exitFunctionDeclaration(Rockstar.FunctionDeclarationContext ctx) {
        exitBlock();
    }

    @Override
    public void exitReturnStmt(Rockstar.ReturnStmtContext ctx) {
        Expression e = new Expression(ctx.expression());
        ResultHandle rh = e.getResultHandle(currentCreator, creator);

        currentCreator.returnValue(rh);
    }

    @Override
    public void enterStatementList(Rockstar.StatementListContext ctx) {
        BytecodeCreator scope;
        if (controlScopes.isEmpty()) {
            scope = currentCreator.createScope();
        } else {
            scope = controlScopes.pop();
        }
        enterBlock(scope, ctx);
    }

    @Override
    public void exitStatementList(Rockstar.StatementListContext ctx) {
        if (blocks.size() > 1) {
            exitBlock();
        }
    }

    private void exitBlock() {
        blocks.pop();
        currentCreator = blocks.peek().scope;
    }

    private void enterBlock(BytecodeCreator blockCreator, ParserRuleContext ctx) {
        currentCreator = blockCreator;
        blocks.push(new Block(ctx, blockCreator));
    }

}