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
import io.codelaser.maddi.run.main.PluginOptions;
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
 *     <li>source sets beyond main, test in the same project (e.g. functionalTest) (DONE)</li>
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
        /**
         * ⛔⛔ <b>THE ANALYSED PROJECT'S OWN VIEW OF A SOURCE SET WINS, AND IT USED TO LOSE.</b> The two sides
         * describe the same set and only one of them can see the compile task: {@link #makeSourceSet} asks it for
         * {@code buildUnit}, {@code sourceRelease}, {@code addModules} and {@code warningFlags} and points
         * {@code uri} at the class OUTPUT, while {@link #dependentProjectResult} — which exists for SIBLINGS —
         * passes {@code null}, {@code 0} and two empty lists by design, because a sibling's compile task belongs
         * to another project. Merging the dependents LAST let the sibling-shaped record overwrite the real one.
         *
         * <p>⚠ <b>AND THE PROJECT IS ONE OF ITS OWN DEPENDENTS</b>, which is why this fires at all: with the
         * plugin applied to it, it publishes the {@code maddiSourceElements} variant and then resolves that
         * variant through its own configurations, so {@code collectProjectSources} hands it back to itself.
         *
         * <p>⚠ <b>MEASURED, on OpenSearch {@code :libs:opensearch-common}</b> (2026-08-20). Its build file scopes
         * {@code --add-modules jdk.incubator.vector} to {@code compileJava}, so losing that set's
         * {@code addModules} loses the flag: <b>13 × "Unknown module jdk.incubator.vector", 24 stub types</b> for
         * {@code jdk.internal.vm.vector.VectorSupport$*}. The {@code uri} went with it — the sibling record names
         * {@code build/distributions/opensearch-common-3.9.0-SNAPSHOT.jar}, which does not exist — so
         * {@code opensearch-common/test} could not resolve into {@code main} and its units were dropped.
         *
         * <p>⛔ <b>AND NONE OF IT REACHED THE GATE.</b> {@code ErrorReport} counted <b>1 warning</b>: the 13 are
         * {@code ClassSymbolScanner} lines it does not collect. A parse that has lost a module reports clean.
         */
        public Map<String, SourceSet> allSourceSetsByName() {
            Map<String, SourceSet> map = new HashMap<>();
            sourceSetDependencies.forEach(r -> map.putAll(r.allSourceSetsByName()));
            map.putAll(sourceSetsByName);
            return map;
        }
    }

    /**
     * What {@link #collectProjectSources} found, keyed by project NAME: the source directories each sibling
     * publishes, and the project PATH that name belongs to.
     *
     * <p>⭐ The path is the sibling's IDENTITY path, so that {@link #dependentProjectResult} can give it a
     * {@code buildUnit} that means the same thing as the analysed project's. It is the one fact of the four a
     * sibling used to lose that needs <b>no</b> cross-project access at all: {@code ProjectComponentIdentifier}
     * carries it, and the consumer already has that identifier in hand. See {@link #identityPathOf} for why
     * the project path alone is not enough.
     *
     * <p>⚠ Keyed by NAME because the rest of this class is; two projects in one build may share a name and the
     * last one seen would win. Pre-existing, not introduced here, and worth knowing before anything depends on
     * the key being unique.
     */
    private record ProjectSources(Map<String, List<Path>> sourcesByName, Map<String, String> pathByName,
                                 Map<String, SourceFacts> factsByName) {
    }

    /**
     * A class-path part already recorded for one sibling output: its name, and whether the file behind it is a
     * class DIRECTORY. ⚠ The flag is carried rather than re-derived from the recorded {@code uri}: a URI that
     * happens to end in {@code /} is an inference, and {@code File.isDirectory()} is the question itself.
     */
    private record RecordedPart(String name, boolean directory) {
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
        ProjectSources projectSources = collectProjectSources(project, configurations);
        Map<String, List<Path>> sourcesByProject = projectSources.sourcesByName();
        // ...and the artifact each of them WOULD have contributed is exactly the class output their source set
        // needs, so inspectConfigurations hands it back rather than dropping it on the floor.
        Map<String, Path> classOutputByProject = inspectConfigurations(excludeFromClasspath, configurations,
                sourceSetsByName, sourcesByProject.keySet());
        String mainSourceSetName = projectName + "/main";
        List<Result> dependentProjects = sourcesByProject.entrySet().stream()
                .map(e -> dependentProjectResult(e.getKey(), projectSources.pathByName().get(e.getKey()),
                        e.getValue(), restrictSourcesToPackages, encoding,
                        classOutputByProject.get(e.getKey()), projectSources.factsByName().get(e.getKey())))
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
        // ⛔⛔ ONE CLASS-PATH PART PER SIBLING PROJECT, AND IT USED TO BE ONE PER ARTIFACT VARIANT.
        // Gradle answers `compileClasspath` with a project's classes DIRECTORY (java-api) and `runtimeClasspath`
        // with its JAR (java-runtime), so a sibling that does not publish its sources was recorded TWICE, under
        // two names, from two configurations. The de-duplication a few lines below is keyed on the sibling
        // PUBLISHING ITS SOURCES, i.e. on the plugin being applied to it too -- which is never the case in the
        // single-module mode a user of this plugin is in.
        // ⚠ HARMLESS ON THE CLASS PATH AND FATAL ON THE MODULE PATH, which is why it went unnoticed:
        // duplicate types are tolerated, ONE MODULE NAME FROM TWO LOCATIONS is not. MEASURED on OpenSearch's
        // JPMS corpus (2026-08-20): 12 of 12 siblings recorded twice, both `module=true`, and the parse died
        // with `Cannot map javac's type org.opensearch.core.compress.Compressor onto a TypeInfo`.
        // ⚠ AND THE CONTROL SAYS IT IS THE MODULES AND NOT THE TWINS AS SUCH: the same module, the same
        // plugin, the same twin shape on PRISTINE OpenSearch -- where nothing is a module -- parses. pulsar's
        // committed configuration has the same shape for the same reason and has always parsed.
        Map<String, RecordedPart> partByOutput = new LinkedHashMap<>();
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
                        // one part per project OUTPUT: the first sighting wins, and a DIRECTORY displaces a
                        // jar. ⚠ sortConfigurations already puts the non-runtime configurations first, so in
                        // practice the directory arrives first and nothing is displaced; the displacement is
                        // here because that order is a heuristic and this rule is not.
                        // ⚠ `projectId`, not `pci`: the pattern variable of the `else if` above is bound in a
                        // branch that does NOT complete abruptly, so whether it is still in scope here is a
                        // question about JLS 6.3.2 rather than about this code. A different name has no
                        // question attached to it.
                        if (rar.getVariant().getOwner() instanceof ProjectComponentIdentifier projectId) {
                            String key = outputKey(projectId, rar);
                            RecordedPart already = partByOutput.get(key);
                            boolean isDirectory = file.isDirectory();
                            if (already != null && already.name().equals(name)) {
                                continue;                       // same output, same artifact, seen again
                            }
                            if (already == null) {
                                partByOutput.put(key, new RecordedPart(name, isDirectory));
                            } else if (!already.directory() && !isDirectory) {
                                // ⛔ NOT THE CASE THIS RULE IS FOR. Two PACKAGED artifacts under one capability
                                // are two different things, not two views of one, and collapsing them would
                                // delete every package the second provides. Only the directory/jar pair is one
                                // output seen twice.
                                LOGGER.warn(" -- project {} contributes two packaged artifacts under one"
                                            + " capability: {} and {}. Both kept.", projectId.getProjectPath(),
                                        already.name(), name);
                            } else if (!isDirectory) {
                                LOGGER.info(" -- project {} already contributes the directory {}, so {} is not"
                                            + " recorded a second time", projectId.getProjectPath(),
                                        already.name(), name);
                                continue;
                            } else {
                                LOGGER.info(" -- project {} contributes the directory {}, which replaces {}",
                                        projectId.getProjectPath(), name, already.name());
                                sourceSetsByName.remove(already.name());
                                partByOutput.put(key, new RecordedPart(name, true));
                            }
                        }
                        SourceSet existing = sourceSetsByName.get(name);
                        if (existing == null) {
                            LOGGER.info(" -- dependency {} ({}) in {}", description, name, configurationName);
                            sourceSetsByName.put(name,
                                    PluginSourceSets.classPathPart(name, file, isTest, isRuntimeOnly));
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
     * The source directories that dependency projects publish on their {@code maddiSourceElements} variant,
     * keyed by project name. This is the cross-project aggregation pattern Gradle blesses (the same one
     * {@code test-report-aggregation} and {@code jacoco-report-aggregation} use): an artifact view with
     * <em>variant reselection</em> asks each already-resolved component for a different variant of itself. It
     * reads only what the producer chose to publish, so it is not the unsafe cross-project configuration
     * resolution that Gradle 9 forbids.
     * <p>
     * Lenient because most components have no such variant at all -- every external jar, and any project on
     * which the plugin was not applied. Those must be skipped silently and stay ordinary classpath parts.
     */
    private ProjectSources collectProjectSources(Project project, List<Configuration> configurations) {
        Category sourcesCategory = project.getObjects()
                .named(Category.class, AnalyzerExtension.SOURCES_CATEGORY);
        Map<String, Set<Path>> byProject = new LinkedHashMap<>();
        Map<String, String> pathByName = new LinkedHashMap<>();
        Map<String, SourceFacts> factsByName = new LinkedHashMap<>();
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
                        pathByName.putIfAbsent(pci.getProjectName(), identityPathOf(pci));
                    } else if (SourceFactsFile.FILE_NAME.equals(file.getName()) && file.canRead()) {
                        // ⭐ THE THREE FACTS A SIBLING COULD NOT HAVE, arriving through the variant it
                        // already publishes. Matched by NAME rather than by "not a directory": a producer is
                        // free to publish other things, and a name says what was found.
                        SourceFacts facts = SourceFactsFile.read(file);
                        if (facts != null) factsByName.putIfAbsent(pci.getProjectName(), facts);
                    }
                }
            }
        }
        byProject.forEach((name, paths) -> LOGGER.info(" -- project {} contributes sources {}", name, paths));
        // ⚠ A source-providing project WITHOUT facts is the version-skew case (an older producer), not an
        // error; it is logged so that a run where every sibling lacks them is visible rather than assumed.
        for (String name : byProject.keySet()) {
            if (!factsByName.containsKey(name)) {
                LOGGER.info(" -- project {} published sources but no {}", name, SourceFactsFile.FILE_NAME);
            }
        }
        return new ProjectSources(byProject.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> List.copyOf(e.getValue()), (a, b) -> a, LinkedHashMap::new)), pathByName, factsByName);
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
    private Result dependentProjectResult(String projectName, String projectPath, List<Path> paths,
                                          String restrictTo, String encodingString, Path classOutput,
                                          SourceFacts facts) {
        String sourceSetName = projectName + "/main";
        // ⛔ THE CLASS OUTPUT MATTERS MOST HERE, not least. The variant publishes source DIRECTORIES, so this
        // set's uri used to be the first of them -- and a dependent resolves into it through javac's class path,
        // which doubles as a source path, so only the types under THAT ONE directory were findable. A project
        // whose sources are split across roots lost everything outside the first: measured on pulsar, where
        // pulsar-common generates org.apache.pulsar.common.api.proto into a second, generated root and 64
        // diagnostics followed. The artifact the class path already carried is that output; see
        // inspectConfigurations, which now hands it over instead of discarding it.
        // ⭐ buildUnit IS available, and it used to be null. It comes from the ProjectComponentIdentifier the
        // consumer already holds -- no cross-project access at all -- and it is the field that GROUPS source
        // sets into build units downstream. MEASURED on OpenSearch (2026-08-20, applyTo=all): 19 of 21 source
        // sets arrived with buildUnit=null, sourceRelease=None and warningFlags=[], and a consumer that groups
        // by build unit had nothing to group them by.
        // ⭐⭐ AND THE OTHER THREE ARRIVE TOO, SINCE 2026-08-21. sourceRelease, addModules and the warning
        // flags live on a JavaCompile task, and a sibling's task belongs to another project -- reading it is
        // the cross-project access this whole mechanism exists to avoid. They come instead through the
        // variant the sibling ALREADY publishes, as a file its own configuration writes: SourceFactsFile,
        // which also records the three shapes that were rejected (in particular an artifact a TASK produces,
        // which does not exist yet at this method's configuration time -- that one was written and taken
        // back out).
        // ⚠ WHAT ITS ABSENCE COST, measured on OpenSearch (2026-08-20, applyTo=all): 2 source sets of 21
        // carried any warning flag, and 19 arrived with sourceRelease=0 -- which silently reinstates
        // "whatever JDK maddi happens to run on" for each of them.
        // ⚠ NULL IS STILL A CASE, and it is the version-skew one: a producer without this plugin, or with an
        // older one, publishes no such file. Then this is exactly what it was before.
        SourceFacts f = facts == null ? new SourceFacts(0, List.of(), List.of()) : facts;
        SourceSet sourceSet = PluginSourceSets.sourceSet(sourceSetName, projectPath, paths, classOutput,
                encodingString == null ? null : Charset.forName(encodingString), false,
                PluginOptions.splitToSetOrNull(restrictTo), f.sourceRelease(), f.addModules(),
                f.warningFlags());
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


    /**
     * The order in which configurations are read, which decides <b>which one records a shared class-path
     * part</b>: the first sighting wins, and the recorder's NAME is where {@code test} and {@code runtimeOnly}
     * come from. Production before runtime-only, non-test before test, then alphabetical.
     *
     * <p>⛔⛔ <b>THE TEST/NON-TEST TIEBREAK COMPARED {@code n1} TWICE AND THEREFORE NEVER FIRED</b>, so the
     * order fell through to alphabetical -- which HAPPENS to agree for the standard names ({@code
     * compileClasspath} sorts before {@code testCompileClasspath} either way) and disagrees for every
     * non-test configuration whose name sorts after {@code test}: {@code zip}, and the tool configurations
     * {@code jarHell}, {@code jdkJarHell}, {@code jacocoAgent}, {@code missingdoclet}, {@code
     * loggerUsagePlugin}, {@code resolveableCompileOnly}.
     *
     * <p>⭐⭐ <b>THE REPAIR WAS PRICED BEFORE IT WAS MADE, and the price is the argument FOR it.</b> MEASURED
     * on OpenSearch (2026-08-21), 214 projects, over the RESOLVABLE configurations only -- the ones this
     * class reads: <b>93 projects change order and 109 class-path parts in 16 projects change their
     * flags</b>, {@code :server} alone 43 of its 135. Every transition is a production dependency that had
     * been labelled a test one: {@code opensearch-grok}, {@code opensearch-rest-client} and 60 more moved
     * from {@code internalClusterTestRuntimeClasspath} to {@code runtimeClasspath}; {@code lucene-core} and
     * 17 more from {@code aggregateTestReportResults} to {@code compileClasspath}. The defect was
     * systematically calling production jars test-only wherever a test configuration's NAME sorted first.
     *
     * <p>⛔ <b>A THREE-PROJECT SAMPLE OF THIS SAID ZERO.</b> The projects with the most flipped PAIRS are
     * not the projects with the most changed PARTS -- {@code :server}'s driver is a configuration the other
     * three do not have -- so the sample, chosen by a proxy for the mechanism, measured the proxy.
     *
     * <p>⚠ The residue, filed and not fixed: 3 parts ({@code asm}, {@code asm-tree}, {@code asm-analysis})
     * move from a test configuration to {@code loggerUsagePlugin}, a BUILD TOOL's own configuration. Being
     * claimed by a tool is a third state these two flags cannot express, and no failure has demanded it.
     */
    static final Comparator<String> CONFIGURATION_ORDER = (n1, n2) -> {
        boolean t1 = n1.toLowerCase().contains("runtime");
        boolean t2 = n2.toLowerCase().contains("runtime");
        if (!t1 && t2) return -1;
        if (t1 && !t2) return 1;
        boolean r1 = n1.toLowerCase().contains("test");
        boolean r2 = n2.toLowerCase().contains("test");
        if (!r1 && r2) return -1;
        if (r1 && !r2) return 1;
        return n1.compareTo(n2);
    };

    private static @NotNull List<Configuration> sortConfigurations(Project project) {
        List<Configuration> configurations = new ArrayList<>(project.getConfigurations());
        configurations.sort(Comparator.comparing(Configuration::getName, CONFIGURATION_ORDER));
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
                                    String maddiSourceSetName,
                                    String buildUnit,
                                    String restrictTo,
                                    String encodingString,
                                    boolean test) {
        Set<String> restrictToPackages = PluginOptions.splitToSetOrNull(restrictTo);
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
        SourceFacts facts = factsOf(project, gradleSourceSet);
        return PluginSourceSets.sourceSet(maddiSourceSetName, buildUnit, paths, classOutput, sourceEncoding,
                test, restrictToPackages, facts.sourceRelease(), facts.addModules(), facts.warningFlags());
    }

    /**
     * What one source set's {@code JavaCompile} task says about its compilation, in one value.
     *
     * <p>⚠ <b>ONE EXTRACTION, ONE CALLER — FOR NOW.</b> It is a method rather than three inline calls because
     * the second caller is already named: whatever channel eventually carries these facts to a SIBLING (see
     * {@link #dependentProjectResult}) has to produce exactly this, and a second reading of "what does this
     * compile task say" would be a second model that agrees until the day it does not.
     */
    static SourceFacts factsOf(Project project, org.gradle.api.tasks.SourceSet gradleSourceSet) {
        return new SourceFacts(sourceReleaseOf(project, gradleSourceSet),
                addModulesOf(project, gradleSourceSet),
                warningFlagsOf(project, gradleSourceSet));
    }

    /**
     * javac's {@code --add-modules} for this source set, from its own {@code JavaCompile} task's
     * {@code options.compilerArgs}.
     *
     * <p>⛔ <b>NOT REACHABLE BY WIDENING {@code jmods}.</b> An incubator module is not in the {@code java.se}
     * closure and cannot be added to it: it has to be in the ROOT SET of the compilation that uses it, or every
     * type in it reads as "package X is not visible". Found on the Maven side, on trino (2026-08-19), and fixed
     * here at the same time because the two plugins are twins and a defect in one is a hypothesis about the
     * other -- which has been right both times it was tested this week.
     *
     * <p>⚠ <b>NO CORPUS EXERCISES THIS PATH ON THE GRADLE SIDE.</b> Neither fernflower nor pulsar passes
     * {@code --add-modules}, so what stands behind it is the unit test and the symmetry, not a measurement. Said
     * plainly rather than left to look measured.
     */
    private static List<String> addModulesOf(Project project,
                                             org.gradle.api.tasks.SourceSet gradleSourceSet) {
        JavaCompile compile = (JavaCompile) project.getTasks()
                .findByName(gradleSourceSet.getCompileJavaTaskName());
        if (compile == null) return List.of();
        return PluginSourceSets.addModulesFrom(compile.getOptions().getCompilerArgs());
    }

    /**
     * This source set's warning policy -- {@code -Werror}, {@code -nowarn}, {@code -Xlint...} -- from the same
     * {@code options.compilerArgs} the modules above come from.
     *
     * <p>⭐ <b>THE LIST IS ALREADY RESOLVED, WHICH IS THE WHOLE VALUE OF ASKING GRADLE INSTEAD OF A BUILD
     * FILE.</b> OpenSearch's root appends {@code -Werror} to every compile task ({@code build.gradle:280}) and
     * 12 subprojects subtract it again ({@code libs/common/build.gradle:56}, whose comment says why: "use of
     * incubator modules is reported as a warning"). Read here, at configuration time, the subtraction has
     * happened: the flag is present exactly in the sets that will fail on a warning. A grep over build files
     * finds the word in 13 places and cannot say that about any of them.
     *
     * <p>⚠ Asked PER SOURCE SET for the same reason {@code sourceRelease} is: each has its own
     * {@code JavaCompile} task, and a build that exempts its main compilation need not exempt its tests.
     */
    private static List<String> warningFlagsOf(Project project,
                                               org.gradle.api.tasks.SourceSet gradleSourceSet) {
        JavaCompile compile = (JavaCompile) project.getTasks()
                .findByName(gradleSourceSet.getCompileJavaTaskName());
        if (compile == null) return List.of();
        return PluginSourceSets.warningFlagsFrom(compile.getOptions().getCompilerArgs());
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
    /**
     * The build unit of a SIBLING project: its identity path, which is what {@link #buildUnitOf} gives the
     * analysed project.
     *
     * <p>⛔⛔ <b>NOT {@code getProjectPath()}, WHICH IS RELATIVE TO THE SIBLING'S OWN BUILD.</b> Measured on
     * OpenSearch (2026-08-21, {@code applyTo=all}): the first version of this used the project path, and
     * {@code missing-doclet} — the root project of an INCLUDED build — came out as {@code ":"}. Every included
     * build has a root, so that value is ambiguous by construction, and it would have made two unrelated units
     * one. The identity path is {@code <buildPath><projectPath>}, and it is what the analysed project's own
     * source sets already carry, so the two sides finally agree.
     */
    private static String identityPathOf(ProjectComponentIdentifier pci) {
        String buildPath = pci.getBuild().getBuildPath();
        String projectPath = pci.getProjectPath();
        if (buildPath == null || ":".equals(buildPath)) return projectPath;
        return ":".equals(projectPath) ? buildPath : buildPath + projectPath;
    }

    private static String projectPartName(ProjectComponentIdentifier pci, File file) {
        return pci.getProjectPath() + "/" + file.getName();
    }

    /**
     * What identifies ONE OUTPUT of a sibling project, so that the same output seen through two variants is
     * recorded once.
     *
     * <p>⛔⛔ <b>THE PROJECT PATH ALONE IS THE WRONG KEY, AND IT WOULD DELETE TEST FIXTURES.</b> Gradle answers
     * {@code compileClasspath} with a project's classes DIRECTORY and {@code runtimeClasspath} with its JAR —
     * one output, two variants, and that pair is what has to collapse. But
     * {@code testImplementation(testFixtures(project(":foo")))} puts a SECOND, genuinely different artifact of
     * {@code :foo} on the same class path, and it differs from the first only by its CAPABILITY
     * ({@code foo-test-fixtures} against {@code foo}). Keying on the project would collapse those two as well
     * and silently remove every package the fixtures provide.
     *
     * <p>⚠ No corpus exercises the test-fixtures case; what stands behind it is the capability model and the
     * fact that the failure would be silent. Said plainly rather than left to look measured.
     */
    private static String outputKey(ProjectComponentIdentifier pci, ResolvedArtifactResult rar) {
        String capabilities = rar.getVariant().getCapabilities().stream()
                .map(c -> c.getGroup() + ":" + c.getName())
                .sorted()
                .collect(Collectors.joining(","));
        return pci.getProjectPath() + "|" + capabilities;
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
