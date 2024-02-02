package io.quarkiverse.bonjova.quarkus.extension.deployment.rockscore;

import io.quarkiverse.bonjova.deployment.rockscore.RockScoreCalculator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RockScoreCalculatorTest {
    @Test
    void testCalculateRockScore() throws IOException {
        var expectedRockScore = 100;

        var rockScoreCalculator = new RockScoreCalculator();
        var actualRockScore = rockScoreCalculator.calculateRockScore(
                String.join(System.lineSeparator(),
                        Files.readAllLines(Path.of("src/test/resources/hello_hanno_hello_holly.rock"))));

        assertEquals(expectedRockScore, actualRockScore);
    }
}