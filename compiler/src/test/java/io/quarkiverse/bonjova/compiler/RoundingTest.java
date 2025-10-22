package io.quarkiverse.bonjova.compiler;

import io.quarkiverse.bonjova.compiler.util.ParseHelper;
import io.quarkiverse.bonjova.compiler.util.RoundingAndAssignmentPair;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TestClassLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
Note that even though we're just testing the Roundings, they need to be couched in something like output statement to be
recognised.
 */
public class RoundingTest {

    @BeforeEach
    public void clearState() {
        Variable.clearState();
    }

    @Test
    public void shouldRoundToClosestInteger() throws IOException {
        assertEquals(4d, getExecutionResult("X is 3.6\nturn X round"));

        assertEquals(3d, getExecutionResult("Y is 3.2\nturn Y round"));
    }

    @Test
    public void shouldHonourPronouns() throws IOException {
        assertEquals(4d, getExecutionResult("X is 3.6\nturn it round"));

        assertEquals(3d, getExecutionResult("Y is 3.2\nturn it round"));
    }

    @Test
    public void shouldHandleAroundAsASynonym() throws IOException {
        assertEquals(4d, getExecutionResult("X is 3.6\nturn X around"));

        assertEquals(3d, getExecutionResult("Y is 3.2\nturn Y round"));
    }

    @Test
    public void shouldRoundDown() throws IOException {
        assertEquals(3d, getExecutionResult("X is 3.6\nturn X down"));

        assertEquals(3d, getExecutionResult("Y is 3.2\nturn Y down"));
    }

    @Test
    public void shouldRoundUp() throws IOException {
        assertEquals(4d, getExecutionResult("X is 3.6\nturn X up"));

        assertEquals(4d, getExecutionResult("Y is 3.2\nturn Y up"));
    }

    private Object getExecutionResult(String program) throws IOException {
        RoundingAndAssignmentPair bug = new ParseHelper().getRounding(program);
        Rockstar.AssignmentStmtContext ctx = bug.assignmentStmtContext();
        Assignment a = new Assignment(ctx);
        Rounding r = new Rounding(bug.roundingStmtContext());
        return execute(ctx, a, r);
    }

    private Object execute(Rockstar.AssignmentStmtContext ctx, Assignment a, Rounding r) {
        TestClassLoader cl = new TestClassLoader(this.getClass().getClassLoader().getParent());

        // The auto-close on this triggers the write
        String className = "com.RoundingTestGeneratedCode";
        String methodName = "method";
        try (ClassCreator creator = ClassCreator.builder()
                .classOutput(cl)
                .className(className)
                .build()) {

            MethodCreator method = creator.getMethodCreator(methodName, Object.class)
                    .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);

            Block block = new Block(ctx, method, creator, new VariableScope(), null);

            a.toCode(block);
            ResultHandle rh = r.toCode(block);
            method.returnValue(rh);
        }

        try {
            Class<?> clazz = cl.loadClass(className);
            return clazz.getMethod(methodName)
                    .invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException("Test error: " + e);
        }
    }
}