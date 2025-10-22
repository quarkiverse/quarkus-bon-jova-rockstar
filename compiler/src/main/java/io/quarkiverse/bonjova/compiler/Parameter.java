package io.quarkiverse.bonjova.compiler;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;

public class Parameter extends Variable {

    public Parameter(Rockstar.VariableContext variable, Class<?> variableClass) {
        this(variable.getText(), variable.PRONOUNS(), variableClass);
    }

    private Parameter(String text, TerminalNode pronouns, Class<?> variableClass) {
        this(text, pronouns, false);

        this.variableClass = variableClass;
        // TODO type chaos
        this.variableClass = Object.class;
    }

    public Parameter(Rockstar.VariableContext variable) {
        this(variable, true);
    }

    private Parameter(Rockstar.VariableContext variable, boolean enforceType) {
        this(variable.getText(), variable.PRONOUNS(), enforceType);
    }

    private Parameter(String text, TerminalNode pronouns, boolean enforceType) {
        super(text, pronouns, enforceType);
        // Work out the variable name
        // In principle trivial, in practice made a bit complicated by normalisation and more complicated by pronouns
        // TODO type chaos
        this.variableClass = Object.class;

        if (pronouns != null) {
            // This could be an internal error or a program one
            throw new RuntimeException("Cannot use pronouns in function declarations");
        }
    }

    /*
     * Pronouns refer to the last named variable determined by parsing order.
     * In practice, this is only on assignment, not on any reference, so this needs to be triggered externally.
     */
    @Override
    public void track() {
        // Pronouns don't apply to parameters; do nothing
    }

    @Override
    public ResultHandle getResultHandle(Block block) {
        FieldDescriptor field = getField(block);
        // TODO not a static field
        return block.method().readStaticField(field);
    }

    @Override
    public void write(Block block, ResultHandle value) {
        FieldDescriptor field = getOrCreateField(block);

        // TODO a parameter is not a static field, but it might be modifiable, so we can't just always reference the function param by index
        block.method().writeStaticField(field, value);
    }

    @Override
    protected FieldDescriptor getField(Block block) {
        FieldDescriptor field;
        if (isAlreadyWritten(block)) {
            field = block.variables().get(variableName);

        } else {
            // This is an internal error, not a program one

            throw new RuntimeException("Moral panic: Could not find variable called " + variableName
                    + ". \nKnown variables are " + block.variables().getAllKnownVariables());
        }

        return field;
    }

    @Override
    protected FieldDescriptor getOrCreateField(Block block) {
        FieldDescriptor field;
        ClassCreator creator = block.creator();
        if (!isAlreadyWritten(block)) {
            // Variables are global in method, so need to be stored at the class level (either as static or instance variables)
            field = creator.getFieldCreator(variableName, variableClass)
                    .setModifiers(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE)
                    .getFieldDescriptor();

            block.variables().put(variableName, field);
        } else {
            field = getField(block);
            if (!creator.getClassName().equals(field.getDeclaringClass())) {
                throw new RuntimeException("Internal error: Attempting to use a field on class " + field.getDeclaringClass()
                        + " from " + creator.getClassName());
            }
        }
        return field;
    }
}
