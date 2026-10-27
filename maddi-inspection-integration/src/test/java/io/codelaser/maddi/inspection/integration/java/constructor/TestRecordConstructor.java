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

package io.codelaser.maddi.inspection.integration.java.constructor;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestRecordConstructor extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            public record X(String s1, String s2, String s3) {
            	public static final int NUMBER_OF_COLUMNS = 3;
            	public X(String s1, String s2, String s3) {
            		this.s1 = s1 == null ? "": s1;
            		this.s2 = s2 == null ? "": s2;
            		this.s3 = s3 == null ? "": s3;
            	}
            	public String[] getArrayOfValues() {
            		return new String[] { s1, s2, s3 };
            	}
            }
            
            
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);
        // identical signature, therefore: main constructor
        assertEquals(1, typeInfo.constructors().size());
        MethodInfo constructor = typeInfo.constructors().getFirst();
        ParameterInfo pi0 = constructor.parameters().getFirst();
        assertEquals("3-11:3-19", pi0.source().compact2());
    }

    @Language("java")
    private static final String INPUT2 = """
            public record X(String s1, String s2, String s3) {
            	public X {
            		assert s1 != null;
            	}
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2, JavaInspectorImpl.DETAILED_SOURCES);
        assertEquals(1, typeInfo.constructors().size());
        MethodInfo constructor = typeInfo.constructors().getFirst();
        ParameterInfo pi0 = constructor.parameters().getFirst();
        assertNull(pi0.source());
        assertTrue(pi0.isSynthetic());
        List<Statement> statements = constructor.methodBody().statements();
        assertEquals(4, statements.size());
        assertEquals("assert this.s1!=null;", statements.getFirst().toString());
        assertEquals("this.s1=s1;", statements.get(1).toString());
        assertSame(runtime.methodTypeCompactConstructor(), constructor.methodType());
    }

    @Language("java")
    private static final String INPUT3 = """
            public record X(String s1, String s2, String s3) {
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3, JavaInspectorImpl.DETAILED_SOURCES);
        assertEquals(1, typeInfo.constructors().size());
        MethodInfo constructor = typeInfo.constructors().getFirst();
        ParameterInfo pi0 = constructor.parameters().getFirst();
        assertNull(pi0.source());
        assertTrue(pi0.isSynthetic());
        FieldInfo f0 = typeInfo.fields().getFirst();
        assertEquals("s1", f0.name());
        assertEquals("1-17:1-25", f0.source().compact2());
    }

}
