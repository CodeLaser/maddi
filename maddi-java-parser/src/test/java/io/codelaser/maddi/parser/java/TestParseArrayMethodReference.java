/*
 * e2immu: a static code analyzer for effective and eventual immutability
 * Copyright 2020-2026, CodeLaser BV, https://www.e2immu.org
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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.info.TypeInfo;
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
