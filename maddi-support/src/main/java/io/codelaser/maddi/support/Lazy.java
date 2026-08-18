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

import io.codelaser.maddi.annotation.*;
import io.codelaser.maddi.annotation.eventual.Mark;
import io.codelaser.maddi.annotation.eventual.TestMark;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Implementation of a lazy value, where <code>null</code> is used to indicate that the value has not been
 * evaluated yet.
 * <p>
 * The Java half of an idiom Kotlin has in its standard library, and deliberately shaped like it: the
 * supplier is dropped once the value exists. It stops short of Kotlin's contract in two places, both on
 * purpose — a null value is rejected rather than permitted (Kotlin allows {@code Lazy<String?>}), and the
 * supplier may run more than once under contention. {@code TestKotlinLazyVsJavaLazy} in
 * {@code maddi-run-kotlin} analyses the two side by side and records what maddi makes of each.
 * <p>
 * This is an example class! Please extend and modify for your needs.
 *
 * @param <T> the container's content type
 */

@ImmutableContainer(after = "t", hc = true)
public class Lazy<T> {
    /**
     * Dropped at the transition, so that a lazy value does not keep its initializer — and everything the
     * initializer captured — alive for as long as the value itself. That is why Kotlin's three
     * {@code Lazy} implementations all do {@code initializer = null}, and it is what
     * <em>The Road to Immutability</em> §12.6 describes.
     * <p>
     * Not final, therefore, and read exactly once per call into a local. Reading the field twice is a real
     * race and not a theoretical one: two threads find {@code t == null}, the first completes and nulls this
     * field, the second dereferences what it re-reads as {@code null}. See {@link #get()}.
     */
    @Final(after = "t")
    private volatile Supplier<T> supplier;

    @Final(after = "t")
    private volatile T t;

    /**
     * Construct the lazy object by storing a supplier.
     *
     * @param supplierParam the supplier that will compute the value; it should not produce a null value
     * @throws NullPointerException when the argument is <code>null</code>
     */
    public Lazy(Supplier<T> supplierParam) {
        if (supplierParam == null) throw new NullPointerException("Null not allowed");
        this.supplier = supplierParam;
    }

    /**
     * Obtain the value, either by evaluation, if this is the first call, or from the cached field.
     * <p>
     * {@code supplier} is read once, into a local, and the value is published <em>before</em> the supplier is
     * dropped. Both matter, and in that order: a reader that observes {@code supplier == null} has therefore
     * also observed {@code t != null}, so neither {@code return t} below can hand back {@code null}. Re-reading
     * {@code t} is harmless in a way that re-reading {@code supplier} is not — it can only have been
     * overwritten with another valid value, never with {@code null}.
     * <p>
     * Concurrent first calls may still each evaluate the supplier, and callers may then receive different
     * instances; the last write wins and every later call agrees with it. That is unchanged from the version
     * that never dropped the supplier, and it is where Kotlin's default {@code lazy {}} differs — it
     * synchronizes, so the supplier runs once. Extend this class if you need that guarantee.
     * <p>
     * ⛔ The last statement is {@code return t} and not {@code return value}, and that is not a style choice:
     * writing the local instead makes today's analyzer conclude the return is fully {@code @Independent},
     * one level above the honest {@code @Independent(hc=true)} the other four shapes get. The two say the same
     * thing — {@code t} holds exactly what {@code value} holds — so one of the two verdicts is wrong, and it
     * is the stronger one: {@code get()} hands the caller the object the field holds. Measured in
     * {@code TestLazyShapeIndependence}, which pins the discrepancy so that it fails when the analyzer improves.
     *
     * @return the value
     * @throws NullPointerException if the evaluation returns <code>null</code>
     */
    @NotNull
    @Modified
    @Independent(hc = true)
    @Mark(value = "t")
    public T get() {
        T value = t;
        if (value != null) return value;
        Supplier<T> localSupplier = supplier;
        // another thread finished the transition between the two reads above; it published t first
        if (localSupplier == null) return t;
        t = Objects.requireNonNull(localSupplier.get()); // publish...
        supplier = null;                                 // ...then drop, never the other way round
        return t;
    }

    /**
     * @return true when the lazy object has been evaluated
     */
    @NotModified
    @TestMark("t")
    public boolean hasBeenEvaluated() {
        return t != null;
    }

}
