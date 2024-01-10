package org.example;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;
import rock.RockstarBaseListener;

import java.text.DecimalFormat;
import java.util.Stack;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class BytecodeGeneratingListener extends RockstarBaseListener {

    private BytecodeCreator currentCreator;
    private final ClassCreator creator;

    private final FieldDescriptor formatter;

    private final Stack<BytecodeCreator> blocks = new Stack<>();


    public BytecodeGeneratingListener(ClassCreator creator) {
        super();

        MethodCreator main = creator.getMethodCreator("main", void.class, String[].class);
        main.setModifiers(ACC_PUBLIC + ACC_STATIC);

        enterBlock(main);

        // Ideally this would be static final, but I got a bit stuck on the <clinit>
        ResultHandle formatterInstance = currentCreator.newInstance(MethodDescriptor.ofConstructor(DecimalFormat.class, String.class),
                currentCreator.load("#.#########"));
        formatter = creator.getFieldCreator("formatter", DecimalFormat.class)
                           .setModifiers(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE)
                           .getFieldDescriptor();
        currentCreator.writeStaticField(formatter, formatterInstance);


        this.creator = creator;

    }

    // Ensure we don't get cross-talk between programs for the statics
    @Override
    public void enterProgram(Rockstar.ProgramContext ctx) {
        Variable.clearPronouns();
    }

    @Override
    public void exitProgram(Rockstar.ProgramContext ctx) {
        while (!blocks.isEmpty()) {
            currentCreator.returnVoid();
            currentCreator = blocks.pop();
        }
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

        for (int i = 0; i < count; i++) {

            Variable variable = new Variable(ctx.variable());
            ResultHandle value = variable.read(currentCreator);
            ResultHandle incremented;
            try {
                // This is ugly, but the result handle does not expose the method to get the type publicly, so we need trial and error
                // (or to track it ourselves)
                incremented = currentCreator.add(value, currentCreator.load((double) 1));
            } catch (RuntimeException ee) {
                // This must be a string
                ResultHandle constant = currentCreator.load("1");
                incremented = currentCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod("java/lang/String", "concat", "Ljava/lang/String;", "Ljava/lang/String;"), value,
                        constant);
            }

            variable.write(currentCreator, incremented);
        }
    }

    @Override
    public void enterDecrementStmt(Rockstar.DecrementStmtContext ctx) {

        int count = ctx.downs()
                       .KW_DOWN()
                       .size();

        for (int i = 0; i < count; i++) {
            Variable variable = new Variable(ctx.variable());
            ResultHandle value = variable.read(currentCreator);
            ResultHandle incremented = currentCreator.add(value, currentCreator.load((double) -1));
            variable.write(currentCreator, incremented);
        }
    }

    @Override
    public void enterIfStmt(Rockstar.IfStmtContext ctx) {

        Condition condition = new Condition(ctx);
        BranchResult code = condition.toCode(currentCreator);
        if (condition.hasElse()) {
            enterBlock(code.falseBranch());
        }
        enterBlock(code.trueBranch());
    }

    @Override
    public void exitStatementList(Rockstar.StatementListContext ctx) {
        if (blocks.size() > 1) {
            exitBlock();
        }
    }

    @Override
    public void enterOutputStmt(Rockstar.OutputStmtContext ctx) {
        Expression expression = new Expression(ctx.expression());
        ResultHandle value = expression.getResultHandle(currentCreator);

        // We want to do a special toString on numbers, to avoid tacking decimals onto integers
        final ResultHandle toStringed;
        if (isNumber(value)) {
            ResultHandle df = currentCreator.readStaticField(formatter);

            toStringed = currentCreator
                    .invokeVirtualMethod(
                            MethodDescriptor.ofMethod(DecimalFormat.class, "format", String.class, double.class),
                            df, value);

        } else {
            toStringed = Gizmo.toString(currentCreator, value);
        }
        Gizmo.systemOutPrintln(currentCreator, toStringed);
    }

    @Override
    public void enterLoopStmt(Rockstar.LoopStmtContext ctx) {

        Expression expression = new Expression(ctx.expression());

        final Function<BytecodeCreator, BranchResult> fun;

        if (ctx.KW_WHILE() != null) {
            fun = (BytecodeCreator method) -> {
                ResultHandle evaluated = expression.getResultHandle(method);
                return method.ifTrue(evaluated);
            };
        } else if (ctx.KW_UNTIL() != null) {
            fun = (BytecodeCreator method) -> {
                ResultHandle evaluated = expression.getResultHandle(method);
                return method.ifFalse(evaluated);
            };
        } else {
            throw new RuntimeException("Could not understand loop " + ctx.getText());
        }
        BytecodeCreator loop = currentCreator.whileLoop(fun)
                                             .block();
        enterBlock(loop);
    }

    private void exitBlock() {
        blocks.pop();
        currentCreator = blocks.peek();
    }

    private void enterBlock(BytecodeCreator blockCreator) {
        currentCreator = blockCreator;
        blocks.push(blockCreator);
    }

    // It would be nice to get rid of this, but when we get an expression, we don't always know what the type of thing in the result
    // handle is
    private static boolean isNumber(ResultHandle value) {
        // ResultHandle knows the type, but it's private
        // Doing an instanceof check on a primitive tends to blow up, and it clutters the output code, so cheat
        // Take advantage of the toString format of ResultHandle
        return value.toString()
                    .contains("type='D'");
    }
}