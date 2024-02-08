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

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
Note: The programs here need some context outside the literal, such as an assignment or an output statement, to be recognised by the
grammar.
 Note also that poetic string and poetic literals are not handled by the literal class. */
public class LiteralTest {

    @Disabled("type chaos")
    @Test
    public void shouldParseIntegerLiterals() {
        Rockstar.LiteralContext ctx = new ParseHelper().getLiteral("thing is 5");
        Literal a = new Literal(ctx);
        // The number should be stored as a double, even though it was entered as an integer
        assertEquals(5d, a.getValue());
        assertEquals(double.class, a.getValueClass());
    }

    /* Numbers in Rockstar are double-precision floating point numbers, stored according to the IEEE 754 standard. */
    @Disabled("type chaos")
    @Test
    public void shouldParseFloatingPointLiterals() {
        Rockstar.LiteralContext ctx = new ParseHelper().getLiteral("thing is 3.141");
        Literal a = new Literal(ctx);
        assertEquals(3.141, a.getValue());
        assertEquals(double.class, a.getValueClass());
    }

    @Disabled("type chaos")
    @Test
    public void shouldParseNegativeLiterals() {
        Rockstar.LiteralContext ctx = new ParseHelper().getLiteral("thing is -5");
        Literal a = new Literal(ctx);
        // The number should be stored as a double, even though it was entered as an integer
        assertEquals(-5d, a.getValue());
        assertEquals(double.class, a.getValueClass());
    }

    @Disabled("type chaos")
    @Test
    public void shouldIdentifyTypeOfStringLiterals() {
        Rockstar.LiteralContext ctx = new ParseHelper().getLiteral("thing is \"Yes hello\"");
        Literal a = new Literal(ctx);
        assertEquals(String.class, a.getValueClass());
    }

    @Test
    public void shouldParseStringLiterals() {
        Rockstar.LiteralContext ctx = new ParseHelper().getLiteral("thing is \"Yes hello\"");
        Literal a = new Literal(ctx);
        assertEquals("Yes hello", a.getValue());
        assertEquals("Yes hello", execute(a));
    }

    @Test
    public void shouldReturnCorrectBytecodeForStringLiterals() {
        Rockstar.LiteralContext ctx = new ParseHelper().getLiteral("thing is \"Yes hello\"");
        Literal a = new Literal(ctx);
        assertEquals("Yes hello", execute(a));
    }

    @Test
    public void shouldReturnCorrectBytecodeForNumberLiterals() {
        Rockstar.LiteralContext ctx = new ParseHelper().getLiteral("thing is 61");
        Literal a = new Literal(ctx);
        assertEquals(61d, execute(a));
    }

    private Object execute(Literal a) {
        TestClassLoader cl = new TestClassLoader(this.getClass().getClassLoader().getParent());

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

}
