package io.quarkiverse.bonjova.compiler;

import io.quarkiverse.bonjova.compiler.util.ParseHelper;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TestClassLoader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;

import java.lang.reflect.InvocationTargetException;

import static io.quarkiverse.bonjova.support.Nothing.NOTHING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConstantTest {

    /*
     * empty , silent , and silence are aliases for the empty string ( "" ).
     */
    @Test
    public void shouldParseEmptyStringAliases() {
        Rockstar.ConstantContext ctx = new ParseHelper().getConstant("life is silence");
        Constant a = new Constant(ctx);
        assertEquals("", a.getValue());

        ctx = new ParseHelper().getConstant("life is silent");
        a = new Constant(ctx);
        assertEquals("", a.getValue());

        ctx = new ParseHelper().getConstant("life is empty");
        a = new Constant(ctx);
        assertEquals("", a.getValue());
    }

    @Disabled("type chaos")
    @Test
    public void shouldParseBooleanConstantsForTrueCase() {
        Rockstar.ConstantContext ctx = new ParseHelper().getConstant("life is true");
        Constant a = new Constant(ctx);
        assertEquals(true, a.getValue());
        assertEquals(boolean.class, a.getValueClass());

        ctx = new ParseHelper().getConstant("life is right");
        a = new Constant(ctx);
        assertEquals(true, a.getValue());

        ctx = new ParseHelper().getConstant("life is ok");
        a = new Constant(ctx);
        assertEquals(true, a.getValue());

        ctx = new ParseHelper().getConstant("life is yes");
        a = new Constant(ctx);
        assertEquals(true, a.getValue());
    }

    @Disabled("type chaos")
    @Test
    public void shouldParseBooleanConstantsForFalseCase() {
        Rockstar.ConstantContext ctx = new ParseHelper().getConstant("life is false");
        Constant a = new Constant(ctx);
        assertEquals(false, a.getValue());
        assertEquals(boolean.class, a.getValueClass());

        ctx = new ParseHelper().getConstant("life is lies");
        a = new Constant(ctx);
        assertEquals(false, a.getValue());

        ctx = new ParseHelper().getConstant("life is wrong");
        a = new Constant(ctx);
        assertEquals(false, a.getValue());

        ctx = new ParseHelper().getConstant("life is no");
        a = new Constant(ctx);
        assertEquals(false, a.getValue());
    }

    /*
     * Rockstar makes a distinction between `null` and `undefined`. Javascript also does this,
     * but since Java does not, we will ignore that for the moment.
     */
    @Test
    public void shouldParseUndefinedConstants() {
        Rockstar.ConstantContext ctx = new ParseHelper().getConstant("life is mysterious");
        Constant a = new Constant(ctx);
        assertNull(a.getValue());
    }

    @Test
    public void shouldParseNullConstants() {
        Rockstar.ConstantContext ctx = new ParseHelper().getConstant("life is nothing");
        Constant a = new Constant(ctx);
        assertEquals(NOTHING, a.getValue());

        ctx = new ParseHelper().getConstant("life is nobody");
        a = new Constant(ctx);
        assertEquals(NOTHING, a.getValue());

        ctx = new ParseHelper().getConstant("life is nowhere");
        a = new Constant(ctx);
        assertEquals(NOTHING, a.getValue());

        ctx = new ParseHelper().getConstant("life is gone");
        a = new Constant(ctx);
        assertEquals(NOTHING, a.getValue());
    }

    @Test
    public void shouldTreatNothingAsNothingInBytecode() {
        Rockstar.ConstantContext ctx = new ParseHelper().getConstant("thing is nothing");
        Constant a = new Constant(ctx);
        assertEquals(NOTHING, execute(a));
    }

    @Test
    public void shouldReturnFalseForNothingInBooleanContexr() {
        Rockstar.ConstantContext ctx = new ParseHelper().getConstant("say nothing");
        Constant a = new Constant(ctx);

        assertEquals(false, execute(a, Expression.Context.BOOLEAN));
    }

    @Test
    public void shouldReturnZeroForNothingInScalarContext() {
        Rockstar.ConstantContext ctx = new ParseHelper().getConstant("say nothing");
        Constant a = new Constant(ctx);

        assertEquals(0d, execute(a, Expression.Context.SCALAR));
    }

    @Disabled("No string context yet")
    @Test
    public void shouldReturnZeroForNothingInStringContext() {
        Rockstar.ConstantContext ctx = new ParseHelper().getConstant("say nothing");
        Constant a = new Constant(ctx);

        assertEquals("null", execute(a, Expression.Context.SCALAR));
    }

    @Test
    public void shouldTreatMysteriousAsNullInBytecode() {
        Rockstar.ConstantContext ctx = new ParseHelper().getConstant("thing is mysterious");
        Constant a = new Constant(ctx);
        assertNull(execute(a));
    }

    @Test
    public void shouldReturnCorrectBytecodeForEmpty() {
        Rockstar.ConstantContext ctx = new ParseHelper().getConstant("thing is empty");
        Constant a = new Constant(ctx);
        assertEquals("", execute(a));
    }

    @Test
    public void shouldReturnCorrectBytecodeForFalse() {
        Rockstar.ConstantContext ctx = new ParseHelper().getConstant("thing is lies");
        Constant a = new Constant(ctx);
        assertFalse((Boolean) execute(a));
    }

    @Test
    public void shouldReturnCorrectBytecodeForTrue() {
        Rockstar.ConstantContext ctx = new ParseHelper().getConstant("thing is ok");
        Constant a = new Constant(ctx);
        assertTrue((Boolean) execute(a));
    }

    private Object execute(Constant a) {
        TestClassLoader cl = new TestClassLoader(this.getClass().getClassLoader());

        // The auto-close on this triggers the write
        try (ClassCreator creator = ClassCreator.builder()
                .classOutput(cl)
                .className("com.MyTest")
                .build()) {

            MethodCreator method = creator.getMethodCreator("method", Object.class)
                    .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
            ResultHandle rh = a.getResultHandle(method);
            method.returnValue(rh);
        }

        try {
            Class<?> clazz = cl.loadClass("com.MyTest");
            return clazz.getMethod("method")
                    .invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException("Test error: " + e);
        }
    }

    private Object execute(Constant a, Expression.Context context) {
        TestClassLoader cl = new TestClassLoader(this.getClass().getClassLoader());

        // The auto-close on this triggers the write
        try (ClassCreator creator = ClassCreator.builder()
                .classOutput(cl)
                .className("com.MyTest")
                .build()) {

            MethodCreator method = creator.getMethodCreator("method", Object.class)
                    .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
            ResultHandle rh = a.getResultHandle(method, context);
            method.returnValue(rh);
        }

        try {
            Class<?> clazz = cl.loadClass("com.MyTest");
            return clazz.getMethod("method")
                    .invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException("Test error: " + e);
        }
    }

}
