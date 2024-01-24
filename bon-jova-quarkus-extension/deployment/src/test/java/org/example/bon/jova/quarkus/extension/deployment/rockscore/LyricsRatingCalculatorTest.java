package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LyricsRatingCalculatorTest {
    public static final Path LYRICS_DIR = Path.of("src/main/resources/rockscore/lyrics");
    private static Map<String, Integer> wordCounts;

    @BeforeAll
    static void setUp() {
        wordCounts = WordCounter.countWords(LYRICS_DIR);
    }

    @Test
    void testCalculateLyricsRating() throws IOException {
        var expectedLyricsRating = 101;

        var lyricsRatingCalculator = new LyricsRatingCalculator(wordCounts);
        var actualLyricsRating = lyricsRatingCalculator.calculateLyricsRating(String.join(System.lineSeparator(),
                        Files.readAllLines(Path.of("src/test/resources/hello_hanno_hello_holly.rock"))));

        assertEquals(expectedLyricsRating, actualLyricsRating);
    }

    @Test
    void testCalculateMaxLyricsRating() {
        var expectedSongLyricsRating = new SongLyricsRating("097-love-song.txt", 109);

        var lyricsRatingCalculator = new LyricsRatingCalculator(wordCounts);
        var actualMaxLyricsRating = lyricsRatingCalculator.calculateMaxLyricsRating(LYRICS_DIR);

        assertEquals(expectedSongLyricsRating, actualMaxLyricsRating);
    }
}