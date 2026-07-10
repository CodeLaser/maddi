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

package org.e2immu.analyzer.aapi.archive.jdk;
import java.util.Comparator;
import java.util.function.*;

import org.e2immu.annotation.Independent;
import org.e2immu.annotation.Modified;
import org.e2immu.annotation.NotModified;
import org.e2immu.annotation.NotNull;

public class JavaUtilFunction {
    public static final String PACKAGE_NAME = "java.util.function";
    //public interface BiConsumer
    @Independent(hc = true)
    class BiConsumer$<T, U> {
        void accept(/*@Independent(hc=true)[T] @NotModified[T]*/ @Modified T t, /*@Independent(hc=true)[T] @NotModified[T]*/ @Modified U u) { }
        @NotNull
        @NotModified BiConsumer<T, U> andThen(/*@IgnoreModifications[T]*/ @NotNull BiConsumer<? super T, ? super U> after) { return null; }
    }

    //public interface BiFunction
    @Independent(hc = true)
    class BiFunction$<T, U, R> {
        R apply(/*@Independent(hc=true)[T]*/ @Modified T t, /*@Independent(hc=true)[T]*/ @Modified U u) { return null; }
        @NotModified
        @NotNull
        <V> BiFunction<T, U, V> andThen(/*@IgnoreModifications[T]*/ @NotNull Function<? super R, ? extends V> after) {
            return null;
        }
    }

    //public interface BiPredicate
    @Independent(hc = true)
    class BiPredicate$<T, U> {
        //frequency 1
        boolean test(/*@Independent(hc=true)[T]*/ @Modified T t, /*@Independent(hc=true)[T]*/ @Modified U u) {
            return false;
        }

        //@Independent(hc=true)[O]
        @NotNull

        @NotModified BiPredicate<T, U> and(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ @NotNull BiPredicate<? super T, ? super U> other) {
            return null;
        }

        //@Independent(hc=true)[O]
        @NotNull
        @NotModified BiPredicate<T, U> negate() { return null; }

        //@Independent(hc=true)[O]
        @NotNull

        @NotModified BiPredicate<T, U> or(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ @NotNull BiPredicate<? super T, ? super U> other) {
            return null;
        }
    }

    //public interface BinaryOperator implements BiFunction<T,T,T>
    @Independent(hc = true)
    class BinaryOperator$<T> {
        @NotNull
        static <T> BinaryOperator<T> minBy(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Comparator<? super T> comparator) {
            return null;
        }

        @NotNull
        static <T> BinaryOperator<T> maxBy(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Comparator<? super T> comparator) {
            return null;
        }
    }

    //public interface Consumer
    @Independent(hc = true)
    class Consumer$<T> {
        //frequency 1
        void accept(/*@Independent(hc=true)[T]*/ @Modified T t) { }

        //@Independent(hc=true)[O]
        @NotNull
        @NotModified Consumer<T> andThen(/*@IgnoreModifications[T] @Independent(hc=true)[T]*/ @NotNull Consumer<? super T> after) {
            return null;
        }
    }

    //public interface DoubleConsumer
    @Independent(hc = true)
    class DoubleConsumer$ {
        @NotModified @NotNull DoubleConsumer andThen(/*@IgnoreModifications[T]*/ @NotNull DoubleConsumer after) { return null; }
    }

    //public interface DoublePredicate
    @Independent(hc = true)
    class DoublePredicate$ {
        @NotModified @NotNull DoublePredicate and(/*@IgnoreModifications[T]*/ @NotNull DoublePredicate other) { return null; }
        @NotModified @NotNull DoublePredicate negate() { return null; }
        @NotModified @NotNull DoublePredicate or(/*@IgnoreModifications[T]*/ @NotNull DoublePredicate other) { return null; }
    }

    //public interface DoubleUnaryOperator
    @Independent(hc = true)
    class DoubleUnaryOperator$ {
        @NotModified @NotNull DoubleUnaryOperator compose(/*@IgnoreModifications[T]*/ @NotNull DoubleUnaryOperator before) { return null; }
        @NotModified @NotNull DoubleUnaryOperator andThen(/*@IgnoreModifications[T]*/ @NotNull DoubleUnaryOperator after) { return null; }
    }

    //public interface Function
    @Independent(hc = true)
    class Function$<T, R> {
        //frequency 5
        //@Independent(hc=true)[O]
        R apply(/*@Independent(hc=true)[T]*/ @Modified T t) { return null; }

        //@Independent(hc=true)[O]
        @NotNull
        @NotModified <V> Function<V, R> compose(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ @NotNull Function<? super V, ? extends T> before) {
            return null;
        }

        //@Independent(hc=true)[O]
        @NotNull
        @NotModified <V> Function<T, V> andThen(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ @NotNull Function<? super R, ? extends V> after) {
            return null;
        }

        //frequency 2
        //@Independent(hc=true)[O]
        @NotNull
        static <T> Function<T, T> identity() { return null; }
    }

    //public interface IntConsumer
    @Independent(hc = true)
    class IntConsumer$ {
        @NotModified @NotNull IntConsumer andThen(/*@IgnoreModifications[T]*/ @NotNull IntConsumer after) { return null; }
    }

    //public interface IntFunction
    @Independent(hc = true)
    class IntFunction$<R> {
        //@Independent(hc=true)[T]
        R apply(int i) { return null; }
    }

    //public interface IntPredicate
    @Independent(hc = true)
    class IntPredicate$ {
        @NotModified @NotNull IntPredicate and(/*@IgnoreModifications[T]*/ @NotNull IntPredicate other) { return null; }
        @NotModified @NotNull IntPredicate negate() { return null; }
        @NotModified @NotNull IntPredicate or(/*@IgnoreModifications[T]*/ @NotNull IntPredicate other) { return null; }
    }

    //public interface IntUnaryOperator
    @Independent(hc = true)
    class IntUnaryOperator$ {
        @NotModified @NotNull IntUnaryOperator compose(/*@IgnoreModifications[T]*/ @NotNull IntUnaryOperator before) { return null; }
        @NotModified @NotNull IntUnaryOperator andThen(/*@IgnoreModifications[T]*/ @NotNull IntUnaryOperator after) { return null; }
    }

    //public interface LongConsumer
    @Independent(hc = true)
    class LongConsumer$ {
        @NotModified @NotNull LongConsumer andThen(/*@IgnoreModifications[T]*/ @NotNull LongConsumer after) { return null; }
    }

    //public interface LongPredicate
    @Independent(hc = true)
    class LongPredicate$ {
        @NotModified @NotNull LongPredicate and(/*@IgnoreModifications[T]*/ @NotNull LongPredicate other) { return null; }
        @NotModified @NotNull LongPredicate negate() { return null; }
        @NotModified @NotNull LongPredicate or(/*@IgnoreModifications[T]*/ @NotNull LongPredicate other) { return null; }
    }

    //public interface LongUnaryOperator
    @Independent(hc = true)
    class LongUnaryOperator$ {
        @NotModified @NotNull LongUnaryOperator compose(/*@IgnoreModifications[T]*/ @NotNull LongUnaryOperator before) { return null; }
        @NotModified @NotNull LongUnaryOperator andThen(/*@IgnoreModifications[T]*/ @NotNull LongUnaryOperator after) { return null; }
    }

    //public interface Predicate
    @Independent(hc = true)
    class Predicate$<T> {
        boolean test(/*@Independent(hc=true)[T] @NotModified[T]*/ @Modified T t) { return false; }
        @NotNull
        @NotModified Predicate<T> and(/*@IgnoreModifications[T]*/ @NotNull Predicate<? super T> other) { return null; }
        @NotNull
        @NotModified Predicate<T> negate() { return null; }
        @NotNull
        @NotModified Predicate<T> or(/*@IgnoreModifications[T]*/ @NotNull Predicate<? super T> other) { return null; }
        @NotNull
        static <T> Predicate<T> isEqual(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Object targetRef) { return null; }
        @NotNull
        static <T> Predicate<T> not(/*@IgnoreModifications[T]*/@NotNull Predicate<? super T> target) { return null; }
    }

    //public interface Supplier
    //@Container[M]
    @Independent(hc = true)
    class Supplier$<T> {
        //@Independent(hc=true)[O]
        T get() { return null; }
    }

    //public interface UnaryOperator implements Function<T,T>
    // inherits andThen/compose (already @NotModified) from Function; identity() is a static factory
    @Independent(hc = true)
    class UnaryOperator$<T> {
        @NotNull
        static <T> UnaryOperator<T> identity() { return null; }
    }

}
