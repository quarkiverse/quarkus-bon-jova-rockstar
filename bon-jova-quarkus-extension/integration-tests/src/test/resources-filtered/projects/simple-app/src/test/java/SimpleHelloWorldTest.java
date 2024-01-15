import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SimpleHelloWorldTest {
    private final ByteArrayOutputStream testOut = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setup() {
        System.setOut(new PrintStream(testOut));
    }

    @Test
    void helloWorldDotRockShouldOutputHelloWorld() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException,
            NoSuchMethodException {

        // Find the class using reflection, since the test will not have it at compile-time
        Class clazz = this.getClass()
                          .getClassLoader()
                          .loadClass("simple_hello_world");

        Method meth = clazz.getMethod("main", String[].class);
        meth.invoke(null, (Object) null);

        assertTrue(testOut.toString()
                          .contains("This is A Hello World"));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }

}
