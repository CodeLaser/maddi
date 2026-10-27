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

package io.codelaser.maddi.java.openjdk.method;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestOverride4 extends CommonTest {
    @Language("java")
    String A = """
            package a;

            public interface A {
                void m();
            }
            """;

    @Language("java")
    String B = """
            package a;

            public interface B extends A {
                @Override
                void m();
            }
            """;

    @Test
    public void test() {
        Map<String, TypeInfo> types = scan(false, "a.A", A, "a.B", B);
        TypeInfo A = types.get("a.A");
        MethodInfo am = A.findUniqueMethod("m", 0);
        TypeInfo B = types.get("a.B");
        MethodInfo bm = B.findUniqueMethod("m", 0);
        assertTrue(bm.overrides().contains(am));
    }
}
