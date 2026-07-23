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

import org.e2immu.annotation.ImmutableContainer;
import org.e2immu.annotation.NotModified;
import org.e2immu.annotation.NotNull;
import org.e2immu.annotation.Nullable;

import java.util.Objects;

/**
 * A class holding either an object of type <code>A</code>, or one of type <code>B</code>; it cannot hold both,
 * nor can it hold neither.
 * <p>
 * <code>Either</code> is immutable, because its fields are explicitly final, and of implicitly immutable
 * type (unbound parameter type).
 * <p>
 * This is an example class! Please extend and modify for your needs.
 *
 * @param <A> The type of the left object.
 * @param <B> The type fo the right object.
 */
@ImmutableContainer(hc = true)
public class Either<A, B> {

    // contracted, because getLeft and getRight cause exceptions
    @Nullable(contract = true)
    private final A left;
    @Nullable(contract = true)
    private final B right;

    private Either(@Nullable A a, @Nullable B b) {
        if ((a == null && b == null) || (a != null && b != null)) throw new IllegalArgumentException();
        left = a;
        right = b;
    }

    /**
     * This method should only be called when the left value has been initialised.
     *
     * @return the left value, of type A.
     * @throws NullPointerException when the right value had been initialised.
     */
    @NotNull
    @NotModified
    public A getLeft() {
        return Objects.requireNonNull(left);
    }

    /**
     * This method should only be called when the right value has been initialised.
     *
     * @return the right value, of type B.
     * @throws NullPointerException when the left value had been initialised.
     */
    @NotNull
    @NotModified
    public B getRight() {
        return Objects.requireNonNull(right);
    }

    /**
     * Test if the left value has been initialised.
     *
     * @return <code>true</code> when the left value has been initialised.
     */
    public boolean isLeft() {
        return left != null;
    }

    /**
     * Test if the right value has been initialised.
     *
     * @return <code>true</code> when the right value has been initialised.
     */
    public boolean isRight() {
        return right != null;
    }

    /**
     * Factory method that produces an <code>Either</code> object with the right side filled in.
     *
     * @param right of type <code>R</code>; cannot be <code>null</code>
     * @param <L>   type of left side
     * @param <R>   type of right side
     * @return an <code>Either</code> object with the right side filled in.
     * @throws NullPointerException when the argument is <code>null</code>.
     */
    @NotModified
    @NotNull
    public static <L, R> Either<L, R> right(@NotNull R right) {
        return new Either<>(null, Objects.requireNonNull(right));
    }

    /**
     * Factory method that produces an <code>Either</code> object with the left side filled in.
     *
     * @param left of type <code>L</code>
     * @param <L>  type of left side
     * @param <R>  type of right side
     * @return an <code>Either</code> object with the left side filled in.
     * @throws NullPointerException when the argument is <code>null</code>.
     */
    @NotModified
    @NotNull
    public static <L, R> Either<L, R> left(@NotNull L left) {
        return new Either<>(Objects.requireNonNull(left), null);
    }

    /**
     * Getter with alternative.
     *
     * @param orElse an alternative value, in case the left value had not been initialised.
     * @return the left value or the alternative value
     * @throws NullPointerException when the <code>orElse</code> parameter is <code>null</code>.
     */
    @NotNull
    @NotModified
    public A getLeftOrElse(@NotNull A orElse) {
        A local = left;
        return local != null ? local : Objects.requireNonNull(orElse);
    }

    /**
     * Getter with alternative.
     *
     * @param orElse an alternative value, in case the right value had not been initialised.
     * @return the right value or the alternative value
     * @throws NullPointerException when the <code>orElse</code> parameter is <code>null</code>.
     */
    @NotNull
    @NotModified
    public B getRightOrElse(@NotNull B orElse) {
        B local = right;
        return local != null ? local : Objects.requireNonNull(orElse);
    }

    /**
     * Delegating equals.
     *
     * @param o the other value
     * @return <code>true</code> when <code>o</code> is also an <code>Either</code> object, with
     * the same left or right object, as defined by the <code>equals</code> method on <code>A</code>
     * or <code>B</code> respectively.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Either<?, ?> either = (Either<?, ?>) o;
        return Objects.equals(left, either.left) &&
                Objects.equals(right, either.right);
    }

    /**
     * Delegating hash code.
     *
     * @return a hash code based on the hash code of the left or right value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    /**
     * String representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return "[" + left + "|" + right + "]";
    }
}
