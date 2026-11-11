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

package io.codelaser.maddi.cst.print.rust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.info.TypeInfo;

/** JUnit test classes translated: {@code #[test]} runners, and the assertions as {@code assert!}. */
public class TestJavaToRustJUnit extends CommonJavaToRust {

    private static final String COUNTER_TEST = """
            package a.b;
            import org.junit.jupiter.api.AfterEach;
            import org.junit.jupiter.api.BeforeEach;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.*;
            abstract class Base {
                protected int count;
                @BeforeEach
                void baseSetUp() { count = 10; }
            }
            public class CounterTest extends Base {
                private String name;
                @BeforeEach
                void setUp() { count = count + 1; name = "x"; }
                @AfterEach
                void tearDown() { count = 0; }
                @Test
                void increments() {
                    count++;
                    assertEquals(12, count);
                    assertTrue(count > 1, "more than one");
                    assertFalse(count > 100);
                }
                @Test
                void named() {
                    assertEquals("x", name);
                    assertNotNull(name);
                    assertNotEquals("y", name);
                }
                @Test
                void fails() {
                    assertEquals(3, count, "count is 11");
                }
            }
            """;

    @Test
    public void runners() throws Exception {
        List<TypeInfo> types = parse(COUNTER_TEST);
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "#[test]\nfn counter_test__increments() {");
        // the superclass's @BeforeEach first, then the class's; @AfterEach after the test
        contains(rust, "t.base_set_up();\n    t.set_up();\n    t.increments();\n    t.tear_down();");
        contains(rust, "assert!(");
        assertEquals(List.of(), rustcErrors(), rust);
        String run = rustTests();
        // two pass, and the one that asserts something false fails, with its message
        assertTrue(run.contains("test result: FAILED. 2 passed; 1 failed"), run);
        assertTrue(run.contains("count is 11"), run);
    }
}
