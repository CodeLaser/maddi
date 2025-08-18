package org.e2immu.analyzer.aapi.archive.v2.jdk;
import java.util.Comparator;
import java.util.function.*;

import org.e2immu.annotation.Independent;
import org.e2immu.annotation.Modified;
import org.e2immu.annotation.NotNull;

public class JavaUtilFunction {
    public static final String PACKAGE_NAME = "java.util.function";
    //public interface BiConsumer
    @Independent(hc = true)
    class BiConsumer$<T, U> {
        void accept(/*@Independent(hc=true)[T] @NotModified[T]*/ @Modified T t, /*@Independent(hc=true)[T] @NotModified[T]*/ @Modified U u) { }
        @NotNull
        BiConsumer<T, U> andThen(/*@IgnoreModifications[T]*/ @NotNull BiConsumer<? super T, ? super U> after) { return null; }
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

        BiPredicate<T, U> and(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ @NotNull BiPredicate<? super T, ? super U> other) {
            return null;
        }

        //@Independent(hc=true)[O]
        @NotNull
        BiPredicate<T, U> negate() { return null; }

        //@Independent(hc=true)[O]
        @NotNull

        BiPredicate<T, U> or(
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
        Consumer<T> andThen(/*@IgnoreModifications[T] @Independent(hc=true)[T]*/ @NotNull Consumer<? super T> after) {
            return null;
        }
    }

    //public interface Function
    @Independent(hc = true)
    class Function$<T, R> {
        //frequency 5
        //@Independent(hc=true)[O]
        R apply(/*@Independent(hc=true)[T]*/ @Modified T t) { return null; }

        //@Independent(hc=true)[O]
        @NotNull
        <V> Function<V, R> compose(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ @NotNull Function<? super V, ? extends T> before) {
            return null;
        }

        //@Independent(hc=true)[O]
        @NotNull
        <V> Function<T, V> andThen(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ @NotNull Function<? super R, ? extends V> after) {
            return null;
        }

        //frequency 2
        //@Independent(hc=true)[O]
        @NotNull
        static <T> Function<T, T> identity() { return null; }
    }

    //public interface IntFunction
    @Independent(hc = true)
    class IntFunction$<R> {
        //@Independent(hc=true)[T]
        R apply(int i) { return null; }
    }

    //public interface Predicate
    class Predicate$<T> {
        boolean test(/*@Independent(hc=true)[T] @NotModified[T]*/ @Modified T t) { return false; }
        @NotNull
        Predicate<T> and(/*@IgnoreModifications[T]*/ @NotNull Predicate<? super T> other) { return null; }
        @NotNull
        Predicate<T> negate() { return null; }
        @NotNull
        Predicate<T> or(/*@IgnoreModifications[T]*/ @NotNull Predicate<? super T> other) { return null; }
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
}
