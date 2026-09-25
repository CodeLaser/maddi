package io.codelaser.maddi.aapi.archive.libs.vavr;
import io.vavr.*;
import io.vavr.collection.Array;
import io.vavr.collection.CharSeq;
import io.vavr.collection.IndexedSeq;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.PriorityQueue;
import io.vavr.collection.Queue;
import io.vavr.collection.Seq;
import io.vavr.collection.SortedMap;
import io.vavr.collection.SortedSet;
import io.vavr.collection.Tree;
import io.vavr.collection.Vector;
import io.vavr.concurrent.Future;
import io.vavr.control.*;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;
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
public class IoVavr {
    public static final String PACKAGE_NAME = "io.vavr";
    //public final class API
    @Immutable
    class API$ {
        //public static class For1
        @Immutable(hc = true)
        @Independent
        class For1<T1> {
            @Independent @Modified @NotModified(after = "ts1") Iterator<T1> yield() { return null; }
            @Independent @Modified @NotModified(after = "ts1")
            <R> Iterator<R> yield(@Independent @Modified Function<? super T1, ? extends R> f) { return null; }
        }

        //public static class For1Either
        @Immutable(hc = true)
        class For1Either<L, T1> {
            @Independent(hc = true) @NotModified Either<L, T1> yield() { return null; }
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(@Independent @Modified Function<? super T1, ? extends R> f) { return null; }
        }

        //public static class For1Future
        //EXPECTED @Immutable(hc = true), once Future is (G3) -- computed @FinalFields @Independent -- downstream of G3: a comprehension over Futures (VAVR.md)
        @FinalFields
        @Independent
        class For1Future<T1> {
            @Independent @NotModified Future<T1> yield() { return null; }
            @Independent @NotModified
            <R> Future<R> yield(@Independent @Modified Function<? super T1, ? extends R> f) { return null; }
        }

        //public static class For1List
        @Immutable(hc = true)
        @Independent
        class For1List<T1> {
            @Independent @NotModified List<T1> yield() { return null; }
            @Independent @NotModified
            <R> List<R> yield(@Independent @Modified Function<? super T1, ? extends R> f) { return null; }
        }

        //public static class For1Option
        @Immutable(hc = true)
        @Independent
        class For1Option<T1> {
            @Independent @NotModified Option<T1> yield() { return null; }
            @Independent @NotModified
            <R> Option<R> yield(@Independent @Modified Function<? super T1, ? extends R> f) { return null; }
        }

        //public static class For1Try
        //EXPECTED @FinalFields -- computed @FinalFields @Dependent -- downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) (VAVR.md)
        @FinalFields
        @Independent(absent = true)
        class For1Try<T1> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1") Try<T1> yield() { return null; }
            @Independent(absent = true) @Modified @NotModified(after = "ts1")
            <R> Try<R> yield(@Independent(absent = true) @Modified Function<? super T1, ? extends R> f) { return null; }
        }

        //public static class For1Validation
        @Immutable(hc = true)
        class For1Validation<L, T1> {
            @Independent(hc = true) @NotModified Validation<L, T1> yield() { return null; }
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(@Independent @Modified Function<? super T1, ? extends R> f) { return null; }
        }

        //public static class For2
        @Immutable(hc = true)
        @Independent
        class For2<T1, T2> {
            @Independent @Modified @NotModified(after = "ts1,ts2")
            <R> Iterator<R> yield(@Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> f) {
                return null;
            }
        }

        //public static class For2Either
        @Immutable(hc = true)
        class For2Either<L, T1, T2> {
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(@Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> f) {
                return null;
            }
        }

        //public static class For2Future
        //EXPECTED @Immutable(hc = true), once Future is (G3) -- computed @FinalFields @Independent -- downstream of G3: a comprehension over Futures (VAVR.md)
        @FinalFields
        @Independent
        class For2Future<T1, T2> {
            @Independent @Modified @NotModified(after = "ts1,ts2")
            <R> Future<R> yield(@Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> f) { return null; }
        }

        //public static class For2List
        @Immutable(hc = true)
        @Independent
        class For2List<T1, T2> {
            @Independent @NotModified
            <R> List<R> yield(@Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> f) { return null; }
        }

        //public static class For2Option
        @Immutable(hc = true)
        @Independent
        class For2Option<T1, T2> {
            @Independent @NotModified
            <R> Option<R> yield(@Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> f) { return null; }
        }

        //public static class For2Try
        //EXPECTED @FinalFields -- computed @FinalFields @Dependent -- downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) (VAVR.md)
        @FinalFields
        @Independent(absent = true)
        class For2Try<T1, T2> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1,ts2")
            <R> Try<R> yield(@Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> f) { return null; }
        }

        //public static class For2Validation
        @Immutable(hc = true)
        class For2Validation<L, T1, T2> {
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(@Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> f) {
                return null;
            }
        }

        //public static class For3
        @Immutable(hc = true)
        @Independent
        class For3<T1, T2, T3> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3")
            <R> Iterator<R> yield(@Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
                return null;
            }
        }

        //public static class For3Either
        @Immutable(hc = true)
        class For3Either<L, T1, T2, T3> {
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(@Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
                return null;
            }
        }

        //public static class For3Future
        //EXPECTED @Immutable(hc = true), once Future is (G3) -- computed @FinalFields @Independent -- downstream of G3: a comprehension over Futures (VAVR.md)
        @FinalFields
        @Independent
        class For3Future<T1, T2, T3> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3")
            <R> Future<R> yield(@Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
                return null;
            }
        }

        //public static class For3List
        @Immutable(hc = true)
        @Independent
        class For3List<T1, T2, T3> {
            @Independent @NotModified
            <R> List<R> yield(@Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
                return null;
            }
        }

        //public static class For3Option
        @Immutable(hc = true)
        @Independent
        class For3Option<T1, T2, T3> {
            @Independent @NotModified
            <R> Option<R> yield(@Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
                return null;
            }
        }

        //public static class For3Try
        //EXPECTED @FinalFields -- computed @FinalFields @Dependent -- downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) (VAVR.md)
        @FinalFields
        @Independent(absent = true)
        class For3Try<T1, T2, T3> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1,ts2,ts3")
            <R> Try<R> yield(@Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
                return null;
            }
        }

        //public static class For3Validation
        @Immutable(hc = true)
        class For3Validation<L, T1, T2, T3> {
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(
                @Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) { return null; }
        }

        //public static class For4
        @Immutable(hc = true)
        @Independent
        class For4<T1, T2, T3, T4> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4")
            <R> Iterator<R> yield(
                @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
                return null;
            }
        }

        //public static class For4Either
        @Immutable(hc = true)
        class For4Either<L, T1, T2, T3, T4> {
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(
                @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
                return null;
            }
        }

        //public static class For4Future
        //EXPECTED @Immutable(hc = true), once Future is (G3) -- computed @FinalFields @Independent -- downstream of G3: a comprehension over Futures (VAVR.md)
        @FinalFields
        @Independent
        class For4Future<T1, T2, T3, T4> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4")
            <R> Future<R> yield(
                @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
                return null;
            }
        }

        //public static class For4List
        @Immutable(hc = true)
        @Independent
        class For4List<T1, T2, T3, T4> {
            @Independent @NotModified
            <R> List<R> yield(
                @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
                return null;
            }
        }

        //public static class For4Option
        @Immutable(hc = true)
        @Independent
        class For4Option<T1, T2, T3, T4> {
            @Independent @NotModified
            <R> Option<R> yield(
                @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
                return null;
            }
        }

        //public static class For4Try
        //EXPECTED @FinalFields -- computed @FinalFields @Dependent -- downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) (VAVR.md)
        @FinalFields
        @Independent(absent = true)
        class For4Try<T1, T2, T3, T4> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1,ts2,ts3,ts4")
            <R> Try<R> yield(
                @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
                return null;
            }
        }

        //public static class For4Validation
        @Immutable(hc = true)
        class For4Validation<L, T1, T2, T3, T4> {
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(
                @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
                return null;
            }
        }

        //public static class For5
        @Immutable(hc = true)
        @Independent
        class For5<T1, T2, T3, T4, T5> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5")
            <R> Iterator<R> yield(
                @Independent @Modified Function5<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? extends R> f) { return null; }
        }

        //public static class For5Either
        @Immutable(hc = true)
        class For5Either<L, T1, T2, T3, T4, T5> {
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(
                @Independent @Modified Function5<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? extends R> f) { return null; }
        }

        //public static class For5Future
        //EXPECTED @Immutable(hc = true), once Future is (G3) -- computed @FinalFields @Independent -- downstream of G3: a comprehension over Futures (VAVR.md)
        @FinalFields
        @Independent
        class For5Future<T1, T2, T3, T4, T5> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5")
            <R> Future<R> yield(
                @Independent @Modified Function5<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? extends R> f) { return null; }
        }

        //public static class For5List
        @Immutable(hc = true)
        @Independent
        class For5List<T1, T2, T3, T4, T5> {
            @Independent @NotModified
            <R> List<R> yield(
                @Independent @Modified Function5<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? extends R> f) { return null; }
        }

        //public static class For5Option
        @Immutable(hc = true)
        @Independent
        class For5Option<T1, T2, T3, T4, T5> {
            @Independent @NotModified
            <R> Option<R> yield(
                @Independent @Modified Function5<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? extends R> f) { return null; }
        }

        //public static class For5Try
        //EXPECTED @FinalFields -- computed @FinalFields @Dependent -- downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) (VAVR.md)
        @FinalFields
        @Independent(absent = true)
        class For5Try<T1, T2, T3, T4, T5> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5")
            <R> Try<R> yield(
                @Independent @Modified Function5<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? extends R> f) { return null; }
        }

        //public static class For5Validation
        @Immutable(hc = true)
        class For5Validation<L, T1, T2, T3, T4, T5> {
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(
                @Independent @Modified Function5<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? extends R> f) { return null; }
        }

        //public static class For6
        @Immutable(hc = true)
        @Independent
        class For6<T1, T2, T3, T4, T5, T6> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6")
            <R> Iterator<R> yield(
                @Independent @Modified Function6<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? extends R> f) { return null; }
        }

        //public static class For6Either
        @Immutable(hc = true)
        class For6Either<L, T1, T2, T3, T4, T5, T6> {
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(
                @Independent @Modified Function6<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? extends R> f) { return null; }
        }

        //public static class For6Future
        //EXPECTED @Immutable(hc = true), once Future is (G3) -- computed @FinalFields @Independent -- downstream of G3: a comprehension over Futures (VAVR.md)
        @FinalFields
        @Independent
        class For6Future<T1, T2, T3, T4, T5, T6> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6")
            <R> Future<R> yield(
                @Independent @Modified Function6<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? extends R> f) { return null; }
        }

        //public static class For6List
        @Immutable(hc = true)
        @Independent
        class For6List<T1, T2, T3, T4, T5, T6> {
            @Independent @NotModified
            <R> List<R> yield(
                @Independent @Modified Function6<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? extends R> f) { return null; }
        }

        //public static class For6Option
        @Immutable(hc = true)
        @Independent
        class For6Option<T1, T2, T3, T4, T5, T6> {
            @Independent @NotModified
            <R> Option<R> yield(
                @Independent @Modified Function6<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? extends R> f) { return null; }
        }

        //public static class For6Try
        //EXPECTED @FinalFields -- computed @FinalFields @Dependent -- downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) (VAVR.md)
        @FinalFields
        @Independent(absent = true)
        class For6Try<T1, T2, T3, T4, T5, T6> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6")
            <R> Try<R> yield(
                @Independent @Modified Function6<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? extends R> f) { return null; }
        }

        //public static class For6Validation
        @Immutable(hc = true)
        class For6Validation<L, T1, T2, T3, T4, T5, T6> {
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(
                @Independent @Modified Function6<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? extends R> f) { return null; }
        }

        //public static class For7
        @Immutable(hc = true)
        @Independent
        class For7<T1, T2, T3, T4, T5, T6, T7> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6,ts7")
            <R> Iterator<R> yield(
                @Independent @Modified Function7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R> f) { return null; }
        }

        //public static class For7Either
        @Immutable(hc = true)
        class For7Either<L, T1, T2, T3, T4, T5, T6, T7> {
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(
                @Independent @Modified Function7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R> f) { return null; }
        }

        //public static class For7Future
        //EXPECTED @Immutable(hc = true), once Future is (G3) -- computed @FinalFields @Independent -- downstream of G3: a comprehension over Futures (VAVR.md)
        @FinalFields
        @Independent
        class For7Future<T1, T2, T3, T4, T5, T6, T7> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6,ts7")
            <R> Future<R> yield(
                @Independent @Modified Function7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R> f) { return null; }
        }

        //public static class For7List
        @Immutable(hc = true)
        @Independent
        class For7List<T1, T2, T3, T4, T5, T6, T7> {
            @Independent @NotModified
            <R> List<R> yield(
                @Independent @Modified Function7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R> f) { return null; }
        }

        //public static class For7Option
        @Immutable(hc = true)
        @Independent
        class For7Option<T1, T2, T3, T4, T5, T6, T7> {
            @Independent @NotModified
            <R> Option<R> yield(
                @Independent @Modified Function7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R> f) { return null; }
        }

        //public static class For7Try
        //EXPECTED @FinalFields -- computed @FinalFields @Dependent -- downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) (VAVR.md)
        @FinalFields
        @Independent(absent = true)
        class For7Try<T1, T2, T3, T4, T5, T6, T7> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6,ts7")
            <R> Try<R> yield(
                @Independent @Modified Function7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R> f) { return null; }
        }

        //public static class For7Validation
        @Immutable(hc = true)
        class For7Validation<L, T1, T2, T3, T4, T5, T6, T7> {
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(
                @Independent @Modified Function7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R> f) { return null; }
        }

        //public static class For8
        @Immutable(hc = true)
        @Independent
        class For8<T1, T2, T3, T4, T5, T6, T7, T8> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6,ts7,ts8")
            <R> Iterator<R> yield(
                @Independent @Modified Function8<
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

        //public static class For8Either
        @Immutable(hc = true)
        class For8Either<L, T1, T2, T3, T4, T5, T6, T7, T8> {
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(
                @Independent @Modified Function8<
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

        //public static class For8Future
        //EXPECTED @Immutable(hc = true), once Future is (G3) -- computed @FinalFields @Independent -- downstream of G3: a comprehension over Futures (VAVR.md)
        @FinalFields
        @Independent
        class For8Future<T1, T2, T3, T4, T5, T6, T7, T8> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6,ts7,ts8")
            <R> Future<R> yield(
                @Independent @Modified Function8<
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

        //public static class For8List
        @Immutable(hc = true)
        @Independent
        class For8List<T1, T2, T3, T4, T5, T6, T7, T8> {
            @Independent @NotModified
            <R> List<R> yield(
                @Independent @Modified Function8<
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

        //public static class For8Option
        @Immutable(hc = true)
        @Independent
        class For8Option<T1, T2, T3, T4, T5, T6, T7, T8> {
            @Independent @NotModified
            <R> Option<R> yield(
                @Independent @Modified Function8<
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

        //public static class For8Try
        //EXPECTED @FinalFields -- computed @FinalFields @Dependent -- downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) (VAVR.md)
        @FinalFields
        @Independent(absent = true)
        class For8Try<T1, T2, T3, T4, T5, T6, T7, T8> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6,ts7,ts8")
            <R> Try<R> yield(
                @Independent @Modified Function8<
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

        //public static class For8Validation
        @Immutable(hc = true)
        class For8Validation<L, T1, T2, T3, T4, T5, T6, T7, T8> {
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(
                @Independent @Modified Function8<
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

        //public static class ForLazy2Either
        @FinalFields
        @Independent
        class ForLazy2Either<L, T1, T2> {
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(@Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> f) {
                return null;
            }
        }

        //public static class ForLazy2Future
        @FinalFields
        @Independent
        class ForLazy2Future<T1, T2> {
            @Independent @Modified @NotModified(after = "ts1,ts2")
            <R> Future<R> yield(@Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> f) { return null; }
        }

        //public static class ForLazy2List
        @FinalFields
        @Independent
        class ForLazy2List<T1, T2> {
            @Independent @NotModified
            <R> List<R> yield(@Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> f) { return null; }
        }

        //public static class ForLazy2Option
        @FinalFields
        @Independent
        class ForLazy2Option<T1, T2> {
            @Independent @NotModified
            <R> Option<R> yield(@Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> f) { return null; }
        }

        //public static class ForLazy2Try
        @FinalFields
        @Independent(absent = true)
        class ForLazy2Try<T1, T2> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1,ts2")
            <R> Try<R> yield(@Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> f) { return null; }
        }

        //public static class ForLazy2Validation
        @FinalFields
        @Independent
        class ForLazy2Validation<L, T1, T2> {
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(@Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> f) {
                return null;
            }
        }

        //public static class ForLazy3Either
        @FinalFields
        @Independent
        class ForLazy3Either<L, T1, T2, T3> {
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(@Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
                return null;
            }
        }

        //public static class ForLazy3Future
        @FinalFields
        @Independent
        class ForLazy3Future<T1, T2, T3> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3")
            <R> Future<R> yield(@Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
                return null;
            }
        }

        //public static class ForLazy3List
        @FinalFields
        @Independent
        class ForLazy3List<T1, T2, T3> {
            @Independent @NotModified
            <R> List<R> yield(@Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
                return null;
            }
        }

        //public static class ForLazy3Option
        @FinalFields
        @Independent
        class ForLazy3Option<T1, T2, T3> {
            @Independent @NotModified
            <R> Option<R> yield(@Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
                return null;
            }
        }

        //public static class ForLazy3Try
        @FinalFields
        @Independent(absent = true)
        class ForLazy3Try<T1, T2, T3> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1,ts2,ts3")
            <R> Try<R> yield(@Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
                return null;
            }
        }

        //public static class ForLazy3Validation
        @FinalFields
        @Independent
        class ForLazy3Validation<L, T1, T2, T3> {
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(
                @Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) { return null; }
        }

        //public static class ForLazy4Either
        @FinalFields
        @Independent
        class ForLazy4Either<L, T1, T2, T3, T4> {
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(
                @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
                return null;
            }
        }

        //public static class ForLazy4Future
        @FinalFields
        @Independent
        class ForLazy4Future<T1, T2, T3, T4> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4")
            <R> Future<R> yield(
                @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
                return null;
            }
        }

        //public static class ForLazy4List
        @FinalFields
        @Independent
        class ForLazy4List<T1, T2, T3, T4> {
            @Independent @NotModified
            <R> List<R> yield(
                @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
                return null;
            }
        }

        //public static class ForLazy4Option
        @FinalFields
        @Independent
        class ForLazy4Option<T1, T2, T3, T4> {
            @Independent @NotModified
            <R> Option<R> yield(
                @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
                return null;
            }
        }

        //public static class ForLazy4Try
        @FinalFields
        @Independent(absent = true)
        class ForLazy4Try<T1, T2, T3, T4> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1,ts2,ts3,ts4")
            <R> Try<R> yield(
                @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
                return null;
            }
        }

        //public static class ForLazy4Validation
        @FinalFields
        @Independent
        class ForLazy4Validation<L, T1, T2, T3, T4> {
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(
                @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
                return null;
            }
        }

        //public static class ForLazy5Either
        @FinalFields
        @Independent
        class ForLazy5Either<L, T1, T2, T3, T4, T5> {
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(
                @Independent @Modified Function5<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy5Future
        @FinalFields
        @Independent
        class ForLazy5Future<T1, T2, T3, T4, T5> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5")
            <R> Future<R> yield(
                @Independent @Modified Function5<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy5List
        @FinalFields
        @Independent
        class ForLazy5List<T1, T2, T3, T4, T5> {
            @Independent @NotModified
            <R> List<R> yield(
                @Independent @Modified Function5<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy5Option
        @FinalFields
        @Independent
        class ForLazy5Option<T1, T2, T3, T4, T5> {
            @Independent @NotModified
            <R> Option<R> yield(
                @Independent @Modified Function5<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy5Try
        @FinalFields
        @Independent(absent = true)
        class ForLazy5Try<T1, T2, T3, T4, T5> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5")
            <R> Try<R> yield(
                @Independent @Modified Function5<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy5Validation
        @FinalFields
        @Independent
        class ForLazy5Validation<L, T1, T2, T3, T4, T5> {
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(
                @Independent @Modified Function5<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy6Either
        @FinalFields
        @Independent
        class ForLazy6Either<L, T1, T2, T3, T4, T5, T6> {
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(
                @Independent @Modified Function6<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy6Future
        @FinalFields
        @Independent
        class ForLazy6Future<T1, T2, T3, T4, T5, T6> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6")
            <R> Future<R> yield(
                @Independent @Modified Function6<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy6List
        @FinalFields
        @Independent
        class ForLazy6List<T1, T2, T3, T4, T5, T6> {
            @Independent @NotModified
            <R> List<R> yield(
                @Independent @Modified Function6<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy6Option
        @FinalFields
        @Independent
        class ForLazy6Option<T1, T2, T3, T4, T5, T6> {
            @Independent @NotModified
            <R> Option<R> yield(
                @Independent @Modified Function6<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy6Try
        @FinalFields
        @Independent(absent = true)
        class ForLazy6Try<T1, T2, T3, T4, T5, T6> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6")
            <R> Try<R> yield(
                @Independent @Modified Function6<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy6Validation
        @FinalFields
        @Independent
        class ForLazy6Validation<L, T1, T2, T3, T4, T5, T6> {
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(
                @Independent @Modified Function6<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy7Either
        @FinalFields
        @Independent
        class ForLazy7Either<L, T1, T2, T3, T4, T5, T6, T7> {
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(
                @Independent @Modified Function7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy7Future
        @FinalFields
        @Independent
        class ForLazy7Future<T1, T2, T3, T4, T5, T6, T7> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6,ts7")
            <R> Future<R> yield(
                @Independent @Modified Function7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy7List
        @FinalFields
        @Independent
        class ForLazy7List<T1, T2, T3, T4, T5, T6, T7> {
            @Independent @NotModified
            <R> List<R> yield(
                @Independent @Modified Function7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy7Option
        @FinalFields
        @Independent
        class ForLazy7Option<T1, T2, T3, T4, T5, T6, T7> {
            @Independent @NotModified
            <R> Option<R> yield(
                @Independent @Modified Function7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy7Try
        @FinalFields
        @Independent(absent = true)
        class ForLazy7Try<T1, T2, T3, T4, T5, T6, T7> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6,ts7")
            <R> Try<R> yield(
                @Independent @Modified Function7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy7Validation
        @FinalFields
        @Independent
        class ForLazy7Validation<L, T1, T2, T3, T4, T5, T6, T7> {
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(
                @Independent @Modified Function7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R> f) { return null; }
        }

        //public static class ForLazy8Either
        @FinalFields
        @Independent
        class ForLazy8Either<L, T1, T2, T3, T4, T5, T6, T7, T8> {
            @Independent(hc = true) @NotModified
            <R> Either<L, R> yield(
                @Independent @Modified Function8<
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

        //public static class ForLazy8Future
        @FinalFields
        @Independent
        class ForLazy8Future<T1, T2, T3, T4, T5, T6, T7, T8> {
            @Independent @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6,ts7,ts8")
            <R> Future<R> yield(
                @Independent @Modified Function8<
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

        //public static class ForLazy8List
        @FinalFields
        @Independent
        class ForLazy8List<T1, T2, T3, T4, T5, T6, T7, T8> {
            @Independent @NotModified
            <R> List<R> yield(
                @Independent @Modified Function8<
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

        //public static class ForLazy8Option
        @FinalFields
        @Independent
        class ForLazy8Option<T1, T2, T3, T4, T5, T6, T7, T8> {
            @Independent @NotModified
            <R> Option<R> yield(
                @Independent @Modified Function8<
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

        //public static class ForLazy8Try
        @FinalFields
        @Independent(absent = true)
        class ForLazy8Try<T1, T2, T3, T4, T5, T6, T7, T8> {
            @Independent(absent = true) @Modified @NotModified(after = "ts1,ts2,ts3,ts4,ts5,ts6,ts7,ts8")
            <R> Try<R> yield(
                @Independent @Modified Function8<
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

        //public static class ForLazy8Validation
        @FinalFields
        @Independent
        class ForLazy8Validation<L, T1, T2, T3, T4, T5, T6, T7, T8> {
            @Independent(hc = true) @NotModified
            <R> Validation<L, R> yield(
                @Independent @Modified Function8<
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

        //public static final class Match
        @ImmutableContainer(hc = true)
        @Independent
        class Match<T> {
            //public interface Case implements PartialFunction<T,R>
            @FinalFields
            @Container
            @Independent(absent = true)
            class Case<T, R> { static final long serialVersionUID = 0L; }

            //public static final class Case0 implements API.Match.Case<T,R>
            @FinalFields
            @Independent
            class Case0<T, R> {
                //override from io.vavr.Function1, io.vavr.PartialFunction, java.util.function.Function
                @Independent @Modified
                R apply(@Independent @Modified T obj) { return null; }

                //override from io.vavr.PartialFunction
                @NotModified
                boolean isDefinedAt(@Independent @Modified T obj) { return false; }
            }

            //public static final class Case1 implements API.Match.Case<T,R>
            @FinalFields
            @Independent
            class Case1<T, T1, R> {
                //override from io.vavr.Function1, io.vavr.PartialFunction, java.util.function.Function
                @Independent @Modified
                R apply(@Independent @Modified T obj) { return null; }

                //override from io.vavr.PartialFunction
                @NotModified
                boolean isDefinedAt(@Independent @Modified T obj) { return false; }
            }

            //public static final class Case2 implements API.Match.Case<T,R>
            @FinalFields
            @Independent
            class Case2<T, T1, T2, R> {
                //override from io.vavr.Function1, io.vavr.PartialFunction, java.util.function.Function
                @Independent(absent = true) @Modified
                R apply(@Independent(absent = true) @Modified T obj) { return null; }

                //override from io.vavr.PartialFunction
                @NotModified
                boolean isDefinedAt(@Independent @Modified T obj) { return false; }
            }

            //public static final class Case3 implements API.Match.Case<T,R>
            @FinalFields
            @Independent
            class Case3<T, T1, T2, T3, R> {
                //override from io.vavr.Function1, io.vavr.PartialFunction, java.util.function.Function
                @Independent @Modified
                R apply(@Independent @Modified T obj) { return null; }

                //override from io.vavr.PartialFunction
                @NotModified
                boolean isDefinedAt(@Independent @Modified T obj) { return false; }
            }

            //public static final class Case4 implements API.Match.Case<T,R>
            @FinalFields
            @Independent
            class Case4<T, T1, T2, T3, T4, R> {
                //override from io.vavr.Function1, io.vavr.PartialFunction, java.util.function.Function
                @Independent @Modified
                R apply(@Independent @Modified T obj) { return null; }

                //override from io.vavr.PartialFunction
                @NotModified
                boolean isDefinedAt(@Independent @Modified T obj) { return false; }
            }

            //public static final class Case5 implements API.Match.Case<T,R>
            @FinalFields
            @Independent
            class Case5<T, T1, T2, T3, T4, T5, R> {
                //override from io.vavr.Function1, io.vavr.PartialFunction, java.util.function.Function
                @Independent @Modified
                R apply(@Independent @Modified T obj) { return null; }

                //override from io.vavr.PartialFunction
                @NotModified
                boolean isDefinedAt(@Independent @Modified T obj) { return false; }
            }

            //public static final class Case6 implements API.Match.Case<T,R>
            @FinalFields
            @Independent
            class Case6<T, T1, T2, T3, T4, T5, T6, R> {
                //override from io.vavr.Function1, io.vavr.PartialFunction, java.util.function.Function
                @Independent @Modified
                R apply(@Independent @Modified T obj) { return null; }

                //override from io.vavr.PartialFunction
                @NotModified
                boolean isDefinedAt(@Independent @Modified T obj) { return false; }
            }

            //public static final class Case7 implements API.Match.Case<T,R>
            @FinalFields
            @Independent
            class Case7<T, T1, T2, T3, T4, T5, T6, T7, R> {
                //override from io.vavr.Function1, io.vavr.PartialFunction, java.util.function.Function
                @Independent @Modified
                R apply(@Independent @Modified T obj) { return null; }

                //override from io.vavr.PartialFunction
                @NotModified
                boolean isDefinedAt(@Independent @Modified T obj) { return false; }
            }

            //public static final class Case8 implements API.Match.Case<T,R>
            @FinalFields
            @Independent
            class Case8<T, T1, T2, T3, T4, T5, T6, T7, T8, R> {
                //override from io.vavr.Function1, io.vavr.PartialFunction, java.util.function.Function
                @Independent @Modified
                R apply(@Independent @Modified T obj) { return null; }

                //override from io.vavr.PartialFunction
                @NotModified
                boolean isDefinedAt(@Independent @Modified T obj) { return false; }
            }

            //public interface Pattern implements PartialFunction<T,R>
            @FinalFields
            @Container
            @Independent(absent = true)
            class Pattern<T, R> { }

            //public abstract static class Pattern0 implements API.Match.Pattern<T,T>
            @FinalFields
            @Container
            @Independent(absent = true)
            class Pattern0<T> {
                @Independent @NotModified static <T> API.Match.Pattern0<T> any() { return null; }
                @Independent @NotModified static <T> API.Match.Pattern0<T> of(Class<? super T> type) { return null; }
            }

            //public abstract static class Pattern1 implements API.Match.Pattern<T,T1>
            @FinalFields
            @Independent(absent = true)
            class Pattern1<T, T1> {
                @Independent @NotModified
                static <T, T1 extends U1, U1> API.Match.Pattern1<T, T1> of(
                    Class<? super T> type,
                    @Independent @Modified API.Match.Pattern<T1, ?> p1,
                    @Independent @Modified Function<T, Tuple1<U1>> unapply) { return null; }
            }

            //public abstract static class Pattern2 implements API.Match.Pattern<T,Tuple2<T1,T2>>
            @FinalFields
            @Independent(absent = true)
            class Pattern2<T, T1, T2> {
                @Independent @NotModified
                static <T, T1 extends U1, U1, T2 extends U2, U2> API.Match.Pattern2<T, T1, T2> of(
                    Class<? super T> type,
                    @Independent @Modified API.Match.Pattern<T1, ?> p1,
                    @Independent @NotModified API.Match.Pattern<T2, ?> p2,
                    @Independent @Modified Function<T, Tuple2<U1, U2>> unapply) { return null; }
            }

            //public abstract static class Pattern3 implements API.Match.Pattern<T,Tuple3<T1,T2,T3>>
            @FinalFields
            @Independent(absent = true)
            class Pattern3<T, T1, T2, T3> {
                @Independent @NotModified
                static <T, T1 extends U1, U1, T2 extends U2, U2, T3 extends U3, U3> API.Match.Pattern3<T, T1, T2, T3> of(
                    Class<? super T> type,
                    @Independent @Modified API.Match.Pattern<T1, ?> p1,
                    @Independent @NotModified API.Match.Pattern<T2, ?> p2,
                    @Independent @NotModified API.Match.Pattern<T3, ?> p3,
                    @Independent @Modified Function<T, Tuple3<U1, U2, U3>> unapply) { return null; }
            }

            //public abstract static class Pattern4 implements API.Match.Pattern<T,Tuple4<T1,T2,T3,T4>>
            @FinalFields
            @Independent(absent = true)
            class Pattern4<T, T1, T2, T3, T4> {
                @Independent @NotModified
                static <T, T1 extends U1, U1, T2 extends U2, U2, T3 extends U3, U3, T4 extends U4, U4>
                    API.Match.Pattern4<T, T1, T2, T3, T4> of(
                    Class<? super T> type,
                    @Independent @Modified API.Match.Pattern<T1, ?> p1,
                    @Independent @NotModified API.Match.Pattern<T2, ?> p2,
                    @Independent @NotModified API.Match.Pattern<T3, ?> p3,
                    @Independent @NotModified API.Match.Pattern<T4, ?> p4,
                    @Independent @Modified Function<T, Tuple4<U1, U2, U3, U4>> unapply) { return null; }
            }

            //public abstract static class Pattern5 implements API.Match.Pattern<T,Tuple5<T1,T2,T3,T4,T5>>
            @FinalFields
            @Independent(absent = true)
            class Pattern5<T, T1, T2, T3, T4, T5> {
                @Independent @NotModified
                static <
                    T,
                    T1 extends U1,
                    U1,
                    T2 extends U2,
                    U2,
                    T3 extends U3,
                    U3,
                    T4 extends U4,
                    U4,
                    T5 extends U5,
                    U5> API.Match.Pattern5<T, T1, T2, T3, T4, T5> of(
                    Class<? super T> type,
                    @Independent @Modified API.Match.Pattern<T1, ?> p1,
                    @Independent @NotModified API.Match.Pattern<T2, ?> p2,
                    @Independent @NotModified API.Match.Pattern<T3, ?> p3,
                    @Independent @NotModified API.Match.Pattern<T4, ?> p4,
                    @Independent @NotModified API.Match.Pattern<T5, ?> p5,
                    @Independent @Modified Function<T, Tuple5<U1, U2, U3, U4, U5>> unapply) { return null; }
            }

            //public abstract static class Pattern6 implements API.Match.Pattern<T,Tuple6<T1,T2,T3,T4,T5,T6>>
            @FinalFields
            @Independent(absent = true)
            class Pattern6<T, T1, T2, T3, T4, T5, T6> {
                @Independent @NotModified
                static <
                    T,
                    T1 extends U1,
                    U1,
                    T2 extends U2,
                    U2,
                    T3 extends U3,
                    U3,
                    T4 extends U4,
                    U4,
                    T5 extends U5,
                    U5,
                    T6 extends U6,
                    U6> API.Match.Pattern6<T, T1, T2, T3, T4, T5, T6> of(
                    Class<? super T> type,
                    @Independent @Modified API.Match.Pattern<T1, ?> p1,
                    @Independent @NotModified API.Match.Pattern<T2, ?> p2,
                    @Independent @NotModified API.Match.Pattern<T3, ?> p3,
                    @Independent @NotModified API.Match.Pattern<T4, ?> p4,
                    @Independent @NotModified API.Match.Pattern<T5, ?> p5,
                    @Independent @NotModified API.Match.Pattern<T6, ?> p6,
                    @Independent @Modified Function<T, Tuple6<U1, U2, U3, U4, U5, U6>> unapply) { return null; }
            }

            //public abstract static class Pattern7 implements API.Match.Pattern<T,Tuple7<T1,T2,T3,T4,T5,T6,T7>>
            @FinalFields
            @Independent(absent = true)
            class Pattern7<T, T1, T2, T3, T4, T5, T6, T7> {
                @Independent @NotModified
                static <
                    T,
                    T1 extends U1,
                    U1,
                    T2 extends U2,
                    U2,
                    T3 extends U3,
                    U3,
                    T4 extends U4,
                    U4,
                    T5 extends U5,
                    U5,
                    T6 extends U6,
                    U6,
                    T7 extends U7,
                    U7> API.Match.Pattern7<T, T1, T2, T3, T4, T5, T6, T7> of(
                    Class<? super T> type,
                    @Independent @Modified API.Match.Pattern<T1, ?> p1,
                    @Independent @NotModified API.Match.Pattern<T2, ?> p2,
                    @Independent @NotModified API.Match.Pattern<T3, ?> p3,
                    @Independent @NotModified API.Match.Pattern<T4, ?> p4,
                    @Independent @NotModified API.Match.Pattern<T5, ?> p5,
                    @Independent @NotModified API.Match.Pattern<T6, ?> p6,
                    @Independent @NotModified API.Match.Pattern<T7, ?> p7,
                    @Independent @Modified Function<T, Tuple7<U1, U2, U3, U4, U5, U6, U7>> unapply) { return null; }
            }

            //public abstract static class Pattern8 implements API.Match.Pattern<T,Tuple8<T1,T2,T3,T4,T5,T6,T7,T8>>
            @FinalFields
            @Independent(absent = true)
            class Pattern8<T, T1, T2, T3, T4, T5, T6, T7, T8> {
                @Independent @NotModified
                static <
                    T,
                    T1 extends U1,
                    U1,
                    T2 extends U2,
                    U2,
                    T3 extends U3,
                    U3,
                    T4 extends U4,
                    U4,
                    T5 extends U5,
                    U5,
                    T6 extends U6,
                    U6,
                    T7 extends U7,
                    U7,
                    T8 extends U8,
                    U8> API.Match.Pattern8<T, T1, T2, T3, T4, T5, T6, T7, T8> of(
                    Class<? super T> type,
                    @Independent @Modified API.Match.Pattern<T1, ?> p1,
                    @Independent @NotModified API.Match.Pattern<T2, ?> p2,
                    @Independent @NotModified API.Match.Pattern<T3, ?> p3,
                    @Independent @NotModified API.Match.Pattern<T4, ?> p4,
                    @Independent @NotModified API.Match.Pattern<T5, ?> p5,
                    @Independent @NotModified API.Match.Pattern<T6, ?> p6,
                    @Independent @NotModified API.Match.Pattern<T7, ?> p7,
                    @Independent @NotModified API.Match.Pattern<T8, ?> p8,
                    @Independent @Modified Function<T, Tuple8<U1, U2, U3, U4, U5, U6, U7, U8>> unapply) { return null; }
            }

            @Independent @NotModified
            <R> R of(@Independent @NotModified API.Match.Case<? extends T, ? extends R> ... cases) { return null; }

            @Independent @NotModified
            <R> Option<R> option(@Independent @NotModified API.Match.Case<? extends T, ? extends R> ... cases) {
                return null;
            }
        }
        @Independent @NotModified static <T> API.Match.Pattern0<T> $() { return null; }
        @Independent @NotModified
        static <T> API.Match.Pattern0<T> $(@Independent @NotModified T prototype) { return null; }

        @Independent @NotModified
        static <T> API.Match.Pattern0<T> $(@Independent @Modified Predicate<? super T> predicate) { return null; }
        @Independent @NotModified static <T> Array<T> Array() { return null; }
        @Independent @NotModified static <T> Array<T> Array(@Independent @NotModified T element) { return null; }
        @Independent @NotModified static <T> Array<T> Array(@Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified
        static <T, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern0<T> pattern,
            @Independent @NotModified R retVal) { return null; }

        @Independent @NotModified
        static <T, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern0<T> pattern,
            @Independent @Modified Function<? super T, ? extends R> f) { return null; }

        @Independent @NotModified
        static <T, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern0<T> pattern,
            @Independent @Modified Supplier<? extends R> supplier) { return null; }

        @Independent @NotModified
        static <T, T1, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern1<T, T1> pattern,
            @Independent @NotModified R retVal) { return null; }

        @Independent @NotModified
        static <T, T1, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern1<T, T1> pattern,
            @Independent @Modified Function<? super T1, ? extends R> f) { return null; }

        @Independent @NotModified
        static <T, T1, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern1<T, T1> pattern,
            @Independent @Modified Supplier<? extends R> supplier) { return null; }

        @Independent @NotModified
        static <T, T1, T2, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern2<T, T1, T2> pattern,
            @Independent @NotModified R retVal) { return null; }

        @Independent @NotModified
        static <T, T1, T2, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern2<T, T1, T2> pattern,
            @Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> f) { return null; }

        @Independent @NotModified
        static <T, T1, T2, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern2<T, T1, T2> pattern,
            @Independent @Modified Supplier<? extends R> supplier) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern3<T, T1, T2, T3> pattern,
            @Independent @NotModified R retVal) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern3<T, T1, T2, T3> pattern,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern3<T, T1, T2, T3> pattern,
            @Independent @Modified Supplier<? extends R> supplier) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern4<T, T1, T2, T3, T4> pattern,
            @Independent @NotModified R retVal) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern4<T, T1, T2, T3, T4> pattern,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
            return null;
        }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern4<T, T1, T2, T3, T4> pattern,
            @Independent @Modified Supplier<? extends R> supplier) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, T5, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern5<T, T1, T2, T3, T4, T5> pattern,
            @Independent @NotModified R retVal) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, T5, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern5<T, T1, T2, T3, T4, T5> pattern,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> f) {
            return null;
        }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, T5, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern5<T, T1, T2, T3, T4, T5> pattern,
            @Independent @Modified Supplier<? extends R> supplier) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, T5, T6, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern6<T, T1, T2, T3, T4, T5, T6> pattern,
            @Independent @NotModified R retVal) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, T5, T6, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern6<T, T1, T2, T3, T4, T5, T6> pattern,
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? extends R> f) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, T5, T6, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern6<T, T1, T2, T3, T4, T5, T6> pattern,
            @Independent @Modified Supplier<? extends R> supplier) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, T5, T6, T7, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern7<T, T1, T2, T3, T4, T5, T6, T7> pattern,
            @Independent @NotModified R retVal) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, T5, T6, T7, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern7<T, T1, T2, T3, T4, T5, T6, T7> pattern,
            @Independent @Modified Function7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? extends R> f) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, T5, T6, T7, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern7<T, T1, T2, T3, T4, T5, T6, T7> pattern,
            @Independent @Modified Supplier<? extends R> supplier) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, T5, T6, T7, T8, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern8<T, T1, T2, T3, T4, T5, T6, T7, T8> pattern,
            @Independent @NotModified R retVal) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, T5, T6, T7, T8, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern8<T, T1, T2, T3, T4, T5, T6, T7, T8> pattern,
            @Independent @Modified Function8<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? super T8,
                ? extends R> f) { return null; }

        @Independent @NotModified
        static <T, T1, T2, T3, T4, T5, T6, T7, T8, R> API.Match.Case<T, R> Case(
            @Independent @Modified API.Match.Pattern8<T, T1, T2, T3, T4, T5, T6, T7, T8> pattern,
            @Independent @Modified Supplier<? extends R> supplier) { return null; }

        @Independent @NotModified
        static CharSeq CharSeq(@Independent @NotModified CharSequence sequence) { return null; }
        @Independent @NotModified static CharSeq CharSeq(char character) { return null; }
        @Independent @NotModified
        static CharSeq CharSeq(@Independent @NotModified char ... characters) { return null; }

        @Independent @NotModified
        static <R> CheckedFunction0<R> CheckedFunction(@Independent @NotModified CheckedFunction0<R> methodReference) {
            return null;
        }

        @Independent @NotModified
        static <T1, R> CheckedFunction1<T1, R> CheckedFunction(
            @Independent @NotModified CheckedFunction1<T1, R> methodReference) { return null; }

        @Independent @NotModified
        static <T1, T2, R> CheckedFunction2<T1, T2, R> CheckedFunction(
            @Independent @NotModified CheckedFunction2<T1, T2, R> methodReference) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, R> CheckedFunction3<T1, T2, T3, R> CheckedFunction(
            @Independent @NotModified CheckedFunction3<T1, T2, T3, R> methodReference) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, R> CheckedFunction4<T1, T2, T3, T4, R> CheckedFunction(
            @Independent @NotModified CheckedFunction4<T1, T2, T3, T4, R> methodReference) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, R> CheckedFunction5<T1, T2, T3, T4, T5, R> CheckedFunction(
            @Independent @NotModified CheckedFunction5<T1, T2, T3, T4, T5, R> methodReference) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, R> CheckedFunction6<T1, T2, T3, T4, T5, T6, R> CheckedFunction(
            @Independent @NotModified CheckedFunction6<T1, T2, T3, T4, T5, T6, R> methodReference) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, R> CheckedFunction7<T1, T2, T3, T4, T5, T6, T7, R> CheckedFunction(
            @Independent @NotModified CheckedFunction7<T1, T2, T3, T4, T5, T6, T7, R> methodReference) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8, R> CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> CheckedFunction(
            @Independent @NotModified CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> methodReference) {
            return null;
        }

        @Independent @NotModified
        static <T> Try.Failure<T> Failure(@Independent @Modified Throwable exception) { return null; }

        @Independent @NotModified
        static <T1> API.For1<T1> For(@Independent @Modified Iterable<T1> ts1) { return null; }

        @Independent @NotModified
        static <T1, T2> API.For2<T1, T2> For(
            @Independent @Modified Iterable<T1> ts1,
            @Independent @Modified Iterable<T2> ts2) { return null; }

        @Independent @NotModified
        static <T1, T2, T3> API.For3<T1, T2, T3> For(
            @Independent @Modified Iterable<T1> ts1,
            @Independent @Modified Iterable<T2> ts2,
            @Independent @Modified Iterable<T3> ts3) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4> API.For4<T1, T2, T3, T4> For(
            @Independent @Modified Iterable<T1> ts1,
            @Independent @Modified Iterable<T2> ts2,
            @Independent @Modified Iterable<T3> ts3,
            @Independent @Modified Iterable<T4> ts4) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5> API.For5<T1, T2, T3, T4, T5> For(
            @Independent @Modified Iterable<T1> ts1,
            @Independent @Modified Iterable<T2> ts2,
            @Independent @Modified Iterable<T3> ts3,
            @Independent @Modified Iterable<T4> ts4,
            @Independent @Modified Iterable<T5> ts5) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6> API.For6<T1, T2, T3, T4, T5, T6> For(
            @Independent @Modified Iterable<T1> ts1,
            @Independent @Modified Iterable<T2> ts2,
            @Independent @Modified Iterable<T3> ts3,
            @Independent @Modified Iterable<T4> ts4,
            @Independent @Modified Iterable<T5> ts5,
            @Independent @Modified Iterable<T6> ts6) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7> API.For7<T1, T2, T3, T4, T5, T6, T7> For(
            @Independent @Modified Iterable<T1> ts1,
            @Independent @Modified Iterable<T2> ts2,
            @Independent @Modified Iterable<T3> ts3,
            @Independent @Modified Iterable<T4> ts4,
            @Independent @Modified Iterable<T5> ts5,
            @Independent @Modified Iterable<T6> ts6,
            @Independent @Modified Iterable<T7> ts7) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8> API.For8<T1, T2, T3, T4, T5, T6, T7, T8> For(
            @Independent @Modified Iterable<T1> ts1,
            @Independent @Modified Iterable<T2> ts2,
            @Independent @Modified Iterable<T3> ts3,
            @Independent @Modified Iterable<T4> ts4,
            @Independent @Modified Iterable<T5> ts5,
            @Independent @Modified Iterable<T6> ts6,
            @Independent @Modified Iterable<T7> ts7,
            @Independent @Modified Iterable<T8> ts8) { return null; }

        @Independent @NotModified
        static <T, U> Iterator<U> For(
            @Independent @Modified Iterable<T> ts,
            @Independent @Modified Function<? super T, ? extends Iterable<U>> f) { return null; }

        @Independent @NotModified
        static <T1> API.For1List<T1> For(@Independent @NotModified List<T1> ts1) { return null; }

        @Independent @NotModified
        static <T1, T2> API.ForLazy2List<T1, T2> For(
            @Independent @NotModified List<T1> ts1,
            @Independent @Modified Function1<? super T1, List<T2>> ts2) { return null; }

        @Independent @NotModified
        static <T1, T2, T3> API.ForLazy3List<T1, T2, T3> For(
            @Independent @NotModified List<T1> ts1,
            @Independent @Modified Function1<? super T1, List<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, List<T3>> ts3) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4> API.ForLazy4List<T1, T2, T3, T4> For(
            @Independent @NotModified List<T1> ts1,
            @Independent @Modified Function1<? super T1, List<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, List<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, List<T4>> ts4) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5> API.ForLazy5List<T1, T2, T3, T4, T5> For(
            @Independent @NotModified List<T1> ts1,
            @Independent @Modified Function1<? super T1, List<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, List<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, List<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, List<T5>> ts5) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6> API.ForLazy6List<T1, T2, T3, T4, T5, T6> For(
            @Independent @NotModified List<T1> ts1,
            @Independent @Modified Function1<? super T1, List<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, List<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, List<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, List<T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, List<T6>> ts6) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7> API.ForLazy7List<T1, T2, T3, T4, T5, T6, T7> For(
            @Independent @NotModified List<T1> ts1,
            @Independent @Modified Function1<? super T1, List<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, List<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, List<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, List<T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, List<T6>> ts6,
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                List<T7>> ts7) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8> API.ForLazy8List<T1, T2, T3, T4, T5, T6, T7, T8> For(
            @Independent @NotModified List<T1> ts1,
            @Independent @Modified Function1<? super T1, List<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, List<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, List<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, List<T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, List<T6>> ts6,
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                List<T7>> ts7,
            @Independent @Modified Function7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                List<T8>> ts8) { return null; }

        @Independent @NotModified
        static <T1, T2> API.For2List<T1, T2> For(
            @Independent @NotModified List<T1> ts1,
            @Independent @NotModified List<T2> ts2) { return null; }

        @Independent @NotModified
        static <T1, T2, T3> API.For3List<T1, T2, T3> For(
            @Independent @NotModified List<T1> ts1,
            @Independent @NotModified List<T2> ts2,
            @Independent @NotModified List<T3> ts3) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4> API.For4List<T1, T2, T3, T4> For(
            @Independent @NotModified List<T1> ts1,
            @Independent @NotModified List<T2> ts2,
            @Independent @NotModified List<T3> ts3,
            @Independent @NotModified List<T4> ts4) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5> API.For5List<T1, T2, T3, T4, T5> For(
            @Independent @NotModified List<T1> ts1,
            @Independent @NotModified List<T2> ts2,
            @Independent @NotModified List<T3> ts3,
            @Independent @NotModified List<T4> ts4,
            @Independent @NotModified List<T5> ts5) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6> API.For6List<T1, T2, T3, T4, T5, T6> For(
            @Independent @NotModified List<T1> ts1,
            @Independent @NotModified List<T2> ts2,
            @Independent @NotModified List<T3> ts3,
            @Independent @NotModified List<T4> ts4,
            @Independent @NotModified List<T5> ts5,
            @Independent @NotModified List<T6> ts6) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7> API.For7List<T1, T2, T3, T4, T5, T6, T7> For(
            @Independent @NotModified List<T1> ts1,
            @Independent @NotModified List<T2> ts2,
            @Independent @NotModified List<T3> ts3,
            @Independent @NotModified List<T4> ts4,
            @Independent @NotModified List<T5> ts5,
            @Independent @NotModified List<T6> ts6,
            @Independent @NotModified List<T7> ts7) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8> API.For8List<T1, T2, T3, T4, T5, T6, T7, T8> For(
            @Independent @NotModified List<T1> ts1,
            @Independent @NotModified List<T2> ts2,
            @Independent @NotModified List<T3> ts3,
            @Independent @NotModified List<T4> ts4,
            @Independent @NotModified List<T5> ts5,
            @Independent @NotModified List<T6> ts6,
            @Independent @NotModified List<T7> ts7,
            @Independent @NotModified List<T8> ts8) { return null; }

        @Independent @NotModified
        static <T1> API.For1Future<T1> For(@Independent @Modified Future<T1> ts1) { return null; }

        @Independent @NotModified
        static <T1, T2> API.ForLazy2Future<T1, T2> For(
            @Independent @Modified Future<T1> ts1,
            @Independent @Modified Function1<? super T1, Future<T2>> ts2) { return null; }

        @Independent @NotModified
        static <T1, T2, T3> API.ForLazy3Future<T1, T2, T3> For(
            @Independent @Modified Future<T1> ts1,
            @Independent @Modified Function1<? super T1, Future<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Future<T3>> ts3) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4> API.ForLazy4Future<T1, T2, T3, T4> For(
            @Independent @Modified Future<T1> ts1,
            @Independent @Modified Function1<? super T1, Future<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Future<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Future<T4>> ts4) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5> API.ForLazy5Future<T1, T2, T3, T4, T5> For(
            @Independent @Modified Future<T1> ts1,
            @Independent @Modified Function1<? super T1, Future<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Future<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Future<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Future<T5>> ts5) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6> API.ForLazy6Future<T1, T2, T3, T4, T5, T6> For(
            @Independent @Modified Future<T1> ts1,
            @Independent @Modified Function1<? super T1, Future<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Future<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Future<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Future<T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Future<T6>> ts6) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7> API.ForLazy7Future<T1, T2, T3, T4, T5, T6, T7> For(
            @Independent @Modified Future<T1> ts1,
            @Independent @Modified Function1<? super T1, Future<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Future<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Future<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Future<T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Future<T6>> ts6,
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                Future<T7>> ts7) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8> API.ForLazy8Future<T1, T2, T3, T4, T5, T6, T7, T8> For(
            @Independent @Modified Future<T1> ts1,
            @Independent @Modified Function1<? super T1, Future<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Future<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Future<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Future<T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Future<T6>> ts6,
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                Future<T7>> ts7,
            @Independent @Modified Function7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                Future<T8>> ts8) { return null; }

        @Independent @NotModified
        static <T1, T2> API.For2Future<T1, T2> For(
            @Independent @Modified Future<T1> ts1,
            @Independent @Modified Future<T2> ts2) { return null; }

        @Independent @NotModified
        static <T1, T2, T3> API.For3Future<T1, T2, T3> For(
            @Independent @Modified Future<T1> ts1,
            @Independent @Modified Future<T2> ts2,
            @Independent @Modified Future<T3> ts3) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4> API.For4Future<T1, T2, T3, T4> For(
            @Independent @Modified Future<T1> ts1,
            @Independent @Modified Future<T2> ts2,
            @Independent @Modified Future<T3> ts3,
            @Independent @Modified Future<T4> ts4) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5> API.For5Future<T1, T2, T3, T4, T5> For(
            @Independent @Modified Future<T1> ts1,
            @Independent @Modified Future<T2> ts2,
            @Independent @Modified Future<T3> ts3,
            @Independent @Modified Future<T4> ts4,
            @Independent @Modified Future<T5> ts5) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6> API.For6Future<T1, T2, T3, T4, T5, T6> For(
            @Independent @Modified Future<T1> ts1,
            @Independent @Modified Future<T2> ts2,
            @Independent @Modified Future<T3> ts3,
            @Independent @Modified Future<T4> ts4,
            @Independent @Modified Future<T5> ts5,
            @Independent @Modified Future<T6> ts6) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7> API.For7Future<T1, T2, T3, T4, T5, T6, T7> For(
            @Independent @Modified Future<T1> ts1,
            @Independent @Modified Future<T2> ts2,
            @Independent @Modified Future<T3> ts3,
            @Independent @Modified Future<T4> ts4,
            @Independent @Modified Future<T5> ts5,
            @Independent @Modified Future<T6> ts6,
            @Independent @Modified Future<T7> ts7) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8> API.For8Future<T1, T2, T3, T4, T5, T6, T7, T8> For(
            @Independent @Modified Future<T1> ts1,
            @Independent @Modified Future<T2> ts2,
            @Independent @Modified Future<T3> ts3,
            @Independent @Modified Future<T4> ts4,
            @Independent @Modified Future<T5> ts5,
            @Independent @Modified Future<T6> ts6,
            @Independent @Modified Future<T7> ts7,
            @Independent @Modified Future<T8> ts8) { return null; }

        @Independent @NotModified
        static <L, T1> API.For1Either<L, T1> For(@Independent @NotModified Either<L, T1> ts1) { return null; }

        @Independent @NotModified
        static <L, T1, T2> API.ForLazy2Either<L, T1, T2> For(
            @Independent @NotModified Either<L, T1> ts1,
            @Independent @Modified Function1<? super T1, Either<L, T2>> ts2) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3> API.ForLazy3Either<L, T1, T2, T3> For(
            @Independent @NotModified Either<L, T1> ts1,
            @Independent @Modified Function1<? super T1, Either<L, T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Either<L, T3>> ts3) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4> API.ForLazy4Either<L, T1, T2, T3, T4> For(
            @Independent @NotModified Either<L, T1> ts1,
            @Independent @Modified Function1<? super T1, Either<L, T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Either<L, T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Either<L, T4>> ts4) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5> API.ForLazy5Either<L, T1, T2, T3, T4, T5> For(
            @Independent @NotModified Either<L, T1> ts1,
            @Independent @Modified Function1<? super T1, Either<L, T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Either<L, T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Either<L, T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Either<L, T5>> ts5) {
            return null;
        }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5, T6> API.ForLazy6Either<L, T1, T2, T3, T4, T5, T6> For(
            @Independent @NotModified Either<L, T1> ts1,
            @Independent @Modified Function1<? super T1, Either<L, T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Either<L, T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Either<L, T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Either<L, T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Either<L, T6>>
                ts6) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5, T6, T7> API.ForLazy7Either<L, T1, T2, T3, T4, T5, T6, T7> For(
            @Independent @NotModified Either<L, T1> ts1,
            @Independent @Modified Function1<? super T1, Either<L, T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Either<L, T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Either<L, T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Either<L, T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Either<L, T6>>
                ts6,
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                Either<L, T7>> ts7) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5, T6, T7, T8> API.ForLazy8Either<L, T1, T2, T3, T4, T5, T6, T7, T8> For(
            @Independent @NotModified Either<L, T1> ts1,
            @Independent @Modified Function1<? super T1, Either<L, T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Either<L, T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Either<L, T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Either<L, T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Either<L, T6>>
                ts6,
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                Either<L, T7>> ts7,
            @Independent @Modified Function7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                Either<L, T8>> ts8) { return null; }

        @Independent @NotModified
        static <L, T1, T2> API.For2Either<L, T1, T2> For(
            @Independent @NotModified Either<L, T1> ts1,
            @Independent @NotModified Either<L, T2> ts2) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3> API.For3Either<L, T1, T2, T3> For(
            @Independent @NotModified Either<L, T1> ts1,
            @Independent @NotModified Either<L, T2> ts2,
            @Independent @NotModified Either<L, T3> ts3) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4> API.For4Either<L, T1, T2, T3, T4> For(
            @Independent @NotModified Either<L, T1> ts1,
            @Independent @NotModified Either<L, T2> ts2,
            @Independent @NotModified Either<L, T3> ts3,
            @Independent @NotModified Either<L, T4> ts4) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5> API.For5Either<L, T1, T2, T3, T4, T5> For(
            @Independent @NotModified Either<L, T1> ts1,
            @Independent @NotModified Either<L, T2> ts2,
            @Independent @NotModified Either<L, T3> ts3,
            @Independent @NotModified Either<L, T4> ts4,
            @Independent @NotModified Either<L, T5> ts5) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5, T6> API.For6Either<L, T1, T2, T3, T4, T5, T6> For(
            @Independent @NotModified Either<L, T1> ts1,
            @Independent @NotModified Either<L, T2> ts2,
            @Independent @NotModified Either<L, T3> ts3,
            @Independent @NotModified Either<L, T4> ts4,
            @Independent @NotModified Either<L, T5> ts5,
            @Independent @NotModified Either<L, T6> ts6) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5, T6, T7> API.For7Either<L, T1, T2, T3, T4, T5, T6, T7> For(
            @Independent @NotModified Either<L, T1> ts1,
            @Independent @NotModified Either<L, T2> ts2,
            @Independent @NotModified Either<L, T3> ts3,
            @Independent @NotModified Either<L, T4> ts4,
            @Independent @NotModified Either<L, T5> ts5,
            @Independent @NotModified Either<L, T6> ts6,
            @Independent @NotModified Either<L, T7> ts7) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5, T6, T7, T8> API.For8Either<L, T1, T2, T3, T4, T5, T6, T7, T8> For(
            @Independent @NotModified Either<L, T1> ts1,
            @Independent @NotModified Either<L, T2> ts2,
            @Independent @NotModified Either<L, T3> ts3,
            @Independent @NotModified Either<L, T4> ts4,
            @Independent @NotModified Either<L, T5> ts5,
            @Independent @NotModified Either<L, T6> ts6,
            @Independent @NotModified Either<L, T7> ts7,
            @Independent @NotModified Either<L, T8> ts8) { return null; }

        @Independent @NotModified
        static <T1> API.For1Option<T1> For(@Independent @NotModified Option<T1> ts1) { return null; }

        @Independent @NotModified
        static <T1, T2> API.ForLazy2Option<T1, T2> For(
            @Independent @NotModified Option<T1> ts1,
            @Independent @Modified Function1<? super T1, Option<T2>> ts2) { return null; }

        @Independent @NotModified
        static <T1, T2, T3> API.ForLazy3Option<T1, T2, T3> For(
            @Independent @NotModified Option<T1> ts1,
            @Independent @Modified Function1<? super T1, Option<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Option<T3>> ts3) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4> API.ForLazy4Option<T1, T2, T3, T4> For(
            @Independent @NotModified Option<T1> ts1,
            @Independent @Modified Function1<? super T1, Option<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Option<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Option<T4>> ts4) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5> API.ForLazy5Option<T1, T2, T3, T4, T5> For(
            @Independent @NotModified Option<T1> ts1,
            @Independent @Modified Function1<? super T1, Option<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Option<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Option<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Option<T5>> ts5) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6> API.ForLazy6Option<T1, T2, T3, T4, T5, T6> For(
            @Independent @NotModified Option<T1> ts1,
            @Independent @Modified Function1<? super T1, Option<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Option<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Option<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Option<T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Option<T6>> ts6) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7> API.ForLazy7Option<T1, T2, T3, T4, T5, T6, T7> For(
            @Independent @NotModified Option<T1> ts1,
            @Independent @Modified Function1<? super T1, Option<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Option<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Option<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Option<T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Option<T6>> ts6,
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                Option<T7>> ts7) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8> API.ForLazy8Option<T1, T2, T3, T4, T5, T6, T7, T8> For(
            @Independent @NotModified Option<T1> ts1,
            @Independent @Modified Function1<? super T1, Option<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Option<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Option<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Option<T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Option<T6>> ts6,
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                Option<T7>> ts7,
            @Independent @Modified Function7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                Option<T8>> ts8) { return null; }

        @Independent @NotModified
        static <T1, T2> API.For2Option<T1, T2> For(
            @Independent @NotModified Option<T1> ts1,
            @Independent @NotModified Option<T2> ts2) { return null; }

        @Independent @NotModified
        static <T1, T2, T3> API.For3Option<T1, T2, T3> For(
            @Independent @NotModified Option<T1> ts1,
            @Independent @NotModified Option<T2> ts2,
            @Independent @NotModified Option<T3> ts3) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4> API.For4Option<T1, T2, T3, T4> For(
            @Independent @NotModified Option<T1> ts1,
            @Independent @NotModified Option<T2> ts2,
            @Independent @NotModified Option<T3> ts3,
            @Independent @NotModified Option<T4> ts4) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5> API.For5Option<T1, T2, T3, T4, T5> For(
            @Independent @NotModified Option<T1> ts1,
            @Independent @NotModified Option<T2> ts2,
            @Independent @NotModified Option<T3> ts3,
            @Independent @NotModified Option<T4> ts4,
            @Independent @NotModified Option<T5> ts5) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6> API.For6Option<T1, T2, T3, T4, T5, T6> For(
            @Independent @NotModified Option<T1> ts1,
            @Independent @NotModified Option<T2> ts2,
            @Independent @NotModified Option<T3> ts3,
            @Independent @NotModified Option<T4> ts4,
            @Independent @NotModified Option<T5> ts5,
            @Independent @NotModified Option<T6> ts6) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7> API.For7Option<T1, T2, T3, T4, T5, T6, T7> For(
            @Independent @NotModified Option<T1> ts1,
            @Independent @NotModified Option<T2> ts2,
            @Independent @NotModified Option<T3> ts3,
            @Independent @NotModified Option<T4> ts4,
            @Independent @NotModified Option<T5> ts5,
            @Independent @NotModified Option<T6> ts6,
            @Independent @NotModified Option<T7> ts7) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8> API.For8Option<T1, T2, T3, T4, T5, T6, T7, T8> For(
            @Independent @NotModified Option<T1> ts1,
            @Independent @NotModified Option<T2> ts2,
            @Independent @NotModified Option<T3> ts3,
            @Independent @NotModified Option<T4> ts4,
            @Independent @NotModified Option<T5> ts5,
            @Independent @NotModified Option<T6> ts6,
            @Independent @NotModified Option<T7> ts7,
            @Independent @NotModified Option<T8> ts8) { return null; }
        @Independent @NotModified static <T1> API.For1Try<T1> For(@Independent @Modified Try<T1> ts1) { return null; }
        @Independent @NotModified
        static <T1, T2> API.ForLazy2Try<T1, T2> For(
            @Independent @Modified Try<T1> ts1,
            @Independent @Modified Function1<? super T1, Try<T2>> ts2) { return null; }

        @Independent @NotModified
        static <T1, T2, T3> API.ForLazy3Try<T1, T2, T3> For(
            @Independent @Modified Try<T1> ts1,
            @Independent @Modified Function1<? super T1, Try<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Try<T3>> ts3) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4> API.ForLazy4Try<T1, T2, T3, T4> For(
            @Independent @Modified Try<T1> ts1,
            @Independent @Modified Function1<? super T1, Try<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Try<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Try<T4>> ts4) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5> API.ForLazy5Try<T1, T2, T3, T4, T5> For(
            @Independent @Modified Try<T1> ts1,
            @Independent @Modified Function1<? super T1, Try<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Try<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Try<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Try<T5>> ts5) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6> API.ForLazy6Try<T1, T2, T3, T4, T5, T6> For(
            @Independent @Modified Try<T1> ts1,
            @Independent @Modified Function1<? super T1, Try<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Try<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Try<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Try<T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Try<T6>> ts6) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7> API.ForLazy7Try<T1, T2, T3, T4, T5, T6, T7> For(
            @Independent @Modified Try<T1> ts1,
            @Independent @Modified Function1<? super T1, Try<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Try<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Try<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Try<T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Try<T6>> ts6,
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                Try<T7>> ts7) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8> API.ForLazy8Try<T1, T2, T3, T4, T5, T6, T7, T8> For(
            @Independent @Modified Try<T1> ts1,
            @Independent @Modified Function1<? super T1, Try<T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Try<T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Try<T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Try<T5>> ts5,
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Try<T6>> ts6,
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                Try<T7>> ts7,
            @Independent @Modified Function7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                Try<T8>> ts8) { return null; }

        @Independent @NotModified
        static <T1, T2> API.For2Try<T1, T2> For(@Independent @Modified Try<T1> ts1, @Independent @Modified Try<T2> ts2) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3> API.For3Try<T1, T2, T3> For(
            @Independent @Modified Try<T1> ts1,
            @Independent @Modified Try<T2> ts2,
            @Independent @Modified Try<T3> ts3) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4> API.For4Try<T1, T2, T3, T4> For(
            @Independent @Modified Try<T1> ts1,
            @Independent @Modified Try<T2> ts2,
            @Independent @Modified Try<T3> ts3,
            @Independent @Modified Try<T4> ts4) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5> API.For5Try<T1, T2, T3, T4, T5> For(
            @Independent @Modified Try<T1> ts1,
            @Independent @Modified Try<T2> ts2,
            @Independent @Modified Try<T3> ts3,
            @Independent @Modified Try<T4> ts4,
            @Independent @Modified Try<T5> ts5) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6> API.For6Try<T1, T2, T3, T4, T5, T6> For(
            @Independent @Modified Try<T1> ts1,
            @Independent @Modified Try<T2> ts2,
            @Independent @Modified Try<T3> ts3,
            @Independent @Modified Try<T4> ts4,
            @Independent @Modified Try<T5> ts5,
            @Independent @Modified Try<T6> ts6) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7> API.For7Try<T1, T2, T3, T4, T5, T6, T7> For(
            @Independent @Modified Try<T1> ts1,
            @Independent @Modified Try<T2> ts2,
            @Independent @Modified Try<T3> ts3,
            @Independent @Modified Try<T4> ts4,
            @Independent @Modified Try<T5> ts5,
            @Independent @Modified Try<T6> ts6,
            @Independent @Modified Try<T7> ts7) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8> API.For8Try<T1, T2, T3, T4, T5, T6, T7, T8> For(
            @Independent @Modified Try<T1> ts1,
            @Independent @Modified Try<T2> ts2,
            @Independent @Modified Try<T3> ts3,
            @Independent @Modified Try<T4> ts4,
            @Independent @Modified Try<T5> ts5,
            @Independent @Modified Try<T6> ts6,
            @Independent @Modified Try<T7> ts7,
            @Independent @Modified Try<T8> ts8) { return null; }

        @Independent @NotModified
        static <L, T1> API.For1Validation<L, T1> For(@Independent @NotModified Validation<L, T1> ts1) { return null; }

        @Independent @NotModified
        static <L, T1, T2> API.ForLazy2Validation<L, T1, T2> For(
            @Independent @Modified Validation<L, T1> ts1,
            @Independent @Modified Function1<? super T1, Validation<L, T2>> ts2) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3> API.ForLazy3Validation<L, T1, T2, T3> For(
            @Independent @Modified Validation<L, T1> ts1,
            @Independent @Modified Function1<? super T1, Validation<L, T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Validation<L, T3>> ts3) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4> API.ForLazy4Validation<L, T1, T2, T3, T4> For(
            @Independent @Modified Validation<L, T1> ts1,
            @Independent @Modified Function1<? super T1, Validation<L, T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Validation<L, T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Validation<L, T4>> ts4) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5> API.ForLazy5Validation<L, T1, T2, T3, T4, T5> For(
            @Independent @Modified Validation<L, T1> ts1,
            @Independent @Modified Function1<? super T1, Validation<L, T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Validation<L, T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Validation<L, T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Validation<L, T5>> ts5) {
            return null;
        }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5, T6> API.ForLazy6Validation<L, T1, T2, T3, T4, T5, T6> For(
            @Independent @Modified Validation<L, T1> ts1,
            @Independent @Modified Function1<? super T1, Validation<L, T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Validation<L, T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Validation<L, T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Validation<L, T5>> ts5,
            @Independent @Modified Function5<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                Validation<L, T6>> ts6) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5, T6, T7> API.ForLazy7Validation<L, T1, T2, T3, T4, T5, T6, T7> For(
            @Independent @Modified Validation<L, T1> ts1,
            @Independent @Modified Function1<? super T1, Validation<L, T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Validation<L, T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Validation<L, T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Validation<L, T5>> ts5,
            @Independent @Modified Function5<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                Validation<L, T6>> ts6,
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                Validation<L, T7>> ts7) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5, T6, T7, T8> API.ForLazy8Validation<L, T1, T2, T3, T4, T5, T6, T7, T8> For(
            @Independent @Modified Validation<L, T1> ts1,
            @Independent @Modified Function1<? super T1, Validation<L, T2>> ts2,
            @Independent @Modified Function2<? super T1, ? super T2, Validation<L, T3>> ts3,
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, Validation<L, T4>> ts4,
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, Validation<L, T5>> ts5,
            @Independent @Modified Function5<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                Validation<L, T6>> ts6,
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                Validation<L, T7>> ts7,
            @Independent @Modified Function7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                Validation<L, T8>> ts8) { return null; }

        @Independent @NotModified
        static <L, T1, T2> API.For2Validation<L, T1, T2> For(
            @Independent @Modified Validation<L, T1> ts1,
            @Independent @Modified Validation<L, T2> ts2) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3> API.For3Validation<L, T1, T2, T3> For(
            @Independent @Modified Validation<L, T1> ts1,
            @Independent @Modified Validation<L, T2> ts2,
            @Independent @Modified Validation<L, T3> ts3) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4> API.For4Validation<L, T1, T2, T3, T4> For(
            @Independent @Modified Validation<L, T1> ts1,
            @Independent @Modified Validation<L, T2> ts2,
            @Independent @Modified Validation<L, T3> ts3,
            @Independent @Modified Validation<L, T4> ts4) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5> API.For5Validation<L, T1, T2, T3, T4, T5> For(
            @Independent @Modified Validation<L, T1> ts1,
            @Independent @Modified Validation<L, T2> ts2,
            @Independent @Modified Validation<L, T3> ts3,
            @Independent @Modified Validation<L, T4> ts4,
            @Independent @Modified Validation<L, T5> ts5) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5, T6> API.For6Validation<L, T1, T2, T3, T4, T5, T6> For(
            @Independent @Modified Validation<L, T1> ts1,
            @Independent @Modified Validation<L, T2> ts2,
            @Independent @Modified Validation<L, T3> ts3,
            @Independent @Modified Validation<L, T4> ts4,
            @Independent @Modified Validation<L, T5> ts5,
            @Independent @Modified Validation<L, T6> ts6) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5, T6, T7> API.For7Validation<L, T1, T2, T3, T4, T5, T6, T7> For(
            @Independent @Modified Validation<L, T1> ts1,
            @Independent @Modified Validation<L, T2> ts2,
            @Independent @Modified Validation<L, T3> ts3,
            @Independent @Modified Validation<L, T4> ts4,
            @Independent @Modified Validation<L, T5> ts5,
            @Independent @Modified Validation<L, T6> ts6,
            @Independent @Modified Validation<L, T7> ts7) { return null; }

        @Independent @NotModified
        static <L, T1, T2, T3, T4, T5, T6, T7, T8> API.For8Validation<L, T1, T2, T3, T4, T5, T6, T7, T8> For(
            @Independent @Modified Validation<L, T1> ts1,
            @Independent @Modified Validation<L, T2> ts2,
            @Independent @Modified Validation<L, T3> ts3,
            @Independent @Modified Validation<L, T4> ts4,
            @Independent @Modified Validation<L, T5> ts5,
            @Independent @Modified Validation<L, T6> ts6,
            @Independent @Modified Validation<L, T7> ts7,
            @Independent @Modified Validation<L, T8> ts8) { return null; }

        @Independent @NotModified
        static <R> Function0<R> Function(@Independent @NotModified Function0<R> methodReference) { return null; }

        @Independent @NotModified
        static <T1, R> Function1<T1, R> Function(@Independent @NotModified Function1<T1, R> methodReference) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, R> Function2<T1, T2, R> Function(@Independent @NotModified Function2<T1, T2, R> methodReference) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, R> Function3<T1, T2, T3, R> Function(
            @Independent @NotModified Function3<T1, T2, T3, R> methodReference) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, R> Function4<T1, T2, T3, T4, R> Function(
            @Independent @NotModified Function4<T1, T2, T3, T4, R> methodReference) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, R> Function5<T1, T2, T3, T4, T5, R> Function(
            @Independent @NotModified Function5<T1, T2, T3, T4, T5, R> methodReference) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, R> Function6<T1, T2, T3, T4, T5, T6, R> Function(
            @Independent @NotModified Function6<T1, T2, T3, T4, T5, T6, R> methodReference) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, R> Function7<T1, T2, T3, T4, T5, T6, T7, R> Function(
            @Independent @NotModified Function7<T1, T2, T3, T4, T5, T6, T7, R> methodReference) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> Function(
            @Independent @NotModified Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> methodReference) { return null; }
        @Independent @NotModified static <T> Future<T> Future(@Independent @NotModified T result) { return null; }
        @Independent @NotModified
        static <T> Future<T> Future(@Independent @Modified CheckedFunction0<? extends T> computation) { return null; }

        @Independent @NotModified
        static <T> Future<T> Future(@Independent @Modified Executor executorService, @Independent @NotModified T result) {
            return null;
        }

        @Independent @NotModified
        static <T> Future<T> Future(
            @Independent @Modified Executor executorService,
            @Independent @Modified CheckedFunction0<? extends T> computation) { return null; }
        @Independent @NotModified static <T> IndexedSeq<T> IndexedSeq() { return null; }
        @Independent @NotModified
        static <T> IndexedSeq<T> IndexedSeq(@Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T> IndexedSeq<T> IndexedSeq(@Independent @Modified T ... elements) { return null; }

        @Independent @NotModified
        static <E, T> Validation.Invalid<E, T> Invalid(@Independent @NotModified E error) { return null; }

        @Independent @NotModified
        static <T> Lazy<T> Lazy(@Independent @Modified Supplier<? extends T> supplier) { return null; }

        @Independent @NotModified
        static <L, R> Either.Left<L, R> Left(@Independent @NotModified L left) { return null; }
        @Independent @NotModified static <K, V> io.vavr.collection.Map<K, V> LinkedMap() { return null; }
        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> LinkedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> LinkedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> LinkedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> LinkedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> LinkedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> LinkedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> LinkedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> LinkedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> LinkedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8,
            @Independent @NotModified K k9,
            @Independent @NotModified V v9) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> LinkedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8,
            @Independent @NotModified K k9,
            @Independent @NotModified V v9,
            @Independent @NotModified K k10,
            @Independent @NotModified V v10) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> LinkedMap(
            @Independent @Modified Tuple2<? extends K, ? extends V> ... entries) { return null; }
        @Independent @NotModified static <T> io.vavr.collection.Set<T> LinkedSet() { return null; }
        @Independent @NotModified
        static <T> io.vavr.collection.Set<T> LinkedSet(@Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Set<T> LinkedSet(@Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified static <T> List<T> List() { return null; }
        @Independent @NotModified static <T> List<T> List(@Independent @NotModified T element) { return null; }
        @Independent @NotModified static <T> List<T> List(@Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified static <K, V> io.vavr.collection.Map<K, V> Map() { return null; }
        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> Map(@Independent @NotModified K k1, @Independent @NotModified V v1) {
            return null;
        }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> Map(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> Map(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> Map(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> Map(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> Map(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> Map(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> Map(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> Map(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8,
            @Independent @NotModified K k9,
            @Independent @NotModified V v9) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> Map(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8,
            @Independent @NotModified K k9,
            @Independent @NotModified V v9,
            @Independent @NotModified K k10,
            @Independent @NotModified V v10) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> Map(
            @Independent @NotModified Tuple2<? extends K, ? extends V> ... entries) { return null; }
        @Independent @NotModified static <T> API.Match<T> Match(@Independent @NotModified T value) { return null; }
        @Independent @NotModified static <T> Option.None<T> None() { return null; }
        @Independent @NotModified static <T> Option<T> Option(@Independent @NotModified T value) { return null; }
        @Independent @NotModified
        static <T extends Comparable<? super T>> PriorityQueue<T> PriorityQueue() { return null; }

        @Independent @NotModified
        static <T extends Comparable<? super T>> PriorityQueue<T> PriorityQueue(@Independent @NotModified T element) {
            return null;
        }

        @Independent @NotModified
        static <T extends Comparable<? super T>> PriorityQueue<T> PriorityQueue(
            @Independent @NotModified T ... elements) { return null; }

        @Independent @NotModified
        static <T extends Comparable<? super T>> PriorityQueue<T> PriorityQueue(
            @Independent @Modified Comparator<? super T> comparator) { return null; }

        @Independent @NotModified
        static <T> PriorityQueue<T> PriorityQueue(
            @Independent @Modified Comparator<? super T> comparator,
            @Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T> PriorityQueue<T> PriorityQueue(
            @Independent @Modified Comparator<? super T> comparator,
            @Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified static <T> Queue<T> Queue() { return null; }
        @Independent @NotModified static <T> Queue<T> Queue(@Independent @Modified T element) { return null; }
        @Independent @NotModified static <T> Queue<T> Queue(@Independent @Modified T ... elements) { return null; }
        @Independent @NotModified
        static <L, R> Either.Right<L, R> Right(@Independent @NotModified R right) { return null; }
        @Independent @NotModified static <T> Seq<T> Seq() { return null; }
        @Independent @NotModified static <T> Seq<T> Seq(@Independent @NotModified T element) { return null; }
        @Independent @NotModified static <T> Seq<T> Seq(@Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified static <T> io.vavr.collection.Set<T> Set() { return null; }
        @Independent @NotModified
        static <T> io.vavr.collection.Set<T> Set(@Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Set<T> Set(@Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified static <T> Option.Some<T> Some(@Independent @NotModified T value) { return null; }
        @Independent @NotModified
        static <K extends Comparable<? super K>, V> SortedMap<K, V> SortedMap() { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> SortedMap<K, V> SortedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> SortedMap<K, V> SortedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> SortedMap<K, V> SortedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> SortedMap<K, V> SortedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> SortedMap<K, V> SortedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> SortedMap<K, V> SortedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> SortedMap<K, V> SortedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> SortedMap<K, V> SortedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> SortedMap<K, V> SortedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8,
            @Independent @NotModified K k9,
            @Independent @NotModified V v9) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> SortedMap<K, V> SortedMap(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8,
            @Independent @NotModified K k9,
            @Independent @NotModified V v9,
            @Independent @NotModified K k10,
            @Independent @NotModified V v10) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> SortedMap<K, V> SortedMap(
            @Independent @NotModified Tuple2<? extends K, ? extends V> ... entries) { return null; }

        @Independent @NotModified
        static <K, V> SortedMap<K, V> SortedMap(@Independent @NotModified Comparator<? super K> keyComparator) {
            return null;
        }

        @Independent @NotModified
        static <K, V> SortedMap<K, V> SortedMap(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified K key,
            @Independent @NotModified V value) { return null; }

        @Independent @NotModified
        static <K, V> SortedMap<K, V> SortedMap(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified Tuple2<? extends K, ? extends V> ... entries) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> SortedMap<K, V> SortedMap(
            @Independent @NotModified Map<? extends K, ? extends V> map) { return null; }
        @Independent @NotModified static <T extends Comparable<? super T>> SortedSet<T> SortedSet() { return null; }
        @Independent @NotModified
        static <T extends Comparable<? super T>> SortedSet<T> SortedSet(@Independent @NotModified T element) {
            return null;
        }

        @Independent @NotModified
        static <T extends Comparable<? super T>> SortedSet<T> SortedSet(@Independent @NotModified T ... elements) {
            return null;
        }

        @Independent @NotModified
        static <T extends Comparable<? super T>> SortedSet<T> SortedSet(
            @Independent @NotModified Comparator<? super T> comparator) { return null; }

        @Independent @NotModified
        static <T> SortedSet<T> SortedSet(
            @Independent @NotModified Comparator<? super T> comparator,
            @Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T> SortedSet<T> SortedSet(
            @Independent @NotModified Comparator<? super T> comparator,
            @Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified static <T> io.vavr.collection.Stream<T> Stream() { return null; }
        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> Stream(@Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> Stream(@Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified static <T> Try.Success<T> Success(@Independent @NotModified T value) { return null; }
        @Independent @NotModified static <T> T TODO() { return null; }
        @Independent @NotModified static <T> T TODO(String msg) { return null; }
        @Independent @NotModified
        static <T> Try<T> Try(@Independent @Modified CheckedFunction0<? extends T> supplier) { return null; }
        @Independent @NotModified static Tuple0 Tuple() { return null; }
        @Independent @NotModified static <T1> Tuple1<T1> Tuple(@Independent @NotModified T1 t1) { return null; }
        @Independent @NotModified
        static <T1, T2> Tuple2<T1, T2> Tuple(@Independent @NotModified T1 t1, @Independent @NotModified T2 t2) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3> Tuple3<T1, T2, T3> Tuple(
            @Independent @NotModified T1 t1,
            @Independent @NotModified T2 t2,
            @Independent @NotModified T3 t3) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> Tuple(
            @Independent @NotModified T1 t1,
            @Independent @NotModified T2 t2,
            @Independent @NotModified T3 t3,
            @Independent @NotModified T4 t4) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> Tuple(
            @Independent @NotModified T1 t1,
            @Independent @NotModified T2 t2,
            @Independent @NotModified T3 t3,
            @Independent @NotModified T4 t4,
            @Independent @NotModified T5 t5) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> Tuple(
            @Independent @NotModified T1 t1,
            @Independent @NotModified T2 t2,
            @Independent @NotModified T3 t3,
            @Independent @NotModified T4 t4,
            @Independent @NotModified T5 t5,
            @Independent @NotModified T6 t6) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> Tuple(
            @Independent @NotModified T1 t1,
            @Independent @NotModified T2 t2,
            @Independent @NotModified T3 t3,
            @Independent @NotModified T4 t4,
            @Independent @NotModified T5 t5,
            @Independent @NotModified T6 t6,
            @Independent @NotModified T7 t7) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> Tuple(
            @Independent @NotModified T1 t1,
            @Independent @NotModified T2 t2,
            @Independent @NotModified T3 t3,
            @Independent @NotModified T4 t4,
            @Independent @NotModified T5 t5,
            @Independent @NotModified T6 t6,
            @Independent @NotModified T7 t7,
            @Independent @NotModified T8 t8) { return null; }

        @Independent @NotModified
        static <E, T> Validation.Valid<E, T> Valid(@Independent @NotModified T value) { return null; }
        @Independent @NotModified static <T> Vector<T> Vector() { return null; }
        @Independent @NotModified static <T> Vector<T> Vector(@Independent @NotModified T element) { return null; }
        @Independent @NotModified static <T> Vector<T> Vector(@Independent @Modified T ... elements) { return null; }
        @NotModified static void print(@Independent @NotModified Object obj) { }
        @NotModified static void printf(String format, @Independent @NotModified Object ... args) { }
        @NotModified static void println() { }
        @NotModified static void println(@Independent @NotModified Object obj) { }
        @NotModified static Void run(@Independent @Modified Runnable unit) { return null; }
        @Independent @NotModified
        static <R> Function0<R> unchecked(@Independent @NotModified CheckedFunction0<R> f) { return null; }

        @Independent @NotModified
        static <T1, R> Function1<T1, R> unchecked(@Independent @NotModified CheckedFunction1<T1, R> f) { return null; }

        @Independent @NotModified
        static <T1, T2, R> Function2<T1, T2, R> unchecked(@Independent @NotModified CheckedFunction2<T1, T2, R> f) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, R> Function3<T1, T2, T3, R> unchecked(
            @Independent @NotModified CheckedFunction3<T1, T2, T3, R> f) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, R> Function4<T1, T2, T3, T4, R> unchecked(
            @Independent @NotModified CheckedFunction4<T1, T2, T3, T4, R> f) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, R> Function5<T1, T2, T3, T4, T5, R> unchecked(
            @Independent @NotModified CheckedFunction5<T1, T2, T3, T4, T5, R> f) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, R> Function6<T1, T2, T3, T4, T5, T6, R> unchecked(
            @Independent @NotModified CheckedFunction6<T1, T2, T3, T4, T5, T6, R> f) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, R> Function7<T1, T2, T3, T4, T5, T6, T7, R> unchecked(
            @Independent @NotModified CheckedFunction7<T1, T2, T3, T4, T5, T6, T7, R> f) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> unchecked(
            @Independent @NotModified CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> f) { return null; }
    }

    //public interface CheckedConsumer
    @FinalFields
    @Independent
    class CheckedConsumer$<T> {
        @Modified void accept(@Independent(absent = true) @NotModified T arg0) { }
        @Independent @NotModified
        CheckedConsumer<T> andThen(@Independent @Modified CheckedConsumer<? super T> after) { return null; }

        @Identity @Independent @NotModified
        static <T> CheckedConsumer<T> of(@Independent @NotModified CheckedConsumer<T> methodReference) { return null; }
        @Independent @NotModified Consumer<T> unchecked() { return null; }
    }

    //public interface CheckedFunction0 implements Serializable
    @FinalFields
    @Independent
    class CheckedFunction0$<R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> CheckedFunction0<V> andThen(@Independent @Modified CheckedFunction1<? super R, ? extends V> after) {
            return null;
        }
        @Independent(absent = true) @Modified R apply() { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        static <R> CheckedFunction0<R> constant(@Independent @NotModified R value) { return null; }
        @Fluent @Independent @NotModified CheckedFunction0<R> curried() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <R> Function0<Option<R>> lift(@Independent @Modified CheckedFunction0<? extends R> partialFunction) {
            return null;
        }

        @Independent @NotModified
        static <R> Function0<Try<R>> liftTry(@Independent @Modified CheckedFunction0<? extends R> partialFunction) {
            return null;
        }
        @Fluent @Independent @NotModified CheckedFunction0<R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <R> CheckedFunction0<R> narrow(@Independent @NotModified CheckedFunction0<? extends R> f) { return null; }

        @Identity @Independent @NotModified
        static <R> CheckedFunction0<R> of(@Independent @NotModified CheckedFunction0<R> methodReference) { return null; }

        @Independent @NotModified
        Function0<R> recover(
            @Independent @Modified Function<? super Throwable, ? extends Supplier<? extends R>> recover) { return null; }
        @Fluent @Independent @NotModified CheckedFunction0<R> reversed() { return null; }
        @Independent @NotModified CheckedFunction1<Tuple0, R> tupled() { return null; }
        @Independent @NotModified Function0<R> unchecked() { return null; }
    }

    //public interface CheckedFunction1 implements Serializable
    @FinalFields
    @Independent
    class CheckedFunction1$<T1, R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> CheckedFunction1<T1, V> andThen(@Independent @Modified CheckedFunction1<? super R, ? extends V> after) {
            return null;
        }

        @Independent(absent = true) @Modified
        R apply(@Independent(absent = true) @NotModified T1 arg0) { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <V> CheckedFunction1<V, R> compose(@Independent @Modified CheckedFunction1<? super V, ? extends T1> before) {
            return null;
        }

        @Independent @NotModified
        <S> CheckedFunction1<S, R> compose1(@Independent @Modified Function1<? super S, ? extends T1> before) {
            return null;
        }

        @Independent @NotModified
        static <T1, R> CheckedFunction1<T1, R> constant(@Independent @NotModified R value) { return null; }
        @Fluent @Independent @NotModified CheckedFunction1<T1, R> curried() { return null; }
        @Independent @NotModified static <T> CheckedFunction1<T, T> identity() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, R> Function1<T1, Option<R>> lift(
            @Independent @Modified CheckedFunction1<? super T1, ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static <T1, R> Function1<T1, Try<R>> liftTry(
            @Independent @Modified CheckedFunction1<? super T1, ? extends R> partialFunction) { return null; }
        @Fluent @Independent @NotModified CheckedFunction1<T1, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, R> CheckedFunction1<T1, R> narrow(
            @Independent @NotModified CheckedFunction1<? super T1, ? extends R> f) { return null; }

        @Identity @Independent @NotModified
        static <T1, R> CheckedFunction1<T1, R> of(@Independent @NotModified CheckedFunction1<T1, R> methodReference) {
            return null;
        }

        @Independent @NotModified
        Function1<T1, R> recover(
            @Independent @Modified Function<? super Throwable, ? extends Function<? super T1, ? extends R>> recover) {
            return null;
        }
        @Fluent @Independent @NotModified CheckedFunction1<T1, R> reversed() { return null; }
        @Independent @NotModified CheckedFunction1<Tuple1<T1>, R> tupled() { return null; }
        @Independent @NotModified Function1<T1, R> unchecked() { return null; }
    }

    //public interface CheckedFunction2 implements Serializable
    @FinalFields
    @Independent
    class CheckedFunction2$<T1, T2, R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> CheckedFunction2<T1, T2, V> andThen(@Independent @Modified CheckedFunction1<? super R, ? extends V> after) {
            return null;
        }
        @Independent @NotModified CheckedFunction1<T2, R> apply(@Independent @Modified T1 t1) { return null; }
        @Independent(absent = true) @Modified
        R apply(@Independent(absent = true) @NotModified T1 arg0, @Independent(absent = true) @NotModified T2 arg1) {
            return null;
        }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <S> CheckedFunction2<S, T2, R> compose1(@Independent @Modified Function1<? super S, ? extends T1> before) {
            return null;
        }

        @Independent @NotModified
        <S> CheckedFunction2<T1, S, R> compose2(@Independent @Modified Function1<? super S, ? extends T2> before) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, R> CheckedFunction2<T1, T2, R> constant(@Independent @NotModified R value) { return null; }
        @Independent @NotModified Function1<T1, CheckedFunction1<T2, R>> curried() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, T2, R> Function2<T1, T2, Option<R>> lift(
            @Independent @Modified CheckedFunction2<? super T1, ? super T2, ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static <T1, T2, R> Function2<T1, T2, Try<R>> liftTry(
            @Independent @Modified CheckedFunction2<? super T1, ? super T2, ? extends R> partialFunction) { return null; }
        @Fluent @Independent @NotModified CheckedFunction2<T1, T2, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, T2, R> CheckedFunction2<T1, T2, R> narrow(
            @Independent @NotModified CheckedFunction2<? super T1, ? super T2, ? extends R> f) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, R> CheckedFunction2<T1, T2, R> of(
            @Independent @NotModified CheckedFunction2<T1, T2, R> methodReference) { return null; }

        @Independent @NotModified
        Function2<T1, T2, R> recover(
            @Independent @Modified Function<
                ? super Throwable,
                ? extends BiFunction<? super T1, ? super T2, ? extends R>> recover) { return null; }
        @Independent @NotModified CheckedFunction2<T2, T1, R> reversed() { return null; }
        @Independent @NotModified CheckedFunction1<Tuple2<T1, T2>, R> tupled() { return null; }
        @Independent @NotModified Function2<T1, T2, R> unchecked() { return null; }
    }

    //public interface CheckedFunction3 implements Serializable
    @FinalFields
    @Independent
    class CheckedFunction3$<T1, T2, T3, R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> CheckedFunction3<T1, T2, T3, V> andThen(
            @Independent @Modified CheckedFunction1<? super R, ? extends V> after) { return null; }
        @Independent @NotModified CheckedFunction2<T2, T3, R> apply(@Independent @Modified T1 t1) { return null; }
        @Independent @NotModified
        CheckedFunction1<T3, R> apply(@Independent @Modified T1 t1, @Independent @Modified T2 t2) { return null; }

        @Independent(absent = true) @Modified
        R apply(
            @Independent(absent = true) @NotModified T1 arg0,
            @Independent(absent = true) @NotModified T2 arg1,
            @Independent(absent = true) @NotModified T3 arg2) { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <S> CheckedFunction3<S, T2, T3, R> compose1(@Independent @Modified Function1<? super S, ? extends T1> before) {
            return null;
        }

        @Independent @NotModified
        <S> CheckedFunction3<T1, S, T3, R> compose2(@Independent @Modified Function1<? super S, ? extends T2> before) {
            return null;
        }

        @Independent @NotModified
        <S> CheckedFunction3<T1, T2, S, R> compose3(@Independent @Modified Function1<? super S, ? extends T3> before) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, R> CheckedFunction3<T1, T2, T3, R> constant(@Independent @NotModified R value) {
            return null;
        }
        @Independent @NotModified Function1<T1, Function1<T2, CheckedFunction1<T3, R>>> curried() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, T2, T3, R> Function3<T1, T2, T3, Option<R>> lift(
            @Independent @Modified CheckedFunction3<? super T1, ? super T2, ? super T3, ? extends R> partialFunction) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, R> Function3<T1, T2, T3, Try<R>> liftTry(
            @Independent @Modified CheckedFunction3<? super T1, ? super T2, ? super T3, ? extends R> partialFunction) {
            return null;
        }
        @Fluent @Independent @NotModified CheckedFunction3<T1, T2, T3, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, T2, T3, R> CheckedFunction3<T1, T2, T3, R> narrow(
            @Independent @NotModified CheckedFunction3<? super T1, ? super T2, ? super T3, ? extends R> f) {
            return null;
        }

        @Identity @Independent @NotModified
        static <T1, T2, T3, R> CheckedFunction3<T1, T2, T3, R> of(
            @Independent @NotModified CheckedFunction3<T1, T2, T3, R> methodReference) { return null; }

        @Independent @NotModified
        Function3<T1, T2, T3, R> recover(
            @Independent @Modified Function<
                ? super Throwable,
                ? extends Function3<? super T1, ? super T2, ? super T3, ? extends R>> recover) { return null; }
        @Independent @NotModified CheckedFunction3<T3, T2, T1, R> reversed() { return null; }
        @Independent @NotModified CheckedFunction1<Tuple3<T1, T2, T3>, R> tupled() { return null; }
        @Independent @NotModified Function3<T1, T2, T3, R> unchecked() { return null; }
    }

    //public interface CheckedFunction4 implements Serializable
    @FinalFields
    @Independent
    class CheckedFunction4$<T1, T2, T3, T4, R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> CheckedFunction4<T1, T2, T3, T4, V> andThen(
            @Independent @Modified CheckedFunction1<? super R, ? extends V> after) { return null; }
        @Independent @NotModified CheckedFunction3<T2, T3, T4, R> apply(@Independent @Modified T1 t1) { return null; }
        @Independent @NotModified
        CheckedFunction2<T3, T4, R> apply(@Independent @Modified T1 t1, @Independent @Modified T2 t2) { return null; }

        @Independent @NotModified
        CheckedFunction1<T4, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3) { return null; }

        @Independent(absent = true) @Modified
        R apply(
            @Independent(absent = true) @NotModified T1 arg0,
            @Independent(absent = true) @NotModified T2 arg1,
            @Independent(absent = true) @NotModified T3 arg2,
            @Independent(absent = true) @NotModified T4 arg3) { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <S> CheckedFunction4<S, T2, T3, T4, R> compose1(
            @Independent @Modified Function1<? super S, ? extends T1> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction4<T1, S, T3, T4, R> compose2(
            @Independent @Modified Function1<? super S, ? extends T2> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction4<T1, T2, S, T4, R> compose3(
            @Independent @Modified Function1<? super S, ? extends T3> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction4<T1, T2, T3, S, R> compose4(
            @Independent @Modified Function1<? super S, ? extends T4> before) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, R> CheckedFunction4<T1, T2, T3, T4, R> constant(@Independent @NotModified R value) {
            return null;
        }

        @Independent @NotModified
        Function1<T1, Function1<T2, Function1<T3, CheckedFunction1<T4, R>>>> curried() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, T2, T3, T4, R> Function4<T1, T2, T3, T4, Option<R>> lift(
            @Independent @Modified CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R>
                partialFunction) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, R> Function4<T1, T2, T3, T4, Try<R>> liftTry(
            @Independent @Modified CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R>
                partialFunction) { return null; }
        @Fluent @Independent @NotModified CheckedFunction4<T1, T2, T3, T4, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, R> CheckedFunction4<T1, T2, T3, T4, R> narrow(
            @Independent @NotModified CheckedFunction4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
            return null;
        }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, R> CheckedFunction4<T1, T2, T3, T4, R> of(
            @Independent @NotModified CheckedFunction4<T1, T2, T3, T4, R> methodReference) { return null; }

        @Independent @NotModified
        Function4<T1, T2, T3, T4, R> recover(
            @Independent @Modified Function<
                ? super Throwable,
                ? extends Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R>> recover) {
            return null;
        }
        @Independent @NotModified CheckedFunction4<T4, T3, T2, T1, R> reversed() { return null; }
        @Independent @NotModified CheckedFunction1<Tuple4<T1, T2, T3, T4>, R> tupled() { return null; }
        @Independent @NotModified Function4<T1, T2, T3, T4, R> unchecked() { return null; }
    }

    //public interface CheckedFunction5 implements Serializable
    @FinalFields
    @Independent
    class CheckedFunction5$<T1, T2, T3, T4, T5, R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> CheckedFunction5<T1, T2, T3, T4, T5, V> andThen(
            @Independent @Modified CheckedFunction1<? super R, ? extends V> after) { return null; }

        @Independent @NotModified
        CheckedFunction4<T2, T3, T4, T5, R> apply(@Independent @Modified T1 t1) { return null; }

        @Independent @NotModified
        CheckedFunction3<T3, T4, T5, R> apply(@Independent @Modified T1 t1, @Independent @Modified T2 t2) { return null; }

        @Independent @NotModified
        CheckedFunction2<T4, T5, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3) { return null; }

        @Independent @NotModified
        CheckedFunction1<T5, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4) { return null; }

        @Independent(absent = true) @Modified
        R apply(
            @Independent(absent = true) @NotModified T1 arg0,
            @Independent(absent = true) @NotModified T2 arg1,
            @Independent(absent = true) @NotModified T3 arg2,
            @Independent(absent = true) @NotModified T4 arg3,
            @Independent(absent = true) @NotModified T5 arg4) { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <S> CheckedFunction5<S, T2, T3, T4, T5, R> compose1(
            @Independent @Modified Function1<? super S, ? extends T1> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction5<T1, S, T3, T4, T5, R> compose2(
            @Independent @Modified Function1<? super S, ? extends T2> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction5<T1, T2, S, T4, T5, R> compose3(
            @Independent @Modified Function1<? super S, ? extends T3> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction5<T1, T2, T3, S, T5, R> compose4(
            @Independent @Modified Function1<? super S, ? extends T4> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction5<T1, T2, T3, T4, S, R> compose5(
            @Independent @Modified Function1<? super S, ? extends T5> before) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, R> CheckedFunction5<T1, T2, T3, T4, T5, R> constant(
            @Independent @NotModified R value) { return null; }

        @Independent @NotModified
        Function1<T1, Function1<T2, Function1<T3, Function1<T4, CheckedFunction1<T5, R>>>>> curried() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, T2, T3, T4, T5, R> Function5<T1, T2, T3, T4, T5, Option<R>> lift(
            @Independent @Modified CheckedFunction5<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, R> Function5<T1, T2, T3, T4, T5, Try<R>> liftTry(
            @Independent @Modified CheckedFunction5<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? extends R> partialFunction) { return null; }
        @Fluent @Independent @NotModified CheckedFunction5<T1, T2, T3, T4, T5, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, R> CheckedFunction5<T1, T2, T3, T4, T5, R> narrow(
            @Independent @NotModified CheckedFunction5<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? extends R> f) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, R> CheckedFunction5<T1, T2, T3, T4, T5, R> of(
            @Independent @NotModified CheckedFunction5<T1, T2, T3, T4, T5, R> methodReference) { return null; }

        @Independent @NotModified
        Function5<T1, T2, T3, T4, T5, R> recover(
            @Independent @Modified Function<
                ? super Throwable,
                ? extends Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R>> recover) {
            return null;
        }
        @Independent @NotModified CheckedFunction5<T5, T4, T3, T2, T1, R> reversed() { return null; }
        @Independent @NotModified CheckedFunction1<Tuple5<T1, T2, T3, T4, T5>, R> tupled() { return null; }
        @Independent @NotModified Function5<T1, T2, T3, T4, T5, R> unchecked() { return null; }
    }

    //public interface CheckedFunction6 implements Serializable
    @FinalFields
    @Independent
    class CheckedFunction6$<T1, T2, T3, T4, T5, T6, R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> CheckedFunction6<T1, T2, T3, T4, T5, T6, V> andThen(
            @Independent @Modified CheckedFunction1<? super R, ? extends V> after) { return null; }

        @Independent @NotModified
        CheckedFunction5<T2, T3, T4, T5, T6, R> apply(@Independent @Modified T1 t1) { return null; }

        @Independent @NotModified
        CheckedFunction4<T3, T4, T5, T6, R> apply(@Independent @Modified T1 t1, @Independent @Modified T2 t2) {
            return null;
        }

        @Independent @NotModified
        CheckedFunction3<T4, T5, T6, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3) { return null; }

        @Independent @NotModified
        CheckedFunction2<T5, T6, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4) { return null; }

        @Independent @NotModified
        CheckedFunction1<T6, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4,
            @Independent @Modified T5 t5) { return null; }

        @Independent(absent = true) @Modified
        R apply(
            @Independent(absent = true) @NotModified T1 arg0,
            @Independent(absent = true) @NotModified T2 arg1,
            @Independent(absent = true) @NotModified T3 arg2,
            @Independent(absent = true) @NotModified T4 arg3,
            @Independent(absent = true) @NotModified T5 arg4,
            @Independent(absent = true) @NotModified T6 arg5) { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <S> CheckedFunction6<S, T2, T3, T4, T5, T6, R> compose1(
            @Independent @Modified Function1<? super S, ? extends T1> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction6<T1, S, T3, T4, T5, T6, R> compose2(
            @Independent @Modified Function1<? super S, ? extends T2> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction6<T1, T2, S, T4, T5, T6, R> compose3(
            @Independent @Modified Function1<? super S, ? extends T3> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction6<T1, T2, T3, S, T5, T6, R> compose4(
            @Independent @Modified Function1<? super S, ? extends T4> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction6<T1, T2, T3, T4, S, T6, R> compose5(
            @Independent @Modified Function1<? super S, ? extends T5> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction6<T1, T2, T3, T4, T5, S, R> compose6(
            @Independent @Modified Function1<? super S, ? extends T6> before) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, R> CheckedFunction6<T1, T2, T3, T4, T5, T6, R> constant(
            @Independent @NotModified R value) { return null; }

        @Independent @NotModified
        Function1<T1, Function1<T2, Function1<T3, Function1<T4, Function1<T5, CheckedFunction1<T6, R>>>>>> curried() {
            return null;
        }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, R> Function6<T1, T2, T3, T4, T5, T6, Option<R>> lift(
            @Independent @Modified CheckedFunction6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, R> Function6<T1, T2, T3, T4, T5, T6, Try<R>> liftTry(
            @Independent @Modified CheckedFunction6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? extends R> partialFunction) { return null; }
        @Fluent @Independent @NotModified CheckedFunction6<T1, T2, T3, T4, T5, T6, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, R> CheckedFunction6<T1, T2, T3, T4, T5, T6, R> narrow(
            @Independent @NotModified CheckedFunction6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? extends R> f) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, R> CheckedFunction6<T1, T2, T3, T4, T5, T6, R> of(
            @Independent @NotModified CheckedFunction6<T1, T2, T3, T4, T5, T6, R> methodReference) { return null; }

        @Independent @NotModified
        Function6<T1, T2, T3, T4, T5, T6, R> recover(
            @Independent @Modified Function<
                ? super Throwable,
                ? extends Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R>>
                recover) { return null; }
        @Independent @NotModified CheckedFunction6<T6, T5, T4, T3, T2, T1, R> reversed() { return null; }
        @Independent @NotModified CheckedFunction1<Tuple6<T1, T2, T3, T4, T5, T6>, R> tupled() { return null; }
        @Independent @NotModified Function6<T1, T2, T3, T4, T5, T6, R> unchecked() { return null; }
    }

    //public interface CheckedFunction7 implements Serializable
    @FinalFields
    @Independent
    class CheckedFunction7$<T1, T2, T3, T4, T5, T6, T7, R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> CheckedFunction7<T1, T2, T3, T4, T5, T6, T7, V> andThen(
            @Independent @Modified CheckedFunction1<? super R, ? extends V> after) { return null; }

        @Independent @NotModified
        CheckedFunction6<T2, T3, T4, T5, T6, T7, R> apply(@Independent @Modified T1 t1) { return null; }

        @Independent @NotModified
        CheckedFunction5<T3, T4, T5, T6, T7, R> apply(@Independent @Modified T1 t1, @Independent @Modified T2 t2) {
            return null;
        }

        @Independent @NotModified
        CheckedFunction4<T4, T5, T6, T7, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3) { return null; }

        @Independent @NotModified
        CheckedFunction3<T5, T6, T7, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4) { return null; }

        @Independent @NotModified
        CheckedFunction2<T6, T7, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4,
            @Independent @Modified T5 t5) { return null; }

        @Independent @NotModified
        CheckedFunction1<T7, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4,
            @Independent @Modified T5 t5,
            @Independent @Modified T6 t6) { return null; }

        @Independent(absent = true) @Modified
        R apply(
            @Independent(absent = true) @NotModified T1 arg0,
            @Independent(absent = true) @NotModified T2 arg1,
            @Independent(absent = true) @NotModified T3 arg2,
            @Independent(absent = true) @NotModified T4 arg3,
            @Independent(absent = true) @NotModified T5 arg4,
            @Independent(absent = true) @NotModified T6 arg5,
            @Independent(absent = true) @NotModified T7 arg6) { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <S> CheckedFunction7<S, T2, T3, T4, T5, T6, T7, R> compose1(
            @Independent @Modified Function1<? super S, ? extends T1> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction7<T1, S, T3, T4, T5, T6, T7, R> compose2(
            @Independent @Modified Function1<? super S, ? extends T2> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction7<T1, T2, S, T4, T5, T6, T7, R> compose3(
            @Independent @Modified Function1<? super S, ? extends T3> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction7<T1, T2, T3, S, T5, T6, T7, R> compose4(
            @Independent @Modified Function1<? super S, ? extends T4> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction7<T1, T2, T3, T4, S, T6, T7, R> compose5(
            @Independent @Modified Function1<? super S, ? extends T5> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction7<T1, T2, T3, T4, T5, S, T7, R> compose6(
            @Independent @Modified Function1<? super S, ? extends T6> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction7<T1, T2, T3, T4, T5, T6, S, R> compose7(
            @Independent @Modified Function1<? super S, ? extends T7> before) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, R> CheckedFunction7<T1, T2, T3, T4, T5, T6, T7, R> constant(
            @Independent @NotModified R value) { return null; }

        @Independent @NotModified
        Function1<
            T1,
            Function1<T2, Function1<T3, Function1<T4, Function1<T5, Function1<T6, CheckedFunction1<T7, R>>>>>>> curried() {
            return null;
        }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, R> Function7<T1, T2, T3, T4, T5, T6, T7, Option<R>> lift(
            @Independent @Modified CheckedFunction7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, R> Function7<T1, T2, T3, T4, T5, T6, T7, Try<R>> liftTry(
            @Independent @Modified CheckedFunction7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? extends R> partialFunction) { return null; }
        @Fluent @Independent @NotModified CheckedFunction7<T1, T2, T3, T4, T5, T6, T7, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, R> CheckedFunction7<T1, T2, T3, T4, T5, T6, T7, R> narrow(
            @Independent @NotModified CheckedFunction7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? extends R> f) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, R> CheckedFunction7<T1, T2, T3, T4, T5, T6, T7, R> of(
            @Independent @NotModified CheckedFunction7<T1, T2, T3, T4, T5, T6, T7, R> methodReference) { return null; }

        @Independent @NotModified
        Function7<T1, T2, T3, T4, T5, T6, T7, R> recover(
            @Independent @Modified Function<
                ? super Throwable,
                ? extends Function7<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? extends R>> recover) { return null; }
        @Independent @NotModified CheckedFunction7<T7, T6, T5, T4, T3, T2, T1, R> reversed() { return null; }
        @Independent @NotModified CheckedFunction1<Tuple7<T1, T2, T3, T4, T5, T6, T7>, R> tupled() { return null; }
        @Independent @NotModified Function7<T1, T2, T3, T4, T5, T6, T7, R> unchecked() { return null; }
    }

    //public interface CheckedFunction8 implements Serializable
    @FinalFields
    @Independent
    class CheckedFunction8$<T1, T2, T3, T4, T5, T6, T7, T8, R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, V> andThen(
            @Independent @Modified CheckedFunction1<? super R, ? extends V> after) { return null; }

        @Independent @NotModified
        CheckedFunction7<T2, T3, T4, T5, T6, T7, T8, R> apply(@Independent @Modified T1 t1) { return null; }

        @Independent @NotModified
        CheckedFunction6<T3, T4, T5, T6, T7, T8, R> apply(@Independent @Modified T1 t1, @Independent @Modified T2 t2) {
            return null;
        }

        @Independent @NotModified
        CheckedFunction5<T4, T5, T6, T7, T8, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3) { return null; }

        @Independent @NotModified
        CheckedFunction4<T5, T6, T7, T8, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4) { return null; }

        @Independent @NotModified
        CheckedFunction3<T6, T7, T8, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4,
            @Independent @Modified T5 t5) { return null; }

        @Independent @NotModified
        CheckedFunction2<T7, T8, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4,
            @Independent @Modified T5 t5,
            @Independent @Modified T6 t6) { return null; }

        @Independent @NotModified
        CheckedFunction1<T8, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4,
            @Independent @Modified T5 t5,
            @Independent @Modified T6 t6,
            @Independent @Modified T7 t7) { return null; }

        @Independent(absent = true) @Modified
        R apply(
            @Independent(absent = true) @NotModified T1 arg0,
            @Independent(absent = true) @NotModified T2 arg1,
            @Independent(absent = true) @NotModified T3 arg2,
            @Independent(absent = true) @NotModified T4 arg3,
            @Independent(absent = true) @NotModified T5 arg4,
            @Independent(absent = true) @NotModified T6 arg5,
            @Independent(absent = true) @NotModified T7 arg6,
            @Independent(absent = true) @NotModified T8 arg7) { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <S> CheckedFunction8<S, T2, T3, T4, T5, T6, T7, T8, R> compose1(
            @Independent @Modified Function1<? super S, ? extends T1> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction8<T1, S, T3, T4, T5, T6, T7, T8, R> compose2(
            @Independent @Modified Function1<? super S, ? extends T2> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction8<T1, T2, S, T4, T5, T6, T7, T8, R> compose3(
            @Independent @Modified Function1<? super S, ? extends T3> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction8<T1, T2, T3, S, T5, T6, T7, T8, R> compose4(
            @Independent @Modified Function1<? super S, ? extends T4> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction8<T1, T2, T3, T4, S, T6, T7, T8, R> compose5(
            @Independent @Modified Function1<? super S, ? extends T5> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction8<T1, T2, T3, T4, T5, S, T7, T8, R> compose6(
            @Independent @Modified Function1<? super S, ? extends T6> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction8<T1, T2, T3, T4, T5, T6, S, T8, R> compose7(
            @Independent @Modified Function1<? super S, ? extends T7> before) { return null; }

        @Independent @NotModified
        <S> CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, S, R> compose8(
            @Independent @Modified Function1<? super S, ? extends T8> before) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8, R> CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> constant(
            @Independent @NotModified R value) { return null; }

        @Independent @NotModified
        Function1<
            T1,
            Function1<
                T2,
                Function1<T3, Function1<T4, Function1<T5, Function1<T6, Function1<T7, CheckedFunction1<T8, R>>>>>>>>
            curried() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function8<T1, T2, T3, T4, T5, T6, T7, T8, Option<R>> lift(
            @Independent @Modified CheckedFunction8<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? super T8,
                ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function8<T1, T2, T3, T4, T5, T6, T7, T8, Try<R>> liftTry(
            @Independent @Modified CheckedFunction8<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? super T8,
                ? extends R> partialFunction) { return null; }

        @Fluent @Independent @NotModified
        CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> memoized() { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8, R> CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> narrow(
            @Independent @NotModified CheckedFunction8<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? super T8,
                ? extends R> f) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8, R> CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> of(
            @Independent @NotModified CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> methodReference) {
            return null;
        }

        @Independent @NotModified
        Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> recover(
            @Independent @Modified Function<
                ? super Throwable,
                ? extends Function8<
                    ? super T1,
                    ? super T2,
                    ? super T3,
                    ? super T4,
                    ? super T5,
                    ? super T6,
                    ? super T7,
                    ? super T8,
                    ? extends R>> recover) { return null; }
        @Independent @NotModified CheckedFunction8<T8, T7, T6, T5, T4, T3, T2, T1, R> reversed() { return null; }
        @Independent @NotModified CheckedFunction1<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>, R> tupled() { return null; }
        @Independent @NotModified Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> unchecked() { return null; }
    }

    //public interface CheckedPredicate
    @FinalFields
    @Container
    @Independent
    class CheckedPredicate$<T> {
        @Independent @NotModified CheckedPredicate<T> negate() { return null; }
        @Identity @Independent @NotModified
        static <T> CheckedPredicate<T> of(@Independent @NotModified CheckedPredicate<T> methodReference) { return null; }
        @Modified boolean test(@Independent(absent = true) @NotModified T arg0) { return false; }
        @Independent @NotModified Predicate<T> unchecked() { return null; }
    }

    //public interface CheckedRunnable
    @FinalFields
    @Container
    @Independent
    class CheckedRunnable$ {
        @Identity @Independent @NotModified
        static CheckedRunnable of(@Independent @NotModified CheckedRunnable methodReference) { return null; }
        @Modified void run() { }
        @Independent @NotModified Runnable unchecked() { return null; }
    }

    //public interface Function0 implements Serializable, Supplier<R>
    @FinalFields
    @Independent
    class Function0$<R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> Function0<V> andThen(@Independent @Modified Function<? super R, ? extends V> after) { return null; }
        @Independent(absent = true) @Modified R apply() { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified static <R> Function0<R> constant(@Independent @NotModified R value) { return null; }
        @Fluent @Independent @NotModified Function0<R> curried() { return null; }
        //override from java.util.function.Supplier
        @Independent @Modified
        R get() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <R> Function0<Option<R>> lift(@Independent @Modified Supplier<? extends R> partialFunction) {
            return null;
        }

        @Independent @NotModified
        static <R> Function0<Try<R>> liftTry(@Independent @Modified Supplier<? extends R> partialFunction) {
            return null;
        }
        @Fluent @Independent @Modified Function0<R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <R> Function0<R> narrow(@Independent @NotModified Function0<? extends R> f) { return null; }

        @Identity @Independent @NotModified
        static <R> Function0<R> of(@Independent @NotModified Function0<R> methodReference) { return null; }
        @Fluent @Independent @NotModified Function0<R> reversed() { return null; }
        @Independent @NotModified Function1<Tuple0, R> tupled() { return null; }
    }

    //public interface Function1 implements Serializable, Function<T1,R>
    @FinalFields
    @Independent(absent = true)
    class Function1$<T1, R> {
        static final long serialVersionUID = 0L;
        //override from java.util.function.Function
        @Independent @NotModified
        <V> Function1<T1, V> andThen(@Independent @Modified Function<? super R, ? extends V> after) { return null; }

        //override from java.util.function.Function
        @Independent(absent = true) @Modified
        R apply(@Independent @Modified T1 arg0) { return null; }
        @NotModified int arity() { return 0; }
        //override from java.util.function.Function
        @Independent @NotModified
        <V> Function1<V, R> compose(@Independent @Modified Function<? super V, ? extends T1> before) { return null; }

        @Independent @NotModified
        <S> Function1<S, R> compose1(@Independent @Modified Function1<? super S, ? extends T1> before) { return null; }

        @Independent @NotModified
        static <T1, R> Function1<T1, R> constant(@Independent @NotModified R value) { return null; }
        @Fluent @Independent @NotModified Function1<T1, R> curried() { return null; }
        @Independent @NotModified static <T> Function1<T, T> identity() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, R> Function1<T1, Option<R>> lift(
            @Independent @Modified Function<? super T1, ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static <T1, R> Function1<T1, Try<R>> liftTry(
            @Independent @Modified Function<? super T1, ? extends R> partialFunction) { return null; }
        @Fluent @Independent @NotModified Function1<T1, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, R> Function1<T1, R> narrow(@Independent @NotModified Function1<? super T1, ? extends R> f) {
            return null;
        }

        @Identity @Independent @NotModified
        static <T1, R> Function1<T1, R> of(@Independent @NotModified Function1<T1, R> methodReference) { return null; }

        @Independent @Modified
        PartialFunction<T1, R> partial(@Independent @Modified Predicate<? super T1> isDefinedAt) { return null; }
        @Fluent @Independent @NotModified Function1<T1, R> reversed() { return null; }
        @Independent @NotModified Function1<Tuple1<T1>, R> tupled() { return null; }
    }

    //public interface Function2 implements Serializable, BiFunction<T1,T2,R>
    @FinalFields
    @Independent
    class Function2$<T1, T2, R> {
        static final long serialVersionUID = 0L;
        //override from java.util.function.BiFunction
        @Independent @NotModified
        <V> Function2<T1, T2, V> andThen(@Independent @Modified Function<? super R, ? extends V> after) { return null; }
        @Independent @NotModified Function1<T2, R> apply(@Independent @Modified T1 t1) { return null; }
        //override from java.util.function.BiFunction
        @Independent(absent = true) @Modified

        R apply(@Independent(absent = true) @NotModified T1 arg0, @Independent(absent = true) @NotModified T2 arg1) {
            return null;
        }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <S> Function2<S, T2, R> compose1(@Independent @Modified Function1<? super S, ? extends T1> before) {
            return null;
        }

        @Independent @NotModified
        <S> Function2<T1, S, R> compose2(@Independent @Modified Function1<? super S, ? extends T2> before) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, R> Function2<T1, T2, R> constant(@Independent @NotModified R value) { return null; }
        @Independent @NotModified Function1<T1, Function1<T2, R>> curried() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, T2, R> Function2<T1, T2, Option<R>> lift(
            @Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static <T1, T2, R> Function2<T1, T2, Try<R>> liftTry(
            @Independent @Modified BiFunction<? super T1, ? super T2, ? extends R> partialFunction) { return null; }
        @Fluent @Independent @NotModified Function2<T1, T2, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, T2, R> Function2<T1, T2, R> narrow(
            @Independent @NotModified Function2<? super T1, ? super T2, ? extends R> f) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, R> Function2<T1, T2, R> of(@Independent @NotModified Function2<T1, T2, R> methodReference) {
            return null;
        }
        @Independent @NotModified Function2<T2, T1, R> reversed() { return null; }
        @Independent @NotModified Function1<Tuple2<T1, T2>, R> tupled() { return null; }
    }

    //public interface Function3 implements Serializable
    @FinalFields
    @Independent
    class Function3$<T1, T2, T3, R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> Function3<T1, T2, T3, V> andThen(@Independent @Modified Function<? super R, ? extends V> after) {
            return null;
        }
        @Independent @NotModified Function2<T2, T3, R> apply(@Independent @Modified T1 t1) { return null; }
        @Independent @NotModified
        Function1<T3, R> apply(@Independent @Modified T1 t1, @Independent @Modified T2 t2) { return null; }

        @Independent(absent = true) @Modified
        R apply(
            @Independent(absent = true) @NotModified T1 arg0,
            @Independent(absent = true) @NotModified T2 arg1,
            @Independent(absent = true) @NotModified T3 arg2) { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <S> Function3<S, T2, T3, R> compose1(@Independent @Modified Function1<? super S, ? extends T1> before) {
            return null;
        }

        @Independent @NotModified
        <S> Function3<T1, S, T3, R> compose2(@Independent @Modified Function1<? super S, ? extends T2> before) {
            return null;
        }

        @Independent @NotModified
        <S> Function3<T1, T2, S, R> compose3(@Independent @Modified Function1<? super S, ? extends T3> before) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, R> Function3<T1, T2, T3, R> constant(@Independent @NotModified R value) { return null; }
        @Independent @NotModified Function1<T1, Function1<T2, Function1<T3, R>>> curried() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, T2, T3, R> Function3<T1, T2, T3, Option<R>> lift(
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> partialFunction) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, R> Function3<T1, T2, T3, Try<R>> liftTry(
            @Independent @Modified Function3<? super T1, ? super T2, ? super T3, ? extends R> partialFunction) {
            return null;
        }
        @Fluent @Independent @NotModified Function3<T1, T2, T3, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, T2, T3, R> Function3<T1, T2, T3, R> narrow(
            @Independent @NotModified Function3<? super T1, ? super T2, ? super T3, ? extends R> f) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, T3, R> Function3<T1, T2, T3, R> of(
            @Independent @NotModified Function3<T1, T2, T3, R> methodReference) { return null; }
        @Independent @NotModified Function3<T3, T2, T1, R> reversed() { return null; }
        @Independent @NotModified Function1<Tuple3<T1, T2, T3>, R> tupled() { return null; }
    }

    //public interface Function4 implements Serializable
    @FinalFields
    @Independent
    class Function4$<T1, T2, T3, T4, R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> Function4<T1, T2, T3, T4, V> andThen(@Independent @Modified Function<? super R, ? extends V> after) {
            return null;
        }
        @Independent @NotModified Function3<T2, T3, T4, R> apply(@Independent @Modified T1 t1) { return null; }
        @Independent @NotModified
        Function2<T3, T4, R> apply(@Independent @Modified T1 t1, @Independent @Modified T2 t2) { return null; }

        @Independent @NotModified
        Function1<T4, R> apply(@Independent @Modified T1 t1, @Independent @Modified T2 t2, @Independent @Modified T3 t3) {
            return null;
        }

        @Independent(absent = true) @Modified
        R apply(
            @Independent(absent = true) @NotModified T1 arg0,
            @Independent(absent = true) @NotModified T2 arg1,
            @Independent(absent = true) @NotModified T3 arg2,
            @Independent(absent = true) @NotModified T4 arg3) { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <S> Function4<S, T2, T3, T4, R> compose1(@Independent @Modified Function1<? super S, ? extends T1> before) {
            return null;
        }

        @Independent @NotModified
        <S> Function4<T1, S, T3, T4, R> compose2(@Independent @Modified Function1<? super S, ? extends T2> before) {
            return null;
        }

        @Independent @NotModified
        <S> Function4<T1, T2, S, T4, R> compose3(@Independent @Modified Function1<? super S, ? extends T3> before) {
            return null;
        }

        @Independent @NotModified
        <S> Function4<T1, T2, T3, S, R> compose4(@Independent @Modified Function1<? super S, ? extends T4> before) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, T4, R> Function4<T1, T2, T3, T4, R> constant(@Independent @NotModified R value) {
            return null;
        }

        @Independent @NotModified
        Function1<T1, Function1<T2, Function1<T3, Function1<T4, R>>>> curried() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, T2, T3, T4, R> Function4<T1, T2, T3, T4, Option<R>> lift(
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R>
                partialFunction) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, R> Function4<T1, T2, T3, T4, Try<R>> liftTry(
            @Independent @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R>
                partialFunction) { return null; }
        @Fluent @Independent @NotModified Function4<T1, T2, T3, T4, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, R> Function4<T1, T2, T3, T4, R> narrow(
            @Independent @NotModified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
            return null;
        }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, R> Function4<T1, T2, T3, T4, R> of(
            @Independent @NotModified Function4<T1, T2, T3, T4, R> methodReference) { return null; }
        @Independent @NotModified Function4<T4, T3, T2, T1, R> reversed() { return null; }
        @Independent @NotModified Function1<Tuple4<T1, T2, T3, T4>, R> tupled() { return null; }
    }

    //public interface Function5 implements Serializable
    @FinalFields
    @Independent
    class Function5$<T1, T2, T3, T4, T5, R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> Function5<T1, T2, T3, T4, T5, V> andThen(@Independent @Modified Function<? super R, ? extends V> after) {
            return null;
        }
        @Independent @NotModified Function4<T2, T3, T4, T5, R> apply(@Independent @Modified T1 t1) { return null; }
        @Independent @NotModified
        Function3<T3, T4, T5, R> apply(@Independent @Modified T1 t1, @Independent @Modified T2 t2) { return null; }

        @Independent @NotModified
        Function2<T4, T5, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3) { return null; }

        @Independent @NotModified
        Function1<T5, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4) { return null; }

        @Independent(absent = true) @Modified
        R apply(
            @Independent(absent = true) @NotModified T1 arg0,
            @Independent(absent = true) @NotModified T2 arg1,
            @Independent(absent = true) @NotModified T3 arg2,
            @Independent(absent = true) @NotModified T4 arg3,
            @Independent(absent = true) @NotModified T5 arg4) { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <S> Function5<S, T2, T3, T4, T5, R> compose1(@Independent @Modified Function1<? super S, ? extends T1> before) {
            return null;
        }

        @Independent @NotModified
        <S> Function5<T1, S, T3, T4, T5, R> compose2(@Independent @Modified Function1<? super S, ? extends T2> before) {
            return null;
        }

        @Independent @NotModified
        <S> Function5<T1, T2, S, T4, T5, R> compose3(@Independent @Modified Function1<? super S, ? extends T3> before) {
            return null;
        }

        @Independent @NotModified
        <S> Function5<T1, T2, T3, S, T5, R> compose4(@Independent @Modified Function1<? super S, ? extends T4> before) {
            return null;
        }

        @Independent @NotModified
        <S> Function5<T1, T2, T3, T4, S, R> compose5(@Independent @Modified Function1<? super S, ? extends T5> before) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, R> Function5<T1, T2, T3, T4, T5, R> constant(@Independent @NotModified R value) {
            return null;
        }

        @Independent @NotModified
        Function1<T1, Function1<T2, Function1<T3, Function1<T4, Function1<T5, R>>>>> curried() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, T2, T3, T4, T5, R> Function5<T1, T2, T3, T4, T5, Option<R>> lift(
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R>
                partialFunction) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, R> Function5<T1, T2, T3, T4, T5, Try<R>> liftTry(
            @Independent @Modified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R>
                partialFunction) { return null; }
        @Fluent @Independent @NotModified Function5<T1, T2, T3, T4, T5, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, R> Function5<T1, T2, T3, T4, T5, R> narrow(
            @Independent @NotModified Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R>
                f) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, R> Function5<T1, T2, T3, T4, T5, R> of(
            @Independent @NotModified Function5<T1, T2, T3, T4, T5, R> methodReference) { return null; }
        @Independent @NotModified Function5<T5, T4, T3, T2, T1, R> reversed() { return null; }
        @Independent @NotModified Function1<Tuple5<T1, T2, T3, T4, T5>, R> tupled() { return null; }
    }

    //public interface Function6 implements Serializable
    @FinalFields
    @Independent
    class Function6$<T1, T2, T3, T4, T5, T6, R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> Function6<T1, T2, T3, T4, T5, T6, V> andThen(@Independent @Modified Function<? super R, ? extends V> after) {
            return null;
        }
        @Independent @NotModified Function5<T2, T3, T4, T5, T6, R> apply(@Independent @Modified T1 t1) { return null; }
        @Independent @NotModified
        Function4<T3, T4, T5, T6, R> apply(@Independent @Modified T1 t1, @Independent @Modified T2 t2) { return null; }

        @Independent @NotModified
        Function3<T4, T5, T6, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3) { return null; }

        @Independent @NotModified
        Function2<T5, T6, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4) { return null; }

        @Independent @NotModified
        Function1<T6, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4,
            @Independent @Modified T5 t5) { return null; }

        @Independent(absent = true) @Modified
        R apply(
            @Independent(absent = true) @NotModified T1 arg0,
            @Independent(absent = true) @NotModified T2 arg1,
            @Independent(absent = true) @NotModified T3 arg2,
            @Independent(absent = true) @NotModified T4 arg3,
            @Independent(absent = true) @NotModified T5 arg4,
            @Independent(absent = true) @NotModified T6 arg5) { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <S> Function6<S, T2, T3, T4, T5, T6, R> compose1(
            @Independent @Modified Function1<? super S, ? extends T1> before) { return null; }

        @Independent @NotModified
        <S> Function6<T1, S, T3, T4, T5, T6, R> compose2(
            @Independent @Modified Function1<? super S, ? extends T2> before) { return null; }

        @Independent @NotModified
        <S> Function6<T1, T2, S, T4, T5, T6, R> compose3(
            @Independent @Modified Function1<? super S, ? extends T3> before) { return null; }

        @Independent @NotModified
        <S> Function6<T1, T2, T3, S, T5, T6, R> compose4(
            @Independent @Modified Function1<? super S, ? extends T4> before) { return null; }

        @Independent @NotModified
        <S> Function6<T1, T2, T3, T4, S, T6, R> compose5(
            @Independent @Modified Function1<? super S, ? extends T5> before) { return null; }

        @Independent @NotModified
        <S> Function6<T1, T2, T3, T4, T5, S, R> compose6(
            @Independent @Modified Function1<? super S, ? extends T6> before) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, R> Function6<T1, T2, T3, T4, T5, T6, R> constant(
            @Independent @NotModified R value) { return null; }

        @Independent @NotModified
        Function1<T1, Function1<T2, Function1<T3, Function1<T4, Function1<T5, Function1<T6, R>>>>>> curried() {
            return null;
        }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, R> Function6<T1, T2, T3, T4, T5, T6, Option<R>> lift(
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, R> Function6<T1, T2, T3, T4, T5, T6, Try<R>> liftTry(
            @Independent @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? extends R> partialFunction) { return null; }
        @Fluent @Independent @NotModified Function6<T1, T2, T3, T4, T5, T6, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, R> Function6<T1, T2, T3, T4, T5, T6, R> narrow(
            @Independent @NotModified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? extends R> f) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, R> Function6<T1, T2, T3, T4, T5, T6, R> of(
            @Independent @NotModified Function6<T1, T2, T3, T4, T5, T6, R> methodReference) { return null; }
        @Independent @NotModified Function6<T6, T5, T4, T3, T2, T1, R> reversed() { return null; }
        @Independent @NotModified Function1<Tuple6<T1, T2, T3, T4, T5, T6>, R> tupled() { return null; }
    }

    //public interface Function7 implements Serializable
    @FinalFields
    @Independent
    class Function7$<T1, T2, T3, T4, T5, T6, T7, R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> Function7<T1, T2, T3, T4, T5, T6, T7, V> andThen(
            @Independent @Modified Function<? super R, ? extends V> after) { return null; }

        @Independent @NotModified
        Function6<T2, T3, T4, T5, T6, T7, R> apply(@Independent @Modified T1 t1) { return null; }

        @Independent @NotModified
        Function5<T3, T4, T5, T6, T7, R> apply(@Independent @Modified T1 t1, @Independent @Modified T2 t2) { return null; }

        @Independent @NotModified
        Function4<T4, T5, T6, T7, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3) { return null; }

        @Independent @NotModified
        Function3<T5, T6, T7, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4) { return null; }

        @Independent @NotModified
        Function2<T6, T7, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4,
            @Independent @Modified T5 t5) { return null; }

        @Independent @NotModified
        Function1<T7, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4,
            @Independent @Modified T5 t5,
            @Independent @Modified T6 t6) { return null; }

        @Independent(absent = true) @Modified
        R apply(
            @Independent(absent = true) @NotModified T1 arg0,
            @Independent(absent = true) @NotModified T2 arg1,
            @Independent(absent = true) @NotModified T3 arg2,
            @Independent(absent = true) @NotModified T4 arg3,
            @Independent(absent = true) @NotModified T5 arg4,
            @Independent(absent = true) @NotModified T6 arg5,
            @Independent(absent = true) @NotModified T7 arg6) { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <S> Function7<S, T2, T3, T4, T5, T6, T7, R> compose1(
            @Independent @Modified Function1<? super S, ? extends T1> before) { return null; }

        @Independent @NotModified
        <S> Function7<T1, S, T3, T4, T5, T6, T7, R> compose2(
            @Independent @Modified Function1<? super S, ? extends T2> before) { return null; }

        @Independent @NotModified
        <S> Function7<T1, T2, S, T4, T5, T6, T7, R> compose3(
            @Independent @Modified Function1<? super S, ? extends T3> before) { return null; }

        @Independent @NotModified
        <S> Function7<T1, T2, T3, S, T5, T6, T7, R> compose4(
            @Independent @Modified Function1<? super S, ? extends T4> before) { return null; }

        @Independent @NotModified
        <S> Function7<T1, T2, T3, T4, S, T6, T7, R> compose5(
            @Independent @Modified Function1<? super S, ? extends T5> before) { return null; }

        @Independent @NotModified
        <S> Function7<T1, T2, T3, T4, T5, S, T7, R> compose6(
            @Independent @Modified Function1<? super S, ? extends T6> before) { return null; }

        @Independent @NotModified
        <S> Function7<T1, T2, T3, T4, T5, T6, S, R> compose7(
            @Independent @Modified Function1<? super S, ? extends T7> before) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, R> Function7<T1, T2, T3, T4, T5, T6, T7, R> constant(
            @Independent @NotModified R value) { return null; }

        @Independent @NotModified
        Function1<T1, Function1<T2, Function1<T3, Function1<T4, Function1<T5, Function1<T6, Function1<T7, R>>>>>>>
            curried() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, R> Function7<T1, T2, T3, T4, T5, T6, T7, Option<R>> lift(
            @Independent @Modified Function7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, R> Function7<T1, T2, T3, T4, T5, T6, T7, Try<R>> liftTry(
            @Independent @Modified Function7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? extends R> partialFunction) { return null; }
        @Fluent @Independent @NotModified Function7<T1, T2, T3, T4, T5, T6, T7, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, R> Function7<T1, T2, T3, T4, T5, T6, T7, R> narrow(
            @Independent @NotModified Function7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? extends R> f) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, R> Function7<T1, T2, T3, T4, T5, T6, T7, R> of(
            @Independent @NotModified Function7<T1, T2, T3, T4, T5, T6, T7, R> methodReference) { return null; }
        @Independent @NotModified Function7<T7, T6, T5, T4, T3, T2, T1, R> reversed() { return null; }
        @Independent @NotModified Function1<Tuple7<T1, T2, T3, T4, T5, T6, T7>, R> tupled() { return null; }
    }

    //public interface Function8 implements Serializable
    @FinalFields
    @Independent
    class Function8$<T1, T2, T3, T4, T5, T6, T7, T8, R> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <V> Function8<T1, T2, T3, T4, T5, T6, T7, T8, V> andThen(
            @Independent @Modified Function<? super R, ? extends V> after) { return null; }

        @Independent @NotModified
        Function7<T2, T3, T4, T5, T6, T7, T8, R> apply(@Independent @Modified T1 t1) { return null; }

        @Independent @NotModified
        Function6<T3, T4, T5, T6, T7, T8, R> apply(@Independent @Modified T1 t1, @Independent @Modified T2 t2) {
            return null;
        }

        @Independent @NotModified
        Function5<T4, T5, T6, T7, T8, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3) { return null; }

        @Independent @NotModified
        Function4<T5, T6, T7, T8, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4) { return null; }

        @Independent @NotModified
        Function3<T6, T7, T8, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4,
            @Independent @Modified T5 t5) { return null; }

        @Independent @NotModified
        Function2<T7, T8, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4,
            @Independent @Modified T5 t5,
            @Independent @Modified T6 t6) { return null; }

        @Independent @NotModified
        Function1<T8, R> apply(
            @Independent @Modified T1 t1,
            @Independent @Modified T2 t2,
            @Independent @Modified T3 t3,
            @Independent @Modified T4 t4,
            @Independent @Modified T5 t5,
            @Independent @Modified T6 t6,
            @Independent @Modified T7 t7) { return null; }

        @Independent(absent = true) @Modified
        R apply(
            @Independent(absent = true) @NotModified T1 arg0,
            @Independent(absent = true) @NotModified T2 arg1,
            @Independent(absent = true) @NotModified T3 arg2,
            @Independent(absent = true) @NotModified T4 arg3,
            @Independent(absent = true) @NotModified T5 arg4,
            @Independent(absent = true) @NotModified T6 arg5,
            @Independent(absent = true) @NotModified T7 arg6,
            @Independent(absent = true) @NotModified T8 arg7) { return null; }
        @NotModified int arity() { return 0; }
        @Independent @NotModified
        <S> Function8<S, T2, T3, T4, T5, T6, T7, T8, R> compose1(
            @Independent @Modified Function1<? super S, ? extends T1> before) { return null; }

        @Independent @NotModified
        <S> Function8<T1, S, T3, T4, T5, T6, T7, T8, R> compose2(
            @Independent @Modified Function1<? super S, ? extends T2> before) { return null; }

        @Independent @NotModified
        <S> Function8<T1, T2, S, T4, T5, T6, T7, T8, R> compose3(
            @Independent @Modified Function1<? super S, ? extends T3> before) { return null; }

        @Independent @NotModified
        <S> Function8<T1, T2, T3, S, T5, T6, T7, T8, R> compose4(
            @Independent @Modified Function1<? super S, ? extends T4> before) { return null; }

        @Independent @NotModified
        <S> Function8<T1, T2, T3, T4, S, T6, T7, T8, R> compose5(
            @Independent @Modified Function1<? super S, ? extends T5> before) { return null; }

        @Independent @NotModified
        <S> Function8<T1, T2, T3, T4, T5, S, T7, T8, R> compose6(
            @Independent @Modified Function1<? super S, ? extends T6> before) { return null; }

        @Independent @NotModified
        <S> Function8<T1, T2, T3, T4, T5, T6, S, T8, R> compose7(
            @Independent @Modified Function1<? super S, ? extends T7> before) { return null; }

        @Independent @NotModified
        <S> Function8<T1, T2, T3, T4, T5, T6, T7, S, R> compose8(
            @Independent @Modified Function1<? super S, ? extends T8> before) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> constant(
            @Independent @NotModified R value) { return null; }

        @Independent @NotModified
        Function1<
            T1,
            Function1<T2, Function1<T3, Function1<T4, Function1<T5, Function1<T6, Function1<T7, Function1<T8, R>>>>>>>>
            curried() { return null; }
        @NotModified boolean isMemoized() { return false; }
        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function8<T1, T2, T3, T4, T5, T6, T7, T8, Option<R>> lift(
            @Independent @Modified Function8<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? super T8,
                ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function8<T1, T2, T3, T4, T5, T6, T7, T8, Try<R>> liftTry(
            @Independent @Modified Function8<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? super T8,
                ? extends R> partialFunction) { return null; }
        @Fluent @Independent @NotModified Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> memoized() { return null; }
        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> narrow(
            @Independent @NotModified Function8<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? super T8,
                ? extends R> f) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> of(
            @Independent @NotModified Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> methodReference) { return null; }
        @Independent @NotModified Function8<T8, T7, T6, T5, T4, T3, T2, T1, R> reversed() { return null; }
        @Independent @NotModified Function1<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>, R> tupled() { return null; }
    }

    //public final class Lazy implements Value<T>, Supplier<T>, Serializable
    //EXPECTED eventually @Immutable(hc = true), after evaluation -- computed mutable @Independent(hc = true) -- G3: memoizes its supplier once (VAVR.md)
    @Independent(hc = true)
    class Lazy$<T> {
        //override from io.vavr.Value, java.lang.Object
        @Modified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        @Independent(hc = true) @Modified
        Option<T> filter(@Independent @Modified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value, java.util.function.Supplier
        @Independent(hc = true) @Modified
        T get() { return null; }

        //override from io.vavr.Value, java.lang.Object
        @Modified
        public int hashCode() { return 0; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isEmpty() { return false; }
        @NotModified boolean isEvaluated() { return false; }
        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isSingleValued() { return false; }

        //override from io.vavr.Value, java.lang.Iterable
        @Independent @Modified
        Iterator<T> iterator() { return null; }

        //override from io.vavr.Value
        @Independent @Modified
        <U> Lazy<U> map(@Independent @Modified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.Value
        @Independent @Modified
        <U> Lazy<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value
        @Independent @Modified
        Lazy<Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <T> Lazy<T> narrow(@Independent @NotModified Lazy<? extends T> lazy) { return null; }

        @Independent @NotModified
        static <T> Lazy<T> of(@Independent @Modified Supplier<? extends T> supplier) { return null; }

        //override from io.vavr.Value
        @Fluent @Independent @Modified
        Lazy<T> peek(@Independent @Modified Consumer<? super T> action) { return null; }

        @Independent @NotModified
        static <T> Lazy<Seq<T>> sequence(@Independent @Modified Iterable<? extends Lazy<? extends T>> values) {
            return null;
        }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent @Modified
        <U> U transform(@Independent @Modified Function<? super Lazy<T>, ? extends U> f) { return null; }

        @Independent @NotModified
        static <T> T val(@Independent @Modified Supplier<? extends T> supplier, Class<T> type) { return null; }
    }

    //public class MatchError extends NoSuchElementException
    //EXPECTED @FinalFields -- computed @FinalFields @Container @Dependent -- G4 (computed is right): holds a Throwable, which is mutable (VAVR.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class MatchError$ {@Independent(hc = true) @NotModified @GetSet("obj") Object getObject() { return null; } }

    //public class NotImplementedError extends Error
    //EXPECTED @FinalFields -- computed @FinalFields @Container @Dependent -- G4 (computed is right): holds a Throwable, which is mutable (VAVR.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class NotImplementedError$ {NotImplementedError$() { }NotImplementedError$(String message) { } }

    //public interface PartialFunction implements Function1<T,R>
    @FinalFields
    @Independent(absent = true)
    class PartialFunction$<T, R> {
        static final long serialVersionUID = 0L;
        //override from io.vavr.Function1, java.util.function.Function
        @Independent(absent = true) @Modified
        R apply(@Independent @Modified T arg0) { return null; }
        @Independent @NotModified static <T, V extends Value<T>> PartialFunction<V, T> getIfDefined() { return null; }
        @NotModified boolean isDefinedAt(@Independent @Modified T arg0) { return false; }
        @Independent @NotModified Function1<T, Option<R>> lift() { return null; }
        @Independent @NotModified
        static <T, R> PartialFunction<T, R> unlift(
            @Independent @Modified Function<? super T, ? extends Option<? extends R>> totalFunction) { return null; }
    }

    //public final class Patterns
    //EXPECTED @UtilityClass / @Immutable -- computed @FinalFields @Independent -- G5: static extractors only (VAVR.md)
    @FinalFields
    @Independent
    class Patterns$ {
        @Independent @NotModified static final API.Match.Pattern0<Tuple0> $Tuple0 = null;
        @Independent @NotModified
        static <T, _1 extends T, _2 extends List<T>> API.Match.Pattern2<List.Cons<T>, _1, _2> $Cons(
            @Independent @Modified API.Match.Pattern<_1, ?> p1,
            @Independent @NotModified API.Match.Pattern<_2, ?> p2) { return null; }

        @Independent @NotModified
        static <T, _1 extends Throwable> API.Match.Pattern1<Try.Failure<T>, _1> $Failure(
            @Independent @Modified API.Match.Pattern<_1, ?> p1) { return null; }

        @Independent @NotModified
        static <T, _1 extends Option<Try<T>>> API.Match.Pattern1<Future<T>, _1> $Future(
            @Independent @Modified API.Match.Pattern<_1, ?> p1) { return null; }

        @Independent @NotModified
        static <E, T, _1 extends E> API.Match.Pattern1<Validation.Invalid<E, T>, _1> $Invalid(
            @Independent @Modified API.Match.Pattern<_1, ?> p1) { return null; }

        @Independent @NotModified
        static <L, R, _1 extends L> API.Match.Pattern1<Either.Left<L, R>, _1> $Left(
            @Independent @Modified API.Match.Pattern<_1, ?> p1) { return null; }
        @Independent @NotModified static <T> API.Match.Pattern0<List.Nil<T>> $Nil() { return null; }
        @Independent @NotModified static <T> API.Match.Pattern0<Option.None<T>> $None() { return null; }
        @Independent @NotModified
        static <L, R, _1 extends R> API.Match.Pattern1<Either.Right<L, R>, _1> $Right(
            @Independent @Modified API.Match.Pattern<_1, ?> p1) { return null; }

        @Independent @NotModified
        static <T, _1 extends T> API.Match.Pattern1<Option.Some<T>, _1> $Some(
            @Independent @Modified API.Match.Pattern<_1, ?> p1) { return null; }

        @Independent @NotModified
        static <T, _1 extends T> API.Match.Pattern1<Try.Success<T>, _1> $Success(
            @Independent @Modified API.Match.Pattern<_1, ?> p1) { return null; }

        @Independent @NotModified
        static <T1, _1 extends T1> API.Match.Pattern1<Tuple1<T1>, _1> $Tuple1(
            @Independent @Modified API.Match.Pattern<_1, ?> p1) { return null; }

        @Independent @NotModified
        static <T1, T2, _1 extends T1, _2 extends T2> API.Match.Pattern2<Tuple2<T1, T2>, _1, _2> $Tuple2(
            @Independent @Modified API.Match.Pattern<_1, ?> p1,
            @Independent @NotModified API.Match.Pattern<_2, ?> p2) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, _1 extends T1, _2 extends T2, _3 extends T3> API.Match.Pattern3<
            Tuple3<T1, T2, T3>,
            _1,
            _2,
            _3> $Tuple3(
            @Independent @Modified API.Match.Pattern<_1, ?> p1,
            @Independent @NotModified API.Match.Pattern<_2, ?> p2,
            @Independent @NotModified API.Match.Pattern<_3, ?> p3) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, _1 extends T1, _2 extends T2, _3 extends T3, _4 extends T4> API.Match.Pattern4<
            Tuple4<T1, T2, T3, T4>,
            _1,
            _2,
            _3,
            _4> $Tuple4(
            @Independent @Modified API.Match.Pattern<_1, ?> p1,
            @Independent @NotModified API.Match.Pattern<_2, ?> p2,
            @Independent @NotModified API.Match.Pattern<_3, ?> p3,
            @Independent @NotModified API.Match.Pattern<_4, ?> p4) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, _1 extends T1, _2 extends T2, _3 extends T3, _4 extends T4, _5 extends T5>
            API.Match.Pattern5<Tuple5<T1, T2, T3, T4, T5>, _1, _2, _3, _4, _5> $Tuple5(
            @Independent @Modified API.Match.Pattern<_1, ?> p1,
            @Independent @NotModified API.Match.Pattern<_2, ?> p2,
            @Independent @NotModified API.Match.Pattern<_3, ?> p3,
            @Independent @NotModified API.Match.Pattern<_4, ?> p4,
            @Independent @NotModified API.Match.Pattern<_5, ?> p5) { return null; }

        @Independent @NotModified
        static <
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            _1 extends T1,
            _2 extends T2,
            _3 extends T3,
            _4 extends T4,
            _5 extends T5,
            _6 extends T6> API.Match.Pattern6<Tuple6<T1, T2, T3, T4, T5, T6>, _1, _2, _3, _4, _5, _6> $Tuple6(
            @Independent @Modified API.Match.Pattern<_1, ?> p1,
            @Independent @NotModified API.Match.Pattern<_2, ?> p2,
            @Independent @NotModified API.Match.Pattern<_3, ?> p3,
            @Independent @NotModified API.Match.Pattern<_4, ?> p4,
            @Independent @NotModified API.Match.Pattern<_5, ?> p5,
            @Independent @NotModified API.Match.Pattern<_6, ?> p6) { return null; }

        @Independent @NotModified
        static <
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            _1 extends T1,
            _2 extends T2,
            _3 extends T3,
            _4 extends T4,
            _5 extends T5,
            _6 extends T6,
            _7 extends T7> API.Match.Pattern7<Tuple7<T1, T2, T3, T4, T5, T6, T7>, _1, _2, _3, _4, _5, _6, _7> $Tuple7(
            @Independent @Modified API.Match.Pattern<_1, ?> p1,
            @Independent @NotModified API.Match.Pattern<_2, ?> p2,
            @Independent @NotModified API.Match.Pattern<_3, ?> p3,
            @Independent @NotModified API.Match.Pattern<_4, ?> p4,
            @Independent @NotModified API.Match.Pattern<_5, ?> p5,
            @Independent @NotModified API.Match.Pattern<_6, ?> p6,
            @Independent @NotModified API.Match.Pattern<_7, ?> p7) { return null; }

        @Independent @NotModified
        static <
            T1,
            T2,
            T3,
            T4,
            T5,
            T6,
            T7,
            T8,
            _1 extends T1,
            _2 extends T2,
            _3 extends T3,
            _4 extends T4,
            _5 extends T5,
            _6 extends T6,
            _7 extends T7,
            _8 extends T8> API.Match.Pattern8<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>, _1, _2, _3, _4, _5, _6, _7, _8>
            $Tuple8(
            @Independent @Modified API.Match.Pattern<_1, ?> p1,
            @Independent @NotModified API.Match.Pattern<_2, ?> p2,
            @Independent @NotModified API.Match.Pattern<_3, ?> p3,
            @Independent @NotModified API.Match.Pattern<_4, ?> p4,
            @Independent @NotModified API.Match.Pattern<_5, ?> p5,
            @Independent @NotModified API.Match.Pattern<_6, ?> p6,
            @Independent @NotModified API.Match.Pattern<_7, ?> p7,
            @Independent @NotModified API.Match.Pattern<_8, ?> p8) { return null; }

        @Independent @NotModified
        static <E, T, _1 extends T> API.Match.Pattern1<Validation.Valid<E, T>, _1> $Valid(
            @Independent @Modified API.Match.Pattern<_1, ?> p1) { return null; }
    }

    //public final class Predicates
    @Immutable
    class Predicates$ {
        @Independent @NotModified
        static <T> Predicate<T> allOf(@Independent @NotModified Predicate<T> ... predicates) { return null; }

        @Independent @NotModified
        static <T> Predicate<T> anyOf(@Independent @NotModified Predicate<T> ... predicates) { return null; }

        @Independent @NotModified
        static <T> Predicate<Iterable<T>> exists(@Independent @Modified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        static <T> Predicate<Iterable<T>> forAll(@Independent @NotModified Predicate<? super T> predicate) {
            return null;
        }
        @Independent @NotModified static <T> Predicate<T> instanceOf(Class<? extends T> type) { return null; }
        @Independent @NotModified static <T> Predicate<T> is(@Independent @NotModified T value) { return null; }
        @Independent @NotModified static <T> Predicate<T> isIn(@Independent @NotModified T ... values) { return null; }
        @Independent @NotModified static <T> Predicate<T> isNotNull() { return null; }
        @Independent @NotModified static <T> Predicate<T> isNull() { return null; }
        @Independent @NotModified
        static <T> Predicate<T> noneOf(@Independent @NotModified Predicate<T> ... predicates) { return null; }

        @Independent @NotModified
        static <T> Predicate<T> not(@Independent @NotModified Predicate<? super T> predicate) { return null; }
    }

    //public interface Tuple implements Serializable
    @Immutable(hc = true)
    class Tuple$ {
        static final int MAX_ARITY = 0;
        static final long serialVersionUID = 0L;
        @NotModified int arity() { return 0; }
        @Independent @NotModified static Tuple0 empty() { return null; }
        @Independent @NotModified
        static <T1, T2> Tuple2<T1, T2> fromEntry(@Independent @NotModified Map.Entry<? extends T1, ? extends T2> entry) {
            return null;
        }
        @NotModified static int hash(@Independent @NotModified Object o1) { return 0; }
        @NotModified
        static int hash(@Independent @NotModified Object o1, @Independent @NotModified Object o2) { return 0; }

        @NotModified
        static int hash(
            @Independent @NotModified Object o1,
            @Independent @NotModified Object o2,
            @Independent @NotModified Object o3) { return 0; }

        @NotModified
        static int hash(
            @Independent @NotModified Object o1,
            @Independent @NotModified Object o2,
            @Independent @NotModified Object o3,
            @Independent @NotModified Object o4) { return 0; }

        @NotModified
        static int hash(
            @Independent @NotModified Object o1,
            @Independent @NotModified Object o2,
            @Independent @NotModified Object o3,
            @Independent @NotModified Object o4,
            @Independent @NotModified Object o5) { return 0; }

        @NotModified
        static int hash(
            @Independent @NotModified Object o1,
            @Independent @NotModified Object o2,
            @Independent @NotModified Object o3,
            @Independent @NotModified Object o4,
            @Independent @NotModified Object o5,
            @Independent @NotModified Object o6) { return 0; }

        @NotModified
        static int hash(
            @Independent @NotModified Object o1,
            @Independent @NotModified Object o2,
            @Independent @NotModified Object o3,
            @Independent @NotModified Object o4,
            @Independent @NotModified Object o5,
            @Independent @NotModified Object o6,
            @Independent @NotModified Object o7) { return 0; }

        @NotModified
        static int hash(
            @Independent @NotModified Object o1,
            @Independent @NotModified Object o2,
            @Independent @NotModified Object o3,
            @Independent @NotModified Object o4,
            @Independent @NotModified Object o5,
            @Independent @NotModified Object o6,
            @Independent @NotModified Object o7,
            @Independent @NotModified Object o8) { return 0; }

        @Identity @Independent @NotModified
        static <T1> Tuple1<T1> narrow(@Independent @NotModified Tuple1<? extends T1> t) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2> Tuple2<T1, T2> narrow(@Independent @NotModified Tuple2<? extends T1, ? extends T2> t) {
            return null;
        }

        @Identity @Independent @NotModified
        static <T1, T2, T3> Tuple3<T1, T2, T3> narrow(
            @Independent @NotModified Tuple3<? extends T1, ? extends T2, ? extends T3> t) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> narrow(
            @Independent @NotModified Tuple4<? extends T1, ? extends T2, ? extends T3, ? extends T4> t) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> narrow(
            @Independent @NotModified Tuple5<? extends T1, ? extends T2, ? extends T3, ? extends T4, ? extends T5> t) {
            return null;
        }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> narrow(
            @Independent @NotModified Tuple6<
                ? extends T1,
                ? extends T2,
                ? extends T3,
                ? extends T4,
                ? extends T5,
                ? extends T6> t) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> narrow(
            @Independent @NotModified Tuple7<
                ? extends T1,
                ? extends T2,
                ? extends T3,
                ? extends T4,
                ? extends T5,
                ? extends T6,
                ? extends T7> t) { return null; }

        @Identity @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> narrow(
            @Independent @NotModified Tuple8<
                ? extends T1,
                ? extends T2,
                ? extends T3,
                ? extends T4,
                ? extends T5,
                ? extends T6,
                ? extends T7,
                ? extends T8> t) { return null; }
        @Independent @NotModified static <T1> Tuple1<T1> of(@Independent @NotModified T1 t1) { return null; }
        @Independent @NotModified
        static <T1, T2> Tuple2<T1, T2> of(@Independent @NotModified T1 t1, @Independent @NotModified T2 t2) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3> Tuple3<T1, T2, T3> of(
            @Independent @NotModified T1 t1,
            @Independent @NotModified T2 t2,
            @Independent @NotModified T3 t3) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(
            @Independent @NotModified T1 t1,
            @Independent @NotModified T2 t2,
            @Independent @NotModified T3 t3,
            @Independent @NotModified T4 t4) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> of(
            @Independent @NotModified T1 t1,
            @Independent @NotModified T2 t2,
            @Independent @NotModified T3 t3,
            @Independent @NotModified T4 t4,
            @Independent @NotModified T5 t5) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> of(
            @Independent @NotModified T1 t1,
            @Independent @NotModified T2 t2,
            @Independent @NotModified T3 t3,
            @Independent @NotModified T4 t4,
            @Independent @NotModified T5 t5,
            @Independent @NotModified T6 t6) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> of(
            @Independent @NotModified T1 t1,
            @Independent @NotModified T2 t2,
            @Independent @NotModified T3 t3,
            @Independent @NotModified T4 t4,
            @Independent @NotModified T5 t5,
            @Independent @NotModified T6 t6,
            @Independent @NotModified T7 t7) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> of(
            @Independent @NotModified T1 t1,
            @Independent @NotModified T2 t2,
            @Independent @NotModified T3 t3,
            @Independent @NotModified T4 t4,
            @Independent @NotModified T5 t5,
            @Independent @NotModified T6 t6,
            @Independent @NotModified T7 t7,
            @Independent @NotModified T8 t8) { return null; }

        @Independent @NotModified
        static <T1> Tuple1<Seq<T1>> sequence1(@Independent @Modified Iterable<? extends Tuple1<? extends T1>> tuples) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2> Tuple2<Seq<T1>, Seq<T2>> sequence2(
            @Independent @Modified Iterable<? extends Tuple2<? extends T1, ? extends T2>> tuples) { return null; }

        @Independent @NotModified
        static <T1, T2, T3> Tuple3<Seq<T1>, Seq<T2>, Seq<T3>> sequence3(
            @Independent @Modified Iterable<? extends Tuple3<? extends T1, ? extends T2, ? extends T3>> tuples) {
            return null;
        }

        @Independent @NotModified
        static <T1, T2, T3, T4> Tuple4<Seq<T1>, Seq<T2>, Seq<T3>, Seq<T4>> sequence4(
            @Independent @Modified Iterable<? extends Tuple4<? extends T1, ? extends T2, ? extends T3, ? extends T4>>
                tuples) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5> Tuple5<Seq<T1>, Seq<T2>, Seq<T3>, Seq<T4>, Seq<T5>> sequence5(
            @Independent @Modified Iterable<? extends Tuple5<
                ? extends T1,
                ? extends T2,
                ? extends T3,
                ? extends T4,
                ? extends T5>> tuples) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6> Tuple6<Seq<T1>, Seq<T2>, Seq<T3>, Seq<T4>, Seq<T5>, Seq<T6>> sequence6(
            @Independent @Modified Iterable<? extends Tuple6<
                ? extends T1,
                ? extends T2,
                ? extends T3,
                ? extends T4,
                ? extends T5,
                ? extends T6>> tuples) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7> Tuple7<Seq<T1>, Seq<T2>, Seq<T3>, Seq<T4>, Seq<T5>, Seq<T6>, Seq<T7>>
            sequence7(
            @Independent @Modified Iterable<? extends Tuple7<
                ? extends T1,
                ? extends T2,
                ? extends T3,
                ? extends T4,
                ? extends T5,
                ? extends T6,
                ? extends T7>> tuples) { return null; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<
            Seq<T1>,
            Seq<T2>,
            Seq<T3>,
            Seq<T4>,
            Seq<T5>,
            Seq<T6>,
            Seq<T7>,
            Seq<T8>> sequence8(
            @Independent @Modified Iterable<? extends Tuple8<
                ? extends T1,
                ? extends T2,
                ? extends T3,
                ? extends T4,
                ? extends T5,
                ? extends T6,
                ? extends T7,
                ? extends T8>> tuples) { return null; }
        @Independent(hc = true) @NotModified Seq<?> toSeq() { return null; }
    }

    //public final class Tuple0 implements Tuple, Comparable<Tuple0>, Serializable
    @Immutable(hc = true)
    class Tuple0$ {
        @Independent @NotModified <T1> Tuple1<T1> append(@Independent @NotModified T1 t1) { return null; }
        @Independent @NotModified <U> U apply(@Independent @Modified Supplier<? extends U> f) { return null; }
        //override from io.vavr.Tuple
        @NotModified
        int arity() { return 0; }
        @Independent @NotModified static Comparator<Tuple0> comparator() { return null; }
        //override from java.lang.Comparable
        @NotModified
        int compareTo(@Independent @NotModified Tuple0 that) { return 0; }
        @Independent @NotModified <T1> Tuple1<T1> concat(@Independent @NotModified Tuple1<T1> tuple) { return null; }
        @Independent @NotModified
        <T1, T2> Tuple2<T1, T2> concat(@Independent @NotModified Tuple2<T1, T2> tuple) { return null; }

        @Independent @NotModified
        <T1, T2, T3> Tuple3<T1, T2, T3> concat(@Independent @NotModified Tuple3<T1, T2, T3> tuple) { return null; }

        @Independent @NotModified
        <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> concat(@Independent @NotModified Tuple4<T1, T2, T3, T4> tuple) {
            return null;
        }

        @Independent @NotModified
        <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> concat(
            @Independent @NotModified Tuple5<T1, T2, T3, T4, T5> tuple) { return null; }

        @Independent @NotModified
        <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> concat(
            @Independent @NotModified Tuple6<T1, T2, T3, T4, T5, T6> tuple) { return null; }

        @Independent @NotModified
        <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> concat(
            @Independent @NotModified Tuple7<T1, T2, T3, T4, T5, T6, T7> tuple) { return null; }

        @Independent @NotModified
        <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(
            @Independent @NotModified Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> tuple) { return null; }

        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }
        @Independent @NotModified static Tuple0 instance() { return null; }
        //override from io.vavr.Tuple
        @Independent @NotModified
        Seq<?> toSeq() { return null; }

        //override from java.lang.Object
        @NotModified
        public String toString() { return null; }
    }

    //public final class Tuple1 implements Tuple, Comparable<Tuple1<T1>>, Serializable
    @Immutable(hc = true)
    @Independent
    class Tuple1$<T1> {
        @Independent @NotModified final T1 _1 = null;
        Tuple1$(@Independent(hc = true) @NotModified T1 t1) { }
        @Independent(hc = true) @NotModified @GetSet("_1") T1 _1() { return null; }
        @Independent(hc = true) @NotModified
        <T2> Tuple2<T1, T2> append(@Independent @NotModified T2 t2) { return null; }

        @Independent @NotModified
        <U> U apply(@Independent @Modified Function<? super T1, ? extends U> f) { return null; }

        //override from io.vavr.Tuple
        @NotModified
        int arity() { return 0; }

        @Independent @NotModified
        static <T1> Comparator<Tuple1<T1>> comparator(@Independent @NotModified Comparator<? super T1> t1Comp) {
            return null;
        }

        //override from java.lang.Comparable
        @NotModified
        int compareTo(@Independent @NotModified Tuple1<T1> that) { return 0; }

        @Independent(hc = true) @NotModified
        <T2> Tuple2<T1, T2> concat(@Independent @NotModified Tuple1<T2> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T2, T3> Tuple3<T1, T2, T3> concat(@Independent @NotModified Tuple2<T2, T3> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T2, T3, T4> Tuple4<T1, T2, T3, T4> concat(@Independent @NotModified Tuple3<T2, T3, T4> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> concat(@Independent @NotModified Tuple4<T2, T3, T4, T5> tuple) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> concat(
            @Independent @NotModified Tuple5<T2, T3, T4, T5, T6> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> concat(
            @Independent @NotModified Tuple6<T2, T3, T4, T5, T6, T7> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(
            @Independent @NotModified Tuple7<T2, T3, T4, T5, T6, T7, T8> tuple) { return null; }

        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        @Independent @NotModified
        <U1> Tuple1<U1> map(@Independent @Modified Function<? super T1, ? extends U1> mapper) { return null; }

        //override from io.vavr.Tuple
        @Independent(hc = true) @NotModified
        Seq<?> toSeq() { return null; }

        //override from java.lang.Object
        @NotModified
        public String toString() { return null; }
        @Independent @NotModified Tuple1<T1> update1(@Independent @NotModified T1 value) { return null; }
    }

    //public final class Tuple2 implements Tuple, Comparable<Tuple2<T1,T2>>, Serializable
    @Immutable(hc = true)
    @Independent
    class Tuple2$<T1, T2> {
        @Independent @NotModified final T1 _1 = null;
        @Independent @NotModified final T2 _2 = null;
        Tuple2$(@Independent(hc = true) @NotModified T1 t1, @Independent(hc = true) @NotModified T2 t2) { }
        @Independent(hc = true) @NotModified @GetSet("_1") T1 _1() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_2") T2 _2() { return null; }
        @Independent(hc = true) @NotModified
        <T3> Tuple3<T1, T2, T3> append(@Independent @NotModified T3 t3) { return null; }

        @Independent @NotModified
        <U> U apply(@Independent @Modified BiFunction<? super T1, ? super T2, ? extends U> f) { return null; }

        //override from io.vavr.Tuple
        @NotModified
        int arity() { return 0; }

        @Independent @NotModified
        static <T1, T2> Comparator<Tuple2<T1, T2>> comparator(
            @Independent @NotModified Comparator<? super T1> t1Comp,
            @Independent @NotModified Comparator<? super T2> t2Comp) { return null; }

        //override from java.lang.Comparable
        @NotModified
        int compareTo(@Independent @NotModified Tuple2<T1, T2> that) { return 0; }

        @Independent(hc = true) @NotModified
        <T3> Tuple3<T1, T2, T3> concat(@Independent @NotModified Tuple1<T3> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T3, T4> Tuple4<T1, T2, T3, T4> concat(@Independent @NotModified Tuple2<T3, T4> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> concat(@Independent @NotModified Tuple3<T3, T4, T5> tuple) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> concat(@Independent @NotModified Tuple4<T3, T4, T5, T6> tuple) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> concat(
            @Independent @NotModified Tuple5<T3, T4, T5, T6, T7> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(
            @Independent @NotModified Tuple6<T3, T4, T5, T6, T7, T8> tuple) { return null; }

        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        @Independent @NotModified
        <U1, U2> Tuple2<U1, U2> map(@Independent @Modified BiFunction<? super T1, ? super T2, Tuple2<U1, U2>> mapper) {
            return null;
        }

        @Independent @NotModified
        <U1, U2> Tuple2<U1, U2> map(
            @Independent @Modified Function<? super T1, ? extends U1> f1,
            @Independent @Modified Function<? super T2, ? extends U2> f2) { return null; }

        @Independent(hc = true) @NotModified
        <U> Tuple2<U, T2> map1(@Independent @Modified Function<? super T1, ? extends U> mapper) { return null; }

        @Independent(hc = true) @NotModified
        <U> Tuple2<T1, U> map2(@Independent @Modified Function<? super T2, ? extends U> mapper) { return null; }
        @Independent(hc = true) @NotModified Tuple2<T2, T1> swap() { return null; }
        @Independent(hc = true) @NotModified Map.Entry<T1, T2> toEntry() { return null; }
        //override from io.vavr.Tuple
        @Independent(hc = true) @NotModified
        Seq<?> toSeq() { return null; }

        //override from java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent(hc = true) @NotModified
        Tuple2<T1, T2> update1(@Independent @NotModified T1 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple2<T1, T2> update2(@Independent @NotModified T2 value) { return null; }
    }

    //public final class Tuple3 implements Tuple, Comparable<Tuple3<T1,T2,T3>>, Serializable
    @Immutable(hc = true)
    @Independent
    class Tuple3$<T1, T2, T3> {
        @Independent @NotModified final T1 _1 = null;
        @Independent @NotModified final T2 _2 = null;
        @Independent @NotModified final T3 _3 = null;
        Tuple3$(
            @Independent(hc = true) @NotModified T1 t1,
            @Independent(hc = true) @NotModified T2 t2,
            @Independent(hc = true) @NotModified T3 t3) { }
        @Independent(hc = true) @NotModified @GetSet("_1") T1 _1() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_2") T2 _2() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_3") T3 _3() { return null; }
        @Independent(hc = true) @NotModified
        <T4> Tuple4<T1, T2, T3, T4> append(@Independent @NotModified T4 t4) { return null; }

        @Independent @NotModified
        <U> U apply(@Independent(hc = true) @Modified Function3<? super T1, ? super T2, ? super T3, ? extends U> f) {
            return null;
        }

        //override from io.vavr.Tuple
        @NotModified
        int arity() { return 0; }

        @Independent @NotModified
        static <T1, T2, T3> Comparator<Tuple3<T1, T2, T3>> comparator(
            @Independent @NotModified Comparator<? super T1> t1Comp,
            @Independent @NotModified Comparator<? super T2> t2Comp,
            @Independent @NotModified Comparator<? super T3> t3Comp) { return null; }

        //override from java.lang.Comparable
        @NotModified
        int compareTo(@Independent @NotModified Tuple3<T1, T2, T3> that) { return 0; }

        @Independent(hc = true) @NotModified
        <T4> Tuple4<T1, T2, T3, T4> concat(@Independent @NotModified Tuple1<T4> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T4, T5> Tuple5<T1, T2, T3, T4, T5> concat(@Independent @NotModified Tuple2<T4, T5> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> concat(@Independent @NotModified Tuple3<T4, T5, T6> tuple) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> concat(
            @Independent @NotModified Tuple4<T4, T5, T6, T7> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(
            @Independent @NotModified Tuple5<T4, T5, T6, T7, T8> tuple) { return null; }

        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        @Independent @NotModified
        <U1, U2, U3> Tuple3<U1, U2, U3> map(
            @Independent(hc = true) @Modified Function3<? super T1, ? super T2, ? super T3, Tuple3<U1, U2, U3>> mapper) {
            return null;
        }

        @Independent @NotModified
        <U1, U2, U3> Tuple3<U1, U2, U3> map(
            @Independent @Modified Function<? super T1, ? extends U1> f1,
            @Independent @Modified Function<? super T2, ? extends U2> f2,
            @Independent @Modified Function<? super T3, ? extends U3> f3) { return null; }

        @Independent(hc = true) @NotModified
        <U> Tuple3<U, T2, T3> map1(@Independent @Modified Function<? super T1, ? extends U> mapper) { return null; }

        @Independent(hc = true) @NotModified
        <U> Tuple3<T1, U, T3> map2(@Independent @Modified Function<? super T2, ? extends U> mapper) { return null; }

        @Independent(hc = true) @NotModified
        <U> Tuple3<T1, T2, U> map3(@Independent @Modified Function<? super T3, ? extends U> mapper) { return null; }

        //override from io.vavr.Tuple
        @Independent(hc = true) @NotModified
        Seq<?> toSeq() { return null; }

        //override from java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent(hc = true) @NotModified
        Tuple3<T1, T2, T3> update1(@Independent @NotModified T1 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple3<T1, T2, T3> update2(@Independent @NotModified T2 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple3<T1, T2, T3> update3(@Independent @NotModified T3 value) { return null; }
    }

    //public final class Tuple4 implements Tuple, Comparable<Tuple4<T1,T2,T3,T4>>, Serializable
    @Immutable(hc = true)
    @Independent
    class Tuple4$<T1, T2, T3, T4> {
        @Independent @NotModified final T1 _1 = null;
        @Independent @NotModified final T2 _2 = null;
        @Independent @NotModified final T3 _3 = null;
        @Independent @NotModified final T4 _4 = null;
        Tuple4$(
            @Independent(hc = true) @NotModified T1 t1,
            @Independent(hc = true) @NotModified T2 t2,
            @Independent(hc = true) @NotModified T3 t3,
            @Independent(hc = true) @NotModified T4 t4) { }
        @Independent(hc = true) @NotModified @GetSet("_1") T1 _1() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_2") T2 _2() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_3") T3 _3() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_4") T4 _4() { return null; }
        @Independent(hc = true) @NotModified
        <T5> Tuple5<T1, T2, T3, T4, T5> append(@Independent @NotModified T5 t5) { return null; }

        @Independent @NotModified
        <U> U apply(
            @Independent(hc = true) @Modified Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends U> f) {
            return null;
        }

        //override from io.vavr.Tuple
        @NotModified
        int arity() { return 0; }

        @Independent @NotModified
        static <T1, T2, T3, T4> Comparator<Tuple4<T1, T2, T3, T4>> comparator(
            @Independent @NotModified Comparator<? super T1> t1Comp,
            @Independent @NotModified Comparator<? super T2> t2Comp,
            @Independent @NotModified Comparator<? super T3> t3Comp,
            @Independent @NotModified Comparator<? super T4> t4Comp) { return null; }

        //override from java.lang.Comparable
        @NotModified
        int compareTo(@Independent @NotModified Tuple4<T1, T2, T3, T4> that) { return 0; }

        @Independent(hc = true) @NotModified
        <T5> Tuple5<T1, T2, T3, T4, T5> concat(@Independent @NotModified Tuple1<T5> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> concat(@Independent @NotModified Tuple2<T5, T6> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> concat(@Independent @NotModified Tuple3<T5, T6, T7> tuple) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(
            @Independent @NotModified Tuple4<T5, T6, T7, T8> tuple) { return null; }

        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        @Independent @NotModified
        <U1, U2, U3, U4> Tuple4<U1, U2, U3, U4> map(
            @Independent(hc = true) @Modified Function4<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                Tuple4<U1, U2, U3, U4>> mapper) { return null; }

        @Independent @NotModified
        <U1, U2, U3, U4> Tuple4<U1, U2, U3, U4> map(
            @Independent @Modified Function<? super T1, ? extends U1> f1,
            @Independent @Modified Function<? super T2, ? extends U2> f2,
            @Independent @Modified Function<? super T3, ? extends U3> f3,
            @Independent @Modified Function<? super T4, ? extends U4> f4) { return null; }

        @Independent(hc = true) @NotModified
        <U> Tuple4<U, T2, T3, T4> map1(@Independent @Modified Function<? super T1, ? extends U> mapper) { return null; }

        @Independent(hc = true) @NotModified
        <U> Tuple4<T1, U, T3, T4> map2(@Independent @Modified Function<? super T2, ? extends U> mapper) { return null; }

        @Independent(hc = true) @NotModified
        <U> Tuple4<T1, T2, U, T4> map3(@Independent @Modified Function<? super T3, ? extends U> mapper) { return null; }

        @Independent(hc = true) @NotModified
        <U> Tuple4<T1, T2, T3, U> map4(@Independent @Modified Function<? super T4, ? extends U> mapper) { return null; }

        //override from io.vavr.Tuple
        @Independent(hc = true) @NotModified
        Seq<?> toSeq() { return null; }

        //override from java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent(hc = true) @NotModified
        Tuple4<T1, T2, T3, T4> update1(@Independent @NotModified T1 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple4<T1, T2, T3, T4> update2(@Independent @NotModified T2 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple4<T1, T2, T3, T4> update3(@Independent @NotModified T3 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple4<T1, T2, T3, T4> update4(@Independent @NotModified T4 value) { return null; }
    }

    //public final class Tuple5 implements Tuple, Comparable<Tuple5<T1,T2,T3,T4,T5>>, Serializable
    @Immutable(hc = true)
    @Independent
    class Tuple5$<T1, T2, T3, T4, T5> {
        @Independent @NotModified final T1 _1 = null;
        @Independent @NotModified final T2 _2 = null;
        @Independent @NotModified final T3 _3 = null;
        @Independent @NotModified final T4 _4 = null;
        @Independent @NotModified final T5 _5 = null;
        Tuple5$(
            @Independent(hc = true) @NotModified T1 t1,
            @Independent(hc = true) @NotModified T2 t2,
            @Independent(hc = true) @NotModified T3 t3,
            @Independent(hc = true) @NotModified T4 t4,
            @Independent(hc = true) @NotModified T5 t5) { }
        @Independent(hc = true) @NotModified @GetSet("_1") T1 _1() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_2") T2 _2() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_3") T3 _3() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_4") T4 _4() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_5") T5 _5() { return null; }
        @Independent(hc = true) @NotModified
        <T6> Tuple6<T1, T2, T3, T4, T5, T6> append(@Independent @NotModified T6 t6) { return null; }

        @Independent @NotModified
        <U> U apply(
            @Independent(hc = true) @Modified Function5<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? extends U> f) { return null; }

        //override from io.vavr.Tuple
        @NotModified
        int arity() { return 0; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5> Comparator<Tuple5<T1, T2, T3, T4, T5>> comparator(
            @Independent @NotModified Comparator<? super T1> t1Comp,
            @Independent @NotModified Comparator<? super T2> t2Comp,
            @Independent @NotModified Comparator<? super T3> t3Comp,
            @Independent @NotModified Comparator<? super T4> t4Comp,
            @Independent @NotModified Comparator<? super T5> t5Comp) { return null; }

        //override from java.lang.Comparable
        @NotModified
        int compareTo(@Independent @NotModified Tuple5<T1, T2, T3, T4, T5> that) { return 0; }

        @Independent(hc = true) @NotModified
        <T6> Tuple6<T1, T2, T3, T4, T5, T6> concat(@Independent @NotModified Tuple1<T6> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> concat(@Independent @NotModified Tuple2<T6, T7> tuple) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(@Independent @NotModified Tuple3<T6, T7, T8> tuple) {
            return null;
        }

        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        @Independent @NotModified
        <U1, U2, U3, U4, U5> Tuple5<U1, U2, U3, U4, U5> map(
            @Independent(hc = true) @Modified Function5<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                Tuple5<U1, U2, U3, U4, U5>> mapper) { return null; }

        @Independent @NotModified
        <U1, U2, U3, U4, U5> Tuple5<U1, U2, U3, U4, U5> map(
            @Independent @Modified Function<? super T1, ? extends U1> f1,
            @Independent @Modified Function<? super T2, ? extends U2> f2,
            @Independent @Modified Function<? super T3, ? extends U3> f3,
            @Independent @Modified Function<? super T4, ? extends U4> f4,
            @Independent @Modified Function<? super T5, ? extends U5> f5) { return null; }

        @Independent(hc = true) @NotModified
        <U> Tuple5<U, T2, T3, T4, T5> map1(@Independent @Modified Function<? super T1, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple5<T1, U, T3, T4, T5> map2(@Independent @Modified Function<? super T2, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple5<T1, T2, U, T4, T5> map3(@Independent @Modified Function<? super T3, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple5<T1, T2, T3, U, T5> map4(@Independent @Modified Function<? super T4, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple5<T1, T2, T3, T4, U> map5(@Independent @Modified Function<? super T5, ? extends U> mapper) {
            return null;
        }

        //override from io.vavr.Tuple
        @Independent(hc = true) @NotModified
        Seq<?> toSeq() { return null; }

        //override from java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent(hc = true) @NotModified
        Tuple5<T1, T2, T3, T4, T5> update1(@Independent @NotModified T1 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple5<T1, T2, T3, T4, T5> update2(@Independent @NotModified T2 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple5<T1, T2, T3, T4, T5> update3(@Independent @NotModified T3 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple5<T1, T2, T3, T4, T5> update4(@Independent @NotModified T4 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple5<T1, T2, T3, T4, T5> update5(@Independent @NotModified T5 value) { return null; }
    }

    //public final class Tuple6 implements Tuple, Comparable<Tuple6<T1,T2,T3,T4,T5,T6>>, Serializable
    @Immutable(hc = true)
    @Independent
    class Tuple6$<T1, T2, T3, T4, T5, T6> {
        @Independent @NotModified final T1 _1 = null;
        @Independent @NotModified final T2 _2 = null;
        @Independent @NotModified final T3 _3 = null;
        @Independent @NotModified final T4 _4 = null;
        @Independent @NotModified final T5 _5 = null;
        @Independent @NotModified final T6 _6 = null;
        Tuple6$(
            @Independent(hc = true) @NotModified T1 t1,
            @Independent(hc = true) @NotModified T2 t2,
            @Independent(hc = true) @NotModified T3 t3,
            @Independent(hc = true) @NotModified T4 t4,
            @Independent(hc = true) @NotModified T5 t5,
            @Independent(hc = true) @NotModified T6 t6) { }
        @Independent(hc = true) @NotModified @GetSet("_1") T1 _1() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_2") T2 _2() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_3") T3 _3() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_4") T4 _4() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_5") T5 _5() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_6") T6 _6() { return null; }
        @Independent(hc = true) @NotModified
        <T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> append(@Independent @NotModified T7 t7) { return null; }

        @Independent @NotModified
        <U> U apply(
            @Independent(hc = true) @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? extends U> f) { return null; }

        //override from io.vavr.Tuple
        @NotModified
        int arity() { return 0; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6> Comparator<Tuple6<T1, T2, T3, T4, T5, T6>> comparator(
            @Independent @NotModified Comparator<? super T1> t1Comp,
            @Independent @NotModified Comparator<? super T2> t2Comp,
            @Independent @NotModified Comparator<? super T3> t3Comp,
            @Independent @NotModified Comparator<? super T4> t4Comp,
            @Independent @NotModified Comparator<? super T5> t5Comp,
            @Independent @NotModified Comparator<? super T6> t6Comp) { return null; }

        //override from java.lang.Comparable
        @NotModified
        int compareTo(@Independent @NotModified Tuple6<T1, T2, T3, T4, T5, T6> that) { return 0; }

        @Independent(hc = true) @NotModified
        <T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> concat(@Independent @NotModified Tuple1<T7> tuple) { return null; }

        @Independent(hc = true) @NotModified
        <T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(@Independent @NotModified Tuple2<T7, T8> tuple) {
            return null;
        }

        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        @Independent @NotModified
        <U1, U2, U3, U4, U5, U6> Tuple6<U1, U2, U3, U4, U5, U6> map(
            @Independent(hc = true) @Modified Function6<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                Tuple6<U1, U2, U3, U4, U5, U6>> mapper) { return null; }

        @Independent @NotModified
        <U1, U2, U3, U4, U5, U6> Tuple6<U1, U2, U3, U4, U5, U6> map(
            @Independent @Modified Function<? super T1, ? extends U1> f1,
            @Independent @Modified Function<? super T2, ? extends U2> f2,
            @Independent @Modified Function<? super T3, ? extends U3> f3,
            @Independent @Modified Function<? super T4, ? extends U4> f4,
            @Independent @Modified Function<? super T5, ? extends U5> f5,
            @Independent @Modified Function<? super T6, ? extends U6> f6) { return null; }

        @Independent(hc = true) @NotModified
        <U> Tuple6<U, T2, T3, T4, T5, T6> map1(@Independent @Modified Function<? super T1, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple6<T1, U, T3, T4, T5, T6> map2(@Independent @Modified Function<? super T2, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple6<T1, T2, U, T4, T5, T6> map3(@Independent @Modified Function<? super T3, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple6<T1, T2, T3, U, T5, T6> map4(@Independent @Modified Function<? super T4, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple6<T1, T2, T3, T4, U, T6> map5(@Independent @Modified Function<? super T5, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple6<T1, T2, T3, T4, T5, U> map6(@Independent @Modified Function<? super T6, ? extends U> mapper) {
            return null;
        }

        //override from io.vavr.Tuple
        @Independent(hc = true) @NotModified
        Seq<?> toSeq() { return null; }

        //override from java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent(hc = true) @NotModified
        Tuple6<T1, T2, T3, T4, T5, T6> update1(@Independent @NotModified T1 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple6<T1, T2, T3, T4, T5, T6> update2(@Independent @NotModified T2 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple6<T1, T2, T3, T4, T5, T6> update3(@Independent @NotModified T3 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple6<T1, T2, T3, T4, T5, T6> update4(@Independent @NotModified T4 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple6<T1, T2, T3, T4, T5, T6> update5(@Independent @NotModified T5 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple6<T1, T2, T3, T4, T5, T6> update6(@Independent @NotModified T6 value) { return null; }
    }

    //public final class Tuple7 implements Tuple, Comparable<Tuple7<T1,T2,T3,T4,T5,T6,T7>>, Serializable
    @Immutable(hc = true)
    @Independent
    class Tuple7$<T1, T2, T3, T4, T5, T6, T7> {
        @Independent @NotModified final T1 _1 = null;
        @Independent @NotModified final T2 _2 = null;
        @Independent @NotModified final T3 _3 = null;
        @Independent @NotModified final T4 _4 = null;
        @Independent @NotModified final T5 _5 = null;
        @Independent @NotModified final T6 _6 = null;
        @Independent @NotModified final T7 _7 = null;
        Tuple7$(
            @Independent(hc = true) @NotModified T1 t1,
            @Independent(hc = true) @NotModified T2 t2,
            @Independent(hc = true) @NotModified T3 t3,
            @Independent(hc = true) @NotModified T4 t4,
            @Independent(hc = true) @NotModified T5 t5,
            @Independent(hc = true) @NotModified T6 t6,
            @Independent(hc = true) @NotModified T7 t7) { }
        @Independent(hc = true) @NotModified @GetSet("_1") T1 _1() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_2") T2 _2() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_3") T3 _3() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_4") T4 _4() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_5") T5 _5() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_6") T6 _6() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_7") T7 _7() { return null; }
        @Independent(hc = true) @NotModified
        <T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> append(@Independent @NotModified T8 t8) { return null; }

        @Independent @NotModified
        <U> U apply(
            @Independent(hc = true) @Modified Function7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? extends U> f) { return null; }

        //override from io.vavr.Tuple
        @NotModified
        int arity() { return 0; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7> Comparator<Tuple7<T1, T2, T3, T4, T5, T6, T7>> comparator(
            @Independent @NotModified Comparator<? super T1> t1Comp,
            @Independent @NotModified Comparator<? super T2> t2Comp,
            @Independent @NotModified Comparator<? super T3> t3Comp,
            @Independent @NotModified Comparator<? super T4> t4Comp,
            @Independent @NotModified Comparator<? super T5> t5Comp,
            @Independent @NotModified Comparator<? super T6> t6Comp,
            @Independent @NotModified Comparator<? super T7> t7Comp) { return null; }

        //override from java.lang.Comparable
        @NotModified
        int compareTo(@Independent @NotModified Tuple7<T1, T2, T3, T4, T5, T6, T7> that) { return 0; }

        @Independent(hc = true) @NotModified
        <T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> concat(@Independent @NotModified Tuple1<T8> tuple) { return null; }

        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        @Independent @NotModified
        <U1, U2, U3, U4, U5, U6, U7> Tuple7<U1, U2, U3, U4, U5, U6, U7> map(
            @Independent(hc = true) @Modified Function7<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                Tuple7<U1, U2, U3, U4, U5, U6, U7>> mapper) { return null; }

        @Independent @NotModified
        <U1, U2, U3, U4, U5, U6, U7> Tuple7<U1, U2, U3, U4, U5, U6, U7> map(
            @Independent @Modified Function<? super T1, ? extends U1> f1,
            @Independent @Modified Function<? super T2, ? extends U2> f2,
            @Independent @Modified Function<? super T3, ? extends U3> f3,
            @Independent @Modified Function<? super T4, ? extends U4> f4,
            @Independent @Modified Function<? super T5, ? extends U5> f5,
            @Independent @Modified Function<? super T6, ? extends U6> f6,
            @Independent @Modified Function<? super T7, ? extends U7> f7) { return null; }

        @Independent(hc = true) @NotModified
        <U> Tuple7<U, T2, T3, T4, T5, T6, T7> map1(@Independent @Modified Function<? super T1, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple7<T1, U, T3, T4, T5, T6, T7> map2(@Independent @Modified Function<? super T2, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple7<T1, T2, U, T4, T5, T6, T7> map3(@Independent @Modified Function<? super T3, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple7<T1, T2, T3, U, T5, T6, T7> map4(@Independent @Modified Function<? super T4, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple7<T1, T2, T3, T4, U, T6, T7> map5(@Independent @Modified Function<? super T5, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple7<T1, T2, T3, T4, T5, U, T7> map6(@Independent @Modified Function<? super T6, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple7<T1, T2, T3, T4, T5, T6, U> map7(@Independent @Modified Function<? super T7, ? extends U> mapper) {
            return null;
        }

        //override from io.vavr.Tuple
        @Independent(hc = true) @NotModified
        Seq<?> toSeq() { return null; }

        //override from java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent(hc = true) @NotModified
        Tuple7<T1, T2, T3, T4, T5, T6, T7> update1(@Independent @NotModified T1 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple7<T1, T2, T3, T4, T5, T6, T7> update2(@Independent @NotModified T2 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple7<T1, T2, T3, T4, T5, T6, T7> update3(@Independent @NotModified T3 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple7<T1, T2, T3, T4, T5, T6, T7> update4(@Independent @NotModified T4 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple7<T1, T2, T3, T4, T5, T6, T7> update5(@Independent @NotModified T5 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple7<T1, T2, T3, T4, T5, T6, T7> update6(@Independent @NotModified T6 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple7<T1, T2, T3, T4, T5, T6, T7> update7(@Independent @NotModified T7 value) { return null; }
    }

    //public final class Tuple8 implements Tuple, Comparable<Tuple8<T1,T2,T3,T4,T5,T6,T7,T8>>, Serializable
    @Immutable(hc = true)
    @Independent
    class Tuple8$<T1, T2, T3, T4, T5, T6, T7, T8> {
        @Independent @NotModified final T1 _1 = null;
        @Independent @NotModified final T2 _2 = null;
        @Independent @NotModified final T3 _3 = null;
        @Independent @NotModified final T4 _4 = null;
        @Independent @NotModified final T5 _5 = null;
        @Independent @NotModified final T6 _6 = null;
        @Independent @NotModified final T7 _7 = null;
        @Independent @NotModified final T8 _8 = null;
        Tuple8$(
            @Independent(hc = true) @NotModified T1 t1,
            @Independent(hc = true) @NotModified T2 t2,
            @Independent(hc = true) @NotModified T3 t3,
            @Independent(hc = true) @NotModified T4 t4,
            @Independent(hc = true) @NotModified T5 t5,
            @Independent(hc = true) @NotModified T6 t6,
            @Independent(hc = true) @NotModified T7 t7,
            @Independent(hc = true) @NotModified T8 t8) { }
        @Independent(hc = true) @NotModified @GetSet("_1") T1 _1() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_2") T2 _2() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_3") T3 _3() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_4") T4 _4() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_5") T5 _5() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_6") T6 _6() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_7") T7 _7() { return null; }
        @Independent(hc = true) @NotModified @GetSet("_8") T8 _8() { return null; }
        @Independent @NotModified
        <U> U apply(
            @Independent(hc = true) @Modified Function8<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? super T8,
                ? extends U> f) { return null; }

        //override from io.vavr.Tuple
        @NotModified
        int arity() { return 0; }

        @Independent @NotModified
        static <T1, T2, T3, T4, T5, T6, T7, T8> Comparator<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> comparator(
            @Independent @NotModified Comparator<? super T1> t1Comp,
            @Independent @NotModified Comparator<? super T2> t2Comp,
            @Independent @NotModified Comparator<? super T3> t3Comp,
            @Independent @NotModified Comparator<? super T4> t4Comp,
            @Independent @NotModified Comparator<? super T5> t5Comp,
            @Independent @NotModified Comparator<? super T6> t6Comp,
            @Independent @NotModified Comparator<? super T7> t7Comp,
            @Independent @NotModified Comparator<? super T8> t8Comp) { return null; }

        //override from java.lang.Comparable
        @NotModified
        int compareTo(@Independent @NotModified Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> that) { return 0; }

        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        @Independent @NotModified
        <U1, U2, U3, U4, U5, U6, U7, U8> Tuple8<U1, U2, U3, U4, U5, U6, U7, U8> map(
            @Independent(hc = true) @Modified Function8<
                ? super T1,
                ? super T2,
                ? super T3,
                ? super T4,
                ? super T5,
                ? super T6,
                ? super T7,
                ? super T8,
                Tuple8<U1, U2, U3, U4, U5, U6, U7, U8>> mapper) { return null; }

        @Independent @NotModified
        <U1, U2, U3, U4, U5, U6, U7, U8> Tuple8<U1, U2, U3, U4, U5, U6, U7, U8> map(
            @Independent @Modified Function<? super T1, ? extends U1> f1,
            @Independent @Modified Function<? super T2, ? extends U2> f2,
            @Independent @Modified Function<? super T3, ? extends U3> f3,
            @Independent @Modified Function<? super T4, ? extends U4> f4,
            @Independent @Modified Function<? super T5, ? extends U5> f5,
            @Independent @Modified Function<? super T6, ? extends U6> f6,
            @Independent @Modified Function<? super T7, ? extends U7> f7,
            @Independent @Modified Function<? super T8, ? extends U8> f8) { return null; }

        @Independent(hc = true) @NotModified
        <U> Tuple8<U, T2, T3, T4, T5, T6, T7, T8> map1(@Independent @Modified Function<? super T1, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple8<T1, U, T3, T4, T5, T6, T7, T8> map2(@Independent @Modified Function<? super T2, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple8<T1, T2, U, T4, T5, T6, T7, T8> map3(@Independent @Modified Function<? super T3, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple8<T1, T2, T3, U, T5, T6, T7, T8> map4(@Independent @Modified Function<? super T4, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple8<T1, T2, T3, T4, U, T6, T7, T8> map5(@Independent @Modified Function<? super T5, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple8<T1, T2, T3, T4, T5, U, T7, T8> map6(@Independent @Modified Function<? super T6, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple8<T1, T2, T3, T4, T5, T6, U, T8> map7(@Independent @Modified Function<? super T7, ? extends U> mapper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <U> Tuple8<T1, T2, T3, T4, T5, T6, T7, U> map8(@Independent @Modified Function<? super T8, ? extends U> mapper) {
            return null;
        }

        //override from io.vavr.Tuple
        @Independent(hc = true) @NotModified
        Seq<?> toSeq() { return null; }

        //override from java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent(hc = true) @NotModified
        Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> update1(@Independent @NotModified T1 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> update2(@Independent @NotModified T2 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> update3(@Independent @NotModified T3 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> update4(@Independent @NotModified T4 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> update5(@Independent @NotModified T5 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> update6(@Independent @NotModified T6 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> update7(@Independent @NotModified T7 value) { return null; }

        @Independent(hc = true) @NotModified
        Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> update8(@Independent @NotModified T8 value) { return null; }
    }

    //public interface Value implements Iterable<T>
    //EXPECTED no immutability claim -- computed @FinalFields @Dependent -- root of G1 (computed is right): also implemented by Iterator, Future, Lazy: stateful (VAVR.md)
    @FinalFields
    @Independent(absent = true)
    class Value$<T> {
        @Independent @Modified
        <R> R collect(
            @Independent @NotModified Supplier<R> supplier,
            @Independent @NotModified BiConsumer<R, ? super T> accumulator,
            @Independent @NotModified BiConsumer<R, R> combiner) { return null; }

        @Independent @Modified
        <R, A> R collect(@Independent @NotModified Collector<? super T, A, R> collector) { return null; }
        @NotModified boolean contains(@Independent @NotModified T element) { return false; }
        @Modified
        <U> boolean corresponds(
            @Independent @NotModified Iterable<U> that,
            @Independent @Modified BiPredicate<? super T, ? super U> predicate) { return false; }
        @Modified boolean eq(@Independent @Modified Object o) { return false; }
        //override from java.lang.Object
        @Modified
        public boolean equals(@Independent @NotModified Object arg0) { return false; }
        @NotModified boolean exists(@Independent @Modified Predicate<? super T> predicate) { return false; }
        @NotModified boolean forAll(@Independent @NotModified Predicate<? super T> predicate) { return false; }
        //override from java.lang.Iterable
        @NotModified
        void forEach(@Independent @Modified Consumer<? super T> action) { }
        @Independent(hc = true) @Modified T get() { return null; }
        @Independent @Modified T getOrElse(@Independent @NotModified T other) { return null; }
        @Independent @Modified T getOrElse(@Independent @Modified Supplier<? extends T> supplier) { return null; }
        @Independent @Modified
        <X extends Throwable> T getOrElseThrow(@Independent @Modified Supplier<X> supplier) { return null; }

        @Independent @Modified
        T getOrElseTry(@Independent @Modified CheckedFunction0<? extends T> supplier) { return null; }
        @Independent @Modified T getOrNull() { return null; }
        //override from java.lang.Object
        @Modified
        public int hashCode() { return 0; }
        @NotModified boolean isAsync() { return false; }
        @Modified boolean isEmpty() { return false; }
        @NotModified boolean isLazy() { return false; }
        @NotModified boolean isSingleValued() { return false; }
        //override from java.lang.Iterable
        @Independent(absent = true) @Modified @NotNull
        Iterator<T> iterator() { return null; }

        @Independent @Modified
        <U> Value<U> map(
            @IgnoreModifications @Independent(absent = true) @Modified Function<? super T, ? extends U> arg0) {
            return null;
        }
        @Independent @Modified <U> Value<U> mapTo(@Independent @NotModified U value) { return null; }
        @Independent @Modified Value<Void> mapToVoid() { return null; }
        @Identity @Independent @NotModified
        static <T> Value<T> narrow(@Independent @NotModified Value<? extends T> value) { return null; }
        @NotModified void out(@Independent @Modified PrintStream out) { }
        @NotModified void out(@Independent @Modified PrintWriter writer) { }
        @Independent @Modified
        Value<T> peek(@IgnoreModifications @Independent @Modified Consumer<? super T> arg0) { return null; }

        //override from java.lang.Iterable
        @Independent @Modified
        Spliterator<T> spliterator() { return null; }
        @NotModified void stderr() { }
        @NotModified void stdout() { }
        @Modified @NotModified(after = "emptyContainer") String stringPrefix() { return null; }
        @Independent @Modified Array<T> toArray() { return null; }
        @Fluent @Independent @Modified CharSeq toCharSeq() { return null; }
        @Independent @Modified CompletableFuture<T> toCompletableFuture() { return null; }
        @Fluent @Independent @Modified
        <L> Either<L, T> toEither(@Independent(hc = true) @NotModified L left) { return null; }

        @Fluent @Independent @Modified
        <L> Either<L, T> toEither(@Independent @Modified Supplier<? extends L> leftSupplier) { return null; }
        @Independent @Modified <U> Validation<T, U> toInvalid(@Independent @NotModified U value) { return null; }
        @Independent @Modified
        <U> Validation<T, U> toInvalid(@Independent @Modified Supplier<? extends U> valueSupplier) { return null; }
        @Independent @Modified Object [] toJavaArray() { return null; }
        @Independent @NotModified T [] toJavaArray(Class<T> componentType) { return null; }
        @Independent @NotModified
        T [] toJavaArray(@Independent @Modified IntFunction<T []> arrayFactory) { return null; }

        @Independent @NotModified
        <C extends Collection<T>> C toJavaCollection(@Independent @Modified Function<Integer, C> factory) { return null; }
        @Independent @NotModified java.util.List<T> toJavaList() { return null; }
        @Independent @NotModified
        <LIST extends java.util.List<T>> LIST toJavaList(@Independent @Modified Function<Integer, LIST> factory) {
            return null;
        }

        @Independent @Modified
        <K, V> Map<K, V> toJavaMap(
            @Independent @Modified Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) { return null; }

        @Independent @Modified
        <K, V, MAP extends Map<K, V>> MAP toJavaMap(
            @Independent @Modified Supplier<MAP> factory,
            @Independent @Modified Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) { return null; }

        @Independent @Modified
        <K, V, MAP extends Map<K, V>> MAP toJavaMap(
            @Independent @Modified Supplier<MAP> factory,
            @Independent @Modified Function<? super T, ? extends K> keyMapper,
            @Independent @Modified Function<? super T, ? extends V> valueMapper) { return null; }
        @Independent @Modified Optional<T> toJavaOptional() { return null; }
        @Independent @Modified Stream<T> toJavaParallelStream() { return null; }
        @Independent @NotModified Set<T> toJavaSet() { return null; }
        @Independent @NotModified
        <SET extends Set<T>> SET toJavaSet(@Independent @Modified Function<Integer, SET> factory) { return null; }
        @Independent @Modified Stream<T> toJavaStream() { return null; }
        @Independent @Modified <R> Either<T, R> toLeft(@Independent @NotModified R right) { return null; }
        @Independent @Modified
        <R> Either<T, R> toLeft(@Independent @Modified Supplier<? extends R> right) { return null; }

        @Independent @Modified
        <K, V> io.vavr.collection.Map<K, V> toLinkedMap(
            @Independent @Modified Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) { return null; }

        @Independent @Modified
        <K, V> io.vavr.collection.Map<K, V> toLinkedMap(
            @Independent @Modified Function<? super T, ? extends K> keyMapper,
            @Independent @Modified Function<? super T, ? extends V> valueMapper) { return null; }
        @Independent @Modified io.vavr.collection.Set<T> toLinkedSet() { return null; }
        @Independent @Modified List<T> toList() { return null; }
        @Independent @Modified
        <K, V> io.vavr.collection.Map<K, V> toMap(
            @Independent @Modified Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) { return null; }

        @Independent @Modified
        <K, V> io.vavr.collection.Map<K, V> toMap(
            @Independent @Modified Function<? super T, ? extends K> keyMapper,
            @Independent @Modified Function<? super T, ? extends V> valueMapper) { return null; }
        @Fluent @Independent @Modified Option<T> toOption() { return null; }
        @Fluent @Independent @Modified PriorityQueue<T> toPriorityQueue() { return null; }
        @Independent @Modified
        PriorityQueue<T> toPriorityQueue(@Independent @Modified Comparator<? super T> comparator) { return null; }
        @Independent @Modified Queue<T> toQueue() { return null; }
        @Independent @Modified <L> Either<L, T> toRight(@Independent @NotModified L left) { return null; }
        @Independent @Modified
        <L> Either<L, T> toRight(@Independent @Modified Supplier<? extends L> left) { return null; }
        @Independent @Modified io.vavr.collection.Set<T> toSet() { return null; }
        @Independent @Modified
        <K, V> SortedMap<K, V> toSortedMap(
            @Independent @NotModified Comparator<? super K> comparator,
            @Independent @Modified Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) { return null; }

        @Independent @Modified
        <K, V> SortedMap<K, V> toSortedMap(
            @Independent @NotModified Comparator<? super K> comparator,
            @Independent @Modified Function<? super T, ? extends K> keyMapper,
            @Independent @Modified Function<? super T, ? extends V> valueMapper) { return null; }

        @Independent @Modified
        <K extends Comparable<? super K>, V> SortedMap<K, V> toSortedMap(
            @Independent @Modified Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) { return null; }

        @Independent @Modified
        <K extends Comparable<? super K>, V> SortedMap<K, V> toSortedMap(
            @Independent @Modified Function<? super T, ? extends K> keyMapper,
            @Independent @Modified Function<? super T, ? extends V> valueMapper) { return null; }
        @Fluent @Independent @Modified SortedSet<T> toSortedSet() { return null; }
        @Independent @Modified
        SortedSet<T> toSortedSet(@Independent @NotModified Comparator<? super T> comparator) { return null; }
        @Independent @Modified io.vavr.collection.Stream<T> toStream() { return null; }
        //override from java.lang.Object
        @Modified @NotNull
        public String toString() { return null; }
        @Independent @Modified Tree<T> toTree() { return null; }
        @Fluent @Independent @Modified
        <ID> List<Tree.Node<T>> toTree(
            @Independent @NotModified Function<? super T, ? extends ID> idMapper,
            @Independent @Modified Function<? super T, ? extends ID> parentMapper) { return null; }
        @Fluent @Independent @Modified Try<T> toTry() { return null; }
        @Fluent @Independent @Modified
        Try<T> toTry(@Independent @Modified Supplier<? extends Throwable> ifEmpty) { return null; }
        @Independent @Modified <E> Validation<E, T> toValid(@Independent @NotModified E error) { return null; }
        @Independent @Modified
        <E> Validation<E, T> toValid(@Independent @Modified Supplier<? extends E> errorSupplier) { return null; }
        @Independent @Modified <E> Validation<E, T> toValidation(@Independent @NotModified E invalid) { return null; }
        @Fluent @Independent @Modified
        <E> Validation<E, T> toValidation(@Independent @Modified Supplier<? extends E> invalidSupplier) { return null; }
        @Independent @Modified Vector<T> toVector() { return null; }
    }
}
