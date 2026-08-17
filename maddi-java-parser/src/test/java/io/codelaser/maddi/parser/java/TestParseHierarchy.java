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

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseHierarchy extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              interface I { }
              interface J extends I { }
              class A implements I { }
              class B extends A implements I, J { }
              class K<T> { }
              class D extends K<Integer> { }
              class E<S> extends K<S> { }
              class S extends E<String> { }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        TypeInfo I = typeInfo.findSubType("I");
        assertTrue(I.isInterface());
        TypeInfo J = typeInfo.findSubType("J");
        assertTrue(J.isInterface());
        assertSame(I, J.interfacesImplemented().get(0).bestTypeInfo());
        TypeInfo A = typeInfo.findSubType("A");
        assertFalse(A.isInterface());
        assertSame(I, A.interfacesImplemented().get(0).bestTypeInfo());
        TypeInfo B = typeInfo.findSubType("B");
        assertSame(A, B.parentClass().typeInfo());
        assertEquals(2, B.interfacesImplemented().size());
        assertSame(J, B.interfacesImplemented().get(1).typeInfo());

        TypeInfo K = typeInfo.findSubType("K");
        TypeInfo D = typeInfo.findSubType("D");
        assertTrue(D.isDescendantOf(K));
        assertFalse(K.isDescendantOf(D));
        assertEquals("Type K<Integer>", D.parentClass().toString());
        assertSame(K, D.parentClass().typeInfo());
        TypeInfo E = typeInfo.findSubType("E");
        assertEquals(1, E.typeParameters().size());
        assertSame(E.typeParameters().get(0), E.parentClass().parameters().get(0).typeParameter());
        TypeInfo S = typeInfo.findSubType("S");
        assertTrue(S.isDescendantOf(K));
        assertFalse(E.isDescendantOf(S));
    }
}
