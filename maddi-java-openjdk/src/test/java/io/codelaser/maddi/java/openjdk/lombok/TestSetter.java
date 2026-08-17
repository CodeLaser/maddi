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

package io.codelaser.maddi.java.openjdk.lombok;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class TestSetter extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package io.codelaser.maddi.test;

            import lombok.Setter;
            import java.util.List;

            public class X {

                @Setter private List<String> list;
                @Setter static int i;
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = scan("io.codelaser.maddi.test.X", INPUT1);
        FieldInfo fieldInfo = typeInfo.getFieldByName("list", true);
        assertEquals("java.util.List", fieldInfo.type().typeInfo().fullyQualifiedName());
        {
            MethodInfo m = typeInfo.findUniqueMethod("setList", 1);
            assertTrue(m.annotations().stream().anyMatch(a -> "Generated".equals(a.typeInfo().simpleName())));

            assertFalse(m.isStatic());
            assertEquals("io.codelaser.maddi.test.X.setList(java.util.List)", m.fullyQualifiedName());
            assertEquals("{this.list=list;}", m.methodBody().toString());
        }
        {
            MethodInfo m = typeInfo.findUniqueMethod("setI", 1);
            assertTrue(m.annotations().stream().anyMatch(a -> "Generated".equals(a.typeInfo().simpleName())));

            assertTrue(m.isStatic());
            assertEquals("io.codelaser.maddi.test.X.setI(int)", m.fullyQualifiedName());
            assertEquals("{X.i=i;}", m.methodBody().toString());
        }

    }

    @Language("java")
    private static final String INPUT2 = """
            package io.codelaser.maddi.test;

            import lombok.Setter;
            import java.util.List;

            @Setter
            public class X {

                private List<String> list;
                static int i;
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = scan("io.codelaser.maddi.test.X", INPUT2);
        FieldInfo fieldInfo = typeInfo.getFieldByName("list", true);
        assertEquals("java.util.List", fieldInfo.type().typeInfo().fullyQualifiedName());
        {
            MethodInfo m = typeInfo.findUniqueMethod("setList", 1);
            assertTrue(m.annotations().stream().anyMatch(a -> "Generated".equals(a.typeInfo().simpleName())));

            assertFalse(m.isStatic());
            assertEquals("io.codelaser.maddi.test.X.setList(java.util.List)", m.fullyQualifiedName());
            assertEquals("{this.list=list;}", m.methodBody().toString());
        }
        assertThrows(NoSuchElementException.class, () -> typeInfo.findUniqueMethod("setI", 1));
    }
}
