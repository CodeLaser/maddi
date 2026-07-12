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
 * Independent field-modification (@Modified / @NotModified field) tests. A field is modified when one of the type's
 * methods calls a modifying method on it (road-to-immutability, "Modification"). These assert FieldInfo.isModified()
 * directly, not the linked-variable representation from modification-link.
 */
public class TestFieldModification extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class X {
                static class M {
                    int i;
                    void set(int i) { this.i = i; }
                }
                static class Holder {
                    private final M mutated = new M();  // has a modifying call -> modified
                    private final M untouched = new M(); // only read -> not modified
                    private final List<String> list = new ArrayList<>(); // add() called -> modified
                    private final String label = "x";   // immutable, never modified

                    void go() {
                        mutated.set(1);
                        int j = untouched.i;
                        list.add(label);
                    }
                }
            }
            """;

    @DisplayName("a field is modified only when a method modifies the object it holds")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT1);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo holder = X.findSubType("Holder");
        assertTrue(holder.getFieldByName("mutated", true).isModified(), "mutated.set(1) modifies it");
        assertFalse(holder.getFieldByName("untouched", true).isModified(), "only read");
        assertTrue(holder.getFieldByName("list", true).isModified(), "list.add(...) modifies it");
        assertFalse(holder.getFieldByName("label", true).isModified(), "a String is immutable");
    }
}
