package org.example;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for simple Compiler.
 */
public class CompilerTest {

    private static final String DOT_CLASS = ".class";
    private static final String JAVA_HOME = "java.home";
    private static final String ROCK_EXTENSION = "rock";

    @Test
    public void shouldCreateAFile() throws IOException {
        InputStream stream = this.getClass()
                                 .getResourceAsStream("/hello-world.rock");
        Compiler compiler = new Compiler();
        File outFile = File.createTempFile(ROCK_EXTENSION, DOT_CLASS);
        compiler.compile(stream, outFile);
        assertTrue(Files.exists(outFile.toPath()));
    }

    @Test
    public void shouldCreateARecognizedJavaClassFile() throws IOException {
        InputStream stream = this.getClass()
                                 .getResourceAsStream("/hello-world.rock");
        Compiler compiler = new Compiler();
        File outFile = File.createTempFile(ROCK_EXTENSION, DOT_CLASS);
        compiler.compile(stream, outFile);
        InputStream is = new FileInputStream(outFile);
        byte[] magicNumber = new byte[4];
        int read = is.read(magicNumber);
        is.close();

        assertEquals(4, read, "The class file did not have enough content to be recognised as a Java file");
        assertArrayEquals(new byte[]{(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe}, magicNumber);
    }

    /**
     * Test that what the compiler generates can execute without error (even if it doesn't do anything)
     */
    @Test
    public void shouldCreateAValidJavaFile() throws IOException {
        String output = compileAndLaunch("/hello-world.rock");
        // If we got this far, we're happy!
        assertNotNull(output);
    }

    @Test
    public void shouldCompileHelloWorld() throws IOException {
        String output = compileAndLaunch("/hello-world.rock");

        assertEquals("Hello World\n", output);
    }

    /*
     * This is the starting example on https://codewithrockstar.com/online
     * It exercises variables, poetic number literals, and console output
     */
    @Test
    public void shouldCompileTommyIsABigBadMonster() throws IOException {
        String output = compileAndLaunch("/leet-tommy.rock");
        String leet = "1337\n";

        assertEquals(leet, output);
    }

    private String compileAndLaunch(String filename) throws IOException {
        InputStream stream = this.getClass()
                                 .getResourceAsStream(filename);
        Compiler compiler = new Compiler();
        File outFile = File.createTempFile(ROCK_EXTENSION, DOT_CLASS);
        try {
            compiler.compile(stream, outFile);
            return launch(outFile);
        } catch (Throwable e) {
            fail("Problem with file " + outFile + ": " + e);
            return null;
        }
    }

    public static File getJreExecutable() throws FileNotFoundException {
        String jreDirectory = System.getProperty(JAVA_HOME);
        if (jreDirectory == null) {
            throw new IllegalStateException(JAVA_HOME);
        }
        File executable;

        executable = new File(jreDirectory, "bin/java");
        if (!executable.isFile()) {
            throw new FileNotFoundException(executable.toString());
        }
        return executable;
    }

    public static String launch(File file) throws IOException,
            InterruptedException {
        List<String> arguments = new ArrayList<>();
        arguments.add(getJreExecutable().toString());
        arguments.add(getBasename(file));

        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
        File workingDir = file.getParentFile();
        processBuilder.directory(workingDir);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        InputStream in = process.getInputStream();
        int code = process.waitFor();
        String output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, code, "Execution did not complete successfully. Output was:\n" + output);

        return output;
    }

    private static String getBasename(File file) {
        return file.getName()
                   .replace(DOT_CLASS, "");
    }

}
