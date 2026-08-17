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

package org.e2immu.language.inspection.integration.java.stub;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestStub2 extends CommonTest {

    public TestStub2() {
        super(true);
    }

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;
            
            public class X {
                private Y y;
                public X(Y y) {
                    this.y = y;
                }
            }
            """;


    @Disabled("at the moment, stubs from the type context are disabled")
    @Test
    public void test() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1, JavaInspectorImpl.FAIL_FAST);
        FieldInfo y = typeInfo.getFieldByName("y", true);
        TypeInfo Y = y.type().bestTypeInfo();
        assertEquals("Y", Y.fullyQualifiedName());
        assertTrue(Y.typeNature().isStub());
        MethodInfo constructor = typeInfo.findConstructor(1);
        ParameterInfo constructor0 = constructor.parameters().getFirst();
        assertEquals(Y, constructor0.parameterizedType().typeInfo());
        assertSame(Y, constructor0.parameterizedType().typeInfo());
    }
}
