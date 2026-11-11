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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.util.MethodFamilies;
import io.codelaser.maddi.inspection.api.util.MethodUses;
import io.codelaser.maddi.inspection.api.util.MethodUses.Kind;
import io.codelaser.maddi.java.openjdk.CommonTest;

/** {@link MethodUses}: each use once, in the member whose code it is written in. */
public class TestMethodUses extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Function;
            import java.util.function.Supplier;
            class X {
                interface Scorer { int score(String s); }
                static class Point {
                    final int x;
                    Point(int x) { this.x = x; }
                    Point() { this(0); }
                    int twice(int factor) { return x * factor; }
                }
                static class Far extends Point {
                    Far(int x) { super(x + 1); }
                }
                static int length(String s) { return s.length(); }

                final Point origin = new Point(1);
                final int doubled = origin.twice(2);

                int direct(Point p) { return p.twice(3); }
                Supplier<Integer> inLambda(Point p) { return () -> p.twice(4); }
                Runnable inAnonymous(Point p) {
                    return new Runnable() { public void run() { p.twice(5); } };
                }
                Scorer byLambda() { return s -> s.length(); }
                Scorer byReference() { return X::length; }
                Function<Integer, Point> creating() { return Point::new; }
                Point anonymousSubclass() { return new Point(6) { }; }
            }
            """;

    private static String describe(List<MethodUses.Use> uses) {
        return uses.stream().map(u -> u.kind() + " in " + u.in().typeInfo().simpleName() + "." + u.in().simpleName()
                                      + (u.arguments().isEmpty() ? "" : " " + u.arguments()))
                .collect(Collectors.joining("; "));
    }

    @Test
    public void uses() {
        TypeInfo X = scan("a.b.X", INPUT);
        MethodUses uses = MethodUses.of(MethodFamilies.allTypes(Set.of(X)));
        TypeInfo point = X.findSubType("Point");
        MethodInfo twice = point.findUniqueMethod("twice", 1);

        // a method, a lambda's body in its method, a field's initializer, an anonymous class's method
        assertEquals("CALL in X.direct [3]; CALL in X.inLambda [4]; CALL in X.doubled [2]; CALL in $1.run [5]",
                describe(uses.uses(twice)));

        MethodInfo ofInt = point.findConstructor(1);
        // the anonymous subclass's creation calls Point(int): the only constructor of one argument
        assertEquals("REFERENCE in X.creating; ANONYMOUS_CREATION in X.anonymousSubclass [6]; "
                     + "CREATION in X.origin [1]; CONSTRUCTOR_INVOCATION in Far.<init> [x+1]; "
                     + "CONSTRUCTOR_INVOCATION in Point.<init> [0]",
                describe(uses.uses(ofInt)));
        assertEquals("", describe(uses.uses(point.findConstructor(0))));

        MethodInfo length = X.findUniqueMethod("length", 1);
        assertEquals("REFERENCE in X.byReference", describe(uses.uses(length)));

        MethodInfo score = X.findSubType("Scorer").findUniqueMethod("score", 1);
        assertEquals("IMPLEMENTATION in X.byLambda; IMPLEMENTATION in X.byReference", describe(uses.uses(score)));
        assertEquals("IMPLEMENTATION in X.byReference", describe(uses.uses(score).subList(1, 2)));
        assertEquals(1, uses.uses(score, Kind.IMPLEMENTATION).stream()
                .filter(u -> u.element() instanceof io.codelaser.maddi.cst.api.expression.Lambda).count());

        assertEquals("X.length, Point.<init>", uses.methods(Kind.REFERENCE).stream()
                .map(m -> m.typeInfo().simpleName() + "." + m.name()).collect(Collectors.joining(", ")));
        // the program's Scorer. Not the JDK's Supplier and Function, loaded lazily here: such a type has no
        // singleAbstractMethod() (CodeLaser/maddi#193)
        assertEquals("Scorer.score", uses.methods(Kind.IMPLEMENTATION).stream()
                .map(m -> m.typeInfo().simpleName() + "." + m.name()).collect(Collectors.joining(", ")));
    }
}
