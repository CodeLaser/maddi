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

package io.codelaser.maddi.aapi.archive.jdk;
import java.util.function.*;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;

public class JavaUtilConcurrentAtomic {
    public static final String PACKAGE_NAME = "java.util.concurrent.atomic";
    //public class AtomicBoolean implements Serializable
    // mutable holder of a boolean; no method modifies a parameter's content
    @Container
    @Independent
    class AtomicBoolean$ {
        @NotModified boolean get() { return false; }
        @Modified boolean compareAndSet(boolean expectedValue, boolean newValue) { return false; }
        @Modified boolean weakCompareAndSet(boolean expectedValue, boolean newValue) { return false; }
        @Modified boolean weakCompareAndSetPlain(boolean expectedValue, boolean newValue) { return false; }
        //frequency 2
        @Modified void set(boolean newValue) { }
        @Modified void lazySet(boolean newValue) { }
        @Modified boolean getAndSet(boolean newValue) { return false; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        @NotModified boolean getPlain() { return false; }
        @Modified void setPlain(boolean newValue) { }
        @NotModified boolean getOpaque() { return false; }
        @Modified void setOpaque(boolean newValue) { }
        @NotModified boolean getAcquire() { return false; }
        @Modified void setRelease(boolean newValue) { }
        @Modified boolean compareAndExchange(boolean expectedValue, boolean newValue) { return false; }
        @Modified boolean compareAndExchangeAcquire(boolean expectedValue, boolean newValue) { return false; }
        @Modified boolean compareAndExchangeRelease(boolean expectedValue, boolean newValue) { return false; }
        @Modified boolean weakCompareAndSetVolatile(boolean expectedValue, boolean newValue) { return false; }
        @Modified boolean weakCompareAndSetAcquire(boolean expectedValue, boolean newValue) { return false; }
        @Modified boolean weakCompareAndSetRelease(boolean expectedValue, boolean newValue) { return false; }
    }

    //public class AtomicInteger extends Number implements Serializable
    // mutable holder of an int; no method modifies a parameter's content
    @Container
    @Independent
    class AtomicInteger$ {
        @NotModified int get() { return 0; }
        @Modified void set(int newValue) { }
        @Modified void lazySet(int newValue) { }
        @Modified int getAndSet(int newValue) { return 0; }
        @Modified boolean compareAndSet(int expectedValue, int newValue) { return false; }
        @Modified boolean weakCompareAndSet(int expectedValue, int newValue) { return false; }
        @Modified boolean weakCompareAndSetPlain(int expectedValue, int newValue) { return false; }
        @Modified int getAndIncrement() { return 0; }
        @Modified int getAndDecrement() { return 0; }
        @Modified int getAndAdd(int delta) { return 0; }
        //frequency 1
        @Modified int incrementAndGet() { return 0; }
        @Modified int decrementAndGet() { return 0; }
        //frequency 1
        @Modified int addAndGet(int delta) { return 0; }
        @Modified int getAndUpdate(/*@IgnoreModifications[T]*/ IntUnaryOperator updateFunction) { return 0; }
        @Modified int updateAndGet(/*@IgnoreModifications[T]*/ IntUnaryOperator updateFunction) { return 0; }
        @Modified int getAndAccumulate(int x, /*@IgnoreModifications[T]*/ IntBinaryOperator accumulatorFunction) { return 0; }
        @Modified int accumulateAndGet(int x, /*@IgnoreModifications[T]*/ IntBinaryOperator accumulatorFunction) { return 0; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }

        //override from java.lang.Number
        //@NotModified[H]
        @NotModified int intValue() { return 0; }

        //override from java.lang.Number
        //@NotModified[H]
        @NotModified long longValue() { return 0L; }

        //override from java.lang.Number
        //@NotModified[H]
        @NotModified float floatValue() { return 0.0F; }

        //override from java.lang.Number
        //@NotModified[H]
        @NotModified double doubleValue() { return 0.0; }
        @NotModified int getPlain() { return 0; }
        @Modified void setPlain(int newValue) { }
        @NotModified int getOpaque() { return 0; }
        @Modified void setOpaque(int newValue) { }
        @NotModified int getAcquire() { return 0; }
        @Modified void setRelease(int newValue) { }
        @Modified int compareAndExchange(int expectedValue, int newValue) { return 0; }
        @Modified int compareAndExchangeAcquire(int expectedValue, int newValue) { return 0; }
        @Modified int compareAndExchangeRelease(int expectedValue, int newValue) { return 0; }
        @Modified boolean weakCompareAndSetVolatile(int expectedValue, int newValue) { return false; }
        @Modified boolean weakCompareAndSetAcquire(int expectedValue, int newValue) { return false; }
        @Modified boolean weakCompareAndSetRelease(int expectedValue, int newValue) { return false; }
    }

    //public class AtomicReference implements Serializable
    // mutable holder of one hidden-content element V; transfers V in/out but never modifies it
    @Container
    @Independent(hc = true)
    class AtomicReference$<V> {
        //@Independent(hc=true)[T]
        @NotModified V get() { return null; }
        @Modified void set(/*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { }
        @Modified void lazySet(/*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { }
        @Modified boolean compareAndSet(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return false; }

        @Modified boolean weakCompareAndSet(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return false; }

        @Modified boolean weakCompareAndSetPlain(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return false; }

        //@Independent(hc=true)[T]
        @Modified V getAndSet(/*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return null; }

        //@Independent(hc=true)[T]
        @Modified V getAndUpdate(/*@IgnoreModifications[T]*/ @NotNull UnaryOperator<V> updateFunction) { return null; }

        //@Independent(hc=true)[T]
        @Modified V updateAndGet(/*@IgnoreModifications[T]*/ @NotNull UnaryOperator<V> updateFunction) { return null; }

        //@Independent(hc=true)[T]
        @Modified V getAndAccumulate(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V x,
            /*@IgnoreModifications[T]*/ @NotNull BinaryOperator<V> accumulatorFunction) { return null; }

        //@Independent(hc=true)[T]
        @Modified V accumulateAndGet(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V x,
            /*@IgnoreModifications[T]*/ @NotNull BinaryOperator<V> accumulatorFunction) { return null; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }

        //@Independent(hc=true)[T]
        @NotModified V getPlain() { return null; }
        @Modified void setPlain(/*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { }
        //@Independent(hc=true)[T]
        @NotModified V getOpaque() { return null; }
        @Modified void setOpaque(/*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { }
        //@Independent(hc=true)[T]
        @NotModified V getAcquire() { return null; }
        @Modified void setRelease(/*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { }
        //@Independent(hc=true)[T]
        @Modified V compareAndExchange(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return null; }

        //@Independent(hc=true)[T]
        @Modified V compareAndExchangeAcquire(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return null; }

        //@Independent(hc=true)[T]
        @Modified V compareAndExchangeRelease(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return null; }

        @Modified boolean weakCompareAndSetVolatile(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return false; }

        @Modified boolean weakCompareAndSetAcquire(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return false; }

        @Modified boolean weakCompareAndSetRelease(
            /*@Independent(hc=true)[T] @NotModified[T]*/ V expectedValue,
            /*@Independent(hc=true)[T] @NotModified[T]*/ V newValue) { return false; }
    }

    //public java.util.concurrent.atomic.AtomicReferenceArray (shadow created for its nullability, generated)
    class AtomicReferenceArray$<E> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        AtomicReferenceArray$(@NotNull E[] p0) { }
        @Modified E accumulateAndGet(int p0, E p1, @NotNull java.util.function.BinaryOperator<E> p2) { return null; }
        @Modified E getAndAccumulate(int p0, E p1, @NotNull java.util.function.BinaryOperator<E> p2) { return null; }
        @Modified E getAndUpdate(int p0, @NotNull java.util.function.UnaryOperator<E> p1) { return null; }
        @Modified E updateAndGet(int p0, @NotNull java.util.function.UnaryOperator<E> p1) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified E compareAndExchange(int p0, E p1, E p2) { return null; }
        @Modified E compareAndExchangeAcquire(int p0, E p1, E p2) { return null; }
        @Modified E compareAndExchangeRelease(int p0, E p1, E p2) { return null; }
        @Modified boolean compareAndSet(int p0, E p1, E p2) { return false; }
        @Modified E getAndSet(int p0, E p1) { return null; }
        @Modified void lazySet(int p0, E p1) { }
        @Modified void set(int p0, E p1) { }
        @Modified void setOpaque(int p0, E p1) { }
        @Modified void setPlain(int p0, E p1) { }
        @Modified void setRelease(int p0, E p1) { }
        @Modified boolean weakCompareAndSet(int p0, E p1, E p2) { return false; }
        @Modified boolean weakCompareAndSetAcquire(int p0, E p1, E p2) { return false; }
        @Modified boolean weakCompareAndSetPlain(int p0, E p1, E p2) { return false; }
        @Modified boolean weakCompareAndSetRelease(int p0, E p1, E p2) { return false; }
        @Modified boolean weakCompareAndSetVolatile(int p0, E p1, E p2) { return false; }
    }

    //public java.util.concurrent.atomic.AtomicReferenceFieldUpdater (shadow created for its nullability, generated)
    class AtomicReferenceFieldUpdater$<T, V> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified V accumulateAndGet(T p0, V p1, @NotNull java.util.function.BinaryOperator<V> p2) { return null; }
        @Modified V getAndAccumulate(T p0, V p1, @NotNull java.util.function.BinaryOperator<V> p2) { return null; }
        @Modified V getAndUpdate(T p0, @NotNull java.util.function.UnaryOperator<V> p1) { return null; }
        static <U, W> java.util.concurrent.atomic.AtomicReferenceFieldUpdater<U, W> newUpdater(@NotNull java.lang.Class<U> p0, @NotNull java.lang.Class<W> p1, @NotNull java.lang.String p2) { return null; }
        @Modified V updateAndGet(T p0, @NotNull java.util.function.UnaryOperator<V> p1) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified boolean compareAndSet(T p0, V p1, V p2) { return false; }
        @Modified V getAndSet(T p0, V p1) { return null; }
        @Modified void lazySet(T p0, V p1) { }
        @Modified void set(T p0, V p1) { }
        @Modified boolean weakCompareAndSet(T p0, V p1, V p2) { return false; }
    }
}
