package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import java.nio.file.Path;

public class RockScoreCalculator {
    private final LyricsRatingCalculator lyricsRatingCalculator;
    private Path lyricsDir = Path.of("../bon-jova-quarkus-extension/deployment/src/main/resources/rockscore/lyrics");

    public RockScoreCalculator(Path lyricsDir) {
        this.lyricsDir = lyricsDir;
        this.lyricsRatingCalculator = new LyricsRatingCalculator(WordCounter.countWords(this.lyricsDir));
    }

    public int calculateRockScore(String rockstarProgram) {
        final var lyricsRating = lyricsRatingCalculator.calculateLyricsRating(rockstarProgram);
        final var maxLyricsRatingOfThe80s = lyricsRatingCalculator.calculateMaxLyricsRating(lyricsDir);

        return Integer.min((int) (lyricsRating / (double) maxLyricsRatingOfThe80s.lyricsRating() * 100), 100);
    }
}
