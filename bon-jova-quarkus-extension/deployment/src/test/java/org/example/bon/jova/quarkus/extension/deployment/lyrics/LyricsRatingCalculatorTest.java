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

        assertEquals(expectedLyricsRating, actualLyricsRating);
    }

    @Test
    void testCalculateMaxLyricsRating() {
        var expectedSongLyricsRating = 83;

        var lyricsRatingCalculator = new LyricsRatingCalculator(wordCounts);
        var actualMaxLyricsRating = lyricsRatingCalculator.calculateMaxLyricsRating(allLyrics);

        assertEquals(expectedSongLyricsRating, actualMaxLyricsRating);
    }
}