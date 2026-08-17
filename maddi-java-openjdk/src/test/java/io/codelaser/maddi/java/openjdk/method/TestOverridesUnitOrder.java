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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The order in which compilation units are handed to javac must not change what a method overrides.
 * <p>
 * It did. A method's identity is its fully qualified name -- {@code MethodInfoImpl} defines equals/hashCode on
 * it -- but that name is only computed when the method's parameters are committed, which is deliberately
 * deferred for methods declared in the sources being scanned. Until then the name was {@code "?.?." + name}, so
 * {@code InterfaceC.cm3common} and {@code InterfaceIC.cm3common} were equal to each other, and the
 * {@code Set<MethodInfo>} that {@code ScanCompilationUnit} collects the overrides into kept only one. Two of
 * the six orders below lost {@code InterfaceIC}; the other four had both interfaces committed before {@code C}
 * was scanned and so escaped it.
 * <p>
 * It reached a consumer: jfocus's splitclass groups members by the interface they implement, so in an affected
 * run one method silently belonged to one interface instead of two, and a downstream "called from exactly one
 * interface?" test flipped. Because real callers pass their sources in a {@code Map.of(...)}, whose iteration
 * order the JDK randomises per JVM run, the split differed between runs of the same input on the same code.
 * <p>
 * Six separate tests rather than six calls in one: the harness's {@code infoByFqn} is per-instance, and a
 * second {@code scan()} in the same method trips "Inspection ... has already been committed".
 */
public class TestOverridesUnitOrder extends CommonTest {

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

    private static final Map<String, String> SRC = Map.of("a.b.InterfaceC", INTERFACE_C,
            "a.b.InterfaceIC", INTERFACE_IC, "a.b.C", C_SRC);

    private List<String> overridesOfCm3common(String... order) {
        Map<String, String> ordered = new LinkedHashMap<>();
        for (String fqn : order) ordered.put(fqn, SRC.get(fqn));
        TypeInfo C = scan(false, ordered).primaryTypes().stream()
                .filter(t -> "a.b.C".equals(t.fullyQualifiedName())).findFirst().orElseThrow();
        MethodInfo cm3common = C.findUniqueMethod("cm3common", 0);
        return cm3common.overrides().stream().map(m -> m.typeInfo().fullyQualifiedName()).sorted().toList();
    }

    private static final List<String> BOTH = List.of("a.b.InterfaceC", "a.b.InterfaceIC");

    // Before the fix, "C first" and "C in the middle, InterfaceIC first" both answered [a.b.InterfaceC].
    @DisplayName("C last")
    @Test
    public void cLast() {
        assertEquals(BOTH, overridesOfCm3common("a.b.InterfaceC", "a.b.InterfaceIC", "a.b.C"));
    }

    @DisplayName("C last, InterfaceIC first")
    @Test
    public void cLastICFirst() {
        assertEquals(BOTH, overridesOfCm3common("a.b.InterfaceIC", "a.b.InterfaceC", "a.b.C"));
    }

    @DisplayName("C first")
    @Test
    public void cFirst() {
        assertEquals(BOTH, overridesOfCm3common("a.b.C", "a.b.InterfaceC", "a.b.InterfaceIC"));
    }

    @DisplayName("C first, InterfaceIC before InterfaceC")
    @Test
    public void cFirstICBeforeC() {
        assertEquals(BOTH, overridesOfCm3common("a.b.C", "a.b.InterfaceIC", "a.b.InterfaceC"));
    }

    @DisplayName("C in the middle")
    @Test
    public void cMiddle() {
        assertEquals(BOTH, overridesOfCm3common("a.b.InterfaceC", "a.b.C", "a.b.InterfaceIC"));
    }

    @DisplayName("C in the middle, InterfaceIC first")
    @Test
    public void cMiddleICFirst() {
        assertEquals(BOTH, overridesOfCm3common("a.b.InterfaceIC", "a.b.C", "a.b.InterfaceC"));
    }
}
