package io.quarkiverse.bonjova.compiler.util;

import io.quarkiverse.bonjova.compiler.RockFileCompiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.quarkiverse.bonjova.compiler.RockFileCompiler.DOT_ROCK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class FileLauncher {

    public static final String ROCK_PREFIX = "rock";

    public static final String DOT_CLASS = ".class";
    private static final String JAVA_HOME = "java.home";

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

    public static File createTempClassFile(String filename) throws IOException {
        return File.createTempFile(filename.replace(DOT_ROCK, ""), DOT_CLASS);
    }

    public static File createTempClassFile() throws IOException {
        return createTempClassFile(ROCK_PREFIX);
    }

    public static String launch(File file, String... args) throws IOException,
            InterruptedException {
        List<String> arguments = new ArrayList<>();
        arguments.add(getJreExecutable().toString());
        arguments.add(getBasename(file));

        if (args != null) {
            arguments.addAll(Arrays.stream(args).toList());
        }

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

    public static String compileAndLaunch(String filename, String... args) throws IOException {
        InputStream stream = FileLauncher.class
                .getResourceAsStream(filename);
        RockFileCompiler compiler = new RockFileCompiler();
        File outFile = createTempClassFile(filename);
        try {
            compiler.compile(stream, outFile);
            return FileLauncher.launch(outFile, args);
        } catch (Throwable e) {
            fail(e);
            return null;
        }
    }
}