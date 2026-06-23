package org.e2immu.analyzer.aapi.parser;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.resource.SourceSetImpl;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @param libraryName                the name (for logging purposes only)
 * @param hintsPath                  where to find the hints files
 * @param packagePrefix              filter in the hintsPath
 * @param preloadAnalysisResultsDirs preload dirs, can be resource:xxx on the classpath
 * @param analysisResultsDir         where to write the .json files
 * @param updatedHintsPath           if non-null, write an updated hints file
 */
public record AnalysisHints(String libraryName,
                            Path hintsPath,
                            String packagePrefix,
                            List<String> preloadAnalysisResultsDirs,
                            Path analysisResultsDir,
                            Path updatedHintsPath) {

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

    public static class Builder {
        String libraryName;
        Path hintsPath;
        String packagePrefix;
        List<String> preloadAnalysisResultsDirs;
        Path analysisResultsDir;
        Path updatedHintsPath;

        public Builder setAnalysisResultsDir(Path analysisResultsDir) {
            this.analysisResultsDir = analysisResultsDir;
            return this;
        }

        public Builder setHintsPath(Path hintsPath) {
            this.hintsPath = hintsPath;
            return this;
        }

        public Builder setLibraryName(String libraryName) {
            this.libraryName = libraryName;
            return this;
        }

        public Builder setPackagePrefix(String packagePrefix) {
            this.packagePrefix = packagePrefix;
            return this;
        }

        public Builder setPreloadAnalysisResultsDirs(List<String> preloadAnalysisResultsDirs) {
            this.preloadAnalysisResultsDirs = preloadAnalysisResultsDirs;
            return this;
        }

        public Builder setUpdatedHintsPath(Path updatedHintsPath) {
            this.updatedHintsPath = updatedHintsPath;
            return this;
        }

        public AnalysisHints build() {
            return new AnalysisHints(libraryName, hintsPath, packagePrefix,
                    Objects.requireNonNullElse(preloadAnalysisResultsDirs, List.of()),
                    analysisResultsDir, updatedHintsPath);
        }
    }
}
