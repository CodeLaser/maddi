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

package io.codelaser.maddi.inspection.integration.java.stub;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
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
            package io.codelaser.maddi.test;
            
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
