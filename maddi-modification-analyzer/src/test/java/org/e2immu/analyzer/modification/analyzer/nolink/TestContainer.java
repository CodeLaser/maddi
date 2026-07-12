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

package org.e2immu.analyzer.modification.analyzer.nolink;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Independent @Container tests. A type is a container when no non-private method or constructor modifies its arguments
 * (road-to-immutability, "Containers"). These tests assert the computed CONTAINER_TYPE property and the parameter
 * modification that drives it; they do not depend on the exact linked-variable representation from modification-link.
 */
public class TestContainer extends CommonTest {

    private static boolean isContainer(TypeInfo typeInfo) {
        return typeInfo.analysis().getOrDefault(PropertyImpl.CONTAINER_TYPE, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            class X {
                // stores its arguments in final fields, never modifies them -> container
                static class Pair<K, V> {
                    public final K k;
                    public final V v;
                    public Pair(K k, V v) { this.k = k; this.v = v; }
                    public K getK() { return k; }
                    public V getV() { return v; }
                }
                // a variable field with a setter; the String argument is never modified -> container
                static class ErrorMessage {
                    private String message;
                    ErrorMessage(String message) { this.message = message; }
                    public String getMessage() { return message; }
                    public void setMessage(String message) { this.message = message; }
                }
                // modifies one of its method's arguments (list.add) -> not a container
                static class Sink {
                    void addTo(List<String> list, String s) { list.add(s); }
                }
            }
            """;

    @DisplayName("Pair and ErrorMessage are containers; a type that modifies a method argument is not")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT1);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo pair = X.findSubType("Pair");
        assertFalse(pair.findUniqueMethod("getK", 0).isModifying());
        assertTrue(isContainer(pair), "Pair should be a container");

        TypeInfo errorMessage = X.findSubType("ErrorMessage");
        MethodInfo setMessage = errorMessage.findUniqueMethod("setMessage", 1);
        assertFalse(setMessage.parameters().getFirst().isModified(), "the String argument is not modified");
        assertTrue(isContainer(errorMessage), "ErrorMessage does not modify any argument, so it is a container");

        TypeInfo sink = X.findSubType("Sink");
        MethodInfo addTo = sink.findUniqueMethod("addTo", 2);
        assertTrue(addTo.parameters().getFirst().isModified(), "the 'list' argument is modified by list.add(s)");
        assertFalse(isContainer(sink), "Sink modifies a method argument, so it is not a container");
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X {
                static class M {
                    int i;
                    void set(int i) { this.i = i; }
                }
                // stores the mutable argument in a field but never modifies it: storing is allowed, so this IS a
                // container (even though it is not independent).
                static class Box {
                    private M m;
                    void put(M m) { this.m = m; }
                    int read() { return m.i; }
                }
                // modifies its argument directly -> not a container
                static class Mutator {
                    void bump(M m) { m.set(1); }
                }
            }
            """;

    @DisplayName("storing a mutable argument keeps container status; only modifying an argument breaks it")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT2);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo box = X.findSubType("Box");
        assertFalse(box.findUniqueMethod("put", 1).parameters().getFirst().isModified(),
                "put stores the argument but does not modify it");
        assertTrue(isContainer(box), "storing (not modifying) an argument is allowed for a container");

        TypeInfo mutator = X.findSubType("Mutator");
        assertTrue(mutator.findUniqueMethod("bump", 1).parameters().getFirst().isModified(),
                "bump calls a modifying method on its argument");
        assertFalse(isContainer(mutator), "Mutator modifies an argument, so it is not a container");
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.List;
            class X {
                static class Base {
                    void safe(String s) { /* does not modify s */ }
                }
                // adds only a non-modifying method -> still a container
                static class GoodChild extends Base {
                    int get() { return 1; }
                }
                // adds a method that modifies its argument -> not a container
                static class BadChild extends Base {
                    void clearIt(List<String> list) { list.clear(); }
                }
            }
            """;

    @DisplayName("a container stays a container through inheritance, unless a subtype modifies an argument")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT3);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        assertTrue(isContainer(X.findSubType("Base")), "Base modifies no argument");
        assertTrue(isContainer(X.findSubType("GoodChild")), "GoodChild only adds a non-modifying method");
        assertFalse(isContainer(X.findSubType("BadChild")), "BadChild modifies an argument via list.clear()");
    }
}
