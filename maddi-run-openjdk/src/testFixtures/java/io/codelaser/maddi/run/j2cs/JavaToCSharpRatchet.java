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

package io.codelaser.maddi.run.j2cs;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.csharp.CSharpCompilationUnitPrinter;
import io.codelaser.maddi.cst.print.csharp.CSharpPrintMessage;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
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
 *
 * <h2>The census</h2>
 * The printer reports every JDK type or member it prints without a BCL counterpart ({@code UNMAPPED_JDK}); their
 * number is ratcheted, and the report lists them by frequency: the mapping's work list.
 * <p>
 * The output lands in {@code build/j2cs/<name>/}: the translated sources, the judge's output and {@code report.txt}.
 */
public record JavaToCSharpRatchet(String name, Path ratchetFile) {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaToCSharpRatchet.class);

    /** name -> direction. "=" must not move, "<=" may only go down, ">=" may only go up. */
    private static final Map<String, String> DIRECTION = new LinkedHashMap<>();

    static {
        DIRECTION.put("types", "=");
        DIRECTION.put("printerCrashes", "<=");
        DIRECTION.put("syntaxErrors", "<=");
        DIRECTION.put("syntaxCleanFiles", ">=");
        DIRECTION.put("compilingFiles", ">=");
        DIRECTION.put("unmappedJdkUses", "<=");
    }

    /** Translates, judges, writes the report, and holds the numbers to the ratchet file. */
    public void run(JavaToKotlinRatchet.Corpus corpus) throws Exception {
        Path out = Path.of("build/j2cs", name);
        deleteRecursively(out);
        Printed printed = print(corpus, corpus.types(), out.resolve("src"));
        Judged judged = judge(printed.files, out);

        Set<Path> withErrors = judged.errors.stream().map(Diagnostic::file).collect(Collectors.toSet());
        Set<Path> withSyntaxErrors = judged.syntaxErrors.stream().map(Diagnostic::file).collect(Collectors.toSet());
        long compiling = printed.files.stream().filter(f -> !withErrors.contains(f) && !withSyntaxErrors.contains(f))
                .count();
        List<CSharpPrintMessage> unmapped = printed.messages.stream()
                .filter(m -> m.code() == CSharpPrintMessage.Code.UNMAPPED_JDK).toList();

        Map<String, Long> measured = new LinkedHashMap<>();
        measured.put("types", (long) corpus.types().size());
        measured.put("printerCrashes", (long) printed.crashes.size());
        measured.put("syntaxErrors", (long) judged.syntaxErrors.size());
        measured.put("syntaxCleanFiles", (long) (printed.files.size() - withSyntaxErrors.size()));
        measured.put("compilingFiles", compiling);
        measured.put("unmappedJdkUses", (long) unmapped.size());

        String report = report(measured, printed, judged, unmapped);
        Files.writeString(out.resolve("report.txt"), report);
        Files.write(out.resolve("messages.txt"), printed.messages.stream().map(CSharpPrintMessage::toString).toList());
        LOGGER.info("\n{}", report);
        ratchet(measured);
    }

    private record Printed(List<Path> files, List<String> crashes, List<CSharpPrintMessage> messages) {
    }

    /** Each compilation unit of {@code types} as a C# file under {@code src}. */
    private static Printed print(JavaToKotlinRatchet.Corpus corpus, List<TypeInfo> types, Path src) throws IOException {
        List<Path> files = new ArrayList<>();
        List<String> crashes = new ArrayList<>();
        List<CSharpPrintMessage> messages = new ArrayList<>();
        Runtime runtime = corpus.javaInspector().runtime();
        Formatter2Impl formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        Set<Object> done = new HashSet<>();
        for (TypeInfo type : types) {
            if (!done.add(type.compilationUnit())) continue; // a file with two primary types prints once
            try {
                CSharpCompilationUnitPrinter.Result result = new CSharpCompilationUnitPrinter(type.compilationUnit(),
                        true).printWithMessages(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
                messages.addAll(result.messages());
                Path file = src.resolve(type.packageName().replace('.', '/')).resolve(type.simpleName() + ".cs");
                Files.createDirectories(file.getParent());
                Files.writeString(file, formatter.write(result.output()) + "\n");
                files.add(file.toAbsolutePath().normalize());
            } catch (RuntimeException | StackOverflowError e) {
                crashes.add(type.fullyQualifiedName() + ": " + e);
            }
        }
        return new Printed(files, crashes, messages);
    }

    // ---------------------------------------------------------------- the judge

    private record Diagnostic(Path file, int line, String code, String message) {
    }

    private record Judged(List<Diagnostic> syntaxErrors, List<Diagnostic> errors) {
    }

    private static Judged judge(List<Path> files, Path out) throws Exception {
        String tool = System.getProperty("maddi.test.csharpCheck");
        assertTrue(tool != null && !tool.isBlank(), "no -Dmaddi.test.csharpCheck: run this through Gradle");
        Path toolDir = Path.of(tool);
        Path buildLog = out.resolve("csharp-check-build.log");
        int built = new ProcessBuilder("dotnet", "build", toolDir.toString(), "-c", "Release", "-nologo", "-v", "q")
                .redirectErrorStream(true).redirectOutput(buildLog.toFile()).start().waitFor();
        if (built != 0) fail("building " + toolDir + " failed (is a .NET 10 SDK on the PATH?); see " + buildLog);

        Path argFile = out.resolve("csharp-check.args");
        Files.write(argFile, files.stream().map(Path::toString).toList());
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
        for (String line : lines) {
            String[] f = line.split("\t", 6);
            if (f.length < 6) continue;
            Diagnostic d = new Diagnostic(Path.of(f[1]).toAbsolutePath().normalize(), Integer.parseInt(f[2]), f[4], f[5]);
            if ("S".equals(f[0])) syntax.add(d);
            else if ("E".equals(f[0])) errors.add(d);
        }
        return new Judged(syntax, errors);
    }

    // ---------------------------------------------------------------- report and ratchet

    private String report(Map<String, Long> measured, Printed printed, Judged judged,
                          List<CSharpPrintMessage> unmapped) {
        StringBuilder sb = new StringBuilder("Java -> C#, " + name + "\n\n");
        measured.forEach((k, v) -> sb.append(String.format("  %-18s %6d%n", k, v)));
        sb.append(String.format("  %-18s %6d   (reported, not ratcheted)%n", "errors", judged.errors.size()));
        if (!printed.crashes.isEmpty()) {
            sb.append("\nprinter crashes:\n");
            printed.crashes.forEach(c -> sb.append("  ").append(c).append('\n'));
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
        sb.append("\nunmapped JDK uses, the BCL mapping's work list (messages.txt has each):\n")
                .append(counted(unmapped.stream().map(CSharpPrintMessage::detail), 60));
        sb.append("\nprinter messages, by severity and code:\n").append(counted(printed.messages.stream()
                .map(m -> String.format("%-16s %s", m.severity(), m.code())), 100));
        return sb.toString();
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
