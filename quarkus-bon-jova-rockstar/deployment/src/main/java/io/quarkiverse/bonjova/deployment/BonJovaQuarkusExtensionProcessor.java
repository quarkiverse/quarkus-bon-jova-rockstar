package io.quarkiverse.bonjova.deployment;

import io.quarkiverse.bonjova.compiler.RockFileCompiler;
import io.quarkiverse.bonjova.deployment.rockscore.RockScoreCalculator;
import io.quarkiverse.bonjova.runtime.RockstarResource;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.quarkiverse.bonjova.compiler.RockFileCompiler.DOT_ROCK;

class BonJovaQuarkusExtensionProcessor {

    private static final String FEATURE = "quarkus-bon-jova";
    private static final int MINUTES = 60 * 1000;
    public static final String DEFAULT_RESOURCES_PATH = "src/main/rockstar";

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

            Collection<SourceDir> sourceDirs = getSourceDirs(model);

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

    private static Collection<SourceDir> getSourceDirs(ApplicationModel model) {
        Collection<SourceDir> sourceDirs = model.getAppArtifact()
                .getSources()
                .getSourceDirs();

        // Also always look in src/main/rockstar
        Path resourcesPath = Path.of(DEFAULT_RESOURCES_PATH);
        if (Files.exists(resourcesPath)) {
            // Make a new, mutable, collection
            sourceDirs = new ArrayList<>(sourceDirs);
            sourceDirs.add(SourceDir.of(resourcesPath, Path.of("target/classes")));
        }
        return sourceDirs;
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
                .replace(DOT_ROCK, RockFileCompiler.DOT_CLASS);
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
    public CardPageBuildItem pages(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        var cardPageBuildItem = new CardPageBuildItem();
        List<RockFileViewModel> rockFiles = generateRockFiles(curateOutcomeBuildItem.getApplicationModel());
        cardPageBuildItem.addBuildTimeData("rockFiles", rockFiles);
        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .icon("font-awesome-regular:address-book")
                .componentLink("qwc-bon-jova-rockstar-endpoints.js")
                .title("Rockstar Endpoints")
                .staticLabel(String.valueOf(rockFiles.size())));

        return cardPageBuildItem;
    }

    private List<RockFileViewModel> generateRockFiles(ApplicationModel model) {
        var rockScoreCalculator = new RockScoreCalculator();

        Collection<SourceDir> sourcePaths = getSourceDirs(model);
        return sourcePaths.stream().map(SourceDir::getDir).flatMap(resourcesPath -> {
            try {
                return getViewModelsForDir(resourcesPath, rockScoreCalculator);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    private Stream<RockFileViewModel> getViewModelsForDir(Path resourcesPath, RockScoreCalculator rockScoreCalculator)
            throws IOException {
        Stream<Path> stream = Files.list(resourcesPath);

        return stream.map(path -> {
            if (!path.toFile().isDirectory() && path.toFile().getName().endsWith(DOT_ROCK)) {
                var fileName = path.getFileName().toString();
                String contents;
                try {
                    contents = String.join(System.lineSeparator(), Files.readAllLines(path));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return new RockFileViewModel(fileName, contents, generateRestUrl(fileName),
                        rockScoreCalculator.calculateRockScore(contents));
            } else {
                return null;
            }
        }).filter(Objects::nonNull);
    }

    private String generateRestUrl(String rockFileName) {
        return "/rockstar/" + rockFileName;
    }

    record RockFileViewModel(String name, String contents, String restUrl, int rockScore) {
    }
}