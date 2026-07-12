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
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Modification detected in less-obvious contexts: a static field mutated from a static method, an enclosing instance's
 * field mutated from an inner class, and a fluent method that both mutates and returns {@code this}.
 */
public class TestModificationContext extends CommonTest {

    private TypeInfo analyze(String input) {
        TypeInfo X = javaInspector.parse("a.b.X", input);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);
        return X;
    }

    @Language("java") private static final String STATIC_FIELD = """
            package a.b;
            import java.util.ArrayList; import java.util.List;
            class X {
                static class Registry {
                    private static final List<String> ALL = new ArrayList<>();
                    static void register(String s) { ALL.add(s); }
                }
            }
            """;

    @DisplayName("a static field mutated from a static method is detected as modified")
    @Test
    public void staticField() {
        TypeInfo registry = analyze(STATIC_FIELD).findSubType("Registry");
        assertTrue(registry.getFieldByName("ALL", true).isModified());
    }

    @Language("java") private static final String INNER = """
            package a.b;
            class X {
                private int count;
                class Inner { void bump() { count++; } }
                int read() { return count; }
            }
            """;

    @DisplayName("an enclosing instance field mutated from an inner class is detected as modified")
    @Test
    public void innerClass() {
        TypeInfo X = analyze(INNER);
        assertTrue(X.getFieldByName("count", true).isModified());
    }

    @Language("java") private static final String FLUENT = """
            package a.b;
            class X {
                static class Builder {
                    private final StringBuilder sb = new StringBuilder();
                    Builder append(String s) { sb.append(s); return this; }
                    String build() { return sb.toString(); }
                }
            }
            """;

    @DisplayName("a fluent method that mutates a field and returns this is both @Fluent and modifying")
    @Test
    public void fluent() {
        TypeInfo builder = analyze(FLUENT).findSubType("Builder");
        MethodInfo append = builder.findUniqueMethod("append", 1);
        assertTrue(append.isFluent());
        assertTrue(append.isModifying());
        assertTrue(builder.getFieldByName("sb", true).isModified());
    }
}
