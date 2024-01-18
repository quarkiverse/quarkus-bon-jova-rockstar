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

    @Disabled("Causing InputTest to fail, for ... some reason?")
    @Test
    public void shouldRun99BottlesOfBeer() throws IOException, URISyntaxException {
        String output = compileAndLaunch("/99-bottles.rock");
        String expected = getFileContents("/expected-99-bottles.output");
        assertEquals(expected, output);
    }

    private String getFileContents(String name) throws URISyntaxException, IOException {
        Path filePath = Path.of(this.getClass()
                .getResource(name)
                .toURI());

        return Files.readString(filePath);
    }
}
