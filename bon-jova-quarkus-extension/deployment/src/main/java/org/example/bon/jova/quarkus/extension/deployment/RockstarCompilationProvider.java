package org.example.bon.jova.quarkus.extension.deployment;

import io.quarkus.deployment.dev.CompilationProvider;
import io.quarkus.paths.PathCollection;
import org.apache.commons.io.FilenameUtils;
import org.example.RockFileCompiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import static org.example.RockFileCompiler.DOT_CLASS;
import static org.example.RockFileCompiler.DOT_ROCK;

public class RockstarCompilationProvider implements CompilationProvider {

    @Override
    public Set<String> handledExtensions() {
        return Collections.singleton(DOT_ROCK);
    }

    @Override
    public String getProviderKey() {
        return "rockstar";
    }

    @Override
    public void compile(Set<File> files, Context context) {

        // Ensure the directory we're going to write into exists
        context.getOutputDirectory()
                .mkdirs();

        final var compiler = new RockFileCompiler();

        for (var file : files) {
            final var outFile = new File(context.getOutputDirectory()
                    .getPath(),
                    FilenameUtils.getBaseName(file.getName()) + DOT_CLASS);

            try {
                compiler.compile(new FileInputStream(file), outFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Path getSourcePath(Path classFilePath, PathCollection sourcePaths, String classesPath) {
        return classFilePath;
    }
}
