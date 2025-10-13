/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
