package org.example.bon.jova.quarkus.extension.deployment.lyrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LyricsReaderTest {

    @Test
    void readLyricsShouldReturn100Songs() {
        List<String> allLyrics = LyricsReader.readAll(true);
        assertEquals(100, allLyrics.size());
    }

    @Test
    void readLyricsTwiceShouldUseTheCache() {
        List<String> allLyrics = LyricsReader.readAll(true);
        List<String> allLyrics2 = LyricsReader.readAll(true);
        assertEquals(100, allLyrics.size());
        assertEquals(100, allLyrics2.size());
    }
}