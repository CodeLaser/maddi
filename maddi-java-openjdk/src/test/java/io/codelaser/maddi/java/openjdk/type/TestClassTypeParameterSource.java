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

package io.codelaser.maddi.java.openjdk.type;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A CLASS type parameter used to lose its declaration source, and get a wildcard-widened bound, when the type
 * referred to its own nested type <em>before</em> declaring it.
 * <p>
 * The shape is guava's {@code AbstractIterator}, and it needs nothing exotic — one file, one source set, no
 * class files, no scan-order dependence:
 * <pre>
 *     private State state = State.NOT_READY;      // State is not registered yet
 *     private enum State { ... }                  // ... it is declared here
 * </pre>
 * Resolving {@code State} at the field finds no registered subtype, so it is loaded from its symbol; loading a
 * nested type lazily loads its <b>enclosing</b> type ("so that we can compute access as soon as possible"), and
 * that enclosing type is the one this scan is in the middle of building. {@code loadType} returns early only on
 * {@code hasBeenInspected()}, which is false until the whole compilation unit is committed — so its setup block
 * ran, and {@code addOrSetTypeParameter} REPLACES by index, so the symbol-built type parameters silently won
 * over the ones the declaration had just produced.
 * <p>
 * The mirror of {@code TestMethodTypeParameterSource}, where the symbol view came first and could not be
 * replaced. Both are the same root cause: the symbol scanner writing type parameters onto a type the source
 * scan owns. See {@code docs/method-type-parameter-source-loss.md}.
 * <p>
 * Measured before the guard: 37 of guava's 698 generic types, 74 of timefold-solver's 1469 — every one of them
 * a type with a nested type.
 */
public class TestClassTypeParameterSource extends CommonTest {

    @Language("java")
    private static final String NESTED_AFTER_USE = """
            package a;
            public class A<T extends Comparable<T>> {
                private State state = State.NOT_READY;
                private T t;
                public T get() { return t; }
                private enum State { NOT_READY, READY }
            }
            """;

    @Language("java")
    private static final String NESTED_BEFORE_USE = """
            package a;
            public class A<T extends Comparable<T>> {
                private enum State { NOT_READY, READY }
                private State state = State.NOT_READY;
                private T t;
                public T get() { return t; }
            }
            """;

    @DisplayName("the nested type declared BEFORE it is used: the type parameter is the declaration's")
    @Test
    public void testNestedDeclaredBeforeUse() {
        assertDeclarationWins(scan("a.A", NESTED_BEFORE_USE));
    }

    @DisplayName("the nested type declared AFTER it is used: identical, and it was not")
    @Test
    public void testNestedDeclaredAfterUse() {
        // Identical to the test above, which is the point: where a nested type is declared cannot decide what
        // its enclosing type's type parameters are. Before the fix this was `T=NULL` with the bound
        // `? extends Comparable<T>`, the symbol path's form.
        assertDeclarationWins(scan("a.A", NESTED_AFTER_USE));
    }

    private static void assertDeclarationWins(TypeInfo a) {
        assertEquals(1, a.typeParameters().size());
        TypeParameter t = a.typeParameters().getFirst();
        assertNotNull(t.source(), "the declaration's own source");
        assertEquals("2-16:2-38", t.source().compact2());
        assertEquals("[Type Comparable<T extends Comparable<T>>]", t.typeBounds().toString());
    }
}
