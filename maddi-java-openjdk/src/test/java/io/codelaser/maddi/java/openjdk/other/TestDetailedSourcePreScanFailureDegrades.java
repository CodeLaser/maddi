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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.java.openjdk.CommonTest;
import io.codelaser.maddi.java.openjdk.ScanCompilationUnits;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression (originally surfaced by Apache Camel's {@code TransformerRouteTest}): the openjdk front-end runs an
 * auxiliary congocc "detailed-source" pre-scan alongside javac. On a construct javac accepts but that congocc's
 * grammar chokes on -- here {@code super(new Runnable(){...})} in a constructor -- the pre-scan throws
 * {@code org.parsers.java.ParseException}.
 * <p>
 * In accumulate (non-fail-fast) mode this used to drop the whole compilation unit as a hard error, aborting the
 * source set. Since the scan result only supplies source positions for detailed sources (all reads are
 * null-guarded), the unit is now kept with degraded detailed sources instead of being dropped.
 */
public class TestDetailedSourcePreScanFailureDegrades extends CommonTest {

    private static final String INPUT = """
            package a.b;
            import java.io.BufferedReader;
            import java.io.StringReader;
            public class X {
                interface Task {
                    void go() throws Exception;
                }
                static class Base {
                    Base(Task t) {
                    }
                }
                static class Sub extends Base {
                    Sub() {
                        super(new Task() {
                            @Override
                            public void go() throws Exception {
                                StringBuilder input = new StringBuilder();
                                try (BufferedReader reader = new BufferedReader(new StringReader("x"))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        input.append(line);
                                    }
                                }
                            }
                        });
                    }
                }
            }
            """;

    @Test
    public void test() {
        // accumulate mode (ignoreErrors = true): the congocc pre-scan fails on 'super(new Runnable(){...})', but
        // the unit must survive with degraded detailed sources rather than being dropped.
        ScanCompilationUnits.Result result = scan(true, Map.of("a.b.X", INPUT));
        assertEquals(1, result.primaryTypes().size(), "the compilation unit must be kept, not dropped");
        assertEquals("a.b.X", result.primaryTypes().getFirst().fullyQualifiedName());
        // the nested Sub (whose 'super(new Runnable(){...})' constructor is what congocc chokes on) survives
        assertTrue(result.primaryTypes().getFirst().subTypes().stream()
                .anyMatch(t -> "Sub".equals(t.simpleName())), "nested Sub must be present");
    }
}
