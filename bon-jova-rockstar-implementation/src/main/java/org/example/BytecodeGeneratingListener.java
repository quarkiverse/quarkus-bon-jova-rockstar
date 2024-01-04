package org.example;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import org.antlr.v4.runtime.tree.TerminalNode;
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
    private String mostRecentVariableName = null;

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
        trackVariable(ctx.variable());

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
    public void enterIncrementStmt(Rockstar.IncrementStmtContext ctx) {
        Rockstar.VariableContext variable = ctx.variable();

        int count = ctx.ups()
                       .KW_UP()
                       .size();

        for (int i = 0; i < count; i++) {

            ResultHandle value = getVariable(variable);
            ResultHandle incremented;
            try {
                // This is ugly, but the result handle does not expose the method to get the type publicly, so we need trial and error
                // (or to track it ourselves)
                // TODO According to the spec, all numbers are actually double, and so in principle we should always increment with 1.0, but
                //  that's not what's implemented now
                incremented = main.increment(value);
            } catch (RuntimeException e) {
                try {
                    incremented = main.add(value, main.load((double) 1));
                } catch (RuntimeException ee) {
                    // This must be a string
                    ResultHandle constant = main.load("1");
                    incremented = main.invokeVirtualMethod(
                            MethodDescriptor.ofMethod("java/lang/String", "concat", "Ljava/lang/String;", "Ljava/lang/String;"), value,
                            constant);
                }
            }

            writeVariable(variable, incremented);
        }
    }

    @Override
    public void enterDecrementStmt(Rockstar.DecrementStmtContext ctx) {
        Rockstar.VariableContext variable = ctx.variable();

        int count = ctx.downs()
                       .KW_DOWN()
                       .size();

        for (int i = 0; i < count; i++) {
            ResultHandle value = getVariable(variable);
            ResultHandle incremented;
            try {
                // This is ugly, but the result handle does not expose the method to get the type publicly, so we need trial and error
                // (or to track it ourselves)
                // TODO According to the spec, all numbers are actually double, and so in principle we should always increment with 1.0, but
                //  that's not what's implemented now
                incremented = main.add(value, main.load(-1));
            } catch (RuntimeException e) {
                incremented = main.add(value, main.load((double) -1));
            }

            writeVariable(variable, incremented);
        }
    }

    private void writeVariable(Rockstar.VariableContext variable, ResultHandle value) {
        String variableName = getVariableName(variable);
        FieldDescriptor field = variables.get(variableName);

        main.writeStaticField(field, value);
    }

    @Override
    public void enterOutputStmt(Rockstar.OutputStmtContext ctx) {
        Rockstar.VariableContext variable = ctx.expression()
                                               .variable();
        if (variable != null) {

            ResultHandle value = getVariable(variable);

            Gizmo.systemOutPrintln(main, Gizmo.toString(main, value));

        } else {
            // This is a literal
            // Strip out the quotes around literals (doing it in the listener rather than the lexer is simpler, and apparently
            // idiomatic-ish)
            String text = ctx.expression()
                             .getText();
            text = text.replaceAll("\"", "");
            Gizmo.systemOutPrintln(main, main.load(text));
        }
    }

    private ResultHandle getVariable(Rockstar.VariableContext variable) {
        String variableName = getVariableName(variable);
        if (variables.containsKey(variableName)) {
            return main.readStaticField(variables.get(variableName));

        } else {
            // This is an internal error, not a program one
            throw new RuntimeException("Moral panic: Could not find variable called " + variableName);
        }
    }

    // In principal trivial, in practice made a bit complicated by normalisation and more complicated by pronouns
    private String getVariableName(Rockstar.VariableContext variable) {
        String variableName;
        TerminalNode pronouns = variable.PRONOUNS();
        if (pronouns != null) {
            if (mostRecentVariableName == null) {
                // This could be an internal error or a program one
                throw new RuntimeException("No good: Unassociated pronoun");
            }
            variableName = mostRecentVariableName;

        } else {
            String text = variable
                    .getText();
            variableName = text.toLowerCase();
        }
        return variableName;
    }

    /*
       Pronouns refer to the last named variable determined by parsing order.
     */
    private void trackVariable(Rockstar.VariableContext variable) {
        if (variable != null) {
            mostRecentVariableName = variable.getText()
                                             .toLowerCase();
        }
    }
}