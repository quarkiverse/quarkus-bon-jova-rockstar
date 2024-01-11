package org.example.bon.jova.quarkus.extension.deployment;

import io.quarkus.deployment.dev.CompilationProvider;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RockstarCompilationProviderTest {

    @Test
    void handledExtensionsShouldSpecifyRockExtension() throws IOException {
        var expectedHandledExtensions = Set.of(".rock");
        try (var compilationProvider = new RockstarCompilationProvider()) {
            assertEquals(expectedHandledExtensions, compilationProvider.handledExtensions());
        }
    }

    @Test
    void getProviderKeyShouldReturnTheRightProvider() throws IOException {
        var expectedProviderKey = "rockstar";

        try (var compilationProvider = new RockstarCompilationProvider()) {
            assertEquals(expectedProviderKey, compilationProvider.getProviderKey());
        }
    }

    @Test
    void compileShouldProduceClassFiles() throws IOException {
        var files = Set.of(
                new File("src/test/resources/hello-world.rock"),
                new File("src/test/resources/leet-tommy.rock"));
        try (var compilationProvider = new RockstarCompilationProvider()) {
            compilationProvider.compile(files, createContext());
        }

        File helloWorld = new File("src/test/resources/classes/hello-world.class");
        File tommyLeet = new File("src/test/resources/classes/leet-tommy.class");

        assertTrue(helloWorld.exists());
        assertTrue(tommyLeet.exists());
    }

    @Test
    void getSourcePathShouldReturnClassFilePath() throws IOException {
        var classFilePath = Path.of(".");
        try (var compilationProvider = new RockstarCompilationProvider()) {
            var sourcePath = compilationProvider.getSourcePath(Path.of("."), null, null);
            assertEquals(classFilePath, sourcePath);
        }
    }

    private CompilationProvider.Context createContext() {
        var outputDirectory = new File("src/test/resources/classes");

        return new CompilationProvider.Context("RockstarCompilationProviderTest",
                null,
                null,
                null,
                null,
                outputDirectory,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @AfterAll
    static void cleanUp() throws IOException {
        var classesDir = new File("src/test/resources/classes");
        if (classesDir.exists()) {
            FileUtils.cleanDirectory(classesDir);
        }
    }
}