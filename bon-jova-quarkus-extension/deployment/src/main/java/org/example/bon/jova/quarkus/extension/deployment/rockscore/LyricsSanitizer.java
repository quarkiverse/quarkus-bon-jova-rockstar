package org.example.bon.jova.quarkus.extension.deployment.rockscore;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class LyricsSanitizer {
    private static final int MIN_WORD_LENGTH = 4;
    private static final List<Character> CHARS_TO_REMOVE = List.of('.', ',', '!', '?', '(', ')', '"', '\'', '.', '[', ']', ',');

    private LyricsSanitizer() {}

    static UnaryOperator<String> noUppercaseLetters() {
        return String::toLowerCase;
    }

    static UnaryOperator<String> noForbiddenCharacters() {
        return word -> {
            for (Character character : CHARS_TO_REMOVE) {
                word = word.replace(character.toString(), "");
            }
            return word;
        };
    }

    static Predicate<String> nonEmptyWords() {
        return word -> !word.isEmpty();
    }

    static Predicate<String> wordsLongerThanLimit() {
        return word -> word.length() >= MIN_WORD_LENGTH;
    }
}
