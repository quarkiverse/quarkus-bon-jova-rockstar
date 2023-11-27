package org.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Hello world!
 */
public class Compiler {
    public static void main(String[] args) {
        System.out.println("Hello World!");
    }

    public void compile(InputStream stream, File outFile) throws IOException {

        byte[] bytes = new BytecodeGenerator().generateBytecode();
        Files.write(outFile.toPath(), bytes);
    }
}
