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
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Independent method-modification (@Modified / @NotModified) tests. A method is modifying when it assigns a field of
 * its own type or calls a modifying method on one; it is non-modifying when it only reads (road-to-immutability,
 * "Modification"). These assert MethodInfo.isModifying() and parameter modification directly, not the linked-variable
 * representation from modification-link.
 */
public class TestMethodModification extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                static class Counter {
                    private int count;
                    void increment() { count++; }        // modifies a field of this -> modifying
                    void set(int c) { this.count = c; }  // assigns a field of this -> modifying
                    int get() { return count; }          // reads only -> non-modifying
                    int plus(int x) { return count + x; }// reads field and argument -> non-modifying
                }
            }
            """;

    @DisplayName("assigning or incrementing an own field is modifying; reading is not")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT1);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo counter = X.findSubType("Counter");
        assertTrue(counter.findUniqueMethod("increment", 0).isModifying());
        assertTrue(counter.findUniqueMethod("set", 1).isModifying());
        assertFalse(counter.findUniqueMethod("get", 0).isModifying());
        assertFalse(counter.findUniqueMethod("plus", 1).isModifying());

        // neither setter nor computation modifies its argument (the ints are copied by value)
        assertFalse(counter.findUniqueMethod("set", 1).parameters().getFirst().isModified());
        assertFalse(counter.findUniqueMethod("plus", 1).parameters().getFirst().isModified());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            class X {
                static class Service {
                    private final List<String> log;
                    Service(List<String> log) { this.log = log; }
                    // calls a modifying method on a field -> modifying
                    void record(String s) { log.add(s); }
                    // only reads the field's size -> non-modifying
                    int size() { return log.size(); }
                }
            }
            """;

    @DisplayName("calling a modifying method on a field is modifying; querying it is not")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT2);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo service = X.findSubType("Service");
        assertTrue(service.findUniqueMethod("record", 1).isModifying());
        assertFalse(service.findUniqueMethod("size", 0).isModifying());
        // the String argument is never modified
        assertFalse(service.findUniqueMethod("record", 1).parameters().getFirst().isModified());
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class X {
                static class M {
                    int i;
                    void set(int i) { this.i = i; }
                }
                static class Chain {
                    private final M m = new M();
                    private void doSet() { m.set(1); }   // modifies the field
                    public void publicSet() { doSet(); } // modifying only because it calls doSet()
                    public int read() { return m.i; }    // reads only
                }
            }
            """;

    @DisplayName("modification propagates through a private-method call: a caller of a modifying method is modifying")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT3);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo chain = X.findSubType("Chain");
        assertTrue(chain.findUniqueMethod("doSet", 0).isModifying());
        assertTrue(chain.findUniqueMethod("publicSet", 0).isModifying(), "propagated from doSet()");
        assertFalse(chain.findUniqueMethod("read", 0).isModifying());
        assertTrue(chain.getFieldByName("m", true).isModified());
    }
}
