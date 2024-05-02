package io.quarkiverse.bonjova.compiler;

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

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class BytecodeGeneratingListener extends RockstarBaseListener {

    private MethodCreator main;

    private final ClassCreator creator;
    private final FieldDescriptor formatter;
    private final Stack<Block> blocks = new Stack<>();
    // For some constructs, we may want to create a method but not switch to it until the next statement list; this stack is a convenient
    // place to store them
    private final Stack<BytecodeCreator> controlScopes = new Stack<>();
    private static final MethodDescriptor STRING_CONCAT = MethodDescriptor.ofMethod("java/lang/String", "concat", String.class,
            String.class);
    private Block currentBlock;
    // For things like loops, break and continue need to jump to the top of the loop, which may include several intermediary scopes
    private BytecodeCreator targetScopeForJumps;

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
        Input.clearState();

        main = creator.getMethodCreator("main", void.class, String[].class);
        main.setModifiers(ACC_PUBLIC + ACC_STATIC);

        enterBlock(new Block(ctx, main, creator, new VariableScope(), null), ctx);
    }

    @Override
    public void exitProgram(Rockstar.ProgramContext ctx) {
        while (!blocks.isEmpty()) {
            currentBlock.method().returnVoid();
            currentBlock = blocks.pop();
        }
    }

    // It would be nice to get rid of this, but when we get an expression, we don't always know what the type of thing in the result
    // handle is
    public static boolean isNumber(ResultHandle value) {
        // ResultHandle knows the type, but it's private
        // Doing an instanceof check on a primitive tends to blow up, and it clutters the output code, so cheat
        // Take advantage of the toString format of ResultHandle
        String string = value.toString();
        return string
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
        assignment.toCode(currentBlock);
    }

    @Override
    public void enterIncrementStmt(Rockstar.IncrementStmtContext ctx) {

        int count = ctx.ups()
                .KW_UP()
                .size();

        ResultHandle one = currentBlock.method().load(1d);

        for (int i = 0; i < count; i++) {

            Variable variable = new Variable(ctx.variable());
            ResultHandle value = variable.getResultHandle(currentBlock);
            value = Constant.coerceNothingIntoType(currentBlock, value, Expression.Context.SCALAR);

            // This intermediate variable is useful to give a bit of flexibility about types
            AssignableResultHandle incremented = currentBlock.method().createVariable(Object.class);

            // TODO on mysterious, this will pass when it should fail

            // See if we can assign the value to a Double - if we can, it is either a number, or null
            TryBlock tryBlock = currentBlock.method().tryBlock();
            {
                AssignableResultHandle casted = tryBlock.createVariable(Double.class);
                tryBlock.assign(casted, tryBlock.checkCast(value, Double.class));
                MethodDescriptor toDouble = MethodDescriptor.ofMethod("java/lang/Double", "doubleValue", double.class);
                ResultHandle primitive = tryBlock.invokeVirtualMethod(toDouble, casted);

                ResultHandle added = tryBlock.add(primitive, one);
                tryBlock.assign(incremented, added);
                variable.write(new Block(null, tryBlock, currentBlock.creator(), currentBlock.variables(), currentBlock),
                        incremented);
            }

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

            variable.write(currentBlock, incremented);
        }
    }

    @Override
    public void enterDecrementStmt(Rockstar.DecrementStmtContext ctx) {

        int count = ctx.downs()
                .KW_DOWN()
                .size();

        BytecodeCreator currentCreator = currentBlock.method();

        ResultHandle minusOne = currentCreator.load(-1d);

        for (int i = 0; i < count; i++) {

            Variable variable = new Variable(ctx.variable());
            ResultHandle value = variable.getResultHandle(currentBlock);

            // This intermediate variable is useful to give a bit of flexibility about types
            AssignableResultHandle incremented = currentCreator.createVariable(Object.class);
            value = Constant.coerceNothingIntoType(currentCreator, value, minusOne, Expression.Operation.ADD);

            // See if we can assign the value to a Double - if we can, it is either a number, or null
            TryBlock tryBlock = currentCreator.tryBlock();
            {
                AssignableResultHandle casted = tryBlock.createVariable(Double.class);
                tryBlock.assign(casted, tryBlock.checkCast(value, Double.class));
                MethodDescriptor toDouble = MethodDescriptor.ofMethod("java/lang/Double", "doubleValue", double.class);
                ResultHandle primitive = tryBlock.invokeVirtualMethod(toDouble, casted);

                ResultHandle added = tryBlock.add(primitive, minusOne);
                tryBlock.assign(incremented, added);
                variable.write(currentBlock, incremented);
            }

            // If not, do the string case; having bytecode that does string manipulations on variables the compiler knows are numbers upsets the verifier
            CatchBlockCreator stringCase = tryBlock.addCatch(ClassCastException.class);
            {
                // This must be a string
                // TODO unless it is a boolean ...
                // We can't decrement a string, and the types go a bit weird, so just use a string NaN
                stringCase.assign(incremented, stringCase.load("NaN"));
            }

            variable.write(currentBlock, incremented);
        }
    }

    @Override
    public void enterRoundingStmt(Rockstar.RoundingStmtContext ctx) {
        Rounding rounding = new Rounding(ctx);
        rounding.toCode(currentBlock);
    }

    @Override
    public void enterArrayStmt(Rockstar.ArrayStmtContext ctx) {
        Array a = new Array(ctx);
        a.toCode(currentBlock);
    }

    @Override
    public void enterOutputStmt(Rockstar.OutputStmtContext ctx) {
        Expression expression = new Expression(ctx.expression());
        ResultHandle value = expression.getResultHandle(currentBlock, Expression.Context.NOT_OBJECT);

        // TODO refactor this conditional formatting into a class-level method?
        // We want to do a special toString on numbers, to avoid tacking decimals onto integers
        TryBlock tryBlock = currentBlock.method().tryBlock();
        ResultHandle castToDouble = tryBlock.checkCast(value, Double.class);

        ResultHandle df = tryBlock.readStaticField(formatter);

        ResultHandle formatted = tryBlock
                .invokeVirtualMethod(
                        MethodDescriptor.ofMethod(DecimalFormat.class, "format", String.class, double.class),
                        df, castToDouble);
        Gizmo.systemOutPrintln(tryBlock, formatted);

        CatchBlockCreator catchBlock = tryBlock.addCatch(ClassCastException.class);
        ResultHandle toStringed = Gizmo.toString(catchBlock, value);
        Gizmo.systemOutPrintln(catchBlock, toStringed);

        // When printed on its own, null is "mysterious"
        CatchBlockCreator nullCatchBlock = tryBlock.addCatch(NullPointerException.class);
        Gizmo.systemOutPrintln(nullCatchBlock, nullCatchBlock.load("mysterious"));
    }

    @Override
    public void enterInputStmt(Rockstar.InputStmtContext ctx) {
        Input input = new Input(ctx);
        input.toCode(currentBlock, main);

    }

    @Override
    public void enterBreakStmt(Rockstar.BreakStmtContext ctx) {
        if (targetScopeForJumps != null) {
            currentBlock.method().breakScope(targetScopeForJumps);
        } else {
            currentBlock.method().breakScope();
        }
    }

    @Override
    public void enterContinueStmt(Rockstar.ContinueStmtContext ctx) {
        if (targetScopeForJumps != null) {
            currentBlock.method().continueScope(targetScopeForJumps);
        } else {
            // This is rather dodgy, but if we don't have an enclosing loop-y method, then a continue behaves like a break
            currentBlock.method().breakScope();
        }
    }

    @Override
    public void enterLoopStmt(Rockstar.LoopStmtContext ctx) {

        Expression expression = new Expression(ctx.expression());

        final Function<BytecodeCreator, BranchResult> fun;

        targetScopeForJumps = currentBlock.method().createScope();
        // TODO new variable scope?
        enterBlock(new Block(ctx, targetScopeForJumps, creator, currentBlock.variables(), currentBlock), ctx);

        if (ctx.KW_WHILE() != null) {
            fun = (BytecodeCreator method) -> {
                // TODO here is where we would make a new scope?
                ResultHandle evaluated = expression.getResultHandle(
                        new Block(ctx, method, creator, currentBlock.variables(), currentBlock),
                        Expression.Context.BOOLEAN);
                // TODO delete this comment Convoluted code! We can't return a boolean from the expression, or we sometimes get java.lang.NoClassDefFoundError: boolean
                // But the Boolean in ifTrue gets cast to Int, so we need to turn our Boolean into a boolean
                //  return method.ifTrue(method.invokeVirtualMethod(BOOLEAN_VALUE, evaluated));
                return method.ifTrue(evaluated);
            };
        } else if (ctx.KW_UNTIL() != null) {
            fun = (BytecodeCreator method) -> {
                // See above - same convolution
                ResultHandle evaluated = expression.getResultHandle(
                        new Block(ctx, method, creator, currentBlock.variables(), currentBlock),
                        Expression.Context.BOOLEAN);
                return method.ifFalse(evaluated);
            };
        } else {
            throw new RuntimeException("Could not understand loop " + ctx.getText());
        }
        BytecodeCreator loop = currentBlock.method().whileLoop(fun)
                .block();
        enterBlock(new Block(ctx, loop, creator, currentBlock.variables(), currentBlock), ctx);
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
        BranchResult code = condition.toCode(currentBlock);
        if (condition.hasElse()) {
            controlScopes.push(code.falseBranch());
        }
        controlScopes.push(code.trueBranch());
        enterBlock(currentBlock, ctx);
    }

    @Override
    public void exitIfStmt(Rockstar.IfStmtContext ctx) {
        exitBlock();
    }

    @Override
    public void enterStringStmt(Rockstar.StringStmtContext ctx) {
        new StringSplit(ctx).toCode(currentBlock);
    }

    @Override
    public void enterFunctionDeclaration(Rockstar.FunctionDeclarationContext ctx) {
        // A function creator in Gizmo is like a lambda, which is not really what we want, so use methods
        List<Rockstar.VariableContext> variableContexts = ctx.paramList()
                .variable();
        final MethodCreator fun;

        //  If a variable is defined inside of a function, it is in local method. Local method variables are available from their initialization until the end of the function they are defined in.
        //
        //While within a function, if you write to a variable that has been defined in global method, you write to that variable; you do not define a new local variable.

        // The spec says all functions must take at least one argument
        // In this case passing a class array to the creator confuses it and doesn't get counted as the varargs
        String functionName = Variable.getNormalisedVariableName(ctx.functionName.getText());
        if (variableContexts.size() == 1) {
            fun = creator.getMethodCreator(functionName, Object.class, Object.class);
        } else {
            Class<?>[] paramClasses = new Class[variableContexts.size()];
            Arrays.fill(paramClasses, Object.class);
            fun = creator.getMethodCreator(functionName, Object.class, paramClasses);
        }
        fun.setModifiers(ACC_PUBLIC + ACC_STATIC);
        // New variable scope, since this is a function
        Block block = new Block(ctx, fun, creator, new VariableScope(), currentBlock);
        enterBlock(block, ctx);

        List<Parameter> parameters = variableContexts.stream()
                .map(vctx -> new Parameter(vctx, Object.class))
                .toList();

        fun.setParameterNames(parameters.stream()
                .map(Variable::getVariableName)
                .toList()
                .toArray(new String[] {}));
        int i = 0;
        for (Parameter p : parameters) {
            p.write(block, fun.getMethodParam(i));
            i++;
        }
    }

    @Override
    public void exitFunctionDeclaration(Rockstar.FunctionDeclarationContext ctx) {
        // Do nothing, as the return statement handles exiting the block (the return is not part of the function declaration)
    }

    @Override
    public void enterCastStmt(Rockstar.CastStmtContext ctx) {
        Cast cast = new Cast(ctx);
        cast.toCode(currentBlock);
    }

    @Override
    public void enterJoinStmt(Rockstar.JoinStmtContext ctx) {
        Array.join(ctx, currentBlock);
    }

    @Override
    public void exitReturnStmt(Rockstar.ReturnStmtContext ctx) {
        Expression e = new Expression(ctx.expression());
        ResultHandle rh = e.getResultHandle(currentBlock);

        currentBlock.method().returnValue(rh);

        // We could have repeated returns, so check we have a block before exiting
        if (isSafeToExitBlock()) {
            exitBlock();
        }

    }

    @Override
    public void enterStatementList(Rockstar.StatementListContext ctx) {
        BytecodeCreator scope;
        if (controlScopes.isEmpty()) {
            scope = currentBlock.method().createScope();
        } else {
            scope = controlScopes.pop();
        }
        // TODO would we make a new variable scope here?

        enterBlock(new Block(ctx, scope, creator, currentBlock.variables(), currentBlock), ctx);
    }

    @Override
    public void exitStatementList(Rockstar.StatementListContext ctx) {
        if (isSafeToExitBlock()) {
            exitBlock();
        }
    }

    private boolean isSafeToExitBlock() {
        return blocks.size() > 1;
    }

    private void exitBlock() {
        blocks.pop();
        currentBlock = blocks.peek();
    }

    private void enterBlock(Block block, ParserRuleContext ctx) {
        currentBlock = block;
        blocks.push(block);
    }

}