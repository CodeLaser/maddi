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

package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JavaDoc on FIELDS and ENUM CONSTANTS. Types ({@link TestJavaDoc2}) and methods have carried resolved javadoc for
 * a long time; fields did not, because {@code ScanCompilationUnit.createField} neither attached the doc comment nor
 * deferred its commit — so the end-of-scan resolver in {@code ScanCompilationUnits.scanJavaDocsAndCommit} could not
 * re-open the builder. Consumers of {@code javaDoc().tags()} (the refactoring toolkit's move/rename, which retargets
 * a {@code {@link X}} to X's new location) therefore saw nothing on a field.
 */
public class TestJavaDocField extends CommonTest {

    @Language("java")
    String aA = """
            package a;
            public class A {
                public static final int CONSTANT = 3;
            }
            """;

    @Language("java")
    String bB = """
            package b;
            import a.A;
            public class B {
                /**
                 * A field, see {@link a.A}
                 */
                public int f = 1;
            }
            """;

    @DisplayName("a field's javadoc is attached to the FieldInfo, and its reference is resolved")
    @Test
    public void testField() {
        Map<String, TypeInfo> pr = scan(false, "a.A", aA, "b.B", bB);
        TypeInfo B = pr.get("b.B");
        FieldInfo f = B.getFieldByName("f", true);
        assertNotNull(f.javaDoc(), "field javadoc not attached");

        JavaDoc.Tag tag = f.javaDoc().tags().getFirst();
        assertEquals("a.A", tag.resolvedReference().toString(), "field javadoc reference not resolved");

        // the token span of the written reference, needed by every consumer that rewrites the reference
        DetailedSources detailedSources = tag.source().detailedSources();
        assertNotNull(detailedSources);
        assertEquals(tag.sourceOfReference().compact2(),
                detailedSources.detail(tag.resolvedReference()).compact2());
        // the 3-character token "a.A" on line 5, after "     * A field, see {@link "
        assertEquals("5-28:5-30", tag.sourceOfReference().compact2());
    }

    @Language("java")
    String bC = """
            package b;
            public class C {
                /**
                 * See {@link D}
                 */
                public int f = 1;
            }
            """;

    @Language("java")
    String bD = """
            package b;
            public class D {
            }
            """;

    @DisplayName("a field's javadoc reference written by SIMPLE name resolves, token not sized to the fqn")
    @Test
    public void testFieldSimpleName() {
        Map<String, TypeInfo> pr = scan(false, "b.C", bC, "b.D", bD);
        TypeInfo C = pr.get("b.C");
        JavaDoc.Tag tag = C.getFieldByName("f", true).javaDoc().tags().getFirst();
        assertEquals("b.D", tag.resolvedReference().toString());
        DetailedSources detailedSources = tag.source().detailedSources();
        assertNotNull(detailedSources);
        // regression guard, as in TestJavaDoc2.test4: the detailed source is the simple-name token as written,
        // not sized to the resolved fqn (which would overshoot the token and overflow the line)
        String c = detailedSources.detail(tag.resolvedReference()).compact2();
        assertEquals(tag.sourceOfReference().compact2(), c);
        assertEquals(c.split(":")[0], c.split(":")[1]);
    }

    @Language("java")
    String bE = """
            package b;
            import a.A;
            public enum E {
                /**
                 * The first one, see {@link a.A}
                 */
                ONE,
                TWO;
            }
            """;

    @DisplayName("an enum constant's javadoc is attached and resolved (ES: CardinalityUpperBound, ScriptType)")
    @Test
    public void testEnumConstant() {
        Map<String, TypeInfo> pr = scan(false, "a.A", aA, "b.E", bE);
        TypeInfo E = pr.get("b.E");
        FieldInfo one = E.getFieldByName("ONE", true);
        assertNotNull(one.javaDoc(), "enum-constant javadoc not attached");
        JavaDoc.Tag tag = one.javaDoc().tags().getFirst();
        assertEquals("a.A", tag.resolvedReference().toString());
        assertNotNull(tag.source().detailedSources());
        assertNotNull(tag.source().detailedSources().detail(tag.resolvedReference()));

        assertNull(E.getFieldByName("TWO", true).javaDoc(), "TWO has no javadoc of its own");
    }

    @Language("java")
    String bG = """
            package b;
            public interface G {
                G INSTANCE = new G() {
                    /**
                     * a field inside an anonymous class body
                     */
                    private final int x = 3;
                };
            }
            """;

    @DisplayName("a field of an ANONYMOUS type is committed: it is outside the end-of-scan walk")
    @Test
    public void testFieldInAnonymousTypeIsCommitted() {
        Map<String, TypeInfo> pr = scan(false, "b.G", bG);
        TypeInfo G = pr.get("b.G");
        ConstructorCall cc = (ConstructorCall) G.getFieldByName("INSTANCE", true).initializer();
        TypeInfo anonymous = cc.anonymousClass();
        assertNotNull(anonymous);
        FieldInfo x = anonymous.getFieldByName("x", true);
        // the deferral of the field commit (so javadoc references can be resolved first) must not strand a field
        // that the primary-type/subtype walk never visits: PrepAnalyzer reads analysisOfInitializer, which throws
        // UnsupportedOperationException on an uncommitted builder
        assertTrue(x.hasBeenInspected(), "field of an anonymous type left uncommitted");
        assertNotNull(x.analysisOfInitializer());
    }

    @Language("java")
    String bF = """
            package b;
            import a.A;
            public class F {
                /**
                 * Member reference {@link a.A#CONSTANT}
                 */
                public int f = 1;
            }
            """;

    @DisplayName("a field's javadoc MEMBER reference resolves to the field it names")
    @Test
    public void testFieldMemberReference() {
        Map<String, TypeInfo> pr = scan(false, "a.A", aA, "b.F", bF);
        TypeInfo F = pr.get("b.F");
        JavaDoc.Tag tag = F.getFieldByName("f", true).javaDoc().tags().getFirst();
        assertEquals("a.A.CONSTANT", tag.resolvedReference().toString());
        DetailedSources detailedSources = tag.source().detailedSources();
        assertNotNull(detailedSources);
        // the TYPE part of the reference carries the token, so a consumer can retarget "a.A" alone
        TypeInfo A = pr.get("a.A");
        assertNotNull(detailedSources.detail(A));
    }
}
