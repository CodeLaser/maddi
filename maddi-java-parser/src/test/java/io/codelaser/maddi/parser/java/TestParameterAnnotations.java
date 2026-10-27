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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.info.TypeInfo;
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
