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

package io.codelaser.maddi.java.openjdk.expression;

import io.codelaser.maddi.cst.api.expression.Cast;
import io.codelaser.maddi.cst.api.expression.InstanceOf;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.IfElseStatement;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestInstanceOf extends CommonTest {
    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Function;
            class C {
              interface I {
                 String map(C c);
              }
              String method(Object o, C c) {
                if(o instanceof I) {
                    I i = (I)o;
                    return i.map(c);
                }
                return null;
              }
              String method2(Object o, C c) {
                if(o instanceof I i) {
                    return i.map(c);
                }
                return null;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        if (method.methodBody().statements().getFirst() instanceof IfElseStatement ifElse) {
            if (ifElse.expression() instanceof InstanceOf io) {
                assertNull(io.patternVariable());
                assertEquals("Type a.b.C.I", io.testType().toString());
                assertEquals("Type boolean", io.parameterizedType().toString());
            } else fail();
            if (ifElse.block().statements().getFirst() instanceof LocalVariableCreation lvc) {
                if (lvc.localVariable().assignmentExpression() instanceof Cast cast) {
                    assertEquals("o", cast.expression().toString());
                    assertEquals("Type a.b.C.I", cast.parameterizedType().toString());
                }
            } else fail();
        } else fail();

        MethodInfo method2 = typeInfo.findUniqueMethod("method2", 2);
        if (method2.methodBody().statements().getFirst() instanceof IfElseStatement ifElse) {
            if (ifElse.expression() instanceof InstanceOf io) {
                assertEquals("i", io.patternVariable().localVariable().simpleName());
            } else fail();
            if (ifElse.block().statements().getFirst() instanceof ReturnStatement rs) {
                if (rs.expression() instanceof MethodCall mc) {
                    if (mc.object() instanceof VariableExpression ve && ve.variable() instanceof LocalVariable lv) {
                        assertEquals("i", lv.simpleName());
                        assertEquals("Type a.b.C.I", lv.parameterizedType().toString());
                    } else fail();
                } else fail();
            } else fail();
        } else fail();
    }
}
