package org.example.bon.jova.quarkus.extension.deployment.lyrics;

import java.util.List;
import java.util.Optional;

public class LyricsReader {
    private LyricsReader() {}

    public static Optional<String> readSingle(Song song) {
        return RemoteLyricsReader.readRemoteLyrics(song, false);
    }

    public static List<String> readAll() {
        return readAll(false);
    }

    static List<String> readAll(boolean logDebugOutput) {
        return RemoteLyricsReader.readRemoteLyrics(logDebugOutput);
    }
}
