package io.codelaser.maddi.run.mvnplugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.*;
import io.codelaser.maddi.run.config.util.ComputeDependencies;
import io.codelaser.maddi.run.config.util.PluginSourceSets;
import io.codelaser.maddi.run.main.PluginOptions;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
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
        List<Path> sourcePaths = existingDirectories(project.getCompileSourceRoots(), "main");
        if (!sourcePaths.isEmpty()) {
            Set<String> restrictToPackages = PluginOptions.splitToSetOrNull(sourcePackages);

            SourceSet mainSourceSet = PluginSourceSets.sourceSet(projectName + "/main", buildUnit, sourcePaths,
                    Path.of(project.getBuild().getOutputDirectory()), encoding, false, restrictToPackages,
                    sourceRelease(false));
            if (mainSourceSet != null) {
                mainSourceSet = mainSourceSet.withDependencies(List.copyOf(deps));
                sourceSetsByName.put(mainSourceSet.name(), mainSourceSet);
            }
        }
        deps.addAll(computeClassPathParts(JavaScopes.TEST, true, false, sourceSetsByName,
                excludeFromClasspathSet));
        log.info("Have " + deps.size() + " dependent source sets for test");
        List<Path> testSourcePaths = existingDirectories(project.getTestCompileSourceRoots(), "test");
        if (!testSourcePaths.isEmpty()) {
            Set<String> restrictToTestPackages = PluginOptions.splitToSetOrNull(testSourcePackages);

            SourceSet testSourceSet = PluginSourceSets.sourceSet(projectName + "/test", buildUnit, testSourcePaths,
                    Path.of(project.getBuild().getTestOutputDirectory()), encoding, true,
                    restrictToTestPackages, sourceRelease(true));
            if (testSourceSet != null) {
                testSourceSet = testSourceSet.withDependencies(List.copyOf(deps));
                sourceSetsByName.put(testSourceSet.name(), testSourceSet);
            }
        }

        return new ComputeDependencies.SourceSetDependencies("main", sourceSetsByName);
    }

    /**
     * Maven's compile source roots are DECLARED, not necessarily present. Both callers used to guard only on the
     * list being empty, so a declared-but-absent directory produced a source set over nothing, and the written
     * inputConfiguration named a path that does not exist.
     * <p>
     * guava is the case that exposed this: its ROOT pom sets {@code <testSourceDirectory>test</testSourceDirectory>}
     * for every module, but the {@code guava} module has no {@code test/} -- its tests live in the sibling
     * {@code guava-tests} module. Maven itself tolerates this and compiles nothing; we recorded
     * {@code .../guava/guava/test} in the configuration, which is what {@code task corpus:verify} then reported as
     * the single unresolvable path out of 3949.
     * <p>
     * Dropping it loses nothing -- a directory that does not exist contributes no compilation units -- and it keeps
     * the configuration to paths that can actually be parsed. Absence is logged rather than silent: on a project
     * whose generated-source root has simply not been generated yet, the line is the clue that the config was
     * written before the build produced it.
     */
    private List<Path> existingDirectories(List<String> roots, String which) {
        List<Path> all = roots.stream().map(path -> Path.of(path).toAbsolutePath().normalize()).toList();
        List<Path> existing = all.stream().filter(Files::isDirectory).toList();
        all.stream().filter(p -> !existing.contains(p))
                .forEach(p -> log.info("Skipping declared but absent " + which + " source directory " + p));
        return existing;
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
                    SourceSet sourceSet = PluginSourceSets.classPathPart(name, artifact.getFile(), test,
                            runtimeOnly);
                    sourceSetsByName.put(name, sourceSet);
                    log.debug("Added class path part " + name);
                    results.add(sourceSet);
                }
            }
        }
        return results;
    }

    /**
     * The Java API level this module is compiled against, or {@code 0} when the pom says nothing.
     *
     * <p>⛔ <b>WITHOUT IT THE PARSE RUNS ON WHATEVER JDK MADDI HAPPENS TO BE, NOT ON THE ONE THE CORPUS
     * TARGETS</b> — and every API removed since then reads as "cannot find symbol", drops the compilation unit,
     * and can cost the whole {@code ParseResult}. {@code --compile-log} reads it straight off the javac line;
     * a plugin has to ask the build model, and neither plugin asked at all.
     *
     * <p>⚠ <b>PROPERTIES ONLY, AND THAT IS A KNOWN LIMIT.</b> These four are what the overwhelming majority of
     * poms state (they are what {@code maven-compiler-plugin} itself reads by default), but a pom that configures
     * {@code <release>} inside the plugin's own {@code <configuration>} block instead is not seen here and falls
     * back to 0, i.e. to today's behaviour. Reading that would mean resolving the effective plugin configuration,
     * which is a different and much larger piece of Maven plumbing; a wrong release would be worse than none.
     *
     * <p>{@code release} before {@code source}: it is the only one that also constrains the API the code is
     * compiled against, which is exactly the question. Test before main for the test set, because a module may
     * compile its tests at a higher level than what it publishes.
     */
    private int sourceRelease(boolean test) {
        java.util.Properties properties = project.getProperties();
        for (String key : test
                ? new String[]{"maven.compiler.testRelease", "maven.compiler.testSource",
                "maven.compiler.release", "maven.compiler.source"}
                : new String[]{"maven.compiler.release", "maven.compiler.source"}) {
            int release = PluginSourceSets.parseRelease(properties.getProperty(key));
            if (release > 0) return release;
        }
        return 0;
    }

}
