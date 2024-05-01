package io.quarkiverse.bonjova.compiler;

import io.quarkiverse.bonjova.compiler.util.ParseHelper;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TestClassLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InputTest {

    @BeforeEach
    public void clearState() throws InterruptedException {
        Input.clearState();
        Variable.clearState();
    }

    @Test
    public void shouldParseVariableName() {
        Rockstar.InputStmtContext ctx = new ParseHelper().getInput("Listen to your heart");
        Input a = new Input(ctx);
        String name = "your__heart";
        assertEquals(name, a.getVariableName());
    }

    @Test
    public void shouldSetValueBasedOnStdIn() {
        String first = "first";
        String program = """
                Listen to your heart
                """;
        Rockstar.InputStmtContext ctx = new ParseHelper().getInput(program);
        Input a = new Input(ctx);
        // This method causes failures, others do not; the raw bytecode tries to load a class left over from an earlier test run
        // It's this test which is the issue, no matter what order we run in
        assertEquals(first, execute(a, new String[] { first }));
    }

    @Test
    @Disabled("hard to get working with the new implementation - move to bytecode generator test?")
    public void shouldSetSubsequentValuesBasedOnStdIn() {
        String first = "first";
        String second = "second";
        String program = """
                Listen to your heart
                Listen to your feelings
                """;
        Rockstar.InputStmtContext ctx = new ParseHelper().getInput(program);
        Input inp = new Input(ctx);
        execute(inp, new String[] { "uno", "dos" });
        // Cheat - the parse helper won't construct a second input, so do it manually to force the increment
        assertEquals(second, execute(inp, new String[] { first, second }));
    }

    private Object execute(Input a, String[] args) {
        TestClassLoader cl = new TestClassLoader(this.getClass().getClassLoader().getParent());

        // The auto-close on this triggers the write
        String className = "com.InputRock";
        String methodName = "method";
        try (ClassCreator creator = ClassCreator.builder()
                .classOutput(cl)
                .className(className)
                .build()) {

            MethodCreator method = creator.getMethodCreator(methodName, Object.class, String[].class)
                    .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
            // Cheat and pass null for the context and hope it doesn't blow up
            Block block = new Block(null, method, creator, new VariableScope(), null);

            Variable v = a.toCode(block, method);
            ResultHandle rh = v.getResultHandle(block);
            method.returnValue(rh);
        }

        Class<?> clazz = null;
        try {
            clazz = cl.loadClass(className);
            return clazz.getMethod(methodName, String[].class)
                    .invoke(null, new Object[] { args });
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
