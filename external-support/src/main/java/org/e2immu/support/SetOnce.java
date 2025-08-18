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

import org.e2immu.annotation.*;
import org.e2immu.annotation.eventual.Mark;
import org.e2immu.annotation.eventual.Only;
import org.e2immu.annotation.eventual.TestMark;

import java.util.Objects;

/**
 * Simple example eventually level 2 immutable type which can hold a single value.
 * This value is either not yet set, or set and immutable.
 * <p>
 * This is an example class! Please extend and modify for your needs.
 *
 * @param <T> The value's type.
 */
@ImmutableContainer(after = "t", hc = true)
public class SetOnce<T> {

    @Final(after = "t")
    @Nullable // eventually not-null, not implemented yet
    private volatile T t;

    /**
     * Set a value. You can do this only once per object.
     *
     * @param t the value to set, not null.
     * @throws IllegalStateException if a value had been set before
     * @throws NullPointerException  if the parameter is null
     */
    @Mark("t")
    @Modified
    public void set(@NotNull T t) {
        if (t == null) throw new NullPointerException("Null not allowed");
        synchronized (this) {
            if (this.t != null) {
                throw new IllegalStateException("Already set: have " + this.t + ", try to set " + t);
            }
            this.t = t;
        }
    }

    /**
     * Obtain the value, but only if it has been set before.
     *
     * @return The value, never null.
     * @throws IllegalStateException if the value had not been set before.
     */
    @Only(after = "t")
    @NotNull
    @NotModified
    public T get() {
        if (t == null) {
            throw new IllegalStateException("Not yet set");
        }
        return t;
    }

    /**
     * Obtain the value, but only if it has been set before.
     * More informative version.
     *
     * @param message a message to show in the exception
     * @return The value, never null.
     * @throws IllegalStateException if the value had not been set before.
     */
    @Only(after = "t")
    @NotNull
    @NotModified
    public T get(String message) {
        if (t == null) {
            throw new IllegalStateException("Not yet set: " + message);
        }
        return t;
    }

    /**
     * More flexible <code>get</code> method. Returns null when the value has not yet been set.
     *
     * @return the value, or null.
     */
    @NotModified
    @Nullable
    public T getOrDefaultNull() {
        if (isSet()) return get();
        return null;
    }

    /**
     * More flexible <code>get</code> method. Returns an alternative value when the value has not yet been set.
     *
     * @param alternative the alternative value, not null
     * @return the value, or the alternative.
     * @throws NullPointerException when the alternative is null
     */
    @NotModified
    @NotNull
    public T getOrDefault(@NotNull T alternative) {
        if (isSet()) return get();
        return Objects.requireNonNull(alternative);
    }

    /**
     * Test if a value has been set.
     *
     * @return <code>true</code> if a value has been set.
     */
    @NotModified
    @TestMark("t")
    public boolean isSet() {
        return t != null;
    }

    /**
     * Copy the value of another <code>SetOnce</code> object.
     *
     * @param other the object whose value will be copied, if set.
     */
    @Modified
    @Mark("t")
    public void copy(@NotNull @NotModified SetOnce<T> other) {
        if (other.isSet()) set(other.get());
    }

    /**
     * Simple toString.
     *
     * @return a string representation of the <code>SetOnce</code> object.
     */
    @Override
    public String toString() {
        return "SetOnce{t=" + t + '}';
    }

    /**
     * Standard equals method, allows null.
     *
     * @param o the object to compare
     * @return equals
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SetOnce<?> setOnce = (SetOnce<?>) o;
        return Objects.equals(t, setOnce.t);
    }

    /**
     * The hashCode
     *
     * @return the hashCode of the value
     */
    @Override
    public int hashCode() {
        return Objects.hash(t);
    }
}
