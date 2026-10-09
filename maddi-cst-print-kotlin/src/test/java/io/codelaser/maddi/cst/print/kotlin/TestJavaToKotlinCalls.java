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

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Calls and types that Kotlin writes differently: varargs given an array, raw types, type arguments a constructor
 * call cannot take, JDK members with another Kotlin name, char labels in a {@code when} over an int.
 */
public class TestJavaToKotlinCalls extends CommonJavaToKotlin {

    private static void contains(String kotlin, String expected) {
        assertTrue(strip(kotlin).contains(strip(expected)), () -> "expected\n" + expected + "\nin\n" + kotlin);
    }

    private static String strip(String s) {
        return s.lines().map(String::strip).collect(Collectors.joining("\n"));
    }

    @Language("java")
    private static final String VARARGS = """
            package a;
            import java.util.*;
            class C {
                static boolean any(String s, String... names) { return names.length > 0; }
                void m(Queue<String> queue, String[] names, String line) {
                    Collections.addAll(queue, names);
                    List<String> list = Arrays.asList(names);
                    List<String> words = new ArrayList<>(Arrays.asList(line.split("\\\\s+")));
                    boolean b = any("x", names) || any("y", "a", "b") || any("z");
                }
            }
            """;

    /** An array passed where Java takes varargs is spread; {@code String.split} takes a regex and returns an array. */
    @Test
    public void varargs() {
        String kotlin = kotlin(VARARGS);
        contains(kotlin, "Collections.addAll(queue, *names)");
        contains(kotlin, "Arrays.asList(*names)");
        contains(kotlin, "Arrays.asList(*line.split(\"\\\\s+\".toRegex())");
        contains(kotlin, ".dropLastWhile { it.isEmpty() }.toTypedArray()");
        contains(kotlin, "any(\"x\", *names) || any(\"y\", \"a\", \"b\") || any(\"z\")");
    }

    @Language("java")
    private static final String TYPES = """
            package a;
            import java.util.*;
            class C<E> {
                static class Box<T> { }
                private final int[][] next = new int[3][];
                private final Box[] boxes = new Box[2];
                private static final Map<String, Integer> CACHE = Collections.synchronizedMap(new HashMap<>());
                boolean m(Object o, String name, List<? extends Number> numbers) throws Exception {
                    Class cls = Class.forName(name);
                    List<? extends Number> copy = new ArrayList<>(numbers);
                    Set<String> set = new HashSet<>(List.of("a"));
                    return o instanceof Box && cls != null;
                }
            }
            """;

    /** Raw types get {@code <*>}; a constructor call takes no projections, nor the type's own parameters. */
    @Test
    public void types() {
        String kotlin = kotlin(TYPES);
        contains(kotlin, "arrayOfNulls<IntArray>(3)");
        contains(kotlin, "arrayOfNulls<Box<*>>(2)");
        contains(kotlin, "Collections.synchronizedMap(HashMap())");
        contains(kotlin, "val cls: Class<*> = ");
        contains(kotlin, "ArrayList(numbers)");
        contains(kotlin, "o is Box<*>");
    }

    @Language("java")
    private static final String LIBRARY = """
            package a;
            import java.util.*;
            import java.util.stream.*;
            class C {
                private final Object lock = new Object();
                String m(List<String> list, Map<String, Integer> map, long val, Boolean flag, String s) {
                    list.sort(Comparator.naturalOrder());
                    synchronized (lock) { lock.notifyAll(); }
                    String joined = map.entrySet().stream().map(Map.Entry::getValue).map(String::valueOf)
                            .collect(Collectors.joining(" "));
                    long count = list.stream().map(x -> flag).filter(Boolean::booleanValue).count();
                    long total = Stream.of(list).flatMap(Collection::stream).count();
                    if (val == -1 || val != 2) return s.concat("x");
                    return "%s and %s".formatted(joined, count);
                }
                String n(String format, Object... args) {
                    return format.formatted(args);
                }
            }
            """;

    /** JDK members Kotlin hides or renames. */
    @Test
    public void library() {
        String kotlin = kotlin(LIBRARY);
        contains(kotlin, "list.sortWith(Comparator.naturalOrder())");
        contains(kotlin, "(lock as java.lang.Object).notifyAll()");
        contains(kotlin, ".map({ it.value })");
        contains(kotlin, ".filter({ it })");
        contains(kotlin, ".flatMap({ it.stream() })");
        contains(kotlin, "`val` == -1L || `val` != 2L");
        contains(kotlin, "s.plus(\"x\")");
        contains(kotlin, "\"%s and %s\".format(joined, count)");
        contains(kotlin, "format.format(*args)");
    }

    @Language("java")
    private static final String CHAR_LABELS = """
            package a;
            class C {
                String m(int tag, char c) {
                    switch (tag) {
                        case 'e': return "enum";
                        case 'c': return "class";
                    }
                    return switch (c) { case 66 -> "B"; default -> "?"; };
                }
            }
            """;

    /** Java compares a char with an int by its code; Kotlin compares types first. */
    @Test
    public void charLabels() {
        String kotlin = kotlin(CHAR_LABELS);
        contains(kotlin, "'e'.code -> { return \"enum\" }");
        contains(kotlin, "when (c.code) {\n66 -> \"B\"");
    }
}
