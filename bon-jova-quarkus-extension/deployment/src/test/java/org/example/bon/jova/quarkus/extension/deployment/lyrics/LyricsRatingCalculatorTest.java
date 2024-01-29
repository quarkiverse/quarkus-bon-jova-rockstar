package org.example.bon.jova.quarkus.extension.deployment.lyrics;

import org.example.bon.jova.quarkus.extension.deployment.wordcounter.WordCounter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LyricsRatingCalculatorTest {
    private static List<String> allLyrics;
    private static Map<String, Integer> wordCounts;

    @BeforeAll
    static void setUp() {
        allLyrics = LyricsReader.readAll();
        wordCounts = WordCounter.countWords(allLyrics);
    }

    @Test
    void testCalculateLyricsRating() throws IOException {
        var expectedLyricsRating = 91;

        var lyricsRatingCalculator = new LyricsRatingCalculator(wordCounts);
        var actualLyricsRating = lyricsRatingCalculator.calculateLyricsRating(String.join(System.lineSeparator(),
                        Files.readAllLines(Path.of("src/test/resources/hello_hanno_hello_holly.rock"))));

        // We assert using a range, because lyrics are downloaded from multiple sources, causing small differences in
        // the lyrics rating over multiple test runs.
        assertWithinRange(expectedLyricsRating, actualLyricsRating, 10);
    }

    @Test
    void testCalculateMaxLyricsRating() {
        var expectedSongLyricsRating = 83;

        var lyricsRatingCalculator = new LyricsRatingCalculator(wordCounts);
        var actualMaxLyricsRating = lyricsRatingCalculator.calculateMaxLyricsRating(allLyrics);

        // We assert using a range, because lyrics are downloaded from multiple sources, causing small differences in
        // the lyrics rating over multiple test runs.
        assertWithinRange(expectedSongLyricsRating, actualMaxLyricsRating, 10);
    }

    private static void assertWithinRange(int expected, int actual, int range) {
        assertTrue(expected >= actual - range && expected <= actual + range);
    }
}