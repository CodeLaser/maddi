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

package io.codelaser.maddi.java.openjdk.statement;

import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.SynchronizedStatement;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSynchronized extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public synchronized void method1() {
                 method2();
              }
              private final String s = "?";
              public void method2() {
                synchronized (s) {
                  System.out.println(s);
                }
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);
        MethodInfo m1 = typeInfo.findUniqueMethod("method1", 0);
        assertTrue(m1.isSynchronized());
        MethodInfo m2 = typeInfo.findUniqueMethod("method2", 0);
        assertFalse(m2.isSynchronized());
        FieldInfo fieldInfo = typeInfo.getFieldByName("s", true);
        if (m2.methodBody().statements().getFirst() instanceof SynchronizedStatement s) {
            if (s.expression() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
                assertSame(fieldInfo, fr.fieldInfo());
            } else fail();
            assertEquals(1, s.block().size());
        } else fail();
    }
}
