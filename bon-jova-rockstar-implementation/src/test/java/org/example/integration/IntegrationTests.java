package org.example.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.example.util.FileLauncher.compileAndLaunch;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegrationTests {

    @Test
    public void shouldRun99BottlesOfBeer() throws IOException, URISyntaxException {
        String output = compileAndLaunch("/99-bottles.rock");
        String expected = getFileContents("/expected-99-bottles.output");
        assertEquals(expected, output);
    }

    @Test
    public void shouldRunFizzBuzz() throws IOException, URISyntaxException {
        String output = compileAndLaunch("/fizzbuzz.rock");
        String expected = getFileContents("/expected-fizzbuzz.output");
        assertEquals(expected, output);
    }

    @Disabled("Not yet working")
    @Test
    public void shouldRunFactorization() throws IOException, URISyntaxException {
        String output = compileAndLaunch("/factorization.rock", "178");
        assertEquals("2\n89\n", output);
    }

    @Disabled("Not yet working")
    @Test
    public void shouldRunMandelbrot() throws IOException, URISyntaxException {
        String output = compileAndLaunch("/mandelbrot.rock");
        String expected = getFileContents("/expected-mandelbrot.output");
        assertEquals(expected, output);
    }

    private String getFileContents(String name) throws URISyntaxException, IOException {
        Path filePath = Path.of(this.getClass()
                .getResource(name)
                .toURI());

        return Files.readString(filePath);
    }
}
