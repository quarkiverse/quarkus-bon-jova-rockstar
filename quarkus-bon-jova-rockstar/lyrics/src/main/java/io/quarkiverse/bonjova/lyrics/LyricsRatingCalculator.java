package io.quarkiverse.bonjova.lyrics;

import io.quarkiverse.bonjova.lyrics.generate.LyricsDataReader;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LyricsRatingCalculator {
    private Map<String, Integer> wordCounts;

    // cached variable that stores deterministic values
    private static Integer maxLyricsRatingOfThe80s;

    public LyricsRatingCalculator() {
        this.wordCounts = LyricsDataReader.readWordCounts();
    }

    public LyricsRatingCalculator(Map<String, Integer> wordCounts) {
        this.wordCounts = wordCounts;
    }

    public int calculateLyricsRating(String lyrics) {
        int lyricsRating = 0;
        String[] words = Arrays.stream(lyrics.split(WordCounter.WORD_SPLITTER))
                .map(LyricsSanitizer.noUppercaseLetters())
                .map(LyricsSanitizer.noForbiddenCharacters())
                .filter(LyricsSanitizer.nonEmptyWords())
                .filter(LyricsSanitizer.wordsLongerThanLimit())
                .toArray(String[]::new);

        for (String word : words) {
            if (wordCounts.containsKey(word)) {
                lyricsRating += wordCounts.get(word);
            }
        }

        return lyricsRating / words.length;
    }

    public Integer calculateMaxLyricsRating(List<String> allLyrics) {
        if (maxLyricsRatingOfThe80s == null) {
            maxLyricsRatingOfThe80s = allLyrics.stream()
                    .map(this::calculateLyricsRating)
                    .max(Integer::compare)
                    .orElse(-1);
        }

        return maxLyricsRatingOfThe80s;
    }
}
