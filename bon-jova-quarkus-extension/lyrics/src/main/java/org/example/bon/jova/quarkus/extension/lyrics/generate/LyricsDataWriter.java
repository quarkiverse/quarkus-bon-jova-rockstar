package org.example.bon.jova.quarkus.extension.lyrics.generate;

import org.example.bon.jova.quarkus.extension.lyrics.LyricsFileUtil;
import org.example.bon.jova.quarkus.extension.lyrics.LyricsRatingCalculator;
import org.example.bon.jova.quarkus.extension.lyrics.LyricsReader;
import org.example.bon.jova.quarkus.extension.lyrics.WordCounter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;

public class LyricsDataWriter {
    public static final Path WORD_COUNTS_LOCATION = Path.of("lyrics/src/main/resources/word-counts.data");
    public static final Path MAX_LYRICS_RATING_LOCATION = Path.of("lyrics/src/main/resources/max-lyrics-rating.data");

    public static void main(String[] args) {
        var allLyrics = LyricsReader.readAll();

        var wordCounts = WordCounter.countWords(allLyrics, false);
        var maxLyricsRating = new LyricsRatingCalculator(wordCounts).calculateMaxLyricsRating(allLyrics);

        try {
            writeDataFile(WORD_COUNTS_LOCATION, wordCounts);
            writeDataFile(MAX_LYRICS_RATING_LOCATION, maxLyricsRating);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeDataFile(Path destination, Object payload) throws IOException {
        LyricsFileUtil.createDirIfAbsent(destination.getParent());

        var file = destination.toFile();

        try (var outputStream = new ObjectOutputStream(new FileOutputStream(file))) {
            outputStream.writeObject(payload);
        }
    }
}
