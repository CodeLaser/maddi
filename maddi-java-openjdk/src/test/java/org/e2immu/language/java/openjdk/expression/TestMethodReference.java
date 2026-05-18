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

package org.e2immu.language.java.openjdk.expression;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.MethodReference;
import org.e2immu.language.cst.api.expression.TypeExpression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMethodReference extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.function.Function;
            class C {
                interface I {
                   String map(C c);
                }
                Function<C, String> mapper(I i) {
                    return i::map;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = scan("a.b.C", INPUT1);
        TypeInfo i = typeInfo.findSubType("I");
        assertSame(typeInfo, i.primaryType());
        MethodInfo map = i.findUniqueMethod("map", 1);
        assertSame(map, i.singleAbstractMethod());

        MethodInfo mapper = typeInfo.findUniqueMethod("mapper", 1);
        if (mapper.methodBody().statements().getFirst() instanceof ReturnStatement rs
            && rs.expression() instanceof MethodReference mr) {
            assertEquals("i::map", mr.toString());
            if (mr.scope() instanceof VariableExpression ve) {
                assertEquals("i", ve.variable().simpleName());
            } else fail();
            assertSame(map, mr.methodInfo());
            assertEquals("Type String", mr.concreteReturnType().toString());
            assertEquals("[Type a.b.C]", mr.concreteParameterTypes().toString());
            assertEquals("Type java.util.function.Function<a.b.C,String>", mr.parameterizedType().toString());
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            
            import java.util.HashMap;
            import java.util.Map;
            import java.util.function.Supplier;
            
            public class C {
            
                private Map<String, Integer> make(Supplier<Map<String, Integer>> supplier) {
                    return supplier.get();
                }
            
                public void method() {
                    Map<String, Integer> map = make(HashMap::new);
                    map.put("a", 1);
                }
            }
            """;


    @DisplayName("constructor method reference")
    @Test
    public void test2() {
        TypeInfo typeInfo = scan("a.b.C", INPUT2);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
        LocalVariableCreation lvc0 = (LocalVariableCreation) methodInfo.methodBody().statements().getFirst();
        MethodCall mc = (MethodCall) lvc0.localVariable().assignmentExpression();
        MethodReference mr = (MethodReference) mc.parameterExpressions().getFirst();
        // IMPORTANT: POLY TYPE Map instead of HashMap
        assertEquals("Type java.util.function.Supplier<java.util.Map<String,Integer>>",
                mr.parameterizedType().toString());
        assertEquals("Type java.util.Map<String,Integer>", mr.concreteReturnType().toString());
        if (mr.scope() instanceof TypeExpression te) {
            assertEquals("14-41:14-47", te.source().compact2());
        } else fail();
    }
}
