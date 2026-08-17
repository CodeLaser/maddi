package io.codelaser.maddi.run.openjdkmain;

import io.codelaser.maddi.aapi.parser.AnalysisHintsConfigurationImpl;
import io.codelaser.maddi.run.config.Configuration;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAnalysisHintsCompilation {

    // use case 2: compile an AAPI package into analyzed-annotated-API (.json) results;
    // use case 3: also write updated AAPI hint (.java) files.
    @Test
    public void useCases2And3(@TempDir Path resultsDir, @TempDir Path hintsDir) throws Exception {
        SourceSet aapiSource = new SourceSetImpl.Builder()
                .setName("archive")
                .setSourceDirectories(List.of(Path.of("../maddi-aapi-archive/src/main/java")))
                .setUri(URI.create("file:./"))
                .build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .setWorkingDirectory(".")
                .addSourceSets(aapiSource)
                .addClassPathParts(SourceSetImpl.javaBase(),
                        SourceSetImpl.sourceSetOf(io.codelaser.maddi.annotation.Container.class),
                        SourceSetImpl.sourceSetOf(org.slf4j.Logger.class))
                .build();
        AnalysisHintsConfigurationImpl aapi = (AnalysisHintsConfigurationImpl) new AnalysisHintsConfigurationImpl.Builder()
                .setAnalysisResultsTargetDir(resultsDir.toString())  // use case 2
                .setUpdatedHintsDir(hintsDir.toString())            // use case 3
                .addHintsPackages("io.codelaser.maddi.aapi.archive.libs.log")
                .build();
        Configuration configuration = new Configuration.Builder()
                .setInputConfiguration(inputConfiguration)
                .setAnalysisHintsConfiguration(aapi)
                .build();

        RunAnalyzer runAnalyzer = new RunAnalyzer(configuration);
        runAnalyzer.run();
        assertEquals(0, runAnalyzer.exitValue());

        try (var walk = Files.walk(resultsDir)) {
            assertTrue(walk.anyMatch(p -> p.getFileName().toString().endsWith(".json")),
                    "expected an AAAPI .json in " + resultsDir);
        }
        try (var walk = Files.walk(hintsDir)) {
            assertTrue(walk.anyMatch(p -> p.getFileName().toString().endsWith(".java")),
                    "expected an updated AAPI hints .java in " + hintsDir);
        }
    }
}
