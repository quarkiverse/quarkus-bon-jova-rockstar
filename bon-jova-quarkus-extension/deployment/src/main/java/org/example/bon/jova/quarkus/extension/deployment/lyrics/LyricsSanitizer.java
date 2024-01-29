package org.example.bon.jova.quarkus.extension.deployment.lyrics;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class LyricsSanitizer {
    private static final int MIN_WORD_LENGTH = 4;
    private static final List<Character> CHARS_TO_REMOVE = List.of('.', ',', '!', '?', '(', ')', '"', '\'', '.', '[', ']', ',');

    private LyricsSanitizer() {}

    public static UnaryOperator<String> noUppercaseLetters() {
        return String::toLowerCase;
    }

    public static UnaryOperator<String> noForbiddenCharacters() {
        return word -> {
            for (Character character : CHARS_TO_REMOVE) {
                word = word.replace(character.toString(), "");
            }
            return word;
        };
    }

    public static Predicate<String> nonEmptyWords() {
        return word -> !word.isEmpty();
    }

    public static Predicate<String> wordsLongerThanLimit() {
        return word -> word.length() >= MIN_WORD_LENGTH;
    }
}
