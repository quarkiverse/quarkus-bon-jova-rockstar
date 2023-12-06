package org.example;

import org.antlr.v4.runtime.tree.TerminalNode;
import rock.Rockstar;

import java.util.stream.Collectors;

public class Assignment {
    private final String originalName;
    private Object value = null;
    private Class<?> variableClass;


    public Assignment(Rockstar.AssignmentStmtContext ctx) {
        originalName = ctx.variable()
                          .getText()
                          .toLowerCase();


        Rockstar.LiteralContext literal = ctx.expression() != null ? ctx.expression()
                                                                        .literal() : ctx.literal();
        Rockstar.ConstantContext constant = ctx.expression() != null ? ctx.expression()
                                                                          .constant() : ctx.constant();

        if (literal != null) {
            if (literal
                    .NUMERIC_LITERAL() != null) {

                TerminalNode num = literal
                        .NUMERIC_LITERAL();
                double parsed = Double.parseDouble(num.getText());
                if (Math.round(parsed) == parsed) {
                    value = Integer.parseInt(num
                            .getText());
                    variableClass = int.class;
                } else {
                    value = parsed;
                    variableClass = double.class;
                }
            } else if (literal
                    .STRING_LITERAL() != null) {
                value = literal
                        .STRING_LITERAL()
                        .getText()
                        .replaceAll("\"", "");
                // Strip out the quotes around literals (doing it in the listener rather than the lexer is simpler, and apparently
                // idiomatic-ish)
                variableClass = String.class;
            }

        } else {
            if (constant != null) {
                if (constant
                        .CONSTANT_TRUE() != null) {
                    value = true;
                    variableClass = boolean.class;
                } else if (constant
                        .CONSTANT_FALSE() != null) {
                    value = false;
                    variableClass = boolean.class;
                } else if (constant
                        .CONSTANT_EMPTY() != null) {
                    value = "";
                    variableClass = String.class;
                }

            } else if (ctx.poeticStringLiteral() != null) {
                value = ctx.poeticStringLiteral()
                           .getText();
                variableClass = String.class;

            } else if (ctx.poeticNumberLiteral() != null) {
                // A poetic number literal begins with a variable name, followed by the keyword is , or the aliases are , was or were .
                // As long as the next symbol is not a Literal Word, the rest of the line is treated as a decimal number in which the
                // values of consecutive digits are given by the lengths of the subsequent barewords, up until the end of the line.
                // To allow the digit zero, and to compensate for a lack of suitably rock'n'roll 1- and 2-letter words, word lengths are
                // parsed modulo 10.
                value = Integer.parseInt(ctx.poeticNumberLiteral()
                                            .poeticNumberLiteralWord()
                                            .stream()
                                            .map(word -> String.valueOf(Math.floorMod(word.getText()
                                                                                          .length(), 10)))
                                            .collect(Collectors.joining()));

                variableClass = int.class;
            } else {
                variableClass = Object.class;
            }
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

    public Class<?> getVariableClass() {
        return variableClass;
    }
}
