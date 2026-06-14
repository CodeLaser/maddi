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

package org.e2immu.language.java.openjdk.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestIsAssignableFrom extends CommonTest {

    @Language("java")
    public static final String INPUT = """
            package a.b;
            class X {
                interface Parent<T extends Parent<?>> {
                }
                interface Child extends Parent<Child> {
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = scan("a.b.X", INPUT);
        TypeInfo parent = X.findSubType("Parent");
        TypeInfo child = X.findSubType("Child");
        assertTrue(parent.asParameterizedType().isAssignableFrom(runtime, child.asParameterizedType()));
    }

    @Disabled("fingerprints not implemented yet")
    @Test
    public void test2() {
        TypeInfo X = scan("a.b.X", INPUT);
        assertEquals("rRYs3LDF1ia1MgjUQEW0Aw==", X.compilationUnit().fingerPrintOrNull().toString());
    }

}
