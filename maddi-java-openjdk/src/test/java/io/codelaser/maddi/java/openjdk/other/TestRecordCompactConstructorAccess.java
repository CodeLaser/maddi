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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.MethodModifier;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The access modifier of a record's COMPACT canonical constructor: its value, and its source position.
 *
 * <h2>Why this test is on the OPENJDK front end and not the hand-written parser</h2>
 * Both front ends build the same CST, and the same test in {@code maddi-java-parser} passes. Every consumer
 * that matters here — the refactoring engine — parses with {@code inspection.openjdk.JavaInspectorImpl}, and a
 * test written against the other parser proves nothing about it. The access value is now correct on both,
 * because it is computed in the shared builder; the SOURCE POSITION of the modifier is recorded by each front
 * end separately, and that is what the second test below pins.
 *
 * <h2>Why the position matters as much as the value</h2>
 * A refactoring that widens a member has to REWRITE the modifier, and to do that it needs to know where the
 * modifier is written. {@code ChangeAccessGeneratorImpl} looks it up as
 * {@code methodInfo.source().detailedSources().detail(modifier)} and refuses when it is null. Splitting trino's
 * {@code HiveWriterFactory.createWriter} widened {@code private record DataColumn} and could not widen its
 * compact canonical constructor, so the file stopped compiling:
 * <pre>
 * invalid canonical constructor in record DataColumn
 *     (attempting to assign stronger access privileges; was package)
 * </pre>
 * The value was right by then; the position was missing, and a missing position is indistinguishable from
 * "nothing to do" unless something asserts it.
 */
public class TestRecordCompactConstructorAccess extends CommonTest {

    @Language("java")
    private static final String PRIVATE_COMPACT = """
            package a.b;
            class C {
                private record DataColumn(String name, String type) {
                    private DataColumn {
                        assert name != null;
                    }
                }
            }
            """;

    @DisplayName("an explicit 'private' on a compact canonical constructor is kept")
    @Test
    public void testAccessValue() {
        TypeInfo c = scan("a.b.C", PRIVATE_COMPACT);
        MethodInfo cc = c.findSubType("DataColumn").findConstructor(2);
        assertTrue(cc.isCompactConstructor(), "expected the compact form, not the explicit one");
        assertEquals("PRIVATE", cc.access().toString(),
                "the source declares 'private DataColumn {', and JLS 8.10.4.1 allows it");
    }

    @DisplayName("the 'private' keyword of a compact canonical constructor has a source position")
    @Test
    public void testModifierHasASourcePosition() {
        TypeInfo c = scan("a.b.C", PRIVATE_COMPACT);
        MethodInfo cc = c.findSubType("DataColumn").findConstructor(2);
        MethodModifier accessModifier = cc.methodModifiers().stream()
                .filter(MethodModifier::isAccessModifier)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no access modifier recorded at all; modifiers: "
                                                      + cc.methodModifiers()));
        assertNotNull(cc.source().detailedSources(),
                "the compact constructor carries no detailed sources at all");
        assertNotNull(cc.source().detailedSources().detail(accessModifier),
                () -> "no source position for the 'private' keyword of the compact canonical constructor, so"
                      + " nothing can rewrite it. The keyword is at line 4 of the fixture; the constructor's"
                      + " own source is " + cc.source());
    }

    /** The explicit form, as the control: same constructor, written out, and it has always worked. */
    @Language("java")
    private static final String PRIVATE_EXPLICIT = """
            package a.b;
            class C {
                private record Explicit(String name, String type) {
                    private Explicit(String name, String type) {
                        this.name = name;
                        this.type = type;
                    }
                }
            }
            """;

    @DisplayName("control: the EXPLICIT canonical constructor has both its access and its position")
    @Test
    public void testExplicitFormIsAlreadyCorrect() {
        TypeInfo c = scan("a.b.C", PRIVATE_EXPLICIT);
        MethodInfo cc = c.findSubType("Explicit").findConstructor(2);
        assertEquals("PRIVATE", cc.access().toString());
        MethodModifier accessModifier = cc.methodModifiers().stream()
                .filter(MethodModifier::isAccessModifier).findFirst().orElseThrow();
        assertNotNull(cc.source().detailedSources().detail(accessModifier));
    }
}
