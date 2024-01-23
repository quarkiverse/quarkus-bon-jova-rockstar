package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import java.nio.file.Path;

public class RockScoreCalculator {
    private LyricsRatingCalculator lyricsRatingCalculator;
    private static final Path LYRICS_DIR = Path.of("../bon-jova-quarkus-extension/deployment/src/main/resources/rockscore/lyrics");

    public RockScoreCalculator() {
        this.lyricsRatingCalculator = new LyricsRatingCalculator(WordCounter.countWords(LYRICS_DIR));
    }

    public int calculateRockScore(String rockstarProgram) {
        final var lyricsRating = lyricsRatingCalculator.calculateLyricsRating(rockstarProgram);
        final var maxLyricsRatingOfThe80s = lyricsRatingCalculator.calculateMaxLyricsRating(LYRICS_DIR);

        return Integer.min((int) (lyricsRating / (double) maxLyricsRatingOfThe80s.lyricsRating() * 100), 100);
    }
}
