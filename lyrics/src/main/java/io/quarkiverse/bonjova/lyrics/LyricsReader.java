package io.quarkiverse.bonjova.lyrics;

import java.util.List;
import java.util.Optional;

public class LyricsReader {
    private LyricsReader() {
    }

    public static Optional<String> readSingle(Song song) {
        return RemoteLyricsReader.readRemoteLyrics(song, false);
    }

    public static List<String> readAll() {
        return readAll(new LyricsFileUtil(), false);
    }

    static List<String> readAll(LyricsFileUtil lyricsFileUtil, boolean logDebugOutput) {
        return RemoteLyricsReader
                .withLyricsFileUtil(lyricsFileUtil)
                .readRemoteLyrics(logDebugOutput);
    }
}
