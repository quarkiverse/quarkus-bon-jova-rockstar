import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TestClassLoader;
import org.example.StringSplit;
import org.example.Variable;
import org.example.util.ParseHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import rock.Rockstar;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        assertDeepEquals(new ArrayList<String>(), execute(new StringSplit(ctx)));
    }

    @Test
    public void shouldSplitIntoCharacterArrayWithNoDelimiter() {
        List expected = Arrays.asList("h", "e", "l", "l", "o");
        Rockstar.StringStmtContext ctx = new ParseHelper().getStringSplit("Cut \"hello\" into pieces");
        assertDeepEquals(expected, execute(new StringSplit(ctx)));
    }

    @Test
    public void shouldSplitIntoCharacterArrayHonouringDelimiter() {
        List expected = Arrays.asList("first", "second");
        Rockstar.StringStmtContext ctx = new ParseHelper().getStringSplit("Cut \"first, second\" into pieces with \", \"");
        assertDeepEquals(expected, execute(new StringSplit(ctx)));
    }

    private static void assertDeepEquals(List a, List b) {
        // The class may be different, so dump to string
        assertEquals(Arrays.toString(a.toArray()), Arrays.toString(b.toArray()));
    }

    private List execute(StringSplit a) {
        TestClassLoader cl = new TestClassLoader(this.getClass().getClassLoader().getParent());

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
            return (List) clazz.getMethod(methodName)
                    .invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            fail(e);
            return null;
        }
    }

}
