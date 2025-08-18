/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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
