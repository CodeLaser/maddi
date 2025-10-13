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

package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestJavaDoc2 extends CommonTest2 {

    @Language("java")
    String aA = """
            package a;
            public class A {
                // empty
            }
            """;

    @Language("java")
    String bB = """
            package b;
            import a.A;
            /**
             * See {@link a.A}
             */
            public class B  {
                // empty
            }
            """;


    // do not touch the spacing here!
    @Language("java")
    String bB2 = """
            package b;
            import a.A;
                /**
                 * See {@link  a.A}
                 */
            public class B2  {
                // empty
            }
            """;

    @Test
    public void test() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.A", aA, "b.B", bB, "b.B2", bB2);
        ParseResult pr1 = init(sourcesByFqn);
        {
            TypeInfo B = pr1.findType("b.B");
            JavaDoc.Tag tag = B.javaDoc().tags().getFirst();
            assertEquals("a.A", tag.resolvedReference().toString());
            DetailedSources detailedSources = tag.source().detailedSources();
            assertNotNull(detailedSources);
            assertEquals("4-15:4-17", detailedSources.detail(tag.resolvedReference()).compact2());
            assertNull(detailedSources.associatedObject(tag.resolvedReference()));
            assertEquals("4-15:4-15", detailedSources.detail(((TypeInfo) tag.resolvedReference()).packageName()).compact2());
        }
        {
            TypeInfo B2 = pr1.findType("b.B2");
            JavaDoc.Tag tag = B2.javaDoc().tags().getFirst();
            assertEquals("a.A", tag.resolvedReference().toString());
            DetailedSources detailedSources = tag.source().detailedSources();
            assertNotNull(detailedSources);
            assertEquals("4-20:4-22", detailedSources.detail(tag.resolvedReference()).compact2());
            assertNull(detailedSources.associatedObject(tag.resolvedReference()));
            assertEquals("4-20:4-20", detailedSources.detail(((TypeInfo) tag.resolvedReference()).packageName()).compact2());
        }
    }

    @Language("java")
    String aA3 = """
            package a;
            import b.B;
            public class A {
                  /**
                   * @param <T>  the type parameter
                   */
                  private static <T> void m(B<T> b) {}
            }
            """;

    @Language("java")
    String aA4 = """
            package a;
            import b.B;
            /**
             * link to {@link #m}
             */
            public class A4 {
                private static <T> void m(B<T> b) {}
            }
            """;

    @Language("java")
    String bB3 = """
            package b;
            public class B<T>  {
                // empty
            }
            """;

    @Test
    public void test3() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.A", aA3, "a.A4", aA4, "b.B", bB3);
        ParseResult pr1 = init(sourcesByFqn);
        {
            TypeInfo A = pr1.findType("a.A");
            assertEquals("""
                    [TypeReference[typeInfo=void, explicit=true], TypeReference[typeInfo=b.B, explicit=true]]\
                    """, A.typesReferenced().toList().toString());

            MethodInfo m = A.findUniqueMethod("m", 1);
            JavaDoc.Tag tag = m.javaDoc().tags().getFirst();
            assertEquals("T=TP#0 in A.m", tag.resolvedReference().toString());
        }
        {
            TypeInfo A4 = pr1.findType("a.A4");
            assertEquals("""
                    [TypeReference[typeInfo=void, explicit=true], TypeReference[typeInfo=b.B, explicit=true]]\
                    """, A4.typesReferenced().toList().toString());
            JavaDoc.Tag tag = A4.javaDoc().tags().getFirst();
            assertTrue(A4.javaDoc().typesReferenced().toList().isEmpty());
            assertEquals("a.A4.m(b.B<T>)", tag.resolvedReference().toString());
        }
    }
}