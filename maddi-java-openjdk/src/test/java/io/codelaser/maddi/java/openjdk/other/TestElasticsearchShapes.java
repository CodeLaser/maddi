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

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
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
