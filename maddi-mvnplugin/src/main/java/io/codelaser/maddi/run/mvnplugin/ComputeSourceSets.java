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

        // Resolved ONCE. The four calls this replaces each re-resolved the whole graph and each got the same
        // one back: the scope was passed to a filter that was computed and then never used.
        DependencyNode dependencyGraph = resolveDependencies();

        Set<SourceSet> deps = new HashSet<>(computeClassPathParts(log, dependencyGraph, JavaScopes.COMPILE, false,
                sourceSetsByName, excludeFromClasspathSet));
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
        deps.addAll(computeClassPathParts(log, dependencyGraph, JavaScopes.TEST, true, sourceSetsByName,
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

    /**
     * The project's resolved dependency graph, once. Every scope reads the same graph -- Aether resolves it whole
     * and stamps each node with its DERIVED scope -- so which entries belong on which class path is a question
     * about the nodes, answered below, not about the resolution.
     */
    private DependencyNode resolveDependencies() throws DependencyResolutionException {
        DependencyResolutionRequest resolutionRequest = new DefaultDependencyResolutionRequest();
        resolutionRequest.setMavenProject(project);
        resolutionRequest.setRepositorySession(session.getRepositorySession());
        return dependenciesResolver.resolve(resolutionRequest).getDependencyGraph();
    }

    /**
     * The class path {@code javac} is given for one of the two compilations, as maddi source sets.
     *
     * <p>⛔⛔ <b>THE SCOPE FILTER WAS COMPUTED AND NEVER APPLIED.</b> The old code built a
     * {@link DependencyFilterUtils#classpathFilter} from the scope, dropped it on the floor, and then walked the
     * unfiltered graph -- so all four scope passes returned the SAME set, the first one (compile) created every
     * part, and the dedup-by-name below handed the rest back unchanged. The result: <b>one class path, used for
     * both compilations</b>, and the {@code test} and {@code runtimeOnly} flags never once set.
     *
     * <p>⚠ <b>MEASURED, on timefold-solver</b> (2026-08-19): {@code core/main} came out with <b>60</b>
     * dependencies where javac's own {@code -classpath} -- the one {@code --compile-log} records -- has
     * <b>12</b>. The 27 non-JDK extras are the whole test toolchain (junit, mockito, assertj, awaitility,
     * hamcrest), the logging backend (logback) and the JAXB <em>runtime</em>: nothing main is compiled against.
     * Nothing was MISSING, which is why this cost no error and survived: a class path that is too wide only
     * resolves types a stricter build would have rejected.
     *
     * <p>Two scopes, not four, and they are exactly {@code maven-compiler-plugin}'s two class paths:
     * {@code compile} ({@code MavenProject#getCompileClasspathElements}, i.e. compile + provided + system) and
     * {@code test} ({@code getTestClasspathElements}, i.e. everything). The {@code provided} pass was redundant
     * -- {@code classpathFilter(COMPILE)} already includes it -- and the {@code runtime} pass put runtime-scope
     * jars on MAIN's class path, where javac never sees them, while marking them {@code runtimeOnly}.
     */
    static Set<SourceSet> computeClassPathParts(Log log, DependencyNode dependencyGraph, String scope,
                                                boolean test, Map<String, SourceSet> sourceSetsByName,
                                                Set<String> excludeFromClasspathSet) {
        log.debug("Computing class path parts for " + scope);
        return processDependencyNodes(log, dependencyGraph, DependencyFilterUtils.classpathFilter(scope),
                new ArrayList<>(), test, sourceSetsByName, excludeFromClasspathSet);
    }

    private static Set<SourceSet> processDependencyNodes(Log log, DependencyNode node, DependencyFilter filter,
                                                         List<DependencyNode> parents, boolean test,
                                                         Map<String, SourceSet> sourceSetsByName,
                                                         Set<String> excludeFromClasspathSet) {
        Set<SourceSet> results = new HashSet<>();
        for (DependencyNode child : node.getChildren()) {
            Artifact artifact = child.getArtifact();
            if (artifact == null || artifact.getFile() == null) continue;
            String name = partName(artifact);
            // Flatten the whole subtree into direct dependencies. A classpath is flat, and nesting the transitive
            // deps under their parent -- combined with the name-dedup below -- would drop an already-seen dep from
            // its parent's child set, leaving it unreachable when maddi walks the graph to build the parse
            // classpath (e.g. slf4j-api under a provided slf4j binding never reaching the compile classpath).
            parents.addFirst(child);
            results.addAll(processDependencyNodes(log, child, filter, parents, test, sourceSetsByName,
                    excludeFromClasspathSet));
            parents.removeFirst();
            // ⚠ The filter is asked about the CHILD but the recursion above is not gated on it. Aether derives a
            // node's scope from its whole path (a compile dependency of a test dependency IS test-scoped), so a
            // rejected node's subtree is rejected node by node on its own merits; skipping the subtree outright
            // would be the same answer only as long as that stays true.
            if (!filter.accept(child, parents)) continue;
            if (!excludeFromClasspathSet.contains(artifact.getArtifactId())) {
                SourceSet existing = sourceSetsByName.get(name);
                if (existing != null) {
                    if (!artifact.getFile().getAbsoluteFile().toURI().equals(existing.uri())) {
                        // ⛔ NOT A TIDINESS PROBLEM: the name IS the identity the serialized configuration
                        // resolves every dependency edge by, so two files under one name silently removes every
                        // package the second provides. Same guard, same reason, as the Gradle plugin's.
                        log.warn(" -- class path name clash: '" + name + "' already means " + existing.uri()
                                 + ", so " + artifact.getFile() + " is DROPPED and the packages it provides"
                                 + " will not resolve");
                    }
                    results.add(existing); // already created (possibly in an earlier scope); still a direct dep here
                } else {
                    SourceSet sourceSet = PluginSourceSets.classPathPart(name, artifact.getFile(), test,
                            // Nothing on either of the two class paths above is runtime-only: they are the
                            // compile class paths, and a runtime-scope artifact reaches only the test one, where
                            // javac genuinely does read it.
                            false);
                    sourceSetsByName.put(name, sourceSet);
                    log.debug("Added class path part " + name);
                    results.add(sourceSet);
                }
            }
        }
        return results;
    }

    /**
     * A class path part's identity.
     *
     * <p>The jar file name, as before -- maddi's own {@code --write-input-configuration} names jars this way, and
     * every checked-in corpus configuration is written in it.
     *
     * <p>⛔ <b>EXCEPT WHEN THE ARTIFACT IS A DIRECTORY, WHERE THE FILE NAME IS NOT AN IDENTITY.</b> A reactor
     * sibling that has not been packaged resolves through Maven's {@code ReactorReader} to its
     * {@code target/classes}, so {@code getFile().getName()} is {@code "classes"} for EVERY sibling -- and the
     * "already have it" branch above then hands back the first one for all of them. This is the same defect the
     * Gradle plugin carried, where it cost 6 of pulsar's 7 siblings and 100 parse errors; it is reachable here
     * through {@code mvn -am}, or through any reactor build that has not run {@code install}.
     *
     * <p>The coordinate is prefixed rather than substituted so that the ordinary jar case is byte-identical to
     * what the plugin wrote before, and the file name is kept as a suffix because one module can contribute more
     * than one directory ({@code classes} and {@code test-classes}) to one class path.
     */
    private static String partName(Artifact artifact) {
        String fileName = artifact.getFile().getName();
        if (!artifact.getFile().isDirectory()) return fileName;
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + "/" + fileName;
    }

    /**
     * The Java API level this module is compiled against, or {@code 0} when the build model says nothing.
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
