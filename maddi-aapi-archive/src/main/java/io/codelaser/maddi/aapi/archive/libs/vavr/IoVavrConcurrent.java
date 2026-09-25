package io.codelaser.maddi.aapi.archive.libs.vavr;
import io.vavr.*;
import io.vavr.collection.Iterator;
import io.vavr.collection.Seq;
import io.vavr.concurrent.Promise;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.FinalFields;
import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.annotation.Identity;
import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.method.GetSet;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;
@Independent(absent = true)
public class IoVavrConcurrent {
    public static final String PACKAGE_NAME = "io.vavr.concurrent";
    //public interface Future implements Value<T>
    //EXPECTED eventually @Immutable(hc = true), after completion -- computed @FinalFields @Dependent, annotated -- G3: completes once (VAVR.md)
    @FinalFields
    @Independent(absent = true)
    class Future$<T> {
        @Independent(absent = true) static final Executor DEFAULT_EXECUTOR = null;
        @Independent @NotModified
        io.vavr.concurrent.Future<T> andThen(@Independent @Modified Consumer<? super Try<T>> action) { return null; }
        @Independent @Modified io.vavr.concurrent.Future<T> await() { return null; }
        @Independent @Modified
        io.vavr.concurrent.Future<T> await(long arg0, @Independent @NotModified TimeUnit arg1) { return null; }
        @Modified boolean cancel() { return false; }
        @Modified boolean cancel(boolean arg0) { return false; }
        @Independent @NotModified
        <R> io.vavr.concurrent.Future<R> collect(
            @Independent @Modified PartialFunction<? super T, ? extends R> partialFunction) { return null; }
        @Independent @NotModified Executor executor() { return null; }
        @Independent(absent = true) @NotModified ExecutorService executorService() { return null; }
        @Independent @NotModified io.vavr.concurrent.Future<Throwable> failed() { return null; }
        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> failed(@Independent @Modified Throwable exception) { return null; }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> failed(
            @Independent @Modified Executor executor,
            @Independent @Modified Throwable exception) { return null; }

        @Independent @NotModified
        io.vavr.concurrent.Future<T> fallbackTo(@Independent @Modified io.vavr.concurrent.Future<? extends T> that) {
            return null;
        }

        @Independent @Modified
        io.vavr.concurrent.Future<T> filter(@Independent @Modified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        io.vavr.concurrent.Future<T> filterTry(@Independent @Modified CheckedPredicate<? super T> predicate) {
            return null;
        }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<Option<T>> find(
            @Independent @Modified Iterable<? extends io.vavr.concurrent.Future<? extends T>> futures,
            @Independent @Modified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<Option<T>> find(
            @Independent @Modified Executor executor,
            @Independent @Modified Iterable<? extends io.vavr.concurrent.Future<? extends T>> futures,
            @Independent @Modified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> firstCompletedOf(
            @Independent @NotModified Iterable<? extends io.vavr.concurrent.Future<? extends T>> futures) { return null; }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> firstCompletedOf(
            @Independent @Modified Executor executor,
            @Independent @NotModified Iterable<? extends io.vavr.concurrent.Future<? extends T>> futures) { return null; }

        @Independent @Modified
        <U> io.vavr.concurrent.Future<U> flatMap(
            @Independent @Modified Function<? super T, ? extends io.vavr.concurrent.Future<? extends U>> mapper) {
            return null;
        }

        @Independent @Modified
        <U> io.vavr.concurrent.Future<U> flatMapTry(
            @Independent @Modified CheckedFunction1<? super T, ? extends io.vavr.concurrent.Future<? extends U>> mapper) {
            return null;
        }

        @Independent @NotModified
        static <T, U> io.vavr.concurrent.Future<U> fold(
            @Independent @Modified Iterable<? extends io.vavr.concurrent.Future<? extends T>> futures,
            @Independent @Modified U zero,
            @Independent @Modified BiFunction<? super U, ? super T, ? extends U> f) { return null; }

        @Independent @NotModified
        static <T, U> io.vavr.concurrent.Future<U> fold(
            @Independent @Modified Executor executor,
            @Independent @Modified Iterable<? extends io.vavr.concurrent.Future<? extends T>> futures,
            @Independent @Modified U zero,
            @Independent @Modified BiFunction<? super U, ? super T, ? extends U> f) { return null; }

        //override from io.vavr.Value, java.lang.Iterable
        @Modified
        void forEach(@Independent @Modified Consumer<? super T> action) { }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> fromCompletableFuture(
            @Independent @Modified CompletableFuture<T> future) { return null; }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> fromCompletableFuture(
            @Independent @Modified Executor executor,
            @Independent @Modified CompletableFuture<T> future) { return null; }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> fromJavaFuture(
            @Independent @Modified Executor executor,
            @Independent @NotModified Future<T> future) { return null; }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> fromJavaFuture(@Independent @NotModified Future<T> future) {
            return null;
        }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> fromTry(@Independent @NotModified Try<? extends T> result) {
            return null;
        }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> fromTry(
            @Independent @Modified Executor executor,
            @Independent @NotModified Try<? extends T> result) { return null; }

        //override from io.vavr.Value
        @Independent @Modified
        T get() { return null; }
        @Independent @NotModified Option<Throwable> getCause() { return null; }
        @Independent(absent = true) @NotModified Option<Try<T>> getValue() { return null; }
        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }
        @NotModified boolean isCancelled() { return false; }
        @NotModified boolean isCompleted() { return false; }
        //override from io.vavr.Value
        @Modified
        boolean isEmpty() { return false; }
        @NotModified boolean isFailure() { return false; }
        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isSingleValued() { return false; }
        @NotModified boolean isSuccess() { return false; }
        //override from io.vavr.Value, java.lang.Iterable
        @Independent @Modified
        Iterator<T> iterator() { return null; }

        //override from io.vavr.Value
        @Independent @NotModified

        <U> io.vavr.concurrent.Future<U> map(@Independent @Modified Function<? super T, ? extends U> mapper) {
            return null;
        }

        //override from io.vavr.Value
        @Independent @NotModified
        <U> io.vavr.concurrent.Future<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value
        @Independent @NotModified
        io.vavr.concurrent.Future<Void> mapToVoid() { return null; }

        @Independent @NotModified
        <U> io.vavr.concurrent.Future<U> mapTry(@Independent @Modified CheckedFunction1<? super T, ? extends U> mapper) {
            return null;
        }

        @Identity @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> narrow(
            @Independent @NotModified io.vavr.concurrent.Future<? extends T> future) { return null; }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> of(@Independent @Modified CheckedFunction0<? extends T> computation) {
            return null;
        }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> of(
            @Independent @Modified Executor executor,
            @Independent @Modified CheckedFunction0<? extends T> computation) { return null; }

        @Independent @Modified
        io.vavr.concurrent.Future<T> onComplete(
            @IgnoreModifications @Independent @Modified Consumer<? super Try<T>> arg0) { return null; }

        @Independent @Modified
        io.vavr.concurrent.Future<T> onFailure(@Independent @Modified Consumer<? super Throwable> action) { return null; }

        @Independent @Modified
        io.vavr.concurrent.Future<T> onSuccess(@Independent @Modified Consumer<? super T> action) { return null; }

        @Independent @Modified
        io.vavr.concurrent.Future<T> orElse(@Independent @Modified io.vavr.concurrent.Future<? extends T> other) {
            return null;
        }

        @Independent @Modified
        io.vavr.concurrent.Future<T> orElse(
            @Independent @Modified Supplier<? extends io.vavr.concurrent.Future<? extends T>> supplier) { return null; }

        //override from io.vavr.Value
        @Fluent @Independent @Modified
        io.vavr.concurrent.Future<T> peek(@Independent @Modified Consumer<? super T> action) { return null; }

        @Independent @NotModified
        io.vavr.concurrent.Future<T> recover(@Independent @Modified Function<? super Throwable, ? extends T> f) {
            return null;
        }

        @Independent @Modified
        io.vavr.concurrent.Future<T> recoverWith(
            @Independent @Modified Function<? super Throwable, ? extends io.vavr.concurrent.Future<? extends T>> f) {
            return null;
        }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> reduce(
            @Independent @Modified Iterable<? extends io.vavr.concurrent.Future<? extends T>> futures,
            @Independent @Modified BiFunction<? super T, ? super T, ? extends T> f) { return null; }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> reduce(
            @Independent @Modified Executor executor,
            @Independent @Modified Iterable<? extends io.vavr.concurrent.Future<? extends T>> futures,
            @Independent @Modified BiFunction<? super T, ? super T, ? extends T> f) { return null; }

        @Independent @NotModified
        static io.vavr.concurrent.Future<Void> run(@Independent @Modified CheckedRunnable unit) { return null; }

        @Independent @NotModified
        static io.vavr.concurrent.Future<Void> run(
            @Independent @Modified Executor executor,
            @Independent @Modified CheckedRunnable unit) { return null; }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<Seq<T>> sequence(
            @Independent @NotModified Iterable<? extends io.vavr.concurrent.Future<? extends T>> futures) { return null; }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<Seq<T>> sequence(
            @Independent @Modified Executor executor,
            @Independent @NotModified Iterable<? extends io.vavr.concurrent.Future<? extends T>> futures) { return null; }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> successful(@Independent @NotModified T result) { return null; }

        @Independent @NotModified
        static <T> io.vavr.concurrent.Future<T> successful(
            @Independent @Modified Executor executor,
            @Independent @NotModified T result) { return null; }

        //override from io.vavr.Value
        @Independent @Modified
        CompletableFuture<T> toCompletableFuture() { return null; }

        @Independent @Modified
        <U> U transform(@Independent @Modified Function<? super io.vavr.concurrent.Future<T>, ? extends U> f) {
            return null;
        }

        @Independent @NotModified
        <U> io.vavr.concurrent.Future<U> transformValue(
            @Independent @Modified Function<? super Try<T>, ? extends Try<? extends U>> f) { return null; }

        @Independent @NotModified
        static <T, U> io.vavr.concurrent.Future<Seq<U>> traverse(
            @Independent @Modified Iterable<? extends T> values,
            @Independent @Modified Function<? super T, ? extends io.vavr.concurrent.Future<? extends U>> mapper) {
            return null;
        }

        @Independent @NotModified
        static <T, U> io.vavr.concurrent.Future<Seq<U>> traverse(
            @Independent @Modified Executor executor,
            @Independent @Modified Iterable<? extends T> values,
            @Independent @Modified Function<? super T, ? extends io.vavr.concurrent.Future<? extends U>> mapper) {
            return null;
        }

        @Independent @NotModified
        <U> io.vavr.concurrent.Future<Tuple2<T, U>> zip(
            @Independent @Modified io.vavr.concurrent.Future<? extends U> that) { return null; }

        @Independent @NotModified
        <U, R> io.vavr.concurrent.Future<R> zipWith(
            @Independent @Modified io.vavr.concurrent.Future<? extends U> that,
            @Independent @Modified BiFunction<? super T, ? super U, ? extends R> combinator) { return null; }
    }

    //public interface Promise
    @FinalFields
    @Independent(absent = true)
    class Promise$<T> {
        @Fluent @Independent @Modified
        Promise<T> complete(@Independent @Modified Try<? extends T> value) { return null; }

        @Fluent @Independent @Modified
        Promise<T> completeWith(@Independent @Modified io.vavr.concurrent.Future<? extends T> other) { return null; }
        @Independent @NotModified Executor executor() { return null; }
        @Independent @NotModified ExecutorService executorService() { return null; }
        @Independent @NotModified
        static <T> Promise<T> failed(@Independent @Modified Throwable exception) { return null; }

        @Independent @NotModified
        static <T> Promise<T> failed(
            @Independent @Modified Executor executor,
            @Independent @Modified Throwable exception) { return null; }
        @Fluent @Independent @Modified Promise<T> failure(@Independent @Modified Throwable exception) { return null; }
        @Independent @NotModified
        static <T> Promise<T> fromTry(@Independent @Modified Try<? extends T> result) { return null; }

        @Independent @NotModified
        static <T> Promise<T> fromTry(
            @Independent @Modified Executor executor,
            @Independent @Modified Try<? extends T> result) { return null; }
        @Independent(absent = true) @NotModified io.vavr.concurrent.Future<T> future() { return null; }
        @NotModified boolean isCompleted() { return false; }
        @Independent @NotModified static <T> Promise<T> make() { return null; }
        @Independent @NotModified static <T> Promise<T> make(@Independent @Modified Executor executor) { return null; }
        @Identity @Independent @NotModified
        static <T> Promise<T> narrow(@Independent @NotModified Promise<? extends T> promise) { return null; }
        @Fluent @Independent @Modified Promise<T> success(@Independent @NotModified T value) { return null; }
        @Independent @NotModified static <T> Promise<T> successful(@Independent @NotModified T result) { return null; }
        @Independent @NotModified
        static <T> Promise<T> successful(@Independent @Modified Executor executor, @Independent @NotModified T result) {
            return null;
        }
        @Modified boolean tryComplete(@Independent(hc = true) @Modified Try<? extends T> arg0) { return false; }
        @Fluent @Independent @Modified
        Promise<T> tryCompleteWith(@Independent @Modified io.vavr.concurrent.Future<? extends T> other) { return null; }
        @Modified boolean tryFailure(@Independent @Modified Throwable exception) { return false; }
        @Modified boolean trySuccess(@Independent @NotModified T value) { return false; }
    }
}
