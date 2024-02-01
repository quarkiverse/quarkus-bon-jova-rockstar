package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import org.example.bon.jova.quarkus.extension.lyrics.LyricsReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RockScoreCalculatorTest {
    @Test
    void testCalculateRockScore() throws IOException {
        var expectedRockScore = 100;
        var allLyrics = LyricsReader.readAll();

        var rockScoreCalculator = new RockScoreCalculator(allLyrics);
        var actualRockScore = rockScoreCalculator.calculateRockScore(String.join(System.lineSeparator(), Files.readAllLines(Path.of("src/test/resources/hello_hanno_hello_holly.rock"))));

        assertEquals(expectedRockScore, actualRockScore);
    }
}