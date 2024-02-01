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

    @Test
    public void shouldRunMandelbrot() throws IOException, URISyntaxException {
        String output = compileAndLaunch("/mandelbrot.rock");
        String expected = getFileContents("/expected-mandelbrot.output");
        assertEquals(expected, output);
    }

    @Test
    public void shouldRunBoringMandelbrot() throws IOException, URISyntaxException {
        String output = compileAndLaunch("/mandelbrot-boring.rock");
        String expected = getFileContents("/expected-mandelbrot.output");
        assertEquals(expected, output);
    }

    @Disabled("Not yet working")
    @Test
    public void shouldRunOneBillionRowChallenge() throws IOException, URISyntaxException {
        String input = getFileContents("/1brc.input");
        String output = compileAndLaunch("/1brc.rock", input);
        String expected = getFileContents("/1brc.output");
        assertEquals(expected, output);
    }

    @Test
    void shouldRunSingIt() throws IOException, URISyntaxException {
        String output = compileAndLaunch("/concept-demo-1-sing-it.rock");
        String expected = getFileContents("/expected-concept-demo-1-sing-it.output");
        assertEquals(expected, output);
    }

    @Test
    void shouldRunTheTideIsLow() throws IOException, URISyntaxException {
        String output = compileAndLaunch("/concept-demo-2-the-tide-is-low.rock").replace(',', '.');
        String expected = getFileContents("/expected-concept-demo-2-the-tide-is-low.output");
        assertEquals(expected, output);
    }

    @Test
    void shouldRunOpen() throws IOException, URISyntaxException {
        String output = compileAndLaunch("/concept-demo-3-open.rock");
        String expected = getFileContents("/expected-concept-demo-3-open.output");
        assertEquals(expected, output);
    }

    @Test
    void shouldRunCityWalls() throws IOException, URISyntaxException {
        String output = compileAndLaunch("/concept-demo-4-city-walls.rock");
        String expected = getFileContents("/expected-concept-demo-4-city-walls.output");
        assertEquals(expected, output);
    }

    @Test
    void shouldRunTrustworthyFriend() throws IOException, URISyntaxException {
        String output = compileAndLaunch("/concept-demo-5-trustworthy-friend.rock");
        String expected = getFileContents("/expected-concept-demo-5-trustworthy-friend.output");
        assertEquals(expected, output);
    }

    private String getFileContents(String name) throws URISyntaxException, IOException {
        Path filePath = Path.of(this.getClass()
                .getResource(name)
                .toURI());

        return Files.readString(filePath);
    }

    private String[] getFileContentsAsArray(String name) throws URISyntaxException, IOException {
        Path filePath = Path.of(this.getClass()
                .getResource(name)
                .toURI());

        return Files.readAllLines(filePath).toArray(new String[0]);
    }
}
