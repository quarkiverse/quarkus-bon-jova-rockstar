package org.example.bon.jova.quarkus.extension.deployment.wordcounter;

import org.example.bon.jova.quarkus.extension.deployment.lyrics.LyricsSanitizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class WordCounter {
    private static Map<String, Integer> wordCounts;
    public static final String WORD_SPLITTER = "\\s|-|%s".formatted(System.lineSeparator()); // split by space, dash or newline

    private WordCounter() {
    }

    public static Map<String, Integer> countWords(List<String> lyrics) {
        return countWords(lyrics, false);
    }

    static Map<String, Integer> countWords(List<String> lyrics, boolean logResults) {
        // Skip cache for small lyrics sets, as this is probably a test configuration.
        if (wordCounts == null || lyrics.size() < wordCounts.size()) {
            wordCounts = new HashMap<>();

            lyrics.forEach(WordCounter::countWordsInSong);

            if (logResults) {
                wordCounts.entrySet()
                        .stream()
                        .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(50)
                        .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));
            }
        }
        return wordCounts;
    }

    private static void countWordsInSong(String lyrics) {
        Stream.of(lyrics.split(WORD_SPLITTER))
                .map(LyricsSanitizer.noUppercaseLetters())
                .map(LyricsSanitizer.noForbiddenCharacters())
                .filter(LyricsSanitizer.nonEmptyWords())
                .filter(LyricsSanitizer.wordsLongerThanLimit())
                .forEach(mergeIntoWordCounts());
    }

    private static Consumer<String> mergeIntoWordCounts() {
        return word -> wordCounts.merge(word, 1, Integer::sum);
    }
}
