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

package io.codelaser.maddi.inspection.integration.java.method;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestOverride3 extends CommonTest2 {
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
    public void test() throws IOException {
        ParseResult parseResult = init(Map.of("a.A", A, "a.B", B, "a.C", C));
        MethodInfo bm = parseResult.findMethod("a.B.m(a.C)");
        assertFalse(bm.overrides().isEmpty());
    }
}
