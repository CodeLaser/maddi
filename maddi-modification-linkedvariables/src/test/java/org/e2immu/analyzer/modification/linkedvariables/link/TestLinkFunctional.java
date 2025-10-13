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

package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLinkFunctional extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.function.Supplier;
            class X {
                static class M { private int i; int getI() { return i; } void setI(int i) { this.i = i; } }
                static final class N { private int i; int getI() { return i; } void setI(int i) { this.i = i; } }
            
                static <X> X m0(Supplier<X> supplier) {
                    return supplier.get();
                }
            
                static <X> X m0b(Supplier<X> supplier) {
                    //noinspection ALL
                    Supplier<X> s = () -> supplier.get();
                    return s.get();
                }
            
                static M m1(Supplier<M> supplier) {
                    return supplier.get();
                }
            
                static Integer m2(Supplier<Integer> supplier) {
                    return supplier.get();
                }
            
                static N m3(Supplier<N> supplier) {
                    return supplier.get();
                }
            }
            """;

    @DisplayName("supplier basics")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo m0 = X.findUniqueMethod("m0", 1);
        assertEquals("*-4-0:supplier", lvs(m0));

        MethodInfo m0b = X.findUniqueMethod("m0b", 1);
        assertEquals("*-4-0:supplier", lvs(m0b));

        MethodInfo m1 = X.findUniqueMethod("m1", 1);
        assertEquals("*M-4-0M:supplier", lvs(m1));

        MethodInfo m2 = X.findUniqueMethod("m2", 1);
        assertEquals("", lvs(m2));

        MethodInfo m3 = X.findUniqueMethod("m3", 1);
        assertEquals("*M-4-0M:supplier", lvs(m3));
    }

    private static String lvs(MethodInfo methodInfo) {
        return methodInfo.analysis().getOrDefault(LinkedVariablesImpl.LINKED_VARIABLES_METHOD, LinkedVariablesImpl.EMPTY).toString();
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.stream.Stream;
            import java.util.Optional;
            class X {
                static class M { private int i; int getI() { return i; } void setI(int i) { this.i = i; } }
            
                static Stream<M> m3(Stream<M> stream) {
                    return stream.filter(m -> m.i == 3);
                }
            
                static Optional<M> m4(Stream<M> stream) {
                    return stream.filter(m -> m.i == 3).findFirst();
                }
            
                static <T> Optional<T> m4b(Stream<T> stream) {
                    return stream.filter(t -> t != null).findFirst();
                }
            
                static M m5(Stream<M> stream) {
                    return stream.filter(m -> m.i == 3).findFirst().orElseThrow();
                }
            
                static Stream<Integer> m6(Stream<Integer> stream) {
                    return stream.filter(i -> i == 3);
                }
            
                static Optional<Integer> m7(Stream<Integer> stream) {
                    return stream.filter(i -> i == 3).findFirst();
                }
            
                static Integer m8(Stream<Integer> stream) {
                    return stream.filter(i -> i == 3).findFirst().orElseThrow();
                }
            }
            """;

    @DisplayName("stream basics")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepWork(X);

        TypeInfo stream = javaInspector.compiledTypesManager().get(Stream.class);
        HiddenContentTypes hctStream = stream.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=T, 1=Stream", hctStream.detailedSortedTypes());
        MethodInfo filter = stream.findUniqueMethod("filter", 1);
        assertEquals("java.util.stream.Stream.filter(java.util.function.Predicate<? super T>)", filter.fullyQualifiedName());
        HiddenContentTypes hctFilter = filter.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=T, 1=Stream - 2=Predicate", hctFilter.detailedSortedTypes());
        HiddenContentSelector hcsFilter = filter.analysis().getOrDefault(HiddenContentSelector.HCS_METHOD, HiddenContentSelector.NONE);
        assertEquals("0=0,1=*", hcsFilter.detailed());

        MethodInfo test = (MethodInfo) analysisOrder.stream()
                .filter(i -> "a.b.X.$0.test(a.b.X.M)".equals(i.fullyQualifiedName())).findFirst().orElseThrow();
        HiddenContentTypes hctTest = test.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=Stream, 1=M - ", hctTest.detailedSortedTypes());

        MethodInfo test1 = (MethodInfo) analysisOrder.stream()
                .filter(i -> "a.b.X.$1.test(a.b.X.M)".equals(i.fullyQualifiedName())).findFirst().orElseThrow();
        HiddenContentTypes hctTest1 = test1.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=Stream, 1=M, 2=Optional - ", hctTest1.detailedSortedTypes());

        MethodInfo m3 = X.findUniqueMethod("m3", 1);
        HiddenContentTypes hctM3 = m3.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals(" - 0=Stream, 1=M", hctM3.detailedSortedTypes());

        analyzer.go(analysisOrder);

        // link to T which gets modifying M, and predicate (also modifying)
        assertEquals("0M-4-0M:stream", lvs(m3));

        MethodInfo m4 = X.findUniqueMethod("m4", 1);
        assertEquals("0M-4-0M:stream", lvs(m4));

        MethodInfo m4b = X.findUniqueMethod("m4b", 1);
        assertEquals("0-4-0:stream", lvs(m4b));

        MethodInfo m5 = X.findUniqueMethod("m5", 1);
        assertEquals("*M-4-0M:stream", lvs(m5));

        MethodInfo m6 = X.findUniqueMethod("m6", 1);
        assertEquals("", lvs(m6)); // as Integer does not have hidden content

        MethodInfo m7 = X.findUniqueMethod("m7", 1);
        assertEquals("", lvs(m7));

        MethodInfo m8 = X.findUniqueMethod("m8", 1);
        assertEquals("", lvs(m8));
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Optional;
            import java.util.function.Predicate;
            import java.util.stream.Stream;
            
            class X {
                // prep test for the m9-m11
                static <X> boolean mPredicate(X x, Predicate<X> predicate) {
                    return predicate.test(x);
                }
            
                static <X> Stream<X> m9(Stream<X> stream, Predicate<X> predicate) {
                    //noinspection ALL
                    return stream.filter(i -> predicate.test(i));
                }
            
                static <X> Stream<X> m9b(Stream<X> stream, Predicate<X> predicate) {
                    //noinspection ALL
                    Predicate<X> p = i -> predicate.test(i);
                    return stream.filter(p);
                }
            
                // lambda is type $8
                static <X> Optional<X> m10(Stream<X> stream, Predicate<X> predicate) {
                    //noinspection ALL
                    return stream.filter(x -> predicate.test(x)).findFirst();
                }
            
                static <X> X m11(Stream<X> stream, Predicate<X> predicate) {
                    //noinspection ALL
                    return stream.filter(i -> predicate.test(i)).findFirst().orElseThrow();
                }
            }
            """;

    @DisplayName("predicate basics")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo mPredicate = X.findUniqueMethod("mPredicate", 2);
        assertEquals("", lvs(mPredicate));

        MethodInfo m9 = X.findUniqueMethod("m9", 2);
        assertEquals("0-4-0:stream", lvs(m9));

        MethodInfo m9b = X.findUniqueMethod("m9b", 2);
        assertEquals("0-4-0:stream", lvs(m9b));

        MethodInfo m10 = X.findUniqueMethod("m10", 2);
        assertEquals("0-4-0:stream", lvs(m10));

        MethodInfo m11 = X.findUniqueMethod("m11", 2);
        assertEquals("*-4-0:stream", lvs(m11));
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.stream.Stream;
            import java.util.function.Function;
            class P {
                static class M { private int i; int getI() { return i; } void setI(int i) { this.i = i; } }
            
                static <X, Y> Stream<Y> m1(Stream<X> stream, Function<X, Y> function) {
                    //noinspection ALL
                    return stream.map(x -> function.apply(x));
                }
            
                static <Y> Stream<Y> m2(Stream<M> stream, Function<M, Y> function) {
                    //noinspection ALL
                    return stream.map(x -> function.apply(x));
                }
            
                static <X, Y> Stream<Y> m3(Stream<X> stream, Function<X, Y> function) {
                    //noinspection ALL
                    Function<X, Y> f = x -> function.apply(x);
                    return stream.map(f);
                }
            
                static <X> Stream<M> m4(Stream<X> stream, Function<X, M> function) {
                    //noinspection ALL
                    Function<X, M> f = x -> function.apply(x);
                    return stream.map(f);
                }
            
                static <X, Y> Stream<Y> m5(Stream<X> stream, Function<X, Y> function) {
                    //noinspection ALL
                    return stream.map(function::apply);
                }
            
                static Stream<M> m6(Stream<M> stream, Function<M, M> function) {
                    //noinspection ALL
                    return stream.map(function::apply);
                }
            
                static Stream<M> m7(Stream<M> stream, Function<M, M> function) {
                    return stream.map(function);
                }
            }
            """;

    @DisplayName("function basics")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        List<Info> analysisOrder = prepWork(X);

        TypeInfo function = javaInspector.compiledTypesManager().get(Function.class);
        assertEquals("0=T, 1=R", function.analysis().getOrNull(HiddenContentTypes.HIDDEN_CONTENT_TYPES,
                HiddenContentTypes.class).detailedSortedTypes());
        MethodInfo m1 = X.findUniqueMethod("m1", 2);
        assertEquals(" - 0=X, 1=Y, 2=Stream, 3=Function", m1.analysis().getOrNull(HiddenContentTypes.HIDDEN_CONTENT_TYPES,
                HiddenContentTypes.class).detailedSortedTypes());
        assertEquals("1=0,2=*", m1.analysis().getOrNull(HiddenContentSelector.HCS_METHOD,
                HiddenContentSelector.class).detailed());

        analyzer.go(analysisOrder);

        // IMPORTANT
        // there is no means of connecting X to Y at this point (not without knowledge of Function!)
        // the result of the method is of type Stream<T>, so it cannot link to the hidden content of Function.
        // Should we assume that Function<X, Y> holds fields of type Y, so that we can link 0-4-1:function??
        assertEquals("", lvs(m1));

        MethodInfo m2 = X.findUniqueMethod("m2", 2);
        assertEquals("", lvs(m2));

        MethodInfo m3 = X.findUniqueMethod("m3", 2);
        assertEquals("", lvs(m3));

        MethodInfo m4 = X.findUniqueMethod("m4", 2);
        assertEquals("", lvs(m4));

        MethodInfo m5 = X.findUniqueMethod("m5", 2);
        assertEquals("", lvs(m5));

        MethodInfo m6 = X.findUniqueMethod("m6", 2);
        assertEquals("", lvs(m6));

        MethodInfo m7 = X.findUniqueMethod("m7", 2);
        assertEquals("", lvs(m7));
    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.function.Consumer;
            import java.util.List;
            class C {
                static class M { private int i; int getI() { return i; } void setI(int i) { this.i = i; } }
            
                static List<String> m1(List<String> in, List<String> out) {
                    //noinspection ALL
                    in.forEach(out::add);
                    return out;
                }
            
                static <X> List<X> m2(List<X> in, List<X> out) {
                    //noinspection ALL
                    in.forEach(out::add);
                    return out;
                }
            
                static <X> List<X> m3(List<X> in, List<X> out) {
                    //noinspection ALL
                    Consumer<X> add = out::add;
                    in.forEach(add);
                    return out;
                }
            
                static List<M> m4(List<M> in, List<M> out) {
                    //noinspection ALL
                    in.forEach(out::add);
                    return out;
                }
            }
            """;

    @DisplayName("forEach basics")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo m1 = X.findUniqueMethod("m1", 2);
        assertEquals("-1-:out", lvs(m1));

        MethodInfo m2 = X.findUniqueMethod("m2", 2);
        assertEquals("0-4-0:in, -1-:out", lvs(m2));

        MethodInfo m3 = X.findUniqueMethod("m3", 2);
        Statement s0 = m3.methodBody().statements().getFirst();
        VariableData vd0 = VariableDataImpl.of(s0);
        VariableInfo vi0add = vd0.variableInfo("add");
        assertEquals("E=out::add", vi0add.staticValues().toString());
        assertEquals("0-4-0:out", vi0add.linkedVariables().toString());

        assertEquals("0-4-0:in, -1-:out", lvs(m3));

        MethodInfo m4 = X.findUniqueMethod("m4", 2);
        assertEquals("0M-4-0M:in, -1-:out", lvs(m4));
    }

    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.util.function.Supplier;
            import java.util.function.IntFunction;
            import java.util.stream.IntStream;
            import java.util.stream.Stream;
            import java.util.List;
            class X {
                static class M { private int i; int getI() { return i; } void setI(int i) { this.i = i; } }
       /*     
                static <X> Stream<X> m1(Supplier<X> supplier) {
                    return IntStream.of(3).mapToObj(i -> supplier.get());
                }
            
                static <X> Stream<X> m2(Supplier<X> supplier) {
                    IntFunction<X> f = i -> supplier.get();
                    return IntStream.of(3).mapToObj(f);
                }
            
                static Stream<M> m3(Supplier<M> supplier) {
                    return IntStream.of(3).mapToObj(i -> supplier.get());
                }
            
                static <X> Stream<X> m4(List<X> list) {
                    //noinspection ALL
                    return IntStream.of(3).mapToObj(i -> list.get(i));
                }
            
                static Stream<M> m5(List<M> list) {
                    //noinspection ALL
                    return IntStream.of(3).mapToObj(i -> list.get(i));
                }
            */
                static <X> Stream<X> m6(List<X> list) {
                    return IntStream.of(3).mapToObj(list::get);
                }
            /*
                static Stream<M> m7(List<M> list) {
                    return IntStream.of(3).mapToObj(list::get);
                }
            
                static Stream<M> m8(List<M> list) {
                    IntFunction<M> get = list::get;
                    return IntStream.of(3).mapToObj(get);
                }
            
                static <X> Stream<X> m9(List<X> list) {
                    return IntStream.of(3).mapToObj(new IntFunction<X>() {
                        @Override
                        public X apply(int value) {
                            return list.get(value);
                        }
                    });
                }
            
                static <X> Stream<X> m10(List<X> list) {
                    IntFunction<X> f = new IntFunction<>() {
                        @Override
                        public X apply(int value) {
                            return list.get(value);
                        }
                    };
                    IntStream intStream = IntStream.of(3);
                    return intStream.mapToObj(f);
                }
            
                static <X> Stream<X> m11(List<X> list) {
                    IntFunction<X> f = new IntFunction<>() {
                        @Override
                        public X apply(int value) {
                            return list.get(value);
                        }
                    };
                    return IntStream.of(3).mapToObj(f);
                }
            
                static Stream<M> m12(List<M> list) {
                    return IntStream.of(3).mapToObj(new IntFunction<M>() {
                        @Override
                        public M apply(int value) {
                            return list.get(value);
                        }
                    });
                }
            */
            }
            """;

    @Disabled
    @DisplayName("around IntStream")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        {
        //    MethodInfo m1 = X.findUniqueMethod("m1", 1);
       //     assertEquals("0-4-0:supplier", lvs(m1));
        }
        {
            MethodInfo m6 = X.findUniqueMethod("m6", 1);
            assertEquals("0-4-0:list", lvs(m6));
        }
    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import java.util.stream.Stream;
            import java.util.Optional;
            class X {
                static class M { private int i; int getI() { return i; } void setI(int i) { this.i = i; } }            
                // We're contracting the anonymous type's method (the SAM) to be @NotModified.
                // As a consequence, we'll prevent creating an inlined method, because that
                // inlined method will link xx to selector (see m27).
            
                static <X> boolean m25(@Independent X xx, Predicate<X> selector) {
                    Predicate<X> independentSelector = new Predicate<X>() {
                        @NotModified(contract = true)
                        @Override
                        public boolean test(@Independent(contract = true) X x) {
                            return selector.test(x);
                        }
                    };
                    return independentSelector.test(xx);
                }
            
                // same as 25, but without the explicit @Independent, which is not relevant
                // because selector is not a field.
                static <X> boolean m26(@Independent X xx, Predicate<X> selector) {
                    Predicate<X> independentSelector = new Predicate<X>() {
                        @NotModified(contract = true)
                        @Override
                        public boolean test(X x) {
                            return selector.test(x);
                        }
                    };
                    return independentSelector.test(xx);
                }
            
                //Here, xx -4- selector in statement 1.
                //xx is still @Independent, because that is wrt the fields (that are absent).
                //The cross-link from xx to selector is not computed (only contracted as of now, 202404)
                static <X> boolean m27(@Independent X xx, Predicate<X> selector) {
                    Predicate<X> independentSelector = new Predicate<X>() {
                        @Override
                        public boolean test(X x) {
                            return selector.test(x);
                        }
                    };
                    return independentSelector.test(xx);
                }
            
            
                //Corresponds to Linking_2.m4
                static boolean m28(@NotModified M m, @Container(contract = true) Predicate<M> selector) {
                    return selector.test(m);
                }
            
                interface X {
                }
            
                interface Y {
                }
            
                // examples of different linkings for actual function implementations
            
                // y = f1.apply(x)  --> y 0-4-1 f1
                static class F1 implements Function<X, Y> {
                    List<Y> ys;
            
                    @Override
                    public Y apply(X x) {
                        int index = Math.abs(x.hashCode()) % ys.size();
                        return ys.get(index);
                    }
                }
            
                interface T {
                }
            
                // y = f2.apply(x)  --> y *-4-0 x
                static class F2 implements Function<List<T>, T> {
                    private int i;
            
                    @Override
                    public T apply(List<T> ts) {
                        return ts.get((++i) % ts.size());
                    }
                }
            
                // y = f3.apply(x)  --> y 0-4-0 x
                static class F3 implements Function<List<T>, List<T>> {
                    private int i;
            
                    @Override
                    public List<T> apply(List<T> ts) {
                        return List.copyOf(ts.subList(0, ++i % ts.size()));
                    }
                }
            
                // y = f3.apply(x)  --> y 0-4-0 x, 0-4-0 f4
                static class F4 implements Function<List<T>, List<T>> {
                    private int i;
                    private List<T> altList;
            
                    @Override
                    public List<T> apply(List<T> ts) {
                        int index = ++i % ts.size();
                        if (index % 2 == 0) return List.of(altList.get(i % altList.size()));
                        return List.copyOf(ts.subList(0, index));
                    }
                }
            }
            """;
}
