package org.example;

import java.io.File;
import java.io.InputStream;

/**
 * Hello world!
 */
public class Compiler {
    private static final String DOT_CLASS = ".class";

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }

    public void compile(InputStream stream, File outFile) {
        new BytecodeGenerator().generateBytecode(getBasename(outFile), outFile);
    }

    private static String getBasename(File file) {
        return file.getName()
                   .replace(DOT_CLASS, "");
    }
}
