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

package org.e2immu.language.java.openjdk.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression (originally surfaced by Jenkins' {@code hudson.model.Descriptor}, on {@code commonType(String,
 * net.sf.json.JSONObject)}): folding the common type of an array initializer whose elements' common supertype is
 * generic but reached with a <em>raw</em> implementation on one side. Here {@code String} implements
 * {@code Comparable<String>} while the local {@code Raw} implements the raw {@code Comparable}; their common
 * supertype is {@code Comparable}.
 * <p>
 * {@code CommonType.commonType} unified the common supertype's type arguments position by position, indexing the
 * raw side's (empty) argument list, and threw {@code ArrayIndexOutOfBoundsException: Index 0 out of bounds for
 * length 0}, dropping the whole compilation unit. The fix falls back to the raw common supertype when the two
 * argument arities differ.
 */
public class TestCommonTypeRawArgument extends CommonTest {

    private static final String INPUT = """
            package a.b;
            public class X {
                @SuppressWarnings({"unchecked", "rawtypes"})
                static class Raw implements Comparable {
                    public int compareTo(Object o) {
                        return 0;
                    }
                }
                Object[] m() {
                    return new Object[]{ "s", new Raw() };
                }
            }
            """;

    @Test
    public void test() {
        // before the fix, this scan threw ArrayIndexOutOfBoundsException while folding the array's common type
        // (commonType(String, Raw): common super Comparable, but Raw implements it raw)
        TypeInfo x = assertDoesNotThrow(() -> scan("a.b.X", INPUT));
        assertNotNull(x);
        assertNotNull(x.findUniqueMethod("m", 0));
    }
}
