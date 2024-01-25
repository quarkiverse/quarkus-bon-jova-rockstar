package org.example;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TestClassLoader;
import org.example.util.ParseHelper;
import org.example.util.RoundingAndAssignmentPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

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
        Assignment a = new Assignment(bug.assignmentStmtContext());
        Rounding r = new Rounding(bug.roundingStmtContext());
        return execute(a, r);
    }

    private Object execute(Assignment a, Rounding r) {
        TestClassLoader cl = new TestClassLoader(this.getClass().getClassLoader().getParent());

        // The auto-close on this triggers the write
        String className = "com.RoundingTest";
        String methodName = "method";
        try (ClassCreator creator = ClassCreator.builder()
                .classOutput(cl)
                .className(className)
                .build()) {

            MethodCreator method = creator.getMethodCreator(methodName, Object.class)
                    .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);

            a.toCode(creator, method);
            ResultHandle rh = r.toCode(method);
            method.returnValue(rh);
        }

        try {
            Class<?> clazz = cl.loadClass(className);
            return clazz.getMethod(methodName)
                    .invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException("Test error: " + e);
        }
    }
}