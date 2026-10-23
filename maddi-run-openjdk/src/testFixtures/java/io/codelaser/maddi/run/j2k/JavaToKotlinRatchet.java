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

package io.codelaser.maddi.run.j2k;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.print.kotlin.KotlinTranslator;
import io.codelaser.maddi.cst.print.kotlin.KotlinPrintCode;
import io.codelaser.maddi.cst.print.translate.PrintMessage;
import io.codelaser.maddi.cst.print.kotlin.KotlinPrintOptions;
import io.codelaser.maddi.cst.print.kotlin.KotlinWorld;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.run.config.util.JsonStreaming;
import io.codelaser.maddi.util.corpus.Corpora;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * ⛔ THE RATCHET FOR JAVA → KOTLIN TRANSLATION, shared: a corpus's main sources, parsed by the openjdk front end,
 * printed by {@code maddi-cst-print-kotlin}, and judged by the Kotlin compiler. The numbers may move, but not
 * quietly: a metric that gets WORSE fails, and one that gets BETTER fails too, until the new value is written into
 * the ratchet file, so every step of progress is recorded by the commit that made it.
 * <p>
 * Two users: maddi-run-openjdk's TestJavaToKotlinFernflower (the printer alone) and maddi-mod's
 * TestJavaToKotlinFernflowerNullability (the printer with the nullability analysis's verdicts), each with its own
 * ratchet file. The test JVM needs {@code -Dmaddi.test.kotlinCompilerClasspath}: a {@code kotlinCompiler}
 * configuration in the module's build, see maddi-run-openjdk's build.gradle.kts.
 *
 * <h2>Two compiles, because a syntax error hides everything else</h2>
 * Kotlin reports the syntax errors of a module and stops before type checking. So:
 * <ol>
 *   <li><b>everything</b> is compiled: the {@code syntax error} messages per file give {@code syntaxErrors} and
 *   {@code syntaxCleanFiles};</li>
 *   <li>only the syntax-clean files are compiled again, against the corpus's ORIGINAL class files for everything
 *   else (Kotlin sources shadow the class files of the same name), which gives {@code compilingFiles}: files the
 *   Kotlin compiler accepts as a drop-in replacement for their Java.</li>
 * </ol>
 * The second compile's error total is reported, not ratcheted.
 *
 * <h2>Then to class files, and the corpus's own tests</h2>
 * The type checker is not the whole compiler: the JVM back end has errors of its own (a property's getter with the
 * JVM name of a Java method), and a file it accepts may still behave differently from its Java. So:
 * <ol start="3">
 *   <li>the compiling files are compiled to class files. A file the back end rejects is left out, and so is a file
 *   that then no longer compiles, until the rest does: {@code bytecodeFiles}, a drop-in replacement for their Java
 *   all the way to the class file;</li>
 *   <li>the corpus's test classes run, in a child JVM with JUnit's console launcher, once against the original
 *   classes (a baseline kept in ~/.cache, run again only when they or the class path change) and once
 *   with the Kotlin classes in front of them: {@code testsPassing}. The original's count, and each
 *   test that passes there and fails with Kotlin, go into the report and {@code tests/}.</li>
 *   <li>the test sources are translated too, and compiled to class files against the Kotlin main classes, leaving
 *   out what does not compile as in 3 (its original test class stays): {@code testBytecodeFiles};</li>
 *   <li>the tests run once more, the translated test classes and the Kotlin main classes in front of the originals:
 *   {@code translatedTestsPassing}, Kotlin tests judging the Kotlin translation. Only the test classes this can
 *   change run again: those translated, and those that use one; the others' outcome is the previous stage's.</li>
 * </ol>
 * The tests run when the corpus has a built test source set and the test JVM has
 * {@code -Dmaddi.test.junitConsoleClasspath} (the {@code junitConsole} configuration beside {@code kotlinCompiler});
 * otherwise {@code testsPassing} is neither measured nor ratcheted. Nor with {@link #COMPILE_ONLY}, a working cycle's
 * switch.
 * <p>
 * The output lands in {@code build/j2k/<name>/}: the translated sources, both compilers' raw output, and
 * {@code report.txt} with the errors by kind. Read the report before tightening.
 * <p>
 * A run may leave some metrics {@code reportedOnly}: measured and written into the report, not ratcheted. Without
 * nullability verdicts every declaration prints non-null, so a class that newly compiles often fails its tests at
 * runtime (a builder's unset lateinit property, a null into a non-null parameter): there, a printer fix that gets
 * more files compiling lowers the test counts. The runs without verdicts ratchet the printer's metrics and only
 * report {@link #TESTS}; the runs with verdicts ratchet them all.
 *
 * @param name         the run's name, for the output directory and the messages
 * @param ratchetFile  the recorded numbers, relative to the module directory; null for a measurement that is not
 *                     ratcheted ({@link #measureOnly})
 * @param reportedOnly the metrics this run measures and reports without ratcheting
 * @param runTests     whether the corpus's tests run (stages 4 and 6); without, the test sources are still translated
 *                     and compiled (stage 5). For a run that would only report the test counts and cannot afford them:
 *                     guava's three passes over its 865,000 tests take over an hour
 */
public record JavaToKotlinRatchet(String name, Path ratchetFile, Set<String> reportedOnly, boolean runTests) {

    /** The tests run. */
    public JavaToKotlinRatchet(String name, Path ratchetFile, Set<String> reportedOnly) {
        this(name, ratchetFile, reportedOnly, true);
    }

    /** The test counts: what a translation without verdicts reports and does not ratchet. */
    public static final Set<String> TESTS = Set.of("testsPassing", "translatedTestsPassing");

    /**
     * The switch for a working cycle: {@code J2K_COMPILE_ONLY=true} (passed on by the build as
     * {@code -Dmaddi.j2k.compileOnly}) translates and compiles as always, test sources included, and runs none of
     * the corpus's tests. {@link #TESTS} are then not measured, so neither recorded nor ratcheted: guava's three test
     * passes take some 75 of its 80 minutes, its type errors the first 8. Run the full ratchet before writing a record.
     */
    public static final String COMPILE_ONLY = "J2K_COMPILE_ONLY";

    private static boolean compileOnly() {
        String value = System.getProperty("maddi.j2k.compileOnly");
        if (value == null || value.isBlank()) value = System.getenv(COMPILE_ONLY);
        return value != null && !value.isBlank() && !"false".equalsIgnoreCase(value.trim());
    }

    /** Every metric ratcheted. */
    public JavaToKotlinRatchet(String name, Path ratchetFile) {
        this(name, ratchetFile, Set.of());
    }

    /**
     * The same translation, compilation, tests and report in {@code build/j2k/<name>/}, held to no ratchet file: for a
     * measurement beside the ratchets (an ablation of the analyses, a scoreboard), which must never move their numbers.
     */
    public static JavaToKotlinRatchet measureOnly(String name) {
        return new JavaToKotlinRatchet(name, null, Set.of());
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaToKotlinRatchet.class);

    /** name -> direction. "=" must not move, "<=" may only go down, ">=" may only go up. */
    private static final Map<String, String> DIRECTION = new LinkedHashMap<>();

    static {
        DIRECTION.put("types", "=");
        DIRECTION.put("printerCrashes", "<=");
        DIRECTION.put("syntaxErrors", "<=");
        DIRECTION.put("syntaxCleanFiles", ">=");
        DIRECTION.put("compilingFiles", ">=");
        DIRECTION.put("bytecodeFiles", ">=");
        DIRECTION.put("testsPassing", ">=");
        DIRECTION.put("testBytecodeFiles", ">=");
        DIRECTION.put("translatedTestsPassing", ">=");
    }

    // kotlinc's plain renderer: "<path>.kt:<line>:<column>: error: <message>", the path relative to the working
    // directory, and "syntax error: ..." in lower case
    private static final Pattern CRASHED_ON = Pattern.compile("While analysing (\\S+?\\.kt)");
    private static final Pattern DIAGNOSTIC = Pattern.compile("^(.+?\\.kt):(\\d+):(\\d+): error: (.*)$");

    /**
     * A parsed corpus: the inspector, the parse, the configuration, the main source set and its primary types,
     * sorted by name.
     */
    /**
     * {@code siblings}: the corpus's other main source sets are modules of its own, whose class directories go on
     * the class path (nacos: core's Kotlin calls api's and common's classes). Not guava's, whose second main source
     * set holds the very classes translated.
     */
    public record Corpus(InputConfigurationImpl config, JavaInspector javaInspector, Summary summary,
                         SourceSet main, List<TypeInfo> types, List<TypeInfo> testTypes, SourceSet test,
                         boolean junit4, boolean siblings) {
    }

    /** Parses a corpus of {@link Corpora#oss}, which must be built (its class files are compile 2's class path). */
    public static Corpus parse(String corpusName) throws IOException {
        return switch (corpusName) {
            // guava's reactor: `guava/9` compiles guava/src (and the module-info) and absorbs `guava/main`, whose classes
            // stay on the class path as a library; its tests are guava-tests', not guava-testlib's own
            case "guava" -> parse(corpusName, "guava/9", "guava-tests/test-classes", true);
            default -> parse(corpusName, null, null, false);
        };
    }

    /**
     * As {@link #parse(String)}, with the main and test source sets named: a multi-module corpus (guava) has more than
     * one of each, and the first is not always the one translated. Null for the first non-test source set, and for
     * every test source set's types with the first test source set's tests run. {@code junit4}: the tests are JUnit 3
     * or 4 (guava's), run by JUnit's vintage engine beside Jupiter.
     */
    public static Corpus parse(String corpusName, String mainName, String testName, boolean junit4) throws IOException {
        return parse(corpusName, mainName, testName, junit4, false);
    }

    /** As {@link #parse(String, String, String, boolean)}; {@code siblings}: see {@link Corpus#siblings()}. */
    public static Corpus parse(String corpusName, String mainName, String testName, boolean junit4, boolean siblings)
            throws IOException {
        Corpora.Corpus corpus = Corpora.oss(corpusName);
        // the locator, not a hand-written assumption: it honours -Dmaddi.corpus.required, which slowTest sets
        corpus.requireConfig();
        InputConfigurationImpl config = JsonStreaming.objectMapper()
                .readValue(corpus.config().toFile(), InputConfigurationImpl.class);
        SourceSet main = config.sourceSets().stream().filter(s -> !s.test())
                .filter(s -> mainName == null || mainName.equals(s.name())).findFirst().orElseThrow();
        SourceSet test = testName == null ? null : config.sourceSets().stream()
                .filter(s -> s.test() && testName.equals(s.name())).findFirst().orElseThrow();
        Path originalClasses = Path.of(main.uri());
        assertTrue(Files.isDirectory(originalClasses), corpusName + " is not built: no " + originalClasses
                                                       + " -- task corpus:build NAME=" + corpusName);
        JavaInspector javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(config);
        javaInspector.preload("java.base::java.util");
        Summary summary = javaInspector.parse(new JavaInspector.ParseOptions.Builder()
                .setFailFast(false).setIgnoreModule(true).build());
        List<TypeInfo> types = summary.parseResult().primaryTypes().stream()
                .filter(t -> t.compilationUnit().sourceSet() == main
                             || t.compilationUnit().sourceSet().name().equals(main.name()))
                .sorted(Comparator.comparing(TypeInfo::fullyQualifiedName))
                .toList();
        List<TypeInfo> testTypes = summary.parseResult().primaryTypes().stream()
                .filter(t -> t.compilationUnit().sourceSet().test()
                             && (test == null || t.compilationUnit().sourceSet().name().equals(test.name())))
                .sorted(Comparator.comparing(TypeInfo::fullyQualifiedName))
                .toList();
        return new Corpus(config, javaInspector, summary, main, types, testTypes, test, junit4, siblings);
    }

    /** Translates, compiles twice, writes the report, and holds the numbers to the ratchet file. */
    public void run(Corpus corpus, KotlinPrintOptions given) throws Exception {
        // main and test sources are compiled as one Kotlin code base: a member's uses are all in sight
        KotlinPrintOptions options = given.world().isFile() ? given.withWorld(KotlinWorld.of(
                java.util.stream.Stream.concat(corpus.types().stream(), corpus.testTypes().stream()).toList())) : given;
        Path out = Path.of("build/j2k", name);
        deleteRecursively(out);
        Printed main = print(corpus, corpus.types(), options, out.resolve("src"));
        judge(corpus, out, main, () -> print(corpus, corpus.testTypes(), options, out.resolve("test-src")));
    }

    /** The translation of the test sources, made when the tests run. */
    @FunctionalInterface
    private interface TestTranslation {
        Printed get() throws IOException;
    }

    /**
     * A translation made elsewhere (IntelliJ J2K, for the KotlinConf scoreboard, CodeLaser/maddi-mod#36), judged by the
     * same rules: the .kt files under {@code mainKotlin} are copied into {@code build/j2k/<name>/src}, and those under
     * {@code testKotlin}, when there are any, into {@code test-src}. Without translated tests, compile 4 and the
     * translated-test run are left out, and the report says so. For {@link #measureOnly}: nothing is ratcheted.
     */
    public void judgeExternal(Corpus corpus, Path mainKotlin, Path testKotlin) throws Exception {
        Path out = Path.of("build/j2k", name);
        deleteRecursively(out);
        Printed main = new Printed(copyKotlin(mainKotlin, out.resolve("src")), List.of(), List.of());
        List<Path> tests = testKotlin == null ? List.of() : copyKotlin(testKotlin, out.resolve("test-src"));
        judge(corpus, out, main, tests.isEmpty() ? null : () -> new Printed(tests, List.of(), List.of()));
    }

    private static List<Path> copyKotlin(Path from, Path to) throws IOException {
        if (!Files.isDirectory(from)) return List.of();
        List<Path> copied = new ArrayList<>();
        try (java.util.stream.Stream<Path> walk = Files.walk(from)) {
            for (Path kt : walk.filter(p -> p.toString().endsWith(".kt")).sorted().toList()) {
                Path target = to.resolve(from.relativize(kt).toString());
                Files.createDirectories(target.getParent());
                Files.copy(kt, target);
                copied.add(target.toAbsolutePath().normalize());
            }
        }
        return copied;
    }

    /** Compiles the main translation twice and to class files, runs the corpus's tests, reports, and ratchets. */
    private void judge(Corpus corpus, Path out, Printed main, TestTranslation testTranslation) throws Exception {
        List<Path> files = main.files;
        List<String> crashes = main.crashes;
        List<PrintMessage<KotlinPrintCode>> messages = main.messages;

        // compile 1: everything, for the syntax errors
        List<String> classPath = new ArrayList<>(libraries(corpus.config()));
        if (corpus.siblings()) classPath.addAll(siblingClasses(corpus));
        Compiled all = compile(files, classPath, out.resolve("compile-all"));
        Map<Path, Long> syntaxPerFile = all.errors.stream().filter(d -> d.message.toLowerCase().startsWith("syntax error"))
                .collect(Collectors.groupingBy(d -> d.file, TreeMap::new, Collectors.counting()));
        List<Path> syntaxClean = files.stream().filter(f -> !syntaxPerFile.containsKey(f)).toList();

        // compile 2: the syntax-clean files, against the original classes for the rest
        List<String> withOriginals = new ArrayList<>(classPath);
        withOriginals.add(Path.of(corpus.main().uri()).toString());
        Compiled clean = compile(syntaxClean, withOriginals, out.resolve("compile-clean"));
        Set<Path> withErrors = clean.errors.stream().map(d -> d.file).collect(Collectors.toCollection(HashSet::new));
        // a file the compiler crashed on was not accepted either, whatever diagnostics it did not get to print
        clean.crashes.forEach(c -> {
            Matcher m = CRASHED_ON.matcher(c);
            if (m.find()) withErrors.add(Path.of(m.group(1)).toAbsolutePath().normalize());
        });
        long compiling = syntaxClean.stream().filter(f -> !withErrors.contains(f)).count();

        // compile 3: the compiling files to class files, leaving out what the back end rejects, and what the
        // original classes, the tests' among them, cannot extend
        Optional<SourceSet> testSet = corpus.test() != null ? Optional.of(corpus.test())
                : corpus.config().sourceSets().stream().filter(SourceSet::test).findFirst();
        List<String> behind = new ArrayList<>(withOriginals);
        testSet.map(t -> Path.of(t.uri())).filter(Files::isDirectory).ifPresent(t -> behind.add(t.toString()));
        List<Path> compilingList = syntaxClean.stream().filter(f -> !withErrors.contains(f)).toList();
        Bytecode bytecode = toBytecode(compilingList, withOriginals, behind, out.resolve("compile-classes"));

        Map<String, Long> measured = new LinkedHashMap<>();
        measured.put("types", (long) corpus.types().size());
        measured.put("printerCrashes", (long) crashes.size());
        measured.put("syntaxErrors", syntaxPerFile.values().stream().mapToLong(Long::longValue).sum());
        measured.put("syntaxCleanFiles", (long) syntaxClean.size());
        measured.put("compilingFiles", compiling);
        measured.put("bytecodeFiles", (long) bytecode.files.size());

        // the corpus's own tests, against the original classes and with the Kotlin classes in front of them
        String testReport = "";
        String runner = System.getProperty("maddi.test.junitConsoleClasspath");
        String vintage = System.getProperty("maddi.test.junitVintageClasspath");
        if (corpus.junit4() && runner != null && vintage != null && !vintage.isBlank()) {
            runner = runner + java.io.File.pathSeparator + vintage;
        }
        boolean compileOnly = compileOnly() || !runTests;
        if (testSet.isPresent() && Files.isDirectory(Path.of(testSet.get().uri()))
            && (compileOnly || runner != null && !runner.isBlank())) {
            Path testClasses = Path.of(testSet.get().uri());
            // the tests translated as well (compile 4), first: which test classes they change decides what the
            // translated-test run runs again
            Printed tests = testTranslation == null ? null : testTranslation.get();
            Bytecode testBytecode = null;
            Set<String> affected = Set.of();
            if (tests != null) {
                List<String> testClassPath = new ArrayList<>(classPath);
                testClassPath.add(bytecode.classes.toAbsolutePath().toString());
                testClassPath.add(Path.of(corpus.main().uri()).toString());
                testClassPath.add(testClasses.toString());
                testBytecode = toBytecode(tests.files, testClassPath, testClassPath, out.resolve("compile-tests"));
                affected = affected(testBytecode.classes, testClasses);
            }
            Tests original = null, rest = null;
            if (compileOnly) {
                testReport = "\ntests: not run (" + (runTests ? "compile only: " + COMPILE_ONLY
                        : "this run's test counts would be reported only, and cost over an hour") + ")\n";
            } else {
                original = baseline(corpus, testClasses, runner, out.resolve("tests/original"));
                // in two runs, the test classes the translated tests change and the rest, which the translated-test
                // run then takes as they are
                List<Path> kotlinClasses = List.of(bytecode.classes);
                Path dir = out.resolve("tests/kotlin");
                rest = runTests(corpus, kotlinClasses, testClasses, runner, dir.resolve("rest"),
                        affected.isEmpty() ? null : new Only(affected, false));
                Tests kotlin = rest.plus(affected.isEmpty() ? Tests.NONE : runTests(corpus, kotlinClasses, testClasses,
                        runner, dir.resolve("affected"), new Only(affected, true)));
                measured.put("testsPassing", (long) kotlin.passed.size());
                testReport = testReport(original, kotlin, out.resolve("tests"), "with Kotlin");
            }

            if (tests == null) {
                testReport += "\ntest sources translated: none given (compile 4 and the translated tests left out)\n";
            } else {
                measured.put("testBytecodeFiles", (long) testBytecode.files.size());
                testReport += "\ntest sources translated: " + corpus.testTypes().size() + " types, "
                              + tests.crashes.size() + " printer crashes" + bytecodeReport(testBytecode, "compile 4 (the "
                              + "test sources to class files)");
                if (!compileOnly) {
                    Tests translated = rest.plus(affected.isEmpty() ? Tests.NONE : runTests(corpus,
                            List.of(testBytecode.classes, bytecode.classes), testClasses, runner,
                            out.resolve("tests/translated"), new Only(affected, true)));
                    measured.put("translatedTestsPassing", (long) translated.passed.size());
                    testReport += testReport(original, translated, out.resolve("tests"), "translated tests")
                                  + "  run again with the translated tests: " + affected.size()
                                  + " test classes (translated, or using one); the others as with Kotlin\n";
                }
                if (!tests.crashes.isEmpty()) testReport += "  test printer crashes:\n" + tests.crashes.stream()
                        .map(c -> "    " + c + "\n").collect(Collectors.joining());
            }
        } else {
            testReport = "\ntests: not run (" + (testSet.isEmpty() ? "no test source set"
                    : runner == null || runner.isBlank() ? "no -Dmaddi.test.junitConsoleClasspath"
                    : "test source set not built: " + testSet.get().uri()) + ")\n";
        }

        String report = report(measured, crashes, all, clean)
                        + bytecodeReport(bytecode, "compile 3 (the compiling files to class files)") + testReport
                        + messageCounts(messages);
        Files.writeString(out.resolve("report.txt"), report);
        Files.write(out.resolve("messages.txt"), messages.stream().map(PrintMessage::toString).toList());
        LOGGER.info("\n{}", report);
        ratchet(measured);
    }

    private record Printed(List<Path> files, List<String> crashes, List<PrintMessage<KotlinPrintCode>> messages) {
    }

    /** Each compilation unit of {@code types} as a Kotlin file under {@code src}: what the CLI's translation writes. */
    private static Printed print(Corpus corpus, List<TypeInfo> types, KotlinPrintOptions options, Path src)
            throws IOException {
        KotlinTranslator.Result result = KotlinTranslator.translate(corpus.javaInspector().runtime(), types, options, src);
        return new Printed(result.files(), result.crashes(), result.messages());
    }

    /** The class directories of the corpus's other main source sets, the built ones. */
    private static List<String> siblingClasses(Corpus corpus) {
        return corpus.config().sourceSets().stream()
                .filter(s -> !s.test() && s != corpus.main() && "file".equals(s.uri().getScheme()))
                .map(s -> Path.of(s.uri()))
                .filter(Files::isDirectory)
                .map(Path::toString)
                .toList();
    }

    /** Every jar the configuration names; the JDK comes from the compiler's own JVM. */
    private static List<String> libraries(InputConfigurationImpl config) {
        return config.classPathParts().stream()
                .filter(s -> !s.partOfJdk() && "file".equals(s.uri().getScheme()))
                .map(s -> Path.of(s.uri()).toString())
                .toList();
    }

    // ---------------------------------------------------------------- the Kotlin compiler

    private record Diagnostic(Path file, int line, String message) {
    }

    /** {@code crashes}: kotlinc's own exceptions ("exception: ..."), the compiler's defect rather than ours. */
    private record Compiled(int exitCode, List<Diagnostic> errors, List<String> crashes) {
    }

    private static void crashLines(Compiled c, String which, StringBuilder sb) {
        if (!c.crashes.isEmpty()) {
            sb.append('\n').append(which).append(": kotlinc crashed (exit ").append(c.exitCode).append("):\n");
            c.crashes.forEach(x -> sb.append("  ").append(x, 0, Math.min(300, x.length())).append('\n'));
        }
    }

    /**
     * kotlinc in a child JVM (see build.gradle.kts for why). The arguments go through an @-file: 199 paths do not
     * belong on a command line.
     */
    private static Compiled compile(List<Path> files, List<String> classPath, Path dir) throws Exception {
        Files.createDirectories(dir);
        if (files.isEmpty()) return new Compiled(0, List.of(), List.of());
        String compilerClassPath = compilerClassPath();
        List<String> cp = new ArrayList<>(classPath);
        cp.addFirst(stdlib());

        List<String> args = new ArrayList<>(List.of("-no-stdlib", "-no-reflect", "-jvm-target", "21",
                "-classpath", String.join(java.io.File.pathSeparator, cp),
                "-d", dir.resolve("classes").toAbsolutePath().toString()));
        files.forEach(f -> args.add(f.toString()));
        Path argFile = dir.resolve("kotlinc.args");
        Files.write(argFile, args.stream().map(a -> "\"" + a.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .toList());

        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        Path log = dir.resolve("kotlinc.log");
        // -Xss: K2's type substitution recurses deep on generic-heavy code (timefold's constraint streams), and
        // overflowed the launcher's default stack
        Process process = new ProcessBuilder(java.toString(), "-Xmx3g", "-Xss32m", "-cp", compilerClassPath,
                "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler", "@" + argFile.toAbsolutePath())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        int exit = process.waitFor();

        List<Diagnostic> errors = new ArrayList<>();
        List<String> crashes = new ArrayList<>();
        for (String line : Files.readAllLines(log)) {
            if (line.startsWith("exception: ")) crashes.add(line);
            Matcher m = DIAGNOSTIC.matcher(line);
            if (m.matches()) {
                errors.add(new Diagnostic(Path.of(m.group(1)).toAbsolutePath().normalize(), Integer.parseInt(m.group(2)),
                        m.group(4)));
            }
        }
        // A compiler that failed without one parsable diagnostic crashed, or the renderer changed: either way the
        // counts below would describe nothing, so stop here.
        if (exit != 0 && errors.isEmpty()) {
            fail("kotlinc exited " + exit + " without a diagnostic this test can read; see " + log.toAbsolutePath());
        }
        return new Compiled(exit, errors, crashes);
    }

    private static String compilerClassPath() {
        String compilerClassPath = System.getProperty("maddi.test.kotlinCompilerClasspath");
        assertTrue(compilerClassPath != null && !compilerClassPath.isBlank(),
                "no -Dmaddi.test.kotlinCompilerClasspath: run this through Gradle");
        return compilerClassPath;
    }

    /** The Kotlin standard library the compiler came with: what compiled classes run against. */
    private static String stdlib() {
        String compilerClassPath = compilerClassPath();
        return Arrays.stream(compilerClassPath.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches("kotlin-stdlib-[0-9.]+\\.jar"))
                .findFirst().orElseThrow(() -> new AssertionError("no kotlin-stdlib in " + compilerClassPath));
    }

    /** The files compiled to class files, the directory they are in, and the files left out, per round. */
    private record Bytecode(List<Path> files, Path classes, List<List<Path>> leftOut) {
    }

    /**
     * Compile 3. A round that fails leaves out the files with errors, and the next round compiles the rest: a file
     * that needs one left out now fails in turn (against the original class, which lacks the Kotlin's members). So
     * does a round whose classes an original Java class cannot extend ({@link #finalOverridden}): one in a directory
     * of {@code behind}, the class path the Kotlin classes will stand in front of when the tests run.
     */
    private static Bytecode toBytecode(List<Path> files, List<String> classPath, List<String> behind, Path dir)
            throws Exception {
        List<Path> remaining = new ArrayList<>(files);
        List<List<Path>> leftOut = new ArrayList<>();
        for (int round = 0; round < 20 && !remaining.isEmpty(); round++) {
            deleteRecursively(dir);
            Compiled c = compile(remaining, classPath, dir);
            if (c.exitCode == 0) {
                Map<Path, String> overridden = finalOverridden(remaining, dir.resolve("classes"), behind);
                if (overridden.isEmpty()) return new Bytecode(remaining, dir.resolve("classes"), leftOut);
                Files.write(dir.resolveSibling(dir.getFileName() + "-round" + (round + 1) + ".log"),
                        overridden.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).toList());
                leftOut.add(List.copyOf(new TreeSet<>(overridden.keySet())));
                remaining.removeAll(overridden.keySet());
                continue;
            }
            Set<Path> failing = c.errors.stream().map(d -> d.file).collect(Collectors.toCollection(TreeSet::new));
            c.crashes.forEach(x -> {
                Matcher m = CRASHED_ON.matcher(x);
                if (m.find()) failing.add(Path.of(m.group(1)).toAbsolutePath().normalize());
            });
            failing.retainAll(remaining);
            // the next round starts afresh: this round's errors, why its files were left out, stay beside it
            Files.copy(dir.resolve("kotlinc.log"), dir.resolveSibling(dir.getFileName() + "-round" + (round + 1) + ".log"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            if (failing.isEmpty()) fail("compile 3 failed without an error in a file it compiled; see " + dir);
            leftOut.add(List.copyOf(failing));
            remaining.removeAll(failing);
        }
        deleteRecursively(dir);
        Files.createDirectories(dir.resolve("classes"));
        return new Bytecode(List.of(), dir.resolve("classes"), leftOut);
    }

    /**
     * The Kotlin files of which an original Java class, one that stays, extends a class and overrides a final method
     * of, with why. Kotlin makes the JVM method of a mapped property final ({@code val size} in a
     * {@code java.util.AbstractMap}: a final {@code size()} calling {@code getSize()}), so the Java subclass fails to
     * load with an IncompatibleClassChangeError, and every class that touches it with it: guava's HashBiMap over a
     * Kotlin Maps.IteratorBasedAbstractMap took {@code FreshValueGenerator}, and whole suites, down. Such a file is
     * not a drop-in replacement for its Java. The original classes are the directories of {@code behind}.
     */
    private static Map<Path, String> finalOverridden(List<Path> files, Path kotlinClasses, List<String> behind)
            throws IOException {
        Map<String, Path> kotlin = classesByName(kotlinClasses);
        Map<String, Path> originals = new HashMap<>();
        for (String entry : behind) {
            if (Files.isDirectory(Path.of(entry))) classesByName(Path.of(entry)).forEach(originals::putIfAbsent);
        }
        Map<Path, String> found = new TreeMap<>();
        for (Map.Entry<String, Path> original : originals.entrySet()) {
            if (kotlin.containsKey(original.getKey())) continue;
            java.lang.classfile.ClassModel model = java.lang.classfile.ClassFile.of().parse(original.getValue());
            Set<String> declared = model.methods().stream().filter(JavaToKotlinRatchet::overridable)
                    .map(m -> m.methodName().stringValue() + m.methodType().stringValue())
                    .collect(Collectors.toSet());
            Optional<java.lang.classfile.constantpool.ClassEntry> superclass = model.superclass();
            while (superclass.isPresent()) {
                String name = superclass.get().asInternalName();
                Path file = kotlin.containsKey(name) ? kotlin.get(name) : originals.get(name);
                if (file == null) break; // the JDK's, a library's
                java.lang.classfile.ClassModel superModel = java.lang.classfile.ClassFile.of().parse(file);
                if (kotlin.containsKey(name)) {
                    for (java.lang.classfile.MethodModel m : superModel.methods()) {
                        String signature = m.methodName().stringValue() + m.methodType().stringValue();
                        if (overridable(m) && (m.flags().flagsMask() & java.lang.classfile.ClassFile.ACC_FINAL) != 0
                            && declared.contains(signature)) {
                            Path source = source(superModel, name, files);
                            if (source != null) {
                                found.putIfAbsent(source, original.getKey().replace('/', '.') + " overrides "
                                        + name.replace('/', '.') + "." + signature + ", final in the Kotlin");
                            }
                        }
                    }
                }
                superclass = superModel.superclass();
            }
        }
        return found;
    }

    private static boolean overridable(java.lang.classfile.MethodModel m) {
        int flags = m.flags().flagsMask();
        return (flags & (java.lang.classfile.ClassFile.ACC_STATIC | java.lang.classfile.ClassFile.ACC_PRIVATE)) == 0
               && !m.methodName().stringValue().startsWith("<");
    }

    /** The .kt file a Kotlin class came from: its package's directory and its SourceFile attribute. */
    private static Path source(java.lang.classfile.ClassModel model, String internalName, List<Path> files) {
        Optional<String> sourceFile = model.findAttribute(java.lang.classfile.Attributes.sourceFile())
                .map(a -> a.sourceFile().stringValue());
        if (sourceFile.isEmpty()) return null;
        int slash = internalName.lastIndexOf('/');
        String relative = (slash < 0 ? "" : internalName.substring(0, slash + 1)) + sourceFile.get();
        return files.stream().filter(f -> f.toString().replace(java.io.File.separatorChar, '/').endsWith("/" + relative))
                .findFirst().orElse(null);
    }

    /** The class files under {@code dir}, by internal name ({@code com/google/common/collect/Maps$Foo}). */
    private static Map<String, Path> classesByName(Path dir) throws IOException {
        Map<String, Path> byName = new HashMap<>();
        if (!Files.isDirectory(dir)) return byName;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path f : walk.filter(p -> p.toString().endsWith(".class")).toList()) {
                String name = dir.relativize(f).toString().replace(java.io.File.separatorChar, '/');
                byName.put(name.substring(0, name.length() - ".class".length()), f);
            }
        }
        return byName;
    }

    private static String bytecodeReport(Bytecode bytecode, String title) {
        StringBuilder sb = new StringBuilder("\n" + title + ": ").append(bytecode.files.size()).append(" files\n");
        for (int i = 0; i < bytecode.leftOut.size(); i++) {
            sb.append("  round ").append(i + 1).append(", left out (").append("its log: -round").append(i + 1)
                    .append(".log):\n");
            bytecode.leftOut.get(i).forEach(f -> sb.append("    ").append(f.getFileName()).append('\n'));
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- the corpus's tests

    /**
     * Test identifiers ("class#method()"), by outcome, one per test run: a parameterized test's invocations share
     * their identifier, so these are lists.
     */
    private record Tests(List<String> passed, List<String> failed, List<String> skipped) {
        static final Tests NONE = new Tests(List.of(), List.of(), List.of());

        Tests plus(Tests other) {
            return new Tests(concat(passed, other.passed), concat(failed, other.failed), concat(skipped, other.skipped));
        }
    }

    /**
     * The test classes a run selects: {@code classes} (top-level names, with their nested classes) when
     * {@code those}, else all others. JUnit's default class name pattern still applies, so the two runs together
     * select what one run without this does.
     */
    private record Only(Set<String> classes, boolean those) {
        // the console launcher's default --include-classname, which an --include-classname of our own replaces
        private static final String STANDARD = "(?:Test.*|.+[.$]Test.*|.*Tests?)";

        List<String> arguments() {
            String names = classes.stream().sorted().map(Pattern::quote)
                    .collect(Collectors.joining("|", "(?:", ")(?:\\$.*)?"));
            return those ? List.of("--include-classname", "^(?=" + STANDARD + "$)" + names + "$")
                    : List.of("--exclude-classname", "^" + names + "$");
        }

        /** A chunk ({@link #testChunks}) holds none of the selected classes: nothing to run. */
        boolean noneIn(List<String> chunk) {
            if (!those) return false;
            return classes.stream().map(c -> c.lastIndexOf('.') < 0 ? "" : c.substring(0, c.lastIndexOf('.')))
                    .noneMatch(pkg -> within(pkg, chunk.get(1)) && java.util.stream.IntStream.range(2, chunk.size())
                            .filter(i -> "--exclude-package".equals(chunk.get(i - 1)))
                            .noneMatch(i -> within(pkg, chunk.get(i))));
        }

        private static boolean within(String pkg, String outer) {
            return pkg.equals(outer) || pkg.startsWith(outer + ".");
        }
    }

    // a class's name in its class file, internal (com/google/common/base/Ascii) or, in a string, binary
    private static final Pattern CLASS_NAME = Pattern.compile("[A-Za-z_$][\\w$]*(?:[/.][A-Za-z_$][\\w$]*)+");

    /**
     * The test classes whose tests may come out differently with the translated test classes in front: those
     * translated, and those that name one in their class files (a helper, a suite listing the class), directly or
     * through another, by top-level name. Every other test class runs the same code with the Kotlin main classes
     * alone. A name only looked like one (a string, a prefix) only adds a class, which then runs again too.
     */
    private static Set<String> affected(Path translatedClasses, Path testClasses) throws IOException {
        Set<String> affected = new HashSet<>(classFiles(translatedClasses).keySet());
        Map<String, Set<String>> named = new HashMap<>();
        for (Map.Entry<String, List<Path>> e : classFiles(testClasses).entrySet()) {
            Set<String> names = new HashSet<>();
            for (Path f : e.getValue()) {
                String bytes = new String(Files.readAllBytes(f), java.nio.charset.StandardCharsets.ISO_8859_1);
                Matcher m = CLASS_NAME.matcher(bytes);
                while (m.find()) names.add(topLevel(m.group().replace('/', '.')));
            }
            named.put(e.getKey(), names);
        }
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Map.Entry<String, Set<String>> e : named.entrySet()) {
                if (!affected.contains(e.getKey()) && e.getValue().stream().anyMatch(affected::contains)) {
                    affected.add(e.getKey());
                    grew = true;
                }
            }
        }
        return affected;
    }

    /** The class files under {@code dir}, by the top-level class they belong to. */
    private static Map<String, List<Path>> classFiles(Path dir) throws IOException {
        Map<String, List<Path>> byTopLevel = new TreeMap<>();
        if (!Files.isDirectory(dir)) return byTopLevel;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path f : walk.filter(p -> p.toString().endsWith(".class")).toList()) {
                String name = dir.relativize(f).toString().replace(java.io.File.separatorChar, '.');
                byTopLevel.computeIfAbsent(topLevel(name.substring(0, name.length() - ".class".length())),
                        k -> new ArrayList<>()).add(f);
            }
        }
        return byTopLevel;
    }

    private static String topLevel(String binaryName) {
        int dollar = binaryName.indexOf('$');
        return dollar < 0 ? binaryName : binaryName.substring(0, dollar);
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    /**
     * The corpus's test classes in a child JVM, from the corpus's working directory (fernflower finds its testData
     * there): JUnit's console launcher scans the (original) test classes, the outcome comes from its XML report.
     * With {@code kotlinClasses}, those come first on the class path, in front of the original classes they replace,
     * test classes included: the launcher finds a test by the original's name and loads the Kotlin one.
     */
    private static Tests runTests(Corpus corpus, List<Path> kotlinClasses, Path testClasses, String runner, Path dir,
                                  Only only) throws Exception {
        List<String> filter = only == null ? List.of() : only.arguments();
        if (!corpus.junit4()) {
            List<String> selection = new ArrayList<>(List.of("--scan-classpath", testClasses.toString()));
            selection.addAll(filter);
            return runTests(corpus, kotlinClasses, testClasses, runner, dir, selection, 60, true);
        }
        // guava's suite (865,681 tests) one package at a time, each in its own JVM with its own limit: a translated
        // class that hangs a test (a latch never counted down) costs that package's tests, which then pass on the
        // original classes and are missing with the Kotlin ones, not the whole run
        deleteRecursively(dir);
        Files.createDirectories(dir);
        List<String> passed = new ArrayList<>(), failed = new ArrayList<>(), skipped = new ArrayList<>();
        for (List<String> chunk : testChunks(testClasses)) {
            if (only != null && only.noneIn(chunk)) continue;
            String pkg = chunk.get(1);
            // scan only the test classes: a package selector also scans the jars, and with them guava-testlib's
            // tests and jimfs's file system provider, which fails to load beside some translated classes
            List<String> selection = new ArrayList<>(List.of("--scan-classpath", testClasses.toString()));
            selection.addAll(chunk);
            selection.addAll(filter);
            Tests tests = runTests(corpus, kotlinClasses, testClasses, runner, dir.resolve(pkg), selection, 20, false);
            if (tests == null) {
                failed.add(pkg + " (timed out after 20 minutes)");
                continue;
            }
            passed.addAll(tests.passed());
            failed.addAll(tests.failed());
            skipped.addAll(tests.skipped());
        }
        return new Tests(passed, failed, skipped);
    }

    /**
     * The include and exclude filters of one chunk per package one level below the test classes' common root
     * package ({@code com.google.common.base}, {@code com.google.common.collect}, ..., {@code com.google.thirdparty}
     * for guava-tests). A package with classes of its own beside its subpackages is a chunk without them.
     */
    private static List<List<String>> testChunks(Path testClasses) throws IOException {
        Path root = testClasses;
        String prefix = "";
        while (true) {
            List<Path> dirs = subdirectories(root);
            if (dirs.size() != 1 || hasClasses(root)) {
                List<List<String>> chunks = new ArrayList<>();
                for (Path d : dirs) {
                    String pkg = prefix + d.getFileName();
                    List<Path> subs = subdirectories(d);
                    if (subs.isEmpty()) {
                        chunks.add(List.of("--include-package", pkg));
                        continue;
                    }
                    for (Path sub : subs) chunks.add(List.of("--include-package", pkg + "." + sub.getFileName()));
                    if (hasClasses(d)) {
                        List<String> own = new ArrayList<>(List.of("--include-package", pkg));
                        for (Path sub : subs) own.addAll(List.of("--exclude-package", pkg + "." + sub.getFileName()));
                        chunks.add(own);
                    }
                }
                return chunks;
            }
            root = dirs.getFirst();
            prefix += root.getFileName() + ".";
        }
    }

    private static List<Path> subdirectories(Path dir) throws IOException {
        try (var children = Files.list(dir)) {
            return children.filter(Files::isDirectory)
                    .filter(d -> !d.getFileName().toString().equals("META-INF"))
                    .sorted().toList();
        }
    }

    private static boolean hasClasses(Path dir) throws IOException {
        try (var children = Files.list(dir)) {
            return children.anyMatch(c -> c.getFileName().toString().endsWith(".class"));
        }
    }

    /**
     * One JUnit console run in its own JVM, selecting the tests with {@code selection}. Past {@code minutes}: a
     * failure when {@code failOnTimeout}, else null.
     */
    private static Tests runTests(Corpus corpus, List<Path> kotlinClasses, Path testClasses, String runner, Path dir,
                                  List<String> selection, int minutes, boolean failOnTimeout) throws Exception {
        deleteRecursively(dir);
        Files.createDirectories(dir);
        List<String> cp = testClassPath(corpus, kotlinClasses, testClasses, runner);
        Path argFile = dir.resolve("java.args");
        Files.write(argFile, List.of("-cp", "\"" + String.join(java.io.File.pathSeparator, cp) + "\""));
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        Path reports = dir.resolve("reports");
        Path log = dir.resolve("tests.log");
        Path workingDirectory = workingDirectory(corpus, testClasses);
        List<String> command = new ArrayList<>(List.of(java.toString(), "-Xmx2g", "-XX:+EnableDynamicAgentLoading",
                "@" + argFile.toAbsolutePath(),
                "org.junit.platform.console.ConsoleLauncher", "execute", "--disable-banner", "--details=summary"));
        command.addAll(selection);
        command.addAll(List.of("--reports-dir", reports.toAbsolutePath().toString()));
        // Mockito attaches its agent at run time, which the JDK will refuse by default
        Process process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(minutes, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            if (!failOnTimeout) return null;
            fail("the tests did not finish within " + minutes + " minutes; see " + log.toAbsolutePath());
        }
        Path xml = reports.resolve("TEST-junit-jupiter.xml");
        if (!Files.isRegularFile(xml)) fail("no test report " + xml + "; see " + log.toAbsolutePath());
        Tests tests = parseReport(xml);
        // JUnit 3 and 4 tests, run by the vintage engine (a corpus with junit4), report beside Jupiter's
        Path vintageXml = reports.resolve("TEST-junit-vintage.xml");
        if (Files.isRegularFile(vintageXml)) {
            Tests vintage = parseReport(vintageXml);
            tests = new Tests(concat(tests.passed(), vintage.passed()), concat(tests.failed(), vintage.failed()),
                    concat(tests.skipped(), vintage.skipped()));
        }
        return tests;
    }

    /** The test JVM's class path: the Kotlin classes, when given, in front of the originals they replace. */
    private static List<String> testClassPath(Corpus corpus, List<Path> kotlinClasses, Path testClasses, String runner) {
        List<String> cp = new ArrayList<>();
        if (kotlinClasses != null) {
            kotlinClasses.forEach(k -> cp.add(k.toAbsolutePath().toString()));
            cp.add(stdlib());
        }
        cp.add(Path.of(corpus.main().uri()).toString());
        cp.add(testClasses.toString());
        cp.addAll(libraries(corpus.config()));
        if (corpus.siblings()) cp.addAll(siblingClasses(corpus));
        cp.addAll(Arrays.asList(runner.split(java.io.File.pathSeparator)));
        return cp;
    }

    /**
     * The tests on the original classes, the report's baseline. No translation changes it, so it is kept in
     * {@code ~/.cache/maddi-j2k-baseline/}, outside the corpus's checkout, under a hash of what it ran: the class
     * files of the class path's directories, its jars by name and size (not by path: each workspace has its own
     * Gradle cache), the JVM's version. A rebuilt corpus, a new library or runner runs it again; a baseline with a
     * package that timed out is not kept. Guava's takes some 25 minutes.
     */
    private static Tests baseline(Corpus corpus, Path testClasses, String runner, Path dir) throws Exception {
        Path cache = Path.of(System.getProperty("user.home"), ".cache", "maddi-j2k-baseline")
                .resolve(baselineKey(testClassPath(corpus, null, testClasses, runner), corpus.junit4()));
        if (Files.isRegularFile(cache)) {
            try (java.io.DataInputStream in = new java.io.DataInputStream(new java.io.BufferedInputStream(
                    Files.newInputStream(cache)))) {
                Tests tests = new Tests(readList(in), readList(in), readList(in));
                LOGGER.info("tests on the original classes: the baseline in {}", cache);
                return tests;
            } catch (IOException corrupt) {
                LOGGER.warn("cannot read the baseline {}, running it: {}", cache, corrupt.toString());
            }
        }
        Tests tests = runTests(corpus, null, testClasses, runner, dir, null);
        if (tests.failed().stream().noneMatch(t -> t.endsWith("(timed out after 20 minutes)"))) {
            try {
                Files.createDirectories(cache.getParent());
                Path tmp = Files.createTempFile(cache.getParent(), "baseline", ".tmp");
                try (java.io.DataOutputStream out = new java.io.DataOutputStream(new java.io.BufferedOutputStream(
                        Files.newOutputStream(tmp)))) {
                    writeList(out, tests.passed());
                    writeList(out, tests.failed());
                    writeList(out, tests.skipped());
                }
                Files.move(tmp, cache, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException notKept) {
                LOGGER.warn("baseline not kept in {}: {}", cache, notKept.toString());
            }
        }
        return tests;
    }

    private static String baselineKey(List<String> classPath, boolean junit4) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        java.util.function.Consumer<String> add = v -> md.update((v + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        add.accept(System.getProperty("java.version"));
        add.accept(String.valueOf(junit4));
        for (String entry : classPath) {
            Path path = Path.of(entry);
            if (Files.isDirectory(path)) {
                try (Stream<Path> walk = Files.walk(path)) {
                    for (Path f : walk.filter(Files::isRegularFile).sorted().toList()) {
                        add.accept(path.relativize(f).toString());
                        md.update(Files.readAllBytes(f));
                    }
                }
            } else if (Files.isRegularFile(path)) {
                add.accept(path.getFileName() + " " + Files.size(path));
            }
        }
        return HexFormat.of().formatHex(md.digest());
    }

    private static void writeList(java.io.DataOutputStream out, List<String> list) throws IOException {
        out.writeInt(list.size());
        for (String s : list) out.writeUTF(s);
    }

    private static List<String> readList(java.io.DataInputStream in) throws IOException {
        int n = in.readInt();
        List<String> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(in.readUTF());
        return list;
    }

    /**
     * Where the tests run: the configuration's working directory when it names one (fernflower finds its testData
     * there), else the build directory of the test classes' module, the nearest ancestor with a pom.xml or Gradle
     * build file, as Maven's surefire runs a module's tests (langchain4j-core).
     */
    private static Path workingDirectory(Corpus corpus, Path testClasses) {
        Path configured = corpus.config().workingDirectory();
        if (configured != null && configured.isAbsolute() && Files.isDirectory(configured)) return configured;
        for (Path dir = testClasses.toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isRegularFile(dir.resolve("pom.xml")) || Files.isRegularFile(dir.resolve("build.gradle.kts"))
                || Files.isRegularFile(dir.resolve("build.gradle"))) {
                return dir;
            }
        }
        return testClasses;
    }

    private static Tests parseReport(Path xml) throws Exception {
        List<String> passed = new ArrayList<>(), failed = new ArrayList<>(), skipped = new ArrayList<>();
        org.w3c.dom.Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(xml.toFile());
        org.w3c.dom.NodeList cases = doc.getElementsByTagName("testcase");
        for (int i = 0; i < cases.getLength(); i++) {
            org.w3c.dom.Element tc = (org.w3c.dom.Element) cases.item(i);
            String id = tc.getAttribute("classname") + "#" + tc.getAttribute("name");
            if (tc.getElementsByTagName("failure").getLength() > 0 || tc.getElementsByTagName("error").getLength() > 0) {
                failed.add(id);
            } else if (tc.getElementsByTagName("skipped").getLength() > 0) {
                skipped.add(id);
            } else {
                passed.add(id);
            }
        }
        return new Tests(passed, failed, skipped);
    }

    private static String testReport(Tests original, Tests kotlin, Path dir, String label) throws IOException {
        Set<String> originalPassed = new HashSet<>(original.passed), originalFailed = new HashSet<>(original.failed);
        List<String> broken = kotlin.failed.stream().filter(originalPassed::contains).distinct().sorted().toList();
        List<String> fixed = kotlin.passed.stream().filter(originalFailed::contains).distinct().sorted().toList();
        Files.write(dir.resolve("failing-" + label.replace(' ', '-') + ".txt"), broken);
        StringBuilder sb = new StringBuilder("\ntests (the corpus's own), " + label + ":\n");
        sb.append(String.format("  %-18s %6d passed, %d failed, %d skipped%n", "original classes",
                original.passed.size(), original.failed.size(), original.skipped.size()));
        sb.append(String.format("  %-18s %6d passed, %d failed, %d skipped%n", label,
                kotlin.passed.size(), kotlin.failed.size(), kotlin.skipped.size()));
        if (!original.failed.isEmpty() && "with Kotlin".equals(label)) {
            sb.append("  failing on the original classes too:\n");
            original.failed.stream().distinct().sorted().forEach(t -> sb.append("    ").append(t).append('\n'));
        }
        if (!broken.isEmpty()) {
            sb.append("  passing on the original, failing ").append(label).append(" (its tests.log has the traces):\n");
            broken.forEach(t -> sb.append("    ").append(t).append('\n'));
        }
        if (!fixed.isEmpty()) {
            sb.append("  failing on the original, passing with Kotlin:\n");
            fixed.forEach(t -> sb.append("    ").append(t).append('\n'));
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- report and ratchet

    private String report(Map<String, Long> measured, List<String> crashes, Compiled all, Compiled clean) {
        StringBuilder sb = new StringBuilder("Java -> Kotlin, " + name + "\n\n");
        measured.forEach((k, v) -> sb.append(reportedOnly.contains(k)
                ? String.format("  %-18s %6d   (reported, not ratcheted)%n", k, v)
                : String.format("  %-18s %6d%n", k, v)));
        sb.append(String.format("  %-18s %6d   (reported, not ratcheted)%n", "typeErrors", clean.errors.size()));
        if (!crashes.isEmpty()) {
            sb.append("\nprinter crashes:\n");
            crashes.forEach(c -> sb.append("  ").append(c).append('\n'));
        }
        crashLines(all, "compile 1", sb);
        crashLines(clean, "compile 2", sb);
        sb.append("\ncompile 1 (all files), errors by kind:\n").append(byKind(all.errors));
        sb.append("\ncompile 2 (syntax-clean files against the original classes), errors by kind:\n")
                .append(byKind(clean.errors));
        return sb.toString();
    }

    /**
     * The printer's own messages, by severity and code (all of them in messages.txt): what the Kotlin does differently,
     * loses, or could not translate. Reported, not ratcheted: a better translation may well report more.
     */
    private static String messageCounts(List<PrintMessage<KotlinPrintCode>> messages) {
        Map<String, Long> byCode = messages.stream().collect(Collectors.groupingBy(
                m -> String.format("%-16s %s", m.severity(), m.code()), TreeMap::new, Collectors.counting()));
        StringBuilder sb = new StringBuilder("\nprinter messages, by severity and code (messages.txt has each):\n");
        byCode.forEach((k, v) -> sb.append(String.format("  %6d  %s%n", v, k)));
        return sb.toString();
    }

    /** Messages with their names and numbers blanked, so that one kind of error is one line. */
    private static String byKind(List<Diagnostic> errors) {
        Map<String, Long> kinds = errors.stream().collect(Collectors.groupingBy(
                d -> d.message.replaceAll("'[^']*'", "'_'").replaceAll("\\d+", "N"), Collectors.counting()));
        return kinds.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> String.format("  %6d  %s%n", e.getValue(), e.getKey()))
                .collect(Collectors.joining());
    }

    private void ratchet(Map<String, Long> measured) throws IOException {
        if (ratchetFile == null) return; // measureOnly
        Path file = ratchetFile;
        String now = measured.entrySet().stream().map(e -> e.getKey() + "\t" + e.getValue())
                .collect(Collectors.joining("\n"));
        if (!Files.isRegularFile(file)) {
            fail("no " + file.toAbsolutePath() + ": record this run's numbers there, below a comment saying "
                 + "what was measured:\n" + now);
        }
        Map<String, Long> recorded = new HashMap<>();
        try (Stream<String> lines = Files.lines(file)) {
            lines.map(String::trim).filter(l -> !l.isEmpty() && !l.startsWith("#")).forEach(l -> {
                String[] kv = l.split("\\s+");
                recorded.put(kv[0], Long.parseLong(kv[1]));
            });
        }
        List<String> worse = new ArrayList<>();
        List<String> better = new ArrayList<>();
        DIRECTION.forEach((name, direction) -> {
            if (!measured.containsKey(name)) return; // testsPassing, where the tests did not run
            if (reportedOnly.contains(name)) return;
            Long was = recorded.get(name);
            long is = measured.get(name);
            if (was == null) {
                worse.add(name + ": not in " + ratchetFile);
            } else if (is != was) {
                boolean improved = switch (direction) {
                    case "<=" -> is < was;
                    case ">=" -> is > was;
                    default -> false;
                };
                (improved ? better : worse).add(name + ": " + was + " -> " + is);
            }
        });
        if (!worse.isEmpty()) {
            fail("THE TRANSLATION GOT WORSE on " + this.name + ": " + worse + ". Read build/j2k/" + this.name
                 + "/report.txt.");
        }
        if (!better.isEmpty()) {
            fail("Progress on " + this.name + ": " + better + ". Tighten the ratchet: write these values into "
                 + ratchetFile + " in the commit that made them.");
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
}
