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

import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestJavaDoc3 extends CommonTest2 {

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

    @DisplayName("Test across source sets, same package")
    @Test
    public void test4() throws IOException {
        ParseResult pr1 = init(Map.of("a.A", MAIN_A), Map.of("a.TestA", TEST_A));
        {
            TypeInfo A = pr1.findType("a.A");
            assertEquals("test-protocol:a.A", A.compilationUnit().sourceSet().name());
            assertFalse(A.compilationUnit().sourceSet().test());
        }
        {
            TypeInfo TestA = pr1.findType("a.TestA");
            assertTrue(TestA.compilationUnit().sourceSet().test());
            assertEquals("test-protocol:a.TestA", TestA.compilationUnit().sourceSet().name());
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
    public void test1() throws IOException {
        for (String a : new String[]{A, A2}) {
            ParseResult pr1 = init(Map.of("a.A", a), Map.of("b.B", B));

            TypeInfo A = pr1.findType("a.A");
            assertEquals("""
                            TypeReference[typeInfo=a.A, explicit=false], \
                            TypeReference[typeInfo=b.B, explicit=true], \
                            TypeReference[typeInfo=java.lang.Object, explicit=true], \
                            TypeReference[typeInfo=void, explicit=true]\
                            """,
                    A.typesReferenced().map(Object::toString).sorted().collect(Collectors.joining(", ")));
        }
    }
}