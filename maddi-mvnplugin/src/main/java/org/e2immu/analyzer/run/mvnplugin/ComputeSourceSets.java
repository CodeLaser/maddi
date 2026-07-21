package org.e2immu.analyzer.run.mvnplugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.*;
import org.e2immu.analyzer.run.config.util.ComputeDependencies;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ComputeSourceSets {

    private final ProjectDependenciesResolver dependenciesResolver;
    private final MavenProject project;
    private final MavenSession session;
    private final Log log;

    public ComputeSourceSets(ProjectDependenciesResolver dependenciesResolver,
                             MavenProject mavenProject, MavenSession mavenSession, Log log) {
        this.dependenciesResolver = dependenciesResolver;
        this.project = mavenProject;
        this.session = mavenSession;
        this.log = log;
    }

    public ComputeDependencies.SourceSetDependencies compute(String sourceEncoding,
                                                             String sourcePackages,
                                                             String testSourcePackages,
                                                             Set<String> excludeFromClasspathSet)
            throws DependencyResolutionException {
        Map<String, SourceSet> sourceSetsByName = new HashMap<>();
        String projectName = project.getName();
        // the source set name is built from project.getName(), which is the POM's <name> (or artifactId) and is
        // not unique across a reactor: sibling modules can share it. The build unit must be unique, so we use the
        // coordinate instead. It groups this module's main and test source sets, and only those.
        String buildUnit = project.getGroupId() + ":" + project.getArtifactId();
        Charset encoding = Charset.forName(sourceEncoding, Charset.defaultCharset());

        Set<SourceSet> deps = new HashSet<>();
        deps.addAll(computeClassPathParts(JavaScopes.COMPILE, false, false, sourceSetsByName,
                excludeFromClasspathSet));
        deps.addAll(computeClassPathParts(JavaScopes.PROVIDED, false, true, sourceSetsByName,
                excludeFromClasspathSet));
        deps.addAll(computeClassPathParts(JavaScopes.RUNTIME, false, true, sourceSetsByName,
                excludeFromClasspathSet));
        log.info("Have " + deps.size() + " dependent source sets for main");
        // Emit absolute source directories (and a hierarchical file:/... URI). maddi resolves relative source dirs
        // against the configured working directory, but the classpath parts are already absolute machine paths, so
        // relativizing the sources buys no portability -- it only coupled the run to the process CWD and produced
        // opaque "file:src/main/java" URIs. Absolute paths make the run independent of where mvn is launched from.
        List<Path> sourcePaths = project.getCompileSourceRoots().stream()
                .map(path -> Path.of(path).toAbsolutePath().normalize()).toList();
        if (!sourcePaths.isEmpty()) {
            Set<String> restrictToPackages = stringToSet(sourcePackages);

            SourceSet mainSourceSet = new SourceSetImpl.Builder()
                    .setName(projectName + "/main")
                    .setBuildUnit(buildUnit)
                    .setSourceDirectories(sourcePaths)
                    .setUri(sourcePaths.getFirst().toUri())
                    .setSourceEncoding(encoding)
                    .setRestrictToPackages(restrictToPackages)
                    .setDependencies(List.copyOf(deps))
                    .build();
            sourceSetsByName.put(mainSourceSet.name(), mainSourceSet);
        }
        deps.addAll(computeClassPathParts(JavaScopes.TEST, true, false, sourceSetsByName,
                excludeFromClasspathSet));
        log.info("Have " + deps.size() + " dependent source sets for test");
        List<Path> testSourcePaths = project.getTestCompileSourceRoots().stream()
                .map(path -> Path.of(path).toAbsolutePath().normalize()).toList();
        if (!testSourcePaths.isEmpty()) {
            Set<String> restrictToTestPackages = stringToSet(testSourcePackages);

            SourceSet testSourceSet = new SourceSetImpl.Builder()
                    .setName(projectName + "/test")
                    .setBuildUnit(buildUnit)
                    .setSourceDirectories(testSourcePaths)
                    .setUri(testSourcePaths.getFirst().toUri())
                    .setSourceEncoding(encoding)
                    .setTest(true)
                    .setRestrictToPackages(restrictToTestPackages)
                    .setDependencies(List.copyOf(deps))
                    .build();
            sourceSetsByName.put(testSourceSet.name(), testSourceSet);
        }

        return new ComputeDependencies.SourceSetDependencies("main", sourceSetsByName);
    }

    private Set<SourceSet> computeClassPathParts(String scope, boolean test, boolean runtimeOnly,
                                                 Map<String, SourceSet> sourceSetsByName, Set<String> excludeFromClasspathSet)
            throws DependencyResolutionException {

        // Create dependency request for this scope
        DependencyFilter classpathFilter = DependencyFilterUtils.classpathFilter(scope);
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setProject(project);

        // Resolve the dependencies
        DependencyResolutionRequest resolutionRequest = new DefaultDependencyResolutionRequest();
        resolutionRequest.setMavenProject(project);
        resolutionRequest.setRepositorySession(session.getRepositorySession());

        DependencyResolutionResult resolutionResult = dependenciesResolver.resolve(resolutionRequest);

        // Process resolution result
        log.debug("Computing class path parts for " + scope);
        return processDependencyNodes(resolutionResult.getDependencyGraph(), test, runtimeOnly, sourceSetsByName,
                excludeFromClasspathSet, 1);
    }

    private Set<SourceSet> processDependencyNodes(DependencyNode node, boolean test, boolean runtimeOnly,
                                                  Map<String, SourceSet> sourceSetsByName,
                                                  Set<String> excludeFromClasspathSet,
                                                  int indent) {
        Set<SourceSet> results = new HashSet<>();
        for (DependencyNode child : node.getChildren()) {
            Artifact artifact = child.getArtifact();
            if (artifact == null || artifact.getFile() == null) continue;
            // maddi keys a classpath source set by its jar file name and resolves it as "jar file: <name>"
            // (its own --write-input-configuration names jars this way too), so the part name must be the jar
            // file name, not the groupId:artifactId:version coordinate.
            String name = artifact.getFile().getName();
            // Flatten the whole subtree into direct dependencies. A classpath is flat, and nesting the transitive
            // deps under their parent -- combined with the name-dedup below -- would drop an already-seen dep from
            // its parent's child set, leaving it unreachable when maddi walks the graph to build the parse
            // classpath (e.g. slf4j-api under a provided slf4j binding never reaching the compile classpath).
            results.addAll(processDependencyNodes(child, test, runtimeOnly, sourceSetsByName,
                    excludeFromClasspathSet, indent + 1));
            if (!excludeFromClasspathSet.contains(artifact.getArtifactId())) {
                SourceSet existing = sourceSetsByName.get(name);
                if (existing != null) {
                    results.add(existing); // already created (possibly in an earlier scope); still a direct dep here
                } else {
                    URI uri = URI.create("file:" + artifact.getFile().getPath());
                    SourceSet sourceSet = new SourceSetImpl.Builder()
                            .setName(name)
                            .setUri(uri)
                            .setTest(test)
                            .setLibrary(true)
                            .setExternalLibrary(true)
                            .setPartOfJdk(false)
                            .setRuntimeOnly(runtimeOnly)
                            .setDependencies(List.of())
                            .build();
                    sourceSetsByName.put(name, sourceSet);
                    log.debug("Added class path part " + name);
                    results.add(sourceSet);
                }
            }
        }
        return results;
    }

    private static Set<String> stringToSet(String sourcePackages) {
        return sourcePackages == null || sourcePackages.isBlank() ? null :
                Arrays.stream(sourcePackages.split("[,;]\\s*"))
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toUnmodifiableSet());
    }
}
