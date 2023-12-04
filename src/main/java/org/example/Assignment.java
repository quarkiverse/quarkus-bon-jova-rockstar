package org.example;

import rock.Rockstar;

import java.util.stream.Collectors;

public class Assignment {
    final String originalName;
    Object value = null;

    public Assignment(Rockstar.AssignmentStmtContext ctx) {
        originalName = ctx.variable()
                          .getText()
                          .toLowerCase();

        if (ctx.literal() != null) {
            // TODO this could be a string or an int
            // We will worry about floats later

            value = Integer.parseInt(ctx.literal()
                                        .NUMERIC_LITERAL()
                                        .getText());

        } else if (ctx.poeticNumberLiteral() != null) {
            value = Integer.parseInt(ctx.poeticNumberLiteral()
                                        .poeticNumberLiteralWord()
                                        .stream()
                                        .map(word -> String.valueOf(word.getText()
                                                                        .length()))
                                        .collect(Collectors.joining()));


        }
    }

    public String getVariableName() {
        return originalName;
    }

    public String getNormalisedVariableName() {
        return originalName
                .replace(" ", "_")
                .toLowerCase();
    }

    public Object getValue() {
        return value;
    }
}
