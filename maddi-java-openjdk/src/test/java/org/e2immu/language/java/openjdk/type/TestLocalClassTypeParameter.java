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

package org.e2immu.language.java.openjdk.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression (originally surfaced by Guava's {@code com.google.common.reflect.Types}): a local class with its own
 * type parameter ({@code class LocalClass<T> {}}), subclassed anonymously ({@code new LocalClass<String>() {}}).
 * <p>
 * Resolving a use of the type variable {@code T} led the class scanner to look up its owner {@code LocalClass} by
 * fully-qualified name; a local class has no canonical FQN, so the lookup returned {@code null} and the scanner hit
 * {@code assert owner != null} (an {@code AssertionError}, which drops the whole compilation unit). The fix resolves
 * a local-class owner from the element stack (where a named local class is registered by simple name) before
 * asserting.
 */
public class TestLocalClassTypeParameter extends CommonTest {

    private static final String INPUT = """
            package a.b;
            class X {
                Class<?> m() {
                    class LocalClass<T> {
                    }
                    return new LocalClass<String>() {
                    }.getClass();
                }
            }
            """;

    @Test
    public void test() {
        // before the fix, this scan threw AssertionError ("Cannot find owner LocalClass of type variable T")
        TypeInfo x = scan("a.b.X", INPUT);
        assertNotNull(x);
        assertEquals("a.b.X", x.fullyQualifiedName());
        assertNotNull(x.findUniqueMethod("m", 0));
    }
}
