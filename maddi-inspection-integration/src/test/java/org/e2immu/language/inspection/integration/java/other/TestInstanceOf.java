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

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.InstanceOf;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestInstanceOf extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class C {
                private String s;
                C(Object o) {
                    if(o instanceof String str) {
                        this.s = str;
                    }
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);
        assertEquals("C", typeInfo.simpleName());
        MethodInfo C = typeInfo.findConstructor(1);
        Expression expression = C.methodBody().statements().getFirst().expression();
        if (expression instanceof InstanceOf io) {
            assertEquals("5-12:5-34", io.source().compact2());
            DetailedSources ds = io.source().detailedSources();
            assertEquals("@5:25-5:30", ds.detail(io.testType()).toString());
            assertEquals("@5:32-5:34", ds.detail(io.patternVariable().localVariable()).toString());
        } else fail();
    }

}
