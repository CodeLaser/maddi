/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.inspection.resource;

import org.e2immu.annotation.Container;
import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.resource.InputConfiguration;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record InputConfigurationImpl(Path workingDirectory,
                                     List<SourceSet> sourceSets,
                                     List<SourceSet> classPathParts,
                                     Path alternativeJREDirectory) implements InputConfiguration {

    public static final String MAVEN_MAIN = "src/main/java";
    public static final String MAVEN_TEST = "src/test/java";

    public static final String[] DEFAULT_MODULES = {
            "jmod:java.base",
            "jmod:java.datatransfer",
            "jmod:java.desktop",
            "jmod:java.logging",
            "jmod:java.net.http",
            "jmod:java.sql",
            "jmod:java.xml",
    };

    static final String NL_TAB = "\n    ";

    @Override
    public InputConfiguration withDefaultModules() {
        Stream<SourceSet> defaultModuleStream = Arrays.stream(DEFAULT_MODULES).map(mod ->
                new SourceSetImpl(mod, null, URI.create(mod), StandardCharsets.UTF_8, false, true,
                        true, true, false, Set.of(), Set.of()));
        return new InputConfigurationImpl(workingDirectory, sourceSets, Stream.concat(classPathParts.stream(),
                defaultModuleStream).toList(), alternativeJREDirectory);
    }

    @Override
    public InputConfiguration withE2ImmuSupportFromClasspath() {
        return withSupportFromClasspath(Map.of("maddiSupport", "org/e2immu/annotation"));
    }


    @Override
    public InputConfiguration withSupportFromClasspath(Map<String, String> sourceSetNameToPackageDir) {
        boolean allFound = true;
        for (String packageDir : sourceSetNameToPackageDir.values()) {
            if (classPathParts.stream()
                    .noneMatch(cpp -> cpp.uri().getSchemeSpecificPart().contains(packageDir))) {
                allFound = false;
                break;
            }
        }
        if (allFound) return this;
        Stream<SourceSet> support = sourceSetNameToPackageDir.entrySet().stream().map(e ->
                new SourceSetImpl(e.getKey(), null,
                        URI.create("jar-on-classpath:" + e.getValue()),
                        StandardCharsets.UTF_8, false, true,
                        true, false, false, Set.of(), Set.of()));
        return new InputConfigurationImpl(workingDirectory, sourceSets,
                Stream.concat(classPathParts.stream(), support).toList(),
                alternativeJREDirectory);
    }

    @Override
    public List<SourceSet> findMostLikelySourceSet(String name) {
        List<SourceSet> exact = Stream.concat(classPathParts.stream(), sourceSets.stream())
                .filter(sourceSet -> sourceSet.name().equals(name))
                .toList();
        if (exact.size() == 1) return exact;
        return Stream.concat(classPathParts.stream(), sourceSets.stream())
                .filter(sourceSet -> sourceSet.name().contains(name))
                .toList();
    }

    @Override
    public String toString() {
        return "InputConfiguration:" +
               NL_TAB + "sourcesSets=" + sourceSets +
               NL_TAB + "classPathParts=" + classPathParts +
               NL_TAB + "alternativeJREDirectory=" + (alternativeJREDirectory == null ? "<default>"
                : alternativeJREDirectory);
    }

    private record SourceSetNamePath(String name, String path) {
    }

    @Container
    public static class Builder implements InputConfiguration.Builder {
        private final List<SourceSet> sourceSets = new ArrayList<>();
        private final List<SourceSet> classPathParts = new ArrayList<>();
        private final List<SourceSetNamePath> sourceDirs = new ArrayList<>();
        private final List<SourceSetNamePath> testSourceDirs = new ArrayList<>();
        private final List<String> classPathStringParts = new ArrayList<>();
        private final List<String> runtimeClassPathParts = new ArrayList<>();
        private final List<String> testClassPathParts = new ArrayList<>();
        private final List<String> testRuntimeClassPathParts = new ArrayList<>();

        private final Set<String> restrictSourceToPackages = new HashSet<>();
        private final Set<String> restrictTestSourceToPackages = new HashSet<>();

        private String workingDirectory;
        private String alternativeJREDirectory;
        private String sourceEncoding;

        public InputConfiguration build() {
            Charset sourceCharset = sourceEncoding == null ? StandardCharsets.UTF_8 : Charset.forName(sourceEncoding);

            for (String cpp : classPathStringParts) {
                String cppName = removeJmod(cpp);
                classPathParts.add(new SourceSetImpl(cppName, null, createURI(cpp), null,
                        false, true, true, isJmod(cpp), false, Set.of(), Set.of()));
            }
            for (String cpp : runtimeClassPathParts) {
                String cppName = removeJmod(cpp);
                classPathParts.add(new SourceSetImpl(cppName, null, createURI(cpp), null,
                        false, true, true, isJmod(cpp), true, Set.of(), Set.of()));
            }
            for (String cpp : testClassPathParts) {
                String cppName = removeJmod(cpp);
                classPathParts.add(new SourceSetImpl(cppName, null, createURI(cpp), null,
                        true, true, true, isJmod(cpp), false, Set.of(), Set.of()));
            }
            for (String cpp : testRuntimeClassPathParts) {
                String cppName = removeJmod(cpp);
                classPathParts.add(new SourceSetImpl(cppName, null, createURI(cpp), null,
                        true, true, true, isJmod(cpp), true, Set.of(), Set.of()));
            }
            for (SourceSetNamePath sourceDir : sourceDirs) {
                Set<SourceSet> allDependencies = Stream.concat(classPathParts.stream(),
                        sourceSets.stream()).collect(Collectors.toUnmodifiableSet());
                URI uri = createURI(sourceDir.path);
                List<Path> list = uri.getScheme().equals("file") ? List.of(Path.of(sourceDir.path)) : List.of();
                sourceSets.add(new SourceSetImpl(sourceDir.name, list, uri, sourceCharset,
                        false, false, false, false, false,
                        restrictSourceToPackages, allDependencies));
            }
            for (SourceSetNamePath sourceDir : testSourceDirs) {
                Set<SourceSet> allDependencies = Stream.concat(classPathParts.stream(),
                        sourceSets.stream()).collect(Collectors.toUnmodifiableSet());
                URI uri = createURI(sourceDir.path);
                List<Path> list = uri.getScheme().equals("file") ? List.of(Path.of(sourceDir.path)) : List.of();
                sourceSets.add(new SourceSetImpl(sourceDir.name, list, uri, sourceCharset,
                        true, false, false, false, false,
                        restrictTestSourceToPackages, allDependencies));
            }
            return new InputConfigurationImpl(workingDirectory == null || workingDirectory.isBlank()
                    ? Path.of(".") : Path.of(workingDirectory),
                    List.copyOf(sourceSets), List.copyOf(classPathParts),
                    alternativeJREDirectory == null || alternativeJREDirectory.isBlank()
                            ? null : Path.of(alternativeJREDirectory));
        }

        // so that InputConfiguration.javaBase() recognizes the java.base java module
        // when DEFAULT_MODULES is used in a test setup
        private String removeJmod(String cpp) {
            if (cpp.startsWith("jmod:")) return cpp.substring(5);
            return cpp;
        }

        private static final Pattern SCHEME = Pattern.compile("([A-Za-z-]+):.+");

        private static URI createURI(String path) {
            if (SCHEME.matcher(path).matches()) {
                return URI.create(path);
            }
            return URI.create("file:" + path);
        }

        private static boolean isJmod(String classPathPart) {
            return classPathPart.startsWith("jmod:");
        }

        @Override
        public Builder setWorkingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        @Override
        public Builder addSourceSets(SourceSet... sourceSets) {
            this.sourceSets.addAll(Arrays.asList(sourceSets));
            return this;
        }

        @Override
        public Builder addClassPathParts(SourceSet... classPathParts) {
            this.classPathParts.addAll(Arrays.asList(classPathParts));
            return this;
        }

        @Override
        public Builder addClassPathParts(Collection<SourceSet> classPathParts) {
            this.classPathParts.addAll(classPathParts);
            return this;
        }

        @Override
        public Builder addSourceSets(Collection<SourceSet> sourceSets) {
            this.sourceSets.addAll(sourceSets);
            return this;
        }

        @Override
        @Fluent
        public Builder addSources(String... sources) {
            Arrays.stream(sources).forEach(s -> sourceDirs.add(new SourceSetNamePath(s, s)));
            return this;
        }

        @Override
        @Fluent
        public Builder addSource(String sourceSetName, String sourceSetPath) {
            sourceDirs.add(new SourceSetNamePath(sourceSetName, sourceSetPath));
            return this;
        }

        @Override
        @Fluent
        public Builder addTestSources(String... sources) {
            Arrays.stream(sources).forEach(s -> testSourceDirs.add(new SourceSetNamePath(s, s)));
            return this;
        }

        @Override
        @Fluent
        public Builder addTestSource(String sourceSetName, String sourceSetPath) {
            testSourceDirs.add(new SourceSetNamePath(sourceSetName, sourceSetPath));
            return this;
        }

        @Override
        @Fluent
        public Builder addClassPath(String... sources) {
            classPathStringParts.addAll(Arrays.asList(sources));
            return this;
        }

        @Override
        @Fluent
        public Builder addRuntimeClassPath(String... sources) {
            runtimeClassPathParts.addAll(Arrays.asList(sources));
            return this;
        }

        @Override
        @Fluent
        public Builder addTestClassPath(String... sources) {
            testClassPathParts.addAll(Arrays.asList(sources));
            return this;
        }

        @Override
        @Fluent
        public Builder addTestRuntimeClassPath(String... sources) {
            testRuntimeClassPathParts.addAll(Arrays.asList(sources));
            return this;
        }

        @Override
        @Fluent
        public Builder setAlternativeJREDirectory(String alternativeJREDirectory) {
            this.alternativeJREDirectory = alternativeJREDirectory;
            return this;
        }

        @Override
        @Fluent
        public Builder setSourceEncoding(String sourceEncoding) {
            this.sourceEncoding = sourceEncoding;
            return this;
        }

        @Override
        @Fluent
        public Builder addRestrictSourceToPackages(String... packages) {
            restrictSourceToPackages.addAll(Arrays.asList(packages));
            return this;
        }

        @Override
        @Fluent
        public Builder addRestrictTestSourceToPackages(String... packages) {
            restrictTestSourceToPackages.addAll(Arrays.asList(packages));
            return this;
        }
    }
}
