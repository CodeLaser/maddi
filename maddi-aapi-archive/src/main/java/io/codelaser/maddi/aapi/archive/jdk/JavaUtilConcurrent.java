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

package io.codelaser.maddi.aapi.archive.jdk;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;

public class JavaUtilConcurrent {
    public static final String PACKAGE_NAME = "java.util.concurrent";
    //public class CompletableFuture implements Future<T>, CompletionStage<T>
    // mutable holder of a result of hidden-content type T; no method modifies a parameter
    @Container
    @Independent(hc = true)
    class CompletableFuture$<T> {
        //public interface AsynchronousCompletionTask
        //@Container[M]
        @Independent class AsynchronousCompletionTask { }
        CompletableFuture$() { }
        static <U> CompletableFuture<U> supplyAsync(/*@IgnoreModifications[T]*/ @NotNull Supplier<U> supplier) { return null; }
        static <U> CompletableFuture<U> supplyAsync(/*@IgnoreModifications[T]*/ @NotNull Supplier<U> supplier, @NotNull Executor executor) {
            return null;
        }
        @NotNull(nullableArguments = 0) static CompletableFuture<Void> runAsync(@NotNull Runnable runnable) { return null; }
        @NotNull(nullableArguments = 0) static CompletableFuture<Void> runAsync(@NotNull Runnable runnable, @NotNull Executor executor) { return null; }
        //frequency 21
        static <U> CompletableFuture<U> completedFuture(/*@Independent(hc=true)[T] @NotModified[T]*/ U value) {
            return null;
        }

        //override from java.util.concurrent.Future
        //frequency 1
        @NotModified boolean isDone() { return false; }

        //override from java.util.concurrent.Future
        //frequency 1
        //@Independent(hc=true)[H]
        @NotModified T get() { return null; }

        //override from java.util.concurrent.Future
        //@Independent(hc=true)[H]
        @NotModified T get(long timeout, @NotNull TimeUnit timeUnit) { return null; }

        //frequency 12
        //@Independent(hc=true)[T]
        @NotModified T join() { return null; }

        //@Independent(hc=true)[T]
        @Nullable @NotModified T getNow(/*@Independent(hc=true)[T] @NotModified[T]*/ @Nullable T valueIfAbsent) { return null; }

        //override from java.util.concurrent.Future
        //@Independent(hc=true)[H]
        @NotModified T resultNow() { return null; }

        //override from java.util.concurrent.Future
        @NotModified Throwable exceptionNow() { return null; }
        boolean complete(/*@Independent(hc=true)[T] @NotModified[T]*/ T value) { return false; }
        boolean completeExceptionally(@NotNull Throwable ex) { return false; }
        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> thenApply(/*@IgnoreModifications[T]*/ @NotNull Function<? super T, ? extends U> fn) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> thenApplyAsync(/*@IgnoreModifications[T]*/ @NotNull Function<? super T, ? extends U> fn) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> thenApplyAsync(
            /*@IgnoreModifications[T]*/ @NotNull Function<? super T, ? extends U> fn,
            @NotNull Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> thenAccept(/*@IgnoreModifications[T]*/ @NotNull Consumer<? super T> action) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> thenAcceptAsync(/*@IgnoreModifications[T]*/ @NotNull Consumer<? super T> action) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> thenAcceptAsync(
            /*@IgnoreModifications[T]*/ @NotNull Consumer<? super T> action,
            @NotNull Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> thenRun(@NotNull Runnable action) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> thenRunAsync(@NotNull Runnable action) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> thenRunAsync(@NotNull Runnable action, @NotNull Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U, V> CompletableFuture<V> thenCombine(
            @NotNull CompletionStage<? extends U> other,
            /*@IgnoreModifications[T]*/ @NotNull BiFunction<? super T, ? super U, ? extends V> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U, V> CompletableFuture<V> thenCombineAsync(
            @NotNull CompletionStage<? extends U> other,
            /*@IgnoreModifications[T]*/ @NotNull BiFunction<? super T, ? super U, ? extends V> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U, V> CompletableFuture<V> thenCombineAsync(
            @NotNull CompletionStage<? extends U> other,
            /*@IgnoreModifications[T]*/ @NotNull BiFunction<? super T, ? super U, ? extends V> fn,
            @NotNull Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) <U> CompletableFuture<Void> thenAcceptBoth(
            @NotNull CompletionStage<? extends U> other,
            /*@IgnoreModifications[T]*/ @NotNull BiConsumer<? super T, ? super U> action) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) <U> CompletableFuture<Void> thenAcceptBothAsync(
            @NotNull CompletionStage<? extends U> other,
            /*@IgnoreModifications[T]*/ @NotNull BiConsumer<? super T, ? super U> action) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) <U> CompletableFuture<Void> thenAcceptBothAsync(
            @NotNull CompletionStage<? extends U> other,
            /*@IgnoreModifications[T]*/ @NotNull BiConsumer<? super T, ? super U> action,
            @NotNull Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> runAfterBoth(@NotNull CompletionStage<?> other, @NotNull Runnable action) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> runAfterBothAsync(@NotNull CompletionStage<?> other, @NotNull Runnable action) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> runAfterBothAsync(@NotNull CompletionStage<?> other, @NotNull Runnable action, @NotNull Executor executor) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> applyToEither(
            @NotNull CompletionStage<? extends T> other,
            /*@IgnoreModifications[T]*/ @NotNull Function<? super T, U> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> applyToEitherAsync(
            @NotNull CompletionStage<? extends T> other,
            /*@IgnoreModifications[T]*/ @NotNull Function<? super T, U> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> applyToEitherAsync(
            @NotNull CompletionStage<? extends T> other,
            /*@IgnoreModifications[T]*/ @NotNull Function<? super T, U> fn,
            @NotNull Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> acceptEither(
            @NotNull CompletionStage<? extends T> other,
            /*@IgnoreModifications[T]*/ @NotNull Consumer<? super T> action) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> acceptEitherAsync(
            @NotNull CompletionStage<? extends T> other,
            /*@IgnoreModifications[T]*/ @NotNull Consumer<? super T> action) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> acceptEitherAsync(
            @NotNull CompletionStage<? extends T> other,
            /*@IgnoreModifications[T]*/ @NotNull Consumer<? super T> action,
            @NotNull Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> runAfterEither(@NotNull CompletionStage<?> other, @NotNull Runnable action) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> runAfterEitherAsync(@NotNull CompletionStage<?> other, @NotNull Runnable action) { return null; }

        //override from java.util.concurrent.CompletionStage
        @NotNull(nullableArguments = 0) CompletableFuture<Void> runAfterEitherAsync(@NotNull CompletionStage<?> other, @NotNull Runnable action, @NotNull Executor executor) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> thenCompose(
            /*@IgnoreModifications[T]*/ @NotNull Function<? super T, ? extends CompletionStage<U>> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> thenComposeAsync(
            /*@IgnoreModifications[T]*/ @NotNull Function<? super T, ? extends CompletionStage<U>> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> thenComposeAsync(
            /*@IgnoreModifications[T]*/ @NotNull Function<? super T, ? extends CompletionStage<U>> fn,
            @NotNull Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> whenComplete(/*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {0, 1}) BiConsumer<? super T, ? super Throwable> action) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> whenCompleteAsync(
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {0, 1}) BiConsumer<? super T, ? super Throwable> action) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> whenCompleteAsync(
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {0, 1}) BiConsumer<? super T, ? super Throwable> action,
            @NotNull Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> handle(/*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {0, 1}) BiFunction<? super T, Throwable, ? extends U> fn) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> handleAsync(
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {0, 1}) BiFunction<? super T, Throwable, ? extends U> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        <U> CompletableFuture<U> handleAsync(
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {0, 1}) BiFunction<? super T, Throwable, ? extends U> fn,
            @NotNull Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> toCompletableFuture() { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> exceptionally(/*@IgnoreModifications[T]*/ @NotNull Function<Throwable, ? extends T> fn) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> exceptionallyAsync(/*@IgnoreModifications[T]*/ @NotNull Function<Throwable, ? extends T> fn) {
            return null;
        }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> exceptionallyAsync(
            /*@IgnoreModifications[T]*/ @NotNull Function<Throwable, ? extends T> fn,
            @NotNull Executor executor) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> exceptionallyCompose(
            /*@IgnoreModifications[T]*/ @NotNull Function<Throwable, ? extends CompletionStage<T>> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> exceptionallyComposeAsync(
            /*@IgnoreModifications[T]*/ @NotNull Function<Throwable, ? extends CompletionStage<T>> fn) { return null; }

        //override from java.util.concurrent.CompletionStage
        CompletableFuture<T> exceptionallyComposeAsync(
            /*@IgnoreModifications[T]*/ @NotNull Function<Throwable, ? extends CompletionStage<T>> fn,
            @NotNull Executor executor) { return null; }
        @NotNull(nullableArguments = 0) static CompletableFuture<Void> allOf(@NotNull CompletableFuture<?> ... cfs) { return null; }
        @NotNull(nullableArguments = 0) static CompletableFuture<Object> anyOf(@NotNull CompletableFuture<?> ... cfs) { return null; }
        //override from java.util.concurrent.Future
        boolean cancel(boolean mayInterruptIfRunning) { return false; }

        //override from java.util.concurrent.Future
        @NotModified boolean isCancelled() { return false; }
        @NotModified boolean isCompletedExceptionally() { return false; }
        //override from java.util.concurrent.Future
        @NotModified Future.State state() { return null; }
        void obtrudeValue(/*@Independent(hc=true)[T] @NotModified[T]*/ T value) { }
        void obtrudeException(@NotNull Throwable ex) { }
        @NotModified int getNumberOfDependents() { return 0; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        <U> CompletableFuture<U> newIncompleteFuture() { return null; }
        Executor defaultExecutor() { return null; }
        CompletableFuture<T> copy() { return null; }
        CompletionStage<T> minimalCompletionStage() { return null; }
        CompletableFuture<T> completeAsync(
            /*@IgnoreModifications[T]*/ @NotNull Supplier<? extends T> supplier,
            @NotNull Executor executor) { return null; }
        CompletableFuture<T> completeAsync(/*@IgnoreModifications[T]*/ @NotNull Supplier<? extends T> supplier) { return null; }
        CompletableFuture<T> orTimeout(long timeout, @NotNull TimeUnit timeUnit) { return null; }
        CompletableFuture<T> completeOnTimeout(
            /*@Independent(hc=true)[T] @NotModified[T]*/ T value,
            long timeout,
            @NotNull TimeUnit timeUnit) { return null; }

        //@Independent[T]
        static Executor delayedExecutor(long delay, @NotNull TimeUnit timeUnit, @NotNull Executor unit) { return null; }

        //@Independent[T]
        static Executor delayedExecutor(long delay, @NotNull TimeUnit timeUnit) { return null; }

        //@Independent[T]
        static <U> CompletionStage<U> completedStage(/*@Independent(hc=true)[T] @NotModified[T]*/ U value) {
            return null;
        }
        static <U> CompletableFuture<U> failedFuture(@NotNull Throwable ex) { return null; }
        //@Independent[T]
        static <U> CompletionStage<U> failedStage(@NotNull Throwable ex) { return null; }
    }

    //public interface CompletionStage
    @Container
    @Independent(hc = true)
    class CompletionStage$<T> {
        <U> CompletionStage<U> thenApply(/*@IgnoreModifications[T]*/ @NotNull Function<? super T, ? extends U> function) {
            return null;
        }

        <U> CompletionStage<U> thenApplyAsync(/*@IgnoreModifications[T]*/ @NotNull Function<? super T, ? extends U> function) {
            return null;
        }

        <U> CompletionStage<U> thenApplyAsync(
            /*@IgnoreModifications[T]*/ @NotNull Function<? super T, ? extends U> function,
            @NotNull Executor executor) { return null; }
        @NotNull(nullableArguments = 0) CompletionStage<Void> thenAccept(/*@IgnoreModifications[T]*/ @NotNull Consumer<? super T> consumer) { return null; }
        @NotNull(nullableArguments = 0) CompletionStage<Void> thenAcceptAsync(/*@IgnoreModifications[T]*/ @NotNull Consumer<? super T> consumer) { return null; }

        @NotNull(nullableArguments = 0) CompletionStage<Void> thenAcceptAsync(
            /*@IgnoreModifications[T]*/ @NotNull Consumer<? super T> consumer,
            @NotNull Executor executor) { return null; }
        @NotNull(nullableArguments = 0) CompletionStage<Void> thenRun(@NotNull Runnable runnable) { return null; }
        @NotNull(nullableArguments = 0) CompletionStage<Void> thenRunAsync(@NotNull Runnable runnable) { return null; }
        @NotNull(nullableArguments = 0) CompletionStage<Void> thenRunAsync(@NotNull Runnable runnable, @NotNull Executor executor) { return null; }
        <U, V> CompletionStage<V> thenCombine(
            @NotNull CompletionStage<? extends U> completionStage,
            /*@IgnoreModifications[T]*/ @NotNull BiFunction<? super T, ? super U, ? extends V> biFunction) { return null; }

        <U, V> CompletionStage<V> thenCombineAsync(
            @NotNull CompletionStage<? extends U> completionStage,
            /*@IgnoreModifications[T]*/ @NotNull BiFunction<? super T, ? super U, ? extends V> biFunction) { return null; }

        <U, V> CompletionStage<V> thenCombineAsync(
            @NotNull CompletionStage<? extends U> completionStage,
            /*@IgnoreModifications[T]*/ @NotNull BiFunction<? super T, ? super U, ? extends V> biFunction,
            @NotNull Executor executor) { return null; }

        @NotNull(nullableArguments = 0) <U> CompletionStage<Void> thenAcceptBoth(
            @NotNull CompletionStage<? extends U> completionStage,
            /*@IgnoreModifications[T]*/ @NotNull BiConsumer<? super T, ? super U> biConsumer) { return null; }

        @NotNull(nullableArguments = 0) <U> CompletionStage<Void> thenAcceptBothAsync(
            @NotNull CompletionStage<? extends U> completionStage,
            /*@IgnoreModifications[T]*/ @NotNull BiConsumer<? super T, ? super U> biConsumer) { return null; }

        @NotNull(nullableArguments = 0) <U> CompletionStage<Void> thenAcceptBothAsync(
            @NotNull CompletionStage<? extends U> completionStage,
            /*@IgnoreModifications[T]*/ @NotNull BiConsumer<? super T, ? super U> biConsumer,
            @NotNull Executor executor) { return null; }
        @NotNull(nullableArguments = 0) CompletionStage<Void> runAfterBoth(@NotNull CompletionStage<?> completionStage, @NotNull Runnable runnable) { return null; }
        @NotNull(nullableArguments = 0) CompletionStage<Void> runAfterBothAsync(@NotNull CompletionStage<?> completionStage, @NotNull Runnable runnable) { return null; }
        @NotNull(nullableArguments = 0) CompletionStage<Void> runAfterBothAsync(
            @NotNull CompletionStage<?> completionStage,
            @NotNull Runnable runnable,
            @NotNull Executor executor) { return null; }

        <U> CompletionStage<U> applyToEither(
            @NotNull CompletionStage<? extends T> completionStage,
            /*@IgnoreModifications[T]*/ @NotNull Function<? super T, U> function) { return null; }

        <U> CompletionStage<U> applyToEitherAsync(
            @NotNull CompletionStage<? extends T> completionStage,
            /*@IgnoreModifications[T]*/ @NotNull Function<? super T, U> function) { return null; }

        <U> CompletionStage<U> applyToEitherAsync(
            @NotNull CompletionStage<? extends T> completionStage,
            /*@IgnoreModifications[T]*/ @NotNull Function<? super T, U> function,
            @NotNull Executor executor) { return null; }

        @NotNull(nullableArguments = 0) CompletionStage<Void> acceptEither(
            @NotNull CompletionStage<? extends T> completionStage,
            /*@IgnoreModifications[T]*/ @NotNull Consumer<? super T> consumer) { return null; }

        @NotNull(nullableArguments = 0) CompletionStage<Void> acceptEitherAsync(
            @NotNull CompletionStage<? extends T> completionStage,
            /*@IgnoreModifications[T]*/ @NotNull Consumer<? super T> consumer) { return null; }

        @NotNull(nullableArguments = 0) CompletionStage<Void> acceptEitherAsync(
            @NotNull CompletionStage<? extends T> completionStage,
            /*@IgnoreModifications[T]*/ @NotNull Consumer<? super T> consumer,
            @NotNull Executor executor) { return null; }
        @NotNull(nullableArguments = 0) CompletionStage<Void> runAfterEither(@NotNull CompletionStage<?> completionStage, @NotNull Runnable runnable) { return null; }
        @NotNull(nullableArguments = 0) CompletionStage<Void> runAfterEitherAsync(@NotNull CompletionStage<?> completionStage, @NotNull Runnable runnable) { return null; }

        @NotNull(nullableArguments = 0) CompletionStage<Void> runAfterEitherAsync(
            @NotNull CompletionStage<?> completionStage,
            @NotNull Runnable runnable,
            @NotNull Executor executor) { return null; }

        <U> CompletionStage<U> thenCompose(
            /*@IgnoreModifications[T]*/ @NotNull Function<? super T, ? extends CompletionStage<U>> function) { return null; }

        <U> CompletionStage<U> thenComposeAsync(
            /*@IgnoreModifications[T]*/ @NotNull Function<? super T, ? extends CompletionStage<U>> function) { return null; }

        <U> CompletionStage<U> thenComposeAsync(
            /*@IgnoreModifications[T]*/ @NotNull Function<? super T, ? extends CompletionStage<U>> function,
            @NotNull Executor executor) { return null; }

        <U> CompletionStage<U> handle(
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {0, 1}) BiFunction<? super T, Throwable, ? extends U> biFunction) { return null; }

        <U> CompletionStage<U> handleAsync(
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {0, 1}) BiFunction<? super T, Throwable, ? extends U> biFunction) { return null; }

        <U> CompletionStage<U> handleAsync(
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {0, 1}) BiFunction<? super T, Throwable, ? extends U> biFunction,
            @NotNull Executor executor) { return null; }

        CompletionStage<T> whenComplete(/*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {0, 1}) BiConsumer<? super T, ? super Throwable> biConsumer) {
            return null;
        }

        CompletionStage<T> whenCompleteAsync(
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {0, 1}) BiConsumer<? super T, ? super Throwable> biConsumer) { return null; }

        CompletionStage<T> whenCompleteAsync(
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {0, 1}) BiConsumer<? super T, ? super Throwable> biConsumer,
            @NotNull Executor executor) { return null; }

        CompletionStage<T> exceptionally(/*@IgnoreModifications[T]*/ @NotNull Function<Throwable, ? extends T> function) {
            return null;
        }

        CompletionStage<T> exceptionallyAsync(/*@IgnoreModifications[T]*/ @NotNull Function<Throwable, ? extends T> fn) {
            return null;
        }

        CompletionStage<T> exceptionallyAsync(
            /*@IgnoreModifications[T]*/ @NotNull Function<Throwable, ? extends T> fn,
            @NotNull Executor executor) { return null; }

        CompletionStage<T> exceptionallyCompose(
            /*@IgnoreModifications[T]*/ @NotNull Function<Throwable, ? extends CompletionStage<T>> fn) { return null; }

        CompletionStage<T> exceptionallyComposeAsync(
            /*@IgnoreModifications[T]*/ @NotNull Function<Throwable, ? extends CompletionStage<T>> fn) { return null; }

        CompletionStage<T> exceptionallyComposeAsync(
            /*@IgnoreModifications[T]*/ @NotNull Function<Throwable, ? extends CompletionStage<T>> fn,
            @NotNull Executor executor) { return null; }
        CompletableFuture<T> toCompletableFuture() { return null; }
    }

    //public class ConcurrentHashMap extends AbstractMap<K,V> implements ConcurrentMap<K,V>, Serializable
    @Container
    class ConcurrentHashMap$<K, V> {
        //public static final class KeySetView extends CollectionView<K,V,K> implements Set<K>, Serializable
        class KeySetView<K, V> {
            //@Independent(hc=true)[T]
            V getMappedValue() { return null; }

            //override from java.util.Collection, java.util.Set, java.util.concurrent.ConcurrentHashMap.CollectionView
            //@NotModified[H]
            boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @NotNull Object o) { return false; }

            //override from java.util.Collection, java.util.Set, java.util.concurrent.ConcurrentHashMap.CollectionView
            boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @NotNull Object o) { return false; }

            //override from java.lang.Iterable, java.util.Collection, java.util.Set, java.util.concurrent.ConcurrentHashMap.CollectionView
            //@NotModified[H] @NotNull[H]
            Iterator<K> iterator() { return null; }

            //override from java.util.Collection, java.util.Set
            boolean add(/*@Independent(hc=true)[H] @NotModified[T]*/ K e) { return false; }

            //override from java.util.Collection, java.util.Set
            boolean addAll(/*@Independent(hc=true)[H] @NotModified[H]*/ @NotNull Collection<? extends K> c) { return false; }

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
                /*@IgnoreModifications[T] @Independent[M] @NotModified[H] @NotNull[H]*/ @NotNull Consumer<? super K> action) { }
        }
        ConcurrentHashMap$(/*@NotModified[O]*/ @Independent(hc = true) @NotNull Map<? extends K, ? extends V> m) { }
        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        boolean isEmpty() { return false; }

        //override from java.util.AbstractMap, java.util.Map
        //frequency 1
        //@Independent(hc=true)[H] @NotModified[H]
        @Nullable V get(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @NotNull Object key) { return null; }

        //override from java.util.AbstractMap, java.util.Map
        //frequency 1
        //@NotModified[H]
        boolean containsKey(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @NotNull Object key) { return false; }

        //override from java.util.AbstractMap, java.util.Map
        boolean containsValue(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @NotNull Object value) {
            return false;
        }

        //override from java.util.AbstractMap, java.util.Map
        //frequency 1
        //@Independent(hc=true)[H]

        @Nullable V put(/*@Independent(hc=true)[H] @NotModified[T]*/ K key, /*@Independent(hc=true)[H] @NotModified[T]*/ V value) {
            return null;
        }

        //override from java.util.AbstractMap, java.util.Map
        void putAll(/*@NotModified[H]*/ @NotNull Map<? extends K, ? extends V> m) { }

        //override from java.util.AbstractMap, java.util.Map
        //@Independent(hc=true)[H]
        @Nullable V remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @NotNull Object key) { return null; }

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

        @Nullable V putIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V value) { return null; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        boolean remove(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @NotNull Object key,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @NotNull Object value) { return false; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        boolean replace(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V oldValue,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V newValue) { return false; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@Independent(hc=true)[H]

        @Nullable V replace(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V value) { return null; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@Independent(hc=true)[H] @NotModified[H]

        V getOrDefault(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @NotNull Object key,
            /*@Independent[M] @NotModified[T]*/ @Nullable V defaultValue) { return null; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@NotModified[H]

        void forEach(
            /*@IgnoreModifications[T] @Independent[M] @NotModified[H]*/ @NotNull BiConsumer<? super K, ? super V> action) { }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        void replaceAll(
            /*@IgnoreModifications[T] @NotModified[H]*/ @NotNull BiFunction<? super K, ? super V, ? extends V> function) { }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@Independent(hc=true)[H]

        V computeIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@IgnoreModifications[T] @NotModified[H]*/ @NotNull Function<? super K, ? extends V> mappingFunction) { return null; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@Independent(hc=true)[H]

        @Nullable V computeIfPresent(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@IgnoreModifications[T] @NotModified[H]*/ @NotNull(nullableArguments = 2) BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return null;
        }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@Independent(hc=true)[H]

        V compute(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@IgnoreModifications[T] @NotModified[H]*/ @NotNull(nullableArguments = {1, 2}) BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return null;
        }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        //@Independent(hc=true)[H]

        V merge(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V value,
            /*@IgnoreModifications[T] @Independent(hc=true)[H] @NotModified[H]*/ @NotNull(nullableArguments = 2) BiFunction<
                ? super V,
                ? super V,
                ? extends V> remappingFunction) { return null; }

        @NotModified
        boolean contains(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @NotNull Object value) {
            return false;
        }
        @NotModified Enumeration<K> keys() { return null; }
        @NotModified Enumeration<V> elements() { return null; }
        @NotModified long mappingCount() { return 0L; }
        static <K> ConcurrentHashMap.KeySetView<K, Boolean> newKeySet() { return null; }
        static <K> ConcurrentHashMap.KeySetView<K, Boolean> newKeySet(int initialCapacity) { return null; }
        ConcurrentHashMap.KeySetView<K, V> keySet(/*@Independent(hc=true)[T] @NotModified[T]*/ V mappedValue) {
            return null;
        }

        void forEach(long parallelismThreshold, /*@IgnoreModifications[T]*/ @NotNull BiConsumer<? super K, ? super V> biConsumer) { }

        <U> void forEach(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 2) BiFunction<? super K, ? super V, ? extends U> biFunction,
            /*@IgnoreModifications[T]*/ @NotNull Consumer<? super U> transformer) { }

        //@Independent(hc=true)[T]
        @Nullable <U> U search(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 2) BiFunction<? super K, ? super V, ? extends U> biFunction) { return null; }

        //@Independent(hc=true)[T]
        @Nullable <U> U reduce(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 2) BiFunction<? super K, ? super V, ? extends U> biFunction,
            /*@IgnoreModifications[T]*/ @NotNull BiFunction<? super U, ? super U, ? extends U> transformer) { return null; }

        double reduceToDouble(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull ToDoubleBiFunction<? super K, ? super V> toDoubleBiFunction,
            double transformer,
            /*@IgnoreModifications[T]*/ @NotNull DoubleBinaryOperator basis) { return 0.0; }

        long reduceToLong(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull ToLongBiFunction<? super K, ? super V> toLongBiFunction,
            long transformer,
            /*@IgnoreModifications[T]*/ @NotNull LongBinaryOperator basis) { return 0L; }

        int reduceToInt(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull ToIntBiFunction<? super K, ? super V> toIntBiFunction,
            int transformer,
            /*@IgnoreModifications[T]*/ @NotNull IntBinaryOperator basis) { return 0; }
        void forEachKey(long parallelismThreshold, /*@IgnoreModifications[T]*/ @NotNull Consumer<? super K> consumer) { }
        <U> void forEachKey(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 1) Function<? super K, ? extends U> function,
            /*@IgnoreModifications[T]*/ @NotNull Consumer<? super U> transformer) { }

        //@Independent(hc=true)[T]
        @Nullable <U> U searchKeys(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 1) Function<? super K, ? extends U> function) { return null; }

        //@Independent(hc=true)[T]
        @Nullable K reduceKeys(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull BiFunction<? super K, ? super K, ? extends K> biFunction) { return null; }

        //@Independent(hc=true)[T]
        @Nullable <U> U reduceKeys(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 1) Function<? super K, ? extends U> function,
            /*@IgnoreModifications[T]*/ @NotNull BiFunction<? super U, ? super U, ? extends U> transformer) { return null; }

        double reduceKeysToDouble(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull ToDoubleFunction<? super K> toDoubleFunction,
            double transformer,
            /*@IgnoreModifications[T]*/ @NotNull DoubleBinaryOperator basis) { return 0.0; }

        long reduceKeysToLong(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull ToLongFunction<? super K> toLongFunction,
            long transformer,
            /*@IgnoreModifications[T]*/ @NotNull LongBinaryOperator basis) { return 0L; }

        int reduceKeysToInt(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull ToIntFunction<? super K> toIntFunction,
            int transformer,
            /*@IgnoreModifications[T]*/ @NotNull IntBinaryOperator basis) { return 0; }
        void forEachValue(long parallelismThreshold, /*@IgnoreModifications[T]*/ @NotNull Consumer<? super V> consumer) { }
        <U> void forEachValue(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 1) Function<? super V, ? extends U> function,
            /*@IgnoreModifications[T]*/ @NotNull Consumer<? super U> transformer) { }

        //@Independent(hc=true)[T]
        @Nullable <U> U searchValues(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 1) Function<? super V, ? extends U> function) { return null; }

        //@Independent(hc=true)[T]
        @Nullable V reduceValues(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull BiFunction<? super V, ? super V, ? extends V> biFunction) { return null; }

        //@Independent(hc=true)[T]
        @Nullable <U> U reduceValues(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 1) Function<? super V, ? extends U> function,
            /*@IgnoreModifications[T]*/ @NotNull BiFunction<? super U, ? super U, ? extends U> transformer) { return null; }

        double reduceValuesToDouble(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull ToDoubleFunction<? super V> toDoubleFunction,
            double transformer,
            /*@IgnoreModifications[T]*/ @NotNull DoubleBinaryOperator basis) { return 0.0; }

        long reduceValuesToLong(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull ToLongFunction<? super V> toLongFunction,
            long transformer,
            /*@IgnoreModifications[T]*/ @NotNull LongBinaryOperator basis) { return 0L; }

        int reduceValuesToInt(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull ToIntFunction<? super V> toIntFunction,
            int transformer,
            /*@IgnoreModifications[T]*/ @NotNull IntBinaryOperator basis) { return 0; }

        void forEachEntry(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull Consumer<? super Map.Entry<K, V>> consumer) { }

        <U> void forEachEntry(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 1) Function<Map.Entry<K, V>, ? extends U> function,
            /*@IgnoreModifications[T]*/ @NotNull Consumer<? super U> transformer) { }

        //@Independent(hc=true)[T]
        @Nullable <U> U searchEntries(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 1) Function<Map.Entry<K, V>, ? extends U> function) { return null; }

        @Nullable Map.Entry<K, V> reduceEntries(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull BiFunction<Map.Entry<K, V>, Map.Entry<K, V>, ? extends Map.Entry<K, V>>
                biFunction) { return null; }

        //@Independent(hc=true)[T]
        @Nullable <U> U reduceEntries(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 1) Function<Map.Entry<K, V>, ? extends U> function,
            /*@IgnoreModifications[T]*/ @NotNull BiFunction<? super U, ? super U, ? extends U> transformer) { return null; }

        double reduceEntriesToDouble(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull ToDoubleFunction<Map.Entry<K, V>> toDoubleFunction,
            double transformer,
            /*@IgnoreModifications[T]*/ @NotNull DoubleBinaryOperator basis) { return 0.0; }

        long reduceEntriesToLong(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull ToLongFunction<Map.Entry<K, V>> toLongFunction,
            long transformer,
            /*@IgnoreModifications[T]*/ @NotNull LongBinaryOperator basis) { return 0L; }

        int reduceEntriesToInt(
            long parallelismThreshold,
            /*@IgnoreModifications[T]*/ @NotNull ToIntFunction<Map.Entry<K, V>> toIntFunction,
            int transformer,
            /*@IgnoreModifications[T]*/ @NotNull IntBinaryOperator basis) { return 0; }
    }

    //public interface ConcurrentMap implements Map<K,V>
    @Container
    class ConcurrentMap$<K, V> {
        //override from java.util.Map
        //@Independent(hc=true)[H] @NotModified[H]

        V getOrDefault(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Object key,
            /*@Independent[M] @NotModified[T]*/ @Nullable V defaultValue) { return null; }

        //override from java.util.Map
        //@NotModified[H]

        void forEach(
            /*@IgnoreModifications[T] @Independent[M] @NotModified[H]*/ @NotNull BiConsumer<? super K, ? super V> action) { }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Nullable V putIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K k,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V v) { return null; }

        //override from java.util.Map
        boolean remove(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @Nullable Object object,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @Nullable Object object1) { return false; }

        //override from java.util.Map
        boolean replace(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K k,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V v,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V v1) { return false; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Nullable V replace(/*@Independent(hc=true)[H] @NotModified[T]*/ K k, /*@Independent(hc=true)[H] @NotModified[T]*/ V v) {
            return null;
        }

        //override from java.util.Map
        void replaceAll(
            /*@IgnoreModifications[T] @NotModified[H]*/ @NotNull BiFunction<? super K, ? super V, ? extends V> function) { }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V computeIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@IgnoreModifications[T] @NotModified[H]*/ @NotNull Function<? super K, ? extends V> mappingFunction) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Nullable V computeIfPresent(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@IgnoreModifications[T] @NotModified[H]*/ @NotNull(nullableArguments = 2) BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return null;
        }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V compute(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@IgnoreModifications[T] @NotModified[H]*/ @NotNull(nullableArguments = {1, 2}) BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return null;
        }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V merge(
            /*@Independent(hc=true)[H] @NotModified[T]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[T]*/ V value,
            /*@IgnoreModifications[T] @Independent(hc=true)[H] @NotModified[H]*/ @NotNull(nullableArguments = 2) BiFunction<
                ? super V,
                ? super V,
                ? extends V> remappingFunction) { return null; }
    }

    //public interface Executor
    @Container
    class Executor$ {void execute(@NotNull Runnable runnable) { } }

    //public interface ExecutorService implements Executor, AutoCloseable
    @Container
    class ExecutorService$ {
        //frequency 1
        void shutdown() { }
        List<Runnable> shutdownNow() { return null; }
        boolean isShutdown() { return false; }
        boolean isTerminated() { return false; }
        boolean awaitTermination(long l, @NotNull TimeUnit timeUnit) { return false; }
        <T> Future<T> submit(@NotNull Callable<T> callable) { return null; }
        <T> Future<T> submit(@NotNull Runnable runnable, /*@Independent(hc=true)[T] @NotModified[T]*/ T t) { return null; }
        Future<?> submit(@NotNull Runnable runnable) { return null; }
        <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> collection) { return null; }
        <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> collection, long l, @NotNull TimeUnit timeUnit) {
            return null;
        }

        //@Independent(hc=true)[T]
        <T> T invokeAny(@NotNull Collection<? extends Callable<T>> collection) { return null; }

        //@Independent(hc=true)[T]
        <T> T invokeAny(@NotNull Collection<? extends Callable<T>> collection, long l, @NotNull TimeUnit timeUnit) { return null; }

        //override from java.lang.AutoCloseable
        void close() { }
    }

    //public interface Future
    @Container
    @Independent(hc = true)
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
        @NotModified boolean isCancelled() { return false; }
        //override has frequency 1
        @NotModified boolean isDone() { return false; }

        //frequency 6
        //@Independent(hc=true)[T]
        @NotModified V get() { return null; }

        //@Independent(hc=true)[T]
        @NotModified V get(long l, @NotNull TimeUnit timeUnit) { return null; }

        //@Independent(hc=true)[T]
        @NotModified V resultNow() { return null; }
        @NotModified Throwable exceptionNow() { return null; }
        @NotModified Future.State state() { return null; }
    }

    //public interface ThreadFactory
    @Container
    class ThreadFactory$ {@Nullable Thread newThread(@NotNull Runnable runnable) { return null; } }

    //public enum TimeUnit
    // without it, the unit read as modified in CountDownLatch.await(long, TimeUnit), schedule(.., TimeUnit), ...
    // (2026-10-13, the library-call census)
    @ImmutableContainer
    class TimeUnit$ { 
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        long convert(@NotNull java.time.Duration p0) { return 0; }
        long convert(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return 0; }
        static java.util.concurrent.TimeUnit of(@NotNull java.time.temporal.ChronoUnit p0) { return null; }
        void timedJoin(@NotNull java.lang.Thread p0, long p1) { }
        void timedWait(@NotNull java.lang.Object p0, long p1) { }
    }

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

    // null contracts (docs/design/nullability.md M2), formerly the analyzer's LibraryNullness
    class BlockingDeque$<E> {
        @Nullable E pollFirst(long timeout, @NotNull TimeUnit unit) { return null; }
        @Nullable E pollLast(long timeout, @NotNull TimeUnit unit) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        boolean offer(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
        boolean offerFirst(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
        boolean offerLast(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
        @Nullable E peek() { return null; }
        @Nullable E poll() { return null; }
        @Nullable E poll(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return null; }
        boolean remove(@Nullable java.lang.Object p0) { return false; }
        boolean removeFirstOccurrence(@Nullable java.lang.Object p0) { return false; }
        boolean removeLastOccurrence(@Nullable java.lang.Object p0) { return false; }
    }
    class BlockingQueue$<E> {
        @Nullable E poll(long timeout, @NotNull TimeUnit unit) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        int drainTo(@NotNull java.util.Collection<? super E> p0) { return 0; }
        int drainTo(@NotNull java.util.Collection<? super E> p0, int p1) { return 0; }
        boolean offer(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
        boolean remove(@Nullable java.lang.Object p0) { return false; }
    }
    class PriorityBlockingQueue$<E> {
        @Nullable @NotModified Comparator<? super E> comparator() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PriorityBlockingQueue$(@NotNull java.util.Collection<? extends E> p0) { }
        PriorityBlockingQueue$(int p0, @Nullable java.util.Comparator<? super E> p1) { }
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        int drainTo(@NotNull java.util.Collection<? super E> p0) { return 0; }
        int drainTo(@NotNull java.util.Collection<? super E> p0, int p1) { return 0; }
        void forEach(@NotNull java.util.function.Consumer<? super E> p0) { }
        boolean offer(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
        @Nullable E peek() { return null; }
        @Nullable E poll() { return null; }
        @Nullable E poll(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return null; }
        boolean remove(@Nullable java.lang.Object p0) { return false; }
        boolean removeAll(@NotNull java.util.Collection<?> p0) { return false; }
        boolean removeIf(@NotNull java.util.function.Predicate<? super E> p0) { return false; }
        boolean retainAll(@NotNull java.util.Collection<?> p0) { return false; }
        <T> T[] toArray(@NotNull T[] p0) { return null; }
    }

    //public java.util.concurrent.AbstractExecutorService (shadow created for its nullability, generated)
    class AbstractExecutorService$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(@NotNull java.util.Collection<? extends java.util.concurrent.Callable<T>> p0) { return null; }
        <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(@NotNull java.util.Collection<? extends java.util.concurrent.Callable<T>> p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return null; }
        <T> T invokeAny(@NotNull java.util.Collection<? extends java.util.concurrent.Callable<T>> p0) { return null; }
        <T> T invokeAny(@NotNull java.util.Collection<? extends java.util.concurrent.Callable<T>> p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return null; }
        <T> java.util.concurrent.RunnableFuture<T> newTaskFor(@NotNull java.util.concurrent.Callable<T> p0) { return null; }
        <T> java.util.concurrent.RunnableFuture<T> newTaskFor(@NotNull java.lang.Runnable p0, T p1) { return null; }
        <T> java.util.concurrent.Future<T> submit(@NotNull java.util.concurrent.Callable<T> p0) { return null; }
        java.util.concurrent.Future<?> submit(@NotNull java.lang.Runnable p0) { return null; }
        <T> java.util.concurrent.Future<T> submit(@NotNull java.lang.Runnable p0, T p1) { return null; }
    }

    //public java.util.concurrent.ArrayBlockingQueue (shadow created for its nullability, generated)
    class ArrayBlockingQueue$<E> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ArrayBlockingQueue$(int p0, boolean p1, @NotNull java.util.Collection<? extends E> p2) { }
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        int drainTo(@NotNull java.util.Collection<? super E> p0) { return 0; }
        int drainTo(@NotNull java.util.Collection<? super E> p0, int p1) { return 0; }
        void forEach(@NotNull java.util.function.Consumer<? super E> p0) { }
        boolean offer(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
        @Nullable E peek() { return null; }
        @Nullable E poll() { return null; }
        @Nullable E poll(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return null; }
        boolean remove(@Nullable java.lang.Object p0) { return false; }
        boolean removeAll(@NotNull java.util.Collection<?> p0) { return false; }
        boolean removeIf(@NotNull java.util.function.Predicate<? super E> p0) { return false; }
        boolean retainAll(@NotNull java.util.Collection<?> p0) { return false; }
        <T> T[] toArray(@NotNull T[] p0) { return null; }
    }

    //public java.util.concurrent.CancellationException (shadow created for its nullability, generated)
    class CancellationException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        CancellationException$(@Nullable java.lang.String p0) { }
    }

    //public java.util.concurrent.CompletionService (shadow created for its nullability, generated)
    class CompletionService$<V> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.util.concurrent.Future<V> poll() { return null; }
        @Nullable java.util.concurrent.Future<V> poll(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return null; }
        java.util.concurrent.Future<V> submit(@NotNull java.util.concurrent.Callable<V> p0) { return null; }
        java.util.concurrent.Future<V> submit(@NotNull java.lang.Runnable p0, V p1) { return null; }
    }

    //public java.util.concurrent.ConcurrentLinkedDeque (shadow created for its nullability, generated)
    class ConcurrentLinkedDeque$<E> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ConcurrentLinkedDeque$(@NotNull java.util.Collection<? extends E> p0) { }
        boolean addAll(@NotNull java.util.Collection<? extends E> p0) { return false; }
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        void forEach(@NotNull java.util.function.Consumer<? super E> p0) { }
        @Nullable E peek() { return null; }
        @Nullable E peekFirst() { return null; }
        @Nullable E peekLast() { return null; }
        @Nullable E poll() { return null; }
        @Nullable E pollFirst() { return null; }
        @Nullable E pollLast() { return null; }
        boolean remove(@NotNull java.lang.Object p0) { return false; }
        boolean removeAll(@NotNull java.util.Collection<?> p0) { return false; }
        boolean removeFirstOccurrence(@NotNull java.lang.Object p0) { return false; }
        boolean removeIf(@NotNull java.util.function.Predicate<? super E> p0) { return false; }
        boolean removeLastOccurrence(@NotNull java.lang.Object p0) { return false; }
        boolean retainAll(@NotNull java.util.Collection<?> p0) { return false; }
        <T> T[] toArray(@NotNull T[] p0) { return null; }
    }

    //public java.util.concurrent.ConcurrentLinkedQueue (shadow created for its nullability, generated)
    class ConcurrentLinkedQueue$<E> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ConcurrentLinkedQueue$(@NotNull java.util.Collection<? extends E> p0) { }
        boolean addAll(@NotNull java.util.Collection<? extends E> p0) { return false; }
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        void forEach(@NotNull java.util.function.Consumer<? super E> p0) { }
        @Nullable E peek() { return null; }
        @Nullable E poll() { return null; }
        boolean remove(@Nullable java.lang.Object p0) { return false; }
        boolean removeAll(@NotNull java.util.Collection<?> p0) { return false; }
        boolean removeIf(@NotNull java.util.function.Predicate<? super E> p0) { return false; }
        boolean retainAll(@NotNull java.util.Collection<?> p0) { return false; }
        <T> T[] toArray(@NotNull T[] p0) { return null; }
    }

    //public java.util.concurrent.ConcurrentSkipListMap (shadow created for its nullability, generated)
    class ConcurrentSkipListMap$<K, V> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ConcurrentSkipListMap$(@Nullable java.util.Comparator<? super K> p0) { }
        ConcurrentSkipListMap$(@NotNull java.util.Map<? extends K, ? extends V> p0) { }
        ConcurrentSkipListMap$(@NotNull java.util.SortedMap<K, ? extends V> p0) { }
        @Nullable java.util.Map.Entry<K, V> ceilingEntry(K p0) { return null; }
        @Nullable K ceilingKey(K p0) { return null; }
        @Nullable java.util.Comparator<? super K> comparator() { return null; }
        V compute(K p0, @NotNull(nullableArguments = {1, 2}) java.util.function.BiFunction<? super K, ? super V, ? extends V> p1) { return null; }
        V computeIfAbsent(K p0, @NotNull java.util.function.Function<? super K, ? extends V> p1) { return null; }
        @Nullable V computeIfPresent(K p0, @NotNull(nullableArguments = 2) java.util.function.BiFunction<? super K, ? super V, ? extends V> p1) { return null; }
        boolean containsKey(@NotNull java.lang.Object p0) { return false; }
        boolean containsValue(@NotNull java.lang.Object p0) { return false; }
        @Nullable java.util.Map.Entry<K, V> firstEntry() { return null; }
        @Nullable java.util.Map.Entry<K, V> floorEntry(K p0) { return null; }
        @Nullable K floorKey(K p0) { return null; }
        void forEach(@NotNull java.util.function.BiConsumer<? super K, ? super V> p0) { }
        @Nullable V get(@NotNull java.lang.Object p0) { return null; }
        V getOrDefault(@NotNull java.lang.Object p0, @Nullable V p1) { return null; }
        @Nullable java.util.Map.Entry<K, V> higherEntry(K p0) { return null; }
        @Nullable K higherKey(K p0) { return null; }
        @Nullable java.util.Map.Entry<K, V> lastEntry() { return null; }
        @Nullable java.util.Map.Entry<K, V> lowerEntry(K p0) { return null; }
        @Nullable K lowerKey(K p0) { return null; }
        V merge(K p0, V p1, @NotNull(nullableArguments = 2) java.util.function.BiFunction<? super V, ? super V, ? extends V> p2) { return null; }
        @Nullable java.util.Map.Entry<K, V> pollFirstEntry() { return null; }
        @Nullable java.util.Map.Entry<K, V> pollLastEntry() { return null; }
        @Nullable V put(K p0, V p1) { return null; }
        @Nullable V putFirst(K p0, V p1) { return null; }
        @Nullable V putIfAbsent(K p0, V p1) { return null; }
        @Nullable V putLast(K p0, V p1) { return null; }
        @Nullable V remove(@NotNull java.lang.Object p0) { return null; }
        boolean remove(@NotNull java.lang.Object p0, @NotNull java.lang.Object p1) { return false; }
        @Nullable V replace(K p0, V p1) { return null; }
        void replaceAll(@NotNull java.util.function.BiFunction<? super K, ? super V, ? extends V> p0) { }
    }

    //public java.util.concurrent.ConcurrentSkipListSet (shadow created for its nullability, generated)
    class ConcurrentSkipListSet$<E> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ConcurrentSkipListSet$(@NotNull java.util.Collection<? extends E> p0) { }
        ConcurrentSkipListSet$(@Nullable java.util.Comparator<? super E> p0) { }
        ConcurrentSkipListSet$(@NotNull java.util.SortedSet<E> p0) { }
        @Nullable E ceiling(E p0) { return null; }
        @Nullable java.util.Comparator<? super E> comparator() { return null; }
        boolean contains(@NotNull java.lang.Object p0) { return false; }
        @Nullable E floor(E p0) { return null; }
        @Nullable E higher(E p0) { return null; }
        @Nullable E lower(E p0) { return null; }
        @Nullable E pollFirst() { return null; }
        @Nullable E pollLast() { return null; }
        boolean remove(@NotNull java.lang.Object p0) { return false; }
        boolean removeAll(@NotNull java.util.Collection<?> p0) { return false; }
    }

    //public java.util.concurrent.CopyOnWriteArrayList (shadow created for its nullability, generated)
    class CopyOnWriteArrayList$<E> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        CopyOnWriteArrayList$(@NotNull java.util.Collection<? extends E> p0) { }
        CopyOnWriteArrayList$(@NotNull E[] p0) { }
        boolean addAll(@NotNull java.util.Collection<? extends E> p0) { return false; }
        boolean addAll(int p0, @NotNull java.util.Collection<? extends E> p1) { return false; }
        int addAllAbsent(@NotNull java.util.Collection<? extends E> p0) { return 0; }
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        boolean containsAll(@NotNull java.util.Collection<?> p0) { return false; }
        void forEach(@NotNull java.util.function.Consumer<? super E> p0) { }
        int indexOf(@Nullable java.lang.Object p0) { return 0; }
        int lastIndexOf(@Nullable java.lang.Object p0) { return 0; }
        boolean remove(@Nullable java.lang.Object p0) { return false; }
        boolean removeAll(@NotNull java.util.Collection<?> p0) { return false; }
        boolean removeIf(@NotNull java.util.function.Predicate<? super E> p0) { return false; }
        void replaceAll(@NotNull java.util.function.UnaryOperator<E> p0) { }
        boolean retainAll(@NotNull java.util.Collection<?> p0) { return false; }
        void sort(@Nullable java.util.Comparator<? super E> p0) { }
        <T> T[] toArray(@NotNull T[] p0) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotNull(nullableArguments = 0) java.lang.Object[] toArray() { return null; }
    }

    //public java.util.concurrent.CopyOnWriteArraySet (shadow created for its nullability, generated)
    class CopyOnWriteArraySet$<E> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        CopyOnWriteArraySet$(@NotNull java.util.Collection<? extends E> p0) { }
        boolean addAll(@NotNull java.util.Collection<? extends E> p0) { return false; }
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        boolean containsAll(@NotNull java.util.Collection<?> p0) { return false; }
        void forEach(@NotNull java.util.function.Consumer<? super E> p0) { }
        boolean remove(@Nullable java.lang.Object p0) { return false; }
        boolean removeAll(@NotNull java.util.Collection<?> p0) { return false; }
        boolean removeIf(@NotNull java.util.function.Predicate<? super E> p0) { return false; }
        boolean retainAll(@NotNull java.util.Collection<?> p0) { return false; }
        <T> T[] toArray(@NotNull T[] p0) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotNull(nullableArguments = 0) java.lang.Object[] toArray() { return null; }
    }

    //public java.util.concurrent.CountedCompleter (shadow created for its nullability, generated)
    class CountedCompleter$<T> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.util.concurrent.CountedCompleter<?> firstComplete() { return null; }
        @Nullable java.util.concurrent.CountedCompleter<?> nextComplete() { return null; }
    }

    //public java.util.concurrent.DelayQueue (shadow created for its nullability, generated)
    class DelayQueue$<E extends java.util.concurrent.Delayed> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        DelayQueue$(@NotNull java.util.Collection<? extends E> p0) { }
        int drainTo(@NotNull java.util.Collection<? super E> p0) { return 0; }
        int drainTo(@NotNull java.util.Collection<? super E> p0, int p1) { return 0; }
        boolean offer(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
        @Nullable E peek() { return null; }
        @Nullable E poll() { return null; }
        @Nullable E poll(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return null; }
        boolean remove(@Nullable java.lang.Object p0) { return false; }
        <T> T[] toArray(@NotNull T[] p0) { return null; }
    }

    //public java.util.concurrent.Delayed (shadow created for its nullability, generated)
    class Delayed$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        long getDelay(@NotNull java.util.concurrent.TimeUnit p0) { return 0; }
    }

    //public java.util.concurrent.ExecutionException (shadow created for its nullability, generated)
    class ExecutionException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ExecutionException$(@Nullable java.lang.String p0) { }
        ExecutionException$(@Nullable java.lang.String p0, @Nullable java.lang.Throwable p1) { }
        ExecutionException$(@Nullable java.lang.Throwable p0) { }
    }

    //public java.util.concurrent.ExecutorCompletionService (shadow created for its nullability, generated)
    class ExecutorCompletionService$<V> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ExecutorCompletionService$(@NotNull java.util.concurrent.Executor p0) { }
        ExecutorCompletionService$(@NotNull java.util.concurrent.Executor p0, @NotNull java.util.concurrent.BlockingQueue<java.util.concurrent.Future<V>> p1) { }
        @Nullable java.util.concurrent.Future<V> poll() { return null; }
        @Nullable java.util.concurrent.Future<V> poll(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return null; }
        java.util.concurrent.Future<V> submit(@NotNull java.util.concurrent.Callable<V> p0) { return null; }
        java.util.concurrent.Future<V> submit(@NotNull java.lang.Runnable p0, V p1) { return null; }
    }

    //public java.util.concurrent.Executors (shadow created for its nullability, generated)
    class Executors$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotNull(nullableArguments = 0) static java.util.concurrent.Callable<java.lang.Object> callable(@NotNull java.security.PrivilegedAction<?> p0) { return null; }
        @NotNull(nullableArguments = 0) static java.util.concurrent.Callable<java.lang.Object> callable(@NotNull java.security.PrivilegedExceptionAction<?> p0) { return null; }
        @NotNull(nullableArguments = 0) static java.util.concurrent.Callable<java.lang.Object> callable(@NotNull java.lang.Runnable p0) { return null; }
        static <T> java.util.concurrent.Callable<T> callable(@NotNull java.lang.Runnable p0, T p1) { return null; }
        static java.util.concurrent.ExecutorService newCachedThreadPool(@NotNull java.util.concurrent.ThreadFactory p0) { return null; }
        static java.util.concurrent.ExecutorService newFixedThreadPool(int p0, @NotNull java.util.concurrent.ThreadFactory p1) { return null; }
        static java.util.concurrent.ScheduledExecutorService newScheduledThreadPool(int p0, @NotNull java.util.concurrent.ThreadFactory p1) { return null; }
        static java.util.concurrent.ExecutorService newSingleThreadExecutor(@NotNull java.util.concurrent.ThreadFactory p0) { return null; }
        static java.util.concurrent.ScheduledExecutorService newSingleThreadScheduledExecutor(@NotNull java.util.concurrent.ThreadFactory p0) { return null; }
        static java.util.concurrent.ExecutorService newThreadPerTaskExecutor(@NotNull java.util.concurrent.ThreadFactory p0) { return null; }
        static <T> java.util.concurrent.Callable<T> privilegedCallable(@NotNull java.util.concurrent.Callable<T> p0) { return null; }
        static <T> java.util.concurrent.Callable<T> privilegedCallableUsingCurrentClassLoader(@NotNull java.util.concurrent.Callable<T> p0) { return null; }
        static java.util.concurrent.ExecutorService unconfigurableExecutorService(@NotNull java.util.concurrent.ExecutorService p0) { return null; }
        static java.util.concurrent.ScheduledExecutorService unconfigurableScheduledExecutorService(@NotNull java.util.concurrent.ScheduledExecutorService p0) { return null; }
    }

    //public java.util.concurrent.ForkJoinTask (shadow created for its nullability, generated)
    class ForkJoinTask$<V> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable static java.util.concurrent.ForkJoinPool getPool() { return null; }
        @Nullable static java.util.concurrent.ForkJoinTask<?> peekNextLocalTask() { return null; }
        @Nullable static java.util.concurrent.ForkJoinTask<?> pollNextLocalTask() { return null; }
        @Nullable static java.util.concurrent.ForkJoinTask<?> pollSubmission() { return null; }
        @Nullable static java.util.concurrent.ForkJoinTask<?> pollTask() { return null; }
    }

    //public java.util.concurrent.FutureTask (shadow created for its nullability, generated)
    class FutureTask$<V> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        FutureTask$(@NotNull java.util.concurrent.Callable<V> p0) { }
        FutureTask$(@NotNull java.lang.Runnable p0, V p1) { }
        V get(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return null; }
        void setException(@NotNull java.lang.Throwable p0) { }
    }

    //public java.util.concurrent.LinkedBlockingDeque (shadow created for its nullability, generated)
    class LinkedBlockingDeque$<E> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        LinkedBlockingDeque$(@NotNull java.util.Collection<? extends E> p0) { }
        boolean addAll(@NotNull java.util.Collection<? extends E> p0) { return false; }
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        int drainTo(@NotNull java.util.Collection<? super E> p0) { return 0; }
        int drainTo(@NotNull java.util.Collection<? super E> p0, int p1) { return 0; }
        void forEach(@NotNull java.util.function.Consumer<? super E> p0) { }
        boolean offer(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
        boolean offerFirst(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
        boolean offerLast(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
        @Nullable E peek() { return null; }
        @Nullable E peekFirst() { return null; }
        @Nullable E peekLast() { return null; }
        @Nullable E poll() { return null; }
        @Nullable E poll(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return null; }
        @Nullable E pollFirst() { return null; }
        @Nullable E pollFirst(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return null; }
        @Nullable E pollLast() { return null; }
        @Nullable E pollLast(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return null; }
        boolean remove(@Nullable java.lang.Object p0) { return false; }
        boolean removeAll(@NotNull java.util.Collection<?> p0) { return false; }
        boolean removeFirstOccurrence(@Nullable java.lang.Object p0) { return false; }
        boolean removeIf(@NotNull java.util.function.Predicate<? super E> p0) { return false; }
        boolean removeLastOccurrence(@Nullable java.lang.Object p0) { return false; }
        boolean retainAll(@NotNull java.util.Collection<?> p0) { return false; }
        <T> T[] toArray(@NotNull T[] p0) { return null; }
    }

    //public java.util.concurrent.LinkedBlockingQueue (shadow created for its nullability, generated)
    class LinkedBlockingQueue$<E> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        LinkedBlockingQueue$(@NotNull java.util.Collection<? extends E> p0) { }
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        int drainTo(@NotNull java.util.Collection<? super E> p0) { return 0; }
        int drainTo(@NotNull java.util.Collection<? super E> p0, int p1) { return 0; }
        void forEach(@NotNull java.util.function.Consumer<? super E> p0) { }
        boolean offer(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
        @Nullable E peek() { return null; }
        @Nullable E poll() { return null; }
        @Nullable E poll(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return null; }
        boolean remove(@Nullable java.lang.Object p0) { return false; }
        boolean removeAll(@NotNull java.util.Collection<?> p0) { return false; }
        boolean removeIf(@NotNull java.util.function.Predicate<? super E> p0) { return false; }
        boolean retainAll(@NotNull java.util.Collection<?> p0) { return false; }
        <T> T[] toArray(@NotNull T[] p0) { return null; }
    }

    //public java.util.concurrent.LinkedTransferQueue (shadow created for its nullability, generated)
    class LinkedTransferQueue$<E> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        LinkedTransferQueue$(@NotNull java.util.Collection<? extends E> p0) { }
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        int drainTo(@NotNull java.util.Collection<? super E> p0) { return 0; }
        int drainTo(@NotNull java.util.Collection<? super E> p0, int p1) { return 0; }
        void forEach(@NotNull java.util.function.Consumer<? super E> p0) { }
        boolean offer(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
        @Nullable E peek() { return null; }
        @Nullable E poll() { return null; }
        @Nullable E poll(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return null; }
        boolean remove(@Nullable java.lang.Object p0) { return false; }
        boolean removeAll(@NotNull java.util.Collection<?> p0) { return false; }
        boolean removeIf(@NotNull java.util.function.Predicate<? super E> p0) { return false; }
        boolean retainAll(@NotNull java.util.Collection<?> p0) { return false; }
        <T> T[] toArray(@NotNull T[] p0) { return null; }
        boolean tryTransfer(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
    }

    //public java.util.concurrent.RecursiveAction (shadow created for its nullability, generated)
    class RecursiveAction$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.lang.Void getRawResult() { return null; }
        void setRawResult(@Nullable java.lang.Void p0) { }
    }

    //public java.util.concurrent.ScheduledExecutorService (shadow created for its nullability, generated)
    class ScheduledExecutorService$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        <V> java.util.concurrent.ScheduledFuture<V> schedule(@NotNull java.util.concurrent.Callable<V> p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return null; }
        java.util.concurrent.ScheduledFuture<?> schedule(@NotNull java.lang.Runnable p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return null; }
        java.util.concurrent.ScheduledFuture<?> scheduleAtFixedRate(@NotNull java.lang.Runnable p0, long p1, long p2, @NotNull java.util.concurrent.TimeUnit p3) { return null; }
        java.util.concurrent.ScheduledFuture<?> scheduleWithFixedDelay(@NotNull java.lang.Runnable p0, long p1, long p2, @NotNull java.util.concurrent.TimeUnit p3) { return null; }
    }

    //public java.util.concurrent.ScheduledThreadPoolExecutor (shadow created for its nullability, generated)
    class ScheduledThreadPoolExecutor$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ScheduledThreadPoolExecutor$(int p0, @NotNull java.util.concurrent.RejectedExecutionHandler p1) { }
        ScheduledThreadPoolExecutor$(int p0, @NotNull java.util.concurrent.ThreadFactory p1) { }
        ScheduledThreadPoolExecutor$(int p0, @NotNull java.util.concurrent.ThreadFactory p1, @NotNull java.util.concurrent.RejectedExecutionHandler p2) { }
        <V> java.util.concurrent.RunnableScheduledFuture<V> decorateTask(@NotNull java.util.concurrent.Callable<V> p0, @NotNull java.util.concurrent.RunnableScheduledFuture<V> p1) { return null; }
        <V> java.util.concurrent.RunnableScheduledFuture<V> decorateTask(@NotNull java.lang.Runnable p0, @NotNull java.util.concurrent.RunnableScheduledFuture<V> p1) { return null; }
        void execute(@NotNull java.lang.Runnable p0) { }
        <V> java.util.concurrent.ScheduledFuture<V> schedule(@NotNull java.util.concurrent.Callable<V> p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return null; }
        java.util.concurrent.ScheduledFuture<?> schedule(@NotNull java.lang.Runnable p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return null; }
        java.util.concurrent.ScheduledFuture<?> scheduleAtFixedRate(@NotNull java.lang.Runnable p0, long p1, long p2, @NotNull java.util.concurrent.TimeUnit p3) { return null; }
        java.util.concurrent.ScheduledFuture<?> scheduleWithFixedDelay(@NotNull java.lang.Runnable p0, long p1, long p2, @NotNull java.util.concurrent.TimeUnit p3) { return null; }
        <T> java.util.concurrent.Future<T> submit(@NotNull java.util.concurrent.Callable<T> p0) { return null; }
        java.util.concurrent.Future<?> submit(@NotNull java.lang.Runnable p0) { return null; }
        <T> java.util.concurrent.Future<T> submit(@NotNull java.lang.Runnable p0, T p1) { return null; }
    }

    //public java.util.concurrent.SynchronousQueue (shadow created for its nullability, generated)
    class SynchronousQueue$<E> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        boolean containsAll(@NotNull java.util.Collection<?> p0) { return false; }
        int drainTo(@NotNull java.util.Collection<? super E> p0) { return 0; }
        int drainTo(@NotNull java.util.Collection<? super E> p0, int p1) { return 0; }
        boolean offer(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
        @Nullable E peek() { return null; }
        @Nullable E poll() { return null; }
        @Nullable E poll(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return null; }
        boolean remove(@Nullable java.lang.Object p0) { return false; }
        boolean removeAll(@NotNull java.util.Collection<?> p0) { return false; }
        boolean retainAll(@NotNull java.util.Collection<?> p0) { return false; }
        <T> T[] toArray(@NotNull T[] p0) { return null; }
    }

    //public java.util.concurrent.ThreadPoolExecutor (shadow created for its nullability, generated)
    class ThreadPoolExecutor$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ThreadPoolExecutor$(int p0, int p1, long p2, @NotNull java.util.concurrent.TimeUnit p3, @NotNull java.util.concurrent.BlockingQueue<java.lang.Runnable> p4) { }
        ThreadPoolExecutor$(int p0, int p1, long p2, @NotNull java.util.concurrent.TimeUnit p3, @NotNull java.util.concurrent.BlockingQueue<java.lang.Runnable> p4, @NotNull java.util.concurrent.RejectedExecutionHandler p5) { }
        ThreadPoolExecutor$(int p0, int p1, long p2, @NotNull java.util.concurrent.TimeUnit p3, @NotNull java.util.concurrent.BlockingQueue<java.lang.Runnable> p4, @NotNull java.util.concurrent.ThreadFactory p5) { }
        ThreadPoolExecutor$(int p0, int p1, long p2, @NotNull java.util.concurrent.TimeUnit p3, @NotNull java.util.concurrent.BlockingQueue<java.lang.Runnable> p4, @NotNull java.util.concurrent.ThreadFactory p5, @NotNull java.util.concurrent.RejectedExecutionHandler p6) { }
        void afterExecute(@NotNull java.lang.Runnable p0, @Nullable java.lang.Throwable p1) { }
        boolean awaitTermination(long p0, @NotNull java.util.concurrent.TimeUnit p1) { return false; }
        void beforeExecute(@NotNull java.lang.Thread p0, @NotNull java.lang.Runnable p1) { }
        void execute(@NotNull java.lang.Runnable p0) { }
        long getKeepAliveTime(@NotNull java.util.concurrent.TimeUnit p0) { return 0; }
        boolean remove(@NotNull java.lang.Runnable p0) { return false; }
        void setKeepAliveTime(long p0, @NotNull java.util.concurrent.TimeUnit p1) { }
        void setRejectedExecutionHandler(@NotNull java.util.concurrent.RejectedExecutionHandler p0) { }
        void setThreadFactory(@NotNull java.util.concurrent.ThreadFactory p0) { }

        //public java.util.concurrent.ThreadPoolExecutor.AbortPolicy (shadow created for its nullability, generated)
        class AbortPolicy {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            void rejectedExecution(@NotNull java.lang.Runnable p0, @NotNull java.util.concurrent.ThreadPoolExecutor p1) { }
        }
        //public java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy (shadow created for its nullability, generated)
        class CallerRunsPolicy {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            void rejectedExecution(@NotNull java.lang.Runnable p0, @NotNull java.util.concurrent.ThreadPoolExecutor p1) { }
        }
        //public java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy (shadow created for its nullability, generated)
        class DiscardOldestPolicy {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            void rejectedExecution(@NotNull java.lang.Runnable p0, @NotNull java.util.concurrent.ThreadPoolExecutor p1) { }
        }
        //public java.util.concurrent.ThreadPoolExecutor.DiscardPolicy (shadow created for its nullability, generated)
        class DiscardPolicy {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            void rejectedExecution(@NotNull java.lang.Runnable p0, @NotNull java.util.concurrent.ThreadPoolExecutor p1) { }
        }
    }

    //public java.util.concurrent.TimeoutException (shadow created for its nullability, generated)
    class TimeoutException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        TimeoutException$(@Nullable java.lang.String p0) { }
    }

    //public java.util.concurrent.TransferQueue (shadow created for its nullability, generated)
    class TransferQueue$<E> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        boolean tryTransfer(E p0, long p1, @NotNull java.util.concurrent.TimeUnit p2) { return false; }
    }
}
