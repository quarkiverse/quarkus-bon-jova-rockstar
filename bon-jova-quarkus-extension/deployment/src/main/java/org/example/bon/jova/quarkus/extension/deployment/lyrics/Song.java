package org.example.bon.jova.quarkus.extension.deployment.lyrics;

import java.util.Objects;
import java.util.Optional;

public final class Song {
    private final String title;
    private final String artist;
    private Optional<String> lyrics;

    public Song(String title, String artist) {
        this.title = title;
        this.artist = artist;
        this.lyrics = Optional.empty();
    }

    public Song(String title, String artist, String lyrics) {
        this.title = title;
        this.artist = artist;
        this.lyrics = Optional.of(lyrics);
    }

    public String toKebabCase() {
        return String.format("%s-%s", artist.toLowerCase().replaceAll("[.|/|'|\\s|(|)|’|&]", "-"), title.toLowerCase().replaceAll("[.|/|'|\\s]", "-"));
    }

    public String title() {
        return title;
    }

    public String artist() {
        return artist;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Song) obj;
        return Objects.equals(this.title, that.title) &&
                Objects.equals(this.artist, that.artist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, artist);
    }

    public void setLyrics(Optional<String> lyrics) {
        this.lyrics = lyrics;
    }

    public Optional<String> lyrics() {
        return lyrics;
    }
}