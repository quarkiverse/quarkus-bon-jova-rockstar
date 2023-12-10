package org.example;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;
import rock.RockstarBaseListener;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class BytecodeGeneratingListener extends RockstarBaseListener {

    private final MethodCreator main;
    private final ClassCreator creator;

    private final Map<String, FieldDescriptor> variables = new HashMap<>();

    public BytecodeGeneratingListener(ClassCreator creator) {
        super();
        main = creator.getMethodCreator("main", void.class, String[].class);
        main.setModifiers(ACC_PUBLIC + ACC_STATIC);

        this.creator = creator;

    }

    @Override
    public void exitProgram(Rockstar.ProgramContext ctx) {
        main.returnVoid();
    }

    @Override
    public void enterAssignmentStmt(Rockstar.AssignmentStmtContext ctx) {

        Assignment assignment = new Assignment(ctx);

        String originalName = assignment.getVariableName();
        FieldDescriptor field;
        if (!variables.containsKey(originalName)) {

            String variableName = assignment.getNormalisedVariableName();

            // It's not strictly necessary to use a field rather than a local variable, but I wasn't sure how to do local variables
            field = creator.getFieldCreator(variableName, assignment.getVariableClass())
                           .setModifiers(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE)
                           .getFieldDescriptor();
            variables.put(originalName, field);
        } else {
            field = variables.get(originalName);
        }

        Object value = assignment.getValue();
        if (String.class.equals(assignment.getVariableClass())) {
            main.writeStaticField(field, main.load((String) value));
        } else if (int.class.equals(assignment.getVariableClass())) {
            main.writeStaticField(field, main.load((int) value));
        } else if (double.class.equals(assignment.getVariableClass())) {
            main.writeStaticField(field, main.load((double) value));
        } else if (boolean.class.equals(assignment.getVariableClass())) {
            main.writeStaticField(field, main.load((boolean) value));
        }


    }


    @Override
    public void enterOutputStmt(Rockstar.OutputStmtContext ctx) {

        String text = ctx.expression()
                         .getText();
        if (variables.containsKey(text.toLowerCase())) {
            ResultHandle value = main.readStaticField(variables.get(text.toLowerCase()));
            Gizmo.systemOutPrintln(main, Gizmo.toString(main, value));
        } else {
            // This is a literal
            // Strip out the quotes around literals (doing it in the listener rather than the lexer is simpler, and apparently
            // idiomatic-ish)
            text = text.replaceAll("\"", "");
            Gizmo.systemOutPrintln(main, main.load(text));
        }
    }
}