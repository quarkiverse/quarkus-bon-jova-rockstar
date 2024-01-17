package org.example;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import org.example.util.DynamicClassLoader;
import org.example.util.ParseHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InputTest {

    @BeforeEach
    public void clearState() {
        Input.setStdIn();
    }

    @Test
    public void shouldParseVariableName() {
        Rockstar.InputStmtContext ctx = ParseHelper.getInput("Listen to your heart");
        Input a = new Input(ctx);
        String name = "your__heart";
        assertEquals(name, a.getVariableName());
    }

    @Test
    public void shouldSetValueBasedOnStdIn() {
        String first = "first";
        String second = "second";
        //    Input.setStdIn(new String[]{first, second});
        Rockstar.InputStmtContext ctx = ParseHelper.getInput("Listen to your heart");
        Input a = new Input(ctx);
        assertEquals(first, execute(a, new String[]{first, second}));
    }

    @Test
    public void shouldSetSubsequentValuesBasedOnStdIn() {
        String first = "first";
        String second = "second";
        String program = """
                Listen to your heart
                Listen to your feelings
                """;
        Rockstar.InputStmtContext ctx = ParseHelper.getInput(program);
        Input a = new Input(ctx);
        // Cheat - the parse helper won't construct a second input, so do it manually to force the increment
        a = new Input(ctx);
        assertEquals(second, execute(a, new String[]{first, second}));
    }

    @Test
    public void shouldWriteToClass() {
        Rockstar.InputStmtContext ctx = ParseHelper.getInput("Listen to your heart");
        Input a = new Input(ctx);

        ClassCreator creator = ClassCreator.builder()
                                           .className("holder")
                                           .build();
        MethodCreator method = creator.getMethodCreator("main", void.class, String[].class);

    }


    private Object execute(Input a, String[] args) {
        DynamicClassLoader cl = new DynamicClassLoader();

        // The auto-close on this triggers the write
        try (ClassCreator creator = ClassCreator.builder()
                                                .classOutput(cl)
                                                .className("com.MyTest")
                                                .build()) {

            MethodCreator method = creator.getMethodCreator("method", Object.class, String[].class)
                                          .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);

            FieldDescriptor fd = a.toCode(creator, method, method);

            ResultHandle rh = method.readStaticField(fd);
            method.returnValue(rh);

        }

        try {
            Class<?> clazz = cl.loadClass("com.MyTest");
            return clazz.getMethod("method", String[].class)
                        .invoke(null, new Object[]{args});
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException("Test error: " + e);
        }
    }

}
