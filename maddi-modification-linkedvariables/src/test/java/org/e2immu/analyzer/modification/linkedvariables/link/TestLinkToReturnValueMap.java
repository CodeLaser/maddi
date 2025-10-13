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

package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLinkToReturnValueMap extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            class X {
                final static class M {
                    private int i;
                    public int getI() { return i; }
                    public void setI(int i) { this.i = i; }
                }
                static <X, Y> Map<X, Y> copy(Map<X, Y> map) {
                    return new HashMap<>(map);
                }
                static <X> Map<X, M> copy2(Map<X, M> map) {
                    return new HashMap<>(map);
                }
                static <Y> Map<M, Y> copy3(Map<M, Y> map) {
                    return new HashMap<>(map);
                }
                static Map<M, String> copy4(Map<M, String> map) {
                    return new HashMap<>(map);
                }
                static <Y> Map<String, Y> copy5(Map<String, Y> map) {
                    return new HashMap<>(map);
                }
                static Map<Object, Object> copy6(Map<Object, Object> map) {
                    return new HashMap<>(map);
                }
                static Map<?, ?> copy7(Map<?, ?> map) {
                    return new HashMap<>(map);
                }
                static Map copy8(Map map) {
                    return new HashMap<>(map);
                }
            }
            """;

    @DisplayName("return new HashMap<>(map)")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo copy = X.findUniqueMethod("copy", 1);
        assertEquals("0,1-4-0,1:map", copy.analysis().getOrDefault(LinkedVariablesImpl.LINKED_VARIABLES_METHOD,
                LinkedVariablesImpl.EMPTY).toString());

        MethodInfo copy2 = X.findUniqueMethod("copy2", 1);
        assertEquals("0,1M-4-0,1M:map", copy2.analysis().getOrDefault(LinkedVariablesImpl.LINKED_VARIABLES_METHOD,
                LinkedVariablesImpl.EMPTY).toString());

        MethodInfo copy3 = X.findUniqueMethod("copy3", 1);
        assertEquals("0M,1-4-0M,1:map", copy3.analysis().getOrDefault(LinkedVariablesImpl.LINKED_VARIABLES_METHOD,
                LinkedVariablesImpl.EMPTY).toString());

        MethodInfo copy4 = X.findUniqueMethod("copy4", 1);
        assertEquals("0M-4-0M:map", copy4.analysis().getOrDefault(LinkedVariablesImpl.LINKED_VARIABLES_METHOD,
                LinkedVariablesImpl.EMPTY).toString());

        MethodInfo copy5 = X.findUniqueMethod("copy5", 1);
        assertEquals("1-4-1:map", copy5.analysis().getOrDefault(LinkedVariablesImpl.LINKED_VARIABLES_METHOD,
                LinkedVariablesImpl.EMPTY).toString());

        MethodInfo copy6 = X.findUniqueMethod("copy6", 1);
        assertEquals("0;1-4-0;1:map", copy6.analysis().getOrDefault(LinkedVariablesImpl.LINKED_VARIABLES_METHOD,
                LinkedVariablesImpl.EMPTY).toString());

        // ? acts like Object
        MethodInfo copy7 = X.findUniqueMethod("copy7", 1);
        assertEquals("0;1-4-0;1:map", copy7.analysis().getOrDefault(LinkedVariablesImpl.LINKED_VARIABLES_METHOD,
                LinkedVariablesImpl.EMPTY).toString());

        // no type parameters acts like Object
        MethodInfo copy8 = X.findUniqueMethod("copy8", 1);
        assertEquals("0;1-4-0;1:map", copy8.analysis().getOrDefault(LinkedVariablesImpl.LINKED_VARIABLES_METHOD,
                LinkedVariablesImpl.EMPTY).toString());
    }


}
