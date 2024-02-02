package io.quarkiverse.bonjova.lyrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class LyricsFileUtil {
    private Path lyricsDir;
    private static final String DOT_TXT = ".txt";

    LyricsFileUtil() {
        this(Path.of("lyrics/target/lyrics"));
    }

    LyricsFileUtil(Path lyricsDir) {
        this.lyricsDir = lyricsDir;
    }

    public Optional<String> readLyricsFromFile(String songInKebabCase) {
        createDirIfAbsent(lyricsDir);

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

    public void writeLyricsToFileIfAbsent(String songInKebabCase, String lyrics) {
        createDirIfAbsent(lyricsDir);
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

    // This synchronized method prevents other threads from creating the directory in-between method execution.
    public static synchronized void createDirIfAbsent(Path dir) {
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
