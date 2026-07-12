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
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.CONTAINER_TYPE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Independent enum tests. An enum with only immutable (or no) instance fields and no modifying methods is immutable and
 * a container. Asserts the computed IMMUTABLE_TYPE / CONTAINER_TYPE properties.
 */
public class TestEnum extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                enum Color { RED, GREEN, BLUE }
                enum Planet {
                    EARTH(9.8), MARS(3.7);
                    private final double gravity;
                    Planet(double g) { this.gravity = g; }
                    double gravity() { return gravity; }
                }
            }
            """;

    @DisplayName("a plain enum and an enum with a final immutable field are immutable containers")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT1);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo color = X.findSubType("Color");
        assertTrue(immutable(color).isAtLeastImmutableHC(), "have " + immutable(color));
        assertTrue(isContainer(color));

        TypeInfo planet = X.findSubType("Planet");
        assertTrue(immutable(planet).isAtLeastImmutableHC(), "final double field, no modification, have " + immutable(planet));
        assertTrue(isContainer(planet));
    }

    private static Value.Immutable immutable(TypeInfo typeInfo) {
        return typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE);
    }

    private static boolean isContainer(TypeInfo typeInfo) {
        return typeInfo.analysis().getOrDefault(CONTAINER_TYPE, ValueImpl.BoolImpl.FALSE).isTrue();
    }
}
