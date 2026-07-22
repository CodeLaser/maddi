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

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TestLazy {

    @Test
    public void test1() {
        AtomicInteger counter = new AtomicInteger();
        Lazy<String> lazy = new Lazy<>(() -> {
            counter.getAndIncrement();
            return "abc";
        });
        assertFalse(lazy.hasBeenEvaluated());

        String content = lazy.get();
        assertEquals("abc", content);
        assertEquals(1, counter.get());
        assertTrue(lazy.hasBeenEvaluated());

        // 2nd evaluation
        content = lazy.get();
        assertEquals("abc", content);
        assertEquals(1, counter.get());
        assertTrue(lazy.hasBeenEvaluated());
    }

    @Test
    public void test2() {
        try {
            new Lazy<String>(null);
            fail();
        } catch (NullPointerException e) {
            // normal behaviour
        }
    }

    @Test
    public void test3() {
        Lazy<String> lazy = new Lazy<>(() -> null);
        try {
            lazy.get();
            fail();
        } catch (NullPointerException e) {
            // normal behaviour
        }
    }
}
