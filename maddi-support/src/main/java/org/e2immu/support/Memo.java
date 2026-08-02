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

import org.e2immu.annotation.rare.IgnoreModifications;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An idempotent lazy cache: a slot that is computed on first read and never observably changes after.
 * <p>
 * The slot is <b>manual hidden content</b> (road-to-immutability §050). Any two writers write equal
 * values — the supplier must be pure with respect to the outcome — so the mutation is observationally
 * invisible, and a field of this type does not make its holder modifying. The class-level
 * {@link IgnoreModifications} says exactly that, once, instead of the annotation being repeated on
 * every memo field; the analyzer treats a field whose type carries the class-level disclaimer as if the
 * field itself were annotated.
 * <p>
 * {@code volatile} because the CST is read concurrently — the analyzer's type loop is parallel. The
 * hand-written memo fields that predate this class (e.g. {@code VariableImpl.cachedFqn}) rely on a
 * benign reference race instead; new code should not.
 * <p>
 * {@code null} is the unset marker, so a memo cannot cache {@code null}.
 *
 * @param <T> the cached type
 */
@IgnoreModifications(comment = "idempotent lazy cache: writes are observationally invisible (road §050)")
public final class Memo<T> {
    private volatile T value;

    /**
     * The cached value, computing and storing it on first call.
     *
     * @param compute must be pure with respect to the outcome, and must not return null
     */
    public T get(Supplier<? extends T> compute) {
        T v = value;
        if (v == null) {
            v = Objects.requireNonNull(compute.get(), "A Memo cannot cache null");
            value = v;
        }
        return v;
    }

    /** Whether the value has been computed; never a reason to branch on the outcome. */
    public boolean isSet() {
        return value != null;
    }

    /** The cached value, or null when not yet computed. For diagnostics and printing, not for logic. */
    public T getOrNull() {
        return value;
    }

    @Override
    public String toString() {
        T v = value;
        return v == null ? "<not computed>" : v.toString();
    }
}
