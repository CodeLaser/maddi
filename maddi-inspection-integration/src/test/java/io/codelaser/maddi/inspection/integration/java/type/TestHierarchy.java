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

package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
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
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);
        TypeInfo A = C.findSubType("A");
        assertEquals("[]", A.typeHierarchyExcludingJLOStream().toList().toString());
        TypeInfo B = C.findSubType("B");
        assertEquals("[]", B.typeHierarchyExcludingJLOStream().toList().toString());
        TypeInfo D = C.findSubType("D");
        assertEquals("[a.b.C.B, a.b.C.A]", D.typeHierarchyExcludingJLOStream().toList().toString());
        assertTrue(B.inHierarchyOf(D));
        assertFalse(D.inHierarchyOf(B));
        TypeInfo E = C.findSubType("E");
        assertEquals("[a.b.C.D, a.b.C.B, a.b.C.A]", E.typeHierarchyExcludingJLOStream().toList().toString());
        assertTrue(B.inHierarchyOf(E));
        assertFalse(E.inHierarchyOf(B));
    }
}
