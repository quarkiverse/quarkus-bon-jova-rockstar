package org.example.bon.jova.quarkus.extension.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalClassLoaderResourcesBuildItem;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import org.example.RockFileCompiler;
import org.example.bon.jova.quarkus.extension.deployment.rockscore.RockScoreCalculator;
import org.example.bon.jova.quarkus.extension.runtime.RockstarResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.example.RockFileCompiler.DOT_CLASS;
import static org.example.RockFileCompiler.DOT_ROCK;

class BonJovaQuarkusExtensionProcessor {

    private static final String FEATURE = "bon-jova-quarkus-extension";
    private static final int MINUTES = 60 * 1000;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Because there's no maven support for compiling .rock files, and writing a maven plugin seems like overkill,
     * the Quarkus app has responsibility for the initial compile of the .rock files.
     */
    @BuildStep
    void eagerlyCompileRockFiles(BuildProducer<AdditionalIndexedClassesBuildItem> producer,
                                 CurateOutcomeBuildItem curateOutcomeBuildItem,
                                 List<AdditionalClassLoaderResourcesBuildItem> additionalResources) {
        ApplicationModel model = curateOutcomeBuildItem.getApplicationModel();

        if (model.getAppArtifact()
                .getWorkspaceModule() != null) {

            Collection<SourceDir> sourceDirs = model.getAppArtifact()
                    .getSources()
                    .getSourceDirs();


            String[] allCompiledFiles = sourceDirs.stream()
                    .flatMap(this::compileEverythingInDir)
                    .collect(Collectors.toList())
                    .toArray(String[]::new);

            // Try and add it to the additional classes to index
            // This seems like a reasonable idea, but it fails noisily with null streams, and causes an infinite loop; not doing it has
            // no ill effects
            // producer.produce(new AdditionalIndexedClassesBuildItem(allCompiledFiles));
        }
    }

    // Returns a stream of file names of the compiled output
    private Stream<String> compileEverythingInDir(SourceDir sourceDir) {
        final RockFileCompiler compiler = new RockFileCompiler();

        try {
            return Files.list(sourceDir.getDir())

                    .filter(file -> file.toString()
                            .endsWith(DOT_ROCK))
                    .map(java.nio.file.Path::toFile)
                    .map(file -> compile(sourceDir.getOutputDir(),
                            file, compiler))
                    .map(file -> file.getAbsolutePath());

            // compiler for the filter, instead
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private File compile(Path buildDir, File file, RockFileCompiler compiler) {
        String outFilename = file.getName()
                .replace(DOT_ROCK, DOT_CLASS);
        File outFile = new File(buildDir.toFile(), outFilename);

        // Check if the file is up to date before compiling, or the compile will trigger a restart, which triggers a compile, which
        // triggers a restart, which ... well, you get the idea.
        if (!isUpToDate(outFile, file)) {

            try (FileInputStream stream = new FileInputStream(file)) {
                compiler.compile(stream, outFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return outFile;
    }


    private boolean isUpToDate(File outFile, File sourceFile) {
        // Check the file exists, and also that it's not significantly older than the source file
        return outFile.exists() && outFile.lastModified() > (sourceFile.lastModified() - 2 * MINUTES);
    }

    @BuildStep
    void addRockstarResource(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(RockstarResource.class));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem pages() {
        var cardPageBuildItem = new CardPageBuildItem();
        List<RockFileViewModel> rockFiles = generateRockFiles();
        cardPageBuildItem.addBuildTimeData("rockFiles", rockFiles);
        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .icon("font-awesome-regular:address-book")
                .componentLink("qwc-bon-jova-rockstar-endpoints.js")
                .title("Rockstar Endpoints")
                .staticLabel(String.valueOf(rockFiles.size())));

        return cardPageBuildItem;
    }

    private List<RockFileViewModel> generateRockFiles() {
        var rockScoreCalculator = setupRockScoreCalculator();

        try (var stream = Files.list(Path.of("src/main/rockstar"))) {
            return stream.map(path -> {
                var fileName = path.getFileName().toString();
                String contents;
                try {
                    contents = String.join(System.lineSeparator(), Files.readAllLines(path));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return new RockFileViewModel(fileName, contents, generateRestUrl(fileName),
                        rockScoreCalculator.calculateRockScore(contents));
            }).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private RockScoreCalculator setupRockScoreCalculator() {
        var properties = new Properties();
        try (var inputStream = BonJovaQuarkusExtensionProcessor.class.getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var extensionBasePath = properties.getProperty("extensionBasePath");
        return new RockScoreCalculator(Path.of(extensionBasePath, "deployment/src/main/resources/rockscore/lyrics"));
    }

    private String generateRestUrl(String rockFileName) {
        return "/rockstar/" + rockFileName;
    }

    record RockFileViewModel(String name, String contents, String restUrl, int rockScore) {
    }
}