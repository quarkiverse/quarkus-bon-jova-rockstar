package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

class WordCounter {
    private static Map<String, Integer> wordCounts;
    static final String WORD_SPLITTER = "\\s|-|%s".formatted(System.lineSeparator()); // split by space, dash or newline

    public static Map<String, Integer> countWords(Path inputFilesDirectory) {
        return countWords(inputFilesDirectory, false);
    }

    static Map<String, Integer> countWords(Path inputFilesDirectory, boolean logResults) {
        if (wordCounts == null) {
            wordCounts = new HashMap<>();

            try (Stream<Path> paths = Files.walk(inputFilesDirectory)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".txt"))
                        .forEach(WordCounter::countWordsInFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

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

    private static void countWordsInFile(Path path) {
        try {
            Files.readAllLines(path)
                    .stream()
                    .flatMap(line -> Stream.of(line.split(WORD_SPLITTER)))
                    .map(LyricsSanitizer.noUppercaseLetters())
                    .map(LyricsSanitizer.noForbiddenCharacters())
                    .filter(LyricsSanitizer.nonEmptyWords())
                    .filter(LyricsSanitizer.wordsLongerThanLimit())
                    .forEach(mergeIntoWordCounts());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Consumer<String> mergeIntoWordCounts() {
        return word -> wordCounts.merge(word, 1, Integer::sum);
    }
}
