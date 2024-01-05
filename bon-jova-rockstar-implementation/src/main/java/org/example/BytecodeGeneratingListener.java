package org.example;

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

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class BytecodeGeneratingListener extends RockstarBaseListener {

    private final MethodCreator main;
    private final ClassCreator creator;

    private final FieldDescriptor formatter;


    public BytecodeGeneratingListener(ClassCreator creator) {
        super();

        main = creator.getMethodCreator("main", void.class, String[].class);
        main.setModifiers(ACC_PUBLIC + ACC_STATIC);

        // Ideally this would be static final, but I got a bit stuck on the <clinit>
        ResultHandle formatterInstance = main.newInstance(MethodDescriptor.ofConstructor(DecimalFormat.class, String.class),
                main.load("#.#########"));
        formatter = creator.getFieldCreator("formatter", DecimalFormat.class)
                           .setModifiers(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE)
                           .getFieldDescriptor();
        main.writeStaticField(formatter, formatterInstance);


        this.creator = creator;

    }

    // Ensure we don't get cross-talk between programs for the statics
    @Override
    public void enterProgram(Rockstar.ProgramContext ctx) {
        Variable.clearPronouns();
    }

    @Override
    public void exitProgram(Rockstar.ProgramContext ctx) {
        main.returnVoid();
    }

    @Override
    public void enterAssignmentStmt(Rockstar.AssignmentStmtContext ctx) {

        Assignment assignment = new Assignment(ctx);
        assignment.toCode(creator, main);
    }

    @Override
    public void enterIncrementStmt(Rockstar.IncrementStmtContext ctx) {

        int count = ctx.ups()
                       .KW_UP()
                       .size();

        for (int i = 0; i < count; i++) {

            Variable variable = new Variable(ctx.variable());
            ResultHandle value = variable.read(main);
            ResultHandle incremented;
            try {
                // This is ugly, but the result handle does not expose the method to get the type publicly, so we need trial and error
                // (or to track it ourselves)
                incremented = main.add(value, main.load((double) 1));
            } catch (RuntimeException ee) {
                // This must be a string
                ResultHandle constant = main.load("1");
                incremented = main.invokeVirtualMethod(
                        MethodDescriptor.ofMethod("java/lang/String", "concat", "Ljava/lang/String;", "Ljava/lang/String;"), value,
                        constant);
            }

            variable.write(main, incremented);
        }
    }

    @Override
    public void enterDecrementStmt(Rockstar.DecrementStmtContext ctx) {

        int count = ctx.downs()
                       .KW_DOWN()
                       .size();

        for (int i = 0; i < count; i++) {
            Variable variable = new Variable(ctx.variable());
            ResultHandle value = variable.read(main);
            ResultHandle incremented = main.add(value, main.load((double) -1));
            variable.write(main, incremented);
        }
    }


    @Override
    public void enterOutputStmt(Rockstar.OutputStmtContext ctx) {

        Expression expression = new Expression(ctx.expression());
        ResultHandle value = expression.getResultHandle(main);

        // We want to do a special toString on numbers, to avoid tacking decimals onto integers
        final ResultHandle toStringed;
        if (isNumber(value)) {
            ResultHandle df = main.readStaticField(formatter);

            toStringed = main
                    .invokeVirtualMethod(
                            MethodDescriptor.ofMethod(DecimalFormat.class, "format", String.class, double.class),
                            df, value);

        } else {
            toStringed = Gizmo.toString(main, value);
        }
        Gizmo.systemOutPrintln(main, toStringed);
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