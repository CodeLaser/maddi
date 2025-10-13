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

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseMethodTypeParameter extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Function;
            class C {
              interface I {}
              <T> T method(String s) { return null; }
              <T, S extends I> T method2(S s) { return null; }
              private <T> C(T t, int i) { }
              <T> C(T t) { }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
        assertEquals("Type param T", methodInfo.returnType().toString());
        assertSame(methodInfo, methodInfo.returnType().typeParameter().getOwner().getRight());

        MethodInfo methodInfo2 = typeInfo.findUniqueMethod("method2", 1);
        assertEquals("Type param T", methodInfo.returnType().toString());
        assertSame(methodInfo, methodInfo.returnType().typeParameter().getOwner().getRight());
        assertEquals(2, methodInfo2.typeParameters().size());
        TypeParameter tp1 = methodInfo2.typeParameters().get(1);
        assertEquals("S=TP#1 in C.method2", tp1.toString());
        assertSame(methodInfo2, tp1.getOwner().getRight());
        assertTrue(tp1.isMethodTypeParameter());
    }
}
