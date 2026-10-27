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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseConstructorCall extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class C {
              int k;
              public static void main(String[] args) {
                C c = new C();
                System.out.println(c);
              }
              public C() {
                k = 1;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo constructor = typeInfo.findConstructor(0);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().getFirst() instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            assertSame(constructor, cc.constructor());
            assertSame(typeInfo, cc.parameterizedType().typeInfo());
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class C<K> {
              K k;
              public static void main(String[] args) {
                C<String> c = new C<>();
                C<Integer> d = new C<Integer>();
                C<K> e = new C();
                System.out.println(c+"="+d);
              }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = parse(INPUT2);
        MethodInfo constructor = typeInfo.findConstructor(0);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            ParameterizedType pt = lvc.localVariable().parameterizedType();
            assertSame(typeInfo, pt.typeInfo());
            assertEquals(1, pt.parameters().size());
            assertSame(runtime.stringTypeInfo(), pt.parameters().getFirst().typeInfo());

            assertSame(constructor, cc.constructor());
            assertSame(typeInfo, cc.parameterizedType().typeInfo());
            assertTrue(cc.diamond().isYes());

            assertEquals("new C<>()", cc.toString());
            assertEquals("C<String> c=new C<>();", lvc.toString());
        } else fail();
        if (main.methodBody().statements().get(1) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            ParameterizedType pt = lvc.localVariable().parameterizedType();

            assertSame(constructor, cc.constructor());
            ParameterizedType ccPt = cc.parameterizedType();
            assertSame(typeInfo, ccPt.typeInfo());
            assertEquals(1, ccPt.parameters().size());
            assertEquals(pt, ccPt);
            assertTrue(cc.diamond().isShowAll());
            assertEquals("new C<Integer>()", cc.toString());
            assertEquals("C<Integer> d=new C<Integer>();", lvc.toString());
        } else fail();

        if (main.methodBody().statements().get(2) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {

            assertSame(constructor, cc.constructor());
            ParameterizedType ccPt = cc.parameterizedType();
            assertSame(typeInfo, ccPt.typeInfo());
            assertEquals(0, ccPt.parameters().size());
            assertTrue(cc.diamond().isNo());
            assertEquals("new C()", cc.toString());
            assertEquals("C<K> e=new C();", lvc.toString());
        } else fail();
    }


    @Language("java")
    private static final String INPUT3 = """
            package k;
            
            public class K {
                private String name;
            
                public K() {}
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = parse(INPUT3, true);
        MethodInfo constructor = typeInfo.findConstructor(0);
        assertEquals("6-5:6-17", constructor.source().compact2());
    }

}
