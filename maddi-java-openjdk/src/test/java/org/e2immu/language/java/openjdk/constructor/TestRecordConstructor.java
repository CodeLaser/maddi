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

package org.e2immu.language.java.openjdk.constructor;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.java.openjdk.CommonTest;
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
        TypeInfo typeInfo = scan("X", INPUT1);
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
        TypeInfo typeInfo = scan("X", INPUT2);
        assertEquals(1, typeInfo.constructors().size());
        MethodInfo constructor = typeInfo.constructors().getFirst();
        ParameterInfo pi0 = constructor.parameters().getFirst();
        assertEquals("1-17:1-25", pi0.source().compact2());

        assertFalse(pi0.isSynthetic());
        List<Statement> statements = constructor.methodBody().statements();
        assertEquals(5, statements.size());
        assertEquals("super();", statements.getFirst().toString());
        assertEquals("assert s1!=null;", statements.get(1).toString());
        assertInstanceOf(ParameterInfo.class, statements.get(1).expression()
                .variableStreamDescend().findFirst().orElseThrow());
        // FIXME differs from std parser (std parser is wrong!)
        assertEquals("this.s1=s1;", statements.get(2).toString());
        assertSame(runtime.methodTypeCompactConstructor(), constructor.methodType());
    }

    @Language("java")
    private static final String INPUT3 = """
            public record X(String s1, String s2, String s3) {
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = scan("X", INPUT3);
        assertEquals(1, typeInfo.constructors().size());
        MethodInfo constructor = typeInfo.constructors().getFirst();
        ParameterInfo pi0 = constructor.parameters().getFirst();
        assertEquals("-@1:17-1:21", pi0.source().toString());
        assertFalse(pi0.isSynthetic()); // FIXME differs from std parser
        FieldInfo f0 = typeInfo.fields().getFirst();
        assertEquals("s1", f0.name());
        assertNull(f0.source()); // FIXME differs from std parser
    }

}
