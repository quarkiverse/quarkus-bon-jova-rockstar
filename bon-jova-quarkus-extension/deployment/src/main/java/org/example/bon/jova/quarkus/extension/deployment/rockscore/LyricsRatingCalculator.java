package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

class LyricsRatingCalculator {
    private Map<String, Integer> wordCounts;

    // cached variable that store deterministic values
    private static Map<Path, SongLyricsRating> maxLyricsRatingOfThe80s = new HashMap<>();

    LyricsRatingCalculator(Map<String, Integer> wordCounts) {
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

    public SongLyricsRating calculateMaxLyricsRating(Path lyricsDir) {
        if (!maxLyricsRatingOfThe80s.containsKey(lyricsDir)) {
            System.out.println("Calculating max lyrics rating for " + lyricsDir.toAbsolutePath());
            try (Stream<Path> paths = Files.walk(lyricsDir)) {
                maxLyricsRatingOfThe80s.put(lyricsDir, paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".txt"))
                        .map(this::toSongLyricsRating)
                        .max(Comparator.comparingInt(SongLyricsRating::lyricsRating))
                        .orElse(new SongLyricsRating("No song found", 0)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return maxLyricsRatingOfThe80s.get(lyricsDir);
    }

    private SongLyricsRating toSongLyricsRating(Path path) {
        try {
            return new SongLyricsRating(path.getFileName().toString(),
                    calculateLyricsRating(String.join(System.lineSeparator(), Files.readAllLines(path))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
