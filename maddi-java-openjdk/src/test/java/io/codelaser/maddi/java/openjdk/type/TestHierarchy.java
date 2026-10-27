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

package io.codelaser.maddi.java.openjdk.type;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestHierarchy extends CommonTest {
    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            class C {
                interface A { }
                static class B { }
                static class D extends B implements A { }
                static class E extends D { }
                interface F extends A { }
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = scan("a.b.C", INPUT1);
        TypeInfo A = C.findSubType("A");
        assertEquals("[]", A.typeHierarchyExcludingJLOStream().toList().toString());
        TypeInfo B = C.findSubType("B");
        assertEquals("[]", B.typeHierarchyExcludingJLOStream().toList().toString());
        TypeInfo D = C.findSubType("D");
        assertEquals("[a.b.C.B, a.b.C.A]", D.typeHierarchyExcludingJLOStream().toList().toString());
        assertTrue(B.inHierarchyOf(D));
        assertFalse(D.inHierarchyOf(B));
        DetailedSources dsD = D.source().detailedSources();
        assertEquals("5-20:5-26", dsD.detail(DetailedSources.EXTENDS).compact2());
        assertEquals("5-30:5-39", dsD.detail(DetailedSources.IMPLEMENTS).compact2());

        TypeInfo E = C.findSubType("E");
        assertEquals("[a.b.C.D, a.b.C.B, a.b.C.A]", E.typeHierarchyExcludingJLOStream().toList().toString());
        assertTrue(B.inHierarchyOf(E));
        assertFalse(E.inHierarchyOf(B));

        TypeInfo F = C.findSubType("F");
        DetailedSources dsF = F.source().detailedSources();
        assertEquals("7-17:7-23", dsF.detail(DetailedSources.EXTENDS).compact2());

        TypeInfo jlo = runtime.objectTypeInfo();
        assertNull(jlo.parentClass());
    }
}
