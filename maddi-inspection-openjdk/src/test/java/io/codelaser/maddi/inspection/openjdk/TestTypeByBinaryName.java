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
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ParseResult#typeByBinaryName} is the inverse of {@link TypeInfo#binaryName()}: it resolves the form a
 * string literal holds, {@code a.b.Outer$Inner}.
 * <p>
 * ⭐ <b>It is a lookup, not a parse of the string, and {@code Weird$Name} is why.</b> A {@code $} is a legal
 * identifier character, so no split of the binary name can tell "the nested type Name inside Weird" from "the
 * top-level type called Weird$Name" — only the types actually declared can. Splitting at the first {@code $} and
 * resolving the prefix, which is what the one hand-rolled reader in the toolkit does, answers the second as the
 * first.
 */
public class TestTypeByBinaryName {

    private JavaInspector javaInspector;
    private SourceSet sourceSet;

    @BeforeEach
    public void before() throws IOException {
        javaInspector = new JavaInspectorImpl();
        sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "1").setUri(URI.create("file:/")).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    @Language("java")
    private static final String OUTER = """
            package a.b;
            public class Outer {
                public static class Inner {
                    public static class Deeper { }
                }
                public Runnable anonymous() {
                    return new Runnable() {
                        @Override public void run() { }
                    };
                }
            }
            """;

    /** A top-level type whose SIMPLE NAME contains a dollar: legal Java, and the reason a split cannot work. */
    @Language("java")
    private static final String DOLLAR = """
            package a.b;
            public class Weird$Name { }
            """;

    private static List<String> fqns(List<TypeInfo> types) {
        return types.stream().map(TypeInfo::fullyQualifiedName).sorted().toList();
    }

    @DisplayName("top-level, nested, doubly nested, a $ in the simple name, and the names that resolve to nothing")
    @Test
    public void test() {
        ParseResult parseResult = javaInspector.parseMultiSourceSet(
                        Map.of(sourceSet, Map.of("a.b.Outer", OUTER, "a.b.Weird$Name", DOLLAR)),
                        JavaInspectorImpl.DETAILED_SOURCES)
                .parseResult();

        assertEquals(List.of("a.b.Outer"), fqns(parseResult.typeByBinaryName("a.b.Outer")));
        assertEquals(List.of("a.b.Outer.Inner"), fqns(parseResult.typeByBinaryName("a.b.Outer$Inner")));
        assertEquals(List.of("a.b.Outer.Inner.Deeper"),
                fqns(parseResult.typeByBinaryName("a.b.Outer$Inner$Deeper")));

        // ⭐ the whole point: this names a TOP-LEVEL type, not Name inside Weird
        assertEquals(List.of("a.b.Weird$Name"), fqns(parseResult.typeByBinaryName("a.b.Weird$Name")));

        // and the round trip holds for every type the parse knows
        TypeInfo deeper = parseResult.findType("a.b.Outer.Inner.Deeper");
        assertEquals("a.b.Outer$Inner$Deeper", deeper.binaryName());
        assertEquals(List.of(deeper), parseResult.typeByBinaryName(deeper.binaryName()));

        // the DOTTED form of a nested type is not a binary name, and must not resolve as one
        assertEquals(List.of(), fqns(parseResult.typeByBinaryName("a.b.Outer.Inner")));
        // the JVM's numbering of an anonymous class is not maddi's: honestly, nothing
        assertEquals(List.of(), fqns(parseResult.typeByBinaryName("a.b.Outer$1")));
        assertEquals(List.of(), fqns(parseResult.typeByBinaryName("a.b.NoSuchType")));
        assertEquals(List.of(), fqns(parseResult.typeByBinaryName("")));
    }
}
