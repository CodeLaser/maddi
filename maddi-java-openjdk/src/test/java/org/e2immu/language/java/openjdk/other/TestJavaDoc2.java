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
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestJavaDoc2 extends CommonTest {

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
    public void test() {
        Map<String, TypeInfo> pr1 = scan(false, "a.A", aA, "b.B", bB, "b.B2", bB2);
        {
            TypeInfo B = pr1.get("b.B");
            JavaDoc.Tag tag = B.javaDoc().tags().getFirst();
            assertEquals("a.A", tag.resolvedReference().toString());
            assertEquals("4-15:4-17", tag.sourceOfReference().compact2());
            DetailedSources detailedSources = tag.source().detailedSources();
            assertNotNull(detailedSources);
            assertEquals("4-15:4-17", detailedSources.detail(tag.resolvedReference()).compact2());
            assertNull(detailedSources.associatedObject(tag.resolvedReference()));
            assertEquals("4-15:4-15", detailedSources.detail(((TypeInfo) tag.resolvedReference()).packageName()).compact2());
        }
        {
            TypeInfo B2 = pr1.get("b.B2");
            JavaDoc.Tag tag = B2.javaDoc().tags().getFirst();
            assertEquals("a.A", tag.resolvedReference().toString());
            assertEquals("4-20:4-22", tag.sourceOfReference().compact2());

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
    public void test3() {
        Map<String, TypeInfo> pr1 = scan(false, "a.A", aA3, "a.A4", aA4, "b.B", bB3);
        {
            TypeInfo A = pr1.get("a.A");
            assertEquals("[void[E], b.B[E], a.A[I]]", A.typesReferenced(null).toList().toString());
            // the 3rd is from the tag in the JavaDoc

            MethodInfo m = A.findUniqueMethod("m", 1);
            JavaDoc.Tag tag = m.javaDoc().tags().getFirst();
            assertEquals("T=TP#0 in A.m", tag.resolvedReference().toString());
        }
        {
            TypeInfo B = pr1.get("b.B");
            assertEquals("[b.B[I]]", B.typesReferenced(null).toList().toString());
        }
        {
            TypeInfo A4 = pr1.get("a.A4");
            // the 3rd one should be an implicit one from the #m reference
            assertEquals("[void[E], b.B[E], a.A4[I]]", A4.typesReferenced(null).toList().toString());
            JavaDoc.Tag tag = A4.javaDoc().tags().getFirst();
            assertEquals("a.A4.m(b.B)", tag.resolvedReference().toString());
            assertEquals("[a.A4[I]]", A4.javaDoc().typesReferenced(null).toList().toString());
        }
    }
}
