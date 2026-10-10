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

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parses a Java compilation unit with the openjdk front end and prints it as C#. */
public abstract class CommonJavaToCSharp {
    protected JavaInspector javaInspector;

    @BeforeEach
    public void beforeEach() throws IOException {
        javaInspector = new JavaInspectorImpl();
        SourceSet sourceSet = new SourceSetImpl.Builder().setName(JavaInspector.TEST_PROTOCOL)
                .setUri(URI.create("file:/")).build();
        javaInspector.initialize(new InputConfigurationImpl.Builder().addSourceSets(sourceSet)
                .addClassPath("jmod:java.base").build());
        javaInspector.onlyPreload();
    }

    /** The C# printing of the compilation unit {@code java}: usings, namespace, and every type in it. */
    protected String csharp(String java) {
        CSharpCompilationUnitPrinter.Result result = translate(java);
        Formatter2Impl formatter = new Formatter2Impl(javaInspector.runtime(), new FormattingOptionsImpl.Builder().build());
        return formatter.write(result.output());
    }

    /** The printer's messages about {@code java}'s translation. */
    protected List<CSharpPrintMessage> messages(String java) {
        return translate(java).messages();
    }

    private CSharpCompilationUnitPrinter.Result translate(String java) {
        // keyed by the first type's name, as parse(fqn, input) does; the other primary types of the file come along
        String pkg = java.replaceAll("(?s)^.*?package\\s+([\\w.]+)\\s*;.*$", "$1");
        String first = java.replaceAll("(?s)^.*?(?:class|interface|enum|record)\\s+(\\w+).*$", "$1");
        List<TypeInfo> primaryTypes = List.copyOf(javaInspector.parse(Map.of(pkg + "." + first, java),
                        new JavaInspector.ParseOptions.Builder().build()).parseResult().primaryTypes());
        TypeInfo typeInfo = primaryTypes.stream()
                .min(Comparator.comparing(t -> t.source() == null ? 0 : t.source().beginLine()))
                .orElseThrow();
        Runtime runtime = javaInspector.runtime();
        // the file is the whole program
        return new CSharpCompilationUnitPrinter(typeInfo.compilationUnit(), true, CSharpProgram.analyze(primaryTypes))
                .printWithMessages(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
    }

    /** Line by line, indentation ignored: the depth of a fragment is not what these tests are about. */
    protected static void contains(String csharp, String expected) {
        assertTrue(strip(csharp).contains(strip(expected)), () -> "expected\n" + expected + "\nin\n" + csharp);
    }

    private static String strip(String s) {
        return s.lines().map(String::strip).collect(Collectors.joining("\n"));
    }
}
