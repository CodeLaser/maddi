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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.info.TypeInfo;
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
