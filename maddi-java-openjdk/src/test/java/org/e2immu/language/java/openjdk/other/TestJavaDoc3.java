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

import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestJavaDoc3 extends CommonTest {

    @Language("java")
    String MAIN_A = """
            package a;
            public class A {
            
            }
            """;

    @Language("java")
    String TEST_A = """
            package a;
            /**
             * link to {@Link A}
             */
            public class TestA {
            }
            """;

    @Disabled("Cannot easily scan 2x in openjdk package")
    @DisplayName("Test across source sets, same package")
    @Test
    public void test4() {
        Map<String, TypeInfo> pr1 = scan(false, "a.A", MAIN_A, "a.TestA", TEST_A);
        {
            TypeInfo A = pr1.get("a.A");
            assertEquals("source", A.compilationUnit().sourceSet().name());
            assertFalse(A.compilationUnit().sourceSet().test());
        }
        {
            TypeInfo TestA = pr1.get("a.TestA");
            assertTrue(TestA.compilationUnit().sourceSet().test());
            assertEquals("source", TestA.compilationUnit().sourceSet().name());
            JavaDoc.Tag tag = TestA.javaDoc().tags().getFirst();
            assertEquals("a.A", tag.resolvedReference().toString());
        }
    }


    @Language("java")
    String A = """
            package a;
            import b.B;
            public class A {
                /**
                 * @param test {@link B#OK}
                 */
                public void m(Object test) {}
            }
            """;

    @Language("java")
    String A2 = """
            package a;
            import b.B;
            public class A {
                /**
                 * @param test {@link B}
                 */
                public void m(Object test) {}
            }
            """;

    @Language("java")
    String B = """
            package b;
            public enum B {
                OK
            }
            """;

    @DisplayName("Type reference of tag")
    @Test
    public void test1() {
        for (String a : new String[]{A, A2}) {
            Map<String, TypeInfo> pr1 = scan(false, "a.A", a, "b.B", B);

            TypeInfo A = pr1.get("a.A");
            assertEquals("a.A[I], b.B[E], java.lang.Object[E], void[E]",
                    A.typesReferenced(null).map(Object::toString).sorted()
                            .collect(Collectors.joining(", ")), "fails for " + a);
        }
    }
}
