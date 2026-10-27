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

package io.codelaser.maddi.inspection.integration.java.constructor;

import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
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
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo test = typeInfo.findUniqueMethod("test", 0);
        if (test.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof ConstructorCall cc) {
            assertNotNull(cc.anonymousClass());
            assertEquals("Type java.util.HashMap<String,String>", cc.anonymousClass().parentClass().toString());
            assertEquals("a.b.C.$0", cc.anonymousClass().fullyQualifiedName());
            MethodInfo constructor = cc.anonymousClass().findConstructor(0);
            assertEquals("a.b.C.$0.<init>()", constructor.fullyQualifiedName());
        } else fail();

        MethodInfo test2 = typeInfo.findUniqueMethod("test2", 0);
        if (test2.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof ConstructorCall cc) {
            assertNotNull(cc.anonymousClass());
            assertEquals("Type java.util.HashMap<String,String>", cc.anonymousClass().parentClass().toString());
        } else fail();
    }

}
