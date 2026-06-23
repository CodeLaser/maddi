package org.e2immu.analyzer.aapi.parser;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.resource.SourceSetImpl;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public record AnalysisHints(String libraryName, Path hintsPath, String packagePrefix, Path analysisResultsDir,
                            Path analysisResultsJar) {

    public SourceSet toSourceSet(List<SourceSet> dependencies) {
        return new SourceSetImpl.Builder()
                .setName(libraryName)
                .setSourceDirectories(List.of(hintsPath))
                .setSourceEncoding(StandardCharsets.UTF_8)
                .setUri(analysisResultsDir.toUri())
                .setRestrictToPackages(packagePrefix == null ? null : Set.of(packagePrefix + "."))
                .setDependencies(dependencies)
                .build();
    }
}
