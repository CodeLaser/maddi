package org.e2immu.analyzer.run.openjdkmain;

import org.e2immu.analyzer.aapi.parser.AnalysisHintsConfigurationImpl;
import org.e2immu.analyzer.run.config.Configuration;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manual generator (use case 2 of the AnalysisHints configuration): compiles the curated
 * {@code libs.support} AAPI package (the e2immu support library: Either, SetOnce, EventuallyFinal,
 * EventuallyFinalOnDemand) into analyzed-annotated-API json under the archive's
 * {@code analyzedPackageFiles/libs/support}, where the dogfood preloads it. Run explicitly after
 * editing {@code OrgE2immuSupport.java}; disabled by default because it writes into main resources.
 */
public class GenerateSupportAnalysisResults {

    @Disabled("manual: regenerates analyzedPackageFiles/libs/support from the curated hints")
    @Test
    public void generate() throws Exception {
        Path target = Path.of("../maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/libs/support");
        Files.createDirectories(target);
        SourceSet aapiSource = new SourceSetImpl.Builder()
                .setName("archive")
                .setSourceDirectories(List.of(Path.of("../maddi-aapi-archive/src/main/java")))
                .setUri(URI.create("file:./"))
                .build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .setWorkingDirectory(".")
                .addSourceSets(aapiSource)
                .addClassPathParts(SourceSetImpl.javaBase(),
                        SourceSetImpl.sourceSetOf(org.e2immu.annotation.Container.class),
                        SourceSetImpl.sourceSetOf(org.e2immu.support.Either.class),
                        SourceSetImpl.sourceSetOf(org.slf4j.Logger.class))
                .build();
        AnalysisHintsConfigurationImpl aapi = (AnalysisHintsConfigurationImpl) new AnalysisHintsConfigurationImpl.Builder()
                .setAnalysisResultsTargetDir(target.toString())
                .addHintsPackages("org.e2immu.analyzer.aapi.archive.libs.support")
                .build();
        Configuration configuration = new Configuration.Builder()
                .setInputConfiguration(inputConfiguration)
                .setAnalysisHintsConfiguration(aapi)
                .build();

        RunAnalyzer runAnalyzer = new RunAnalyzer(configuration);
        runAnalyzer.run();
        assertEquals(0, runAnalyzer.exitValue());

        try (var walk = Files.walk(target)) {
            assertTrue(walk.anyMatch(p -> p.getFileName().toString().endsWith(".json")),
                    "expected an AAPI .json in " + target);
        }
    }
}
