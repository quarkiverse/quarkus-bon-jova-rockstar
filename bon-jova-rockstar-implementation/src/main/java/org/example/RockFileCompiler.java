package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class RockFileCompiler {
    public static final String DOT_CLASS = ".class";
    public static final String DOT_ROCK = ".rock";

    public static void main(String[] args) throws FileNotFoundException {
        String fileName = args[0];
        if (fileName == null) {
            System.out.println("Usage: first argument should be the path to a .rock file");
        }
        try (InputStream stream = new FileInputStream(fileName)) {
            File outFile = new File(fileName.replace(DOT_ROCK, DOT_CLASS));
            System.out.println("Compiling " + fileName + " to " + outFile);
            new RockFileCompiler().compile(stream, outFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
