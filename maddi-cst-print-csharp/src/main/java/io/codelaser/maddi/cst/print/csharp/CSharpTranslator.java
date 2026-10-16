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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import io.codelaser.maddi.cst.print.translate.PrintMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Java to C# for a list of types: each compilation unit as a {@code .cs} file under a directory, by package, and
 * beside them what the policy's library mappings need to build ({@link #writeProject}). What the CLI's
 * {@code --translate csharp} writes and the C# ratchets judge, so that the ratchets measure what ships.
 * <p>
 * A crash of the printer on one compilation unit is recorded and the others go on.
 */
public final class CSharpTranslator {

    /** The framework of the project file. */
    public static final String TARGET_FRAMEWORK = "net10.0";

    public record Result(List<Path> files, List<String> crashes, List<PrintMessage<CSharpPrintCode>> messages) {
    }

    private CSharpTranslator() {
    }

    /**
     * Each compilation unit of {@code types} as a C# file under {@code src}. The types are the whole program: what
     * one file's printing may assume of the others. A type a mapping leaves out is not printed; its uses go through
     * the mapping's rules.
     */
    public static Result translate(Runtime runtime, List<TypeInfo> types, CSharpProgram.Policy policy, Path src)
            throws IOException {
        List<Path> files = new ArrayList<>();
        List<String> crashes = new ArrayList<>();
        List<PrintMessage<CSharpPrintCode>> messages = new ArrayList<>();
        Formatter2Impl formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        Set<Object> done = new HashSet<>();
        CSharpProgram program = CSharpProgram.analyze(types, policy);
        for (TypeInfo type : types) {
            if (policy.libraries().excludes(type)) continue; // left out by a mapping, used through its rules
            if (!done.add(type.compilationUnit())) continue; // a file with two primary types prints once
            try {
                CSharpCompilationUnitPrinter.Result result = new CSharpCompilationUnitPrinter(type.compilationUnit(),
                        true, program).printWithMessages(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
                messages.addAll(result.messages());
                Path file = src.resolve(type.packageName().replace('.', '/')).resolve(type.simpleName() + ".cs");
                Files.createDirectories(file.getParent());
                Files.writeString(file, formatter.write(result.output()) + "\n");
                files.add(file.toAbsolutePath().normalize());
            } catch (RuntimeException | AssertionError | StackOverflowError e) {
                // the frames where it happened: a StackOverflowError's top frames are its cycle
                StackTraceElement[] trace = e.getStackTrace();
                crashes.add(type.fullyQualifiedName() + ": " + e + Arrays.stream(trace).limit(12)
                        .map(f -> "\n      at " + f).collect(Collectors.joining()));
            }
        }
        return new Result(files, crashes, messages);
    }

    /**
     * What goes with the translation in {@code src}: the sources the libraries' mappings need, the compatibility
     * library first, and {@code Translated.csproj}, which builds everything with the mappings' NuGet packages. With
     * the facts of a nullability analysis, the project enables C#'s nullable reference types.
     *
     * @return the library sources written
     */
    public static List<Path> writeProject(CSharpProgram.Policy policy, Path src) throws IOException {
        CSharpLibraries libraries = policy.libraries();
        Files.createDirectories(src);
        List<Path> sources = new ArrayList<>();
        for (LibraryMapping.Source source : libraries.requirements().sources()) {
            Path file = src.resolve(source.fileName()).toAbsolutePath().normalize();
            Files.writeString(file, source.content().get());
            sources.add(file);
        }
        Files.writeString(src.resolve("Translated.csproj"),
                libraries.projectFile(TARGET_FRAMEWORK, policy.facts().nullabilityAnalysed()));
        return sources;
    }
}
