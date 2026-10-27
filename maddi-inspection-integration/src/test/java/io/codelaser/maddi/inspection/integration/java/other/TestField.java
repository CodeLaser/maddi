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

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestField extends CommonTest {


    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class C {
                public final String s;
                C(String s) {
                    this.s = s;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        assertEquals("C", typeInfo.simpleName());
        MethodInfo C = typeInfo.findConstructor(1);
        ExpressionAsStatement c0 = (ExpressionAsStatement) C.methodBody().statements().get(0);
        Assignment a = (Assignment) c0.expression();
        VariableExpression ve = (VariableExpression) ((FieldReference) a.variableTarget()).scope();
        assertEquals("5-9:5-12", ve.source().compact2());
        DetailedSources ds = ve.source().detailedSources();
        assertNull(ds);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            class C {
              List<String> stringList;
              List<Integer> intList1 = List.of(), intList2, intList3 = null;
              int i, iArray[], j;
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
        assertEquals("C", typeInfo.simpleName());

        FieldInfo stringList = typeInfo.fields().get(0);
        assertEquals("Type java.util.List<String>", stringList.type().toString());
        FieldInfo intList1 = typeInfo.fields().get(1);
        assertEquals("Type java.util.List<Integer>", intList1.type().toString());
        assertEquals("List.of()", intList1.initializer().toString());
        FieldInfo intList2 = typeInfo.fields().get(2);
        assertEquals("Type java.util.List<Integer>", intList2.type().toString());
        assertTrue(intList2.initializer().isEmpty());
        FieldInfo intList3 = typeInfo.fields().get(3);
        assertEquals("Type java.util.List<Integer>", intList2.type().toString());
        assertEquals("null", intList3.initializer().toString());

        FieldInfo iArray = typeInfo.getFieldByName("iArray", true);
        assertEquals("Type int[]", iArray.type().toString());
        FieldInfo j = typeInfo.getFieldByName("j", true);
        assertEquals("Type int", j.type().toString());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class B {
                // max with comment
                final static int MAX = 3;
                public boolean m(int j) {
                  return B.MAX < j;
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3, JavaInspectorImpl.DETAILED_SOURCES);
        assertEquals("B", typeInfo.simpleName());
        FieldInfo max = typeInfo.getFieldByName("MAX", true);
        assertEquals("4-22:4-28", max.source().compact2()); // MAX = 3
        Source declarationWithout = max.source().detailedSources().detail(DetailedSources.FIELD_DECLARATION);
        assertEquals("3-5:4-29", declarationWithout.compact2());

        Statement s0 = typeInfo.findUniqueMethod("m", 1).methodBody().statements().getFirst();
        if (s0.expression() instanceof BinaryOperator bo) {
            assertEquals("B.MAX", bo.lhs().toString());
            if (bo.lhs() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
                assertEquals("B", fr.scope().toString());
                assertEquals("6-14:6-14", fr.scope().source().compact2());
            } else fail();
        } else fail("Have " + s0.expression());

    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            class B {
                final int min = 5; // min with comment
                final static int MAX = 3; // max with comment
                public boolean m(int j) {
                  return B.MAX < j;
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = javaInspector.parse(INPUT4, JavaInspectorImpl.DETAILED_SOURCES);
        assertEquals("B", typeInfo.simpleName());

        FieldInfo min = typeInfo.getFieldByName("min", true);
        assertEquals(1, min.comments().size());
        assertEquals(" min with comment", min.comments().getFirst().comment());
        FieldInfo max = typeInfo.getFieldByName("MAX", true);
        assertEquals(1, max.comments().size());
        assertEquals(" max with comment", max.comments().getFirst().comment());
        MethodInfo m = typeInfo.findUniqueMethod("m", 1);
        assertTrue(m.comments().isEmpty());
    }
}
