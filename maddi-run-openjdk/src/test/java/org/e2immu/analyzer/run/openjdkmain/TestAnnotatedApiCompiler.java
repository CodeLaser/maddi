package org.e2immu.analyzer.run.openjdkmain;

import org.e2immu.analyzer.aapi.parser.AnnotatedAPIConfigurationImpl;
import org.e2immu.analyzer.run.config.Configuration;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAnnotatedApiCompiler {

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
                        SourceSetImpl.sourceSetOf(org.e2immu.annotation.Container.class),
                        SourceSetImpl.sourceSetOf(org.slf4j.Logger.class))
                .build();
        AnnotatedAPIConfigurationImpl aapi = (AnnotatedAPIConfigurationImpl) new AnnotatedAPIConfigurationImpl.Builder()
                .setAnalyzedAnnotatedApiTargetDir(resultsDir.toString())  // use case 2
                .setAnnotatedApiTargetDir(hintsDir.toString())            // use case 3
                .addAnnotatedApiPackages("org.e2immu.analyzer.aapi.archive.libs.log")
                .build();
        Configuration configuration = new Configuration.Builder()
                .setInputConfiguration(inputConfiguration)
                .setAnnotatedAPIConfiguration(aapi)
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
