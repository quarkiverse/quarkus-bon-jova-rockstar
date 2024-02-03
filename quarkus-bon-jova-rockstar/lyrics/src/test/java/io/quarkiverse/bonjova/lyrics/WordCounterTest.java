package io.quarkiverse.bonjova.lyrics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static java.util.Map.entry;

class WordCounterTest {
    @Disabled("See #90")
    @Test
    void testCountWords() {
        var songs = List.of(sweetChildOfMine(), backInBlack());
        var actualWordCounts = WordCounter.countWords(songs.stream()
                .map(Song::lyrics)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList(), true);
        Assertions.assertEquals(new TreeMap(expectedWordCounts()), new TreeMap(actualWordCounts));
    }

    @Test
    void printWordCounts() {
        var allLyrics = LyricsReader.readAll();
        var actualWordCounts = WordCounter.countWords(allLyrics)
                .entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .toList();

        System.out.println(actualWordCounts);
    }

    private Song sweetChildOfMine() {
        var sweetChildOfMine = new Song(
                "Sweet Child O' Mine",
                "Guns N' Roses");

        var lyrics = LyricsReader.readSingle(sweetChildOfMine);
        sweetChildOfMine.setLyrics(lyrics);

        return sweetChildOfMine;
    }

    private Song backInBlack() {
        var backInBlack = new Song(
                "Back In Black",
                "AC/DC");

        var lyrics = LyricsReader.readSingle(backInBlack);
        backInBlack.setLyrics(lyrics);

        return backInBlack;
    }

    private Map<String, Integer> expectedWordCounts() {
        return Map.ofEntries(
                entry("play", 1),
                entry("nobodys", 1),
                entry("been", 2),
                entry("sack", 1),
                entry("thought", 1),
                entry("about", 1),
                entry("yeah", 5),
                entry("your", 1),
                entry("gettin", 1),
                entry("theyve", 1),
                entry("when", 1),
                entry("number", 1),
                entry("bang", 1),
                entry("hooo", 1),
                entry("sight", 1),
                entry("hate", 1),
                entry("catch", 1),
                entry("they", 2),
                entry("reminds", 2),
                entry("gang", 1),
                entry("lives", 1),
                entry("pass", 1),
                entry("want", 1),
                entry("them", 1),
                entry("then", 1),
                entry("pack", 1),
                entry("push", 1),
                entry("looking", 1),
                entry("thats", 1),
                entry("love", 3),
                entry("hang", 1),
                entry("probably", 1),
                entry("another", 1),
                entry("bright", 1),
                entry("eyes", 3),
                entry("long", 2),
                entry("into", 1),
                entry("bluest", 1),
                entry("memories", 1),
                entry("where", 16),
                entry("power", 1),
                entry("place", 2),
                entry("takes", 1),
                entry("everything", 1),
                entry("mine", 7),
                entry("childhood", 1),
                entry("break", 1),
                entry("gonna", 1),
                entry("black", 7),
                entry("cadillac", 1),
                entry("hide", 1),
                entry("face", 1),
                entry("cats", 1),
                entry("blue", 1),
                entry("chorus", 1),
                entry("hanging", 1),
                entry("sweet", 9),
                entry("aiai", 1),
                entry("nine", 1),
                entry("luck", 1),
                entry("chorus:", 1),
                entry("thunder", 1),
                entry("oooh", 1),
                entry("down", 1),
                entry("pray", 1),
                entry("smile", 1),
                entry("makin", 1),
                entry("that", 2),
                entry("high", 1),
                entry("from", 1),
                entry("those", 1),
                entry("rain", 2),
                entry("beatin", 1),
                entry("look", 2),
                entry("never", 1),
                entry("forget", 1),
                entry("warm", 1),
                entry("loose", 1),
                entry("safe", 1),
                entry("know", 1),
                entry("seems", 1),
                entry("dont", 1),
                entry("child", 7),
                entry("away", 1),
                entry("abusin", 1),
                entry("woah", 6),
                entry("cause", 4),
                entry("back", 35),
                entry("flack", 1),
                entry("running", 1),
                entry("hair", 1),
                entry("stare", 1),
                entry("track", 1),
                entry("bullet", 1),
                entry("just", 2),
                entry("every", 1),
                entry("noose", 1),
                entry("pain", 1),
                entry("kept", 1),
                entry("special", 1),
                entry("with", 2),
                entry("hearse", 1),
                entry("shes", 2),
                entry("glad", 1),
                entry("well", 8),
                entry("ounce", 1),
                entry("skies", 1),
                entry("fresh", 1),
                entry("wild", 1),
                entry("quietly", 1));
    }
}