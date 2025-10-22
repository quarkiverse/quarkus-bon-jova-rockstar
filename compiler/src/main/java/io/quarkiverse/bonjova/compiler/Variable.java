package io.quarkiverse.bonjova.compiler;

import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;

import java.util.Arrays;

public class Variable implements ValueHolder {

    private static String mostRecentVariableName = null;
    protected final String variableName;
    protected Class<?> variableClass;

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

    protected Variable(String text, TerminalNode pronouns, boolean enforceType) {
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
    }

    public Class<?> getVariableClass() {
        return variableClass;
    }

    public String getVariableName() {
        return variableName;
    }

    /*
     * Pronouns refer to the last named variable determined by parsing order.
     * In practice, this is only on assignment, not on any reference, so this needs to be triggered externally.
     */
    public void track() {
        if (variableName != null) {
            mostRecentVariableName = variableName;
        }
    }

    public ResultHandle getResultHandle(Block block) {
        FieldDescriptor field = getField(block);
        return block.method().readStaticField(field);
    }

    public ResultHandle getResultHandle(Block block, Expression.Context context) {
        return getResultHandle(block);
    }

    public void write(Block block, ResultHandle value) {
        FieldDescriptor field = getOrCreateField(block);
        block.method().writeStaticField(field, value);
    }

    public boolean isAlreadyWritten(Block block) {
        FieldDescriptor field = block.variables().get(variableName);
        return field != null;
    }

    private FieldDescriptor getFieldRecursive(Block block) {
        if (isAlreadyWritten(block)) {
            return block.variables().get(variableName);
        } else if (block.parent() != null) {
            return getFieldRecursive(block.parent());
        } else {
            return null;
        }

    }

    protected FieldDescriptor getField(Block block) {
        FieldDescriptor field = getFieldRecursive(block);

        if (field == null) {
            // This is an internal error, not a program one

            throw new RuntimeException("Moral panic: Could not find variable called " + variableName
                    + ". \nKnown variables are " + Arrays.toString(block.variables().getAllKnownVariables()));
        }

        return field;
    }

    protected FieldDescriptor getOrCreateField(Block block) {
        FieldDescriptor field;
        if (!isAlreadyWritten(block)) {
            // Variables are global in method, so need to be stored at the class level (either as static or instance variables)
            field = block.creator().getFieldCreator(variableName, variableClass)
                    .setModifiers(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE)
                    .getFieldDescriptor();

            block.variables().put(variableName, field);
        } else {
            field = getField(block);
            if (!block.creator().getClassName().equals(field.getDeclaringClass())) {
                throw new RuntimeException("Internal error: Attempting to use a field on class " + field.getDeclaringClass()
                        + " from " + block.creator().getClassName());
            }
        }
        return field;
    }
}
