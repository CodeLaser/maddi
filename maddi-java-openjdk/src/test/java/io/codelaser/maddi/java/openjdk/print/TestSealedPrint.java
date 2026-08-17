/*
 * e2immu: a static code analyzer for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You
 * should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
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
