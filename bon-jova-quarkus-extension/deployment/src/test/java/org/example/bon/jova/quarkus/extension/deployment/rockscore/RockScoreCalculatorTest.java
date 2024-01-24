package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RockScoreCalculatorTest {
    @Test
    void testCalculateRockScore() throws IOException {
        var expectedRockScore = 92;

        var rockScoreCalculator = new RockScoreCalculator(Path.of("src/main/resources/rockscore/lyrics"));
        var actualRockScore = rockScoreCalculator.calculateRockScore(String.join(System.lineSeparator(), Files.readAllLines(Path.of("src/test/resources/hello_hanno_hello_holly.rock"))));

        assertEquals(expectedRockScore, actualRockScore);
    }
}