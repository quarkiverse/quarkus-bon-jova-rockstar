package io.quarkiverse.bonjova.compiler;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Variable {
    private static final Map<String, FieldDescriptor> variables = new HashMap<>();
    // Because this is static, we could get cross-talk between programs, but that's a relatively low risk; we manage it by explicitly
    // clearing statics
    private static String mostRecentVariableName = null;
    private final String variableName;
    private Class<?> variableClass;

    public Variable(Rockstar.VariableContext variable, Class<?> variableClass) {
        this(variable.getText(), variable.PRONOUNS(), variableClass);
    }


    private Variable(String text, TerminalNode pronouns, Class<?> variableClass) {
        this(text, pronouns, false);

        this.variableClass = variableClass;
        // TODO
        this.variableClass = Object.class;
    }

    public Variable(Rockstar.VariableContext variable) {
        this(variable, true);
    }

    private Variable(Rockstar.VariableContext variable, boolean enforceType) {
        this(variable.getText(), variable.PRONOUNS(), enforceType);
    }

    private Variable(String text, TerminalNode pronouns, boolean enforceType) {
        // Work out the variable name
        // In principle trivial, in practice made a bit complicated by normalisation and more complicated by pronouns
// TODO type chaos
        this.variableClass = Object.class;

        if (pronouns != null) {
            if (mostRecentVariableName == null) {
                // This could be an internal error or a program one
                throw new RuntimeException("No good: Unassociated pronoun");
            }
            variableName = mostRecentVariableName;

        } else {
            variableName = getNormalisedVariableName(text);
        }
    }

    /**
     * This is useful for things like variable names in bytecode, where spaces are not ok
     */
    static String getNormalisedVariableName(String variableName) {
        return variableName
                .replaceAll(" +", "__") // use two underscores to reduce the chance of a clash with variable names in the program
                .toLowerCase();
    }

    public static void clearState() {
        mostRecentVariableName = null;
        variables.clear();
    }

    public Class<?> getVariableClass() {
        return variableClass;
    }

    public String getVariableName() {
        return variableName;
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

    public ResultHandle read(BytecodeCreator method) {
        FieldDescriptor field = getField();
        return method.readStaticField(field);
    }

    public void write(BytecodeCreator method, ClassCreator creator, ResultHandle value) {
        FieldDescriptor field = getOrCreateField(creator, method);
        method.writeStaticField(field, value);
    }

    public boolean isAlreadyWritten() {
        FieldDescriptor field = variables.get(variableName);
        return field != null;
    }

    private FieldDescriptor getField() {
        FieldDescriptor field;
        if (isAlreadyWritten()) {
            field = variables.get(variableName);

        } else {
            // This is an internal error, not a program one

            throw new RuntimeException("Moral panic: Could not find variable called " + variableName + ". \nKnown variables are " + Arrays.toString(variables.keySet().toArray()));
        }

        return field;
    }

    private FieldDescriptor getOrCreateField(ClassCreator creator, BytecodeCreator method) {
        FieldDescriptor field;
        if (!isAlreadyWritten()) {
            // Variables are global in scope, so need to be stored at the class level (either as static or instance variables)
            field = creator.getFieldCreator(variableName, variableClass)
                    .setModifiers(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE)
                    .getFieldDescriptor();

            variables.put(variableName, field);
        } else {
            field = getField();
            if (!creator.getClassName().equals(field.getDeclaringClass())) {
                throw new RuntimeException("Internal error: Attempting to use a field on class " + field.getDeclaringClass() + " from " + creator.getClassName());
            }
        }
        return field;
    }
}

