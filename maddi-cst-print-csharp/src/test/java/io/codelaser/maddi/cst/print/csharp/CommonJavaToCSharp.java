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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    /** Where the golden files are: the whole expected translation of a structural sample. */
    private static final Path GOLDEN = Path.of("src/test/resources/io/codelaser/maddi/cst/print/csharp/golden");

    /**
     * Whether this class's samples are compared whole with their golden file, {@code golden/<test>-<name>.cs}: a
     * structural sample, whose every line is the point. A missing golden file is written and the test fails, so that
     * a new sample's translation is looked at once; {@code -PupdateGolden} rewrites them all.
     */
    protected boolean golden() {
        return false;
    }

    /** {@code java}'s translation, written to {@code build/csharp-samples/<test>-<name>.cs}; its messages kept. */
    protected String translate(String name, String java) {
        Translation translation = translation(java);
        messages = translation.messages();
        String csharp = translation.csharp();
        String fileName = test + "-" + name + ".cs";
        try {
            Path dir = Path.of("build/csharp-samples");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(fileName), csharp);
            if (golden()) {
                Path file = GOLDEN.resolve(fileName);
                if (Boolean.getBoolean("csharp.golden.update") || !Files.exists(file)) {
                    boolean created = !Files.exists(file);
                    Files.createDirectories(GOLDEN);
                    Files.writeString(file, csharp);
                    assertFalse(created && !Boolean.getBoolean("csharp.golden.update"),
                            "new golden file " + file + ": look at it, and run again");
                } else {
                    assertEquals(Files.readString(file), csharp, "the golden file " + file
                                                                 + " (-PupdateGolden after an intended change)");
                }
            }
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
