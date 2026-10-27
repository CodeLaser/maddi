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

package io.codelaser.maddi.inspection.integration.java.other;

import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.IfElseStatement;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestOperators extends CommonTest {
    /*
   want to ensure that == is an object equals, not an int equals.
    */
    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.HashMap;
            import java.util.Map;
            
            public class Basics_9 {
                private final Map<String, Integer> map = new HashMap<>();
            
                public int method1(String k) {
                    Integer v = map.get(k);
                    int r;
                    if (v == null) {
                        int newValue = k.length();
                        map.put(k, newValue);
                        r = newValue;
                    } else {
                        r = v;
                    }
                    return r;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo method1 = typeInfo.findUniqueMethod("method1", 1);
        if (method1.methodBody().statements().get(2) instanceof IfElseStatement ifElse
            && ifElse.expression() instanceof BinaryOperator bo) {
            assertSame(javaInspector.runtime().equalsOperatorObject(), bo.operator());
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.Collection;
            import java.util.HashMap;
            import java.util.Map;
            
            public class X {
            
                public int method1(Map<String, Integer> map) {
                   assert map instanceof Map;
                   assert map instanceof HashMap<String,Integer>;
                   assert map instanceof Collection<?>; // interface -> still possible
                   assert map instanceof Object;
                   assert map instanceof Exception; // class -> not possible
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
        MethodInfo method1 = typeInfo.findUniqueMethod("method1", 1);
        assertTrue(eval(method1, 0).isBoolValueTrue());
        assertEquals("map instanceof HashMap<String,Integer>", eval(method1, 1).toString());
        assertEquals("map instanceof Collection<?>", eval(method1, 2).toString());
        assertTrue(eval(method1, 3).isBoolValueTrue());
        assertTrue(eval(method1, 4).isBoolValueFalse());
    }

    private Expression eval(MethodInfo method1, int i) {
        return javaInspector.runtime().sortAndSimplify(true, method1.methodBody().statements().get(i).expression());
    }

    interface M {
    }

    interface C {
    }

    class HM implements M {
    }

    class HMS extends HM implements C {
    }

    M m = new HMS();

    @Test
    public void testJava() {
        assertFalse(M.class.isAssignableFrom(C.class));
        assertFalse(C.class.isAssignableFrom(M.class));
        assertTrue(m instanceof C);
    }
}
