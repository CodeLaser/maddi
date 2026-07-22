/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, Bart Naudts, https://github.com/CodeLaser/maddi
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

package org.e2immu.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestEither {

    @Test
    public void test1() {
        Either<String, Integer> a = Either.left("Hello");
        assertEquals("Hello", a.getLeft());
        assertEquals("Hello", a.getLeftOrElse("There"));
        assertEquals((Integer)34, a.getRightOrElse(34));
        try {
            a.getRight();
            fail();
        } catch (NullPointerException e) {
            // normal behaviour
        }
        assertTrue(a.isLeft());
        assertFalse(a.isRight());

        Either<String, Integer> b = Either.right(42);
        assertEquals((Integer)42, b.getRight());
        assertEquals("There", b.getLeftOrElse("There"));
        assertEquals((Integer)42, b.getRightOrElse(34));
        try {
            b.getLeft();
            fail();
        } catch (NullPointerException e) {
            // normal behaviour
        }
        assertFalse(b.isLeft());
        assertTrue(b.isRight());

        assertNotEquals(null, b);
        assertNotEquals("string", b);

        assertNotEquals(a, b);
        Either<String, Integer> b2 = Either.right(42);
        assertEquals(b, b2);
        assertEquals(b.hashCode(), b2.hashCode());
    }
}
