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

package org.e2immu.gradleplugin.inputconfig;

import org.e2immu.gradleplugin.AnalyzerExtension;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
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
import java.nio.file.Files;
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
                         List<Result> sourceSetDependencies) {
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
            SourceSet sourceSet = makeSourceSet(gradleSourceSet, sourceSetName, buildUnit,
                    test ? restrictTestSourcesToPackages : restrictSourcesToPackages,
                    encoding, test);
            if (sourceSet != null) sourceSetsByName.put(sourceSet.name(), sourceSet);
        }

        List<Configuration> configurations = sortConfigurations(project);
        // sibling projects that publish their sources come first: their artifacts must NOT also be recorded as
        // jar classpath parts, or the same types arrive twice, once parsed and once shallow
        Map<String, List<Path>> sourcesByProject = collectProjectSources(project, configurations);
        inspectConfigurations(excludeFromClasspath, configurations, sourceSetsByName, sourcesByProject.keySet());
        String mainSourceSetName = projectName + "/main";
        List<Result> dependentProjects = sourcesByProject.entrySet().stream()
                .map(e -> dependentProjectResult(e.getKey(), e.getValue(), restrictSourcesToPackages, encoding))
                .toList();
        Result result = new Result(mainSourceSetName, sourceSetsByName, dependentProjects);
        LOGGER.info("Exit compute source sets with result: {} has source sets/classpath parts {}",
                result.mainSourceSetName, result.sourceSetsByName.keySet());
        return result;
    }

    private void inspectConfigurations(Set<String> excludeFromClasspath,
                                       List<Configuration> configurations, Map<String, SourceSet> sourceSetsByName,
                                       Set<String> projectsProvidingSources) {
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
                    if (rar.getVariant().getOwner() instanceof ModuleComponentIdentifier mci) {
                        description = mci.getGroup() + ":" + mci.getModule() + ":" + mci.getVersion();
                        excludedByCoordinate = excludeFromClasspath.contains(description)
                                                || excludeFromClasspath.contains(mci.getModule());
                    } else if (rar.getVariant().getOwner() instanceof ProjectComponentIdentifier pci) {
                        description = pci.getProjectName();
                        excludedByCoordinate = excludeFromClasspath.contains(pci.getProjectName())
                                               || projectsProvidingSources.contains(pci.getProjectName());
                    } else {
                        continue;
                    }
                    File file = rar.getFile();
                    // maddi keys a classpath source set by its jar file name and resolves it as "jar file: <name>",
                    // so the part name must be the jar file name, not the coordinate (otherwise: "Cannot find class
                    // path source set interpreted as jar file: ...").
                    String name = file.getName();
                    if (!sourceSetsByName.containsKey(name) && file.canRead()
                        && !excludeFromClasspath.contains(name) && !excludedByCoordinate) {
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
                    }
                }
            }
        }
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
                                          String encodingString) {
        String sourceSetName = projectName + "/main";
        SourceSet sourceSet = new SourceSetImpl.Builder().setName(sourceSetName)
                .setSourceDirectories(paths).setUri(paths.getFirst().toUri())
                .setSourceEncoding(encodingString == null ? null : Charset.forName(encodingString))
                .setModule(isModularSource(paths))
                .setRestrictToPackages(restrictToPackages(restrictTo))
                .build();
        return new Result(sourceSetName, new HashMap<>(Map.of(sourceSetName, sourceSet)), List.of());
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

    private SourceSet makeSourceSet(org.gradle.api.tasks.SourceSet gradleSourceSet,
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
        // Emit ABSOLUTE source directories and a hierarchical file:/... URI. A relative/opaque URI (file:src) makes
        // maddi's openjdk inspector skip this source set when it appears as another set's dependency (test -> main),
        // because it cannot Path.of() an opaque URI -- so test sources then fail to resolve main types. Absolute
        // paths also remove any dependence on the process working directory. (Mirrors the Maven plugin.)
        List<Path> paths = srcDirs.stream()
                .filter(File::canRead).map(f -> f.getAbsoluteFile().toPath().normalize()).toList();
        if (paths.isEmpty()) return null;
        return new SourceSetImpl.Builder().setName(e2immuSourceSetName)
                .setBuildUnit(buildUnit)
                .setSourceDirectories(paths).setUri(paths.getFirst().toUri())
                .setSourceEncoding(sourceEncoding).setTest(test)
                .setModule(isModularSource(paths))
                .setRestrictToPackages(restrictToPackages).build();
    }

    /**
     * A source set is a Java module when one of its source directories holds a {@code module-info.java}. The
     * distinction is not cosmetic: the openjdk front end puts a module's dependencies on javac's <em>module
     * path</em>, and without the flag every {@code requires}d package comes back as "package X is not visible".
     */
    private static boolean isModularSource(List<Path> paths) {
        return paths.stream().anyMatch(p -> Files.isRegularFile(p.resolve("module-info.java")));
    }

    /** As {@link #isModularSource}, for a dependency: an explicit module carries a {@code module-info.class}. */
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
