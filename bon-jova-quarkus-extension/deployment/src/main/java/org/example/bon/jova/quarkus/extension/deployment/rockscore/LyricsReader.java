package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import java.util.List;

public class LyricsReader {
    public List<String> read() {
        return RemoteLyricsReader.readRemoteLyrics();
    }
}
