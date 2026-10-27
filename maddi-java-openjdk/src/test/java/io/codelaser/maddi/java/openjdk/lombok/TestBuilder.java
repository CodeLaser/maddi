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

package io.codelaser.maddi.java.openjdk.lombok;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestBuilder extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package io.codelaser.maddi.test;
            
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
        TypeInfo typeInfo = scan("io.codelaser.maddi.test.X", INPUT1);
        TypeInfo builder = typeInfo.findSubType("XBuilder");
        assertTrue(builder.access().isPublic());
        assertTrue(builder.typeNature().isClass());
        assertTrue(builder.isStatic());

        // fields
        assertEquals(3, builder.fields().size());
        assertTrue(builder.getFieldByName("s", true).access().isPrivate());

        // setters (called "s", rather than setS!!)
        MethodInfo setS = builder.findUniqueMethod("s", 1);
        assertEquals("io.codelaser.maddi.test.X.XBuilder.s(String)", setS.fullyQualifiedName());
        assertEquals("{this.s=s;return this;}", setS.methodBody().toString());
        assertEquals(2, setS.annotations().size());
        assertEquals("@SuppressWarnings(\"all\")", setS.annotations().getFirst().toString());
        assertEquals("@Generated", setS.annotations().getLast().toString());

        // build method
        MethodInfo build = builder.findUniqueMethod("build", 0);
        assertEquals(typeInfo.asParameterizedType(), build.returnType());
    }

}
