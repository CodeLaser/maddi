/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.java.openjdk.CommonTest;
import org.e2immu.language.java.openjdk.ScanCompilationUnits;
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
