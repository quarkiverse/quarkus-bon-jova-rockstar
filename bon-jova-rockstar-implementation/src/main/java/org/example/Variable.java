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
    private static final Map<String, FieldDescriptor> variables = new HashMap<>();
    private static final Map<String, Class> variableTypes = new HashMap<>();
    // Because this is static, we could get cross-talk between programs, but that's a relatively low risk; we manage it by explicitly
    // clearing statics
    private static String mostRecentVariableName = null;
    private final String variableName;
    private Class variableClass;


    public Variable(Rockstar.VariableContext variable, Class<?> variableClass) {
        this(variable, false);

        this.variableClass = variableClass;
        if (!variableTypes.containsKey(variableName)) {
            variableTypes.put(variableName, variableClass);
        }

        // TODO check the type if it exists, re-assign the variable name so we have truly dynamic types
    }

    public Variable(Rockstar.VariableContext variable) {
        this(variable, true);
    }

    private Variable(Rockstar.VariableContext variable, boolean enforceType) {
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
            variableName = getNormalisedVariableName(text);
        }

        if (enforceType) {
            if (variableTypes.containsKey(variableName)) {
                variableClass = variableTypes.get(variableName);
            } else {
                throw new RuntimeException(
                        "Reference to a variable, " + variableName + ", but we do not have enough information about the type.");
            }
        }
    }

    /**
     * This is useful for things like variable names in bytecode, where spaces are not ok
     */
    private static String getNormalisedVariableName(String variableName) {
        return variableName
                .replaceAll(" +", "__") // use two underscores to reduce the chance of a clash with variable names in the program
                .toLowerCase();
    }

    public static void clearState() {
        mostRecentVariableName = null;
        variables.clear();
        variableTypes.clear();
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
        if (isAlreadyDefined()) {
            return method.readStaticField(variables.get(variableName));
        } else {
            // This is an internal error, not a program one
            throw new RuntimeException("Moral panic: Could not find variable called " + variableName);
        }
    }

    public boolean isAlreadyDefined() {
        return variables.containsKey(variableName);
    }

    public void write(BytecodeCreator method, ClassCreator creator, ResultHandle value) {

        FieldDescriptor field = getField(creator, method);
        method.writeStaticField(field, value);
    }

    private FieldDescriptor getField(ClassCreator creator, BytecodeCreator method) {
        FieldDescriptor field;
        if (!isAlreadyDefined()) {
            // Variables are global in scope, so need to be stored at the class level (either as static or instance variables)
            field = creator.getFieldCreator(variableName, variableClass)
                    .setModifiers(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE)
                    .getFieldDescriptor();
            variables.put(variableName, field);
        } else {
            field = variables.get(variableName);
            if (!creator.getClassName().equals(field.getDeclaringClass())) {
                throw new RuntimeException("Internal error: Attempting to use a field on class " + field.getDeclaringClass() + " from " + creator.getClassName());
            }
        }
        return field;
    }
}

