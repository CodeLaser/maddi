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

package io.codelaser.maddi.inspection.integration.java.other;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
            assertEquals("[void[E], b.B[E], a.A[I]]", A.typesReferenced(null).toList().toString());
            // the 3rd is from the tag in the JavaDoc

            MethodInfo m = A.findUniqueMethod("m", 1);
            JavaDoc.Tag tag = m.javaDoc().tags().getFirst();
            assertEquals("T=TP#0 in A.m", tag.resolvedReference().toString());
        }
        {
            TypeInfo A4 = pr1.findType("a.A4");
            assertEquals("[void[E], b.B[E], a.A4[I]]", A4.typesReferenced(null).toList().toString());
            JavaDoc.Tag tag = A4.javaDoc().tags().getFirst();
            assertEquals("[a.A4[I]]", A4.javaDoc().typesReferenced(null).toList().toString());
            assertEquals("a.A4.m(b.B)", tag.resolvedReference().toString());
        }
    }
}