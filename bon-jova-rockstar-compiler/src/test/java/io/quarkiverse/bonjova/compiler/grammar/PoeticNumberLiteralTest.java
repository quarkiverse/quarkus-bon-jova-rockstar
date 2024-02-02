package io.quarkiverse.bonjova.compiler.grammar;

import io.quarkiverse.bonjova.compiler.util.ParseHelper;
import org.junit.jupiter.api.Test;
import rock.Rockstar;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PoeticNumberLiteralTest {


    @Test
    public void shouldParseIntegerPoeticNumberLiterals() {
        Rockstar.PoeticNumberLiteralContext ctx = new ParseHelper().getPoeticNumberLiteral("My thing is a big bad monster");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(1337d, a.getValue());
        assertEquals(double.class, a.getVariableClass());
    }

    @Test
    public void shouldParsePoeticNumberLiteralsContainingZeros() {
        Rockstar.PoeticNumberLiteralContext ctx = new ParseHelper().getPoeticNumberLiteral("Tommy was a lovestruck ladykiller");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(100d, a.getValue());
        assertEquals(double.class, a.getVariableClass());
    }

    @Test
    public void shouldIgnoreCommas() {
        Rockstar.PoeticNumberLiteralContext ctx = new ParseHelper().getPoeticNumberLiteral("My thing is a big, bad, monster");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(1337d, a.getValue());
        assertEquals(double.class, a.getVariableClass());
    }

    @Test
    public void shouldIgnoreApostrophes() {
        Rockstar.PoeticNumberLiteralContext ctx = new ParseHelper().getPoeticNumberLiteral("My thing is a smokin' gun");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(163d, a.getValue());
        assertEquals(double.class, a.getVariableClass());
    }

    // The hyphen (-) is counted as a letter – so you can use terms like ‘all-consuming’ (13 letters > 3) and ‘power-hungry’ (12 letters
    // > 2) instead of having to think of 12- and 13-letter words.
    @Test
    public void shouldCountHyphenAsALetter() {
        Rockstar.PoeticNumberLiteralContext ctx = new ParseHelper().getPoeticNumberLiteral("grace is all-consuming");
        assertEquals(3d, new PoeticNumberLiteral(ctx).getValue());

        ctx = new ParseHelper().getPoeticNumberLiteral("bob is power-hungry");
        assertEquals(2d, new PoeticNumberLiteral(ctx).getValue());
    }

    @Test
    public void shouldIgnoreApostrophesAtTheBeginningOfWords() {
        String program = "Life is 'bout love";
        Rockstar.PoeticNumberLiteralContext ctx = new ParseHelper().getPoeticNumberLiteral(program);
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(44d, a.getValue());
    }

    @Test
    public void shouldIgnoreApostrophesInTheMiddleOfWords() {
        String program = "The beers were numbering fa'too'many";
        Rockstar.PoeticNumberLiteralContext ctx = new ParseHelper().getPoeticNumberLiteral(program);
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(99d, a.getValue());
        assertEquals(double.class, a.getVariableClass());
    }

    @Test
    public void shouldIgnoreSemicolons() {
        Rockstar.PoeticNumberLiteralContext ctx = new ParseHelper().getPoeticNumberLiteral("My thing is good; too good for you");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(43433d, a.getValue());
        assertEquals(double.class, a.getVariableClass());
    }

    @Test
    public void shouldIgnorePlusSigns() {
        // The hyphen (-) is counted as a letter – so you can use terms like ‘all-consuming’ (13 letters > 3) and ‘power-hungry’ (12
        // letters > 2) instead of having to think of 12- and 13-letter words.
        //         The semi-colon, comma, apostrophe and any other non-alphabetical characters are ignored.
        Rockstar.PoeticNumberLiteralContext ctx = new ParseHelper().getPoeticNumberLiteral(
                "life is death + taxes");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(55d, a.getValue());


    }

    @Test
    public void shouldParseFloatingPointPoeticNumberLiterals() {
        Rockstar.PoeticNumberLiteralContext ctx = new ParseHelper().getPoeticNumberLiteral(
                "My dreams were ice. A life unfulfilled; wakin' everybody up, taking booze and pills");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(3.1415926535, a.getValue());
        assertEquals(double.class, a.getVariableClass());

        ctx = new ParseHelper().getPoeticNumberLiteral(
                "My dreams were ice A. life unfulfilled; wakin' everybody up, taking booze and pills");
        a = new PoeticNumberLiteral(ctx);
        assertEquals(31.415926535, a.getValue());

        ctx = new ParseHelper().getPoeticNumberLiteral(
                "My dreams were ice A life unfulfilled; wakin' everybody. up, taking booze and pills");
        a = new PoeticNumberLiteral(ctx);
        assertEquals(314159.26535, a.getValue());
    }

    @Test
    public void shouldParseFloatingPointPoeticNumberLiteralsWithHyphens() {
        Rockstar.PoeticNumberLiteralContext ctx = new ParseHelper().getPoeticNumberLiteral(
                "life is all-consuming");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(3d, a.getValue());
        assertEquals(double.class, a.getVariableClass());

        ctx = new ParseHelper().getPoeticNumberLiteral(
                "life is all-consuming. death is a sweet rest.");
        a = new PoeticNumberLiteral(ctx);
        assertEquals(3.52154, a.getValue());
    }

    @Test
    public void shouldParseFloatingPointPoeticNumberLiteralsWithIgnoredElements() {
        Rockstar.PoeticNumberLiteralContext ctx = new ParseHelper().getPoeticNumberLiteral(
                "life is all ! yeah , so what. just hang!");
        PoeticNumberLiteral a = new PoeticNumberLiteral(ctx);
        assertEquals(3424.44, a.getValue());
        assertEquals(double.class, a.getVariableClass());
    }

}
