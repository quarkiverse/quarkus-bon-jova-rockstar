package org.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Compiler {
    private static final String DOT_CLASS = ".class";

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }

    public void compile(InputStream stream, File outFile) throws IOException {
        new BytecodeGenerator().generateBytecode(stream, getBasename(outFile), outFile);
    }

    private static String getBasename(File file) {
        return file.getName()
                   .replace(DOT_CLASS, "");
    }
}
