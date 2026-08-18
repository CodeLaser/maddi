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

import io.codelaser.maddi.annotation.rare.IgnoreModifications;

import java.util.function.IntSupplier;

/**
 * The primitive twin of {@link Memo}: an idempotent lazy {@code int} cache, for the cached-hash idiom.
 * <p>
 * {@code 0} is the unset marker. A computation that genuinely yields {@code 0} is stored as {@code 1}, so
 * it is computed once rather than on every call — without the remap a memo silently stops being a memo for
 * exactly the values that hash to zero. That is the refinement every hand-written cached-hash slot in
 * cst-impl now makes as well. The remap is also why this class exposes no way to read the raw slot: the
 * stored value is the answer, and callers must not reason about the difference.
 * <p>
 * See {@link Memo} for why the class carries {@link IgnoreModifications}, why the slot is volatile, and why
 * the hand-written memo fields that predate it were deliberately left as they are.
 */
@IgnoreModifications(comment = "idempotent lazy cache: writes are observationally invisible (road §050)")
public final class IntMemo {
    private volatile int value;

    /**
     * The cached int, computing and storing it on first call.
     *
     * @param compute must be pure with respect to the outcome
     */
    public int get(IntSupplier compute) {
        int v = value;
        if (v == 0) {
            v = compute.getAsInt();
            if (v == 0) v = 1; // 0 is the unset sentinel: remap, or a genuine 0 recomputes forever
            value = v;
        }
        return v;
    }

    /** Whether the value has been computed; never a reason to branch on the outcome. */
    public boolean isSet() {
        return value != 0;
    }

    @Override
    public String toString() {
        int v = value;
        return v == 0 ? "<not computed>" : Integer.toString(v);
    }
}
