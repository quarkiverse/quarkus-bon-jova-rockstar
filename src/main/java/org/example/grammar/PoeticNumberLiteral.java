package org.example.grammar;

import rock.Rockstar;

import java.util.stream.Collectors;

public class PoeticNumberLiteral {
    private final Class<?> variableClass;
    
    final Number value;

    public PoeticNumberLiteral(Rockstar.PoeticNumberLiteralContext ctx) {
        // A poetic number literal begins with a variable name, followed by the keyword is , or the aliases are , was or were .
        // As long as the next symbol is not a Literal Word, the rest of the line is treated as a decimal number in which the
        // values of consecutive digits are given by the lengths of the subsequent barewords, up until the end of the line.
        // To allow the digit zero, and to compensate for a lack of suitably rock'n'roll 1- and 2-letter words, word lengths are
        // parsed modulo 10.
        String string = ctx
                .poeticNumberLiteralWord()
                .stream()
                .map(word -> String.valueOf(Math.floorMod(word.getText()
                                                              .length(), 10)))
                .collect(Collectors.joining());

        // This is a complex way of handling the decimal point; the antlr listener mechanism may be simpler
        if (ctx.poeticNumberLiteralDecimalSeparator() != null && ctx.poeticNumberLiteralDecimalSeparator()
                                                                    .size() > 0) {
            // The spec says to only look at the first decimal, which makes things easier
            Rockstar.PoeticNumberLiteralDecimalSeparatorContext dot = ctx.poeticNumberLiteralDecimalSeparator(0);
            int baseIndex = ctx.getStart()
                               .getTokenIndex();
            int numberOfTokensIncludingWhitespaceTokens = dot.getStart()
                                                             .getTokenIndex() - baseIndex;
            // Fragile code alert! Assume every token is whitespace or a word-digit
            int index = (int) (Math.ceil(numberOfTokensIncludingWhitespaceTokens / 2.0));

            // Insert the dot at the right point
            string = string.substring(0, index) + "." + string.substring(index);
            variableClass = double.class;
            value = Double.parseDouble(string);
        } else {
            variableClass = int.class;
            value = Integer.parseInt(string);
        }


    }

    public Class<?> getVariableClass() {
        return variableClass;
    }

    public Number getValue() {
        return value;
    }
}
