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

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestParseResult extends CommonTest2 {

    @Language("java")
    private static final String A_B_X = """
            package a.b;
            import java.util.Set;
            import java.util.List;
            class X {
                interface R {
                    int i();
                }
                record RI(Set<Integer> set, int i, List<String> list) implements R {}
            }
            """;

    @DisplayName("basics of findMostLikelyType")
    @Test
    public void test1() throws IOException {
        ParseResult parseResult = init(Map.of("a.b.X", A_B_X));
        List<TypeInfo> testR = parseResult.findMostLikelyType("r");
        assertEquals(1, testR.size());
        assertEquals("a.b.X.R", testR.getFirst().fullyQualifiedName());
        List<TypeInfo> testXR = parseResult.findMostLikelyType("X.r");
        assertEquals(1, testXR.size());
        assertEquals("a.b.X.R", testXR.getFirst().fullyQualifiedName());
    }

    @Language("java")
    private static final String A_B_X_2 = """
            package a.b;
            import java.util.Set;
            import java.util.List;
            class X {
                interface R {
                    int i();
                }
                record RI(Set<Integer> set, int i, List<String> list) implements R {}
                static class Y {
                    interface R {
                        int j();
                    }
                }
                static class X {
                    interface R {
                        int j();
                    }
                }
            }
            """;

    @DisplayName("findMostLikelyType, partial name")
    @Test
    public void test2() throws IOException {
        ParseResult parseResult = init(Map.of("a.b.X", A_B_X_2));
        assertEquals(3, parseResult.findMostLikelyType("r").size());
        assertEquals(3, parseResult.findMostLikelyType(".r").size());
        assertEquals(3, parseResult.findMostLikelyType("r.").size());

        assertEquals(2, parseResult.findMostLikelyType("X.r").size());
        assertEquals("a.b.X.R,a.b.X.X.R", parseResult.findMostLikelyType("X.r")
                .stream().map(TypeInfo::fullyQualifiedName).sorted().collect(Collectors.joining(",")));

        assertEquals("a.b.X.X.R.j(),a.b.X.Y.R.j()", parseResult.findMostLikelyMethod("j")
                .stream().map(MethodInfo::fullyQualifiedName).sorted().collect(Collectors.joining(",")));
        assertEquals("a.b.X.X.R.j(),a.b.X.Y.R.j()", parseResult.findMostLikelyMethod("r.j")
                .stream().map(MethodInfo::fullyQualifiedName).sorted().collect(Collectors.joining(",")));
        assertEquals("a.b.X.Y.R.j()", parseResult.findMostLikelyMethod("Y.R.j")
                .stream().map(MethodInfo::fullyQualifiedName).sorted().collect(Collectors.joining(",")));
        assertEquals("a.b.X.Y.R.j()", parseResult.findMostLikelyMethod("y.r.j")
                .stream().map(MethodInfo::fullyQualifiedName).sorted().collect(Collectors.joining(",")));
        assertEquals("a.b.X.Y.R.j()", parseResult.findMostLikelyMethod("y.r.J")
                .stream().map(MethodInfo::fullyQualifiedName).sorted().collect(Collectors.joining(",")));
    }


    @Language("java")
    private static final String A_B_X_3 = """
            package a.b;
            abstract class X {
                void method(int i);
                void method(float i);
                interface Y {
                    void method(int i);
                    void method(int i, char c);
                }
                void methodWithObject(Object o);
            }
            """;

    @DisplayName("findMostLikelyMethod")
    @Test
    public void test3() throws IOException {
        ParseResult parseResult = init(Map.of("a.b.X", A_B_X_3));
        assertEquals(1, parseResult.findMostLikelyMethod("X").size());
        assertEquals(0, parseResult.findMostLikelyMethod("Y").size());
        assertEquals(4, parseResult.findMostLikelyMethod("method").size());
        assertEquals(0, parseResult.findMostLikelyMethod("toString").size());
        assertEquals(2, parseResult.findMostLikelyMethod("x.method").size());
        assertEquals(2, parseResult.findMostLikelyMethod("y.Method").size());
        assertEquals(1, parseResult.findMostLikelyMethod("y.method(int, char)").size());
        assertEquals(1, parseResult.findMostLikelyMethod("y.method(Integer, Character)").size());
        assertEquals(0, parseResult.findMostLikelyMethod("y.method(float, Character)").size());
        assertEquals(1, parseResult.findMostLikelyMethod("methodWithObject").size());
        assertEquals(1, parseResult.findMostLikelyMethod("methodWithObject(Object)").size());
        assertEquals(1, parseResult.findMostLikelyMethod("methodWithObject(java.lang.Object)").size());
    }


    @Language("java")
    private static final String A_B_X_4 = """
            package a.b;
            abstract class X {
                int i;
                class Y {
                    int i;
                }
            }
            """;

    @DisplayName("findMostLikelyField")
    @Test
    public void test4() throws IOException {
        ParseResult parseResult = init(Map.of("a.b.X", A_B_X_4));
        assertEquals(2, parseResult.findMostLikelyField("i").size());
        assertEquals(1, parseResult.findMostLikelyField("x.i").size());
        assertEquals(1, parseResult.findMostLikelyField("y.i").size());
    }
}
