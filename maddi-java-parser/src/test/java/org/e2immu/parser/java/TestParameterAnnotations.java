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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestParameterAnnotations extends CommonTestParse {


    @Language("java")
    private static final String INPUT = """
            package jfocus.test;

            public class MoveDown_0<K, V> {

                private TestMap<K, V> map = null;

                interface TestMap<K, V> {
                    @SuppressWarnings("g")
                    V get(K k);

                    @SuppressWarnings("h")
                    void put(K k, V v);
                }

                interface Remap<V> {
                    @SuppressWarnings("i")
                    V apply(@SuppressWarnings("v") V v1, @SuppressWarnings("w")  V v2);
                }

                void same1(K key, V value, Remap<V> remap) {
                    V oldValue = map.get(key);
                    if (oldValue == null) {
                        map.put(key, value);
                    } else {
                        V newValue = remap.apply(oldValue, value);
                        map.put(key, newValue);
                    }
                }

                void same2(K key, V value, Remap<V> remap) {
                    V oldValue = map.get(key);
                    V newValue;
                    if (oldValue == null) {
                        newValue = value;
                    } else {
                        newValue = remap.apply(oldValue, value);
                    }
                    map.put(key, newValue);
                }
            }
            """;

    @Test
    public void test() {
        parse(INPUT);
    }
}
