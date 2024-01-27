package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import com.jagrosh.jlyrics.LyricsClient;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

public class LyricsClientTest {
    @Test
    void testGetLyrics() throws ExecutionException, InterruptedException {
        var lyrics = new LyricsClient().getLyrics("Livin' on a Prayer - Bon Jovi").get();

        System.out.println(lyrics.getAuthor());
        System.out.println(lyrics.getTitle());
        System.out.println(lyrics.getContent());
    }
}
