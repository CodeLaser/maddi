package io.codelaser.maddi.aapi.archive.libs.vavr;
import io.vavr.*;
import io.vavr.collection.Iterator;
import io.vavr.collection.Seq;
import io.vavr.control.*;
import java.util.Optional;
import java.util.concurrent.Callable;
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
public class IoVavrControl {
    public static final String PACKAGE_NAME = "io.vavr.control";
    //public interface Either implements Value<R>, Serializable
    @Immutable(hc = true)
    class Either$<L, R> {
        static final long serialVersionUID = 0L;
        //static class Failure extends Exception
        //annotated as EXPECTED; computed @FinalFields @Container @Dependent -- G4 (computed is right): holds a Throwable, which is mutable (VAVR.md)
        @FinalFields
        @Container
        @Independent(absent = true)
        class Failure {
            Failure(@Independent(hc = true) @NotModified Object value) { }
            @Independent(hc = true) @NotModified @GetSet("value") Object getValue() { return null; }
        }

        //static final class Left implements Either<L,R>, Serializable
        @ImmutableContainer(hc = true)
        class Left<L, R> {
            //override from io.vavr.Value, io.vavr.control.Either, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object obj) { return false; }

            //override from io.vavr.Value, io.vavr.control.Either
            @Independent @NotModified
            R get() { return null; }

            //override from io.vavr.control.Either
            @Independent(hc = true) @NotModified @GetSet("value")
            L getLeft() { return null; }

            //override from io.vavr.Value, io.vavr.control.Either, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from io.vavr.control.Either
            @NotModified
            boolean isLeft() { return false; }

            //override from io.vavr.control.Either
            @NotModified
            boolean isRight() { return false; }

            //override from io.vavr.Value
            @NotModified
            String stringPrefix() { return null; }

            //override from io.vavr.Value, io.vavr.control.Either, java.lang.Object
            @NotModified
            public String toString() { return null; }
        }

        //static final class LeftProjection implements Value<L>
        @Immutable(hc = true)
        @Independent
        class LeftProjection<L, R> {
            @Independent @NotModified
            <L2, R2> Either.LeftProjection<L2, R2> bimap(
                @Independent @Modified Function<? super L, ? extends L2> leftMapper,
                @Independent @Modified Function<? super R, ? extends R2> rightMapper) { return null; }

            //override from io.vavr.Value, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object obj) { return false; }

            @Fluent @Independent @NotModified
            Option<Either.LeftProjection<L, R>> filter(@Independent @Modified Predicate<? super L> predicate) {
                return null;
            }

            @Fluent @Independent @NotModified
            <U> Either.LeftProjection<U, R> flatMap(
                @Independent @Modified Function<? super L, ? extends Either.LeftProjection<? extends U, R>> mapper) {
                return null;
            }

            //override from io.vavr.Value
            @Independent(hc = true) @NotModified
            L get() { return null; }

            //override from io.vavr.Value
            @Independent(hc = true) @NotModified
            L getOrElse(@Independent(hc = true) @NotModified L other) { return null; }

            @Independent(hc = true) @NotModified
            L getOrElseGet(@Independent @Modified Function<? super R, ? extends L> other) { return null; }

            @Independent(hc = true) @NotModified
            <X extends Throwable> L getOrElseThrow(@Independent @Modified Function<? super R, X> exceptionFunction) {
                return null;
            }

            //override from io.vavr.Value, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from io.vavr.Value
            @NotModified
            boolean isAsync() { return false; }

            //override from io.vavr.Value
            @NotModified
            boolean isEmpty() { return false; }

            //override from io.vavr.Value
            @NotModified
            boolean isLazy() { return false; }

            //override from io.vavr.Value
            @NotModified
            boolean isSingleValued() { return false; }

            //override from io.vavr.Value, java.lang.Iterable
            @Independent @NotModified
            Iterator<L> iterator() { return null; }

            //override from io.vavr.Value
            @Fluent @Independent @Modified @NotModified(after = "either")

            <U> Either.LeftProjection<U, R> map(
                @Independent(absent = true) @Modified Function<? super L, ? extends U> mapper) { return null; }

            @Fluent @Independent @NotModified
            Either.LeftProjection<L, R> orElse(
                @Independent @NotModified Either.LeftProjection<? extends L, ? extends R> other) { return null; }

            @Fluent @Independent @NotModified
            Either.LeftProjection<L, R> orElse(
                @Independent @Modified Supplier<? extends Either.LeftProjection<? extends L, ? extends R>> supplier) {
                return null;
            }
            @NotModified void orElseRun(@Independent @Modified Consumer<? super R> action) { }
            //override from io.vavr.Value
            @Fluent @Independent @NotModified
            Either.LeftProjection<L, R> peek(@Independent @Modified Consumer<? super L> action) { return null; }

            //override from io.vavr.Value
            @NotModified
            String stringPrefix() { return null; }
            @Independent(hc = true) @NotModified @GetSet("either") Either<L, R> toEither() { return null; }
            //override from io.vavr.Value, java.lang.Object
            @NotModified
            public String toString() { return null; }

            @Independent @Modified
            <U> U transform(@Independent @Modified Function<? super Either.LeftProjection<L, R>, ? extends U> f) {
                return null;
            }
        }

        //static final class Right implements Either<L,R>, Serializable
        @ImmutableContainer(hc = true)
        class Right<L, R> {
            //override from io.vavr.Value, io.vavr.control.Either, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object obj) { return false; }

            //override from io.vavr.Value, io.vavr.control.Either
            @Independent(hc = true) @NotModified @GetSet("value")
            R get() { return null; }

            //override from io.vavr.control.Either
            @Independent @NotModified
            L getLeft() { return null; }

            //override from io.vavr.Value, io.vavr.control.Either, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from io.vavr.control.Either
            @NotModified
            boolean isLeft() { return false; }

            //override from io.vavr.control.Either
            @NotModified
            boolean isRight() { return false; }

            //override from io.vavr.Value
            @NotModified
            String stringPrefix() { return null; }

            //override from io.vavr.Value, io.vavr.control.Either, java.lang.Object
            @NotModified
            public String toString() { return null; }
        }

        //static final class RightProjection implements Value<R>
        @Immutable(hc = true)
        class RightProjection<L, R> {
            @Independent @NotModified
            <L2, R2> Either.RightProjection<L2, R2> bimap(
                @Independent @Modified Function<? super L, ? extends L2> leftMapper,
                @Independent @Modified Function<? super R, ? extends R2> rightMapper) { return null; }

            //override from io.vavr.Value, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object obj) { return false; }

            @Fluent @Independent @NotModified
            Option<Either.RightProjection<L, R>> filter(@Independent @Modified Predicate<? super R> predicate) {
                return null;
            }

            @Fluent @Independent @NotModified
            <U> Either.RightProjection<L, U> flatMap(
                @Independent @Modified Function<? super R, ? extends Either.RightProjection<L, ? extends U>> mapper) {
                return null;
            }

            //override from io.vavr.Value
            @Independent(hc = true) @NotModified
            R get() { return null; }

            //override from io.vavr.Value
            @Independent(hc = true) @Modified @NotModified(after = "either")
            R getOrElse(@Independent(hc = true) @NotModified R other) { return null; }

            @Independent(hc = true) @NotModified
            R getOrElseGet(@Independent(absent = true) @Modified Function<? super L, ? extends R> other) { return null; }

            @Independent(hc = true) @NotModified
            <X extends Throwable> R getOrElseThrow(@Independent @Modified Function<? super L, X> exceptionFunction) {
                return null;
            }

            //override from io.vavr.Value, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from io.vavr.Value
            @NotModified
            boolean isAsync() { return false; }

            //override from io.vavr.Value
            @NotModified
            boolean isEmpty() { return false; }

            //override from io.vavr.Value
            @NotModified
            boolean isLazy() { return false; }

            //override from io.vavr.Value
            @NotModified
            boolean isSingleValued() { return false; }

            //override from io.vavr.Value, java.lang.Iterable
            @Independent @NotModified
            Iterator<R> iterator() { return null; }

            //override from io.vavr.Value
            @Fluent @Independent @Modified @NotModified(after = "either")

            <U> Either.RightProjection<L, U> map(
                @Independent(absent = true) @Modified Function<? super R, ? extends U> mapper) { return null; }

            @Fluent @Independent @NotModified
            Either.RightProjection<L, R> orElse(
                @Independent @NotModified Either.RightProjection<? extends L, ? extends R> other) { return null; }

            @Fluent @Independent @NotModified
            Either.RightProjection<L, R> orElse(
                @Independent @Modified Supplier<? extends Either.RightProjection<? extends L, ? extends R>> supplier) {
                return null;
            }
            @NotModified void orElseRun(@Independent @Modified Consumer<? super L> action) { }
            //override from io.vavr.Value
            @Fluent @Independent @NotModified
            Either.RightProjection<L, R> peek(@Independent @Modified Consumer<? super R> action) { return null; }

            //override from io.vavr.Value
            @NotModified
            String stringPrefix() { return null; }
            @Independent(hc = true) @NotModified @GetSet("either") Either<L, R> toEither() { return null; }
            //override from io.vavr.Value, java.lang.Object
            @NotModified
            public String toString() { return null; }

            @Independent @Modified
            <U> U transform(@Independent @Modified Function<? super Either.RightProjection<L, R>, ? extends U> f) {
                return null;
            }
        }

        @Independent @NotModified
        <X, Y> Either<X, Y> bimap(
            @Independent @Modified Function<? super L, ? extends X> leftMapper,
            @Independent @Modified Function<? super R, ? extends Y> rightMapper) { return null; }

        @Independent @NotModified
        static <L, R> Either<L, R> cond(
            boolean test,
            @Independent @NotModified R right,
            @Independent @NotModified L left) { return null; }

        @Independent @NotModified
        static <L, R> Either<L, R> cond(
            boolean test,
            @Independent @Modified Supplier<? extends R> right,
            @Independent @Modified Supplier<? extends L> left) { return null; }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object arg0) { return false; }

        @Fluent @Independent @NotModified
        Option<Either<L, R>> filter(@Independent @Modified Predicate<? super R> predicate) { return null; }

        @Fluent @Independent @NotModified
        Either<L, R> filterOrElse(
            @Independent @Modified Predicate<? super R> predicate,
            @Independent @Modified Function<? super R, ? extends L> zero) { return null; }

        @Fluent @Independent @NotModified
        <U> Either<L, U> flatMap(@Independent @Modified Function<? super R, ? extends Either<L, ? extends U>> mapper) {
            return null;
        }

        @Independent @NotModified
        <U> U fold(
            @Independent @Modified Function<? super L, ? extends U> leftMapper,
            @Independent @Modified Function<? super R, ? extends U> rightMapper) { return null; }

        //override from io.vavr.Value
        @Independent(hc = true) @NotModified
        R get() { return null; }
        @Independent(hc = true) @NotModified L getLeft() { return null; }
        @Independent @NotModified
        R getOrElseGet(@Independent @Modified Function<? super L, ? extends R> other) { return null; }

        @Independent @NotModified
        <X extends Throwable> R getOrElseThrow(@Independent @Modified Function<? super L, X> exceptionFunction) {
            return null;
        }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }
        @NotModified boolean isLeft() { return false; }
        @NotModified boolean isRight() { return false; }
        //override from io.vavr.Value
        @NotModified
        boolean isSingleValued() { return false; }

        //override from io.vavr.Value, java.lang.Iterable
        @Independent @NotModified
        Iterator<R> iterator() { return null; }
        @Fluent @Independent @Modified Either.LeftProjection<L, R> left() { return null; }
        @Independent @NotModified static <L, R> Either<L, R> left(@Independent @NotModified L left) { return null; }
        //override from io.vavr.Value
        @Fluent @Independent @NotModified
        <U> Either<L, U> map(@Independent @Modified Function<? super R, ? extends U> mapper) { return null; }

        @Fluent @Independent @NotModified
        <U> Either<U, R> mapLeft(@Independent @Modified Function<? super L, ? extends U> leftMapper) { return null; }

        //override from io.vavr.Value
        @Fluent @Independent @NotModified
        <U> Either<L, U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value
        @Fluent @Independent @NotModified
        Either<L, Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <L, R> Either<L, R> narrow(@Independent @NotModified Either<? extends L, ? extends R> either) {
            return null;
        }

        @Fluent @Independent @NotModified
        Either<L, R> orElse(@Independent @NotModified Either<? extends L, ? extends R> other) { return null; }

        @Fluent @Independent @NotModified
        Either<L, R> orElse(@Independent @Modified Supplier<? extends Either<? extends L, ? extends R>> supplier) {
            return null;
        }
        @NotModified void orElseRun(@Independent @Modified Consumer<? super L> action) { }
        //override from io.vavr.Value
        @Fluent @Independent @NotModified
        Either<L, R> peek(@Independent @Modified Consumer<? super R> action) { return null; }

        @Fluent @Independent @NotModified
        Either<L, R> peekLeft(@Independent @Modified Consumer<? super L> action) { return null; }
        @Fluent @Independent @Modified Either.RightProjection<L, R> right() { return null; }
        @Independent @NotModified static <L, R> Either<L, R> right(@Independent @NotModified R right) { return null; }
        @Independent @NotModified
        static <L, R> Either<Seq<L>, Seq<R>> sequence(
            @Independent @NotModified Iterable<? extends Either<? extends L, ? extends R>> eithers) { return null; }

        @Independent @NotModified
        static <L, R> Either<L, Seq<R>> sequenceRight(
            @Independent @NotModified Iterable<? extends Either<? extends L, ? extends R>> eithers) { return null; }
        @Independent @NotModified Either<R, L> swap() { return null; }
        //override from io.vavr.Value, java.lang.Object
        @NotModified @NotNull
        public String toString() { return null; }

        //override from io.vavr.Value
        @Independent @NotModified
        Try<R> toTry() { return null; }
        @Independent @NotModified Validation<L, R> toValidation() { return null; }
        @Independent @NotModified
        static <L, R, T> Either<Seq<L>, Seq<R>> traverse(
            @Independent @Modified Iterable<? extends T> values,
            @Independent @Modified Function<? super T, ? extends Either<? extends L, ? extends R>> mapper) {
            return null;
        }

        @Independent @NotModified
        static <L, R, T> Either<L, Seq<R>> traverseRight(
            @Independent @Modified Iterable<? extends T> values,
            @Independent @Modified Function<? super T, ? extends Either<? extends L, ? extends R>> mapper) {
            return null;
        }
    }

    //public interface HashCodes
    @ImmutableContainer(hc = true)
    @Independent
    class HashCodes$ {
        @NotModified static int hash(@Independent @NotModified Object value) { return 0; }
        @NotModified
        static int hash(@Independent @NotModified Object v1, @Independent @NotModified Object v2) { return 0; }

        @NotModified
        static int hash(
            @Independent @NotModified Object v1,
            @Independent @NotModified Object v2,
            @Independent @NotModified Object v3) { return 0; }

        @NotModified
        static int hash(
            @Independent @NotModified Object v1,
            @Independent @NotModified Object v2,
            @Independent @NotModified Object v3,
            @Independent @NotModified Object v4) { return 0; }

        @NotModified
        static int hash(
            @Independent @NotModified Object v1,
            @Independent @NotModified Object v2,
            @Independent @NotModified Object v3,
            @Independent @NotModified Object v4,
            @Independent @NotModified Object v5) { return 0; }

        @NotModified
        static int hash(
            @Independent @NotModified Object v1,
            @Independent @NotModified Object v2,
            @Independent @NotModified Object v3,
            @Independent @NotModified Object v4,
            @Independent @NotModified Object v5,
            @Independent @NotModified Object v6) { return 0; }

        @NotModified
        static int hash(
            @Independent @NotModified Object v1,
            @Independent @NotModified Object v2,
            @Independent @NotModified Object v3,
            @Independent @NotModified Object v4,
            @Independent @NotModified Object v5,
            @Independent @NotModified Object v6,
            @Independent @NotModified Object v7) { return 0; }

        @NotModified
        static int hash(
            @Independent @NotModified Object v1,
            @Independent @NotModified Object v2,
            @Independent @NotModified Object v3,
            @Independent @NotModified Object v4,
            @Independent @NotModified Object v5,
            @Independent @NotModified Object v6,
            @Independent @NotModified Object v7,
            @Independent @NotModified Object v8) { return 0; }
        @NotModified static int hash(boolean value) { return 0; }
        @NotModified static int hash(boolean v1, @Independent @NotModified Object v2) { return 0; }
        @NotModified static int hash(byte value) { return 0; }
        @NotModified static int hash(byte v1, @Independent @NotModified Object v2) { return 0; }
        @NotModified static int hash(char value) { return 0; }
        @NotModified static int hash(char v1, @Independent @NotModified Object v2) { return 0; }
        @NotModified static int hash(double value) { return 0; }
        @NotModified static int hash(double v1, @Independent @NotModified Object v2) { return 0; }
        @NotModified static int hash(float value) { return 0; }
        @NotModified static int hash(float v1, @Independent @NotModified Object v2) { return 0; }
        @NotModified static int hash(int value) { return 0; }
        @NotModified static int hash(int v1, @Independent @NotModified Object v2) { return 0; }
        @NotModified static int hash(int v1, int v2) { return 0; }
        @NotModified static int hash(long value) { return 0; }
        @NotModified static int hash(long v1, @Independent @NotModified Object v2) { return 0; }
        @NotModified static int hash(short value) { return 0; }
        @NotModified static int hash(short v1, @Independent @NotModified Object v2) { return 0; }
    }

    //public interface Option implements Value<T>, Serializable
    @Immutable(hc = true)
    class Option$<T> {
        static final long serialVersionUID = 0L;
        //static final class None implements Option<T>, Serializable
        @ImmutableContainer
        class None<T> {
            //override from io.vavr.Value, io.vavr.control.Option, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object o) { return false; }

            //override from io.vavr.Value, io.vavr.control.Option
            @Independent @NotModified
            T get() { return null; }

            //override from io.vavr.Value, io.vavr.control.Option, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from io.vavr.Value, io.vavr.control.Option
            @NotModified
            boolean isEmpty() { return false; }

            //override from io.vavr.Value
            @NotModified
            String stringPrefix() { return null; }

            //override from io.vavr.Value, io.vavr.control.Option, java.lang.Object
            @NotModified
            public String toString() { return null; }
        }

        //static final class Some implements Option<T>, Serializable
        @ImmutableContainer(hc = true)
        class Some<T> {
            //override from io.vavr.Value, io.vavr.control.Option, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object obj) { return false; }

            //override from io.vavr.Value, io.vavr.control.Option
            @Independent(hc = true) @NotModified @GetSet("value")
            T get() { return null; }

            //override from io.vavr.Value, io.vavr.control.Option, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from io.vavr.Value, io.vavr.control.Option
            @NotModified
            boolean isEmpty() { return false; }

            //override from io.vavr.Value
            @NotModified
            String stringPrefix() { return null; }

            //override from io.vavr.Value, io.vavr.control.Option, java.lang.Object
            @NotModified
            public String toString() { return null; }
        }

        @Independent @NotModified
        <R> Option<R> collect(@Independent @NotModified PartialFunction<? super T, ? extends R> partialFunction) {
            return null;
        }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object arg0) { return false; }

        @Fluent @Independent @NotModified
        Option<T> filter(@Independent @Modified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        <U> Option<U> flatMap(@Independent @Modified Function<? super T, ? extends Option<? extends U>> mapper) {
            return null;
        }

        @Independent @NotModified
        <U> U fold(
            @Independent @Modified Supplier<? extends U> ifNone,
            @Independent @Modified Function<? super T, ? extends U> f) { return null; }

        //override from io.vavr.Value
        @Independent(hc = true) @NotModified
        T get() { return null; }

        //override from io.vavr.Value
        @Independent @NotModified
        T getOrElse(@Independent @NotModified T other) { return null; }

        //override from io.vavr.Value
        @Independent @NotModified
        T getOrElse(@Independent @Modified Supplier<? extends T> supplier) { return null; }

        //override from io.vavr.Value
        @Independent @NotModified
        <X extends Throwable> T getOrElseThrow(@Independent @Modified Supplier<X> exceptionSupplier) { return null; }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }
        @NotModified boolean isDefined() { return false; }
        //override from io.vavr.Value
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isSingleValued() { return false; }

        //override from io.vavr.Value, java.lang.Iterable
        @Independent @NotModified
        Iterator<T> iterator() { return null; }

        //override from io.vavr.Value
        @Independent @NotModified
        <U> Option<U> map(@Independent @Modified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.Value
        @Independent @NotModified
        <U> Option<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value
        @Independent @NotModified
        Option<Void> mapToVoid() { return null; }

        @Fluent @Independent @Modified
        <U> Try<U> mapTry(@Independent @Modified CheckedFunction1<? super T, ? extends U> mapper) { return null; }

        @Identity @Independent @NotModified
        static <T> Option<T> narrow(@Independent @NotModified Option<? extends T> option) { return null; }
        @Independent @NotModified static <T> Option<T> none() { return null; }
        @Independent @NotModified static <T> Option<T> of(@Independent @NotModified T value) { return null; }
        @Independent @NotModified
        static <T> Option<T> ofOptional(@Independent @NotModified Optional<? extends T> optional) { return null; }
        @Fluent @Independent @NotModified Option<T> onEmpty(@Independent @Modified Runnable action) { return null; }
        @Fluent @Independent @NotModified
        Option<T> orElse(@Independent @NotModified Option<? extends T> other) { return null; }

        @Fluent @Independent @NotModified
        Option<T> orElse(@Independent @Modified Supplier<? extends Option<? extends T>> supplier) { return null; }

        //override from io.vavr.Value
        @Fluent @Independent @NotModified
        Option<T> peek(@Independent @Modified Consumer<? super T> action) { return null; }

        @Independent @NotModified
        static <T> Option<Seq<T>> sequence(@Independent @NotModified Iterable<? extends Option<? extends T>> values) {
            return null;
        }
        @Independent @NotModified static <T> Option<T> some(@Independent @NotModified T value) { return null; }
        //override from io.vavr.Value, java.lang.Object
        @NotModified @NotNull
        public String toString() { return null; }

        @Independent @Modified
        <U> U transform(@Independent @Modified Function<? super Option<T>, ? extends U> f) { return null; }

        @Independent @NotModified
        static <T, U> Option<Seq<U>> traverse(
            @Independent @Modified Iterable<? extends T> values,
            @Independent @Modified Function<? super T, ? extends Option<? extends U>> mapper) { return null; }

        @Independent @NotModified
        static <T> Option<T> when(boolean condition, @Independent @NotModified T value) { return null; }

        @Independent @NotModified
        static <T> Option<T> when(boolean condition, @Independent @Modified Supplier<? extends T> supplier) {
            return null;
        }
    }

    //public interface Try implements Value<T>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G4 (computed is right): holds a Throwable, which is mutable (VAVR.md)
    @FinalFields
    @Independent(absent = true)
    class Try$<T> {
        static final long serialVersionUID = 0L;
        //static final class Failure implements Try<T>, Serializable
        //annotated as EXPECTED; computed @FinalFields @Container @Dependent -- G4 (computed is right): holds a Throwable, which is mutable (VAVR.md)
        @FinalFields
        @Container
        @Independent(absent = true)
        class Failure<T> {
            //override from io.vavr.Value, io.vavr.control.Try, java.lang.Object
            @Modified
            public boolean equals(@Independent @NotModified Object obj) { return false; }

            //override from io.vavr.Value, io.vavr.control.Try
            @Independent @NotModified
            T get() { return null; }

            //override from io.vavr.control.Try
            @Independent(absent = true) @NotModified @GetSet("cause")
            Throwable getCause() { return null; }

            //override from io.vavr.Value, io.vavr.control.Try, java.lang.Object
            @Modified
            public int hashCode() { return 0; }

            //override from io.vavr.Value, io.vavr.control.Try
            @NotModified
            boolean isEmpty() { return false; }

            //override from io.vavr.control.Try
            @NotModified
            boolean isFailure() { return false; }

            //override from io.vavr.control.Try
            @NotModified
            boolean isSuccess() { return false; }

            //override from io.vavr.Value
            @NotModified
            String stringPrefix() { return null; }

            //override from io.vavr.Value, io.vavr.control.Try, java.lang.Object
            @NotModified
            public String toString() { return null; }
        }

        //static final class Success implements Try<T>, Serializable
        @ImmutableContainer(hc = true)
        class Success<T> {
            //override from io.vavr.Value, io.vavr.control.Try, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object obj) { return false; }

            //override from io.vavr.Value, io.vavr.control.Try
            @Independent(hc = true) @NotModified @GetSet("value")
            T get() { return null; }

            //override from io.vavr.control.Try
            @Independent @NotModified
            Throwable getCause() { return null; }

            //override from io.vavr.Value, io.vavr.control.Try, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from io.vavr.Value, io.vavr.control.Try
            @NotModified
            boolean isEmpty() { return false; }

            //override from io.vavr.control.Try
            @NotModified
            boolean isFailure() { return false; }

            //override from io.vavr.control.Try
            @NotModified
            boolean isSuccess() { return false; }

            //override from io.vavr.Value
            @NotModified
            String stringPrefix() { return null; }

            //override from io.vavr.Value, io.vavr.control.Try, java.lang.Object
            @NotModified
            public String toString() { return null; }
        }

        //static final class WithResources1
        @FinalFields
        @Independent
        class WithResources1<T1 extends AutoCloseable> {
            @Independent @NotModified
            <R> Try<R> of(@Independent @Modified CheckedFunction1<? super T1, ? extends R> f) { return null; }
        }

        //static final class WithResources2
        @FinalFields
        @Independent
        class WithResources2<T1 extends AutoCloseable, T2 extends AutoCloseable> {
            @Independent @NotModified
            <R> Try<R> of(@Independent @Modified CheckedFunction2<? super T1, ? super T2, ? extends R> f) { return null; }
        }

        //static final class WithResources3
        @FinalFields
        @Independent
        class WithResources3<T1 extends AutoCloseable, T2 extends AutoCloseable, T3 extends AutoCloseable> {
            @Independent @NotModified
            <R> Try<R> of(@Independent @Modified CheckedFunction3<? super T1, ? super T2, ? super T3, ? extends R> f) {
                return null;
            }
        }

        //static final class WithResources4
        @FinalFields
        @Independent
        class WithResources4<
            T1 extends AutoCloseable,
            T2 extends AutoCloseable,
            T3 extends AutoCloseable,
            T4 extends AutoCloseable> {
            @Independent @NotModified
            <R> Try<R> of(
                @Independent @Modified CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
                return null;
            }
        }

        //static final class WithResources5
        @FinalFields
        @Independent
        class WithResources5<
            T1 extends AutoCloseable,
            T2 extends AutoCloseable,
            T3 extends AutoCloseable,
            T4 extends AutoCloseable,
            T5 extends AutoCloseable> {
            @Independent @NotModified
            <R> Try<R> of(
                @Independent @Modified CheckedFunction5<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? extends R> f) { return null; }
        }

        //static final class WithResources6
        @FinalFields
        @Independent
        class WithResources6<
            T1 extends AutoCloseable,
            T2 extends AutoCloseable,
            T3 extends AutoCloseable,
            T4 extends AutoCloseable,
            T5 extends AutoCloseable,
            T6 extends AutoCloseable> {
            @Independent @NotModified
            <R> Try<R> of(
                @Independent @Modified CheckedFunction6<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? extends R> f) { return null; }
        }

        //static final class WithResources7
        @FinalFields
        @Independent
        class WithResources7<
            T1 extends AutoCloseable,
            T2 extends AutoCloseable,
            T3 extends AutoCloseable,
            T4 extends AutoCloseable,
            T5 extends AutoCloseable,
            T6 extends AutoCloseable,
            T7 extends AutoCloseable> {
            @Independent @NotModified
            <R> Try<R> of(
                @Independent @Modified CheckedFunction7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R> f) { return null; }
        }

        //static final class WithResources8
        @FinalFields
        @Independent
        class WithResources8<
            T1 extends AutoCloseable,
            T2 extends AutoCloseable,
            T3 extends AutoCloseable,
            T4 extends AutoCloseable,
            T5 extends AutoCloseable,
            T6 extends AutoCloseable,
            T7 extends AutoCloseable,
            T8 extends AutoCloseable> {
            @Independent @NotModified
            <R> Try<R> of(
                @Independent @Modified CheckedFunction8<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? super T8,
                    ? extends R> f) { return null; }
        }
        @Fluent @Independent @NotModified Try<T> andFinally(@Independent @Modified Runnable runnable) { return null; }
        @Fluent @Independent @NotModified
        Try<T> andFinallyTry(@Independent @Modified CheckedRunnable runnable) { return null; }
        @Fluent @Independent @NotModified Try<T> andThen(@Independent @Modified Runnable runnable) { return null; }
        @Fluent @Independent @Modified
        Try<T> andThen(@Independent @Modified Consumer<? super T> consumer) { return null; }

        @Fluent @Independent @NotModified
        Try<T> andThenTry(@Independent @Modified CheckedConsumer<? super T> consumer) { return null; }

        @Fluent @Independent @NotModified
        Try<T> andThenTry(@Independent @Modified CheckedRunnable runnable) { return null; }

        @Fluent @Independent @Modified
        <R> Try<R> collect(@Independent @Modified PartialFunction<? super T, ? extends R> partialFunction) {
            return null;
        }

        //override from io.vavr.Value, java.lang.Object
        @Modified
        public boolean equals(@Independent @NotModified Object arg0) { return false; }
        @Independent @NotModified Try<Throwable> failed() { return null; }
        @Independent @NotModified
        static <T> Try<T> failure(@Independent @Modified Throwable exception) { return null; }

        @Fluent @Independent @Modified
        Try<T> filter(@Independent @Modified Predicate<? super T> predicate) { return null; }

        @Fluent @Independent @Modified
        Try<T> filter(
            @Independent @Modified Predicate<? super T> predicate,
            @Independent @Modified Function<? super T, ? extends Throwable> errorProvider) { return null; }

        @Fluent @Independent @Modified
        Try<T> filter(
            @Independent @Modified Predicate<? super T> predicate,
            @Independent @Modified Supplier<? extends Throwable> throwableSupplier) { return null; }

        @Fluent @Independent @NotModified
        Try<T> filterTry(@Independent @Modified CheckedPredicate<? super T> predicate) { return null; }

        @Fluent @Independent @NotModified
        Try<T> filterTry(
            @Independent @Modified CheckedPredicate<? super T> predicate,
            @Independent @Modified CheckedFunction1<? super T, ? extends Throwable> errorProvider) { return null; }

        @Fluent @Independent @NotModified
        Try<T> filterTry(
            @Independent @Modified CheckedPredicate<? super T> predicate,
            @Independent @Modified Supplier<? extends Throwable> throwableSupplier) { return null; }

        @Fluent @Independent @Modified
        <U> Try<U> flatMap(@Independent @Modified Function<? super T, ? extends Try<? extends U>> mapper) { return null; }

        @Fluent @Independent @NotModified
        <U> Try<U> flatMapTry(@Independent @Modified CheckedFunction1<? super T, ? extends Try<? extends U>> mapper) {
            return null;
        }

        @Independent @NotModified
        <X> X fold(
            @Independent @Modified Function<? super Throwable, ? extends X> ifFail,
            @Independent @Modified Function<? super T, ? extends X> f) { return null; }

        //override from io.vavr.Value
        @Independent(hc = true) @NotModified
        T get() { return null; }
        @Independent(absent = true) @NotModified Throwable getCause() { return null; }
        @Independent @NotModified
        T getOrElseGet(@Independent @Modified Function<? super Throwable, ? extends T> other) { return null; }

        @Independent @NotModified
        <X extends Throwable> T getOrElseThrow(@Independent @Modified Function<? super Throwable, X> exceptionProvider) {
            return null;
        }

        //override from io.vavr.Value, java.lang.Object
        @Modified
        public int hashCode() { return 0; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value
        @NotModified
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
        @Independent @NotModified
        Iterator<T> iterator() { return null; }

        //override from io.vavr.Value
        @Fluent @Independent @Modified
        <U> Try<U> map(@Independent @Modified Function<? super T, ? extends U> mapper) { return null; }

        @Fluent @Independent @NotModified
        Try<T> mapFailure(@Independent @NotModified API.Match.Case<? extends Throwable, ? extends Throwable> ... cases) {
            return null;
        }

        //override from io.vavr.Value
        @Fluent @Independent @Modified
        <U> Try<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value
        @Fluent @Independent @Modified
        Try<Void> mapToVoid() { return null; }

        @Fluent @Independent @NotModified
        <U> Try<U> mapTry(@Independent @Modified CheckedFunction1<? super T, ? extends U> mapper) { return null; }

        @Identity @Independent @NotModified
        static <T> Try<T> narrow(@Independent @NotModified Try<? extends T> t) { return null; }

        @Independent @NotModified
        static <T> Try<T> of(@Independent @Modified CheckedFunction0<? extends T> supplier) { return null; }

        @Independent @NotModified
        static <T> Try<T> ofCallable(@Independent @Modified Callable<? extends T> callable) { return null; }

        @Independent @NotModified
        static <T> Try<T> ofSupplier(@Independent @Modified Supplier<? extends T> supplier) { return null; }

        @Fluent @Independent @NotModified
        <X extends Throwable> Try<T> onFailure(
            Class<X> exceptionType,
            @Independent @Modified Consumer<? super X> action) { return null; }

        @Fluent @Independent @NotModified
        Try<T> onFailure(@Independent @Modified Consumer<? super Throwable> action) { return null; }

        @Fluent @Independent @NotModified
        Try<T> onSuccess(@Independent @Modified Consumer<? super T> action) { return null; }

        @Fluent @Independent @NotModified
        Try<T> orElse(@Independent @NotModified Try<? extends T> other) { return null; }

        @Fluent @Independent @NotModified
        Try<T> orElse(@Independent @Modified Supplier<? extends Try<? extends T>> supplier) { return null; }
        @NotModified void orElseRun(@Independent @Modified Consumer<? super Throwable> action) { }
        //override from io.vavr.Value
        @Fluent @Independent @NotModified
        Try<T> peek(@Independent @Modified Consumer<? super T> action) { return null; }

        @Fluent @Independent @NotModified
        <X extends Throwable> Try<T> recover(Class<X> exceptionType, @Independent @NotModified T value) { return null; }

        @Fluent @Independent @NotModified
        <X extends Throwable> Try<T> recover(
            Class<X> exceptionType,
            @Independent @Modified Function<? super X, ? extends T> f) { return null; }

        @Fluent @Independent @NotModified
        Try<T> recover(@Independent @Modified Function<? super Throwable, ? extends T> f) { return null; }

        @Fluent @Independent @NotModified
        Try<T> recoverAllAndTry(@Independent @Modified CheckedFunction0<? extends T> recoveryAttempt) { return null; }

        @Fluent @Independent @NotModified
        <X extends Throwable> Try<T> recoverAndTry(
            Class<X> exceptionType,
            @Independent @Modified CheckedFunction0<? extends T> recoveryAttempt) { return null; }

        @Fluent @Independent @NotModified
        <X extends Throwable> Try<T> recoverWith(
            Class<X> exceptionType,
            @Independent @NotModified Try<? extends T> recovered) { return null; }

        @Fluent @Independent @NotModified
        <X extends Throwable> Try<T> recoverWith(
            Class<X> exceptionType,
            @Independent @Modified Function<? super X, Try<? extends T>> f) { return null; }

        @Fluent @Independent @NotModified
        Try<T> recoverWith(@Independent @Modified Function<? super Throwable, ? extends Try<? extends T>> f) {
            return null;
        }

        @Independent @NotModified
        static Try<Void> run(@Independent @Modified CheckedRunnable runnable) { return null; }

        @Independent @NotModified
        static Try<Void> runRunnable(@Independent @Modified Runnable runnable) { return null; }

        @Independent @NotModified
        static <T> Try<Seq<T>> sequence(@Independent @NotModified Iterable<? extends Try<? extends T>> values) {
            return null;
        }
        @Independent @NotModified static <T> Try<T> success(@Independent @NotModified T value) { return null; }
        @Independent @NotModified Either<Throwable, T> toEither() { return null; }
        @Independent @NotModified
        <L> Either<L, T> toEither(@Independent @Modified Function<? super Throwable, ? extends L> throwableMapper) {
            return null;
        }

        //override from io.vavr.Value, java.lang.Object
        @NotModified @NotNull
        public String toString() { return null; }
        @Independent @NotModified Validation<Throwable, T> toValidation() { return null; }
        @Independent @NotModified
        <U> Validation<U, T> toValidation(
            @Independent @Modified Function<? super Throwable, ? extends U> throwableMapper) { return null; }

        @Independent @Modified
        <U> U transform(@Independent @Modified Function<? super Try<T>, ? extends U> f) { return null; }

        @Independent @NotModified
        static <T, U> Try<Seq<U>> traverse(
            @Independent @Modified Iterable<? extends T> values,
            @Independent @Modified Function<? super T, ? extends Try<? extends U>> mapper) { return null; }

        @Independent @NotModified
        static <T1 extends AutoCloseable> Try.WithResources1<T1> withResources(
            @Independent @Modified CheckedFunction0<? extends T1> t1Supplier) { return null; }

        @Independent @NotModified
        static <T1 extends AutoCloseable, T2 extends AutoCloseable> Try.WithResources2<T1, T2> withResources(
            @Independent @Modified CheckedFunction0<? extends T1> t1Supplier,
            @Independent @Modified CheckedFunction0<? extends T2> t2Supplier) { return null; }

        @Independent @NotModified
        static <T1 extends AutoCloseable, T2 extends AutoCloseable, T3 extends AutoCloseable> Try.WithResources3<
            T1,
            T2,
            T3> withResources(
            @Independent @Modified CheckedFunction0<? extends T1> t1Supplier,
            @Independent @Modified CheckedFunction0<? extends T2> t2Supplier,
            @Independent @Modified CheckedFunction0<? extends T3> t3Supplier) { return null; }

        @Independent @NotModified
        static <T1 extends AutoCloseable, T2 extends AutoCloseable, T3 extends AutoCloseable, T4 extends AutoCloseable>
            Try.WithResources4<T1, T2, T3, T4> withResources(
            @Independent @Modified CheckedFunction0<? extends T1> t1Supplier,
            @Independent @Modified CheckedFunction0<? extends T2> t2Supplier,
            @Independent @Modified CheckedFunction0<? extends T3> t3Supplier,
            @Independent @Modified CheckedFunction0<? extends T4> t4Supplier) { return null; }

        @Independent @NotModified
        static <
            T1 extends AutoCloseable,
            T2 extends AutoCloseable,
            T3 extends AutoCloseable,
            T4 extends AutoCloseable,
            T5 extends AutoCloseable> Try.WithResources5<T1, T2, T3, T4, T5> withResources(
            @Independent @Modified CheckedFunction0<? extends T1> t1Supplier,
            @Independent @Modified CheckedFunction0<? extends T2> t2Supplier,
            @Independent @Modified CheckedFunction0<? extends T3> t3Supplier,
            @Independent @Modified CheckedFunction0<? extends T4> t4Supplier,
            @Independent @Modified CheckedFunction0<? extends T5> t5Supplier) { return null; }

        @Independent @NotModified
        static <
            T1 extends AutoCloseable,
            T2 extends AutoCloseable,
            T3 extends AutoCloseable,
            T4 extends AutoCloseable,
            T5 extends AutoCloseable,
            T6 extends AutoCloseable> Try.WithResources6<T1, T2, T3, T4, T5, T6> withResources(
            @Independent @Modified CheckedFunction0<? extends T1> t1Supplier,
            @Independent @Modified CheckedFunction0<? extends T2> t2Supplier,
            @Independent @Modified CheckedFunction0<? extends T3> t3Supplier,
            @Independent @Modified CheckedFunction0<? extends T4> t4Supplier,
            @Independent @Modified CheckedFunction0<? extends T5> t5Supplier,
            @Independent @Modified CheckedFunction0<? extends T6> t6Supplier) { return null; }

        @Independent @NotModified
        static <
            T1 extends AutoCloseable,
            T2 extends AutoCloseable,
            T3 extends AutoCloseable,
            T4 extends AutoCloseable,
            T5 extends AutoCloseable,
            T6 extends AutoCloseable,
            T7 extends AutoCloseable> Try.WithResources7<T1, T2, T3, T4, T5, T6, T7> withResources(
            @Independent @Modified CheckedFunction0<? extends T1> t1Supplier,
            @Independent @Modified CheckedFunction0<? extends T2> t2Supplier,
            @Independent @Modified CheckedFunction0<? extends T3> t3Supplier,
            @Independent @Modified CheckedFunction0<? extends T4> t4Supplier,
            @Independent @Modified CheckedFunction0<? extends T5> t5Supplier,
            @Independent @Modified CheckedFunction0<? extends T6> t6Supplier,
            @Independent @Modified CheckedFunction0<? extends T7> t7Supplier) { return null; }

        @Independent @NotModified
        static <
            T1 extends AutoCloseable,
            T2 extends AutoCloseable,
            T3 extends AutoCloseable,
            T4 extends AutoCloseable,
            T5 extends AutoCloseable,
            T6 extends AutoCloseable,
            T7 extends AutoCloseable,
            T8 extends AutoCloseable> Try.WithResources8<T1, T2, T3, T4, T5, T6, T7, T8> withResources(
            @Independent @Modified CheckedFunction0<? extends T1> t1Supplier,
            @Independent @Modified CheckedFunction0<? extends T2> t2Supplier,
            @Independent @Modified CheckedFunction0<? extends T3> t3Supplier,
            @Independent @Modified CheckedFunction0<? extends T4> t4Supplier,
            @Independent @Modified CheckedFunction0<? extends T5> t5Supplier,
            @Independent @Modified CheckedFunction0<? extends T6> t6Supplier,
            @Independent @Modified CheckedFunction0<? extends T7> t7Supplier,
            @Independent @Modified CheckedFunction0<? extends T8> t8Supplier) { return null; }
    }

    //public interface Validation implements Value<T>, Serializable
    @Immutable(hc = true)
    class Validation$<E, T> {
        static final long serialVersionUID = 0L;
        //static final class Builder
        @ImmutableContainer(hc = true)
        @Independent
        class Builder<E, T1, T2> {
            @Independent(hc = true) @NotModified
            <R> Validation<Seq<E>, R> ap(@Independent @NotModified Function2<T1, T2, R> f) { return null; }

            @Independent(hc = true) @NotModified
            <T3> Validation.Builder3<E, T1, T2, T3> combine(@Independent @NotModified Validation<E, T3> v3) {
                return null;
            }
        }

        //static final class Builder3
        @ImmutableContainer(hc = true)
        @Independent
        class Builder3<E, T1, T2, T3> {
            @Independent(hc = true) @NotModified
            <R> Validation<Seq<E>, R> ap(@Independent @NotModified Function3<T1, T2, T3, R> f) { return null; }

            @Independent(hc = true) @NotModified
            <T4> Validation.Builder4<E, T1, T2, T3, T4> combine(@Independent @NotModified Validation<E, T4> v4) {
                return null;
            }
        }

        //static final class Builder4
        @ImmutableContainer(hc = true)
        @Independent
        class Builder4<E, T1, T2, T3, T4> {
            @Independent(hc = true) @NotModified
            <R> Validation<Seq<E>, R> ap(@Independent @NotModified Function4<T1, T2, T3, T4, R> f) { return null; }

            @Independent(hc = true) @NotModified
            <T5> Validation.Builder5<E, T1, T2, T3, T4, T5> combine(@Independent @NotModified Validation<E, T5> v5) {
                return null;
            }
        }

        //static final class Builder5
        @ImmutableContainer(hc = true)
        @Independent
        class Builder5<E, T1, T2, T3, T4, T5> {
            @Independent(hc = true) @NotModified
            <R> Validation<Seq<E>, R> ap(@Independent @NotModified Function5<T1, T2, T3, T4, T5, R> f) { return null; }

            @Independent(hc = true) @NotModified
            <T6> Validation.Builder6<E, T1, T2, T3, T4, T5, T6> combine(@Independent @NotModified Validation<E, T6> v6) {
                return null;
            }
        }

        //static final class Builder6
        @ImmutableContainer(hc = true)
        @Independent
        class Builder6<E, T1, T2, T3, T4, T5, T6> {
            @Independent(hc = true) @NotModified
            <R> Validation<Seq<E>, R> ap(@Independent @NotModified Function6<T1, T2, T3, T4, T5, T6, R> f) {
                return null;
            }

            @Independent(hc = true) @NotModified
            <T7> Validation.Builder7<E, T1, T2, T3, T4, T5, T6, T7> combine(
                @Independent @NotModified Validation<E, T7> v7) { return null; }
        }

        //static final class Builder7
        @ImmutableContainer(hc = true)
        @Independent
        class Builder7<E, T1, T2, T3, T4, T5, T6, T7> {
            @Independent(hc = true) @NotModified
            <R> Validation<Seq<E>, R> ap(@Independent @NotModified Function7<T1, T2, T3, T4, T5, T6, T7, R> f) {
                return null;
            }

            @Independent(hc = true) @NotModified
            <T8> Validation.Builder8<E, T1, T2, T3, T4, T5, T6, T7, T8> combine(
                @Independent @NotModified Validation<E, T8> v8) { return null; }
        }

        //static final class Builder8
        @ImmutableContainer(hc = true)
        @Independent
        class Builder8<E, T1, T2, T3, T4, T5, T6, T7, T8> {
            @Independent(hc = true) @NotModified
            <R> Validation<Seq<E>, R> ap(@Independent @NotModified Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> f) {
                return null;
            }
        }

        //static final class Invalid implements Validation<E,T>, Serializable
        @ImmutableContainer(hc = true)
        class Invalid<E, T> {
            //override from io.vavr.Value, io.vavr.control.Validation, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object obj) { return false; }

            //override from io.vavr.Value, io.vavr.control.Validation
            @Independent @NotModified
            T get() { return null; }

            //override from io.vavr.control.Validation
            @Independent(hc = true) @NotModified @GetSet("error")
            E getError() { return null; }

            //override from io.vavr.Value, io.vavr.control.Validation, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from io.vavr.control.Validation
            @NotModified
            boolean isInvalid() { return false; }

            //override from io.vavr.control.Validation
            @NotModified
            boolean isValid() { return false; }

            //override from io.vavr.Value
            @NotModified
            String stringPrefix() { return null; }

            //override from io.vavr.Value, io.vavr.control.Validation, java.lang.Object
            @NotModified
            public String toString() { return null; }
        }

        //static final class Valid implements Validation<E,T>, Serializable
        @ImmutableContainer(hc = true)
        class Valid<E, T> {
            //override from io.vavr.Value, io.vavr.control.Validation, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object obj) { return false; }

            //override from io.vavr.Value, io.vavr.control.Validation
            @Independent(hc = true) @NotModified @GetSet("value")
            T get() { return null; }

            //override from io.vavr.control.Validation
            @Independent @NotModified
            E getError() { return null; }

            //override from io.vavr.Value, io.vavr.control.Validation, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from io.vavr.control.Validation
            @NotModified
            boolean isInvalid() { return false; }

            //override from io.vavr.control.Validation
            @NotModified
            boolean isValid() { return false; }

            //override from io.vavr.Value
            @NotModified
            String stringPrefix() { return null; }

            //override from io.vavr.Value, io.vavr.control.Validation, java.lang.Object
            @NotModified
            public String toString() { return null; }
        }

        @Independent @NotModified
        <U> Validation<Seq<E>, U> ap(
            @Independent @Modified Validation<Seq<E>, ? extends Function<? super T, ? extends U>> validation) {
            return null;
        }

        @Independent @NotModified
        <E2, T2> Validation<E2, T2> bimap(
            @Independent @Modified Function<? super E, ? extends E2> errorMapper,
            @Independent @Modified Function<? super T, ? extends T2> valueMapper) { return null; }

        @Fluent @Independent @NotModified
        <U> Validation.Builder<E, T, U> combine(@Independent @NotModified Validation<E, U> validation) { return null; }

        @Independent @NotModified
        static <E, T1, T2> Validation.Builder<E, T1, T2> combine(
            @Independent @NotModified Validation<E, T1> validation1,
            @Independent @NotModified Validation<E, T2> validation2) { return null; }

        @Independent @NotModified
        static <E, T1, T2, T3> Validation.Builder3<E, T1, T2, T3> combine(
            @Independent @NotModified Validation<E, T1> validation1,
            @Independent @NotModified Validation<E, T2> validation2,
            @Independent @NotModified Validation<E, T3> validation3) { return null; }

        @Independent @NotModified
        static <E, T1, T2, T3, T4> Validation.Builder4<E, T1, T2, T3, T4> combine(
            @Independent @NotModified Validation<E, T1> validation1,
            @Independent @NotModified Validation<E, T2> validation2,
            @Independent @NotModified Validation<E, T3> validation3,
            @Independent @NotModified Validation<E, T4> validation4) { return null; }

        @Independent @NotModified
        static <E, T1, T2, T3, T4, T5> Validation.Builder5<E, T1, T2, T3, T4, T5> combine(
            @Independent @NotModified Validation<E, T1> validation1,
            @Independent @NotModified Validation<E, T2> validation2,
            @Independent @NotModified Validation<E, T3> validation3,
            @Independent @NotModified Validation<E, T4> validation4,
            @Independent @NotModified Validation<E, T5> validation5) { return null; }

        @Independent @NotModified
        static <E, T1, T2, T3, T4, T5, T6> Validation.Builder6<E, T1, T2, T3, T4, T5, T6> combine(
            @Independent @NotModified Validation<E, T1> validation1,
            @Independent @NotModified Validation<E, T2> validation2,
            @Independent @NotModified Validation<E, T3> validation3,
            @Independent @NotModified Validation<E, T4> validation4,
            @Independent @NotModified Validation<E, T5> validation5,
            @Independent @NotModified Validation<E, T6> validation6) { return null; }

        @Independent @NotModified
        static <E, T1, T2, T3, T4, T5, T6, T7> Validation.Builder7<E, T1, T2, T3, T4, T5, T6, T7> combine(
            @Independent @NotModified Validation<E, T1> validation1,
            @Independent @NotModified Validation<E, T2> validation2,
            @Independent @NotModified Validation<E, T3> validation3,
            @Independent @NotModified Validation<E, T4> validation4,
            @Independent @NotModified Validation<E, T5> validation5,
            @Independent @NotModified Validation<E, T6> validation6,
            @Independent @NotModified Validation<E, T7> validation7) { return null; }

        @Independent @NotModified
        static <E, T1, T2, T3, T4, T5, T6, T7, T8> Validation.Builder8<E, T1, T2, T3, T4, T5, T6, T7, T8> combine(
            @Independent @NotModified Validation<E, T1> validation1,
            @Independent @NotModified Validation<E, T2> validation2,
            @Independent @NotModified Validation<E, T3> validation3,
            @Independent @NotModified Validation<E, T4> validation4,
            @Independent @NotModified Validation<E, T5> validation5,
            @Independent @NotModified Validation<E, T6> validation6,
            @Independent @NotModified Validation<E, T7> validation7,
            @Independent @NotModified Validation<E, T8> validation8) { return null; }

        @Independent @NotModified
        static <E, T> Validation<E, T> cond(
            boolean test,
            @Independent @NotModified T valid,
            @Independent @NotModified E error) { return null; }

        @Independent @NotModified
        static <E, T> Validation<E, T> cond(
            boolean test,
            @Independent @Modified Supplier<? extends T> valid,
            @Independent @Modified Supplier<? extends E> error) { return null; }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object arg0) { return false; }

        @Fluent @Independent @NotModified
        Option<Validation<E, T>> filter(@Independent @Modified Predicate<? super T> predicate) { return null; }

        @Fluent @Independent @NotModified
        <U> Validation<E, U> flatMap(
            @Independent @Modified Function<? super T, ? extends Validation<E, ? extends U>> mapper) { return null; }

        @Independent @NotModified
        <U> U fold(
            @Independent @Modified Function<? super E, ? extends U> ifInvalid,
            @Independent @Modified Function<? super T, ? extends U> ifValid) { return null; }

        //override from io.vavr.Value, java.lang.Iterable
        @NotModified
        void forEach(@Independent @Modified Consumer<? super T> action) { }

        @Independent @NotModified
        static <E, T> Validation<E, T> fromEither(@Independent @NotModified Either<E, T> either) { return null; }

        @Independent @NotModified
        static <T> Validation<Throwable, T> fromTry(@Independent @NotModified Try<? extends T> t) { return null; }

        //override from io.vavr.Value
        @Independent(hc = true) @NotModified
        T get() { return null; }
        @Independent(hc = true) @NotModified E getError() { return null; }
        @Independent @NotModified
        T getOrElseGet(@Independent @Modified Function<? super E, ? extends T> other) { return null; }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        @Independent @NotModified
        static <E, T> Validation<E, T> invalid(@Independent @NotModified E error) { return null; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isEmpty() { return false; }
        @NotModified boolean isInvalid() { return false; }
        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isSingleValued() { return false; }
        @NotModified boolean isValid() { return false; }
        //override from io.vavr.Value, java.lang.Iterable
        @Independent @NotModified
        Iterator<T> iterator() { return null; }

        //override from io.vavr.Value
        @Independent @NotModified
        <U> Validation<E, U> map(@Independent @Modified Function<? super T, ? extends U> f) { return null; }

        @Independent @NotModified
        <U> Validation<U, T> mapError(@Independent @Modified Function<? super E, ? extends U> f) { return null; }

        @Identity @Independent @NotModified
        static <E, T> Validation<E, T> narrow(@Independent @NotModified Validation<? extends E, ? extends T> validation) {
            return null;
        }

        @Fluent @Independent @NotModified
        Validation<E, T> orElse(@Independent @NotModified Validation<? extends E, ? extends T> other) { return null; }

        @Fluent @Independent @NotModified
        Validation<E, T> orElse(@Independent @Modified Supplier<Validation<? extends E, ? extends T>> supplier) {
            return null;
        }

        //override from io.vavr.Value
        @Fluent @Independent @NotModified
        Validation<E, T> peek(@Independent @Modified Consumer<? super T> action) { return null; }

        @Independent @NotModified
        static <E, T> Validation<Seq<E>, Seq<T>> sequence(
            @Independent @NotModified Iterable<? extends Validation<? extends Seq<? extends E>, ? extends T>> values) {
            return null;
        }
        @Independent @NotModified Validation<T, E> swap() { return null; }
        @Independent @NotModified Either<E, T> toEither() { return null; }
        //override from io.vavr.Value, java.lang.Object
        @NotModified @NotNull
        public String toString() { return null; }

        @Independent @NotModified
        static <E, T, U> Validation<Seq<E>, Seq<U>> traverse(
            @Independent @Modified Iterable<? extends T> values,
            @Independent @Modified Function<? super T, ? extends Validation<? extends Seq<? extends E>, ? extends U>>
                mapper) { return null; }

        @Independent @NotModified
        static <E, T> Validation<E, T> valid(@Independent @NotModified T value) { return null; }
    }
}
