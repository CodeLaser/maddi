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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Soundness regression tests: a field modification must be detected even when it happens through an indirection --
 * a returning getter, a local alias, a conditional, or a loop. Missing any of these would be an unsound false negative.
 * Asserts FieldInfo.isModified() and MethodInfo.isModifying().
 */
public class TestModificationIndirect extends CommonTest {

    private TypeInfo analyze(String input, String subType) {
        TypeInfo X = javaInspector.parse("a.b.X", input);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);
        return X.findSubType(subType);
    }

    @Language("java") private static final String INDIRECT_GETTER = """
            package a.b;
            import java.util.ArrayList; import java.util.List;
            class X {
                static class Y {
                    private final List<String> list = new ArrayList<>();
                    List<String> getList() { return list; }
                    void indirectAdd(String s) { getList().add(s); }
                }
            }
            """;

    @DisplayName("modification through a returning getter is detected")
    @Test
    public void indirectGetter() {
        TypeInfo Y = analyze(INDIRECT_GETTER, "Y");
        assertTrue(Y.getFieldByName("list", true).isModified());
        assertTrue(Y.findUniqueMethod("indirectAdd", 1).isModifying());
    }

    @Language("java") private static final String LOCAL_ALIAS = """
            package a.b;
            import java.util.ArrayList; import java.util.List;
            class X {
                static class Y {
                    private final List<String> list = new ArrayList<>();
                    void aliasAdd(String s) { List<String> l = list; l.add(s); }
                }
            }
            """;

    @DisplayName("modification through a local alias of a field is detected")
    @Test
    public void localAlias() {
        TypeInfo Y = analyze(LOCAL_ALIAS, "Y");
        assertTrue(Y.getFieldByName("list", true).isModified());
        assertTrue(Y.findUniqueMethod("aliasAdd", 1).isModifying());
    }

    @Language("java") private static final String CONDITIONAL_AND_LOOP = """
            package a.b;
            import java.util.ArrayList; import java.util.List;
            class X {
                static class Y {
                    private final List<String> conditional = new ArrayList<>();
                    private final List<String> loop = new ArrayList<>();
                    void maybeAdd(String s, boolean b) { if (b) { conditional.add(s); } }
                    void addAll(String[] items) { for (String s : items) loop.add(s); }
                }
            }
            """;

    @DisplayName("modification inside a conditional or a loop is detected")
    @Test
    public void conditionalAndLoop() {
        TypeInfo Y = analyze(CONDITIONAL_AND_LOOP, "Y");
        assertTrue(Y.getFieldByName("conditional", true).isModified());
        assertTrue(Y.getFieldByName("loop", true).isModified());
        assertTrue(Y.findUniqueMethod("maybeAdd", 2).isModifying());
        assertTrue(Y.findUniqueMethod("addAll", 1).isModifying());
    }
}
