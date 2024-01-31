package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import org.example.bon.jova.quarkus.extension.deployment.lyrics.LyricsRatingCalculator;
import org.example.bon.jova.quarkus.extension.deployment.lyrics.Song;
import org.example.bon.jova.quarkus.extension.deployment.wordcounter.WordCounter;

import java.util.List;

public class RockScoreCalculator {
    private final LyricsRatingCalculator lyricsRatingCalculator;
    private List<String> allLyrics;

    public RockScoreCalculator(List<String> allLyrics) {
        this.allLyrics = allLyrics;
        this.lyricsRatingCalculator = new LyricsRatingCalculator(WordCounter.countWords(this.allLyrics));
    }

    public int calculateRockScore(String rockstarProgram) {
        if (rockstarProgram.isBlank()) {
            return 0;
        }

        final var lyricsRating = lyricsRatingCalculator.calculateLyricsRating(rockstarProgram);
        final var maxLyricsRatingOfThe80s = lyricsRatingCalculator.calculateMaxLyricsRating(allLyrics);

        return Integer.min((int) (lyricsRating / (double) maxLyricsRatingOfThe80s * 100), 100);
    }
}
