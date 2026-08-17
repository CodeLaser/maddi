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
 * Printing a TYPE PATTERN whose variable is unnamed: {@code case Square _ ->} (Java 21).
 * <p>
 * {@code RecordPatternImpl#print} has an {@code unnamedPattern} branch that emits a bare {@code _},
 * which is right for a record-deconstruction component ({@code case Point(int x, _)}). A type pattern
 * with an unnamed variable is a different shape: it takes the {@code localVariable} branch, whose
 * {@code simpleName()} is the empty string, and {@code TextImpl}'s constructor asserts against blank
 * text — so printing throws {@code AssertionError} rather than emitting {@code Square _}.
 * <p>
 * Found on trino 2026-08-13 through {@code extract.extractCompanion}: moving
 * {@code io.trino.util.variant.VariantWriter}'s factory to a companion re-printed its switch and
 * produced {@code case VariantType ->}, which javac rejects with "type pattern expected". The
 * refactoring reported success; only the post-write recompile caught it.
 */
public class TestUnnamedPatternVariable extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
                sealed interface Shape permits Circle, Square { }
                record Circle(double radius) implements Shape { }
                record Square(double side) implements Shape { }

                static String describe(Shape shape) {
                    return switch (shape) {
                        case Circle c -> "circle " + c.radius();
                        case Square _ -> "a square";
                    };
                }
            }
            """;

    // nested types print qualified (C.Square), so match on the pattern itself, not the leading "case"
    @DisplayName("a type pattern with an unnamed variable prints as 'Square _', not '_' and not a crash")
    @Test
    public void unnamedTypePatternPrints() {
        TypeInfo C = scan("a.b.C", INPUT);
        String printed = print2(C.compilationUnit());
        assertTrue(printed.contains("Square _ ->"),
                "the type must survive alongside the unnamed variable:\n" + printed);
        assertTrue(printed.contains("Circle c ->"),
                "a NAMED pattern variable was never affected -- the control:\n" + printed);
    }
}
