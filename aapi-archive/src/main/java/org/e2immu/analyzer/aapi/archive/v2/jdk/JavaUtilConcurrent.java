package org.e2immu.analyzer.aapi.archive.v2.jdk;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.e2immu.annotation.Independent;

public class JavaUtilConcurrent {
    public static final String PACKAGE_NAME = "java.util.concurrent";
    //public class CompletableFuture implements Future<T>, CompletionStage<T>
    class CompletableFuture$<T> {
        //public interface AsynchronousCompletionTask
        //@Container[M]
        @Independent class AsynchronousCompletionTask { }
        CompletableFuture$() { }
        static <U> CompletableFuture<U> supplyAsync(/*@IgnoreModifications[T]*/ Supplier<U> supplier) { return null; }
        static <U> CompletableFuture<U> supplyAsync(/*@IgnoreModifications[T]*/ Supplier<U> supplier, Executor executor) {
            return null;
        }
        static CompletableFuture<Void> runAsync(Runnable runnable) { return null; }
        static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) { return null; }
        //frequency 21
        static <U> CompletableFuture<U> completedFuture(/*@Independent(hc=true)[T] @NotModified[T]*/ U value) {
            return null;
        }

        //override from java.util.concurrent.Future
        //frequency 1
        boolean isDone() { return false; }

        //override from java.util.concurrent.Future
        //frequency 1
        //@Independent(hc=true)[H]
        T get() { return null; }

        //override from java.util.concurrent.Future
        //@Independent(hc=true)[H]
        T get(long timeout, TimeUnit timeUnit) { return null; }

        //frequency 12
        //@Independent(hc=true)[T]
        T join() { return null; }

        //@Independent(hc=true)[T]
        T getNow(/*@Independent(hc=true)[T] @NotModified[T]*/ T valueIfAbsent) { return null; }

        //override from java.util.concurrent.Future
        //@Independent(hc=true)[H]
        T resultNow() { return null; }

        //override from java.util.concurrent.Future
        Throwable exceptionNow() { return null; }
        boolean complete(/*@Independent(hc=true)[T] @NotModified[T]*/ T value) { return false; }
        boolean completeExceptionally(Throwable ex) { return false; }
        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> thenApply(/*@IgnoreModifications[T]*/ Function<? super T, ? extends U> fn) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> thenApplyAsync(/*@IgnoreModifications[T]*/ Function<? super T, ? extends U> fn) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> thenApplyAsync(
            /*@IgnoreModifications[T]*/ Function<? super T, ? extends U> fn,
            Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> thenAccept(/*@IgnoreModifications[T]*/ Consumer<? super T> action) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> thenAcceptAsync(/*@IgnoreModifications[T]*/ Consumer<? super T> action) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> thenAcceptAsync(
            /*@IgnoreModifications[T]*/ Consumer<? super T> action,
            Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> thenRun(Runnable action) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> thenRunAsync(Runnable action) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U, V> CompletableFuture<V> thenCombine(
            CompletionStage<? extends U> other,
            /*@IgnoreModifications[T]*/ BiFunction<? super T, ? super U, ? extends V> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U, V> CompletableFuture<V> thenCombineAsync(
            CompletionStage<? extends U> other,
            /*@IgnoreModifications[T]*/ BiFunction<? super T, ? super U, ? extends V> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U, V> CompletableFuture<V> thenCombineAsync(
            CompletionStage<? extends U> other,
            /*@IgnoreModifications[T]*/ BiFunction<? super T, ? super U, ? extends V> fn,
            Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<Void> thenAcceptBoth(
            CompletionStage<? extends U> other,
            /*@IgnoreModifications[T]*/ BiConsumer<? super T, ? super U> action) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other,
            /*@IgnoreModifications[T]*/ BiConsumer<? super T, ? super U> action) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other,
            /*@IgnoreModifications[T]*/ BiConsumer<? super T, ? super U> action,
            Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> applyToEither(
            CompletionStage<? extends T> other,
            /*@IgnoreModifications[T]*/ Function<? super T, U> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> applyToEitherAsync(
            CompletionStage<? extends T> other,
            /*@IgnoreModifications[T]*/ Function<? super T, U> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> applyToEitherAsync(
            CompletionStage<? extends T> other,
            /*@IgnoreModifications[T]*/ Function<? super T, U> fn,
            Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> acceptEither(
            CompletionStage<? extends T> other,
            /*@IgnoreModifications[T]*/ Consumer<? super T> action) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> acceptEitherAsync(
            CompletionStage<? extends T> other,
            /*@IgnoreModifications[T]*/ Consumer<? super T> action) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> acceptEitherAsync(
            CompletionStage<? extends T> other,
            /*@IgnoreModifications[T]*/ Consumer<? super T> action,
            Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> thenCompose(
            /*@IgnoreModifications[T]*/ Function<? super T, ? extends CompletionStage<U>> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> thenComposeAsync(
            /*@IgnoreModifications[T]*/ Function<? super T, ? extends CompletionStage<U>> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> thenComposeAsync(
            /*@IgnoreModifications[T]*/ Function<? super T, ? extends CompletionStage<U>> fn,
            Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> whenComplete(/*@IgnoreModifications[T]*/ BiConsumer<? super T, ? super Throwable> action) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> whenCompleteAsync(
            /*@IgnoreModifications[T]*/ BiConsumer<? super T, ? super Throwable> action) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> whenCompleteAsync(
            /*@IgnoreModifications[T]*/ BiConsumer<? super T, ? super Throwable> action,
            Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> handle(/*@IgnoreModifications[T]*/ BiFunction<? super T, Throwable, ? extends U> fn) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> handleAsync(
            /*@IgnoreModifications[T]*/ BiFunction<? super T, Throwable, ? extends U> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> handleAsync(
            /*@IgnoreModifications[T]*/ BiFunction<? super T, Throwable, ? extends U> fn,
            Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> toCompletableFuture() { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> exceptionally(/*@IgnoreModifications[T]*/ Function<Throwable, ? extends T> fn) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> exceptionallyAsync(/*@IgnoreModifications[T]*/ Function<Throwable, ? extends T> fn) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> exceptionallyAsync(
            /*@IgnoreModifications[T]*/ Function<Throwable, ? extends T> fn,
            Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> exceptionallyCompose(
            /*@IgnoreModifications[T]*/ Function<Throwable, ? extends CompletionStage<T>> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> exceptionallyComposeAsync(
            /*@IgnoreModifications[T]*/ Function<Throwable, ? extends CompletionStage<T>> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> exceptionallyComposeAsync(
            /*@IgnoreModifications[T]*/ Function<Throwable, ? extends CompletionStage<T>> fn,
            Executor executor) { return null; }
        static CompletableFuture<Void> allOf(CompletableFuture<?> ... cfs) { return null; }
        static CompletableFuture<Object> anyOf(CompletableFuture<?> ... cfs) { return null; }
        //override from java.util.concurrent.Future
        boolean cancel(boolean mayInterruptIfRunning) { return false; }

        //override from java.util.concurrent.Future
        boolean isCancelled() { return false; }
        boolean isCompletedExceptionally() { return false; }
        //override from java.util.concurrent.Future
        Future.State state() { return null; }
        void obtrudeValue(/*@Independent(hc=true)[T] @NotModified[T]*/ T value) { }
        void obtrudeException(Throwable ex) { }
        int getNumberOfDependents() { return 0; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        <U> CompletableFuture<U> newIncompleteFuture() { return null; }
        Executor defaultExecutor() { return null; }
        CompletableFuture<T> copy() { return null; }
        CompletionStage<T> minimalCompletionStage() { return null; }
        CompletableFuture<T> completeAsync(
            /*@IgnoreModifications[T]*/ Supplier<? extends T> supplier,
            Executor executor) { return null; }
        CompletableFuture<T> completeAsync(/*@IgnoreModifications[T]*/ Supplier<? extends T> supplier) { return null; }
        CompletableFuture<T> orTimeout(long timeout, TimeUnit timeUnit) { return null; }
        CompletableFuture<T> completeOnTimeout(
            /*@Independent(hc=true)[T] @NotModified[T]*/ T value,
            long timeout,
            TimeUnit timeUnit) { return null; }

        //@Independent[T]
        static Executor delayedExecutor(long delay, TimeUnit timeUnit, Executor unit) { return null; }

        //@Independent[T]
        static Executor delayedExecutor(long delay, TimeUnit timeUnit) { return null; }

        //@Independent[T]
        static <U> CompletionStage<U> completedStage(/*@Independent(hc=true)[T] @NotModified[T]*/ U value) {
            return null;
        }
        static <U> CompletableFuture<U> failedFuture(Throwable ex) { return null; }
        //@Independent[T]
        static <U> CompletionStage<U> failedStage(Throwable ex) { return null; }
    }

    //public interface CompletionStage
    class CompletionStage$<T> {
        <U> CompletionStage<U> thenApply(/*@IgnoreModifications[T]*/ Function<? super T, ? extends U> function) {
            return null;
        }

        <U> CompletionStage<U> thenApplyAsync(/*@IgnoreModifications[T]*/ Function<? super T, ? extends U> function) {
            return null;
        }

        <U> CompletionStage<U> thenApplyAsync(
            /*@IgnoreModifications[T]*/ Function<? super T, ? extends U> function,
            Executor executor) { return null; }
        CompletionStage<Void> thenAccept(/*@IgnoreModifications[T]*/ Consumer<? super T> consumer) { return null; }
        CompletionStage<Void> thenAcceptAsync(/*@IgnoreModifications[T]*/ Consumer<? super T> consumer) { return null; }

        CompletionStage<Void> thenAcceptAsync(
            /*@IgnoreModifications[T]*/ Consumer<? super T> consumer,
            Executor executor) { return null; }
        CompletionStage<Void> thenRun(Runnable runnable) { return null; }
        CompletionStage<Void> thenRunAsync(Runnable runnable) { return null; }
        CompletionStage<Void> thenRunAsync(Runnable runnable, Executor executor) { return null; }
        <U, V> CompletionStage<V> thenCombine(
            CompletionStage<? extends U> completionStage,
            /*@IgnoreModifications[T]*/ BiFunction<? super T, ? super U, ? extends V> biFunction) { return null; }

        <U, V> CompletionStage<V> thenCombineAsync(
            CompletionStage<? extends U> completionStage,
            /*@IgnoreModifications[T]*/ BiFunction<? super T, ? super U, ? extends V> biFunction) { return null; }

        <U, V> CompletionStage<V> thenCombineAsync(
            CompletionStage<? extends U> completionStage,
            /*@IgnoreModifications[T]*/ BiFunction<? super T, ? super U, ? extends V> biFunction,
            Executor executor) { return null; }

        <U> CompletionStage<Void> thenAcceptBoth(
            CompletionStage<? extends U> completionStage,
            /*@IgnoreModifications[T]*/ BiConsumer<? super T, ? super U> biConsumer) { return null; }

        <U> CompletionStage<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> completionStage,
            /*@IgnoreModifications[T]*/ BiConsumer<? super T, ? super U> biConsumer) { return null; }

        <U> CompletionStage<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> completionStage,
            /*@IgnoreModifications[T]*/ BiConsumer<? super T, ? super U> biConsumer,
            Executor executor) { return null; }
        CompletionStage<Void> runAfterBoth(CompletionStage<?> completionStage, Runnable runnable) { return null; }
        CompletionStage<Void> runAfterBothAsync(CompletionStage<?> completionStage, Runnable runnable) { return null; }
        CompletionStage<Void> runAfterBothAsync(
            CompletionStage<?> completionStage,
            Runnable runnable,
            Executor executor) { return null; }

        <U> CompletionStage<U> applyToEither(
            CompletionStage<? extends T> completionStage,
            /*@IgnoreModifications[T]*/ Function<? super T, U> function) { return null; }

        <U> CompletionStage<U> applyToEitherAsync(
            CompletionStage<? extends T> completionStage,
            /*@IgnoreModifications[T]*/ Function<? super T, U> function) { return null; }

        <U> CompletionStage<U> applyToEitherAsync(
            CompletionStage<? extends T> completionStage,
            /*@IgnoreModifications[T]*/ Function<? super T, U> function,
            Executor executor) { return null; }

        CompletionStage<Void> acceptEither(
            CompletionStage<? extends T> completionStage,
            /*@IgnoreModifications[T]*/ Consumer<? super T> consumer) { return null; }

        CompletionStage<Void> acceptEitherAsync(
            CompletionStage<? extends T> completionStage,
            /*@IgnoreModifications[T]*/ Consumer<? super T> consumer) { return null; }

        CompletionStage<Void> acceptEitherAsync(
            CompletionStage<? extends T> completionStage,
            /*@IgnoreModifications[T]*/ Consumer<? super T> consumer,
            Executor executor) { return null; }
        CompletionStage<Void> runAfterEither(CompletionStage<?> completionStage, Runnable runnable) { return null; }
        CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> completionStage, Runnable runnable) { return null; }

        CompletionStage<Void> runAfterEitherAsync(
            CompletionStage<?> completionStage,
            Runnable runnable,
            Executor executor) { return null; }

        <U> CompletionStage<U> thenCompose(
            /*@IgnoreModifications[T]*/ Function<? super T, ? extends CompletionStage<U>> function) { return null; }

        <U> CompletionStage<U> thenComposeAsync(
            /*@IgnoreModifications[T]*/ Function<? super T, ? extends CompletionStage<U>> function) { return null; }

        <U> CompletionStage<U> thenComposeAsync(
            /*@IgnoreModifications[T]*/ Function<? super T, ? extends CompletionStage<U>> function,
            Executor executor) { return null; }

        <U> CompletionStage<U> handle(
            /*@IgnoreModifications[T]*/ BiFunction<? super T, Throwable, ? extends U> biFunction) { return null; }

        <U> CompletionStage<U> handleAsync(
            /*@IgnoreModifications[T]*/ BiFunction<? super T, Throwable, ? extends U> biFunction) { return null; }

        <U> CompletionStage<U> handleAsync(
            /*@IgnoreModifications[T]*/ BiFunction<? super T, Throwable, ? extends U> biFunction,
            Executor executor) { return null; }

        CompletionStage<T> whenComplete(/*@IgnoreModifications[T]*/ BiConsumer<? super T, ? super Throwable> biConsumer) {
            return null;
        }

        CompletionStage<T> whenCompleteAsync(
            /*@IgnoreModifications[T]*/ BiConsumer<? super T, ? super Throwable> biConsumer) { return null; }

        CompletionStage<T> whenCompleteAsync(
            /*@IgnoreModifications[T]*/ BiConsumer<? super T, ? super Throwable> biConsumer,
            Executor executor) { return null; }

        CompletionStage<T> exceptionally(/*@IgnoreModifications[T]*/ Function<Throwable, ? extends T> function) {
            return null;
        }

        CompletionStage<T> exceptionallyAsync(/*@IgnoreModifications[T]*/ Function<Throwable, ? extends T> fn) {
            return null;
        }

        CompletionStage<T> exceptionallyAsync(
            /*@IgnoreModifications[T]*/ Function<Throwable, ? extends T> fn,
            Executor executor) { return null; }

        CompletionStage<T> exceptionallyCompose(
            /*@IgnoreModifications[T]*/ Function<Throwable, ? extends CompletionStage<T>> fn) { return null; }

        CompletionStage<T> exceptionallyComposeAsync(
            /*@IgnoreModifications[T]*/ Function<Throwable, ? extends CompletionStage<T>> fn) { return null; }

        CompletionStage<T> exceptionallyComposeAsync(
            /*@IgnoreModifications[T]*/ Function<Throwable, ? extends CompletionStage<T>> fn,
            Executor executor) { return null; }
        CompletableFuture<T> toCompletableFuture() { return null; }
    }

    //public class ConcurrentHashMap extends AbstractMap<K,V> implements ConcurrentMap<K,V>, Serializable
    class ConcurrentHashMap$<K, V> {
        //public static final class KeySetView extends CollectionView<K,V,K> implements Set<K>, Serializable
        class KeySetView<K, V> {
            //@Independent(hc=true)[T]
            V getMappedValue() { return null; }

            //override from java.util.Collection, java.util.Set, java.util.concurrent.ConcurrentHashMap.CollectionView
            //@NotModified[H]
            boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object o) { return false; }

            //override from java.util.Collection, java.util.Set, java.util.concurrent.ConcurrentHashMap.CollectionView
            boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object o) { return false; }

            //override from java.lang.Iterable, java.util.Collection, java.util.Set, java.util.concurrent.ConcurrentHashMap.CollectionView
            //@NotModified[H] @NotNull[H]
            Iterator<K> iterator() { return null; }

            //override from java.util.Collection, java.util.Set
            boolean add(/*@Independent(hc=true)[H] @NotModified[T]*/ K e) { return false; }

            //override from java.util.Collection, java.util.Set
            boolean addAll(/*@Independent(hc=true)[H] @NotModified[H]*/ Collection<? extends K> c) { return false; }

            //override from java.lang.Object, java.util.Collection, java.util.Set
            //@NotModified[H]
            public int hashCode() { return 0; }

            //override from java.lang.Object, java.util.Collection, java.util.Set
            //@NotModified[H]
            public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object o) { return false; }

            //override from java.lang.Iterable, java.util.Collection, java.util.Set
            //@Independent(hc=true)[H] @NotModified[H] @NotNull[H]
            Spliterator<K> spliterator() { return null; }

            //override from java.lang.Iterable
            //@NotModified[H]

            void forEach(
                /*@IgnoreModifications[T] @Independent[M] @NotModified[H] @NotNull[H]*/ Consumer<? super K> action) { }
        }
        ConcurrentHashMap$() { }
        ConcurrentHashMap$(int initialCapacity) { }
        ConcurrentHashMap$(Map<? extends K, ? extends V> m) { }
        ConcurrentHashMap$(int initialCapacity, float loadFactor) { }
        ConcurrentHashMap$(int initialCapacity, float loadFactor, int concurrencyLevel) { }
        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        boolean isEmpty() { return false; }

        //override from java.util.AbstractMap, java.util.Map
        //frequency 1
        //@Independent(hc=true)[H] @NotModified[H]
        V get(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object key) { return null; }

        //override from java.util.AbstractMap, java.util.Map
        //frequency 1
        //@NotModified[H]
        boolean containsKey(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object key) { return false; }

        //override from java.util.AbstractMap, java.util.Map
        boolean containsValue(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object value) {
            return false;
        }

        //override from java.util.AbstractMap, java.util.Map
        //frequency 1
        //@Independent(hc=true)[H]

        V put(/*@Independent(hc=true)[H] @NotModified[T]*/ K key, /*@Independent(hc=true)[H] @NotModified[T]*/ V value) {
            return null;
        }

        //override from java.util.AbstractMap, java.util.Map
        void putAll(/*@NotModified[H]*/ Map<? extends K, ? extends V> m) { }

        //override from java.util.AbstractMap, java.util.Map
        //@Independent(hc=true)[H]
        V remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object key) { return null; }

        //override from java.util.AbstractMap, java.util.Map
        void clear() { }

        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        ConcurrentHashMap.KeySetView<K, V> keySet() { return null; }

        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        Collection<V> values() { return null; }

        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        Set<Map.Entry<K, V>> entrySet() { return null; }

        //override from java.lang.Object, java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object, java.util.AbstractMap
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }

        //override from java.lang.Object, java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object o) { return false; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@Independent(hc=true)[H]

        V putIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V value) { return null; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        boolean remove(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object key,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object value) { return false; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        boolean replace(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V oldValue,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V newValue) { return false; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@Independent(hc=true)[H]

        V replace(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V value) { return null; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@Independent(hc=true)[H] @NotModified[H]

        V getOrDefault(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object key,
            /*@Independent[M] @NotModified[T]*/ V defaultValue) { return null; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@NotModified[H]

        void forEach(
            /*@IgnoreModifications[T] @Independent[M] @NotModified[H]*/ BiConsumer<? super K, ? super V> action) { }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        void replaceAll(
            /*@IgnoreModifications[T] @NotModified[H]*/ BiFunction<? super K, ? super V, ? extends V> function) { }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@Independent(hc=true)[H]

        V computeIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@IgnoreModifications[T] @NotModified[H]*/ Function<? super K, ? extends V> mappingFunction) { return null; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@Independent(hc=true)[H]

        V computeIfPresent(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@IgnoreModifications[T] @NotModified[H]*/ BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return null;
        }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@Independent(hc=true)[H]

        V compute(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@IgnoreModifications[T] @NotModified[H]*/ BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return null;
        }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@Independent(hc=true)[H]

        V merge(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V value,
            /*@IgnoreModifications[T] @Independent(hc=true)[H] @NotModified[H]*/ BiFunction<
                ? super V,
                ? super V,
                ? extends V> remappingFunction) { return null; }

        boolean contains(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object value) {
            return false;
        }
        Enumeration<K> keys() { return null; }
        Enumeration<V> elements() { return null; }
        long mappingCount() { return 0L; }
        static <K> ConcurrentHashMap.KeySetView<K, Boolean> newKeySet() { return null; }
        static <K> ConcurrentHashMap.KeySetView<K, Boolean> newKeySet(int initialCapacity) { return null; }
        ConcurrentHashMap.KeySetView<K, V> keySet(/*@Independent(hc=true)[T] @NotModified[T]*/ V mappedValue) {
            return null;
        }

        void forEach(long parallelismThreshold, /*@IgnoreModifications[T]*/ BiConsumer<? super K, ? super V> biConsumer) { }

        <U> void forEach(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ BiFunction<? super K, ? super V, ? extends U> biFunction,
            /*@IgnoreModifications[T]*/ Consumer<? super U> transformer) { }

        //@Independent(hc=true)[T]
        <U> U search(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ BiFunction<? super K, ? super V, ? extends U> biFunction) { return null; }

        //@Independent(hc=true)[T]
        <U> U reduce(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ BiFunction<? super K, ? super V, ? extends U> biFunction,
            /*@IgnoreModifications[T]*/ BiFunction<? super U, ? super U, ? extends U> transformer) { return null; }

        double reduceToDouble(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ ToDoubleBiFunction<? super K, ? super V> toDoubleBiFunction,
            double transformer,
            /*@IgnoreModifications[T]*/ DoubleBinaryOperator basis) { return 0.0; }

        long reduceToLong(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ ToLongBiFunction<? super K, ? super V> toLongBiFunction,
            long transformer,
            /*@IgnoreModifications[T]*/ LongBinaryOperator basis) { return 0L; }

        int reduceToInt(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ ToIntBiFunction<? super K, ? super V> toIntBiFunction,
            int transformer,
            /*@IgnoreModifications[T]*/ IntBinaryOperator basis) { return 0; }
        void forEachKey(long parallelismThreshold, /*@IgnoreModifications[T]*/ Consumer<? super K> consumer) { }
        <U> void forEachKey(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ Function<? super K, ? extends U> function,
            /*@IgnoreModifications[T]*/ Consumer<? super U> transformer) { }

        //@Independent(hc=true)[T]
        <U> U searchKeys(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ Function<? super K, ? extends U> function) { return null; }

        //@Independent(hc=true)[T]
        K reduceKeys(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ BiFunction<? super K, ? super K, ? extends K> biFunction) { return null; }

        //@Independent(hc=true)[T]
        <U> U reduceKeys(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ Function<? super K, ? extends U> function,
            /*@IgnoreModifications[T]*/ BiFunction<? super U, ? super U, ? extends U> transformer) { return null; }

        double reduceKeysToDouble(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ ToDoubleFunction<? super K> toDoubleFunction,
            double transformer,
            /*@IgnoreModifications[T]*/ DoubleBinaryOperator basis) { return 0.0; }

        long reduceKeysToLong(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ ToLongFunction<? super K> toLongFunction,
            long transformer,
            /*@IgnoreModifications[T]*/ LongBinaryOperator basis) { return 0L; }

        int reduceKeysToInt(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ ToIntFunction<? super K> toIntFunction,
            int transformer,
            /*@IgnoreModifications[T]*/ IntBinaryOperator basis) { return 0; }
        void forEachValue(long parallelismThreshold, /*@IgnoreModifications[T]*/ Consumer<? super V> consumer) { }
        <U> void forEachValue(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ Function<? super V, ? extends U> function,
            /*@IgnoreModifications[T]*/ Consumer<? super U> transformer) { }

        //@Independent(hc=true)[T]
        <U> U searchValues(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ Function<? super V, ? extends U> function) { return null; }

        //@Independent(hc=true)[T]
        V reduceValues(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ BiFunction<? super V, ? super V, ? extends V> biFunction) { return null; }

        //@Independent(hc=true)[T]
        <U> U reduceValues(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ Function<? super V, ? extends U> function,
            /*@IgnoreModifications[T]*/ BiFunction<? super U, ? super U, ? extends U> transformer) { return null; }

        double reduceValuesToDouble(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ ToDoubleFunction<? super V> toDoubleFunction,
            double transformer,
            /*@IgnoreModifications[T]*/ DoubleBinaryOperator basis) { return 0.0; }

        long reduceValuesToLong(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ ToLongFunction<? super V> toLongFunction,
            long transformer,
            /*@IgnoreModifications[T]*/ LongBinaryOperator basis) { return 0L; }

        int reduceValuesToInt(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ ToIntFunction<? super V> toIntFunction,
            int transformer,
            /*@IgnoreModifications[T]*/ IntBinaryOperator basis) { return 0; }

        void forEachEntry(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ Consumer<? super Map.Entry<K, V>> consumer) { }

        <U> void forEachEntry(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ Function<Map.Entry<K, V>, ? extends U> function,
            /*@IgnoreModifications[T]*/ Consumer<? super U> transformer) { }

        //@Independent(hc=true)[T]
        <U> U searchEntries(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ Function<Map.Entry<K, V>, ? extends U> function) { return null; }

        Map.Entry<K, V> reduceEntries(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ BiFunction<Map.Entry<K, V>, Map.Entry<K, V>, ? extends Map.Entry<K, V>>
                biFunction) { return null; }

        //@Independent(hc=true)[T]
        <U> U reduceEntries(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ Function<Map.Entry<K, V>, ? extends U> function,
            /*@IgnoreModifications[T]*/ BiFunction<? super U, ? super U, ? extends U> transformer) { return null; }

        double reduceEntriesToDouble(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ ToDoubleFunction<Map.Entry<K, V>> toDoubleFunction,
            double transformer,
            /*@IgnoreModifications[T]*/ DoubleBinaryOperator basis) { return 0.0; }

        long reduceEntriesToLong(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ ToLongFunction<Map.Entry<K, V>> toLongFunction,
            long transformer,
            /*@IgnoreModifications[T]*/ LongBinaryOperator basis) { return 0L; }

        int reduceEntriesToInt(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ ToIntFunction<Map.Entry<K, V>> toIntFunction,
            int transformer,
            /*@IgnoreModifications[T]*/ IntBinaryOperator basis) { return 0; }
    }

    //public interface ConcurrentMap implements Map<K,V>
    class ConcurrentMap$<K, V> {
        //override from java.util.Map
        //@Independent(hc=true)[H] @NotModified[H]

        V getOrDefault(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object key,
            /*@Independent[M] @NotModified[T]*/ V defaultValue) { return null; }

        //override from java.util.Map
        //@NotModified[H]

        void forEach(
            /*@IgnoreModifications[T] @Independent[M] @NotModified[H]*/ BiConsumer<? super K, ? super V> action) { }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V putIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K k,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V v) { return null; }

        //override from java.util.Map
        boolean remove(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object object1) { return false; }

        //override from java.util.Map
        boolean replace(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K k,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V v,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V v1) { return false; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V replace(/*@Independent(hc=true)[H] @NotModified[T]*/ K k, /*@Independent(hc=true)[H] @NotModified[T]*/ V v) {
            return null;
        }

        //override from java.util.Map
        void replaceAll(
            /*@IgnoreModifications[T] @NotModified[H]*/ BiFunction<? super K, ? super V, ? extends V> function) { }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V computeIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@IgnoreModifications[T] @NotModified[H]*/ Function<? super K, ? extends V> mappingFunction) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V computeIfPresent(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@IgnoreModifications[T] @NotModified[H]*/ BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return null;
        }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V compute(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@IgnoreModifications[T] @NotModified[H]*/ BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return null;
        }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V merge(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V value,
            /*@IgnoreModifications[T] @Independent(hc=true)[H] @NotModified[H]*/ BiFunction<
                ? super V,
                ? super V,
                ? extends V> remappingFunction) { return null; }
    }

    //public interface Executor
    class Executor$ {void execute(Runnable runnable) { } }

    //public interface ExecutorService implements Executor, AutoCloseable
    class ExecutorService$ {
        //frequency 1
        void shutdown() { }
        List<Runnable> shutdownNow() { return null; }
        boolean isShutdown() { return false; }
        boolean isTerminated() { return false; }
        boolean awaitTermination(long l, TimeUnit timeUnit) { return false; }
        <T> Future<T> submit(Callable<T> callable) { return null; }
        <T> Future<T> submit(Runnable runnable, /*@Independent(hc=true)[T] @NotModified[T]*/ T t) { return null; }
        Future<?> submit(Runnable runnable) { return null; }
        <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection) { return null; }
        <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) {
            return null;
        }

        //@Independent(hc=true)[T]
        <T> T invokeAny(Collection<? extends Callable<T>> collection) { return null; }

        //@Independent(hc=true)[T]
        <T> T invokeAny(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) { return null; }

        //override from java.lang.AutoCloseable
        void close() { }
    }

    //public interface Future
    class Future$<V> {
        //public enum State extends Enum<State>
        class State {
            //@NotNull[O]
            static final Future.State RUNNING = null;

            //@NotNull[O]
            static final Future.State SUCCESS = null;

            //@NotNull[O]
            static final Future.State FAILED = null;

            //@NotNull[O]
            static final Future.State CANCELLED = null;
            static Future.State [] values() { return null; }
            static Future.State valueOf(String name) { return null; }
        }
        boolean cancel(boolean b) { return false; }
        boolean isCancelled() { return false; }
        //override has frequency 1
        boolean isDone() { return false; }

        //frequency 6
        //@Independent(hc=true)[T]
        V get() { return null; }

        //@Independent(hc=true)[T]
        V get(long l, TimeUnit timeUnit) { return null; }

        //@Independent(hc=true)[T]
        V resultNow() { return null; }
        Throwable exceptionNow() { return null; }
        Future.State state() { return null; }
    }

    //public interface ThreadFactory
    class ThreadFactory$ {Thread newThread(Runnable runnable) { return null; } }

    //public final class ThreadLocalRandom extends Random
    class ThreadLocalRandom$ {
        //frequency 1
        static ThreadLocalRandom current() { return null; }

        //override from java.util.Random
        void setSeed(long seed) { }

        //override from java.util.Random, java.util.random.RandomGenerator
        boolean nextBoolean() { return false; }

        //override from java.util.Random, java.util.random.RandomGenerator
        int nextInt() { return 0; }

        //override from java.util.Random, java.util.random.RandomGenerator
        int nextInt(int bound) { return 0; }

        //override from java.util.random.RandomGenerator
        int nextInt(int origin, int bound) { return 0; }

        //override from java.util.Random, java.util.random.RandomGenerator
        long nextLong() { return 0L; }

        //override from java.util.random.RandomGenerator
        long nextLong(long bound) { return 0L; }

        //override from java.util.random.RandomGenerator
        long nextLong(long origin, long l) { return 0L; }

        //override from java.util.Random, java.util.random.RandomGenerator
        float nextFloat() { return 0.0F; }

        //override from java.util.random.RandomGenerator
        float nextFloat(float bound) { return 0.0F; }

        //override from java.util.random.RandomGenerator
        float nextFloat(float origin, float bound) { return 0.0F; }

        //override from java.util.Random, java.util.random.RandomGenerator
        //frequency 1
        double nextDouble() { return 0.0; }

        //override from java.util.random.RandomGenerator
        double nextDouble(double bound) { return 0.0; }

        //override from java.util.random.RandomGenerator
        double nextDouble(double origin, double d) { return 0.0; }

        //override from java.util.Random, java.util.random.RandomGenerator
        IntStream ints(long streamSize) { return null; }

        //override from java.util.Random, java.util.random.RandomGenerator
        IntStream ints() { return null; }

        //override from java.util.Random, java.util.random.RandomGenerator
        IntStream ints(long streamSize, int i, int randomNumberOrigin) { return null; }

        //override from java.util.Random, java.util.random.RandomGenerator
        IntStream ints(int randomNumberOrigin, int randomNumberBound) { return null; }

        //override from java.util.Random, java.util.random.RandomGenerator
        LongStream longs(long streamSize) { return null; }

        //override from java.util.Random, java.util.random.RandomGenerator
        LongStream longs() { return null; }

        //override from java.util.Random, java.util.random.RandomGenerator
        LongStream longs(long streamSize, long l, long randomNumberOrigin) { return null; }

        //override from java.util.Random, java.util.random.RandomGenerator
        LongStream longs(long randomNumberOrigin, long l) { return null; }

        //override from java.util.Random, java.util.random.RandomGenerator
        DoubleStream doubles(long streamSize) { return null; }

        //override from java.util.Random, java.util.random.RandomGenerator
        DoubleStream doubles() { return null; }

        //override from java.util.Random, java.util.random.RandomGenerator
        DoubleStream doubles(long streamSize, double d, double randomNumberOrigin) { return null; }

        //override from java.util.Random, java.util.random.RandomGenerator
        DoubleStream doubles(double randomNumberOrigin, double d) { return null; }
    }
}
