package org.example;

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
    private static final Map<String, Map<Class<?>, FieldDescriptor>> variables = new HashMap<>();
    // Because this is static, we could get cross-talk between programs, but that's a relatively low risk; we manage it by explicitly
    // clearing statics
    private static String mostRecentVariableName = null;
    private final String variableName;
    private Class<?> variableClass;

    public Variable(Rockstar.VariableContext variable, Class<?> variableClass) {
        this(variable.getText(), variable.PRONOUNS(), variableClass);
    }

    Variable(String text, Class<?> variableClass) {
        this(text, null, variableClass);
    }

    /**
     * Used for synthetic variables; should not generally be used
     */
    private Variable(String text, TerminalNode pronouns, Class<?> variableClass) {
        this(text, pronouns, false);

        this.variableClass = variableClass;

        if (!variables.containsKey(variableName)) {
            Map<Class<?>, FieldDescriptor> map = new HashMap<>();
            map.put(this.variableClass, null);
            variables.put(this.variableName, map);
        } else {
            Map<Class<?>, FieldDescriptor> map = variables.get(this.variableName);
            // Overwrite any previous type references
            if (!map.containsKey(variableClass)) {
                map.clear();
                map.put(variableClass, null);
            }
        }
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

        if (pronouns != null) {
            if (mostRecentVariableName == null) {
                // This could be an internal error or a program one
                throw new RuntimeException("No good: Unassociated pronoun");
            }
            variableName = mostRecentVariableName;

        } else {
            variableName = getNormalisedVariableName(text);
        }

        if (enforceType) {
            Map<Class<?>, FieldDescriptor> map = variables.get(variableName);
            if (map != null) {
                if (map.size() == 1) {
                    variableClass = map.keySet().iterator().next();
                } else {
                    String types = Arrays.toString(map.keySet().toArray());
                    throw new RuntimeException(
                            "The variable, " + variableName + " has been used as the following types, so we do not know which type this reference should be: " + types);
                }
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
        Map<Class<?>, FieldDescriptor> classFieldDescriptorMap = variables.get(variableName);
        return classFieldDescriptorMap != null && classFieldDescriptorMap.get(variableClass) != null;
    }

    private FieldDescriptor getField() {
        FieldDescriptor field;
        if (isAlreadyWritten()) {
            Map<Class<?>, FieldDescriptor> classFieldDescriptorMap = variables.get(variableName);
            field = classFieldDescriptorMap.get(variableClass);
        } else {
            // This is an internal error, not a program one
            if (variables.get(variableName) != null) {
                throw new RuntimeException("Different class: We looked for a " + variableClass + " but the variable " + variableName + " is stored as a class " + Arrays.toString(variables.get(variableName).keySet().toArray()));
            } else {
                throw new RuntimeException("Moral panic: Could not find variable called " + variableName + " of class " + variableClass + ". \nKnown variables are " + Arrays.toString(variables.keySet().toArray()));
            }
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
            Map<Class<?>, FieldDescriptor> classFieldDescriptorMap = variables.get(variableName);
            classFieldDescriptorMap.put(variableClass, field);
        } else {
            field = getField();
            if (!creator.getClassName().equals(field.getDeclaringClass())) {
                throw new RuntimeException("Internal error: Attempting to use a field on class " + field.getDeclaringClass() + " from " + creator.getClassName());
            }
        }
        return field;
    }


}

