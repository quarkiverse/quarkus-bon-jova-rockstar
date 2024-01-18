package org.example.grammar;

import rock.Rockstar;

import java.util.stream.Collectors;

public class PoeticNumberLiteral {
    final double value;
    private final Class<?> variableClass;

    public PoeticNumberLiteral(Rockstar.PoeticNumberLiteralContext ctx) {
        // A poetic number literal begins with a variable name, followed by the keyword is , or the aliases are , was or were .
        // As long as the next symbol is not a Literal Word, the rest of the line is treated as a decimal number in which the
        // values of consecutive digits are given by the lengths of the subsequent barewords, up until the end of the line.
        // To allow the digit zero, and to compensate for a lack of suitably rock'n'roll 1- and 2-letter words, word lengths are
        // parsed modulo 10.
        String string = ctx
                .poeticNumberLiteralWord()
                .stream()
                .map(word -> wordToNumber(word))
                .collect(Collectors.joining());

        // This is a complex way of handling the decimal point; the antlr listener mechanism may be simpler
        if (ctx.poeticNumberLiteralDecimalSeparator() != null && ctx.poeticNumberLiteralDecimalSeparator()
                .size() > 0) {

            Rockstar.PoeticNumberLiteralDecimalSeparatorContext dot = ctx.poeticNumberLiteralDecimalSeparator(0);

            // The spec says to only look at the first decimal, which makes things easier

            // We want to find the position of the dot among the children (not the tokens)
            // Do it by brute force
            int index = 0;
            for (int i = 0; i < ctx.getChildCount(); i++) {
                var child = ctx.getChild(i);
                if (child == dot) {
                    break;
                } else if (child instanceof Rockstar.PoeticNumberLiteralWordContext) {
                    // Only count words, not garbage or whitespace
                    index++;
                }
            }

            // Insert the dot at the right point
            string = string.substring(0, index) + "." + string.substring(index);
            variableClass = double.class;
            value = Double.parseDouble(string);
        } else {
            variableClass = double.class;
            // We can parse as an int, and then store as a double
            value = Integer.parseInt(string);
        }


    }

    private static String wordToNumber(Rockstar.PoeticNumberLiteralWordContext word) {
        // Ignore apostrophes; because they can be in the middle of the word this is hard to do in the grammar
        int length = word.getText()
                .replaceAll("'", "")
                .length();
        return String.valueOf(Math.floorMod(length, 10));
    }

    public Class<?> getVariableClass() {
        return variableClass;
    }

    public Number getValue() {
        return value;
    }
}
