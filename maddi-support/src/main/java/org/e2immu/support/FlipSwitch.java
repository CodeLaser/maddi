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

/**
 * Most simple example of an eventually level 2 immutable type:
 * the object's state marker is the content of the object.
 * The only modification one can make to the object is to transition its state.
 * <p>
 * This is an example class! Please extend and modify for your needs.
 */
@ImmutableContainer(after = "isSet")
public final class FlipSwitch {

    @Final(after = "isSet")
    private volatile boolean isSet;

    /**
     * Transition the state from <em>before</em> to <em>after</em>.
     *
     * @throws IllegalStateException when the object was already in the <em>after</em> state
     */
    @Mark("isSet")
    public void set() {
        synchronized (this) {
            if (isSet) {
                throw new IllegalStateException("Already set");
            }
            isSet = true;
        }
    }

    /**
     * Test if the object is already in the <em>after</em> state.
     *
     * @return <code>true</code> when the object is in the <em>after</em> or final state.
     */
    @NotModified
    @TestMark("isSet")
    public boolean isSet() {
        return isSet;
    }

    /**
     * Copy the state of another object
     *
     * @param other the other object
     * @throws IllegalStateException if the object was already in <em>after</em> or final state
     */
    @Mark("isSet") // but conditionally
    @Modified
    public void copy(FlipSwitch other) {
        if (other.isSet()) set();
    }

    /**
     * A string representation of the object
     *
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        return "FlipSwitch{" + isSet + '}';
    }
}
