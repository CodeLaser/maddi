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

import io.codelaser.maddi.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

public class TestMethodCall10 extends CommonTest2 {

    @Language("java")
    private static final String ABX = """
            package a.b;
            import static a.c.Y.when;
            class X {
                interface Mocked {
                    void deleteObject(String key, String id);
                }
                static <T> T any() { return null; }
                interface Stubber {
                    <T> T when(T t);
                }
                static Stubber doNothing() { return null; }
            
                void method(Mocked mocked){
                    doNothing().when(mocked).deleteObject(any(), any());
                }
            }
            """;

    @Language("java")
    private static final String ACY = """
            package a.c;
            public class Y {
                public static <T> String when(T t) { return "" + t; }
            }
            """;

    @DisplayName("instance and static method, in instance setup")
    @Test
    public void test1() throws IOException {
        init(Map.of("a.b.X", ABX, "a.c.Y", ACY));
    }
}
