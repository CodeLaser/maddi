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
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestOverride3 extends CommonTest {
    @Language("java")
    String A = """
            package a;

            public interface A {
                <T extends C> void m(T t);
            }
            """;

    @Language("java")
    String B = """
            package a;

            public class B implements A {
                public void m(C c) {}
            }
            """;

    @Language("java")
    String C = """
            package a;

            public class C {}
            """;

    @Test
    public void test() {
        MethodInfo bm = scan(false, "a.A", A, "a.B", B, "a.C", C).get("a.B").findUniqueMethod("m", 1);
        assertFalse(bm.overrides().isEmpty());
    }
}
