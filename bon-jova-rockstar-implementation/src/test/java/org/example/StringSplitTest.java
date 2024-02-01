package org.example;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TestClassLoader;
import org.example.util.ParseHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class StringSplitTest {

    @BeforeEach
    public void clearState() {
        Variable.clearState();
    }

    @Test
    public void shouldParseVariableNameWithSimpleVariableStringSplit() {
        Rockstar.StringStmtContext ctx = new ParseHelper().getStringSplit("Cut \"my life\" into pieces");
        StringSplit a = new StringSplit(ctx);
        // Variable names should be normalised to lower case
        String name = "pieces";
        assertEquals(name, a.getVariableName());
    }

    @Test
    public void shouldFailToSplitWithoutAVariable() {
        assertThrows(RuntimeException.class, () -> new StringSplit(new ParseHelper().getStringSplit("Cut \"my life\"")));
    }

    @Test
    public void shouldCreateAnArrayOnSimpleSplit() {
        Rockstar.StringStmtContext ctx = new ParseHelper().getStringSplit("Cut \"\" into pieces");
        assertDeepEquals(new RockstarArray(), execute(new StringSplit(ctx)));
    }

    @Test
    public void shouldSplitIntoCharacterArrayWithNoDelimiter() {
        RockstarArray expected = new RockstarArray();
        expected.addAll(Arrays.asList("h", "e", "l", "l", "o"));
        Rockstar.StringStmtContext ctx = new ParseHelper().getStringSplit("Cut \"hello\" into pieces");
        assertDeepEquals(expected, execute(new StringSplit(ctx)));
    }

    @Test
    public void shouldSplitIntoCharacterArrayHonouringDelimiter() {
        RockstarArray expected = new RockstarArray();
        expected.addAll(Arrays.asList("first", "second"));
        Rockstar.StringStmtContext ctx = new ParseHelper().getStringSplit("Cut \"first, second\" into pieces with \", \"");
        assertDeepEquals(expected, execute(new StringSplit(ctx)));
    }

    private static void assertDeepEquals(RockstarArray a, RockstarArray b) {
        // The class may be different, so dump to string
        assertEquals(Arrays.toString(a.list.toArray()), Arrays.toString(b.list.toArray()));
    }

    private RockstarArray execute(StringSplit a) {
        TestClassLoader cl = new TestClassLoader(this.getClass().getClassLoader());

        // The auto-close on this triggers the write
        String className = "com.StringSplitTest";
        String methodName = "method";
        try (ClassCreator creator = ClassCreator.builder()
                .classOutput(cl)
                .className(className)
                .build()) {

            MethodCreator method = creator.getMethodCreator(methodName, Object.class)
                    .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
            ResultHandle rh = a.toCode(method, creator);
            method.returnValue(rh);
        }

        try {
            Class<?> clazz = cl.loadClass(className);
            return (RockstarArray) clazz.getMethod(methodName)
                    .invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            fail(e);
            return null;
        }
    }

}
