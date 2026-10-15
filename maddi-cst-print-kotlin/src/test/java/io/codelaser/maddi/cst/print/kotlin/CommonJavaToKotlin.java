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

import io.codelaser.maddi.cst.print.translate.PrintMessage;
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
    protected List<PrintMessage<KotlinPrintCode>> messages(String java) {
        return messages(java, KotlinPrintOptions.DEFAULT);
    }

    /** As {@link #messages(String)}, with print options. */
    protected List<PrintMessage<KotlinPrintCode>> messages(String java, KotlinPrintOptions options) {
        return translate(java, options).messages();
    }

    /** As {@link #kotlin(String)}, after {@code beforePrint} has seen the parsed type (to set analysis results on it). */
    protected String kotlin(String java, java.util.function.Consumer<TypeInfo> beforePrint) {
        return kotlin(java, KotlinPrintOptions.DEFAULT, beforePrint);
    }

    /** As {@link #kotlin(String, java.util.function.Consumer)}, with print options. */
    protected String kotlin(String java, KotlinPrintOptions options, java.util.function.Consumer<TypeInfo> beforePrint) {
        KotlinCompilationUnitPrinter.Result result = translate(java, options, beforePrint);
        Formatter2Impl formatter = new Formatter2Impl(javaInspector.runtime(), new FormattingOptionsImpl.Builder().build());
        return formatter.write(result.output());
    }

    private KotlinCompilationUnitPrinter.Result translate(String java, KotlinPrintOptions options) {
        return translate(java, options, t -> {
        });
    }

    /**
     * As {@link #kotlin(String, KotlinPrintOptions, java.util.function.Consumer)}, with the file's primary types as
     * the {@link KotlinWorld}: the code translated together.
     */
    protected String kotlinInWorld(String java, KotlinPrintOptions options,
                                   java.util.function.Consumer<TypeInfo> beforePrint) {
        KotlinCompilationUnitPrinter.Result result = translate(java, types -> options.withWorld(KotlinWorld.of(types)),
                beforePrint);
        Formatter2Impl formatter = new Formatter2Impl(javaInspector.runtime(), new FormattingOptionsImpl.Builder().build());
        return formatter.write(result.output());
    }

    private KotlinCompilationUnitPrinter.Result translate(String java, KotlinPrintOptions options,
                                                          java.util.function.Consumer<TypeInfo> beforePrint) {
        return translate(java, types -> options, beforePrint);
    }

    private KotlinCompilationUnitPrinter.Result translate(String java,
                                                          java.util.function.Function<List<TypeInfo>, KotlinPrintOptions> options,
                                                          java.util.function.Consumer<TypeInfo> beforePrint) {
        // keyed by the first type's name, as parse(fqn, input) does; the other primary types of the file come along
        String pkg = java.replaceAll("(?s)^.*?package\\s+([\\w.]+)\\s*;.*$", "$1");
        String first = java.replaceAll("(?s)^.*?(?:class|interface|enum|record)\\s+(\\w+).*$", "$1");
        List<TypeInfo> primaryTypes = List.copyOf(javaInspector.parse(Map.of(pkg + "." + first, java),
                        new JavaInspector.ParseOptions.Builder().build())
                .parseResult().primaryTypes());
        TypeInfo typeInfo = primaryTypes.stream()
                .min(Comparator.comparing(t -> t.source() == null ? 0 : t.source().beginLine()))
                .orElseThrow();
        beforePrint.accept(typeInfo);
        Runtime runtime = javaInspector.runtime();
        return new KotlinCompilationUnitPrinter(typeInfo.compilationUnit(), true, options.apply(primaryTypes))
                .printWithMessages(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
    }

    /**
     * The library contracts the annotated archive gives in a full run, which these tests do not load: the JDK methods
     * named here, wherever {@code type}'s code calls them, are {@code @NotModified} (maddi#141: the printer reads the
     * stored verdict, not a list of names).
     */
    protected static void notModified(TypeInfo type, String... methodNames) {
        java.util.Set<String> names = java.util.Set.of(methodNames);
        io.codelaser.maddi.cst.print.kotlin.KotlinReadOnlyTestAccess.forEachBody(type, body -> body.visit(
                (io.codelaser.maddi.cst.api.element.Element e) -> {
                    if (e instanceof io.codelaser.maddi.cst.api.expression.MethodCall mc && mc.methodInfo() != null
                        && mc.methodInfo().typeInfo().fullyQualifiedName().startsWith("java.")
                        && names.contains(mc.methodInfo().name())
                        && !mc.methodInfo().analysis().haveAnalyzedValueFor(
                            io.codelaser.maddi.cst.impl.analysis.PropertyImpl.NON_MODIFYING_METHOD)) {
                        mc.methodInfo().analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.NON_MODIFYING_METHOD,
                                io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE);
                    }
                    return true;
                }));
    }
}
