package io.quarkiverse.bonjova.quarkus.extension.deployment;

import io.quarkiverse.bonjova.deployment.RockstarCompilationProvider;
import io.quarkus.deployment.dev.CompilationProvider;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RockstarCompilationProviderTest {
    private static final String sourceDirectory = "src/test/resources/";
    private static final String outputDirectory = "target/test-classes/";
    private static final Set<File> sourceFiles = Set.of(
            new File(sourceDirectory + "hello_world.rock"),
            new File(sourceDirectory + "hello_hanno_hello_holly.rock"),
            // This isn't needed for this test, but the BonJovaQuarkusExtensionProcessorTest needs it to be compiled, and getting rid of the
            // cross-dependency is non-trivial
            new File(sourceDirectory + "leet_tommy.rock"));
    private static final Set<File> outputFiles = Set.of(
            new File(outputDirectory + "hello_world.class"),
            new File(outputDirectory + "hello_hanno_hello_holly.class"),
            new File(outputDirectory + "leet_tommy.class"));

    @BeforeAll
    public static void removeOutputFiles() throws IOException {
        if (!new File(outputDirectory).exists()) {
            Files.createDirectory(Path.of(outputDirectory));
        }

        outputFiles.forEach(File::delete);
    }

    private static void compile(Set<File> files, File outputDirectory) throws IOException {
        try (var compilationProvider = new RockstarCompilationProvider()) {
            compilationProvider.compile(files,
                    createContext(outputDirectory));
        }
    }

    private static CompilationProvider.Context createContext(File outputDirectory) {
        return new CompilationProvider.Context("RockstarTest",
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
                null, null, null, null);
    }

    @AfterAll
    static void cleanUp() throws IOException {
        var classesDir = new File("src/test/resources/classes");
        if (classesDir.exists()) {
            FileUtils.cleanDirectory(classesDir);
        }
    }

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
        compile(sourceFiles, new File(outputDirectory));
        outputFiles.forEach(outputFile -> assertTrue(outputFile.exists(), outputFile + " does not exist"));
    }

    @Test
    void getSourcePathShouldReturnClassFilePath() throws IOException {
        var classFilePath = Path.of(".");
        try (var compilationProvider = new RockstarCompilationProvider()) {
            var sourcePath = compilationProvider.getSourcePath(Path.of("."), null, null);
            assertEquals(classFilePath, sourcePath);
        }
    }
}