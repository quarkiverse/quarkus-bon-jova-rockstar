package io.quarkiverse.bonjova.compiler.integration;

import io.quarkiverse.bonjova.compiler.util.FileLauncher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntegrationTests {
    @ParameterizedTest(name = "{1}")
    @MethodSource("provideTestData")
    void runningRockstarProgramShouldYieldExpectedOutput(String inputFile, String rockstarFile,
            String expectedOutputFile) throws IOException, URISyntaxException {
        String output;

        if (inputFile != null) {
            output = FileLauncher.compileAndLaunch(rockstarFile, getFileContents(inputFile));
        } else {
            output = FileLauncher.compileAndLaunch(rockstarFile);
        }

        String expected = getFileContents(expectedOutputFile);
        assertEquals(expected, output);
    }

    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.of(null, "/99-bottles.rock", "/expected-99-bottles.output"),
                Arguments.of(null, "/fizzbuzz.rock", "/expected-fizzbuzz.output"),
                // not yet working: Arguments.of("/factorization.rock", "178"),
                Arguments.of(null, "/mandelbrot.rock", "/expected-mandelbrot.output"),
                Arguments.of(null, "/mandelbrot-boring.rock", "/expected-mandelbrot.output"),
                // not yet working: Arguments.of("/1brc.input", "/1brc.rock", "/1brc.output"),
                Arguments.of(null, "/concept-demo-1-sing-it.rock", "/expected-concept-demo-1-sing-it.output"),
                Arguments.of(null, "/concept-demo-1-boring.rock", "/expected-concept-demo-1-sing-it.output"),
                Arguments.of(null, "/concept-demo-2-the-tide-is-low.rock", "/expected-concept-demo-2-the-tide-is-low.output"),
                Arguments.of(null, "/concept-demo-2-boring.rock", "/expected-concept-demo-2-the-tide-is-low.output"),
                Arguments.of("/concept-demo-3-open.input", "/concept-demo-3-open.rock", "/expected-concept-demo-3-open.output"),
                Arguments.of("/concept-demo-3-open.input", "/concept-demo-3-boring.rock",
                        "/expected-concept-demo-3-open.output"),
                Arguments.of(null, "/concept-demo-4-city-walls.rock", "/expected-concept-demo-4-city-walls.output"),
                Arguments.of(null, "/concept-demo-4-boring.rock", "/expected-concept-demo-4-city-walls.output"),
                Arguments.of(null, "/concept-demo-5-trustworthy-friend.rock",
                        "/expected-concept-demo-5-trustworthy-friend.output"),
                Arguments.of(null, "/concept-demo-5-boring.rock", "/expected-concept-demo-5-trustworthy-friend.output"),
                Arguments.of(null, "/concept-demo-6-mama-taking-charge.rock",
                        "/expected-concept-demo-6-mama-taking-charge.output"),
                Arguments.of(null, "/concept-demo-6-boring.rock", "/expected-concept-demo-6-mama-taking-charge.output"));
    }

    private String getFileContents(String name) throws URISyntaxException, IOException {
        Path filePath = Path.of(this.getClass().getResource(name).toURI());

        return Files.readString(filePath);
    }
}
