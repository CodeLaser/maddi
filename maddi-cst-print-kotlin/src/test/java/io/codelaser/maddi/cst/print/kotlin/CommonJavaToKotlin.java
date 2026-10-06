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
import io.codelaser.maddi.cst.api.output.OutputBuilder;
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

/**
 * Java in, Kotlin out: parses a Java primary type with the openjdk front end and prints it with
 * {@link KotlinTypePrinter}. The corpus-scale counterpart, which also compiles the result, is maddi-run-openjdk's
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

    /** The Kotlin printing of every primary type declared in {@code java}, in source order, blank-line separated. */
    protected String kotlin(String java) {
        // keyed by the first type's name, as parse(fqn, input) does; the other primary types of the file come along
        String pkg = java.replaceAll("(?s)^.*?package\\s+([\\w.]+)\\s*;.*$", "$1");
        String first = java.replaceAll("(?s)^.*?(?:class|interface|enum|record)\\s+(\\w+).*$", "$1");
        List<TypeInfo> types = javaInspector.parse(Map.of(pkg + "." + first, java),
                        new JavaInspector.ParseOptions.Builder().build())
                .parseResult().primaryTypes().stream()
                .sorted(Comparator.comparing(t -> t.source() == null ? 0 : t.source().beginLine()))
                .toList();
        Runtime runtime = javaInspector.runtime();
        Formatter2Impl formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        return types.stream().map(typeInfo -> {
            OutputBuilder ob = new KotlinTypePrinter(typeInfo, true)
                    .print(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType(), true);
            return formatter.write(ob);
        }).collect(Collectors.joining("\n"));
    }
}
