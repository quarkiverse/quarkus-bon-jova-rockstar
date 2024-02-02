package io.quarkiverse.bonjova.lyrics.generate;

import java.io.ObjectInputStream;
import java.util.Map;

public class LyricsDataReader {
    public static Integer readMaxLyricsRating() {
        return (Integer) readDataFile("max-lyrics-rating.data");
    }

    public static Map<String, Integer> readWordCounts() {
        return (Map<String, Integer>) readDataFile("word-counts.data");
    }

    private static Object readDataFile(String fileName) {
        try (var inputStream = new ObjectInputStream(LyricsDataReader.class.getClassLoader().getResourceAsStream(fileName))) {
            return inputStream.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
