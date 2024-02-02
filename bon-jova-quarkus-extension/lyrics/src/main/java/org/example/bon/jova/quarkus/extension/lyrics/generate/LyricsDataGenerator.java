package org.example.bon.jova.quarkus.extension.lyrics.generate;

import org.example.bon.jova.quarkus.extension.lyrics.LyricsRatingCalculator;
import org.example.bon.jova.quarkus.extension.lyrics.LyricsReader;
import org.example.bon.jova.quarkus.extension.lyrics.WordCounter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;

public class LyricsDataGenerator {
    public static void main(String[] args) {
        var allLyrics = LyricsReader.readAll();

        var wordCounts = WordCounter.countWords(allLyrics, true);
        var maxLyricsRating = new LyricsRatingCalculator(wordCounts).calculateMaxLyricsRating(allLyrics);

        try {
            writeFile(Path.of("lyrics/src/main/resources/word-counts.data"), wordCounts);
            writeFile(Path.of("lyrics/src/main/resources/max-lyrics-rating.data"), maxLyricsRating);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeFile(Path destination, Object payload) throws IOException {
        var file = destination.toFile();

        try (var outputStream = new ObjectOutputStream(new FileOutputStream(file))) {
            outputStream.writeObject(payload);
        }
    }
}
