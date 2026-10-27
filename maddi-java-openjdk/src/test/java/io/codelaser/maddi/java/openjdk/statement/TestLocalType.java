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

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestLocalType extends CommonTest {

    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            class X {
                interface A { void method(String s); }
                A make(String t) {
                    final class C implements A {
                        @Override
                        public void method(String s) {
                            System.out.println(s+t);
                        }
                    }
                    for(int i=0; i<10; ++i) {
                        System.out.println("This tests currentMethod != null");
                    }
                    return new C();
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = scan("a.b.X", INPUT2);
        MethodInfo make = X.findUniqueMethod("make", 1);
        LocalTypeDeclaration ltd = (LocalTypeDeclaration) make.methodBody().statements().getFirst();
        assertEquals("a.b.X.0$make$C", ltd.typeInfo().fullyQualifiedName());

        assertEquals("@5:23-5:32", ltd.typeInfo().source().detailedSources()
                .detail(DetailedSources.IMPLEMENTS).toString());
        MethodInfo method = ltd.typeInfo().findUniqueMethod("method", 1);
        assertEquals("a.b.X.0$make$C.method(String)", method.fullyQualifiedName());
        assertNotNull(method.methodBody());
        assertEquals("System.out.println(s+t)",
                method.methodBody().statements().getFirst().expression().toString());
    }

}
