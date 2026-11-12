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
package io.codelaser.maddi.java.openjdk.method;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.ValueTypes;
import io.codelaser.maddi.cst.impl.analysis.ValueTypes.Obstacle;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link ValueTypes}: what stands in the way of a class being a value type, per obstacle. */
public class TestValueTypes extends CommonTest {

    @Language("java")
    private static final String SHAPES = """
            package a.b;
            import java.util.ArrayList;
            import java.util.IdentityHashMap;
            import java.util.List;
            import java.util.Map;
            public class Shapes {
                // a value in all but name
                public static final class Point {
                    private final int x;
                    private final int y;
                    public Point(int x, int y) { this.x = x; this.y = y; }
                    public int x() { return x; }
                    @Override public boolean equals(Object o) { return o instanceof Point p && p.x == x && p.y == y; }
                    @Override public int hashCode() { return 31 * x + y; }
                }
                public record Range(int from, int to) { }
                // not final, assigned only in its constructor: effectively final
                public static final class Late {
                    private int n;
                    Late(int n) { this.n = n; }
                }
                public static class Counter {
                    private int n;
                    void inc() { n++; }
                }
                public static class Base { protected int k; }
                public static final class Derived extends Base { }
                public static abstract class Abstract { int v; }
                public static final class Locked {
                    private final int v = 1;
                    synchronized int get() { return v; }
                }
                public static final class Leaky {
                    private final int v;
                    Leaky(List<Leaky> all) { v = 1; all.add(this); }
                }
                public static final class Empty { }
                // compared by identity, synchronized on, identity-keyed, given null
                public static final class Token {
                    private final String name;
                    Token(String name) { this.name = name; }
                }
                public final class Inner {
                    private final int v;
                    Inner() { v = size; }
                }
                private int size;
                interface Shape { }
                enum Color { RED }
                static boolean same(Token a, Token b) { return a == b; }
                static void lock(Token t) { synchronized (t) { } }
                static Map<Token, String> names() { return new IdentityHashMap<Token, String>(); }
                static boolean none(Range r) { return r == null; }
                static Point origin() { Point p = null; return p; }
                static Late noLate() { return null; }
                static List<Late> lates() { return new ArrayList<>(); }
            }
            """;

    private static String obstacles(ValueTypes values, TypeInfo t) {
        ValueTypes.Verdict v = values.verdict(t);
        return v == null ? "none" : v.obstacles().stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Test
    public void obstacles() {
        TypeInfo shapes = scan("a.b.Shapes", SHAPES);
        List<TypeInfo> all = shapes.recursiveSubTypeStreamIncludingLocal().toList();
        ValueTypes values = ValueTypes.of(all);

        // a local initialized with null, and returned
        assertEquals("NULL_USED", obstacles(values, shapes.findSubType("Point")));
        assertEquals(1, values.verdict(shapes.findSubType("Point")).sites().size(), "one site: the local");
        assertTrue(values.verdict(shapes.findSubType("Point")).ownEquals());
        assertEquals(2, values.verdict(shapes.findSubType("Point")).fields().size());
        assertNull(values.verdict(shapes.findSubType("Point")).immutable(), "no analysis ran");
        assertEquals("NULL_USED", obstacles(values, shapes.findSubType("Range")));
        assertEquals("NULL_USED", obstacles(values, shapes.findSubType("Late")), "return null");
        assertEquals("MUTABLE_FIELD", obstacles(values, shapes.findSubType("Counter")));
        assertEquals("SUBCLASSED", obstacles(values, shapes.findSubType("Base")));
        assertEquals("SUPERCLASS,NO_FIELDS", obstacles(values, shapes.findSubType("Derived")));
        assertEquals("ABSTRACT", obstacles(values, shapes.findSubType("Abstract")));
        assertEquals("SYNCHRONIZED", obstacles(values, shapes.findSubType("Locked")));
        assertEquals("THIS_ESCAPES", obstacles(values, shapes.findSubType("Leaky")));
        assertEquals("NO_FIELDS", obstacles(values, shapes.findSubType("Empty")));
        assertEquals("SYNCHRONIZED,IDENTITY_COMPARISON,IDENTITY_HASH", obstacles(values, shapes.findSubType("Token")));
        assertFalse(values.verdict(shapes.findSubType("Token")).identityFree());
        assertEquals(1, values.verdict(shapes.findSubType("Token")).sites(Obstacle.IDENTITY_COMPARISON).size());
        assertTrue(values.verdict(shapes.findSubType("Range")).identityFree());
        assertEquals("USES_ENCLOSING_INSTANCE", obstacles(values, shapes.findSubType("Inner")));
        assertEquals("none", obstacles(values, shapes.findSubType("Shape")));
        assertEquals("none", obstacles(values, shapes.findSubType("Color")));
        // Shapes itself: a field assigned nowhere, but Inner reads it, and it is no value either way
        assertTrue(values.verdict(shapes).free(Set.of(Obstacle.MUTABLE_FIELD)));
    }
}
