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

import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExplicitConstructorInvocation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestExtendedConstructor extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            
            import java.util.Map;
            import java.util.HashMap;
            
            class C {
                public C() {
                    System.out.println("!");
                }
            
                private Map<String, String> test() {
                    return new HashMap<String, String>() {
                        {
                            put("x", "abc");
                        }
                    };
                }
            
                private Map<String, String> test2() {
                    return new HashMap<>() {
                        {
                            put("y", "12345");
                        }
                    };
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = scan("a.b.C", INPUT1);
        MethodInfo constructorC = typeInfo.findConstructor(0);
        if (constructorC.methodBody().statements().getFirst() instanceof ExplicitConstructorInvocation eci) {
            assertTrue(eci.isSuper());
            assertTrue(eci.isSynthetic());
        } else fail();
        assertEquals(2, constructorC.methodBody().statements().size());

        MethodInfo test = typeInfo.findUniqueMethod("test", 0);
        if (test.methodBody().statements().getFirst() instanceof ReturnStatement rs
            && rs.expression() instanceof ConstructorCall cc) {
            assertNotNull(cc.anonymousClass());
            assertEquals("Type java.util.HashMap<String,String>", cc.anonymousClass().parentClass().toString());
            assertEquals("a.b.C.$0", cc.anonymousClass().fullyQualifiedName());
            MethodInfo constructor = cc.anonymousClass().findConstructor(0);
            assertEquals("a.b.C.$0.<init>()", constructor.fullyQualifiedName());
            assertEquals("put(\"x\",\"abc\");", constructor.methodBody().statements().getFirst().toString());
        } else fail();

        MethodInfo test2 = typeInfo.findUniqueMethod("test2", 0);
        if (test2.methodBody().statements().getFirst() instanceof ReturnStatement rs
            && rs.expression() instanceof ConstructorCall cc) {
            assertNotNull(cc.anonymousClass());
            assertEquals("Type java.util.HashMap<String,String>", cc.anonymousClass().parentClass().toString());
            assertEquals("a.b.C.$1", cc.anonymousClass().fullyQualifiedName());
            MethodInfo constructor = cc.anonymousClass().findConstructor(0);
            assertEquals("a.b.C.$1.<init>()", constructor.fullyQualifiedName());
            assertEquals("put(\"y\",\"12345\");", constructor.methodBody().statements().getFirst().toString());
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Collections;
            import java.util.HashMap;
            import java.util.Map;
            
            interface C {
                public static final String A = "A";
            	public static final String B = "B";
            	public static final int i = 26;
            	public static final int j = 32;
            
            	public static final Map<String, Integer> m = Collections.unmodifiableMap(new HashMap<>() {
            
            		private static final long serialVersionUID = 1L;
            		{
                        int k = 3;
            			put(A, i);
            			put(B, j+k);
            		}
            	});
            }""";

    @Test
    public void test2() {
        TypeInfo typeInfo = scan("a.b.C", INPUT2);
        FieldInfo m = typeInfo.getFieldByName("m", true);
        if (m.initializer() instanceof MethodCall mc) {
            ConstructorCall cc = (ConstructorCall) mc.parameterExpressions().getFirst();
            TypeInfo anon = cc.anonymousClass();
            assertEquals("a.b.C.$0", anon.fullyQualifiedName());
            assertEquals(1, anon.constructors().size());
            assertEquals(1, anon.fields().size());
            assertEquals(3, anon.constructors().getFirst().methodBody().statements().size());
        } else fail();
    }

}
