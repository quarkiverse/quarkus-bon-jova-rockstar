package org.example;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;

import java.util.HashMap;
import java.util.Map;

public class Variable {
    // Because this is static, we could get cross-talk between programs, but that's a relatively low risk; we manage it by explicitly
    // clearing statics
    private static String mostRecentVariableName = null;
    private static final Map<String, FieldDescriptor> variables = new HashMap<>();

    private final String variableName;

    public Variable(Rockstar.VariableContext variable) {

        // Work out the variable name
        // In principle trivial, in practice made a bit complicated by normalisation and more complicated by pronouns

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
    }

    public String getVariableName() {
        return variableName;
    }

    /**
     * This is useful for things like variable names in bytecode, where spaces are not ok
     */
    private String getNormalisedVariableName() {
        return variableName
                .replace(" ", "__") // use two underscores to reduce the chance of a clash with variable names in the program
                .toLowerCase();
    }

    /*
       Pronouns refer to the last named variable determined by parsing order.
       In practice, this is only on assignment, not on any reference, so this needs to be triggered externally.
     */
    public void track() {
        if (variableName != null) {
            mostRecentVariableName = variableName;
        }
    }

    public static void clearPronouns() {
        mostRecentVariableName = null;
        variables.clear();
    }


    public ResultHandle read(BytecodeCreator method) {
        if (variables.containsKey(variableName)) {
            return method.readStaticField(variables.get(variableName));

        } else {
            // This is an internal error, not a program one
            throw new RuntimeException("Moral panic: Could not find variable called " + variableName);
        }
    }

    public void write(BytecodeCreator method, ResultHandle value) {

        FieldDescriptor field = variables.get(variableName);

        method.writeStaticField(field, value);
    }

    // TODO if we use local variables we can drop passing the class creator
    // TODO would it be nicer to store our own class?
    public FieldDescriptor getField(ClassCreator creator, BytecodeCreator method, Class<?> clazz) {
        FieldDescriptor field;
        if (!variables.containsKey(variableName)) {

            String javaCompliantName = getNormalisedVariableName();

            // It's not strictly necessary to use a field rather than a local variable, but I wasn't sure how to do local variables
            field = creator.getFieldCreator(javaCompliantName, clazz)
                           .setModifiers(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE)
                           .getFieldDescriptor();
            variables.put(variableName, field);
        } else {
            field = variables.get(variableName);
        }
        return field;
    }


}

