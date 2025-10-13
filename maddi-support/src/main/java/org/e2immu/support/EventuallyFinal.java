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

import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.eventual.Mark;
import org.e2immu.annotation.eventual.Only;
import org.e2immu.annotation.eventual.TestMark;

/**
 * Eventually immutable class, which holds arbitrary values for one field until a final value is written.
 * <p>
 * Note: this class could have been implemented as an extension of {@link Freezable}.
 * <p>
 * This is an example class! Please extend and modify for your needs.
 *
 * @param <T> The type of the value to hold.
 */
@ImmutableContainer(after = "isFinal", hc = true)
public class EventuallyFinal<T> {
    private T value;
    private boolean isFinal;

    /**
     * Get the current value, final or variable.
     *
     * @return the current value.
     */
    public T get() {
        return value;
    }

    /**
     * Write the final value, transition to the <em>after</em> state.
     *
     * @param value the final value
     * @throws IllegalStateException when a final value had been written before.
     */
    @Mark("isFinal")
    public void setFinal(T value) {
        if (this.isFinal) {
            throw new IllegalStateException("Trying to overwrite final value");
        }
        this.isFinal = true;
        this.value = value;
    }

    /**
     * Write a variable value; do not transition but stay in the <em>before</em> state.
     *
     * @param value the variable value
     * @throws IllegalStateException when the object was already in the <em>after</em> state.
     */
    @Only(before = "isFinal")
    public void setVariable(T value) {
        if (this.isFinal) throw new IllegalStateException("Value is already final");
        this.value = value;
    }

    /**
     * Test if the object is in the final or <em>after</em> state.
     *
     * @return <code>true</code> when in the final or <em>after</em> state.
     */
    @TestMark("isFinal")
    public boolean isFinal() {
        return isFinal;
    }

    /**
     * Test if the object is in the variable or <em>before</em> state.
     *
     * @return <code>true</code> when in the variable or <em>before</em> state.
     */
    @TestMark(value = "isFinal", before = true)
    public boolean isVariable() {
        return !isFinal;
    }
}
