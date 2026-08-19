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

package io.codelaser.maddi.gradleplugin.inputconfig;

import io.codelaser.maddi.gradleplugin.AnalyzerExtension;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.run.config.util.PluginSourceSets;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.attributes.Category;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.compile.JavaCompile;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * targets for sources:
 * <ul>
 *     <li>multiple directories in a source set (DONE)</li>
 *     <li>source sets beyond main, test in the same project (e.g. functionalTest in testgradlepluginanalyzer (DONE)</li>
 *     <li>dependent source project in multi-project build (DONE, see {@code collectProjectSources})</li>
 *     <li>dependent source projects in composite build (TODO, current attempts have failed)</li>
 * </ul>
 * <p>
 * target for classpath: simply the main flags: test, runtimeOnly, and filtering using "excludeFromClasspath".
 * There is no dependency information here.
 */
public class ComputeSourceSets {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeSourceSets.class);

    public record Result(String mainSourceSetName, Map<String, SourceSet> sourceSetsByName,
                         List<Result> sourceSetDependencies,
                         Map<String, Set<String>> sourceProjectEdges) {
        public Map<String, SourceSet> allSourceSetsByName() {
            Map<String, SourceSet> map = new HashMap<>(sourceSetsByName);
            sourceSetDependencies.forEach(r -> map.putAll(r.allSourceSetsByName()));
            return map;
        }
    }

    /*
    all paths will be relative to this one
     */
    private final Path workingDirectory;

    public ComputeSourceSets(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
        assert this.workingDirectory.isAbsolute();
        LOGGER.info("Working directory is {}", this.workingDirectory);
    }

    public Result compute(Project project,
                          String restrictSourcesToPackages,
                          String restrictTestSourcesToPackages,
                          Set<String> excludeFromClasspath) {
        LOGGER.info("Computing source sets of {}", project);

        String encoding = detectSourceEncoding(project);
        JavaPluginExtension javaPluginExtension = new DslObject(project).getExtensions()
                .getByType(JavaPluginExtension.class);
        Map<String, SourceSet> sourceSetsByName = new HashMap<>();
        String projectName = project.getName();
        String buildUnit = buildUnitOf(project);

        for (org.gradle.api.tasks.SourceSet gradleSourceSet : javaPluginExtension.getSourceSets()) {
            String sourceSetName = projectName + "/" + gradleSourceSet.getName();
            boolean test = gradleSourceSet.getName().toLowerCase().contains("test");
            SourceSet sourceSet = makeSourceSet(project, gradleSourceSet, sourceSetName, buildUnit,
                    test ? restrictTestSourcesToPackages : restrictSourcesToPackages,
                    encoding, test);
            if (sourceSet != null) sourceSetsByName.put(sourceSet.name(), sourceSet);
        }

        List<Configuration> configurations = sortConfigurations(project);
        // sibling projects that publish their sources come first: their artifacts must NOT also be recorded as
        // jar classpath parts, or the same types arrive twice, once parsed and once shallow
        Map<String, List<Path>> sourcesByProject = collectProjectSources(project, configurations);
        // ...and the artifact each of them WOULD have contributed is exactly the class output their source set
        // needs, so inspectConfigurations hands it back rather than dropping it on the floor.
        Map<String, Path> classOutputByProject = inspectConfigurations(excludeFromClasspath, configurations,
                sourceSetsByName, sourcesByProject.keySet());
        String mainSourceSetName = projectName + "/main";
        List<Result> dependentProjects = sourcesByProject.entrySet().stream()
                .map(e -> dependentProjectResult(e.getKey(), e.getValue(), restrictSourcesToPackages, encoding,
                        classOutputByProject.get(e.getKey())))
                .toList();
        // the dependency edges AMONG the source-contributing projects (e.g. cst-analysis -> cst-api). Without
        // them a transitive source project cannot resolve the types it depends on and the front end drops it.
        Map<String, Set<String>> sourceProjectEdges = collectSourceProjectEdges(configurations,
                sourcesByProject.keySet());
        Result result = new Result(mainSourceSetName, sourceSetsByName, dependentProjects, sourceProjectEdges);
        LOGGER.info("Exit compute source sets with result: {} has source sets/classpath parts {}",
                result.mainSourceSetName, result.sourceSetsByName.keySet());
        return result;
    }

    /**
     * @return the class output of each project in {@code projectsProvidingSources}: the artifact it contributes to
     * this class path, which is precisely what its source set's {@code uri} must be. Collected here because this is
     * the only place that sees it -- the variant those projects publish carries source directories and nothing else.
     */
    private Map<String, Path> inspectConfigurations(Set<String> excludeFromClasspath,
                                       List<Configuration> configurations, Map<String, SourceSet> sourceSetsByName,
                                       Set<String> projectsProvidingSources) {
        Map<String, Path> classOutputByProject = new LinkedHashMap<>();
        for (Configuration configuration : configurations) {
            if (configuration.isCanBeResolved()) {
                String configurationName = configuration.getName();
                LOGGER.info("Inspecting configuration {}", configurationName);
                boolean isTest = configurationName.toLowerCase().contains("test");
                boolean isRuntimeOnly = configurationName.toLowerCase().contains("runtime");

                for (ResolvedArtifactResult rar : configuration.getIncoming().getArtifacts().getArtifacts()) {
                    // External libraries are consumed as their already-resolved compiled artifact. A sibling
                    // project is too, UNLESS it published its sources (see collectProjectSources) -- then it has
                    // already been turned into a source set and must not be added a second time as a jar.
                    // We still never recurse into a sibling to read its configurations: Gradle 9 rejects that as
                    // unsafe cross-project resolution. Variant reselection is what makes the source case legal.
                    String description;
                    boolean excludedByCoordinate;
                    File file = rar.getFile();
                    String name;
                    if (rar.getVariant().getOwner() instanceof ModuleComponentIdentifier mci) {
                        description = mci.getGroup() + ":" + mci.getModule() + ":" + mci.getVersion();
                        excludedByCoordinate = excludeFromClasspath.contains(description)
                                                || excludeFromClasspath.contains(mci.getModule());
                        // An external artifact is a jar, and its file name identifies it.
                        name = file.getName();
                    } else if (rar.getVariant().getOwner() instanceof ProjectComponentIdentifier pci) {
                        description = pci.getProjectName();
                        excludedByCoordinate = excludeFromClasspath.contains(pci.getProjectName())
                                               || projectsProvidingSources.contains(pci.getProjectName());
                        // A directory is the compile output; a jar is the packaged form of the same thing. Prefer
                        // the directory: it is what the producing build actually compiles into, so it is current
                        // whenever the build is, and javac reads a directory as happily as a jar.
                        if (projectsProvidingSources.contains(pci.getProjectName()) && file.canRead()
                            && (file.isDirectory() || !classOutputByProject.containsKey(pci.getProjectName()))) {
                            classOutputByProject.put(pci.getProjectName(), file.getAbsoluteFile().toPath());
                        }
                        name = projectPartName(pci, file);
                    } else {
                        continue;
                    }
                    if (file.canRead() && !excludeFromClasspath.contains(name) && !excludedByCoordinate) {
                        SourceSet existing = sourceSetsByName.get(name);
                        if (existing == null) {
                            LOGGER.info(" -- dependency {} ({}) in {}", description, name, configurationName);
                            SourceSet set = new SourceSetImpl.Builder()
                                    .setName(name)
                                    .setUri(absoluteURI(file))
                                    .setTest(isTest)
                                    .setLibrary(true)
                                    .setExternalLibrary(true)
                                    .setPartOfJdk(false)
                                    .setModule(isModularArtifact(file))
                                    .setRuntimeOnly(isRuntimeOnly)
                                    .build();
                            sourceSetsByName.put(name, set);
                        } else if (!absoluteURI(file).equals(existing.uri())) {
                            // ⛔ NOT A TIDINESS PROBLEM. The name is the identity: the serialized configuration
                            // resolves every `dependencies: ["<name>"]` edge by it, so two files answering to one
                            // name is a coin toss -- and skipping the second, which is all we can do here,
                            // silently removes every package it provides. Say so, loudly, with both files.
                            LOGGER.warn(" -- class path name clash: '{}' already means {}, so {} ({}) is DROPPED"
                                        + " and the packages it provides will not resolve", name, existing.uri(),
                                    file, description);
                        }
                    }
                }
            }
        }
        return classOutputByProject;
    }

    /**
     * The source directories that dependency projects publish on their {@code e2immuSourceElements} variant,
     * keyed by project name. This is the cross-project aggregation pattern Gradle blesses (the same one
     * {@code test-report-aggregation} and {@code jacoco-report-aggregation} use): an artifact view with
     * <em>variant reselection</em> asks each already-resolved component for a different variant of itself. It
     * reads only what the producer chose to publish, so it is not the unsafe cross-project configuration
     * resolution that Gradle 9 forbids.
     * <p>
     * Lenient because most components have no such variant at all -- every external jar, and any project on
     * which the plugin was not applied. Those must be skipped silently and stay ordinary classpath parts.
     */
    private Map<String, List<Path>> collectProjectSources(Project project, List<Configuration> configurations) {
        Category sourcesCategory = project.getObjects()
                .named(Category.class, AnalyzerExtension.SOURCES_CATEGORY);
        Map<String, Set<Path>> byProject = new LinkedHashMap<>();
        for (Configuration configuration : configurations) {
            if (!configuration.isCanBeResolved()) continue;
            ArtifactView view = configuration.getIncoming().artifactView(v -> {
                v.withVariantReselection();
                v.lenient(true);
                v.getAttributes().attribute(Category.CATEGORY_ATTRIBUTE, sourcesCategory);
            });
            for (ResolvedArtifactResult rar : view.getArtifacts().getArtifacts()) {
                if (rar.getVariant().getOwner() instanceof ProjectComponentIdentifier pci) {
                    File file = rar.getFile();
                    if (file.isDirectory() && file.canRead()) {
                        byProject.computeIfAbsent(pci.getProjectName(), k -> new LinkedHashSet<>())
                                .add(file.getAbsoluteFile().toPath().normalize());
                    }
                }
            }
        }
        byProject.forEach((name, paths) -> LOGGER.info(" -- project {} contributes sources {}", name, paths));
        return byProject.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> List.copyOf(e.getValue()), (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * A dependency project, as a {@link Result} of its own so that {@link ComputeDependencies} wires it up: it
     * walks {@code sourceSetDependencies} depth-first and makes this project's source sets depend on the main
     * source set of each, which is exactly the edge the openjdk front end needs to resolve types across them.
     * <p>
     * The package restriction is the <em>consuming</em> project's: {@code sourcePackages} says which packages
     * the user wants analyzed, and asking the sibling for its own setting would mean reading its extension,
     * i.e. the cross-project access this whole mechanism exists to avoid.
     */
    private Result dependentProjectResult(String projectName, List<Path> paths, String restrictTo,
                                          String encodingString, Path classOutput) {
        String sourceSetName = projectName + "/main";
        // ⛔ THE CLASS OUTPUT MATTERS MOST HERE, not least. The variant publishes source DIRECTORIES, so this
        // set's uri used to be the first of them -- and a dependent resolves into it through javac's class path,
        // which doubles as a source path, so only the types under THAT ONE directory were findable. A project
        // whose sources are split across roots lost everything outside the first: measured on pulsar, where
        // pulsar-common generates org.apache.pulsar.common.api.proto into a second, generated root and 64
        // diagnostics followed. The artifact the class path already carried is that output; see
        // inspectConfigurations, which now hands it over instead of discarding it.
        // ⚠ sourceRelease stays 0: a sibling's compile task belongs to another project, and reading it is the
        // cross-project access this whole mechanism exists to avoid.
        SourceSet sourceSet = PluginSourceSets.sourceSet(sourceSetName, null, paths, classOutput,
                encodingString == null ? null : Charset.forName(encodingString), false,
                restrictToPackages(restrictTo), 0);
        // null when none of the published directories exists any more. Map.of would throw on it, and a Result
        // holding no source set is exactly what "this project contributes nothing" means.
        Map<String, SourceSet> byName = new HashMap<>();
        if (sourceSet != null) byName.put(sourceSetName, sourceSet);
        return new Result(sourceSetName, byName, List.of(), Map.of());
    }

    /**
     * The dependency edges among the source-contributing projects, reconstructed from the resolution graph:
     * {@code project -> the source projects it directly depends on}. The flat {@link #collectProjectSources}
     * loses this — every dependent project is a leaf — so a transitive source project (cst-analysis, which
     * implements cst-api's interfaces) has no edge to the project it needs, cannot resolve those types, and is
     * dropped by the front end. {@link ComputeDependencies} turns these into source-set edges. Only edges
     * between two SOURCE projects matter; a dependency on a jar is already a class path part.
     */
    private Map<String, Set<String>> collectSourceProjectEdges(List<Configuration> configurations,
                                                               Set<String> sourceProjectNames) {
        Map<String, Set<String>> edges = new LinkedHashMap<>();
        for (Configuration configuration : configurations) {
            if (!configuration.isCanBeResolved()) continue;
            for (ResolvedComponentResult component :
                    configuration.getIncoming().getResolutionResult().getAllComponents()) {
                if (!(component.getId() instanceof ProjectComponentIdentifier fromPci)) continue;
                String from = fromPci.getProjectName();
                if (!sourceProjectNames.contains(from)) continue;
                for (DependencyResult dr : component.getDependencies()) {
                    if (dr instanceof ResolvedDependencyResult rdr
                        && rdr.getSelected().getId() instanceof ProjectComponentIdentifier toPci) {
                        String to = toPci.getProjectName();
                        if (!from.equals(to) && sourceProjectNames.contains(to)) {
                            edges.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(to);
                        }
                    }
                }
            }
        }
        edges.forEach((from, tos) -> LOGGER.info(" -- source project {} depends on source projects {}", from, tos));
        return edges;
    }

    private static Set<String> restrictToPackages(String restrictTo) {
        return restrictTo == null || restrictTo.isBlank() ? null :
                Arrays.stream(restrictTo.split("[,;]\\s*"))
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toUnmodifiableSet());
    }

    private static @NotNull List<Configuration> sortConfigurations(Project project) {
        List<Configuration> configurations = new ArrayList<>(project.getConfigurations());
        configurations.sort((c1, c2) -> {
            String n1 = c1.getName();
            String n2 = c2.getName();
            boolean t1 = n1.toLowerCase().contains("runtime");
            boolean t2 = n2.toLowerCase().contains("runtime");
            if (!t1 && t2) return -1;
            if (t1 && !t2) return 1;
            boolean r1 = n1.toLowerCase().contains("test");
            boolean r2 = n1.toLowerCase().contains("test");
            if (!r1 && r2) return -1;
            if (r1 && !r2) return 1;
            return n1.compareTo(n2);
        });
        return configurations;
    }

    Path toRelativePath(File file) {
        Path path = file.getAbsoluteFile().toPath();
        try {
            return workingDirectory.relativize(path);
        } catch (IllegalArgumentException iae) {
            return path;
        }
    }

    static URI makeURI(Path path) {
        assert !path.isAbsolute();
        return URI.create("file:" + path);
    }

    // Classpath parts (jars in ~/.gradle/caches, project-dependency class dirs) must carry an ABSOLUTE,
    // hierarchical file URI: maddi's openjdk inspector does a bare Path.of(classPathPart.uri()) with no
    // working-directory resolution, and Path.of throws IllegalArgumentException ("URI is not hierarchical")
    // on the opaque "file:<relative>" form makeURI produces. These paths are machine-specific anyway, so a
    // relative form buys no portability. (Source directories stay relative: the inspector resolves those
    // against the configured working directory.)
    static URI absoluteURI(File file) {
        return file.getAbsoluteFile().toURI();
    }

    /*
    The build unit groups the source sets of one Gradle project, and must be unique across the whole build.

    project.getName() is not: it is a leaf directory name, so ':a:util' and ':b:util' both yield 'util' -- which is
    exactly why the source set names built from it cannot serve as build units.

    project.getPath() (':a:util') is unique within one build, but not across a composite: an included build can
    carry the same ':core'. ProjectInternal.getIdentityPath() prefixes the included build, turning the ':core' of
    included build 'foo' into ':foo:core'. We fall back to getPath() should that internal type ever be absent,
    which remains correct for every non-composite build.
     */
    private static String buildUnitOf(Project project) {
        if (project instanceof ProjectInternal projectInternal) {
            return projectInternal.getIdentityPath().toString();
        }
        LOGGER.warn("Cannot determine the identity path of {}; falling back to its project path", project);
        return project.getPath();
    }

    private SourceSet makeSourceSet(Project project,
                                    org.gradle.api.tasks.SourceSet gradleSourceSet,
                                    String e2immuSourceSetName,
                                    String buildUnit,
                                    String restrictTo,
                                    String encodingString,
                                    boolean test) {
        Set<String> restrictToPackages = restrictToPackages(restrictTo);
        Charset sourceEncoding = encodingString == null ? null : Charset.forName(encodingString);
        // Java source dirs, plus Kotlin source dirs when the Kotlin JVM plugin is applied. SourceSet is
        // ExtensionAware and the Kotlin plugin registers a 'kotlin' SourceDirectorySet per source set; reading it
        // through the extension keeps this plugin free of any compile-time dependency on the Kotlin Gradle plugin.
        Set<File> srcDirs = new LinkedHashSet<>(gradleSourceSet.getAllJava().getSrcDirs());
        Object kotlin = ((ExtensionAware) gradleSourceSet).getExtensions().findByName("kotlin");
        if (kotlin instanceof SourceDirectorySet kotlinDirs) {
            srcDirs.addAll(kotlinDirs.getSrcDirs());
        }
        List<Path> paths = srcDirs.stream().map(f -> f.getAbsoluteFile().toPath().normalize()).toList();
        // The directory this set compiles to: what a DEPENDENT set resolves its references into through javac's
        // class path. Gradle's own destination for the java part of the set; a Kotlin set compiles to a sibling
        // directory that a single uri cannot also name.
        Path classOutput = gradleSourceSet.getJava().getClassesDirectory().get().getAsFile().toPath();
        return PluginSourceSets.sourceSet(e2immuSourceSetName, buildUnit, paths, classOutput, sourceEncoding,
                test, restrictToPackages, sourceReleaseOf(project, gradleSourceSet));
    }

    /**
     * The Java API level this source set is compiled against, or {@code 0} when its build says nothing.
     *
     * <p>⛔ <b>WITHOUT IT THE PARSE RUNS ON WHATEVER JDK MADDI HAPPENS TO BE, NOT ON THE ONE THE CORPUS TARGETS</b>
     * — and every API removed since then reads as "cannot find symbol", drops the compilation unit, and can cost
     * the whole {@code ParseResult}. Measured on pulsar (2026-08-19): the corpus states release 17,
     * {@code --compile-log} recorded it, the plugin recorded nothing, and {@code Thread.suspend()} — which does
     * not exist on JDK 26 — stopped resolving in {@code ZooKeeperUtil}.
     *
     * <p>Asked PER SOURCE SET, because Gradle answers per source set: each has its own {@code JavaCompile} task,
     * and fernflower is the case that shows it matters — {@code compileJava} pins {@code sourceCompatibility=21}
     * while {@code compileTestJava} says nothing and gets the toolchain's own level.
     *
     * <p>{@code options.release} first: it is the only setting that also constrains the API against which the
     * code is compiled, which is exactly the question here. {@code sourceCompatibility} is the older spelling and
     * strictly weaker (it constrains the language level), but it is what a build that predates {@code --release}
     * states, so it is read next — first from the task, then from the project-wide extension.
     */
    private static int sourceReleaseOf(Project project, org.gradle.api.tasks.SourceSet gradleSourceSet) {
        Task task = project.getTasks().findByName(gradleSourceSet.getCompileJavaTaskName());
        if (task instanceof JavaCompile compile) {
            Integer release = compile.getOptions().getRelease().getOrNull();
            if (release != null && release > 0) return release;
            int fromTask = PluginSourceSets.parseRelease(compile.getSourceCompatibility());
            if (fromTask > 0) return fromTask;
        }
        JavaPluginExtension extension = project.getExtensions().findByType(JavaPluginExtension.class);
        return extension == null ? 0 : PluginSourceSets.parseRelease(extension.getSourceCompatibility().toString());
    }

    /**
     * The class-path part name of a SIBLING PROJECT's artifact.
     *
     * <p>⛔⛔ <b>NOT {@code file.getName()}, WHICH IS {@code "main"} FOR EVERY PROJECT IN THE BUILD.</b> Gradle
     * resolves a project dependency on a compile class path to that project's <em>classes directory</em>, not to
     * a jar (that is what compile avoidance is), so every sibling arrives as
     * {@code <project>/build/classes/java/main}. Naming parts by the file name therefore gave them all one name,
     * and the "already have it" guard dropped every sibling after the first -- <b>with no log line</b>, because
     * the guard had no {@code else}.
     *
     * <p>⚠ <b>MEASURED, on pulsar</b> (2026-08-19): {@code :managed-ledger} has 7 sibling projects on its
     * class path and the configuration contained <b>one</b>, called {@code main}. The parse failed with 100
     * errors in managed-ledger's OWN sources -- {@code package org.apache.pulsar.common.policies.data does not
     * exist} and so on -- naming packages that were simply not on the class path any more. The same reactor
     * through {@code --compile-log} parses with 0 errors.
     *
     * <p>The comment this replaces argued the name "must be the jar file name, not the coordinate", because
     * maddi once resolved a part by parsing its name. It does not: {@link
     * io.codelaser.maddi.inspection.api.resource.InputConfiguration#jarOnClasspathSelector} answers that question
     * from an explicit {@code jar-on-classpath:} prefix, and says in as many words that asking the entry settles
     * it "without taking the naming freedom away". The file is located by {@code uri()}; the name is only an
     * identity, and identity is exactly what a directory called {@code main} does not have.
     *
     * <p>The project PATH, not {@code getProjectName()}: the latter is a leaf directory name, so {@code :a:util}
     * and {@code :b:util} are both {@code util}. The file name is kept as a suffix because one project may
     * contribute several directories (classes and resources) to one class path.
     */
    private static String projectPartName(ProjectComponentIdentifier pci, File file) {
        return pci.getProjectPath() + "/" + file.getName();
    }

    /** As {@link PluginSourceSets#isModularSource}, for a dependency: an explicit module carries a
     * {@code module-info.class}. */
    private static boolean isModularArtifact(File file) {
        if (file.isDirectory()) {
            return new File(file, "module-info.class").canRead();
        }
        try (JarFile jarFile = new JarFile(file)) {
            return jarFile.getEntry("module-info.class") != null
                   || jarFile.getEntry("META-INF/versions/9/module-info.class") != null;
        } catch (IOException e) {
            LOGGER.warn("Cannot read {} as a jar, assuming it is not a module: {}", file, e.getMessage());
            return false;
        }
    }

    private static String detectSourceEncoding(Project project) {
        AtomicReference<String> encodingRef = new AtomicReference<>();
        project.getTasks().withType(JavaCompile.class, compile -> {
            String encoding = compile.getOptions().getEncoding();
            if (encoding != null) {
                encodingRef.set(encoding);
            }
        });
        return encodingRef.get();
    }

    Path getWorkingDirectory() {
        return workingDirectory;
    }
}
