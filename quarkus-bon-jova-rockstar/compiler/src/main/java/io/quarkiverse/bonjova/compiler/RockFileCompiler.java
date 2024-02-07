package io.quarkiverse.bonjova.compiler;

import io.quarkiverse.bonjova.support.RockstarArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;

public class RockFileCompiler {
    public static final String DOT_CLASS = ".class";
    public static final String DOT_ROCK = ".rock";

    public static void main(String[] args) throws IOException {
        String fileName = args[0];
        if (fileName == null) {
            System.out.println("Usage: first argument should be the path to a .rock file");
        }

        try (InputStream stream = new FileInputStream(fileName)) {
            File outFile = new File(fileName.replace(DOT_ROCK, DOT_CLASS));
            new RockFileCompiler().compile(stream, outFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getBasename(File file) {
        return file.getName()
                .replace(DOT_CLASS, "");
    }

    public void compile(InputStream stream, File outFile) throws IOException {
        // Copy across supporting classes
        // TODO check if it exists first rather than always copying
        Class c = RockstarArray.class;
        String className = c.getName();
        String classAsPath = className.replace('.', '/') + ".class";
        try (InputStream arrayStream = c.getClassLoader().getResourceAsStream(classAsPath)) {

            File targetFile = new File(outFile.getParentFile(), classAsPath);
            targetFile.mkdirs();
            java.nio.file.Files.copy(
                    arrayStream,
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        ClassFileWriter cl = new ClassFileWriter(outFile);
        new BytecodeGenerator().generateBytecode(stream, getBasename(outFile), cl);
    }
}
