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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** The printer's messages: what the Kotlin does differently from the Java, loses, or could not translate. */
public class TestJavaToKotlinMessages extends CommonJavaToKotlin {

    @Language("java")
    private static final String SOURCE = """
            package a;
            import java.util.*;
            public class M {
                static class Box<T> { }
                static final Box[] EMPTY = new Box[0];
                protected int count;
                String name;
                abstract static class Placeholder {
                    @Override public Placeholder clone() { return null; }
                }
                <T extends Number & Comparable<T>> T max(T a, T b) { return a.compareTo(b) >= 0 ? a : b; }
                int m(Map<String, String> m, Map<String, Integer> counts, String k) {
                    Box<Integer>[] typed = EMPTY;
                    name = m.get(k);
                    return m.get(k).length() + Math.min(counts.get(k), 3) + typed.length;
                }
            }
            """;

    @Test
    public void messages() {
        List<KotlinPrintMessage> messages = messages(SOURCE);
        String all = messages.stream().map(KotlinPrintMessage::toString).collect(Collectors.joining("\n"));
        Map<KotlinPrintMessage.Code, List<KotlinPrintMessage>> byCode = messages.stream()
                .collect(Collectors.groupingBy(KotlinPrintMessage::code));

        KotlinPrintMessage clone = first(byCode, KotlinPrintMessage.Code.CLONE_NULL_THROWS, all);
        assertEquals(KotlinPrintMessage.Severity.BEHAVIOUR_CHANGE, clone.severity(), all);
        assertEquals("a.M.Placeholder", clone.type(), all);
        assertEquals(9, clone.line(), all);

        KotlinPrintMessage intoField = first(byCode, KotlinPrintMessage.Code.ASSERT_INTO_NON_NULL, all);
        assertEquals(14, intoField.line(), all);
        assertTrue(intoField.detail().contains("m.get(k)"), all);

        assertEquals(15, first(byCode, KotlinPrintMessage.Code.ASSERT_AT_DEREFERENCE, all).line(), all);
        assertEquals(15, first(byCode, KotlinPrintMessage.Code.ASSERT_AT_UNBOXING, all).line(), all);
        assertEquals(13, first(byCode, KotlinPrintMessage.Code.UNCHECKED_CAST, all).line(), all);
        assertEquals(6, first(byCode, KotlinPrintMessage.Code.PROTECTED_AS_PUBLIC, all).line(), all);
        KotlinPrintMessage bounds = first(byCode, KotlinPrintMessage.Code.BOUNDS_DROPPED, all);
        assertTrue(bounds.detail().contains("T extends Number & Comparable<T>"), all);
        assertEquals(KotlinPrintMessage.Severity.LOSS, bounds.severity(), all);
        assertEquals(11, bounds.line(), all);
        assertNull(byCode.get(KotlinPrintMessage.Code.JAVA_FALLBACK), all);
    }

    private static KotlinPrintMessage first(Map<KotlinPrintMessage.Code, List<KotlinPrintMessage>> byCode,
                                            KotlinPrintMessage.Code code, String all) {
        List<KotlinPrintMessage> list = byCode.get(code);
        assertNotNull(list, code + " missing in\n" + all);
        return list.getFirst();
    }

    /** A second file gets its own messages: no leftovers from the previous one. */
    @Test
    public void perFile() {
        List<String> first = messages(SOURCE).stream().map(m -> m.code() + ":" + m.line()).toList();
        List<String> second = messages(SOURCE.replace("class M {", "class N {")).stream()
                .map(m -> m.code() + ":" + m.line()).toList();
        assertEquals(first, second);
    }
}
