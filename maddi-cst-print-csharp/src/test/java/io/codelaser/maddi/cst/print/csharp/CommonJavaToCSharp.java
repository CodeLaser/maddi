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

import io.codelaser.maddi.cst.print.translate.PrintMessage;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parses a Java compilation unit with the openjdk front end and prints it as C#. */
public abstract class CommonJavaToCSharp {
    protected JavaInspector javaInspector;
    /** The whole-program policy of the translation: see {@link CSharpProgram}. */
    protected CSharpProgram.Policy policy = CSharpProgram.Policy.DEFAULT;

    @BeforeEach
    public void beforeEach() throws IOException {
        javaInspector = new JavaInspectorImpl();
        SourceSet sourceSet = new SourceSetImpl.Builder().setName(JavaInspector.TEST_PROTOCOL)
                .setUri(URI.create("file:/")).build();
        javaInspector.initialize(new InputConfigurationImpl.Builder().addSourceSets(sourceSet)
                .addClassPath("jmod:java.base")
                // the libraries the mappings' samples use
                .addClassPath("jar-on-classpath:org/slf4j")
                .addClassPath("jar-on-classpath:com/fasterxml/jackson/databind")
                .addClassPath("jar-on-classpath:com/fasterxml/jackson/annotation")
                .addClassPath("jar-on-classpath:com/fasterxml/jackson/core")
                .addClassPath("jar-on-classpath:jakarta/xml/bind")
                .addClassPath("jar-on-classpath:io/micrometer/core")
                .addClassPath("jar-on-classpath:io/micrometer/common").build());
        javaInspector.onlyPreload();
    }

    /** The current test's method name: the samples' file names start with it. */
    private String test;

    /** The messages of the last {@link #translate(String, String)}. */
    protected List<PrintMessage<CSharpPrintCode>> messages = List.of();

    @BeforeEach
    public void testName(TestInfo info) {
        test = info.getTestMethod().map(Method::getName).orElseThrow();
    }

    /** {@code java}'s translation, written to {@code build/csharp-samples/<test>-<name>.cs}; its messages kept. */
    protected String translate(String name, String java) {
        Translation translation = translation(java);
        messages = translation.messages();
        String csharp = translation.csharp();
        try {
            Path dir = Path.of("build/csharp-samples");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(test + "-" + name + ".cs"), csharp);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return csharp;
    }

    /** The C# printing of the compilation unit {@code java}: usings, namespace, and every type in it. */
    protected String csharp(String java) {
        return translation(java).csharp();
    }

    /** The C# printing of {@code java}, and the printer's messages about it: one parse for both. */
    protected record Translation(String csharp, List<PrintMessage<CSharpPrintCode>> messages) {
    }

    protected Translation translation(String java) {
        CSharpCompilationUnitPrinter.Result result = printed(java);
        return new Translation(format(result), result.messages());
    }

    /** The printer's messages about {@code java}'s translation. */
    protected List<PrintMessage<CSharpPrintCode>> messages(String java) {
        return printed(java).messages();
    }

    private CSharpCompilationUnitPrinter.Result printed(String java) {
        List<TypeInfo> primaryTypes = parse(java);
        // the file is the whole program
        return print(new CSharpCompilationUnitPrinter(first(primaryTypes).compilationUnit(), true,
                CSharpProgram.analyze(primaryTypes, policy)));
    }

    /** {@code java} printed without knowledge of a program ({@link CSharpProgram#NONE}), as one file alone. */
    protected String csharpWithoutProgram(String java) {
        return format(print(new CSharpCompilationUnitPrinter(first(parse(java)).compilationUnit(), true)));
    }

    private CSharpCompilationUnitPrinter.Result print(CSharpCompilationUnitPrinter printer) {
        return printer.printWithMessages(new ImportComputerImpl(),
                javaInspector.runtime().qualificationQualifyFromPrimaryType());
    }

    private String format(CSharpCompilationUnitPrinter.Result result) {
        return new Formatter2Impl(javaInspector.runtime(), new FormattingOptionsImpl.Builder().build())
                .write(result.output());
    }

    /** The primary types of {@code java}, one compilation unit. */
    protected List<TypeInfo> parse(String java) {
        // keyed by the first type's name, as parse(fqn, input) does; the other primary types of the file come along.
        // The key is needed before the parse: read from the text without its comments, an @interface is no type key
        String code = java.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
        String pkg = code.replaceAll("(?s)^.*?package\\s+([\\w.]+)\\s*;.*$", "$1");
        String first = code.replaceAll("(?s)^.*?(?<![@\\w])(?:class|interface|enum|record)\\s+(\\w+).*$", "$1");
        return List.copyOf(javaInspector.parse(Map.of(pkg + "." + first, java),
                new JavaInspector.ParseOptions.Builder().build()).parseResult().primaryTypes());
    }

    private static TypeInfo first(List<TypeInfo> primaryTypes) {
        return primaryTypes.stream().min(Comparator.comparing(t -> t.source() == null ? 0 : t.source().beginLine()))
                .orElseThrow();
    }

    /** Line by line, indentation ignored: the depth of a fragment is not what these tests are about. */
    protected static void contains(String csharp, String expected) {
        assertTrue(strip(csharp).contains(strip(expected)), () -> "expected\n" + expected + "\nin\n" + csharp);
    }

    private static String strip(String s) {
        return s.lines().map(String::strip).collect(Collectors.joining("\n"));
    }
}
