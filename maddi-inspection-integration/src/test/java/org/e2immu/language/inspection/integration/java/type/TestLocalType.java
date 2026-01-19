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

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalTypeDeclaration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestLocalType extends CommonTest {

    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            class X {
                interface A { void method(String s); }
                A make(String t) {
                    final class C implements A {
                        @Override
                        void method(String s) {
                            System.out.println(s+t);
                        }
                    }
                    return new C();
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo make = X.findUniqueMethod("make", 1);
        LocalTypeDeclaration ltd = (LocalTypeDeclaration) make.methodBody().statements().getFirst();
        assertEquals("a.b.X.0$make$C", ltd.typeInfo().fullyQualifiedName());

        assertEquals("@5:23-5:32", ltd.typeInfo().source().detailedSources()
                .detail(DetailedSources.IMPLEMENTS).toString());
        MethodInfo method = ltd.typeInfo().findUniqueMethod("method", 1);
        assertEquals("a.b.X.0$make$C.method(String)", method.fullyQualifiedName());
        assertNotNull(method.methodBody());
        assertEquals("System.out.println(s+t)",
                method.methodBody().statements().getFirst().expression().toString());
    }

}
