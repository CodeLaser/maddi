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

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestAnonymousInTypeArgument extends CommonTest {

    // Optional.map(i -> new Base(){...}) infers the map's result type as Optional<anonymous Base subclass>,
    // so the anonymous class surfaces in a type-argument position. Because the lambda's target type is
    // converted before the anonymous body is scanned/registered, the anonymous class is still unknown and
    // has a blank simple name: it must be represented by its base type (Base) rather than looked up by name.
    // Mirrors com.sun.tools.javac.code.Types$DescriptorCache.mergeDescriptors, which crashed the openjdk scan.
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Optional;
            class C {
                abstract static class Base {
                    abstract int getType();
                }
                Base method(Optional<Integer> opt) {
                    return opt.map(i -> new Base() {
                        @Override
                        public int getType() {
                            return i;
                        }
                    }).orElse(null);
                }
            }
            """;

    @DisplayName("anonymous class inferred into a type argument (forward reference)")
    @Test
    public void test1() {
        TypeInfo typeInfo = scan("a.b.C", INPUT1);
        MethodInfo method = typeInfo.findUniqueMethod("method", 1);
        assertNotNull(method.methodBody());
    }
}
