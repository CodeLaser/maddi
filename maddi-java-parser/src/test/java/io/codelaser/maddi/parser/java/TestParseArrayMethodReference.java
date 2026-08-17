/*
 * e2immu: a static code analyser for effective and eventual immutability
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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The array-method-reference construct run through maddi's OWN parser, to establish whether the
 * defect is shared by both front ends or confined to the openjdk one.
 * <p>
 * The functional interfaces are declared locally on purpose: this harness has no real class path,
 * only the hand-written {@code predefined} list in {@link CommonTestParse}, so {@code java.util.*}
 * is not resolvable here and importing it would fail for a reason unrelated to the defect.
 * <p>
 * Companion test: {@code TestArrayMethodReference} in maddi-java-openjdk, where {@code long[]::clone}
 * throws {@code UnresolvedSymbolException: Type Array not found} (ClassSymbolScanner:258) because
 * javac models {@code long[]} with a synthetic ClassSymbol named {@code Array}, owned by nothing.
 */
public class TestParseArrayMethodReference extends CommonTestParse {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class C {
                interface Cloner {
                    long[] apply(long[] in);
                }
                Cloner cloner() {
                    return long[]::clone;
                }
            }
            """;

    @DisplayName("method reference on a primitive array type: long[]::clone")
    @Test
    public void test1() {
        TypeInfo C = parse(INPUT1);
        assertNotNull(C);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class D {
                interface Cloner {
                    String[] apply(String[] in);
                }
                Cloner cloner() {
                    return String[]::clone;
                }
            }
            """;

    @DisplayName("method reference on a reference array type: String[]::clone")
    @Test
    public void test2() {
        TypeInfo D = parse(INPUT2);
        assertNotNull(D);
    }

    // NOTE: the array CONSTRUCTOR reference (long[]::new) is not testable in this harness. The parser
    // synthesises java.util.function.IntFunction for it, and that type is not in CommonTestParse's
    // predefined list, so the test would fail for a reason unrelated to the defect. Its control lives
    // in the openjdk companion test, which has a real class path -- and passes there.
}
