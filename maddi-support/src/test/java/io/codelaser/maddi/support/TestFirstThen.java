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

public class TestFirstThen {

    @Test
    public void test1() {
        FirstThen<String, Integer> a = new FirstThen<>("Hello");
        assertEquals("Hello", a.getFirst());
        try {
            a.get();
            fail();
        } catch (IllegalStateException e) {
            // normal behaviour
        }
        assertTrue(a.isFirst());
        assertFalse(a.isSet());
        a.set(34);
        assertEquals((Integer) 34, a.get());
        try {
            a.getFirst();
            fail();
        } catch (IllegalStateException e) {
            // normal behaviour
        }
        assertFalse(a.isFirst());
        assertTrue(a.isSet());
        try {
            a.set(42);
            fail();
        } catch (IllegalStateException e) {
            // normal behaviour
        }
        assertNotEquals(null, a);
        FirstThen<String, Integer> b = new FirstThen<>("Hello");
        assertNotEquals(b, a);
        b.set(34);
        assertEquals(b, a);
    }

    @Test
    public void test2() {
        FirstThen<Integer, String> a = FirstThen.then("a");
        assertTrue(a.isSet());
        assertFalse(a.isFirst());
        assertEquals("a", a.get());
    }
}
