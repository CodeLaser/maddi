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

package io.codelaser.maddi.inspection.integration.java.type;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSealed extends CommonTest {
    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            class X {
                static sealed class P permits A, B, a.b.X.C {
            
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
        TypeInfo X = javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);

        TypeInfo P = X.findSubType("P", true);
        assertTrue(P.isSealed());
        assertFalse(P.isFinal());
        assertFalse(P.isNonSealed());
        assertEquals("@3:27-3:33", P.source().detailedSources().detail(DetailedSources.PERMITS).toString());
        assertEquals(3, P.permittedWhenSealed().size());
        assertEquals("[a.b.X.A[E], a.b.X.B[E], a.b.X.C[E FQN]]",
                P.typesReferenced(null).toList().toString());

        TypeInfo A = X.findSubType("A", true);
        assertEquals("@6:26-6:32", A.source().detailedSources().detail(DetailedSources.EXTENDS).toString());
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
