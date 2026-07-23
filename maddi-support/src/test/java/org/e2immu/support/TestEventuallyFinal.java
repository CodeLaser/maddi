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

public class TestEventuallyFinal {

    @Test
    public void test1() {
        EventuallyFinal<String> e = new EventuallyFinal<>();
        assertNull(e.get());
        assertTrue(e.isVariable());
        assertFalse(e.isFinal());
        e.setVariable("abc");
        assertEquals("abc", e.get());
        assertTrue(e.isVariable());
        assertFalse(e.isFinal());
        e.setVariable("xyz");
        assertEquals("xyz", e.get());
        assertTrue(e.isVariable());
        assertFalse(e.isFinal());

        e.setFinal("123");
        assertFalse(e.isVariable());
        assertTrue(e.isFinal());
        assertEquals("123", e.get());

        try {
            // cannot overwrite the same value
            e.setFinal("123");
            fail();
        } catch (RuntimeException r) {
            // OK!
        }

        try {
            e.setFinal("1234");
            fail();
        } catch (RuntimeException r) {
            // OK!
        }
        assertFalse(e.isVariable());
        assertTrue(e.isFinal());
        assertEquals("123", e.get());
    }
}
