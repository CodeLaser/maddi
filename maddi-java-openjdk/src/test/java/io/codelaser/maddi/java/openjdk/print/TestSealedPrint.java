/*
 * e2immu: a static code analyzer for effective and eventual immutability
 * Copyright 2020-2026, CodeLaser BV, https://www.e2immu.org
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

package io.codelaser.maddi.java.openjdk.print;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Printing a {@code sealed} type and its {@code permits} clause, and the {@code non-sealed} modifier on
 * a permitted subtype (Java 17).
 * <p>
 * Noticed 2026-08-14 while fixing the unnamed-pattern printer defect: the fixture declared
 * {@code sealed interface Shape permits Circle, Square} and printed back as plain
 * {@code interface Shape { }}. If a modifier the compiler requires is dropped on printing, then any verb
 * that relocates or re-prints such a type silently changes its meaning -- the same class of defect as
 * {@code TestUnnamedPatternVariable}, which produced code javac refuses.
 * <p>
 * Note {@code permits} is only required when the subtypes live in another compilation unit; when they
 * are nested alongside, javac infers it. The {@code sealed} / {@code non-sealed} MODIFIERS are never
 * optional, which is why they are asserted separately here.
 */
public class TestSealedPrint extends CommonTest {

    @Language("java")
    private static final String NESTED = """
            package a.b;
            class C {
                sealed interface Shape permits Circle, Square { }
                record Circle(double radius) implements Shape { }
                record Square(double side) implements Shape { }
            }
            """;

    @DisplayName("a sealed nested interface keeps its 'sealed' modifier")
    @Test
    public void sealedNested() {
        TypeInfo C = scan("a.b.C", NESTED);
        String printed = print2(C.compilationUnit());
        assertTrue(printed.contains("sealed interface Shape"),
                "'sealed' is not optional; dropping it changes what the type permits:\n" + printed);
    }

    @Language("java")
    private static final String TOP_LEVEL = """
            package a.b;
            public sealed interface Vehicle permits Car, Truck {
                int wheels();
            }
            """;

    @Language("java")
    private static final String CAR = """
            package a.b;
            public final class Car implements Vehicle {
                @Override public int wheels() { return 4; }
            }
            """;

    @Language("java")
    private static final String TRUCK = """
            package a.b;
            public non-sealed class Truck implements Vehicle {
                @Override public int wheels() { return 6; }
            }
            """;

    @DisplayName("a top-level sealed interface keeps 'sealed' and its permits clause")
    @Test
    public void sealedTopLevel() {
        TypeInfo vehicle = scan(false, "a.b.Vehicle", TOP_LEVEL, "a.b.Car", CAR, "a.b.Truck", TRUCK)
                .get("a.b.Vehicle");
        String printed = print2(vehicle.compilationUnit());
        assertTrue(printed.contains("sealed interface Vehicle"),
                "'sealed' is not optional:\n" + printed);
        assertTrue(printed.contains("permits"),
                "the subtypes are in other compilation units, so 'permits' is required:\n" + printed);
    }

    @DisplayName("a permitted subtype keeps its 'non-sealed' modifier")
    @Test
    public void nonSealedSubtype() {
        TypeInfo truck = scan(false, "a.b.Vehicle", TOP_LEVEL, "a.b.Car", CAR, "a.b.Truck", TRUCK)
                .get("a.b.Truck");
        String printed = print2(truck.compilationUnit());
        assertTrue(printed.contains("non-sealed class Truck"),
                "a subtype of a sealed type must be final, sealed or non-sealed:\n" + printed);
    }
}
