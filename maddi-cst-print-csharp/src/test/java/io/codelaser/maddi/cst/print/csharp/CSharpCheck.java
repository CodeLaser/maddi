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
package io.codelaser.maddi.cst.print.csharp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;

/**
 * Roslyn's judgment of a printed C# file, for the unit tests: {@code tools/csharp-check}, the ratchets' judge
 * (JavaToCSharpRatchet, maddi-run-openjdk), kept running for the test JVM ({@code -serve}: one argument file per
 * line of its standard input). The tool is built once with {@code dotnet build}; without a .NET SDK on the PATH
 * the tests that compile are skipped, not failed, and the skip says so.
 * <p>
 * A file is judged with the sources its policy's library mappings need (the compatibility library, {@code
 * CSharpTranslator.writeProject}), written once per set of sources, and against the mappings' NuGet packages,
 * restored once per set of packages into {@code build/csharp-deps}.
 */
final class CSharpCheck {

    /** One line of the tool's output: {@code S} a syntax error, {@code E} an error, {@code N} a nullable warning. */
    record Diagnostic(String kind, Path file, int line, int column, String code, String message) {
        @Override
        public String toString() {
            return file.getFileName() + "(" + line + "," + column + "): " + code + ": " + message;
        }
    }

    private static CSharpCheck instance;

    /** The tool, started on first use; the JUnit assumption fails when it cannot run here. */
    static synchronized CSharpCheck instance() {
        if (instance == null) instance = start();
        return instance;
    }

    private final BufferedWriter requests;
    private final BufferedReader answers;
    private final Map<String, List<Path>> sourceSets = new HashMap<>();
    private final Map<String, List<Path>> packageSets = new HashMap<>();

    private CSharpCheck(Process process) {
        requests = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        answers = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Runtime.getRuntime().addShutdownHook(new Thread(process::destroy));
    }

    private static CSharpCheck start() {
        String tool = System.getProperty("maddi.test.csharpCheck");
        Assumptions.assumeTrue(tool != null && !tool.isBlank(),
                "no -Dmaddi.test.csharpCheck: run through Gradle to have Roslyn judge the printed C#");
        Path toolDir = Path.of(tool);
        try {
            Path out = Path.of("build/csharp-check");
            Files.createDirectories(out);
            Path buildLog = out.resolve("build.log");
            Process build;
            try {
                build = new ProcessBuilder("dotnet", "build", toolDir.toString(), "-c", "Release", "-nologo", "-v", "q")
                        .redirectErrorStream(true).redirectOutput(buildLog.toFile()).start();
            } catch (IOException e) {
                Assumptions.abort("no dotnet on the PATH: the printed C# is not judged by Roslyn here (" + e + ")");
                throw new IllegalStateException(e);
            }
            if (build.waitFor() != 0) {
                throw new AssertionError("building " + toolDir + " failed (a .NET 10 SDK on the PATH?); see "
                                         + buildLog.toAbsolutePath());
            }
            Path dll = toolDir.resolve("build/bin/Release/net10.0/csharp-check.dll");
            Process process = new ProcessBuilder("dotnet", dll.toString(), "-serve")
                    .redirectError(out.resolve("serve.err").toFile()).start();
            return new CSharpCheck(process);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@code file} judged with what {@code policy}'s libraries need, C#'s nullable reference types enabled when the
     * policy's facts are a nullability analysis's.
     */
    synchronized List<Diagnostic> check(Path file, CSharpProgram.Policy policy) {
        try {
            CSharpLibraries libraries = policy.libraries();
            LibraryMapping.Requirements requirements = libraries.requirements();
            List<String> arguments = new ArrayList<>();
            arguments.add(file.toAbsolutePath().toString());
            sources(policy, requirements).forEach(p -> arguments.add(p.toString()));
            packages(requirements).forEach(p -> arguments.add("-r:" + p));
            if (policy.facts().nullabilityAnalysed()) arguments.add("-nullable");
            Path argFile = Files.createTempFile(Path.of("build/csharp-check"), "check", ".args");
            Files.write(argFile, arguments);
            requests.write(argFile.toAbsolutePath().toString());
            requests.newLine();
            requests.flush();
            List<Diagnostic> diagnostics = new ArrayList<>();
            String line;
            while ((line = answers.readLine()) != null) {
                if (line.startsWith("F\t")) break;
                String[] f = line.split("\t", 6);
                if (f.length < 6) continue;
                diagnostics.add(new Diagnostic(f[0], Path.of(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3]),
                        f[4], f[5]));
            }
            if (line == null) throw new AssertionError("csharp-check stopped; see build/csharp-check/serve.err");
            Files.delete(argFile);
            return diagnostics;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The library sources of the policy, written once per set under {@code build/csharp-samples/lib-<n>}. */
    private List<Path> sources(CSharpProgram.Policy policy, LibraryMapping.Requirements requirements)
            throws IOException {
        String key = requirements.sources().stream().map(LibraryMapping.Source::fileName).sorted().toList()
                .toString();
        List<Path> sources = sourceSets.get(key);
        if (sources == null) {
            Path dir = Path.of("build/csharp-samples/lib-" + sourceSets.size());
            sources = CSharpTranslator.writeProject(policy, dir);
            sourceSets.put(key, sources);
        }
        return sources;
    }

    /**
     * The assemblies of the mappings' packages, with their dependencies: a project that references them, built
     * once into {@code build/csharp-deps/<n>/bin} (NuGet's restore, which needs the packages in the local cache
     * or the network). None without packages.
     */
    private List<Path> packages(LibraryMapping.Requirements requirements) throws IOException {
        List<LibraryMapping.NuGetPackage> packages = requirements.packages();
        if (packages.isEmpty()) return List.of();
        String key = packages.stream().map(p -> p.id() + "@" + p.version()).sorted().toList().toString();
        List<Path> dlls = packageSets.get(key);
        if (dlls != null) return dlls;
        Path deps = Path.of("build/csharp-deps/" + packageSets.size()).toAbsolutePath();
        Files.createDirectories(deps);
        StringBuilder project = new StringBuilder("<Project Sdk=\"Microsoft.NET.Sdk\">\n  <PropertyGroup>\n")
                .append("    <TargetFramework>").append(CSharpTranslator.TARGET_FRAMEWORK)
                .append("</TargetFramework>\n")
                .append("    <CopyLocalLockFileAssemblies>true</CopyLocalLockFileAssemblies>\n")
                .append("  </PropertyGroup>\n  <ItemGroup>\n");
        packages.forEach(p -> project.append("    <PackageReference Include=\"").append(p.id())
                .append("\" Version=\"").append(p.version()).append("\" />\n"));
        Files.writeString(deps.resolve("deps.csproj"), project.append("  </ItemGroup>\n</Project>\n").toString());
        Path bin = deps.resolve("bin");
        Path log = deps.resolve("build.log");
        try {
            int built = new ProcessBuilder("dotnet", "build", deps.toString(), "-c", "Release", "-o", bin.toString(),
                    "-nologo", "-v", "q").redirectErrorStream(true).redirectOutput(log.toFile()).start().waitFor();
            if (built != 0) {
                throw new AssertionError("restoring the packages " + packages + " failed; see " + log);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        try (Stream<Path> files = Files.list(bin)) {
            dlls = files.filter(f -> f.getFileName().toString().endsWith(".dll")
                                     && !f.getFileName().toString().equals("deps.dll")).sorted().toList();
        }
        packageSets.put(key, dlls);
        return dlls;
    }
}
