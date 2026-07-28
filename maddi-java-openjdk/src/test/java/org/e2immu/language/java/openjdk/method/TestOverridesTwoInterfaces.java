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

package org.e2immu.language.java.openjdk.method;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * One method, one signature, declared by TWO implemented interfaces: {@code overrides()} must report both.
 * <p>
 * A consumer that groups members by the interface they implement -- splitclass does exactly this -- reads
 * {@code overrides()} to decide where a method belongs. If it sees only one of the two interfaces, the method is
 * attributed to that one alone, and a downstream "is this called from exactly one interface?" test flips its
 * answer. That is the shape of a non-determinism seen from splitclass, where the same input moved a helper out
 * of the common part in some runs and not others.
 */
public class TestOverridesTwoInterfaces extends CommonTest {

    @Language("java")
    private static final String INTERFACE_C = """
            package a.b;
            public interface InterfaceC {
                void cm1();
                void cm3common();
            }
            """;

    @Language("java")
    private static final String INTERFACE_IC = """
            package a.b;
            public interface InterfaceIC {
                void cm3common();
                void method3();
            }
            """;

    @Language("java")
    private static final String C_SRC = """
            package a.b;
            public class C implements InterfaceC, InterfaceIC {
                @Override public void cm1() { }
                @Override public void cm3common() { }
                @Override public void method3() { }
            }
            """;

    private TypeInfo parseC() {
        Map<String, TypeInfo> types = scan(false, "a.b.InterfaceC", INTERFACE_C,
                "a.b.InterfaceIC", INTERFACE_IC, "a.b.C", C_SRC);
        return types.get("a.b.C");
    }

    @DisplayName("a method implementing the same signature from two interfaces overrides both")
    @Test
    public void test1() {
        TypeInfo C = parseC();
        MethodInfo cm3common = C.findUniqueMethod("cm3common", 0);
        List<String> owners = cm3common.overrides().stream()
                .map(m -> m.typeInfo().fullyQualifiedName())
                .sorted()
                .toList();
        assertEquals(List.of("a.b.InterfaceC", "a.b.InterfaceIC"), owners,
                "both declaring interfaces must be reported, deterministically");
    }

    @DisplayName("the single-interface methods report exactly their own")
    @Test
    public void test2() {
        TypeInfo C = parseC();
        assertEquals(List.of("a.b.InterfaceC"), C.findUniqueMethod("cm1", 0).overrides().stream()
                .map(m -> m.typeInfo().fullyQualifiedName()).sorted().toList());
        assertEquals(List.of("a.b.InterfaceIC"), C.findUniqueMethod("method3", 0).overrides().stream()
                .map(m -> m.typeInfo().fullyQualifiedName()).sorted().toList());
    }
}
