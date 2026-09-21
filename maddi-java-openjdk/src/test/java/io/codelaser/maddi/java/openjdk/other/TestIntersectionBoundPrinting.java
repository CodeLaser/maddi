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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.type.WildcardEnum;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⛔⛔ <b>PRINTING AN INTERSECTION TYPE THAT CARRIES A PLAIN {@code EXTENDS} WILDCARD RAISED AN
 * {@link AssertionError} AND TOOK A WHOLE REFACTORING DOWN.</b>
 * <p>
 * Cassandra, design G1, 2026-09-21: a read-only {@code extractInterfaceSuggestion} on {@code db.Keyspace} died
 * with <i>"AssertionError with no message"</i> at {@code ParameterizedTypePrinter.intersectionType}. The call
 * that reached it is not a rendering at all — it is
 * {@code CommonAnalyze.getOrCreateReplaceCandidate}, which uses a type's {@code toString()} AS A MAP KEY. So a
 * bare {@code assert} in a printer is not a printer's business: it is an {@link Error}, it goes past every
 * {@code catch (RuntimeException)} on the way out, and the run reports nothing it can act on.
 * <p>
 * ⭐ <b>The shape is one maddi itself creates.</b> {@code ParameterizedTypeImpl} line 652 puts
 * {@code WildcardEnum.EXTENDS} on a bound that is itself a type parameter — deliberately, and documented there.
 * When that bound is an <b>intersection</b> ({@code T extends A & B}), the result has no {@code TypeInfo}, two
 * parameters and an {@code EXTENDS} wildcard: {@code isIntersectionType()} is true, and the printer's
 * {@code assert wildcard().isExtendsIntersection()} is false. Java can only write
 * {@code ? extends A & B} — {@code EXTENDS_INTERSECTION} — so the assert describes the SOURCE language, while
 * the model reaches this state from its own bound handling.
 * <p>
 * ⇒ The printer prints what it is given, for every wildcard kind. {@code toString()} must not throw.
 */
public class TestIntersectionBoundPrinting extends CommonTest {

    @Language("java")
    private static final String TYPES = """
            package a.b;
            public interface A {
            }
            """;

    @Language("java")
    private static final String B = """
            package a.b;
            public interface B {
            }
            """;

    @Language("java")
    private static final String PAIR = """
            package a.b;
            public class Pair<T extends A & B> {
                public T first;
            }
            """;

    /** the model's own intersection: what {@code newIntersectionType} builds for {@code A & B} */
    private ParameterizedType intersection() {
        Map<String, TypeInfo> types = scan(false, "a.b.A", TYPES, "a.b.B", B, "a.b.Pair", PAIR);
        ParameterizedType a = runtime.newParameterizedType(types.get("a.b.A"), 0);
        ParameterizedType b = runtime.newParameterizedType(types.get("a.b.B"), 0);
        ParameterizedType intersection = runtime.newIntersectionType(null, java.util.List.of(a, b));
        assertTrue(intersection.isIntersectionType(), "fixture must be an intersection: " + intersection);
        return intersection;
    }

    @DisplayName("⭐ an intersection bound with a plain EXTENDS wildcard prints instead of raising")
    @Test
    public void plainExtendsOnAnIntersectionPrints() {
        // exactly what ParameterizedTypeImpl:652 builds when a bound is itself a type parameter
        ParameterizedType withExtends = intersection().withWildcard(WildcardEnum.EXTENDS);

        assertEquals("? extends a.b.A&a.b.B", withExtends.fullyQualifiedName());
    }

    @DisplayName("and so does a SUPER wildcard, which no Java source can write")
    @Test
    public void superOnAnIntersectionPrints() {
        ParameterizedType withSuper = intersection().withWildcard(WildcardEnum.SUPER);

        assertEquals("? super a.b.A&a.b.B", withSuper.fullyQualifiedName());
    }

    @DisplayName("the shape Java CAN write is unchanged")
    @Test
    public void extendsIntersectionIsUnchanged() {
        ParameterizedType asWritten = intersection().withWildcard(WildcardEnum.EXTENDS_INTERSECTION);

        assertEquals("? extends a.b.A&a.b.B", asWritten.fullyQualifiedName());
    }

    @DisplayName("and an intersection with no wildcard at all keeps its bare form")
    @Test
    public void noWildcard() {
        assertEquals("a.b.A&a.b.B", intersection().withWildcard(null).fullyQualifiedName());
    }
}
