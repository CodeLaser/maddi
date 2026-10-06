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

package io.codelaser.maddi.cst.print.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kotlin has no {@code ;}, so statements must end up on lines of their own, also in a block the formatter would
 * otherwise put on one line. It did not: {@code { map.put(key, size()) add(element)lstKeys.add(key) }} (fernflower's
 * VBStyleCollection), because the formatter honoured a sub-block's trailing newline only at one split level.
 */
public class TestStatementSeparation extends CommonJavaToKotlin {

    @Language("java")
    private static final String SHORT_BLOCK = """
            package a;
            class C {
                void m(java.util.List<String> list, String s) { list.add(s); list.add(s); list.clear(); }
            }
            class V<E, K> extends java.util.ArrayList<E> {
                private java.util.HashMap<K, Integer> map = new java.util.HashMap<>();
                private java.util.ArrayList<K> lstKeys = new java.util.ArrayList<>();
                public void addWithKey(E element, K key) {
                    map.put(key, size());
                    super.add(element);
                    lstKeys.add(key);
                }
            }
            """;

    @Test
    public void shortBlock() {
        String kotlin = kotlin(SHORT_BLOCK);
        assertTrue(kotlin.contains("""
                        map.put(key, size())
                """), kotlin);
        assertTrue(kotlin.contains("""
                        lstKeys.add(key)
                    }
                """), kotlin);
    }
}
