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

public class TestSealed extends CommonTest {
    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            class X {
                static sealed class P permits A, B, C {
            
                }
                static final class A extends P {
            
                }
                static non-sealed class B extends P {
            
                }
                static sealed class C extends P permits D, E {
            
                }
                static final class D extends C {}
                static final class E extends C {}
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);

        TypeInfo P = X.findSubType("P", true);
        assertTrue(P.isSealed());
        assertFalse(P.isFinal());
        assertFalse(P.isNonSealed());
        assertEquals(3, P.permittedWhenSealed().size());
        assertEquals("""
                [TypeReference[typeInfo=a.b.X.A, explicit=true], \
                TypeReference[typeInfo=a.b.X.B, explicit=true], \
                TypeReference[typeInfo=a.b.X.C, explicit=true]]\
                """, P.typesReferenced().toList().toString());

        TypeInfo A = X.findSubType("A", true);
        assertFalse(A.isSealed());
        assertTrue(A.isFinal());
        assertFalse(A.typeModifiers().contains(javaInspector.runtime().typeModifierNonSealed()));
        assertFalse(A.isNonSealed());
        assertSame(A, P.permittedWhenSealed().getFirst());
        assertSame(P, A.parentClass().typeInfo());

        TypeInfo B = X.findSubType("B", true);
        assertFalse(B.isSealed());
        assertFalse(B.isFinal());
        assertTrue(B.typeModifiers().contains(javaInspector.runtime().typeModifierNonSealed()));
        assertTrue(B.isNonSealed());
        assertSame(B, P.permittedWhenSealed().get(1));
    }

}
