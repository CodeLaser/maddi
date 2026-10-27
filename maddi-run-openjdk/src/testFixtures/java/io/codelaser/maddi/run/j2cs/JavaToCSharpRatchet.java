/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.run.j2cs;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.print.csharp.CSharpCompat;
import io.codelaser.maddi.cst.print.csharp.CSharpTranslator;
import io.codelaser.maddi.cst.print.csharp.CSharpLibraries;
import io.codelaser.maddi.cst.print.csharp.LibraryMapping;
import io.codelaser.maddi.cst.print.csharp.CSharpProgram;
import io.codelaser.maddi.cst.print.csharp.CSharpPrintCode;
import io.codelaser.maddi.cst.print.translate.PrintMessage;
import io.codelaser.maddi.run.j2k.JavaToKotlinRatchet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * ⛔ THE RATCHET FOR JAVA → C# TRANSLATION: a corpus's main sources, parsed by the openjdk front end, printed by
 * {@code maddi-cst-print-csharp}, and judged by Roslyn. As {@link JavaToKotlinRatchet}: a metric that gets WORSE
 * fails, and one that gets BETTER fails too, until the new value is written into the ratchet file, so every step of
 * progress is recorded by the commit that made it.
 *
 * <h2>The judge</h2>
 * {@code tools/csharp-check}, a .NET tool on Roslyn, built here with {@code dotnet build} (a .NET SDK must be on the
 * PATH; the test JVM gets the tool's directory as {@code -Dmaddi.test.csharpCheck}). It parses every file on its own,
 * for the syntax errors, then compiles the syntax-clean files together against the BCL. A file that has no error in
 * that compilation counts as compiling. The error total is reported, not ratcheted: as the BCL mapping lets the
 * compiler bind more, it gets to report errors it could not see before.
 * <h2>The libraries</h2>
 * The policy's library mappings decide what goes with the translation: their sources (the compatibility library) are
 * compiled with it, not counted; their NuGet packages are restored into {@code deps/} and referenced. The output has a
 * {@code Translated.csproj} beside the sources, which builds them with those packages.
 *
 * <h2>The census</h2>
 * The printer reports every library type or member it prints without a mapping ({@code UNMAPPED_JDK}): the JDK's,
 * and those of slf4j and Jackson, which have mappings too. Their number is ratcheted; the report counts them per
 * library and lists them by frequency, the mappings' work list.
 * <p>
 * A corpus's own mapping ({@link #withMapping}) may leave some of its types out ({@code exclude}): they are not
 * printed nor counted as files, and their uses go through its rules, as a library's do; {@code types} stays the
 * corpus's.
 * <p>
 * The output lands in {@code build/j2cs/<name>/}: the translated sources, the judge's output and {@code report.txt}.
 */
public record JavaToCSharpRatchet(String name, Path ratchetFile, CSharpProgram.Policy policy) {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaToCSharpRatchet.class);

    /** The translation as the default policy has it: the libraries the printer maps by default. */
    public JavaToCSharpRatchet(String name, Path ratchetFile) {
        this(name, ratchetFile, CSharpProgram.Policy.DEFAULT);
    }

    /**
     * This ratchet with the corpus's own mapping in front of the policy's: the resource {@code resource} beside this
     * class, such as {@code timefold-gizmo.csmap}, which leaves Gizmo's bytecode generation out of timefold.
     */
    public JavaToCSharpRatchet withMapping(String resource) {
        try (var in = JavaToCSharpRatchet.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("no " + resource + " beside " + JavaToCSharpRatchet.class);
            var mapping = io.codelaser.maddi.cst.print.csharp.CSharpMappingFile.parse(
                    new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), resource);
            return new JavaToCSharpRatchet(name, ratchetFile, policy.withLibraries(policy.libraries().withFirst(mapping)));
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** name -> direction. "=" must not move, "<=" may only go down, ">=" may only go up. */
    private static final Map<String, String> DIRECTION = new LinkedHashMap<>();

    static {
        DIRECTION.put("types", "=");
        DIRECTION.put("printerCrashes", "<=");
        DIRECTION.put("syntaxErrors", "<=");
        DIRECTION.put("syntaxCleanFiles", ">=");
        DIRECTION.put("compilingFiles", ">=");
        DIRECTION.put("unmappedJdkUses", "<=");
        DIRECTION.put("nullableWarnings", "<="); // with the facts of a nullability analysis only
    }

    /** Translates, judges, writes the report, and holds the numbers to the ratchet file. */
    public void run(JavaToKotlinRatchet.Corpus corpus) throws Exception {
        Path out = Path.of("build/j2cs", name);
        deleteRecursively(out);
        Printed printed = print(corpus, corpus.types(), out.resolve("src"), policy);
        // the sources the libraries' mappings need, the compatibility library first, go with the translation; they
        // are compiled with it, and not counted. A project file beside them references the mappings' packages.
        CSharpLibraries libraries = policy.libraries();
        List<Path> sources = CSharpTranslator.writeProject(policy, out.resolve("src"));
        List<Path> all = new ArrayList<>(printed.files);
        all.addAll(sources);
        Judged judged = judge(all, libraries.requirements().packages(), nullable(), out);

        Set<Path> withErrors = judged.errors.stream().map(Diagnostic::file).collect(Collectors.toSet());
        Set<Path> withSyntaxErrors = judged.syntaxErrors.stream().map(Diagnostic::file).collect(Collectors.toSet());
        long compiling = printed.files.stream().filter(f -> !withErrors.contains(f) && !withSyntaxErrors.contains(f))
                .count();
        List<PrintMessage<CSharpPrintCode>> unmapped = printed.messages.stream()
                .filter(m -> m.code() == CSharpPrintCode.UNMAPPED_JDK).toList();

        Map<String, Long> measured = new LinkedHashMap<>();
        measured.put("types", (long) corpus.types().size());
        measured.put("printerCrashes", (long) printed.crashes.size());
        measured.put("syntaxErrors", (long) judged.syntaxErrors.size());
        measured.put("syntaxCleanFiles", (long) (printed.files.size() - withSyntaxErrors.size()));
        measured.put("compilingFiles", compiling);
        measured.put("unmappedJdkUses", (long) unmapped.size());
        if (nullable()) measured.put("nullableWarnings", (long) judged.nullableWarnings.size());

        long excluded = corpus.types().stream().filter(policy.libraries()::excludes).count();
        String report = report(measured, printed, judged, unmapped, sources, excluded);
        Files.writeString(out.resolve("report.txt"), report);
        Files.write(out.resolve("messages.txt"), printed.messages.stream().map(PrintMessage::toString).toList());
        LOGGER.info("\n{}", report);
        ratchet(measured);
    }

    private record Printed(List<Path> files, List<String> crashes, List<PrintMessage<CSharpPrintCode>> messages) {
    }

    /** Each compilation unit of {@code types} as a C# file under {@code src}: what the CLI's translation writes. */
    private static Printed print(JavaToKotlinRatchet.Corpus corpus, List<TypeInfo> types, Path src,
                                 CSharpProgram.Policy policy) throws IOException {
        CSharpTranslator.Result result = CSharpTranslator.translate(corpus.javaInspector().runtime(), types, policy, src);
        return new Printed(result.files(), result.crashes(), result.messages());
    }

    // ---------------------------------------------------------------- the judge

    private record Diagnostic(Path file, int line, String code, String message) {
    }

    private record Judged(List<Diagnostic> syntaxErrors, List<Diagnostic> errors, List<Diagnostic> nullableWarnings) {
    }

    /** The facts come from a nullability analysis: the judge enables C#'s nullable reference types. */
    private boolean nullable() {
        return policy.facts().nullabilityAnalysed();
    }

    private static final String TARGET_FRAMEWORK = CSharpTranslator.TARGET_FRAMEWORK;

    private static Judged judge(List<Path> files, List<LibraryMapping.NuGetPackage> packages, boolean nullable,
                                Path out)
            throws Exception {
        String tool = System.getProperty("maddi.test.csharpCheck");
        assertTrue(tool != null && !tool.isBlank(), "no -Dmaddi.test.csharpCheck: run this through Gradle");
        Path toolDir = Path.of(tool);
        Path buildLog = out.resolve("csharp-check-build.log");
        int built = new ProcessBuilder("dotnet", "build", toolDir.toString(), "-c", "Release", "-nologo", "-v", "q")
                .redirectErrorStream(true).redirectOutput(buildLog.toFile()).start().waitFor();
        if (built != 0) fail("building " + toolDir + " failed (is a .NET 10 SDK on the PATH?); see " + buildLog);

        Path argFile = out.resolve("csharp-check.args");
        List<String> arguments = new ArrayList<>(files.stream().map(Path::toString).toList());
        restore(packages, out).forEach(dll -> arguments.add("-r:" + dll));
        if (nullable) arguments.add("-nullable");
        Files.write(argFile, arguments);
        Path log = out.resolve("csharp-check.out");
        Path dll = toolDir.resolve("build/bin/Release/net10.0/csharp-check.dll");
        int exit = new ProcessBuilder("dotnet", dll.toString(), argFile.toAbsolutePath().toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start().waitFor();
        List<String> lines = Files.readAllLines(log);
        if (exit != 0 || lines.isEmpty() || !lines.getLast().startsWith("F\t")) {
            fail("csharp-check exited " + exit + " without its final line; see " + log.toAbsolutePath());
        }
        List<Diagnostic> syntax = new ArrayList<>();
        List<Diagnostic> errors = new ArrayList<>();
        List<Diagnostic> nullableWarnings = new ArrayList<>();
        for (String line : lines) {
            String[] f = line.split("\t", 6);
            if (f.length < 6) continue;
            Diagnostic d = new Diagnostic(Path.of(f[1]).toAbsolutePath().normalize(), Integer.parseInt(f[2]), f[4], f[5]);
            if ("S".equals(f[0])) syntax.add(d);
            else if ("E".equals(f[0])) errors.add(d);
            else if ("N".equals(f[0])) nullableWarnings.add(d);
        }
        return new Judged(syntax, errors, nullableWarnings);
    }

    /**
     * The assemblies of the mappings' packages, with their dependencies: a project that references them, built into
     * {@code deps/bin} (NuGet's restore, which needs the packages in the local cache or the network). None without
     * packages.
     */
    private static List<Path> restore(List<LibraryMapping.NuGetPackage> packages, Path out) throws Exception {
        if (packages.isEmpty()) return List.of();
        Path deps = out.resolve("deps").toAbsolutePath();
        Files.createDirectories(deps);
        StringBuilder project = new StringBuilder("<Project Sdk=\"Microsoft.NET.Sdk\">\n  <PropertyGroup>\n")
                .append("    <TargetFramework>").append(TARGET_FRAMEWORK).append("</TargetFramework>\n")
                .append("    <CopyLocalLockFileAssemblies>true</CopyLocalLockFileAssemblies>\n")
                .append("  </PropertyGroup>\n  <ItemGroup>\n");
        packages.forEach(p -> project.append("    <PackageReference Include=\"").append(p.id()).append("\" Version=\"")
                .append(p.version()).append("\" />\n"));
        Files.writeString(deps.resolve("deps.csproj"), project.append("  </ItemGroup>\n</Project>\n").toString());
        Path bin = deps.resolve("bin");
        Path log = out.resolve("deps-build.log");
        int built = new ProcessBuilder("dotnet", "build", deps.toString(), "-c", "Release", "-o", bin.toString(),
                "-nologo", "-v", "q").redirectErrorStream(true).redirectOutput(log.toFile()).start().waitFor();
        if (built != 0) fail("restoring the packages " + packages + " failed; see " + log.toAbsolutePath());
        try (Stream<Path> dlls = Files.list(bin)) {
            return dlls.filter(f -> f.getFileName().toString().endsWith(".dll")
                                    && !f.getFileName().toString().equals("deps.dll")).sorted().toList();
        }
    }

    // ---------------------------------------------------------------- report and ratchet

    private String report(Map<String, Long> measured, Printed printed, Judged judged,
                          List<PrintMessage<CSharpPrintCode>> unmapped, List<Path> sources, long excluded) {
        StringBuilder sb = new StringBuilder("Java -> C#, " + name + "\n\n");
        measured.forEach((k, v) -> sb.append(String.format("  %-18s %6d%n", k, v)));
        sb.append(String.format("  %-18s %6d   (reported, not ratcheted)%n", "errors", judged.errors.size()));
        if (excluded > 0) sb.append(String.format("  %-18s %6d   (types a mapping leaves out, not printed)%n", "excluded", excluded));
        if (!judged.nullableWarnings.isEmpty()) {
            sb.append("\nnullable warnings by code:\n").append(counted(judged.nullableWarnings.stream().map(d -> d.code
                    + " " + d.message.replaceAll("'[^']*'", "'_'")), 40));
            sb.append("\nnullable warnings by file:\n").append(counted(judged.nullableWarnings.stream()
                    .map(d -> d.file.getFileName().toString()), 40));
        }
        if (!printed.crashes.isEmpty()) {
            sb.append("\nprinter crashes:\n");
            printed.crashes.forEach(c -> sb.append("  ").append(c).append('\n'));
        }
        List<Diagnostic> inSources = judged.errors.stream().filter(d -> sources.contains(d.file)).toList();
        if (!inSources.isEmpty()) {
            sb.append("\nerrors in the libraries' sources (").append(CSharpCompat.FILE_NAME).append(", …):\n");
            inSources.forEach(d -> sb.append("  ").append(d.file.getFileName()).append(':').append(d.line).append(' ')
                    .append(d.code).append(' ').append(d.message).append('\n'));
        }
        if (!judged.syntaxErrors.isEmpty()) {
            sb.append("\nsyntax errors:\n");
            judged.syntaxErrors.forEach(d -> sb.append("  ").append(d.file.getFileName()).append(':').append(d.line)
                    .append(' ').append(d.code).append(' ').append(d.message).append('\n'));
        }
        sb.append("\nerrors by code:\n").append(counted(judged.errors.stream().map(d -> d.code + " "
                + d.message.replaceAll("'[^']*'", "'_'").replaceAll("\\d+", "N")), 40));
        sb.append("\nnames the compiler does not know (CS0246, CS0103, CS0234):\n").append(counted(judged.errors.stream()
                .filter(d -> Set.of("CS0246", "CS0103", "CS0234").contains(d.code))
                .map(d -> d.message.replaceAll("^[^']*'([^']*)'.*$", "$1")), 40));
        sb.append("\nmembers the compiler does not know (CS1061, CS0117):\n").append(counted(judged.errors.stream()
                .filter(d -> Set.of("CS1061", "CS0117").contains(d.code))
                .map(d -> d.message.replaceAll("^'([^']*)' does not contain a definition for '([^']*)'.*$", "$1.$2")), 40));
        sb.append("\nunmapped uses by library (libraries: ").append(policy.libraries().id()).append("):\n")
                .append(counted(unmapped.stream().map(m -> library(m.detail())), 20));
        sb.append("\nunmapped library uses, the mappings' work list (messages.txt has each):\n")
                .append(counted(unmapped.stream().map(PrintMessage::detail), 60));
        sb.append("\nprinter messages, by severity and code:\n").append(counted(printed.messages.stream()
                .map(m -> String.format("%-16s %s", m.severity(), m.code())), 100));
        return sb.toString();
    }

    /** The library of an unmapped use: {@code jdk}, {@code slf4j}, {@code jackson}, or the package's first segments. */
    private static String library(String detail) {
        if (detail.startsWith("java.") || detail.startsWith("javax.") || detail.startsWith("jdk.")) return "jdk";
        if (detail.startsWith("org.slf4j.")) return "slf4j";
        if (detail.startsWith("com.fasterxml.jackson.")) return "jackson";
        String[] segments = detail.split("\\.");
        return segments.length > 2 ? segments[0] + "." + segments[1] : detail;
    }

    private static String counted(Stream<String> items, int limit) {
        Map<String, Long> counts = items.collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit).map(e -> String.format("  %6d  %s%n", e.getValue(), e.getKey()))
                .collect(Collectors.joining());
    }

    private void ratchet(Map<String, Long> measured) throws IOException {
        String now = measured.entrySet().stream().map(e -> e.getKey() + "\t" + e.getValue())
                .collect(Collectors.joining("\n"));
        if (!Files.isRegularFile(ratchetFile)) {
            fail("no " + ratchetFile.toAbsolutePath() + ": record this run's numbers there, below a comment saying "
                 + "what was measured:\n" + now);
        }
        Map<String, Long> recorded = new HashMap<>();
        try (Stream<String> lines = Files.lines(ratchetFile)) {
            lines.map(String::trim).filter(l -> !l.isEmpty() && !l.startsWith("#")).forEach(l -> {
                String[] kv = l.split("\\s+");
                recorded.put(kv[0], Long.parseLong(kv[1]));
            });
        }
        List<String> worse = new ArrayList<>();
        List<String> better = new ArrayList<>();
        DIRECTION.forEach((metric, direction) -> {
            if (!measured.containsKey(metric)) return;
            Long was = recorded.get(metric);
            long is = measured.get(metric);
            if (was == null) {
                worse.add(metric + ": not in " + ratchetFile);
            } else if (is != was) {
                boolean improved = switch (direction) {
                    case "<=" -> is < was;
                    case ">=" -> is > was;
                    default -> false;
                };
                (improved ? better : worse).add(metric + ": " + was + " -> " + is);
            }
        });
        if (!worse.isEmpty()) {
            fail("THE TRANSLATION GOT WORSE on " + name + ": " + worse + ". Read build/j2cs/" + name + "/report.txt.");
        }
        if (!better.isEmpty()) {
            fail("Progress on " + name + ": " + better + ". Tighten the ratchet: write these values into "
                 + ratchetFile + " in the commit that made them.");
        }
    }

    /** Empties {@code dir}, which exists afterwards. */
    private static void deleteRecursively(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
        Files.createDirectories(dir);
    }
}
