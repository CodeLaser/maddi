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

package io.codelaser.maddi.java.openjdk.type;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
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
