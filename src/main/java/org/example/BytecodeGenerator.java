package org.example;

import io.quarkus.gizmo.ClassCreator;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import rock.Rockstar;
import rock.RockstarLexer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class BytecodeGenerator {

    public void generateBytecode(InputStream stream, String name, File outFile) throws IOException {
        ClassWriter cl = new ClassWriter(outFile);
        try (ClassCreator creator = ClassCreator.builder()
                                                .classOutput(cl)
                                                .className(name)
                                                .build()) {


            CharStream input = CharStreams.fromStream(stream);
            RockstarLexer lexer = new RockstarLexer(input);


            CommonTokenStream tokens = new CommonTokenStream(lexer);
            Rockstar parser = new Rockstar(tokens);

            ParseTree tree = parser.program(); // this method is whatever we call our root rule
            BytecodeGeneratingListener listener = new BytecodeGeneratingListener(creator);

            // Walk the tree so our listener can generate bytecode
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(listener, tree);

        }
    }
}