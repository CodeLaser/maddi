/*
 * e2immu: a static code analyzer for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You
 * should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.java.openjdk.expression;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A method reference whose scope is an ARRAY TYPE rather than a class: {@code long[]::clone}.
 * <p>
 * Found on trino 2026-08-13. The scanner reports {@code UnresolvedSymbolException: Type Array not
 * found} (ClassSymbolScanner:258) and the WHOLE compilation unit is dropped, so every type in the
 * file disappears from the parse. That is not merely a measurement gap: a dropped file is invisible
 * to the refactoring levers too, so their edits silently skip its call sites and the result does not
 * compile.
 * <p>
 * Real instance, {@code lib/trino-parquet/src/test/java/io/trino/parquet/ParquetTestUtils.java:237},
 * inside {@code createRowBlock}:
 * <pre>
 *     long[] valueIsValid = rowIsValid.map(long[]::clone).orElseGet(() -&gt; allValid(positionCount));
 * </pre>
 */
public class TestArrayMethodReference extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Optional;
            class C {
                long[] method(Optional<long[]> in, int n) {
                    return in.map(long[]::clone).orElseGet(() -> new long[n]);
                }
            }
            """;

    @DisplayName("method reference on a primitive array type: long[]::clone")
    @Test
    public void test1() {
        TypeInfo C = scan("a.b.C", INPUT1);
        assertNotNull(C);
        MethodInfo method = C.findUniqueMethod("method", 2);
        assertNotNull(method.methodBody());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Optional;
            class D {
                String[] method(Optional<String[]> in) {
                    return in.map(String[]::clone).orElse(null);
                }
            }
            """;

    @DisplayName("method reference on a reference array type: String[]::clone")
    @Test
    public void test2() {
        TypeInfo D = scan("a.b.D", INPUT2);
        assertNotNull(D);
        MethodInfo method = D.findUniqueMethod("method", 1);
        assertNotNull(method.methodBody());
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.function.IntFunction;
            class E {
                IntFunction<long[]> maker() {
                    return long[]::new;
                }
            }
            """;

    @DisplayName("array constructor reference: long[]::new -- the control, expected to pass")
    @Test
    public void test3() {
        TypeInfo E = scan("a.b.E", INPUT3);
        assertNotNull(E);
        MethodInfo method = E.findUniqueMethod("maker", 0);
        assertNotNull(method.methodBody());
    }
}
