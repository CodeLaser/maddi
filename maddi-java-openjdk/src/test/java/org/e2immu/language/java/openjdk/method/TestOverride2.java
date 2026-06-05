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

package org.e2immu.language.java.openjdk.method;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestOverride2 extends CommonTest {
    @Language("java")
    private static final String aA = """
            package a;
            import c.C;
            class A extends C {
            }
            """;
    @Language("java")
    private static final String bB = """
            package b;
            import a.A;
            class B {
                void m() {
                    A.m();
                }
            }
            """;
    @Language("java")
    private static final String cC = """
            package c;
            class C {
                public static void m();
            }
            """;

    @Test
    public void test() {
        Map<String, TypeInfo> types = scan(false, "a.A", aA, "b.B", bB, "c.C", cC);
        TypeInfo B = types.get("b.B");
        MethodInfo methodInfo = B.findUniqueMethod("m", 0);
        MethodCall mc = (MethodCall) methodInfo.methodBody().lastStatement().expression();
        assertEquals("c.C.m()", mc.methodInfo().fullyQualifiedName());
        assertEquals("A", mc.object().toString());
    }
}
