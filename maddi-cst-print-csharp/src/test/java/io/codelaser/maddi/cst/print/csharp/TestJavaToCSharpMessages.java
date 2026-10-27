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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.print.translate.PrintMessage;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The printer's messages ({@link CSharpPrintCode}): code, severity, type and line, as the ratchets' reports and a
 * translation's message list have them. A message about a type reference (a wildcard, a raw type, {@code X.class})
 * has no element of its own and is placed at its type; only its detail is asserted.
 */
public class TestJavaToCSharpMessages extends CommonJavaToCSharp {

    @Language("java")
    private static final String SOURCE = """
            package a;
            import java.util.*;
            public class M {
                int x;
                { x = 1; }
                static int sum(List<? extends Number> l) { return l.size(); }
                static int raw(List l) { return l.size(); }
                enum Op {
                    PLUS { int apply(int a) { return a; } };
                    abstract int apply(int a);
                }
                enum Level {
                    LOW(1);
                    final int n;
                    Level(int n) { this.n = n; }
                }
                record R(int a) { R(String s) { this(s.length()); } }
                Map<String, ? super Integer> sink;
                static class Stack extends ArrayList<String> { @Override public boolean add(String s) { return super.add(s); } }
                static int match(Object o) { return switch (o) { case R(int a) -> a; default -> 0; }; }
                static Object type() { return Optional.class; }
                static int[][] grid(int a, int b) { return new int[a][b]; }
                static Object sw(Object o) {
                    return switch (o) { case String s when s.isEmpty() -> 0; default -> { yield 1; } };
                }
                static Object unmapped() { return java.util.ServiceLoader.load(Runnable.class).stream(); }
                int local(int k) {
                    class L { int g() { return k + x; } }
                    return new L().g();
                }
            }
            class G<E> { class In { E e; } }
            """;

    @Test
    public void messages() {
        List<PrintMessage<CSharpPrintCode>> messages = messages(SOURCE);
        String all = messages.stream().map(PrintMessage::toString).collect(Collectors.joining("\n"));
        Map<CSharpPrintCode, List<PrintMessage<CSharpPrintCode>>> byCode = messages.stream()
                .collect(Collectors.groupingBy(PrintMessage::code));
        // every message has its code's severity
        messages.forEach(m -> assertEquals(m.code().severity(), m.severity(), all));

        at(byCode, CSharpPrintCode.INSTANCE_INITIALIZER, "a.M", 5, all);
        at(byCode, CSharpPrintCode.ENUM_AS_CLASS, "a.M.Op", 8, all);
        at(byCode, CSharpPrintCode.ANONYMOUS_CLASS, "a.M.Op", 9, all);
        assertEquals("Op.PLUS", first(byCode, CSharpPrintCode.ANONYMOUS_CLASS, all).detail(), all);
        assertEquals(2, byCode.get(CSharpPrintCode.ENUM_AS_CLASS).size(), all);
        at(byCode, CSharpPrintCode.RECORD_CONSTRUCTOR, "a.M.R", 17, all);
        assertEquals("a.M.R.<init>(String)", first(byCode, CSharpPrintCode.RECORD_CONSTRUCTOR, all).detail(), all);
        at(byCode, CSharpPrintCode.LIST_SUBCLASS, "a.M.Stack", 19, all);
        at(byCode, CSharpPrintCode.MULTI_DIMENSIONAL_ARRAY, "a.M", 22, all);
        at(byCode, CSharpPrintCode.LOCAL_CLASS, "a.M", 28, all);
        at(byCode, CSharpPrintCode.NESTED_IN_GENERIC, "a.G.In", 32, all);

        assertEquals("Type ? super Integer", first(byCode, CSharpPrintCode.WILDCARD_AS_BOUND, all).detail(), all);
        assertEquals("java.util.List", first(byCode, CSharpPrintCode.RAW_TYPE, all).detail(), all);
        assertEquals("java.util.Optional.class", first(byCode, CSharpPrintCode.UNMAPPED_JDK, all).detail(), all);

        // the record pattern and the switch expression's block arm are C#'s, without a message
        assertNull(byCode.get(CSharpPrintCode.SWITCH_FORM), all);
        assertNull(byCode.get(CSharpPrintCode.JAVA_FALLBACK), all);
    }

    private static void at(Map<CSharpPrintCode, List<PrintMessage<CSharpPrintCode>>> byCode, CSharpPrintCode code,
                           String type, int line, String all) {
        first(byCode, code, all);
        PrintMessage<CSharpPrintCode> m = byCode.get(code).stream().filter(x -> type.equals(x.type())).findFirst()
                .orElse(null);
        assertNotNull(m, code + " of " + type + " missing in\n" + all);
        assertEquals(line, m.line(), code + " in\n" + all);
    }

    private static PrintMessage<CSharpPrintCode> first(Map<CSharpPrintCode, List<PrintMessage<CSharpPrintCode>>> byCode,
                                                      CSharpPrintCode code, String all) {
        List<PrintMessage<CSharpPrintCode>> list = byCode.get(code);
        assertNotNull(list, code + " missing in\n" + all);
        return list.getFirst();
    }

    /** The messages of one file do not depend on another's: the state is per file. */
    @Test
    public void perFile() {
        List<String> first = messages(SOURCE).stream().map(m -> m.code() + ":" + m.line()).toList();
        // another package: the inspector has parsed a.G already
        List<String> second = messages(SOURCE.replace("package a;", "package b;").replace("class M {", "class N {"))
                .stream().map(m -> m.code() + ":" + m.line()).toList();
        assertEquals(first, second);
    }
}
