import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class HelloWorldTest {
    private final ByteArrayOutputStream testOut = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setup() {
        System.setOut(new PrintStream(testOut));
    }

    @Test
    void shouldOutputHelloWorld() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException,
            NoSuchMethodException {

        // Find the class using reflection, since the test will not have it at compile-time
        Class<?> clazz = this.getClass()
                .getClassLoader()
                .loadClass("hello_world");

        Method meth = clazz.getMethod("main", String[].class);
        meth.invoke(null, (Object) null);

        String output = testOut.toString().trim();
        assertEquals("Hello World", output, "\uD83C\uDFB8");
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }

}
