package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class LyricsFileUtil {
    private static final Path lyricsDir = Path.of("target/lyrics");
    private static final String DOT_TXT = ".txt";

    public static Optional<String> readLyricsFromFile(String songInKebabCase) {
        createDirIfAbsent();

        Path lyricsFilePath = lyricsDir.resolve(songInKebabCase + DOT_TXT);
        if (Files.exists(lyricsFilePath)) {
            try {
                return Optional.of(String.join(System.lineSeparator(), Files.readAllLines(lyricsFilePath)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return Optional.empty();
    }

    public static void writeLyricsToFileIfAbsent(String songInKebabCase, String lyrics) {
        createDirIfAbsent();
        Path lyricsFilePath = lyricsDir.resolve(songInKebabCase + DOT_TXT);
        if (Files.exists(lyricsFilePath)) {
            return;
        }

        try {
            Files.writeString(lyricsFilePath, lyrics);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Synchronize this method, to prevent another thread creating the directory halfway in this method.
    private static synchronized void createDirIfAbsent() {
        if (!Files.exists(lyricsDir)) {
            try {
                Files.createDirectory(lyricsDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
