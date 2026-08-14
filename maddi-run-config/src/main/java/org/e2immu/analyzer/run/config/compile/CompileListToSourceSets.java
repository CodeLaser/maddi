package org.e2immu.analyzer.run.config.compile;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Turns a list of {@link CompileInvocation}s (parsed {@code javac} or {@code kotlinc} command lines) into a
 * source-set graph plus the library jars. The engine is language-independent; see {@link CompileInvocation}
 * for the two Kotlin-provided signals it honours ({@code moduleName}, {@code friendPaths}).
 *
 * <p>Formerly {@code JavacListToSourceSets} (openjdk); generalized here so the kotlin front-end reuses it. The
 * algorithm is unchanged for Java. Kotlin-aware additions, both no-ops for Java:
 * <ul>
 *   <li>{@code -Xfriend-paths} outputs become dependency edges and mark the set as a test set;</li>
 *   <li>the source-root package regex accepts a semicolon-less Kotlin {@code package} declaration.</li>
 * </ul>
 */
public class CompileListToSourceSets {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompileListToSourceSets.class);

    private static final String SEPARATOR = FileSystems.getDefault().getSeparator();

    /**
     * @param buildRoot the directory the build ran from — the InputConfiguration's working directory, and what
     *                  every relative path in the configuration resolves against. ⛔ It is carried HERE because
     *                  this is where it is decided (configured or derived), and a configuration that does not
     *                  know it defaults to {@code "."}, i.e. the JVM's own directory: measured on
     *                  elasticsearch, that made {@code writeModuleInfo} refuse the corpus with
     *                  <i>"Refusing to write outside the project: …/es-phase3/libs/core resolves outside
     *                  …/codelaser-refactor-graalpy"</i> — the analysed tree reported as foreign to itself.
     */
    public record Result(List<JSourceSet> jSourceSets, List<SourceSet> jars, String buildRoot) {
    }

    public record JSourceSet(CompileInvocation invocation, SourceSet sourceSet) {
    }

    /**
     * The same output directory, under the same name, as a library: readable by everyone, parsed by nobody.
     * <p>
     * ⚠ ONE READER. Both places that stop parsing a source set need exactly this — the absorbed sets here, and
     * the {@code build.exclude_source_sets} demotion in {@link CompileListToInputConfiguration} — and a second
     * copy of it is how the two would drift. A DEFECT FIXED IN ONE READER IS NOT FIXED.
     */
    static SourceSet asLibrary(SourceSet sourceSet) {
        return new SourceSetImpl.Builder()
                .setName(sourceSet.name())
                .setBuildUnit(sourceSet.buildUnit())
                .setSourceDirectories(List.of())
                .setUri(sourceSet.uri())
                .setSourceEncoding(sourceSet.sourceEncoding())
                .setLibrary(true)
                .setExternalLibrary(true)
                .build();
    }

    private final String configuredBuildRoot;

    /** The build root is derived from the run; see {@link #CompileListToSourceSets(String)} for why to pass it. */
    public CompileListToSourceSets() {
        this(null);
    }

    /**
     * @param buildRoot the directory the build was run from, or {@code null} to derive it as the longest common
     *                  ancestor of this run's build units.
     *                  <p>⛔⛔ PASS IT WHENEVER THE CALLER KNOWS IT. Source-set names are the build unit's path
     *                  below this root, so the root decides them — and the derived one is a function of WHICH
     *                  modules were compiled: a run over {@code libs/core} and {@code libs/x-content} alone has
     *                  common ancestor {@code .../libs} and names them {@code core/main}, where the full build
     *                  names them {@code libs/core/main}. Narrowing a parse would silently rename every source
     *                  set it kept. The build directory is already configured, and it does not move.
     */
    public CompileListToSourceSets(String buildRoot) {
        this.configuredBuildRoot = buildRoot == null || buildRoot.isBlank() ? null
                : buildRoot.endsWith(SEPARATOR) ? buildRoot.substring(0, buildRoot.length() - SEPARATOR.length())
                : buildRoot;
    }

    /**
     * The directory every compiled module sits under: the longest common ancestor of this run's build units. It
     * is what makes a module's own path — and therefore its source sets' names — readable rather than absolute.
     * {@code null} when no build unit could be determined at all.
     */
    private static String commonBuildRoot(Map<String, String> buildUnitByDestination) {
        String[] first = null;
        int common = 0;
        for (String buildUnit : new TreeSet<>(buildUnitByDestination.values())) {
            String[] parts = buildUnit.split(SEPARATOR);
            if (first == null) {
                first = parts;
                common = parts.length;
                continue;
            }
            int i = 0;
            while (i < common && i < parts.length && parts[i].equals(first[i])) ++i;
            common = i;
        }
        return first == null ? null : combine(first, 0, common);
    }

    public Result compute(List<? extends CompileInvocation> list) {
        // computed once: computeBuildUnit warns when it cannot decide, and it should say so once per destination
        Map<String, String> buildUnitByDestination = new LinkedHashMap<>();
        for (CompileInvocation inv : list) {
            buildUnitByDestination.computeIfAbsent(inv.destination(), CompileListToSourceSets::computeBuildUnit);
        }
        String derived = commonBuildRoot(buildUnitByDestination);
        String buildRoot = configuredBuildRoot == null ? derived : configuredBuildRoot;
        LOGGER.info("Build root of {} build unit(s): {} ({})", new HashSet<>(buildUnitByDestination.values()).size(),
                buildRoot, configuredBuildRoot == null ? "derived; pass one to keep names stable across a narrower"
                        + " parse" : "configured, derived would have been " + derived);
        // a sibling's PACKAGED jar, wherever it is named (classpath or module path); keyed by path, see below
        Map<String, String> jarFileToDestination = computePackagedJars(list);
        // ⚠ THE --module-path MAPPING WINS WHERE BOTH FIRE. It is the older rule and the one the modular corpora
        // are validated against, so the addition above can only ever ADD edges, never re-point an existing one.
        jarFileToDestination.putAll(computeModuleJars(buildRoot, buildUnitByDestination, list));

        Map<String, SourceSet> sourceSetsByPath = new HashMap<>();
        Map<String, SourceSet> classPath = handleClasspath(list, sourceSetsByPath, jarFileToDestination);

        Map<String, SourceSet> sourceSetsByDestination = new HashMap<>();
        Map<String, Integer> duplicateNamePrevention = new HashMap<>();

        List<JSourceSet> jSourceSets = new LinkedList<>();
        // ⛔⛔ ABSORBED, NOT GONE. The containment rule below drops a source set whose source directories another
        // invocation also compiles -- and TWO INVOCATIONS OVER ONE SOURCE TREE HAVE TWO DESTINATIONS. Everything
        // that was compiled against the loser's output directory still names it, so it has to remain readable,
        // as a library. Measured on elasticsearch: `libs/native` is compiled twice from the same 38 files, once
        // for real and once with `-proc:only`; the second absorbed the first, and `libs/native/main` -- named by
        // 208 of 348 source sets -- existed nowhere in the configuration. Nothing said so. The parse said so, a
        // day later: org.elasticsearch.nativeaccess does not exist, one compilation unit dropped, and
        // Summary.parseResult() refuses the WHOLE ParseResult over it.
        Map<String, SourceSet> absorbed = new LinkedHashMap<>();
        for (CompileInvocation inv : list) {
            SourceSet sourceSet = createSourceSet(inv, buildRoot, buildUnitByDestination, sourceSetsByPath,
                    sourceSetsByDestination, jarFileToDestination, duplicateNamePrevention);

            Set<Path> sourceDirSet = new HashSet<>(sourceSet.sourceDirectories());
            // we remove source sets that are fully contained in this one
            jSourceSets.removeIf(set -> {
                if (!sourceDirSet.containsAll(set.sourceSet.sourceDirectories())) return false;
                absorbed.put(set.sourceSet.name(), asLibrary(set.sourceSet));
                return true;
            });
            jSourceSets.add(new JSourceSet(inv, sourceSet));
            sourceSetsByPath.put(inv.destination(), sourceSet);
            // we remove classpath parts that are the output of source sets
            classPath.remove(inv.destination());
        }
        // ⚠ A set absorbed EARLY can be re-created later under the same name (the containment rule is per
        // invocation, and the list is walked once), so only those with no surviving namesake become libraries.
        Set<String> survivors = jSourceSets.stream().map(js -> js.sourceSet.name()).collect(Collectors.toSet());
        List<SourceSet> demoted = absorbed.entrySet().stream().filter(e -> !survivors.contains(e.getKey()))
                .map(Map.Entry::getValue).toList();
        if (!demoted.isEmpty()) {
            LOGGER.info("{} source set(s) were absorbed by another invocation over the same sources; each stays"
                        + " readable as a library, because its output directory is on other source sets'"
                        + " class paths: {}", demoted.size(), demoted.stream().map(SourceSet::name).toList());
            demoted.forEach(library -> classPath.put(library.uri().toString(), library));
        }
        return new Result(jSourceSets,
                classPath.values().stream().sorted(Comparator.comparing(SourceSet::name)).toList(), buildRoot);
    }

    /**
     * A reactor sibling's PACKAGED jar, mapped back to the destination that produced it — keyed by PATH.
     *
     * <p>⛔ <b>BY PATH, BECAUSE THE NAME CANNOT WORK ON MAVEN.</b> {@link #computeModuleName} matches a jar's file
     * name against {@code <module>/main}, which holds for gradle (artifact name = module name) and is simply false
     * for maven, where the artifactId and the module directory are different strings — jenkins ships
     * {@code jenkins-core-2.574-SNAPSHOT.jar} out of a module directory named {@code core}, and no prefix of the
     * file name is ever {@code core}. The signal that IS reliable is already in hand: a build tool writes a
     * module's jar into the same directory it writes that module's classes into ({@code <mod>/target/classes} and
     * {@code <mod>/target/x.jar}).
     *
     * <p>⚠ Keying on the output ROOT rather than on "somewhere under the module" is what keeps a jar VENDORED
     * inside a module ({@code <mod>/lib/foo.jar}) from being mistaken for that module's own output.
     *
     * <p>⛔ <b>A PACKAGED JAR IS NOT ALWAYS PRODUCTION OUTPUT.</b> That was assumed here, and maven's
     * {@code maven-jar-plugin:test-jar} falsifies it: a module that publishes its test fixtures writes
     * {@code <mod>/target/<artifact>-tests.jar} into the SAME output root as its main jar. Keying on the output
     * root alone therefore handed the test-jar to the module's MAIN source set, and the fixtures it carries —
     * every {@code testutil} and {@code testdomain} type a sibling's tests statically import — silently left the
     * parse. javac then reported {@code package ... does not exist}, fabricated an error symbol, and the scanner
     * dereferenced it as a method: {@code "Unexpected symbol for unqualified call to 'assertCode'"}, 79 of them
     * over 9 compilation units on timefold, with the true cause a hundred lines earlier in the log.
     *
     * <p>So both kinds are collected, and a jar carrying a TEST CLASSIFIER resolves to the test destination when
     * the module has one. ⚠ The classifier is the reliable signal precisely because maven puts it AFTER the
     * version: a module actually named {@code ...-integration-test} ships {@code ...-integration-test-1.0.jar},
     * which does not end in {@code -test.jar}. Falling back to the main destination keeps the previous behaviour
     * for every build that publishes no test-jar.
     */
    private static Map<String, String> computePackagedJars(List<? extends CompileInvocation> list) {
        Map<String, String> mainDestinationByOutputRoot = new HashMap<>();
        Map<String, String> testDestinationByOutputRoot = new HashMap<>();
        for (CompileInvocation inv : list) {
            String destination = inv.destination();
            int lastSeparator = destination.lastIndexOf(SEPARATOR);
            if (lastSeparator < 0) continue;
            String outputRoot = destination.substring(0, lastSeparator);
            if (testSourceSetName(lastPart(destination)) != null) {
                testDestinationByOutputRoot.putIfAbsent(outputRoot, destination);
            } else {
                mainDestinationByOutputRoot.putIfAbsent(outputRoot, destination);
            }
        }
        Map<String, String> jarToDestination = new HashMap<>();
        for (CompileInvocation inv : list) {
            for (List<String> paths : Arrays.asList(inv.classpath(), inv.modulePath())) {
                if (paths == null) continue;
                for (String part : paths) {
                    if (!part.endsWith(".jar")) continue;
                    int lastSeparator = part.lastIndexOf(SEPARATOR);
                    if (lastSeparator < 0) continue;
                    String outputRoot = part.substring(0, lastSeparator);
                    String destination = null;
                    if (hasTestClassifier(lastPart(part))) {
                        destination = testDestinationByOutputRoot.get(outputRoot);
                    }
                    if (destination == null) {
                        destination = mainDestinationByOutputRoot.get(outputRoot);
                    }
                    // a module's own jar on its own classpath is not a dependency on itself
                    if (destination != null && !destination.equals(inv.destination())) {
                        jarToDestination.putIfAbsent(part, destination);
                    }
                }
            }
        }
        if (!jarToDestination.isEmpty()) {
            LOGGER.info("Computed {} packaged-jar -> source-set entries (a reactor sibling named as a jar rather"
                        + " than as a class directory): {}", jarToDestination.size(),
                    jarToDestination.keySet().stream().map(CompileListToSourceSets::lastPart).sorted().toList());
        }
        return jarToDestination;
    }

    /**
     * A jar file name carrying a test classifier, i.e. maven's {@code test-jar} goal or gradle's equivalent.
     *
     * <p>⚠ Matched on the FILE NAME's suffix, which is safe because a classifier follows the version:
     * {@code timefold-solver-core-999-SNAPSHOT-tests.jar} is a test-jar, while a module whose artifactId ends
     * in {@code -test} ships {@code ...-test-999-SNAPSHOT.jar} and is not.
     */
    private static boolean hasTestClassifier(String jarFileName) {
        return jarFileName.endsWith("-tests.jar") || jarFileName.endsWith("-test.jar");
    }

    private Map<String, String> computeModuleJars(String buildRoot, Map<String, String> buildUnitByDestination,
                                                  List<? extends CompileInvocation> list) {
        Map<String, String> moduleJarToDestination = new HashMap<>();
        Map<String, String> moduleNameToDestination = new HashMap<>();
        for (CompileInvocation inv : list) {
            String destination = inv.destination();
            ComputeNameResult cnr = computeName(buildRoot, buildUnitByDestination, destination);
            moduleNameToDestination.put(cnr.name, destination);
            // ⚠ AND UNDER ITS LEAF FORM TOO. computeModuleName below matches a jar file name against
            // "<module>/main", and a nested module's name now carries its whole path ("libs/core/main"), which no
            // jar file name ever looks like. Registering the leaf keeps that match working for both layouts.
            int slash = cnr.name.lastIndexOf('/');
            if (slash > 0) {
                moduleNameToDestination.putIfAbsent(lastPart(cnr.name.substring(0, slash)) + cnr.name.substring(slash),
                        destination);
            }

            if (inv.modulePath() != null) {
                for (String modulePart : inv.modulePath()) {
                    if (!moduleJarToDestination.containsKey(modulePart) && modulePart.endsWith(".jar")) {
                        String moduleDestination = computeModuleName(modulePart, moduleNameToDestination);
                        if (moduleDestination != null) {
                            moduleJarToDestination.put(modulePart, moduleDestination);
                        }
                    }
                }
            }
        }
        LOGGER.info("Computed {} moduleJarToDestination entries", moduleJarToDestination.size());
        return moduleJarToDestination;
    }

    // modulePart = .../maddi-support-0.8.2.jar
    // moduleNameToDestination = maddi-support/main -> .../maddi-support/build/...
    private static final Pattern PATTERN = Pattern.compile("[.-]");

    private String computeModuleName(String modulePart, Map<String, String> moduleNameToDestination) {
        int lastSlash = modulePart.lastIndexOf('/');
        String lastModulePart = lastSlash < 0 ? modulePart : modulePart.substring(lastSlash + 1);
        Matcher m = PATTERN.matcher(lastModulePart);
        while (m.find()) {
            String prefix = lastModulePart.substring(0, m.start());
            String inMap = moduleNameToDestination.get(prefix + "/main");
            if (inMap != null) return inMap;
        }
        return null; // nothing found
    }


    private static Map<String, SourceSet> handleClasspath(List<? extends CompileInvocation> list,
                                                          Map<String, SourceSet> sourceSetsByPath,
                                                          Map<String, String> jarFileToDestination) {
        // A jar is a module iff it appears on some invocation's --module-path. Precompute this over ALL invocations
        // so the flag is correct regardless of the order we first meet the jar, or whether it also sits on some
        // classpath elsewhere: a modular dependency must reach the module path for its module's requires to resolve.
        Set<String> moduleJarNames = list.stream()
                .filter(inv -> inv.modulePath() != null)
                .flatMap(inv -> inv.modulePath().stream())
                .filter(SourceSetImpl::isArchive)
                .map(CompileListToSourceSets::lastPart)
                .collect(Collectors.toSet());
        Set<String> skippedClassPathParts = new TreeSet<>();
        Map<String, SourceSet> classPath = new HashMap<>();
        for (CompileInvocation inv : list) {
            String destination = inv.destination();
            if (inv.classpath() != null) {
                for (String part : inv.classpath()) {
                    // ⛔ NOT endsWith(".jar"): see SourceSetImpl.ARCHIVE_EXTENSIONS. A .nar on pulsar's classpath
                    // matched neither branch below and was dropped WITHOUT A WORD, costing 1,831 compilation units.
                    if (SourceSetImpl.isArchive(part)) {
                        // ⛔ a sibling's packaged jar is that sibling's SOURCE SET, not a library: making it one
                        // puts every type it holds in the parse twice, once from source and once from bytecode.
                        if (!jarFileToDestination.containsKey(part)) {
                            handleJarInClasspath(sourceSetsByPath, part, classPath, destination, moduleJarNames);
                        }
                    } else {
                        Path path = Path.of(part);
                        if (Files.isDirectory(path)) {
                            handleDirectoryInClasspath(sourceSetsByPath, part, classPath);
                        } else {
                            // ⚠ AND IT SAYS SO. This branch used to be the silence: whatever javac was given and
                            // maddi has no case for now leaves a trace in the log rather than a missing package
                            // 40,000 lines later.
                            skippedClassPathParts.add(part);
                        }
                    }
                }
            }
            if (inv.modulePath() != null) {
                for (String part : inv.modulePath()) {
                    if (SourceSetImpl.isArchive(part)) {
                        if (!jarFileToDestination.containsKey(part)) {
                            handleJarInClasspath(sourceSetsByPath, part, classPath, destination, moduleJarNames);
                        }
                    }
                }
            }
        }
        if (!skippedClassPathParts.isEmpty()) {
            LOGGER.warn("Skipped {} classpath part(s): neither a known archive {} nor an existing directory."
                        + " If javac reports a missing package, look here FIRST: {}",
                    skippedClassPathParts.size(), SourceSetImpl.ARCHIVE_EXTENSIONS, skippedClassPathParts);
        }
        return classPath;
    }

    private static void handleDirectoryInClasspath(Map<String, SourceSet> sourceSetsByPath,
                                                   String part,
                                                   Map<String, SourceSet> classPath) {
        SourceSet inMap = sourceSetsByPath.get(part);
        if (inMap == null) {
            URI uri = URI.create("file:" + part);
            SourceSet sourceSet = new SourceSetImpl.Builder()
                    .setName(part)
                    .setSourceDirectories(List.of())
                    .setUri(uri)
                    .setLibrary(true)
                    .setExternalLibrary(true)
                    .build();
            sourceSetsByPath.put(part, sourceSet);
            LOGGER.info("Add class directory: {}", uri);
            classPath.put(part, sourceSet);
        }
    }

    private static void handleJarInClasspath(Map<String, SourceSet> sourceSetsByPath,
                                             String part,
                                             Map<String, SourceSet> classPath,
                                             String destination,
                                             Set<String> moduleJarNames) {
        String lastPart = lastPart(part);
        SourceSet inMap = classPath.get(lastPart);
        URI uri = URI.create("file:" + part);
        if (inMap == null) {
            SourceSet sourceSet = new SourceSetImpl.Builder()
                    .setName(lastPart)
                    .setSourceDirectories(List.of())
                    .setUri(uri)
                    .setLibrary(true)
                    .setExternalLibrary(true)
                    .setModule(moduleJarNames.contains(lastPart))
                    .build();
            classPath.put(lastPart, sourceSet);
            sourceSetsByPath.put(part, sourceSet);
            LOGGER.info("Create jar: {} -> {}", lastPart, destination);
        } else if (!uri.equals(inMap.uri())) {
            LOGGER.info("Name clash: {} vs {}", uri, inMap.uri());
            sourceSetsByPath.put(part, inMap);
        }
    }

    private static final Set<String> TEST_NAMES = Set.of("test", "test-classes", "testFixtures", "test-annotations",
            "integrationTest", "intTest",
            "functionalTest", "funcTest",
            "acceptanceTest", "systemTest", "smokeTest", "contractTest");

    // the output directory a build tool writes PRODUCTION classes into: gradle's source set is named "main",
    // maven writes target/classes
    private static final Set<String> MAIN_OUTPUT_NAMES = Set.of("main", "classes");

    /**
     * The source set's kind, when it is a test kind: {@code null} for production code.
     *
     * <p>⛔⛔ <b>IT IS READ FROM THE OUTPUT DIRECTORY, AND FROM NOTHING ABOVE IT.</b> This used to scan every
     * component of the destination path, so a <i>project</i> directory called {@code test} decided the question
     * for a {@code main} source set underneath it. Measured on elasticsearch:
     * {@code x-pack/plugin/esql/compute/test/…/java/main} and {@code esql/qa/testFixtures/…/java/main} are
     * production code declared as tests. A source set's kind is a property of the set, and the only path
     * component that names the set is the last one.
     *
     * <p>⛔⛔ <b>AND A LITERAL LIST CANNOT BE COMPLETE, WHICH IS THE OTHER HALF OF THE SAME DEFECT.</b> A Gradle
     * source set's output directory IS its name, and a build may declare any number of them: elasticsearch has
     * {@code internalClusterTest} (47 source sets), {@code javaRestTest}, {@code yamlRestTest}. None was in the
     * list, so all 47 came out as production code — and an absent {@code test} flag is the hardest kind of wrong,
     * because every consumer has a defensible default and none of them can tell {@code false} from
     * <i>not stated</i>. The convention those names follow is a suffix, so that is what is matched.
     *
     * <p>⚠ MEASURED, both directions at once: on elasticsearch's 348 source sets the old rule gets <b>49</b>
     * flags wrong (47 tests as production, 2 production as tests); this rule reproduces all 348.
     */
    private static String testSourceSetName(String outputDirectory) {
        if (MAIN_OUTPUT_NAMES.contains(outputDirectory)) return null;
        if (TEST_NAMES.contains(outputDirectory)) return outputDirectory;
        return outputDirectory.toLowerCase().endsWith("test") ? outputDirectory : null;
    }

    /**
     * The source set's kind, which for Gradle is simply the output directory: the directory a Gradle source set
     * compiles into IS its name.
     *
     * <p>⛔ IT IS NOT "main OR a test kind", and assuming so collided six source sets on elasticsearch. A
     * multi-release project has {@code build/classes/java/main} alongside {@code main22}, {@code main25},
     * {@code main26}, {@code main27} — real, separately compiled source sets whose leaves are neither
     * {@code main} nor test-shaped. Folding them all to {@code main} handed out {@code entitlement/main2},
     * {@code main3}, {@code main4} by arrival order, throwing away the one thing the directory was telling us.
     *
     * <p>Maven is the only translation: it writes production classes to {@code target/classes}, which is
     * {@code main} everywhere else in this system. {@code target/test-classes} keeps its own name, as it did.
     */
    private static String sourceSetKind(String outputDirectory) {
        return "classes".equals(outputDirectory) ? "main" : outputDirectory;
    }

    // the directory a build tool writes its compiled output into, directly inside the module directory
    private static final Set<String> BUILD_OUTPUT_NAMES = Set.of("target", "build", "out");

    /*
    Derives the build unit -- the module the source sets belong to -- from the javac -d destination, by dropping
    the build tool's output directory and everything below it:

        .../timefold-solver/core/target/classes            -> .../timefold-solver/core
        .../timefold-solver/core/target/test-classes       -> .../timefold-solver/core
        .../quarkus/deployment/build/classes/java/main     -> .../quarkus/deployment

    The module directory is what makes a build unit identifiable here. Unlike the source set name, it is unique,
    and it pairs a module's main and test sets: the names cannot, because computeName below falls back to a
    frequency heuristic and disambiguates collisions with a counter, so main2 and test-classes2 need not belong
    to the same module (in timefold-solver they do not).

    Returns null when no output directory is recognised: an unknown grouping must not be guessed at.
     */
    private static String computeBuildUnit(String destination) {
        String[] split = destination.split(SEPARATOR);
        for (int i = split.length - 1; i > 0; --i) {
            if (BUILD_OUTPUT_NAMES.contains(split[i])) {
                return combine(split, 0, i);
            }
        }
        LOGGER.warn("Cannot determine the build unit of {}: no build output directory in the path", destination);
        return null;
    }

    /**
     * The build tool's output directory itself ({@code .../core/target}, {@code .../deployment/build}), or
     * {@code null} when the destination does not sit inside one. The counterpart of {@link #computeBuildUnit},
     * which returns the module directory above it; both are here so that {@link #BUILD_OUTPUT_NAMES} stays the
     * single place that knows what a build output directory is called.
     */
    static Path buildOutputDirectory(String destination) {
        String[] split = destination.split(SEPARATOR);
        for (int i = split.length - 1; i > 0; --i) {
            if (BUILD_OUTPUT_NAMES.contains(split[i])) {
                return Path.of(combine(split, 0, i + 1));
            }
        }
        return null;
    }

    private SourceSet createSourceSet(CompileInvocation inv,
                                      String buildRoot,
                                      Map<String, String> buildUnitByDestination,
                                      Map<String, SourceSet> sourceSetsByPath,
                                      Map<String, SourceSet> sourceSetsByDestination,
                                      Map<String, String> jarFileToDestination,
                                      Map<String, Integer> duplicateNamePrevention) {
        String destination = inv.destination();
        ComputeNameResult result = computeName(buildRoot, buildUnitByDestination, destination);
        URI uri = URI.create("file:" + inv.destination());
        // sourcePath() may be null (javac with no -sourcepath); source dirs are then inferred from source files
        List<String> sourcePath = inv.sourcePath();
        List<Path> sourceDirs = new ArrayList<>(sourcePath == null ? List.of()
                : sourcePath.stream().map(Path::of).toList());

        List<SourceSet> dependencies = new LinkedList<>();
        if (inv.classpath() != null) {
            for (String classpathPart : inv.classpath()) {
                assert !classpathPart.isBlank();
                if (!classpathPart.equals(destination)) {
                    SourceSet sourceSet = sourceSetsByPath.get(classpathPart);
                    if (sourceSet != null) {
                        dependencies.add(sourceSet);
                    } else {
                        // ⛔ THE SAME FALLBACK THE MODULE-PATH BRANCH BELOW ALWAYS HAD. A sibling named as a
                        // packaged jar is the same edge as one named as a class directory; only the spelling
                        // differs, and which spelling a build uses is not the analysed project's decision.
                        String srcModule = jarFileToDestination.get(classpathPart);
                        SourceSet srcDependency = srcModule == null ? null : sourceSetsByDestination.get(srcModule);
                        if (srcDependency != null) {
                            // ⚠ deduplicated, unlike the module-path branch: a build may name BOTH a sibling's
                            // class directory and its jar on one classpath, and they are one dependency.
                            if (!dependencies.contains(srcDependency)) dependencies.add(srcDependency);
                        } else if (!classpathPart.contains("resources")) {
                            LOGGER.warn("Cannot find classpath part {}", classpathPart);
                        }
                    }
                }
            }
        }
        if (inv.modulePath() != null) {
            for (String modulePart : inv.modulePath()) {
                assert !modulePart.isBlank();
                if (!modulePart.equals(destination)) {
                    SourceSet sourceSet = sourceSetsByPath.get(modulePart);
                    if (sourceSet != null) {
                        dependencies.add(sourceSet);
                    } else {
                        String srcModule = jarFileToDestination.get(modulePart);
                        SourceSet srcDependency = srcModule == null ? null : sourceSetsByDestination.get(srcModule);
                        if (srcDependency != null) {
                            dependencies.add(srcDependency);
                        } else {
                            LOGGER.warn("Cannot find module path part {}", modulePart);
                        }
                    }
                }
            }
        }
        // Kotlin: -Xfriend-paths outputs (a test set pointing at its main output) become dependency edges.
        for (String friend : inv.friendPaths()) {
            if (!friend.equals(destination)) {
                SourceSet sourceSet = sourceSetsByPath.get(friend);
                if (sourceSet != null && !dependencies.contains(sourceSet)) {
                    dependencies.add(sourceSet);
                }
            }
        }

        if (sourceDirs.isEmpty() && !inv.sourceFiles().isEmpty()) {
            sourceDirs.addAll(disjointSourceDirs(inv.sourceFiles()));
        }

        Charset encoding = inv.encoding() == null ? null : Charset.forName(inv.encoding());
        int newIndex = duplicateNamePrevention.merge(result.name(), 1, Integer::sum);
        String name = newIndex == 1 ? result.name() : result.name() + newIndex;
        boolean test = result.testName() != null || !inv.friendPaths().isEmpty();
        // A compiled source set is a named module iff it was compiled in module mode: maven/gradle use a --module-path
        // only for a (main) source set that declares a module-info. Test source sets are patched into their main
        // module and stay non-module. (The source files aren't on the -X "Command line options:" line, so we can't key
        // off a module-info.java argument; the module path is the reliable log-derivable signal.)
        boolean isModule = !test && inv.modulePath() != null && !inv.modulePath().isEmpty();
        SourceSet sourceSet = new SourceSetImpl.Builder()
                .setName(name)
                .setBuildUnit(buildUnitByDestination.get(destination))
                .setSourceDirectories(List.copyOf(sourceDirs))
                .setUri(uri)
                .setSourceEncoding(encoding)
                .setTest(test)
                .setModule(isModule)
                .setDependencies(dependencies)
                .build();
        sourceSetsByDestination.put(destination, sourceSet);
        return sourceSet;
    }

    /** The language directory Gradle inserts as {@code build/classes/<language>/<kind>}; javac's is the default. */
    private static final String DEFAULT_LANGUAGE = "java";

    /**
     * The source set's name: <b>{@code <module>/<kind>}</b>, where the module is its build unit's path below the
     * build root and the kind is its output directory ({@code libs/core/main},
     * {@code x-pack/plugin/analytics/internalClusterTest}, {@code core/test-classes}).
     *
     * <p>⛔⛔ <b>THE NAME IS THE IDENTITY, SO IT MUST BE A PROPERTY OF THE SOURCE SET.</b> {@link SourceSet}'s own
     * contract says so — <i>"source sets are identified by their name() throughout the system, including in
     * serialized dependency references"</i>. What stood here was a frequency heuristic: it walked up the
     * destination until a path suffix occurred at most twice <i>across the whole run</i>, so the name depended on
     * which other projects happened to be compiled beside it. Measured on elasticsearch, that produced
     * {@code es-phase3/main} for {@code :server} — <b>the name of the checkout directory</b> — handed
     * {@code server/main} to a different source set, and gave 54 of 348 sets an order-dependent counter
     * ({@code core/main2}). Adding one unrelated project renamed existing ones.
     *
     * <p>The build unit is already computed, already required to be unique across the build, and cannot be
     * changed by compiling something else. Taken relative to the build root it is short, readable, and stable.
     *
     * <p>⚠ <b>THE ONE DISCRIMINATOR THAT IS KEPT IS THE LANGUAGE</b>, because a mixed module really does compile
     * one Gradle source set twice: {@code proj/build/classes/kotlin/main} and {@code proj/build/classes/java/main}
     * are one module, one kind, two source sets with different parsers. Java is the unmarked case, so a Java-only
     * build never carries a language segment and the Kotlin set becomes {@code proj/kotlin/main}.
     */
    private static ComputeNameResult computeName(String buildRoot, Map<String, String> buildUnitByDestination,
                                                 String destination) {
        String[] split = destination.split(SEPARATOR);
        String leaf = split[split.length - 1];
        String testName = testSourceSetName(leaf);
        String language = language(split);
        String name = module(buildRoot, buildUnitByDestination.get(destination), split)
                      + "/" + (DEFAULT_LANGUAGE.equals(language) ? "" : language + "/")
                      + sourceSetKind(leaf);
        LOGGER.debug("{} -> {}", destination, name);
        return new ComputeNameResult(name, testName);
    }

    /** {@code .../build/classes/<language>/<kind>}, else the default: maven and kotlinc jars have no such level. */
    private static String language(String[] destination) {
        return destination.length >= 3 && "classes".equals(destination[destination.length - 3])
                ? destination[destination.length - 2] : DEFAULT_LANGUAGE;
    }

    /**
     * The build unit's path below the build root. Falls back to the leaf when the two coincide (a single-module
     * build, or the root project itself), and to the directory holding the build output when the build unit could
     * not be determined at all — {@link #computeBuildUnit} has already said so.
     */
    private static String module(String buildRoot, String buildUnit, String[] destination) {
        if (buildUnit == null) {
            return destination.length >= 2 ? destination[destination.length - 2] : destination[0];
        }
        if (buildRoot == null || buildRoot.isEmpty() || buildUnit.equals(buildRoot)) return lastPart(buildUnit);
        if (buildUnit.startsWith(buildRoot + SEPARATOR)) {
            return buildUnit.substring(buildRoot.length() + SEPARATOR.length());
        }
        // outside the build root: a configured root can be wrong or partial, and an absolute path is not a name
        LOGGER.warn("Build unit {} is not below the build root {}; naming it after its own directory", buildUnit,
                buildRoot);
        return lastPart(buildUnit);
    }

    private record ComputeNameResult(String name, String testName) {
    }

    private List<Path> disjointSourceDirs(List<String> sourceFiles) {
        Map<Path, Integer> paths = new HashMap<>();
        for (String sourceFileString : sourceFiles) {
            Path path = Path.of(sourceFileString);
            Path fromMap = findPrefix(path, paths);
            Path prefix;
            if (fromMap == null) {
                try {
                    prefix = loadFileAndComputePrefixFromPackage(path);
                } catch (IOException ioe) {
                    prefix = null; // mostly to allow tests to continue running
                }
            } else {
                prefix = fromMap;
            }
            if (prefix != null) {
                paths.merge(prefix, 1, Integer::sum);
            }
        }
        return paths.entrySet().stream().sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    // the trailing ';' is optional so a semicolon-less Kotlin `package a.b.c` matches too
    private static final Pattern PACKAGE = Pattern.compile("package ([a-zA-Z0-9_.]+);?");

    private Path loadFileAndComputePrefixFromPackage(Path path) throws IOException {
        String content = Files.readString(path);
        Matcher m = PACKAGE.matcher(content);
        if (m.find()) {
            String packageName = m.group(1);
            int parts = packageName.split("\\.").length;
            Path parent = path.getParent();
            for (int i = 0; i < parts; ++i) parent = parent.getParent();
            return parent;
        }
        return null; // try next one
    }

    private Path findPrefix(Path path, Map<Path, Integer> paths) {
        for (Path p : paths.keySet()) {
            if (path.startsWith(p)) return p;
        }
        return null;
    }

    private static String lastPart(String path) {
        int lastIndex = path.lastIndexOf(SEPARATOR);
        if (lastIndex >= 0) return path.substring(lastIndex + SEPARATOR.length());
        return path;
    }

    private static String combine(String[] parts, int from, int to) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = from; i < to; ++i) {
            if (first) first = false;
            else sb.append(SEPARATOR);
            sb.append(parts[i]);
        }
        return sb.toString();
    }
}
