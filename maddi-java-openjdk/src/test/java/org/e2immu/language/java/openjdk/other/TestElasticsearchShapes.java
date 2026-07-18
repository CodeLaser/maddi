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

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Source shapes surfaced by the elasticsearch first-contact scan (2026-07-18); each test pins one
 * previously-crashing construct.
 */
public class TestElasticsearchShapes extends CommonTest {

    @DisplayName("constructor reference on a primitive array: byte[]::new (zstd)")
    @Test
    public void primitiveArrayConstructorReference() {
        // the qualifier scan of 'byte[]' produces no expression; the scanner NPE'd on evaluatedScope
        TypeInfo X = scan("a.b.X", """
                package a.b;
                import java.util.function.IntFunction;
                class X {
                    IntFunction<byte[]> f = byte[]::new;
                    IntFunction<String[]> g = String[]::new; // control: object array, pre-existing path
                }
                """);
        assertNotNull(X);
    }

    @DisplayName("member record of an anonymous class, forward-referenced (BinaryFieldMapperTests.$13)")
    @Test
    public void memberRecordOfAnonymousClassForwardReference() {
        // 'Duplicating type ...$13.BytesCompareUnsigned': the forward reference (Cmp::new) registers the
        // record before its declaration is visited; the declaration must reuse it, not create a duplicate
        TypeInfo X = scan("a.b.X", """
                package a.b;
                import java.util.stream.Stream;
                class X {
                    interface Support { Object example(); }
                    Support make() {
                        return new Support() {
                            @Override
                            public Object example() {
                                return Stream.of(new byte[]{1}).map(Cmp::new).sorted().toList();
                            }
                            private record Cmp(byte[] bytes) implements Comparable<Cmp> {
                                @Override
                                public int compareTo(Cmp o) { return 0; }
                            }
                        };
                    }
                }
                """);
        assertNotNull(X);
    }
}
