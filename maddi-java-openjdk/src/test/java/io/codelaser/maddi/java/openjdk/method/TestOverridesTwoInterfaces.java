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

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
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

    @Language("java")
    private static final String ALL_IN_ONE = """
            package a.b;
            interface InterfaceC {
                void cm1();
                void cm3common();
            }
            interface InterfaceIC {
                void cm3common();
                void method3();
            }
            class C implements InterfaceC, InterfaceIC {
                @Override public void cm1() { }
                @Override public void cm3common() { }
                @Override public void method3() { }
            }
            """;

    /**
     * The same three types in ONE compilation unit, which is legal Java: one file may declare several top-level
     * types as long as at most one is public. The overrides resolve exactly as they do across three units.
     * <p>
     * This test spent a day recorded as a *failing* reproducer of a parser defect ("overrides() is empty for
     * every method of a type sharing a compilation unit"), and there was no defect. {@code CommonTest.scan(fqn,
     * content)} used to answer {@code primaryTypes().getFirst()} — ignoring the fqn it was handed — so the
     * assertions were reading {@code a.b.InterfaceC}, declared first in the file, whose {@code cm1()} correctly
     * overrides nothing. The harness now selects by fully qualified name.
     * <p>
     * Kept as a regression test on both facts: several top-level types per unit resolve their overrides, and
     * the harness hands back the type you asked for.
     */
    @DisplayName("types sharing one compilation unit still resolve their overrides")
    @Test
    public void sameCompilationUnit() {
        TypeInfo C = scan("a.b.C", ALL_IN_ONE);
        assertEquals(List.of("a.b.InterfaceC"), C.findUniqueMethod("cm1", 0).overrides().stream()
                .map(m -> m.typeInfo().fullyQualifiedName()).sorted().toList());
        assertEquals(List.of("a.b.InterfaceC", "a.b.InterfaceIC"),
                C.findUniqueMethod("cm3common", 0).overrides().stream()
                        .map(m -> m.typeInfo().fullyQualifiedName()).sorted().toList());
    }
}
