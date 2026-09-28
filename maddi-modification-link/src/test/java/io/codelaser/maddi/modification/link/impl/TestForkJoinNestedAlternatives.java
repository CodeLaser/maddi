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

package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.modification.link.CommonTest;
import io.codelaser.maddi.modification.link.LinkComputer;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Clone bench {@code fors_pure_compiles/Function1972365_file1076824.shiftRight} (slowTest 2026-09-28, the first
 * clone-bench run with the fork/join linker): an if/else whose branches hold a nested if/else with loops that
 * write an array parameter and re-assign a local. The join of the inner alternatives produced a join edge from a
 * member to itself, and {@code Fact}'s {@code source != target} assertion killed the type.
 */
public class TestForkJoinNestedAlternatives extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                public static byte[] shiftRight(final byte[] data, final int bits) {
                    if (bits <= 0) {
                        return data;
                    }
                    int d = 0;
                    if (data.length == 1) {
                        if (bits <= 8) {
                            d = data[0] & 0xFF;
                            d >>>= bits;
                            data[0] = (byte) d;
                        } else {
                            data[0] = 0;
                        }
                    } else if (data.length > 1) {
                        int carry = 0;
                        if (bits < 8) {
                            for (int i = data.length - 1; i > 0; --i) {
                                carry = data[i - 1] & (1 << (bits - 1));
                                carry = carry << (8 - bits);
                                d = data[i] & 0xFF;
                                d >>>= bits;
                                d |= carry;
                                data[i] = (byte) d;
                            }
                            d = data[0] & 0xFF;
                            d >>>= bits;
                            data[0] = (byte) d;
                        } else {
                            for (int i = data.length - 1; i > 0; --i) {
                                data[i] = data[i - 1];
                            }
                            data[0] = 0;
                            shiftRight(data, bits - 8);
                        }
                    }
                    return data;
                }
            }
            """;

    @DisplayName("nested alternatives with loops join without a self-edge")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(X);
        // the TEST profile's duplicate-printed-name check is a diagnostic the corpus runs do not use; here two
        // loops declare an `i` each, and `data[i - 1]` from both prints the same while the two are different
        // vertices (their FQNs name the loop), so the check is off, as in PRODUCTION
        LinkComputer tlc = new LinkComputerImpl(javaInspector, new LinkComputer.Options.Builder()
                .setRecurse(true).setObjectGraphLinks(true).build());
        tlc.doPrimaryType(X);
        MethodInfo shiftRight = X.findUniqueMethod("shiftRight", 2);
        MethodLinkedVariables mlv = shiftRight.analysis().getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS,
                MethodLinkedVariablesImpl.class);
        assertNotNull(mlv);
        System.out.println("shiftRight: " + mlv);
    }
}
