package org.example.grammar;

import rock.Rockstar;

import java.util.stream.Collectors;

public class PoeticNumberLiteral {
    private final Class<?> variableClass;


    Number value = 0;

    public PoeticNumberLiteral(Rockstar.PoeticNumberLiteralContext ctx) {
        // A poetic number literal begins with a variable name, followed by the keyword is , or the aliases are , was or were .
        // As long as the next symbol is not a Literal Word, the rest of the line is treated as a decimal number in which the
        // values of consecutive digits are given by the lengths of the subsequent barewords, up until the end of the line.
        // To allow the digit zero, and to compensate for a lack of suitably rock'n'roll 1- and 2-letter words, word lengths are
        // parsed modulo 10.
        value = Integer.parseInt(ctx
                .poeticNumberLiteralWord()
                .stream()
                .map(word -> String.valueOf(Math.floorMod(word.getText()
                                                              .length(), 10)))
                .collect(Collectors.joining()));

        variableClass = int.class;
    }

    public Class<?> getVariableClass() {
        return variableClass;
    }

    public Number getValue() {
        return value;
    }
}
