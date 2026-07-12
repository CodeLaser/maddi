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

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Independent @Immutable tests seen through the modification lens (plain classes rather than records). A type with
 * effectively-final fields of immutable type and no modifying methods is immutable; introducing a single field setter
 * makes it mutable (road-to-immutability, "Immutability"). Asserts the computed IMMUTABLE_TYPE property.
 */
public class TestImmutableViaModification extends CommonTest {

    private static Value.Immutable immutable(TypeInfo typeInfo) {
        return typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE);
    }

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                // final class, final fields of immutable type, no modification, returns fresh copies -> deeply
                // immutable (a non-final class would only be immutable-with-hidden-content, as a subtype could add
                // hidden state)
                static final class Point {
                    private final int x;
                    private final int y;
                    Point(int x, int y) { this.x = x; this.y = y; }
                    int x() { return x; }
                    int y() { return y; }
                    Point translate(int dx, int dy) { return new Point(x + dx, y + dy); }
                }
                // same shape but with a setter that mutates a field -> mutable
                static class MutablePoint {
                    private int x;
                    private int y;
                    MutablePoint(int x, int y) { this.x = x; this.y = y; }
                    int x() { return x; }
                    void setX(int x) { this.x = x; }
                }
            }
            """;

    @DisplayName("no modification + final immutable fields is immutable; a single setter makes it mutable")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT1);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo point = X.findSubType("Point");
        assertTrue(point.findUniqueMethod("translate", 2).isNonModifying());
        Value.Immutable immutablePoint = immutable(point);
        assertTrue(immutablePoint.isImmutable(), "Have " + immutablePoint);

        TypeInfo mutablePoint = X.findSubType("MutablePoint");
        assertTrue(mutablePoint.findUniqueMethod("setX", 1).isModifying());
        assertTrue(immutable(mutablePoint).isMutable(), "a setter makes the type mutable");
    }
}
