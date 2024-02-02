package io.quarkiverse.bonjova.lyrics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

class LyricsReaderTest {
    private static final LyricsFileUtil lyricsFileUtil = new LyricsFileUtil(Path.of("target/lyrics"));

    @Test
    void readLyricsShouldReturn100Songs() {
        List<String> allLyrics = LyricsReader.readAll(lyricsFileUtil, false);
        Assertions.assertEquals(100, allLyrics.size());
    }

    @Test
    void readLyricsTwiceShouldUseTheCache() {
        List<String> allLyrics = LyricsReader.readAll(lyricsFileUtil, false);
        List<String> allLyrics2 = LyricsReader.readAll(lyricsFileUtil, false);
        Assertions.assertEquals(100, allLyrics.size());
        Assertions.assertEquals(100, allLyrics2.size());
    }
}