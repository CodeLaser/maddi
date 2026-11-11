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

    private static final String FORMAT_TEST = """
            package a.b;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.*;
            public class FormatTest {
                private boolean verbose = true;
                @Test
                void constant() {
                    assertEquals("a 1 {50%}", String.format("%s %d {50%%}", "a", 1));
                }
                @Test
                void runtime() {
                    String f = verbose ? "x %s y %d" : "%s";
                    assertEquals("x b y 2", String.format(f, "b", 2));
                }
            }
            """;

    private static final String CONTINUE_TEST = """
            package a.b;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.*;
            public class ContinueTest {
                @Test
                void forWithContinue() {
                    int sum = 0;
                    for (int i = 10; i > 0; i -= 2) {
                        if (i == 6) continue;
                        if (i == 2) break;
                        sum += i;
                    }
                    assertEquals(22, sum);
                }
                @Test
                void doWithContinue() {
                    int i = 0;
                    int odd = 0;
                    do {
                        i++;
                        if (i % 2 == 0) continue;
                        odd++;
                    } while (i < 7);
                    assertEquals(4, odd);
                }
                @Test
                void labelledContinue() {
                    int n = 0;
                    outer:
                    for (int i = 0; i < 4; i = i + 1) {
                        for (int j = 0; j < 4; j = j + 1) {
                            if (j > i) continue outer;
                            n++;
                        }
                    }
                    assertEquals(10, n);
                }
            }
            """;

    /**
     * A continue in a for loop with updaters, or in a do loop, goes on with the updaters or the condition: printed
     * as the while loop it was, it skipped them (fernflower's extendCases looped forever).
     */
    @Test
    public void continueRunsTheUpdaters() throws Exception {
        List<TypeInfo> types = parse(CONTINUE_TEST);
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        assertEquals(List.of(), rustcErrors(), rust);
        String run = rustTests();
        assertTrue(run.contains("test result: ok. 3 passed"), run + "\n" + rust);
    }

    private static final String BASE_TEST = """
            package a.b;
            import org.junit.jupiter.api.BeforeEach;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.*;
            abstract class Fixture<T> {
                protected int ready;
                @BeforeEach
                public void setUp() { ready = 1; }
            }
            public class UsesFixture extends Fixture<String> {
                @Test
                void ready() {
                    assertEquals(1, base().ready);
                }
                Fixture<String> base() { return this; }
            }
            """;

    /**
     * A generic superclass is no closed hierarchy: the test class holds it as its {@code base} field, and the
     * runner reaches the superclass's {@code @BeforeEach} through it (E0599 otherwise, which stubbed fernflower's
     * 176 SingleClassesTestBase runners).
     */
    @Test
    public void inheritedSetUpThroughBase() throws Exception {
        List<TypeInfo> types = parse(BASE_TEST);
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "    t.base.set_up();\n    t.ready();\n");
    }

    @Test
    public void stringFormat() throws Exception {
        List<TypeInfo> types = parse(FORMAT_TEST);
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "format!(\"{} {} {{50%}}\", \"a\", 1)");
        contains(rust, "crate::jrt::java_format(");
        assertEquals(List.of(), rustcErrors(), rust);
        String run = rustTests();
        assertTrue(run.contains("test result: ok. 2 passed"), run);
    }

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
