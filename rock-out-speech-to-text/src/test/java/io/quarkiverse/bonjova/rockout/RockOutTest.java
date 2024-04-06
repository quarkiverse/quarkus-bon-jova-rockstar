package io.quarkiverse.bonjova.rockout;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusMainTest
public class RockOutTest {

    @Test
    @Launch
    void testLaunchCommand(LaunchResult result) {
        var output = result.getOutput();
        assertTrue(output.contains("Hello from Rock Out!"));
        assertTrue(output.contains("Waiting for input..."));
    }

}