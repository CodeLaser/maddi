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
 * Regression (originally surfaced by Guava's {@code com.google.common.collect.HashBiMap}): a named local class
 * (here {@code Entry}) declared inside an anonymous class body, but <em>forward-referenced</em> — {@code get()}
 * does {@code new Entry()} textually before the {@code class Entry} declaration that follows it in the same body.
 * <p>
 * The forward reference resolves the type first, which registers {@code X.$0.Entry} as a subtype of the anonymous
 * class via the lazy-load path. When the scanner then reached the {@code class Entry} declaration, its
 * {@code known} lookup used the local class's (empty/synthetic) canonical name, missed the already-registered
 * subtype, created a second {@code TypeInfo} and threw {@code UnsupportedOperationException: Duplicating type
 * X.$0.Entry}. The fix makes {@code visitClass} reuse a subtype already registered on the enclosing type.
 */
public class TestLocalClassForwardReferenceInAnonymous extends CommonTest {

    // mirrors HashBiMap's entryIterator(): an anonymous subclass of a *class* Base whose method forward-references
    // the local class Entry declared later in the same body (Entry extends a class, as MapEntry extends
    // AbstractMapEntry — so this exercises the duplicate-type registration, not the separate interface path).
    private static final String INPUT = """
            package a.b;
            class X {
                abstract static class Base {
                    abstract Object get();
                }
                Base make() {
                    return new Base() {
                        @Override
                        Object get() {
                            return new Entry();
                        }
                        class Entry {
                        }
                    };
                }
            }
            """;

    @Test
    public void test() {
        // The core guard: before the fix this scan threw "Duplicating type a.b.X.$0.Entry". A duplicate
        // registration is exactly what makes the scan throw, so a scan that completes proves Entry was
        // registered once.
        TypeInfo x = scan("a.b.X", INPUT);
        assertNotNull(x);
        assertEquals("a.b.X", x.fullyQualifiedName());
        assertNotNull(x.findUniqueMethod("make", 0));

        // and the forward-referenced local class Entry exists exactly once (a duplicate is what used to throw)
        long entryCount = infoByFqn.typesLoadedForThisSourceSet().stream()
                .filter(ti -> "Entry".equals(ti.simpleName()))
                .count();
        assertEquals(1, entryCount, "expected the forward-referenced local class Entry to be registered exactly once");
    }
}
