package org.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class RockFileCompiler {
    public static final String DOT_CLASS = ".class";
    public static final String DOT_ROCK = ".rock";

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }

    public void compile(InputStream stream, File outFile) throws IOException {
        ClassFileWriter cl = new ClassFileWriter(outFile);
        new BytecodeGenerator().generateBytecode(stream, getBasename(outFile), cl);
    }

    private static String getBasename(File file) {
        return file.getName()
                   .replace(DOT_CLASS, "");
    }
}
