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

    public record Result(List<JSourceSet> jSourceSets, List<SourceSet> jars) {
    }

    public record JSourceSet(CompileInvocation invocation, SourceSet sourceSet) {
    }

    private static Map<String, Integer> makeComponentStats(List<? extends CompileInvocation> list) {
        Map<String, Integer> countSuffix = new HashMap<>();
        for (CompileInvocation inv : list) {
            String destination = inv.destination();
            String[] split = destination.split(SEPARATOR);
            for (int i = 1; i < split.length - 1; ++i) {
                String suffix = combine(split, i, split.length);
                countSuffix.merge(suffix, 1, Integer::sum);
            }
        }
        return countSuffix;
    }

    public Result compute(List<? extends CompileInvocation> list) {
        Map<String, Integer> countSuffix = makeComponentStats(list);
        Map<String, String> jarFileToDestinationModuleJars = computeModuleJars(countSuffix, list);

        Map<String, SourceSet> sourceSetsByPath = new HashMap<>();
        Map<String, SourceSet> classPath = handleClasspath(list, sourceSetsByPath, jarFileToDestinationModuleJars);

        Map<String, SourceSet> sourceSetsByDestination = new HashMap<>();
        Map<String, Integer> duplicateNamePrevention = new HashMap<>();

        List<JSourceSet> jSourceSets = new LinkedList<>();
        for (CompileInvocation inv : list) {
            SourceSet sourceSet = createSourceSet(inv, countSuffix, sourceSetsByPath, sourceSetsByDestination,
                    jarFileToDestinationModuleJars, duplicateNamePrevention);

            Set<Path> sourceDirSet = new HashSet<>(sourceSet.sourceDirectories());
            // we remove source sets that are fully contained in this one
            jSourceSets.removeIf(set -> sourceDirSet.containsAll(set.sourceSet.sourceDirectories()));
            jSourceSets.add(new JSourceSet(inv, sourceSet));
            sourceSetsByPath.put(inv.destination(), sourceSet);
            // we remove classpath parts that are the output of source sets
            classPath.remove(inv.destination());
        }
        return new Result(jSourceSets, classPath.values().stream().sorted(Comparator.comparing(SourceSet::name)).toList());
    }

    private Map<String, String> computeModuleJars(Map<String, Integer> countSuffix, List<? extends CompileInvocation> list) {
        Map<String, String> moduleJarToDestination = new HashMap<>();
        Map<String, String> moduleNameToDestination = new HashMap<>();
        for (CompileInvocation inv : list) {
            String destination = inv.destination();
            ComputeNameResult cnr = computeName(countSuffix, destination);
            moduleNameToDestination.put(cnr.name, destination);

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
                                                          Map<String, String> jarFileToDestinationModuleJars) {
        Map<String, SourceSet> classPath = new HashMap<>();
        for (CompileInvocation inv : list) {
            String destination = inv.destination();
            if (inv.classpath() != null) {
                for (String part : inv.classpath()) {
                    if (part.endsWith(".jar")) {
                        handleJarInClasspath(sourceSetsByPath, part, classPath, destination);
                    } else {
                        Path path = Path.of(part);
                        if (Files.isDirectory(path)) {
                            handleDirectoryInClasspath(sourceSetsByPath, part, classPath);
                        }
                    }
                }
            }
            if (inv.modulePath() != null) {
                for (String part : inv.modulePath()) {
                    if (part.endsWith(".jar")) {
                        if (!jarFileToDestinationModuleJars.containsKey(part)) {
                            handleJarInClasspath(sourceSetsByPath, part, classPath, destination);
                        }
                    }
                }
            }
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
                                             String destination) {
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

    private SourceSet createSourceSet(CompileInvocation inv,
                                      Map<String, Integer> countSuffix,
                                      Map<String, SourceSet> sourceSetsByPath,
                                      Map<String, SourceSet> sourceSetsByDestination,
                                      Map<String, String> jarFileToDestinationModulePath,
                                      Map<String, Integer> duplicateNamePrevention) {
        String destination = inv.destination();
        ComputeNameResult result = computeName(countSuffix, destination);
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
                        if (!classpathPart.contains("resources")) {
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
                        String srcModule = jarFileToDestinationModulePath.get(modulePart);
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
        SourceSet sourceSet = new SourceSetImpl.Builder()
                .setName(name)
                .setSourceDirectories(List.copyOf(sourceDirs))
                .setUri(uri)
                .setSourceEncoding(encoding)
                .setTest(test)
                .setDependencies(dependencies)
                .build();
        sourceSetsByDestination.put(destination, sourceSet);
        return sourceSet;
    }

    private static ComputeNameResult computeName(Map<String, Integer> countSuffix, String destination) {
        String name = "/main";
        String testName = null;
        String[] split = destination.split(SEPARATOR);
        for (int i = split.length - 1; i > 0; --i) {
            String part = split[i];
            if (testName == null && TEST_NAMES.contains(part)) {
                testName = part;
            }
            String suffix = combine(split, i, split.length);
            Integer freq = countSuffix.get(suffix);
            if (freq != null && freq <= 2) {
                name = split[i] + (testName != null ? "/" + testName : "/main");
                LOGGER.info("{} -> {}", destination, name);
                break;
            }
        }
        return new ComputeNameResult(name, testName);
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
