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

import org.e2immu.annotation.Final;
import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.eventual.Mark;
import org.e2immu.annotation.eventual.Only;
import org.e2immu.annotation.eventual.TestMark;

/**
 * Super-class for eventually immutable types.
 * The life cycle of the class has two states: an initial one, and a final one.
 * The transition is irrevocable.
 * Freezable classes start in mutable form, and once frozen, become immutable.
 * <p>
 * Methods that make modifications to the content of fields, should call <code>ensureNotFrozen</code>
 * as their first statement.
 * Methods that can only be called when the class is in its immutable state should call
 * <code>ensureFrozen</code> as their first statement.
 * <p>
 * This is an example class! Please extend and modify for your needs.
 */

@ImmutableContainer(after = "frozen", hc = true)
public abstract class Freezable {

    @Final(after = "frozen")
    private volatile boolean frozen;

    /**
     * The method that transitions the object from initial to final state.
     * This method can only be called once on each object.
     *
     * @throws IllegalStateException when the object was already frozen.
     */
    @Mark("frozen")
    public void freeze() {
        ensureNotFrozen();
        frozen = true;
    }

    /**
     * Check if the object is already in the final, frozen state.
     *
     * @return <code>true</code> when the object is in the final, frozen state.
     */
    @TestMark("frozen")
    public boolean isFrozen() {
        return frozen;
    }

    /**
     * A check to ensure that the object is still in the initial, non-frozen state.
     *
     * @throws IllegalStateException when the object is already in the final, frozen state.
     */
    @Only(before = "frozen")
    public void ensureNotFrozen() {
        if (frozen) throw new IllegalStateException("Already frozen!");
    }

    /**
     * A check to ensure that the object is already in the final, frozen state.
     *
     * @throws IllegalStateException when the object is not yet in the final, frozen state.
     */
    @Only(after = "frozen")
    public void ensureFrozen() {
        if (!frozen) throw new IllegalStateException("Not yet frozen!");
    }

}
