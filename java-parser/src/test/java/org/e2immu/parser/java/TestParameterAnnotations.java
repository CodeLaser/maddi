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
