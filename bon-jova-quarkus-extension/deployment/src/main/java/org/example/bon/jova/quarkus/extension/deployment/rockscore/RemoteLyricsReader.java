package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import com.jagrosh.jlyrics.Lyrics;
import com.jagrosh.jlyrics.LyricsClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

import static io.restassured.RestAssured.get;
import static io.restassured.path.json.JsonPath.with;

public class RemoteLyricsReader {
    private static final List<Song> songsToFetch = List.of(
        new Song("Sweet Child O' Mine", "Guns n' Roses"),
        new Song("Back in Black", "AC/DC"),
        new Song("Livin' on a Prayer", "Bon Jovi"),
        new Song("Every Breath You Take", "The Police"),
        new Song("With or Without You", "U2"),
        new Song("Eye of the Tiger", "Survivor"),
        new Song("Jump", "Van Halen"),
        new Song("Pour Some Sugar on Me", "Def Leppard"),
        new Song("Breaking the Law", "Judas Priest"),
        new Song("Is This Love", "Whitesnake"),
        new Song("Rock You Like a Hurricane", "Scorpions"),
        new Song("Don’t Stop Believin’", "Journey"),
        new Song("I Wanna Know What Love Is", "Foreigner"),
        new Song("Another One Bites the Dust", "Queen"),
        new Song("Summer of 69", "Bryan Adams"),
        new Song("Keep on Loving You", "REO Speedwagon"),
        new Song("I Love Rock ‘n’ Roll", "Joan Jett"),
        new Song("The Final Countdown", "Europe"),
        new Song("Money for Nothing", "Dire Straits"),
        new Song("Start Me Up", "Rolling Stones"),
        new Song("Welcome to the Jungle", "Guns N’ Roses"),
        new Song("Still Loving You", "Scorpions"),
        new Song("Here I Go Again", "Whitesnake"),
        new Song("Every Rose Has Its Thorn", "Poison"),
        new Song("Dancing in the Dark", "Bruce Springteen"),
        new Song("One", "Metallica"),
        new Song("I Still Haven’t Found What I’m Looking For", "U2"),
        new Song("You Give Love a Bad Name", "Bon Jovi"),
        new Song("Ace of Spades", "Motorhead"),
        new Song("Dude (Looks Like a Lady)", "Aerosmith"),
        new Song("Girls Girls Girls", "Motley Crue"),
        new Song("The Flame", "Cheap Trick"),
        new Song("Rebel Yell", "Billy Idol"),
        new Song("18 and Life", "Skid Row"),
        new Song("Free Fallin'", "Tom Petty"),
        new Song("Should I Stay or Should I Go", "The Clash"),
        new Song("Hallowed Be Thy Name", "Iron Maiden"),
        new Song("Barracuda", "Heart"),
        new Song("Walk of Life", "Dire Straits"),
        new Song("(I Just) Died in Your Arms Tonight", "Cutting Crew"),
        new Song("Faithfully", "Journey"),
        new Song("Poison", "Alice Cooper"),
        new Song("Living After Midnight", "Judas Priest"),
        new Song("Born in the U.S.A.", "Bruce Springteen"),
        new Song("Run to the Hills", "Iron Maiden"),
        new Song("I Remember You", "Skid Row"),
        new Song("Dr. Feelgood", "Motley Crue"),
        new Song("Need You Tonight", "Inxs"),
        new Song("We’re Not Gonna Take It", "Twisted Sister"),
        new Song("When I See You Smile", "Bad English"),
        new Song("Nothing’s Gonna Stop Us Now", "Starship"),
        new Song("You’ve Got Another Thing Comin'", "Judas Priest"),
        new Song("We Didn’t Start The Fire", "Billy Joel"),
        new Song("Master of Puppets", "Metallica"),
        new Song("Amanda", "Boston"),
        new Song("Just Like Heaven", "The Cure"),
        new Song("Love Bites", "Def Leppard"),
        new Song("Crazy Train", "Ozzy Osbourne"),
        new Song("Paradise City", "Guns N’ Roses"),
        new Song("Stop Draggin’ My Heart Around", "Tom Petty & The Heartbreakers"),
        new Song("How Soon Is Now", "The Smiths"),
        new Song("Angel", "Aerosmith"),
        new Song("Jack & Diane", "John Mellencamp"),
        new Song("Turn Up the Radio", "Autograph"),
        new Song("Shout", "Tears For Fears"),
        new Song("The Look", "Roxette"),
        new Song("Holy Diver", "Dio"),
        new Song("In the Army Now", "Status Quo"),
        new Song("Never Tear Us Apart", "INXS"),
        new Song("Headed for a Heartbreak", "Winger"),
        new Song("Janie’s Got a Gun", "Aerosmith"),
        new Song("Personal Jesus", "Depeche Mode"),
        new Song("Why Can’t This Be Love", "Van Halen"),
        new Song("Heaven In Your Eyes", "Loverboy"),
        new Song("Rock the Night", "Europe"),
        new Song("Cult of Personality", "Living Colour"),
        new Song("It’s the End of the World as We Know It (and I Feel Fine)", "R.E.M."),
        new Song("Heaven", "Bryan Adams"),
        new Song("Abracadabra", "Steve Miller Band"),
        new Song("Africa", "Toto"),
        new Song("Never Say Goodbye", "Bon Jovi"),
        new Song("You Can Do Magic", "America"),
        new Song("Another Brick in the Wall 2", "Pink Floyd"),
        new Song("Epic", "Faith No More"),
        new Song("Don’t You (Forget About Me)", "Simple Minds"),
        new Song("Balls to the Wall", "Accept"),
        new Song("You’re the Only Woman", "Ambrosia"),
        new Song("Invisible Touch", "Genesis"),
        new Song("Don’t Close Your Eyes", "Kix"),
        new Song("You Shook Me All Night Long", "AC/DC"),
        new Song("Burnin’ For You", "Blue Oyster Cult"),
        new Song("Photograph", "Def Leppard"),
        new Song("Mountain Song", "Jane’s Addiction"),
        new Song("Can’t Fight This Feeling", "REO Speedwagon"),
        new Song("Broken Wings", "Mr. Mister"),
        new Song("Under Pressure", "Queen"),
        new Song("Love Song", "Tesla"),
        new Song("Rock the Casbah", "The Clash"),
        new Song("Sister Christian", "Night Ranger"),
        new Song("Hungry Like the Wolf", "Duran Duran")
    );
    private static final List<LyricsProvider> LYRICS_PROVIDERS = List.of(
            new OvhLyricsProvider(),
            new JLyricsProvider("MusixMatch"),
            new JLyricsProvider("Genius"),
            new JLyricsProvider("LyricsFreak"),
            new JLyricsProvider("A-Z Lyrics"));
    private static final Map<String, String> cache = new HashMap<>();

    public static List<String> readRemoteLyrics() {
        return readRemoteLyrics(false);
    }

    static List<String> readRemoteLyrics(boolean logDebugOutput) {
        List<StructuredTaskScope.Subtask<Optional<String>>> allLyricsResults = new ArrayList<>();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            for (Song song : songsToFetch) {
                allLyricsResults.add(scope.fork(() -> readRemoteLyrics(song, logDebugOutput)));
            }

            try {
                scope.join().throwIfFailed();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            return allLyricsResults.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        }
    }

    private static Optional<String> readRemoteLyrics(Song song, boolean logDebugOutput) {
        // Try the cache first.
        String songInKebabCase = song.toKebabCase();
        if (cache.containsKey(songInKebabCase)) {
            if (logDebugOutput) System.out.println("Lyrics found for " + song + " in cache");
            return Optional.of(cache.get(songInKebabCase));
        }

        // Then try a pre-downloaded file.
        Optional<String> lyricsFromFile = LyricsFileUtil.readLyricsFromFile(songInKebabCase);
        if (lyricsFromFile.isPresent()) {
            if (logDebugOutput) System.out.println("Lyrics found for " + song + " in file");
            cache.putIfAbsent(songInKebabCase, lyricsFromFile.get());
            return lyricsFromFile;
        }

        // Then try the remote APIs.
        for (LyricsProvider lyricsProvider : LYRICS_PROVIDERS) {
            Optional<String> lyrics = lyricsProvider.provideLyrics(song);
            if (lyrics.isPresent()) {
                if (logDebugOutput) System.out.println("Lyrics found for " + song + " in " + lyricsProvider);
                cache.putIfAbsent(songInKebabCase, lyrics.get());
                LyricsFileUtil.writeLyricsToFileIfAbsent(songInKebabCase, lyrics.get());
                return lyrics;
            }
        }

        if (logDebugOutput) System.out.println("Lyrics not found for " + song);

        return Optional.empty();
    }

    record Song(String title, String artist) {
        public String toKebabCase() {
            return String.format("%s-%s", artist.toLowerCase().replaceAll("[.|/|'|\\s|(|)|’|&]", "-"), title.toLowerCase().replaceAll("[.|/|'|\\s]", "-"));
        }
    }

    interface LyricsProvider {
        Optional<String> provideLyrics(Song song);
    }

    static class OvhLyricsProvider implements LyricsProvider {
        @Override
        public String toString() {
            return "lyrics.ovh";
        }

        @Override
        public Optional<String> provideLyrics(Song song) {
            var url = "https://api.lyrics.ovh/v1/{artist}/{title}";
            var ignorePattern = "Paroles de la chanson(.+) par (.+)\\r\\n";

            String response = with(get(url, song.artist, song.title).asString()).get("lyrics");

            if (response == null) {
                return Optional.empty();
            }

            return Optional.of(response.replaceAll(ignorePattern, ""));
        }
    }
    static class JLyricsProvider implements LyricsProvider {
        private final String source;
        private final LyricsClient lyricsClient;

        JLyricsProvider(String source) {
            this.source = source;
            this.lyricsClient = new LyricsClient(source);
        }

        @Override
        public Optional<String> provideLyrics(Song song) {
            try {
                return Optional.ofNullable(lyricsClient.getLyrics(String.format("%s - %s", song.artist, song.title)).get())
                                .map(Lyrics::getContent);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return "JLyrics/" + source;
        }
    }
}
