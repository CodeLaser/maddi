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

package io.codelaser.maddi.java.openjdk.type;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
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
