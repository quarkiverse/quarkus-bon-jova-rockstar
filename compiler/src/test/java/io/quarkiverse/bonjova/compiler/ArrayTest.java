package io.quarkiverse.bonjova.compiler;

import io.quarkiverse.bonjova.compiler.util.ParseHelper;
import io.quarkiverse.bonjova.support.RockstarArray;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TestClassLoader;
import org.antlr.v4.runtime.ParserRuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArrayTest {

    @BeforeEach
    public void clearState() {
        Variable.clearState();
    }

    @Test
    public void shouldParseVariableNameWithSimpleVariableArray() {
        Rockstar.ArrayStmtContext ctx = new ParseHelper().getArray("Rock thing");
        Array a = new Array(ctx);
        // Variable names should be normalised to lower case
        String name = "thing";
        assertEquals(name, a.getVariableName());
    }

    @Test
    public void shouldParseVariableNameWithCommonVariableArray() {
        Rockstar.ArrayStmtContext ctx = new ParseHelper().getArray("Rock my thing");
        Array a = new Array(ctx);
        // Variable names should be normalised to lower case
        String name = "my__thing";
        assertEquals(name, a.getVariableName());
    }

    @Test
    public void shouldParseVariableNameWithProperVariableArray() {
        Rockstar.ArrayStmtContext ctx = new ParseHelper().getArray("Rock Doctor Feelgood");
        Array a = new Array(ctx);
        // Variable names should be normalised to lower case
        assertEquals("doctor__feelgood", a.getVariableName());
    }

    @Test
    public void shouldCreateAnArrayOnInitialisationWithRock() {
        Rockstar.ArrayStmtContext ctx = new ParseHelper().getArray("Rock the thing");
        assertEquals(new ArrayList<Object>(), execute(ctx, new Array(ctx)).list);
    }

    @Test
    public void shouldCreateAnArrayOnInitialisationWithPush() {
        Rockstar.ArrayStmtContext ctx = new ParseHelper().getArray("Push the thing");
        assertEquals(new ArrayList<Object>(), execute(ctx, new Array(ctx)).list);
    }

    @Test
    public void shouldPopulateArrayOnInitialisationWithSingleElement() {
        String program = """
                Rock 4 into arr
                """;
        Rockstar.ArrayStmtContext ctx = new ParseHelper().getArray(program);
        Object[] contents = { 4d };
        assertEquals(Arrays.asList(contents), execute(ctx, new Array(ctx)).list);

    }

    @Test
    public void shouldPopulateArrayOnInitialisationWithList() {
        String program = """
                Rock 1, 2, 3, 4, 8 into arr
                """;
        Rockstar.ArrayStmtContext ctx = new ParseHelper().getArray(program);
        Object[] contents = { 1d, 2d, 3d, 4d, 8d };
        assertEquals(Arrays.asList(contents), execute(ctx, new Array(ctx)).list);
    }

    @Test
    public void shouldPopulateArrayOnInitialisationWithWith() {
        String program = """
                Rock arr with 5
                """;
        Rockstar.ArrayStmtContext ctx = new ParseHelper().getArray(program);
        Object[] contents = { 5d };
        assertEquals(Arrays.asList(contents), execute(ctx, new Array(ctx)).list);
    }

    @Test
    public void shouldPopulateArrayOnInitialisationWithWithList() {
        String program = """
                Rock arr with 1, 2, 3, 4, 8
                """;
        Rockstar.ArrayStmtContext ctx = new ParseHelper().getArray(program);
        Object[] contents = { 1d, 2d, 3d, 4d, 8d };
        assertEquals(Arrays.asList(contents), execute(ctx, new Array(ctx)).list);
    }

    @Test
    public void shouldPopulateArrayOnInitialisationWithSingleExpression() {
        String program = """
                Rock 4 + 5 into me
                """;
        Rockstar.ArrayStmtContext ctx = new ParseHelper().getArray(program);
        Object[] contents = { 9d };
        assertEquals(Arrays.asList(contents), execute(ctx, new Array(ctx)).list);
    }

    @Test
    public void shouldPopulateArrayOnInitialisationAtAIndexNextToTheBeginning() {
        String program = """
                Let arr at 0 be 2
                """;
        Rockstar.ArrayStmtContext ctx = new ParseHelper().getArray(program);
        Object[] contents = { 2d };
        assertEquals(Arrays.asList(contents), execute(ctx, new Array(ctx)).list);
    }

    @Test
    public void shouldPopulateArrayOnInitialisationAtALargeIndex() {
        String program = """
                Let arr at 5 be 2
                """;
        Rockstar.ArrayStmtContext ctx = new ParseHelper().getArray(program);
        Object[] contents = { null, null, null, null, null, 2d };
        assertEquals(Arrays.asList(contents), execute(ctx, new Array(ctx)).list);
    }

    @Test
    public void shouldAcceptNonNumericKeys() {
        String program = """
                Let arr at "hello" be 2
                """;
        Rockstar.ArrayStmtContext ctx = new ParseHelper().getArray(program);
        RockstarArray execute = execute(ctx, new Array(ctx));
        assertEquals(2d, execute.get("hello"));
    }

    @Test
    public void shouldReturnNullForAccessBeyondIndex() {
        String program = """
                Rock 1, 2 into arr
                """;
        Rockstar.ArrayStmtContext ctx = new ParseHelper().getArray(program);
        RockstarArray execute = execute(ctx, new Array(ctx));
        assertEquals(null, execute.get(5));
    }

    // We can't test reading arrays because they're multi-line executions

    private RockstarArray execute(ParserRuleContext ctx, Array a) {
        TestClassLoader cl = new TestClassLoader(this.getClass().getClassLoader());

        // The auto-close on this triggers the write
        String className = "com.ArrayTest";
        String methodName = "method";
        try (ClassCreator creator = ClassCreator.builder()
                .classOutput(cl)
                .className(className)
                .build()) {

            MethodCreator method = creator.getMethodCreator(methodName, Object.class)
                    .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
            Block block = new Block(ctx, method, creator, new VariableScope(), null);
            ResultHandle rh = a.toCode(block);
            method.returnValue(rh);
        }

        try {
            Class<?> clazz = cl.loadClass(className);
            return (RockstarArray) clazz.getMethod(methodName)
                    .invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException("Test error: " + e);
        }
    }

}
