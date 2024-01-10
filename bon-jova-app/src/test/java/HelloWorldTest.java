import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class HelloWorldTest {
    private final ByteArrayOutputStream testOut = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setup() {
        System.setOut(new PrintStream(testOut));
    }

    @Test
    void helloWorldDotRockShouldOutputHelloWorld() {
//        var helloWorld = new hello_world();
//        helloWorld.main(null);

        assertTrue(testOut.toString().contains("Hello World"));
        assertTrue(testOut.toString().contains("Rockstar rockzzz"));
        assertTrue(testOut.toString().contains("What what?"));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }

}
