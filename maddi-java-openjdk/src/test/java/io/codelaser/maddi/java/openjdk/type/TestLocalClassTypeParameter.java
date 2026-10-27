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
