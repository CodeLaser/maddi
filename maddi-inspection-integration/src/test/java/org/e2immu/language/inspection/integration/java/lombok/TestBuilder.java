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

package org.e2immu.language.inspection.integration.java.lombok;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestBuilder extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;
            
            import lombok.Data;
            import lombok.Builder;
            
            @Data
            @Builder
            public class X {
                private final String s;
                private final int t;
                private char c;
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1,
                new JavaInspectorImpl.ParseOptionsBuilder().setLombok(true).build());
        TypeInfo builder = typeInfo.findSubType("Builder");
        assertTrue(builder.access().isPublic());
        assertTrue(builder.typeNature().isClass());
        assertTrue(builder.isStatic());

        // fields
        assertEquals(3, builder.fields().size());
        assertTrue(builder.getFieldByName("s", true).access().isPrivate());

        // setters
        MethodInfo setS = builder.findUniqueMethod("setS", 1);
        assertEquals("org.e2immu.test.X.Builder.setS(String)", setS.fullyQualifiedName());
        assertEquals("{this.s=s;return this;}", setS.methodBody().toString());

        // build method
        MethodInfo build = builder.findUniqueMethod("build", 0);
        assertEquals(typeInfo.asParameterizedType(), build.returnType());
    }

}
