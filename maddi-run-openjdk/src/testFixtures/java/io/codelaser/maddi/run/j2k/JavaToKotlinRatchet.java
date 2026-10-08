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
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import io.codelaser.maddi.cst.print.kotlin.KotlinCompilationUnitPrinter;
import io.codelaser.maddi.cst.print.kotlin.KotlinPrintMessage;
import io.codelaser.maddi.cst.print.kotlin.KotlinPrintOptions;
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
 * <p>
 * The output lands in {@code build/j2k/<name>/}: the translated sources, both compilers' raw output, and
 * {@code report.txt} with the errors by kind. Read the report before tightening.
 *
 * @param name        the run's name, for the output directory and the messages
 * @param ratchetFile the recorded numbers, relative to the module directory
 */
public record JavaToKotlinRatchet(String name, Path ratchetFile) {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaToKotlinRatchet.class);

    /** name -> direction. "=" must not move, "<=" may only go down, ">=" may only go up. */
    private static final Map<String, String> DIRECTION = new LinkedHashMap<>();

    static {
        DIRECTION.put("types", "=");
        DIRECTION.put("printerCrashes", "<=");
        DIRECTION.put("syntaxErrors", "<=");
        DIRECTION.put("syntaxCleanFiles", ">=");
        DIRECTION.put("compilingFiles", ">=");
    }

    // kotlinc's plain renderer: "<path>.kt:<line>:<column>: error: <message>", the path relative to the working
    // directory, and "syntax error: ..." in lower case
    private static final Pattern CRASHED_ON = Pattern.compile("While analysing (\\S+?\\.kt)");
    private static final Pattern DIAGNOSTIC = Pattern.compile("^(.+?\\.kt):(\\d+):(\\d+): error: (.*)$");

    /**
     * A parsed corpus: the inspector, the parse, the configuration, the main source set and its primary types,
     * sorted by name.
     */
    public record Corpus(InputConfigurationImpl config, JavaInspector javaInspector, Summary summary,
                         SourceSet main, List<TypeInfo> types) {
    }

    /** Parses a corpus of {@link Corpora#oss}, which must be built (its class files are compile 2's class path). */
    public static Corpus parse(String corpusName) throws IOException {
        Corpora.Corpus corpus = Corpora.oss(corpusName);
        // the locator, not a hand-written assumption: it honours -Dmaddi.corpus.required, which slowTest sets
        corpus.requireConfig();
        InputConfigurationImpl config = JsonStreaming.objectMapper()
                .readValue(corpus.config().toFile(), InputConfigurationImpl.class);
        SourceSet main = config.sourceSets().stream().filter(s -> !s.test()).findFirst().orElseThrow();
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
        return new Corpus(config, javaInspector, summary, main, types);
    }

    /** Translates, compiles twice, writes the report, and holds the numbers to the ratchet file. */
    public void run(Corpus corpus, KotlinPrintOptions options) throws Exception {
        Path out = Path.of("build/j2k", name);
        deleteRecursively(out);
        Path src = out.resolve("src");
        List<Path> files = new ArrayList<>();
        List<String> crashes = new ArrayList<>();
        List<KotlinPrintMessage> messages = new ArrayList<>();
        Runtime runtime = corpus.javaInspector().runtime();
        Formatter2Impl formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        Set<Object> printed = new HashSet<>();
        for (TypeInfo type : corpus.types()) {
            if (!printed.add(type.compilationUnit())) continue; // a file with two primary types prints once
            try {
                KotlinCompilationUnitPrinter.Result result = new KotlinCompilationUnitPrinter(type.compilationUnit(),
                        true, options).printWithMessages(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
                messages.addAll(result.messages());
                String kotlin = formatter.write(result.output()) + "\n";
                Path file = src.resolve(type.packageName().replace('.', '/')).resolve(type.simpleName() + ".kt");
                Files.createDirectories(file.getParent());
                Files.writeString(file, kotlin);
                files.add(file.toAbsolutePath().normalize());
            } catch (RuntimeException | StackOverflowError e) {
                crashes.add(type.fullyQualifiedName() + ": " + e);
            }
        }

        // compile 1: everything, for the syntax errors
        List<String> classPath = libraries(corpus.config());
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

        Map<String, Long> measured = new LinkedHashMap<>();
        measured.put("types", (long) corpus.types().size());
        measured.put("printerCrashes", (long) crashes.size());
        measured.put("syntaxErrors", syntaxPerFile.values().stream().mapToLong(Long::longValue).sum());
        measured.put("syntaxCleanFiles", (long) syntaxClean.size());
        measured.put("compilingFiles", compiling);

        String report = report(measured, crashes, all, clean) + messageCounts(messages);
        Files.writeString(out.resolve("report.txt"), report);
        Files.write(out.resolve("messages.txt"), messages.stream().map(KotlinPrintMessage::toString).toList());
        LOGGER.info("\n{}", report);
        ratchet(measured);
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
        String compilerClassPath = System.getProperty("maddi.test.kotlinCompilerClasspath");
        assertTrue(compilerClassPath != null && !compilerClassPath.isBlank(),
                "no -Dmaddi.test.kotlinCompilerClasspath: run this through Gradle");
        String stdlib = Arrays.stream(compilerClassPath.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches("kotlin-stdlib-[0-9.]+\\.jar"))
                .findFirst().orElseThrow(() -> new AssertionError("no kotlin-stdlib in " + compilerClassPath));
        List<String> cp = new ArrayList<>(classPath);
        cp.addFirst(stdlib);

        List<String> args = new ArrayList<>(List.of("-no-stdlib", "-no-reflect", "-jvm-target", "21",
                "-classpath", String.join(java.io.File.pathSeparator, cp),
                "-d", dir.resolve("classes").toAbsolutePath().toString()));
        files.forEach(f -> args.add(f.toString()));
        Path argFile = dir.resolve("kotlinc.args");
        Files.write(argFile, args.stream().map(a -> "\"" + a.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .toList());

        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        Path log = dir.resolve("kotlinc.log");
        Process process = new ProcessBuilder(java.toString(), "-Xmx3g", "-cp", compilerClassPath,
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

    // ---------------------------------------------------------------- report and ratchet

    private String report(Map<String, Long> measured, List<String> crashes, Compiled all, Compiled clean) {
        StringBuilder sb = new StringBuilder("Java -> Kotlin, " + name + "\n\n");
        measured.forEach((k, v) -> sb.append(String.format("  %-18s %6d%n", k, v)));
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
    private static String messageCounts(List<KotlinPrintMessage> messages) {
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
