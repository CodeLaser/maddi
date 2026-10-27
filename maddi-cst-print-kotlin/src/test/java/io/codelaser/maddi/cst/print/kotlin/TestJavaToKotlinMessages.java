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


package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.print.translate.PrintMessage;
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
        List<PrintMessage<KotlinPrintCode>> messages = messages(SOURCE);
        String all = messages.stream().map(PrintMessage::toString).collect(Collectors.joining("\n"));
        Map<KotlinPrintCode, List<PrintMessage<KotlinPrintCode>>> byCode = messages.stream()
                .collect(Collectors.groupingBy(PrintMessage::code));

        PrintMessage<KotlinPrintCode> clone = first(byCode, KotlinPrintCode.NULL_OVERRIDE_THROWS, all);
        assertEquals(PrintMessage.Severity.BEHAVIOUR_CHANGE, clone.severity(), all);
        assertEquals("a.M.Placeholder", clone.type(), all);
        assertEquals(9, clone.line(), all);

        PrintMessage<KotlinPrintCode> intoField = first(byCode, KotlinPrintCode.ASSERT_INTO_NON_NULL, all);
        assertEquals(14, intoField.line(), all);
        assertTrue(intoField.detail().contains("m.get(k)"), all);

        assertEquals(15, first(byCode, KotlinPrintCode.ASSERT_AT_DEREFERENCE, all).line(), all);
        assertEquals(15, first(byCode, KotlinPrintCode.ASSERT_AT_UNBOXING, all).line(), all);
        assertEquals(13, first(byCode, KotlinPrintCode.UNCHECKED_CAST, all).line(), all);
        assertEquals(6, first(byCode, KotlinPrintCode.PROTECTED_AS_PUBLIC, all).line(), all);
        assertNull(byCode.get(KotlinPrintCode.JAVA_FALLBACK), all);
    }

    private static PrintMessage<KotlinPrintCode> first(Map<KotlinPrintCode, List<PrintMessage<KotlinPrintCode>>> byCode,
                                            KotlinPrintCode code, String all) {
        List<PrintMessage<KotlinPrintCode>> list = byCode.get(code);
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
