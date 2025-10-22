package io.quarkiverse.bonjova.deployment.rockscore;

import java.util.Random;

public class RockScoreCalculator {
    //    private final LyricsRatingCalculator lyricsRatingCalculator;

    public RockScoreCalculator() {
        //        this.lyricsRatingCalculator = new LyricsRatingCalculator();
    }

    public int calculateRockScore(String rockstarProgram) {
        if (rockstarProgram.isBlank()) {
            return 0;
        }

        //        final var lyricsRating = lyricsRatingCalculator.calculateLyricsRating(rockstarProgram);
        //        final var maxLyricsRatingOfThe80s = LyricsDataReader.readMaxLyricsRating();

        return new Random().nextInt(100);
        //        return Integer.min((int) (lyricsRating / (double) maxLyricsRatingOfThe80s * 100), 100);
    }
}
