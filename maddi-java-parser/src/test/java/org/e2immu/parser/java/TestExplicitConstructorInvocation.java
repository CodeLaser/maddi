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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.IntConstant;
import org.e2immu.language.cst.api.expression.StringConstant;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExplicitConstructorInvocation;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestExplicitConstructorInvocation extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              final int i;
              final String s;
              C() {
                this(1);
              }
              C(int i) {
                 this("a", i);
                 System.out.println(i);
              }
              C(String s, int i) {
                this.s = s;
                this.i = i;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo c0 = typeInfo.findConstructor(0);
        MethodInfo c1 = typeInfo.findConstructor(1);
        MethodInfo c2 = typeInfo.findConstructor(2);

        assertEquals(1, c0.methodBody().size());
        if (c0.methodBody().statements().get(0) instanceof ExplicitConstructorInvocation eci) {
            assertFalse(eci.parameterExpressions().isEmpty());
            assertInstanceOf(IntConstant.class, eci.parameterExpressions().get(0));
            assertFalse(eci.isSuper());
            assertSame(c1, eci.methodInfo());
        } else fail();

        assertEquals(2, c1.methodBody().size());
        if (c1.methodBody().statements().get(0) instanceof ExplicitConstructorInvocation eci) {
            assertEquals(2, eci.parameterExpressions().size());
            assertInstanceOf(StringConstant.class, eci.parameterExpressions().get(0));
            assertFalse(eci.isSuper());
            assertSame(c2, eci.methodInfo());
        } else fail();

        assertEquals(2, c2.methodBody().size());
        assertEquals("1", c2.methodBody().statements().get(1).source().index());
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            public class ExplicitConstructorInvocation_0 {

                public final int n;

                public ExplicitConstructorInvocation_0() {
                    this(3);
                }

                public ExplicitConstructorInvocation_0(int n) {
                    this.n = n;
                }

            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = parse(INPUT2);
    }


}
