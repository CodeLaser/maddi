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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The access of a record's COMPACT canonical constructor, which is currently always reported as public.
 *
 * <h2>The rule (JLS 8.10.4.1)</h2>
 * A canonical constructor may be declared with an access modifier, and it must then provide at least as much
 * access as the record class itself. If it is declared WITHOUT one, it has the same access as the record class
 * — not public.
 *
 * <h2>What is reported instead</h2>
 * {@code MethodInspectionImpl.Builder.computeAccess} answers public for a compact constructor before it looks
 * at anything else:
 * <pre>
 * if (methodInfo.isCompactConstructor()) {
 *     setAccess(AccessEnum.PUBLIC);
 * } else if (methodModifiers.stream().anyMatch(MethodModifier::isPrivate)) {
 * ...
 * </pre>
 * So an explicit modifier on a compact constructor is discarded, and a compact constructor with no modifier
 * is widened to public instead of taking the record's access. The EXPLICIT form of the same constructor —
 * {@code private DataColumn(String name, String type) { ... }} — is parsed correctly, which is what makes the
 * gap specific rather than general.
 *
 * <h2>Where it was found</h2>
 * A refactoring that moves a method's body into a new class beside the original has to widen the private
 * members the moved code reaches. Splitting trino's {@code HiveWriterFactory.createWriter}, it widened the
 * nested {@code private record DataColumn} and left its compact canonical constructor {@code private},
 * because it was told the constructor was already public. That pair does not compile, twice over:
 * <pre>
 * HiveWriterFactory.java: invalid canonical constructor in record DataColumn
 *                         (attempting to assign stronger access privileges; was package)
 * SplitCreateWriter.java: DataColumn(String,HiveType) has private access in HiveWriterFactory.DataColumn
 * </pre>
 * Any caller that decides something from {@code access()} — a visibility check, a call-site legality check, a
 * widening step — is given the wrong answer for this one shape.
 */
public class TestParseRecordCompactConstructorAccess extends CommonTestParse {

    @Language("java")
    static final String PRIVATE_RECORD_PRIVATE_COMPACT = """
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
    public void testPrivateCompactConstructor() {
        TypeInfo typeInfo = parse(PRIVATE_RECORD_PRIVATE_COMPACT);
        TypeInfo dataColumn = typeInfo.findSubType("DataColumn");
        assertTrue(dataColumn.isPrivate());
        MethodInfo cc = dataColumn.findConstructor(2);
        assertTrue(cc.isCompactConstructor(), "expected the compact form, not the explicit one");
        assertEquals("PRIVATE", cc.access().toString(),
                "the source declares 'private DataColumn {', and JLS 8.10.4.1 lets it, so the access of the"
                + " canonical constructor is private");
    }

    @Language("java")
    private static final String PACKAGE_RECORD_NO_MODIFIER = """
            package a.b;
            class C {
                record Pair(String left, String right) {
                    Pair {
                        assert left != null;
                    }
                }
            }
            """;

    @DisplayName("a compact canonical constructor with no modifier takes the record's access, not public")
    @Test
    public void testNoModifierTakesTheRecordsAccess() {
        TypeInfo typeInfo = parse(PACKAGE_RECORD_NO_MODIFIER);
        TypeInfo pair = typeInfo.findSubType("Pair");
        MethodInfo cc = pair.findConstructor(2);
        assertTrue(cc.isCompactConstructor(), "expected the compact form, not the explicit one");
        assertEquals("PACKAGE", cc.access().toString(),
                "the record is package-private and its canonical constructor declares no modifier, so JLS"
                + " 8.10.4.1 gives it the record's access -- package, not public");
    }

    @Language("java")
    private static final String PUBLIC_RECORD_PUBLIC_COMPACT = """
            package a.b;
            public class C {
                public record Visible(String name) {
                    public Visible {
                        assert name != null;
                    }
                }
            }
            """;

    @DisplayName("control: an explicit 'public' is kept -- this is the one case that passes today")
    @Test
    public void testPublicCompactConstructor() {
        TypeInfo typeInfo = parse(PUBLIC_RECORD_PUBLIC_COMPACT);
        TypeInfo visible = typeInfo.findSubType("Visible");
        MethodInfo cc = visible.findConstructor(1);
        assertTrue(cc.isCompactConstructor());
        assertEquals("PUBLIC", cc.access().toString());
    }

    @Language("java")
    private static final String EXPLICIT_PRIVATE_CANONICAL = """
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

    @DisplayName("control: the EXPLICIT canonical constructor keeps its private -- so the gap is the compact form only")
    @Test
    public void testExplicitCanonicalConstructorIsAlreadyCorrect() {
        TypeInfo typeInfo = parse(EXPLICIT_PRIVATE_CANONICAL);
        TypeInfo explicit = typeInfo.findSubType("Explicit");
        MethodInfo cc = explicit.findConstructor(2);
        assertEquals("PRIVATE", cc.access().toString(),
                "written out in full, the same constructor is parsed correctly; only the compact form loses"
                + " its modifier");
    }
}
