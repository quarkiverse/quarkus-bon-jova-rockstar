package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LyricsReaderTest {

    @Test
    void readLyricsShouldReturn100Songs() {
        List<String> lyrics = new LyricsReader().read();
        assertEquals(100, lyrics.size());
    }
}