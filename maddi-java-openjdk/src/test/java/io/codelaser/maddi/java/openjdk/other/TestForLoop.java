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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ForEachStatement;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestForLoop extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            
            class X {
               interface I { String get(); }
               static class Y {
                   final I data[] = new I[3];
               }
               void method(Y y) {
                   for(var i: y.data) {
                       System.out.println(i.get());
                   }
               }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = scan("a.b.X", INPUT1);
        TypeInfo y = typeInfo.findSubType("Y");
        FieldInfo data = y.getFieldByName("data", true);
        assertEquals("Type a.b.X.I[]", data.type().toString());
        assertEquals("new I[3]", data.initializer().toString());
        MethodInfo method = typeInfo.findUniqueMethod("method", 1);
        ForEachStatement fes = (ForEachStatement) method.methodBody().statements().getLast();
        assertEquals("var i;", fes.initializer().toString());
        assertTrue(fes.initializer().isVar());
        assertEquals("Type a.b.X.I", fes.initializer().localVariable().parameterizedType().toString());
    }
}
