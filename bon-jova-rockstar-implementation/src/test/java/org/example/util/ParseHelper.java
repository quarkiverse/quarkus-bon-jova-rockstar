package org.example.util;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import rock.Rockstar;
import rock.RockstarLexer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.fail;

public class ParseHelper {

    public static Rockstar.AssignmentStmtContext getAssignment(String program) {
        return (Rockstar.AssignmentStmtContext) getGrammarElement(program, CapturingListener::getAssignmentStatement);
    }

    public static Rockstar.PoeticNumberLiteralContext getPoeticNumberLiteral(String program) {
        return (Rockstar.PoeticNumberLiteralContext) getGrammarElement(program, CapturingListener::getPoeticNumberLiteral);
    }

    private static RuleContext getGrammarElement(String program, Function<CapturingListener,
            RuleContext> getter) {
/*
Rather than mocking the syntax tree, which will be hard work and not necessarily reliable,
drive a parse to extract the thing we want.
 */
        InputStream stream = new ByteArrayInputStream(program.getBytes(StandardCharsets.UTF_8));

        try {
            CharStream input = CharStreams.fromStream(stream);

            RockstarLexer lexer = new RockstarLexer(input);


            CommonTokenStream tokens = new CommonTokenStream(lexer);
            Rockstar parser = new Rockstar(tokens);

            ParseTree tree = parser.program(); // this method is whatever we call our root rule
            CapturingListener listener = new CapturingListener();

            // Walk the tree so our listener can generate bytecode
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(listener, tree);

            RuleContext answer = getter.apply(listener);
            if (answer == null) {
                fail("There were no assignment statements. Did the program parse correctly?");
            }
            return answer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
