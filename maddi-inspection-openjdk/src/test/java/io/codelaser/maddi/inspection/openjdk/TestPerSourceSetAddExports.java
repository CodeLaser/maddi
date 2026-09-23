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

package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A source set compiles against {@link SourceSet#addExports()} -- javac's {@code --add-exports} -- of its own.
 * <p>
 * Found configuring the jfocus/maddi workspace (2026-09-14): {@code maddi-java-openjdk} is built with five
 * {@code --add-exports jdk.compiler/com.sun.tools.javac.*}, nothing carried them into the parse, and 8 of its 17
 * compilation units were dropped on {@code Type 'Types' not found}. The probe here is the same type.
 */
public class TestPerSourceSetAddExports {

    @Language("java")
    private static final String INPUT = """
            package p;
            import com.sun.tools.javac.code.Types;
            public class UsesJavacTypes {
                Types types;
            }
            """;

    private static ParseResult parse(List<String> addExports) throws java.io.IOException {
        JavaInspector javaInspector = new JavaInspectorImpl();
        SourceSet sourceSet = new SourceSetImpl.Builder()
                .setName(TEST_PROTOCOL)
                .setUri(URI.create("file:/"))
                .setAddExports(addExports)
                .build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath("jmod:jdk.compiler")
                .build();
        javaInspector.initialize(inputConfiguration);
        return javaInspector.parse(Map.of("p.UsesJavacTypes", INPUT), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
    }

    @DisplayName("a set that opens com.sun.tools.javac.code resolves javac's Types")
    @Test
    public void theSetsOwnExportsReachJavac() throws java.io.IOException {
        // the build's spelling, naming its own module as well: the parse compiles unnamed, so it must re-target
        ParseResult parseResult = parse(List.of("jdk.compiler/com.sun.tools.javac.code=my.module,ALL-UNNAMED"));
        TypeInfo usesTypes = parseResult.findType("p.UsesJavacTypes");
        assertNotNull(usesTypes, "the unit is dropped when com.sun.tools.javac.code is not exported to it");
        FieldInfo types = usesTypes.getFieldByName("types", true);
        assertEquals("com.sun.tools.javac.code.Types", types.type().typeInfo().fullyQualifiedName());
    }

    @DisplayName("CONTROL: without the export, the same unit does not resolve")
    @Test
    public void withoutTheExportTheUnitIsDropped() throws java.io.IOException {
        ParseResult parseResult;
        try {
            parseResult = parse(List.of());
        } catch (RuntimeException refused) {
            return; // a refused parse is the other way of saying the same thing
        }
        TypeInfo usesTypes = parseResult.findType("p.UsesJavacTypes");
        if (java.lang.Runtime.version().feature() < 27) {
            assertNull(usesTypes, "com.sun.tools.javac.code is not exported to the unnamed module; if this resolves,"
                                  + " the test above proves nothing");
            return;
        }
        // JDK 27's javac recovers from the unexported symbol: the unit survives, and maddi STUBS the type it cannot
        // read -- a compilation unit with no source set (InfoByFqn, GAP #163). Resolved, it would have one.
        assertNotNull(usesTypes, "JDK 27 keeps the unit");
        TypeInfo types = usesTypes.getFieldByName("types", true).type().typeInfo();
        assertNull(types.compilationUnit().sourceSet(),
                "without the export Types must be a stub; if it resolves, the test above proves nothing");
    }
}
