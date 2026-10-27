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

package io.codelaser.maddi.java.openjdk.type;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.TypeExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestInner2 extends CommonTest {
    @Language("java")
    private static final String aA = """
            package a;
            class A {
                public static class Inner {
                    public static void m() {}
                }
            }
            """;
    @Language("java")
    private static final String aB = """
            package a;
            class B {
                void m() {
                    A.Inner.m();
                }
            }
            """;

    @Test
    public void test() {
        Map<String, TypeInfo> result = scan(false, "a.A", aA, "a.B", aB);
        TypeInfo b = result.get("a.B");
        MethodInfo m = b.findUniqueMethod("m", 0);
        MethodCall mc = (MethodCall) m.methodBody().statements().getFirst().expression();
        if (mc.object() instanceof TypeExpression te) {
            assertEquals("4-9:4-15", te.source().compact2());
            assertEquals("4-9:4-15", te.source().detailedSources()
                    .detail(te.parameterizedType().typeInfo()).compact2());
            TypeInfo a = te.parameterizedType().typeInfo().compilationUnitOrEnclosingType().getRight();
            assertEquals("4-9:4-9", te.source().detailedSources().detail(a).compact2());
        }
    }
}
