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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
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

    // A refers to same-package sibling D by its SIMPLE name; the reference resolves via the package prefix.
    @Language("java")
    String aA5 = """
            package a;
            /**
             * See {@link D}
             */
            public class A5 {
                // empty
            }
            """;

    @Language("java")
    String aD5 = """
            package a;
            public class D {
                // empty
            }
            """;

    @Test
    public void test4() {
        Map<String, TypeInfo> pr1 = scan(false, "a.A5", aA5, "a.D", aD5);
        TypeInfo A5 = pr1.get("a.A5");
        JavaDoc.Tag tag = A5.javaDoc().tags().getFirst();
        assertEquals("a.D", tag.resolvedReference().toString());
        DetailedSources detailedSources = tag.source().detailedSources();
        assertNotNull(detailedSources);
        // regression: the detailed source of the resolved type must be exactly the simple-name token "D" as
        // written, NOT sized to the resolved fqn ("a.D"), which would overshoot the token and overflow the line
        // (the bug that made MoveType generate an edit spanning into the class declaration below).
        assertEquals(tag.sourceOfReference().compact2(), detailedSources.detail(tag.resolvedReference()).compact2());
        // single-column token: begin line/col equals end line/col
        String c = detailedSources.detail(tag.resolvedReference()).compact2();
        assertEquals(c.split(":")[0], c.split(":")[1]);
    }
}
