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

package io.codelaser.maddi.inspection.integration.java.lombok;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
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
        TypeInfo typeInfo = javaInspector.parse(INPUT1,
                new JavaInspector.ParseOptions.Builder().setLombok(true).build());
        FieldInfo fieldInfo = typeInfo.getFieldByName("list", true);
        assertEquals("java.util.List", fieldInfo.type().typeInfo().fullyQualifiedName());
        {
            MethodInfo m = typeInfo.findUniqueMethod("setList", 1);
            assertTrue(m.isSynthetic());
            assertFalse(m.isStatic());
            assertEquals("io.codelaser.maddi.test.X.setList(java.util.List)", m.fullyQualifiedName());
            assertEquals("{this.list=list;}", m.methodBody().toString());
        }
        {
            MethodInfo m = typeInfo.findUniqueMethod("setI", 1);
            assertTrue(m.isSynthetic());
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
        TypeInfo typeInfo = javaInspector.parse(INPUT2,
                new JavaInspector.ParseOptions.Builder().setLombok(true).build());
        FieldInfo fieldInfo = typeInfo.getFieldByName("list", true);
        assertEquals("java.util.List", fieldInfo.type().typeInfo().fullyQualifiedName());
        {
            MethodInfo m = typeInfo.findUniqueMethod("setList", 1);
            assertTrue(m.isSynthetic());
            assertFalse(m.isStatic());
            assertEquals("io.codelaser.maddi.test.X.setList(java.util.List)", m.fullyQualifiedName());
            assertEquals("{this.list=list;}", m.methodBody().toString());
        }
        assertThrows(NoSuchElementException.class, () -> typeInfo.findUniqueMethod("setI", 1));
    }
}
