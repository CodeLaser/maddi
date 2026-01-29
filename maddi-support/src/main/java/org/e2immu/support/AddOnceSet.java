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
import org.e2immu.annotation.eventual.Only;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Example of a freezable set, which disallows both removal and attempts to write an object a second time.
 * Implemented as a map, so you can retrieve the exact object you put in using the <code>get</code> method.
 * Elements must not be null.
 * <p>
 * The type is eventually level 2 immutable since its only field <code>set</code> is explicitly final,
 * after freezing, it cannot be modified anymore, and it is independent because the only way of obtaining
 * the whole set is via a {@link Stream}, or, when in Java 10 or higher, via a level 2 immutable copy.
 * <p>
 * This is an example class! Please extend and modify for your needs.
 *
 * @param <V> The type of elements held by the set.
 */
@ImmutableContainer(after = "frozen", hc = true)
public class AddOnceSet<V> extends Freezable implements Set<V> {

    private final Map<V, V> set = new HashMap<>();

    /**
     * Add an element to the set.
     *
     * @param v The element to be added.
     * @return true
     * @throws IllegalStateException when the element had been added before, or when the set was already frozen.
     * @throws NullPointerException  when the parameter is null.
     */
    @Only(before = "frozen")
    public boolean add(@NotNull V v) {
        Objects.requireNonNull(v);
        ensureNotFrozen();
        if (contains(v)) throw new IllegalStateException("Already decided on " + v);
        set.put(v, v);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return c.stream().allMatch(this::contains);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends V> c) {
        c.forEach(this::add);
        return true;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Obtain the exact element that was added to the set.
     *
     * @param v the element to look up.
     * @return An element, possibly <code>v</code> but definitely equal to <code>v</code>.
     * @throws IllegalStateException when the element is not yet present in the set.
     */
    @NotNull
    @NotModified
    public V get(@NotNull V v) {
        if (!contains(v)) throw new IllegalStateException("Not yet decided on " + v);
        return Objects.requireNonNull(set.get(v)); // causes potential null pointer exception warning; that's OK
    }

    /**
     * Check if the element is present in the set.
     *
     * @param v the element, not null.
     * @return <code>true</code> when the element is present in the set.
     */
    @NotModified
    public boolean contains(@NotNull Object v) {
        return set.containsKey(v);
    }

    /**
     * Check if the set is empty.
     *
     * @return <code>true</code> when the set is empty.
     */
    @NotModified
    public boolean isEmpty() {
        return set.isEmpty();
    }


    @Override
    public @NotNull Iterator<V> iterator() {
        return toImmutableSet().iterator();
    }

    @Override
    public @NotNull Object[] toArray() {
        return set.keySet().toArray();
    }

    @Override
    public @NotNull <T> T[] toArray(@NotNull T[] a) {
        return set.keySet().toArray(a);
    }

    /**
     * Return the size of the set.
     *
     * @return the size of the set
     */
    @NotModified
    public int size() {
        return set.size();
    }

    /**
     * Iterate over all elements of the set.
     *
     * @param consumer a consumer which will accept every value present in the set. No nulls will be presented to the
     *                 <code>accept</code> method of the consumer.
     * @throws NullPointerException when the consumer is null
     */
    @NotModified
    public void forEach(@NotNull(content = true) @Independent(hc = true) Consumer<? super V> consumer) {
        set.keySet().forEach(consumer);
    }

    /**
     * Return a stream of the elements of the set.
     *
     * @return A stream of the elements of the set. The stream will not contain nulls.
     */
    @NotModified
    @NotNull(content = true)
    @Independent(hc = true)
    public Stream<V> stream() {
        return set.keySet().stream();
    }

    /**
     * Make a level 2 immutable copy of the underlying set. Requires Java 10+
     *
     * @return a level 2 immutable copy of the underlying set.
     */
    @NotModified
    @NotNull(content = true)
    @ImmutableContainer
    public Set<V> toImmutableSet() {
        return Set.copyOf(set.keySet());
    }
}
