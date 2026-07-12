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
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Independent @Independent tests. A type is dependent when it exposes its mutable internals to the outside: a
 * constructor/method parameter that is stored into a field of mutable type is a dependent parameter, and an accessor
 * that hands out that mutable field is a dependent method (road-to-immutability, "Linking and dependence"). A type
 * that only stores copies or immutable/primitive data is independent. These assert the computed INDEPENDENT_*
 * properties, not the linked-variable representation.
 */
public class TestIndependent extends CommonTest {

    private static Value.Independent independentType(TypeInfo typeInfo) {
        return typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT);
    }

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                // mutable helper
                static class M {
                    int i;
                    void set(int i) { this.i = i; }
                }
                // stores the mutable argument in a field and hands it back -> dependent parameter, method and type
                static class Holder {
                    private final M m;
                    Holder(M m) { this.m = m; }
                    M getM() { return m; }
                }
                // reads only a primitive out of the argument, stores no reference to it -> independent
                static class Copy {
                    private final int i;
                    Copy(M m) { this.i = m.i; }
                    int getI() { return i; }
                }
            }
            """;

    @DisplayName("exposing a stored mutable field is dependent; storing only a copied primitive is independent")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT1);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo holder = X.findSubType("Holder");
        ParameterInfo holderParam = holder.findConstructor(1).parameters().getFirst();
        assertTrue(holderParam.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT).isDependent(),
                "the mutable argument is stored in a field");
        MethodInfo getM = holder.findUniqueMethod("getM", 0);
        assertTrue(getM.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT).isDependent(),
                "the accessor returns the mutable field");
        assertTrue(independentType(holder).isDependent(), "Holder exposes its mutable internals");

        TypeInfo copy = X.findSubType("Copy");
        ParameterInfo copyParam = copy.findConstructor(1).parameters().getFirst();
        assertTrue(copyParam.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT).isIndependent(),
                "the argument is not stored, only a primitive is read from it");
        assertTrue(independentType(copy).isIndependent(), "Copy retains no reference to mutable arguments");
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X {
                // holds and exposes only immutable data (String); exposing an immutable field is independent
                static class Names {
                    private final String name;
                    Names(String name) { this.name = name; }
                    String getName() { return name; }
                }
            }
            """;

    @DisplayName("exposing an immutable field (String) is independent, even though it is handed out directly")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT2);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo names = X.findSubType("Names");
        ParameterInfo param = names.findConstructor(1).parameters().getFirst();
        assertTrue(param.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT).isIndependent(),
                "a String argument carries no mutable state");
        MethodInfo getName = names.findUniqueMethod("getName", 0);
        assertTrue(getName.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT).isIndependent(),
                "returning an immutable field does not expose mutable state");
        assertTrue(independentType(names).isIndependent());
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class X {
                // an array is mutable; storing and handing out the same array exposes it -> dependent
                static class ArrHolder {
                    private final int[] data;
                    ArrHolder(int[] data) { this.data = data; }
                    int[] getData() { return data; }
                }
            }
            """;

    @DisplayName("exposing an array field is dependent, because arrays are mutable")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT3);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo arrHolder = X.findSubType("ArrHolder");
        assertTrue(arrHolder.findConstructor(1).parameters().getFirst()
                        .analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT).isDependent(),
                "the array argument is stored in a field");
        assertTrue(arrHolder.findUniqueMethod("getData", 0)
                        .analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT).isDependent(),
                "the accessor hands out the mutable array");
        assertTrue(independentType(arrHolder).isDependent());
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class X {
                // copies its argument into the field and hands out copies -> the caller's list is never aliased,
                // so the constructor parameter and the type are independent
                static class Defensive {
                    private final List<String> list;
                    Defensive(List<String> input) { this.list = new ArrayList<>(input); }
                    List<String> snapshot() { return new ArrayList<>(list); }
                }
            }
            """;

    @DisplayName("defensive copying makes the constructor parameter and the type independent")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT4);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo defensive = X.findSubType("Defensive");
        assertFalse(defensive.findConstructor(1).parameters().getFirst()
                        .analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT).isDependent(),
                "the argument is copied, not aliased into the field");
        assertFalse(independentType(defensive).isDependent(), "Defensive retains no alias to the caller's list");
    }
}
