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

import org.e2immu.annotation.*;
import org.e2immu.annotation.eventual.Mark;
import org.e2immu.annotation.eventual.TestMark;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Implementation of a lazy value, where <code>null</code> is used to indicate that the value has not been
 * evaluated yet.
 * <p>
 * This is an example class! Please extend and modify for your needs.
 *
 * @param <T> the container's content type
 */

@ImmutableContainer(after = "t", hc = true)
public class Lazy<T> {
    private final Supplier<T> supplier;

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
     *
     * @return the value
     * @throws NullPointerException if the evaluation returns <code>null</code>
     */
    @NotNull
    @Modified
    @Mark(value = "t")
    public T get() {
        if (t != null) return t;
        t = Objects.requireNonNull(supplier.get()); // this statement causes @NotNull1 and @Independent on supplier
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
