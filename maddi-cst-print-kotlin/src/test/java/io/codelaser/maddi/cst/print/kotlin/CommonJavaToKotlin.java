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

package io.codelaser.maddi.cst.print.kotlin;

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

/**
 * Java in, Kotlin out: parses a Java compilation unit with the openjdk front end and prints it with
 * {@link KotlinCompilationUnitPrinter}. The corpus-scale counterpart, which also compiles the result, is maddi-run-openjdk's
 * TestJavaToKotlinFernflower.
 */
public abstract class CommonJavaToKotlin {
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

    /** The Kotlin printing of the compilation unit {@code java}: package, imports, and every type in it. */
    protected String kotlin(String java) {
        return kotlin(java, KotlinPrintOptions.DEFAULT);
    }

    /** As {@link #kotlin(String)}, with print options: nullability verdicts, the null-check policy. */
    protected String kotlin(String java, KotlinPrintOptions options) {
        KotlinCompilationUnitPrinter.Result result = translate(java, options);
        Formatter2Impl formatter = new Formatter2Impl(javaInspector.runtime(), new FormattingOptionsImpl.Builder().build());
        return formatter.write(result.output());
    }

    /** The printer's messages about {@code java}'s translation. */
    protected List<KotlinPrintMessage> messages(String java) {
        return messages(java, KotlinPrintOptions.DEFAULT);
    }

    /** As {@link #messages(String)}, with print options. */
    protected List<KotlinPrintMessage> messages(String java, KotlinPrintOptions options) {
        return translate(java, options).messages();
    }

    private KotlinCompilationUnitPrinter.Result translate(String java, KotlinPrintOptions options) {
        // keyed by the first type's name, as parse(fqn, input) does; the other primary types of the file come along
        String pkg = java.replaceAll("(?s)^.*?package\\s+([\\w.]+)\\s*;.*$", "$1");
        String first = java.replaceAll("(?s)^.*?(?:class|interface|enum|record)\\s+(\\w+).*$", "$1");
        TypeInfo typeInfo = javaInspector.parse(Map.of(pkg + "." + first, java),
                        new JavaInspector.ParseOptions.Builder().build())
                .parseResult().primaryTypes().stream()
                .min(Comparator.comparing(t -> t.source() == null ? 0 : t.source().beginLine()))
                .orElseThrow();
        Runtime runtime = javaInspector.runtime();
        return new KotlinCompilationUnitPrinter(typeInfo.compilationUnit(), true, options)
                .printWithMessages(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
    }
}
