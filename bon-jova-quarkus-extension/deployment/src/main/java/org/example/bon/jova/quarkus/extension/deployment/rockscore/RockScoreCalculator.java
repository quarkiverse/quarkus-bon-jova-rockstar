package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import org.example.bon.jova.quarkus.extension.lyrics.LyricsRatingCalculator;
import org.example.bon.jova.quarkus.extension.lyrics.generate.LyricsDataReader;

public class RockScoreCalculator {
    private final LyricsRatingCalculator lyricsRatingCalculator;

    public RockScoreCalculator() {
        this.lyricsRatingCalculator = new LyricsRatingCalculator();
    }

    public int calculateRockScore(String rockstarProgram) {
        if (rockstarProgram.isBlank()) {
            return 0;
        }

        final var lyricsRating = lyricsRatingCalculator.calculateLyricsRating(rockstarProgram);
        final var maxLyricsRatingOfThe80s = LyricsDataReader.readMaxLyricsRating();

        return Integer.min((int) (lyricsRating / (double) maxLyricsRatingOfThe80s * 100), 100);
    }
}
