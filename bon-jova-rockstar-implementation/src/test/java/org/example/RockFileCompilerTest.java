package org.example;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static org.example.util.FileLauncher.compileAndLaunch;
import static org.example.util.FileLauncher.createTempClassFile;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for simple RockFileCompiler.
 * It tests reading in a .rock file and output a .class file.
 */
public class RockFileCompilerTest {


    @Test
    public void shouldCreateAClassFile() throws IOException {
        InputStream stream = this.getClass()
                .getResourceAsStream("/hello-world.rock");
        RockFileCompiler compiler = new RockFileCompiler();
        File outFile = createTempClassFile();
        compiler.compile(stream, outFile);
        assertTrue(Files.exists(outFile.toPath()));
    }

    @Test
    public void shouldCreateARecognizedJavaClassFile() throws IOException {
        InputStream stream = this.getClass()
                .getResourceAsStream("/hello-world.rock");
        RockFileCompiler compiler = new RockFileCompiler();
        File outFile = createTempClassFile();
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
}
