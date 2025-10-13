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

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTypesReferenced extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import java.lang.annotation.Annotation;
            class X {
                interface Y<A extends Annotation> {
                   A supply();
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        assertEquals("""
                [TypeReference[typeInfo=java.lang.annotation.Annotation, explicit=true], \
                TypeReference[typeInfo=java.lang.annotation.Annotation, explicit=true]]\
                """, typeInfo.typesReferenced().toList().toString());
        // FIXME PRINT space in front of {A
        String expect = """
                package a.b;
                import java.lang.annotation.Annotation;
                class X { interface Y<A extends Annotation> {A supply(); } }
                """;
        assertEquals(expect, javaInspector.print2(typeInfo));
    }
}
