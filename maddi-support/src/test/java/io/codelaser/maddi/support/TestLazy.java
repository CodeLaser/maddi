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

package io.codelaser.maddi.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
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

    /**
     * The supplier is dropped once the value exists — the reason the field is not final, and the whole
     * point of the alignment with Kotlin's {@code Lazy}. Read reflectively because there is no other way to
     * observe a release: a weak reference plus {@code System.gc()} tests the collector, not this class.
     */
    @Test
    public void test4() throws ReflectiveOperationException {
        Lazy<String> lazy = new Lazy<>(() -> "abc");
        Field supplier = Lazy.class.getDeclaredField("supplier");
        supplier.setAccessible(true);
        assertNotNull(supplier.get(lazy));

        assertEquals("abc", lazy.get());
        assertNull(supplier.get(lazy), "the supplier should have been dropped at the transition");
        // and the value survives its supplier
        assertEquals("abc", lazy.get());
        assertTrue(lazy.hasBeenEvaluated());
    }

    /**
     * A failed evaluation is not a transition: the supplier stays, so a later call can succeed. Kotlin does
     * the same — it nulls the initializer only after the value is published.
     */
    @Test
    public void test5() {
        AtomicInteger counter = new AtomicInteger();
        Lazy<String> lazy = new Lazy<>(() -> counter.getAndIncrement() == 0 ? null : "abc");
        assertThrows(NullPointerException.class, lazy::get);
        assertFalse(lazy.hasBeenEvaluated());

        assertEquals("abc", lazy.get());
        assertEquals(2, counter.get());
    }
}
