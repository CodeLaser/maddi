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

package io.codelaser.maddi.cst.print.csharp;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.List;
import java.util.Set;

/**
 * The Java forms C# says differently: types and their members, statements and expressions, generics, inner
 * and anonymous classes, functional interfaces as delegates. Each test writes its samples' translations to {@code build/csharp-samples/}.
 */
public class TestJavaToCSharpStructure extends CommonJavaToCSharp {

    @Override
    protected boolean golden() {
        return true;
    }

    @Language("java")
    private static final String CLASS = """
            package org.example.shapes;
            public class Counter {
                private int count;
                private final String name;
                public static final int LIMIT = 10;
                public Counter(String name) { this.name = name; }
                public int getCount() { return count; }
                public void increment() {
                    if (count < LIMIT) {
                        count++;
                    } else if (count == LIMIT) {
                        count = 0;
                    } else {
                        count += 2;
                    }
                }
                public final String name() { return name; }
                @Override public String toString() { return name + ":" + count; }
            }
            """;

    /**
     * A getter of a field is a property that takes the field's place: {@code { get; }} for a field assigned only in
     * constructors, {@code private set} for one that its class writes too. The field's uses are the property's.
     */
    @Test
    public void classMembers() {
        String cs = translate("Counter", CLASS);
        contains(cs, "namespace Org.Example.Shapes;");
        contains(cs, "public sealed class Counter {");
        contains(cs, "public int Count { get; private set; }");
        contains(cs, "public string Name { get; }");
        contains(cs, "public const int LIMIT = 10;");
        contains(cs, "public Counter(string name) { this.Name = name; }");
        contains(cs, "if (Count < LIMIT) { Count++; } else if (Count == LIMIT) { Count = 0; } else { Count += 2; }");
        contains(cs, "public override string ToString() => Name + \":\" + Count;");
    }

    /** The policy can keep Java's getters as methods. */
    @Test
    public void classMembersAsMethods() {
        policy = CSharpProgram.Policy.DEFAULT.withAccessors(CSharpProgram.Accessors.METHODS);
        String cs = translate("CounterAsMethods", CLASS);
        contains(cs, "private int count;");
        contains(cs, "private readonly string name;");
        contains(cs, "public const int LIMIT = 10;");
        contains(cs, "public Counter(string name) { this.name = name; }");
        contains(cs, "public int GetCount() => count;");
        contains(cs, "} else if (count == LIMIT) {");
        contains(cs, "public string Name() => name;");
        contains(cs, "public override string ToString() => name + \":\" + count;");
    }

    /** Code outside the program may extend a public class: the policy can keep it, and its methods, open. */
    @Test
    public void classMembersOpenPublicApi() {
        policy = new CSharpProgram.Policy(CSharpProgram.FunctionalInterfaces.DELEGATE_WHERE_POSSIBLE,
                CSharpProgram.Inheritance.OPEN_PUBLIC_API);
        String cs = translate("Counter", CLASS);
        contains(cs, "public class Counter {");
        contains(cs, "public virtual int Count { get; private set; }");
        contains(cs, "public string Name { get; }");
    }

    @Language("java")
    private static final String HIERARCHY = """
            package org.example.shapes;
            interface Shape { double area(); default String describe() { return "shape"; } }
            abstract class Base implements Shape {
                protected final double scale;
                protected Base(double scale) { this.scale = scale; }
                public abstract double unit();
                public double area() { return unit() * scale; }
            }
            final class Square extends Base {
                private final double side;
                Square(double side) { super(1.0); this.side = side; }
                @Override public double unit() { return side * side; }
                @Override public String describe() { return "square"; }
            }
            """;

    /**
     * Methods that override one another, parameterless and of one type, are one property: the interface declares it
     * (with its default body), an abstract class leaves it abstract, the others compute it. The lambda adapter of
     * the functional interface implements it as a property.
     */
    @Test
    public void hierarchy() {
        String cs = translate("Hierarchy", HIERARCHY);
        contains(cs, "internal interface IShape {\ndouble Area { get; }");
        contains(cs, "string Describe => \"shape\";");
        contains(cs, "public sealed class Lambda(Func<double> f) : IShape { public double Area => f(); }");
        contains(cs, "internal abstract class Base : IShape {");
        contains(cs, "protected internal readonly double scale;");
        contains(cs, "public abstract double Unit { get; }");
        contains(cs, "public double Area => Unit * scale;");
        contains(cs, "internal sealed class Square : Base {");
        contains(cs, "internal Square(double side) : base(1.0) { this.side = side; }");
        contains(cs, "public override double Unit => side * side;");
    }

    @Language("java")
    private static final String ENUMS = """
            package org.example.color;
            public class Palette {
                enum Primary { RED, GREEN, BLUE }
                enum Planet {
                    MERCURY(3.303e+23), EARTH(5.976e+24);
                    private final double mass;
                    Planet(double mass) { this.mass = mass; }
                    double mass() { return mass; }
                }
                static String name(Primary p) {
                    switch (p) {
                        case RED: return "red";
                        case GREEN:
                        case BLUE:
                            return "other";
                        default:
                            return "?";
                    }
                }
                static Primary first() { return Primary.RED; }
                static boolean later(Planet a, Planet b) { return a.compareTo(b) > 0; }
            }
            """;

    @Test
    public void enums() {
        String cs = translate("Palette", ENUMS);
        contains(cs, "internal enum Primary { Red, Green, Blue }");
        // Java's enum is Comparable by its ordinal
        contains(cs, "internal sealed class Planet : IComparable<Planet> {");
        contains(cs, "public int CompareTo(Planet other) => Ordinal.CompareTo(other.Ordinal);");
        contains(cs, "internal static bool Later(Planet a, Planet b) => a.CompareTo(b) > 0;");
        contains(cs, "public static readonly Planet Mercury = new Planet(3.303E23) { Name = \"MERCURY\", Ordinal = 0 };");
        contains(cs, "case Primary.Red:");
        contains(cs, "internal static Primary? First() => Primary.Red;");
    }

    @Language("java")
    private static final String ERASED_DIAMONDS = """
            package org.example.erased;
            import java.util.Comparator;
            import java.util.function.Function;
            class Erased {
                interface Sorter<S, T> { }
                static final class ComparingSorter<S, T> implements Sorter<S, T> {
                    ComparingSorter(Comparator<T> comparator) { }
                }
                static final class Joiner<A, B> {
                    private final Function<A, Object>[] mappings;
                    @SuppressWarnings("unchecked")
                    Joiner(Function<A, ?>[] mappings) { this.mappings = (Function<A, Object>[]) mappings; }
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Joiner<A, B> reordered() {
                        var copy = new Function[mappings.length];
                        for (int i = 0; i < copy.length; i++) copy[i] = mappings[mappings.length - 1 - i];
                        return new Joiner<>(copy);
                    }
                }
                static final class Holder<S> {
                    private Sorter<S, Object> sorter;
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    void sortBy(Object instance) {
                        var comparator = (Comparator) instance;
                        sorter = new ComparingSorter<>(comparator);
                    }
                }
            }
            """;

    /** A diamond of raw arguments: Java infers the raw type, C# creates it with the target's type arguments. */
    @Test
    public void erasedDiamonds() {
        String cs = translate("Erased", ERASED_DIAMONDS);
        // not of a raw array, Func<object, object>[], which C# does not pass as the Func<A, object>[] of Joiner<A, B>
        contains(cs, "return new Joiner<object, object>(copy);");
        contains(cs, "sorter = new ComparingSorter<S, object>(comparator);");
    }

    @Language("java")
    private static final String LAMBDA_INFERENCE = """
            package org.example.inference;
            import java.util.List;
            class Groups {
                interface Lifecycle<T> { }
                interface Constructor<T> { }
                interface Builder<K, T> { Object build(K key, int index, Lifecycle<T> next); }
                static final class Node<K, T> { Node(K key, int index, Lifecycle<T> next) { } }
                static <K, T> Constructor<T> groupBy(K key, Builder<K, T> builder) { return null; }
                static Constructor<List<String>> byReference(String key) { return groupBy(key, Node::new); }
                static Constructor<List<String>> byLambda(String key) {
                    return groupBy(key, (k, i, next) -> new Node<>(k, i, next));
                }
            }
            """;

    /** A type parameter only a lambda's or a method reference's parameters mention: C# does not infer it. */
    @Test
    public void lambdaInference() {
        String cs = translate("Groups", LAMBDA_INFERENCE);
        contains(cs, "GroupBy<string, List<string>>(\nkey,");
        // a raw Node::new creates the type of the delegate's parameters
        contains(cs, "(p0, p1, p2) => new Node<string, List<string>>(p0, p1, p2)");
    }

    @Language("java")
    private static final String GENERIC_DEFAULTS = """
            package org.example.defaults;
            import java.util.List;
            class Streams {
                interface Stream<A> {
                    <C> List<C> join(Class<C> other, String... joiners);
                    <S extends Comparable<S>> S penalize(S weight, long match);
                }
                interface InnerStream<A> extends Stream<A> {
                    default <C> List<C> join(Class<C> other, String... joiners) { return List.of(); }
                    default <S extends Comparable<S>> S penalize(S weight, long match) { return weight; }
                }
                static final class Concrete implements InnerStream<String> { }
            }
            """;

    /** A generic default method of a sub-interface implements the inherited one explicitly, with its type parameters. */
    @Test
    public void genericDefaults() {
        String cs = translate("Streams", GENERIC_DEFAULTS);
        contains(cs, "List<C> IStream<A>.Join<C>(params string[] joiners) => new List<C>();");
        contains(cs, "S IStream<A>.Penalize<S>(S weight, long match) => weight;");
    }

    @Language("java")
    private static final String SCOPED_DIAMONDS = """
            package org.example.scoped;
            import java.util.*;
            import java.util.function.Function;
            class Scoped {
                static final class OrderedSet<E> extends AbstractSet<E> {
                    public Iterator<E> iterator() { return Collections.emptyIterator(); }
                    public int size() { return 0; }
                }
                static final class Notifiable<T> {
                    Notifiable(Collection<List<T>> queue) { }
                }
                static <T> Notifiable<T> build(boolean unique) {
                    return new Notifiable<>(unique ? new OrderedSet<>() : new ArrayDeque<>());
                }
                @SuppressWarnings("rawtypes")
                static final class Joiner<A> {
                    @SuppressWarnings("unchecked")
                    private static final Joiner NONE = new Joiner(new Function[0]);
                    Joiner(Function<A, Object>[] mappings) { }
                }
            }
            """;

    /**
     * A diamond whose parameter is a supertype its class reaches through another, Collection through Set; a raw
     * creation's raw array, of no type parameter out of scope.
     */
    @Test
    public void scopedDiamonds() {
        String cs = translate("Scoped", SCOPED_DIAMONDS);
        contains(cs, "unique ? new OrderedSet<List<T>>()\n: new List<List<T>>()");
        contains(cs, "new Joiner<object>(new Func<object, object>[0])");
    }

    @Language("java")
    private static final String GENERIC_CONSTRUCTORS = """
            package org.example.ctors;
            import java.util.function.Function;
            class Joiners {
                static final class Joiner<A, B> {
                    <P> Joiner(Function<A, P> left, Function<B, P> right) { }
                }
                static <A, B, P extends Comparable<P>> Joiner<A, B> lessThan(Function<A, P> left, Function<B, P> right) {
                    return new Joiner<>(left, right);
                }
            }
            """;

    /** A generic constructor, which C# does not have: its own type parameter is object, a delegate of it widened. */
    @Test
    public void genericConstructors() {
        String cs = translate("Joiners", GENERIC_CONSTRUCTORS);
        contains(cs, "internal Joiner(Func<A, object> left, Func<B, object> right) { }");
        contains(cs, "new Joiner<A, B>(x0_ => left(x0_), x0_ => right(x0_))");
    }

    @Language("java")
    private static final String LONG_TYPE_ARGUMENTS = """
            package org.example.longtypes;
            import java.util.*;
            class Notifiables {
                interface ChangeEventOfAVeryLongName<T> { }
                interface ListenerOfAnEvenLongerNameStill<S, T> { boolean requiresUniqueEntityEvents(); }
                interface NotificationWithALongName<S, E, L> { }
                static final class OrderedSetOfSomeLength<E> extends AbstractSet<E> {
                    public Iterator<E> iterator() { return Collections.emptyIterator(); }
                    public int size() { return 0; }
                }
                static final class Notifiable<S> {
                    Notifiable(Object scoreDirector, ListenerOfAnEvenLongerNameStill<S, Object> listener,
                               Collection<NotificationWithALongName<S, ChangeEventOfAVeryLongName<Object>,
                                       ListenerOfAnEvenLongerNameStill<S, Object>>> queue, int globalOrder) { }
                }
                @SuppressWarnings("unchecked")
                static <S> Notifiable<S> build(Object scoreDirector, Object variableListener, int globalOrder) {
                    if (variableListener instanceof ListenerOfAnEvenLongerNameStill<?, ?> basicVariableListener) {
                        return new Notifiable<>(
                                scoreDirector,
                                (ListenerOfAnEvenLongerNameStill<S, Object>) basicVariableListener,
                                basicVariableListener.requiresUniqueEntityEvents() ? new OrderedSetOfSomeLength<>() : new ArrayDeque<>(),
                                globalOrder);
                    }
                    return null;
                }
            }
            """;

    /** A line the formatter cannot fit: a type argument longer than the line splits between tokens, not in one. */
    @Test
    public void longTypeArguments() {
        String cs = translate("Notifiables", LONG_TYPE_ARGUMENTS);
        contains(cs, "basicVariableListener()\n? new OrderedSetOfSomeLength<");
    }

    @Language("java")
    private static final String WILDCARD_DELEGATES = """
            package org.example.collectors;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.function.BiFunction;
            import java.util.function.Function;
            import java.util.function.Supplier;
            class Collectors {
                interface Collector<A, ResultContainer_, Result_> {
                    Supplier<ResultContainer_> supplier();
                    BiFunction<ResultContainer_, A, Runnable> accumulator();
                    Function<ResultContainer_, Result_> finisher();
                }
                static final class ToList<A> implements Collector<A, List<A>, List<A>> {
                    public Supplier<List<A>> supplier() { return ArrayList::new; }
                    public BiFunction<List<A>, A, Runnable> accumulator() {
                        return (list, a) -> { list.add(a); return () -> list.remove(a); };
                    }
                    public Function<List<A>, List<A>> finisher() { return list -> list; }
                }
                static <A> Collector<A, ?, List<A>> toList() { return new ToList<>(); }
                static final class Group<A, R> {
                    private final Collector<A, ?, R> collector;
                    Group(Collector<A, ?, R> collector) { this.collector = collector; }
                    @SuppressWarnings("unchecked")
                    R collect(List<A> items) {
                        Object container = collector.supplier().get();
                        var accumulator = (BiFunction<Object, A, Runnable>) (BiFunction<?, A, Runnable>) collector.accumulator();
                        for (A a : items) accumulator.apply(container, a);
                        return ((Function<Object, R>) (Function<?, R>) collector.finisher()).apply(container);
                    }
                }
                static List<String> names(List<String> items) { return new Group<>(Collectors.<String>toList()).collect(items); }
            }
            """;

    /**
     * A wildcard of a type whose members return a delegate of the wildcarded type parameter: the wildcard interface
     * has them with it erased, {@code Func<object, A, Action>}, which the type's bridge adapts to by a lambda.
     */
    @Test
    public void wildcardDelegates() {
        String cs = translate("Collectors", WILDCARD_DELEGATES);
        contains(cs, "internal static ICollector<A, List<A>> toList<A>() => new ToList<A>();");
        contains(cs, "internal interface ICollector<A, ResultContainer_, Result_> : ICollector<A, Result_> {");
        contains(cs, "Func<object, A, Action> Accumulator();");
        contains(cs, "Func<object> ICollector<A, Result_>.Supplier() { var f = Supplier(); return () => f(); }");
        contains(cs, "Func<object, A, Action> ICollector<A, Result_>.Accumulator() { var f = Accumulator(); return (x0_, x1_) => f((ResultContainer_) x0_, x1_); }");
    }

    @Language("java")
    private static final String ERASURES = """
            package org.example.erasures;
            import java.util.ArrayList;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            import java.util.function.BiFunction;
            import java.util.function.Function;
            import java.util.function.Supplier;
            class Collectors {
                interface Collector<A, ResultContainer_, Result_> {
                    Supplier<ResultContainer_> supplier();
                    BiFunction<ResultContainer_, A, Runnable> accumulator();
                    Function<ResultContainer_, Result_> finisher();
                }
                static final class ToList<A> implements Collector<A, List<A>, List<A>> {
                    public Supplier<List<A>> supplier() { return ArrayList::new; }
                    public BiFunction<List<A>, A, Runnable> accumulator() {
                        return (list, a) -> { list.add(a); return () -> list.remove(a); };
                    }
                    public Function<List<A>, List<A>> finisher() { return list -> list; }
                }
                static <A, C, R> Collector<A, C, R> logged(Collector<A, C, R> collector) { return collector; }
                static <A> Collector<A, ?, List<A>> toList() { return new ToList<>(); }
                static <A> Collector<A, ?, List<A>> loggedList() { return logged(toList()); }
                static <A, C, R> Collector<A, ?, R> plain(Collector<A, C, R> collector) { return collector; }
                static final class Distinct<A, M> implements Collector<A, Set<M>, Integer> {
                    Distinct(Function<? super A, ? extends M> mapper) { }
                    public Supplier<Set<M>> supplier() { return HashSet::new; }
                    public BiFunction<Set<M>, A, Runnable> accumulator() { return (set, a) -> () -> { }; }
                    public Function<Set<M>, Integer> finisher() { return Set::size; }
                }
                static <A, M> Collector<A, ?, Integer> distinct(Function<? super A, ? extends M> mapper) {
                    return new Distinct<>(mapper);
                }
                interface TriFunction<A, B, C, R> { R apply(A a, B b, C c); }
                static final class Concat implements TriFunction<String, String, String, String> {
                    public String apply(String a, String b, String c) { return a + b + c; }
                }
                interface BiCollector<A, B, ResultContainer_, Result_> {
                    Supplier<ResultContainer_> supplier();
                    TriFunction<ResultContainer_, A, B, Runnable> accumulator();
                }
                static final class Count<A, B> implements BiCollector<A, B, int[], Integer> {
                    public Supplier<int[]> supplier() { return () -> new int[1]; }
                    public TriFunction<int[], A, B, Runnable> accumulator() {
                        return (c, a, b) -> { c[0]++; return () -> c[0]--; };
                    }
                }
                static <A, B, C, R> BiCollector<A, B, C, R> loggedBi(BiCollector<A, B, C, R> collector) { return collector; }
                static <A, B> BiCollector<A, B, ?, Integer> count() { return new Count<>(); }
                static <A, B> BiCollector<A, B, ?, Integer> loggedCount() { return loggedBi(count()); }
            }
            """;

    /**
     * A wildcard of a type that the code also passes where a generic method captures its type parameter, which a
     * wildcard interface would not let C# infer: it is the type of object, which a concrete one is adapted to by the
     * interface's erasure adapter.
     */
    @Test
    public void erasures() {
        String cs = translate("Collectors", ERASURES);
        contains(cs, "static ICollector<A, object, Result_> Erased_(ICollector<A, ResultContainer_, Result_> erased) => erased as ICollector<A, object, Result_> ?? new Erasure_(erased);");
        contains(cs, "private sealed class Erasure_ : ICollector<A, object, Result_> {");
        contains(cs, "public Func<object, A, Action> Accumulator() { var f = erased.Accumulator(); return (x0_, x1_) => f((ResultContainer_) x0_, x1_); }");
        contains(cs, "=> ICollector<A, List<A>, List<A>>.Erased_(new ToList<A>());");
        contains(cs, "=> Logged(toList<A>());");
        contains(cs, "=> ICollector<A, C, R>.Erased_(collector);");
        // a diamond's inferred ? extends M is its bound
        contains(cs, "=> ICollector<A, ISet<M>, int>.Erased_(\nnew Distinct<A, M>(mapper));");
        // a functional interface with its lambda adapter
        contains(cs, "public ITriFunction<object, A, B, Action> Accumulator() { var f = erased.Accumulator(); return new ITriFunction<object, A, B, Action>.Lambda((x0_, x1_, x2_) => f.Apply((ResultContainer_) x0_, x1_, x2_)); }");
        contains(cs, "=> IBiCollector<A, B, int[], int>.Erased_(\nnew Count<A, B>());");
    }

    @Language("java")
    private static final String COVARIANT_DELEGATES = """
            package org.example.accumulators;
            import java.util.function.BiFunction;
            class Accumulators {
                interface Accumulator<C, A> extends BiFunction<C, A, Runnable> {
                    Runnable into(C container);
                    @Override
                    default Runnable apply(C container, A a) { return into(container); }
                }
                interface Collector<A, C> {
                    BiFunction<C, A, Runnable> accumulator();
                }
                static final class Counting<A> implements Collector<A, int[]> {
                    public Accumulator<int[], A> accumulator() { return c -> () -> c[0]++; }
                }
            }
            """;

    /**
     * An override that returns a program interface extending a library's functional interface, which C# has as a
     * delegate the interface cannot extend: the interface's method returns the override's method group.
     */
    @Test
    public void covariantDelegates() {
        String cs = translate("Accumulators", COVARIANT_DELEGATES);
        contains(cs, "Func<int[], A, Action> ICollector<A, int[]>.Accumulator() => Accumulator().Apply;");
    }

    @Language("java")
    private static final String PROPERTY_REFERENCES = """
            package org.example.refs;
            import java.util.Comparator;
            import java.util.List;
            import java.util.function.Supplier;
            class Refs {
                record Location(String className, int lineNumber) { }
                static List<String> names(List<Location> locations) {
                    return locations.stream().map(Location::className).toList();
                }
                static final Comparator<Location> ORDER =
                        Comparator.comparing(Location::className).thenComparingInt(Location::lineNumber);
                static Supplier<String> bound(Location location) { return location::className; }
            }
            """;

    /** A method reference to a member C# has as a property, a record's component: a lambda reading it. */
    @Test
    public void propertyReferences() {
        String cs = translate("Refs", PROPERTY_REFERENCES);
        contains(cs, ".Select((Location p0) => p0.ClassName)");
        contains(cs, "() => location.ClassName");
    }

    @Language("java")
    private static final String SWITCHES = """
            package org.example.flow;
            class Switches {
                static int fallThrough(int k) {
                    int r = 0;
                    switch (k) {
                        case 1:
                            r += 1;
                        case 2:
                            r += 2;
                            break;
                        case 3:
                            r = 3;
                        default:
                            r--;
                    }
                    return r;
                }
                static String arrows(int k) {
                    switch (k) {
                        case 1, 2 -> { return "small"; }
                        case 3 -> System.out.println("three");
                        default -> { }
                    }
                    return "large";
                }
                static String patterns(Object o) {
                    switch (o) {
                        case String s -> { return s; }
                        case Integer i when i > 0 -> System.out.println(i);
                        default -> { }
                    }
                    return "";
                }
                static int expression(String s) {
                    return switch (s) {
                        case "a", "b" -> 1;
                        case "c" -> {
                            int x = 2;
                            yield x * 2;
                        }
                        default -> throw new IllegalArgumentException(s);
                    };
                }
            }
            """;

    @Test
    public void switches() {
        String cs = translate("Switches", SWITCHES);
        contains(cs, "r += 1;\ngoto case 2;");
        contains(cs, "r = 3;\ngoto default;");
        contains(cs, "default:\nr--;\nbreak;");
        contains(cs, "case 1: case 2: return \"small\";");
        // an arrow arm's type pattern
        contains(cs, "case string s: return s;");
        contains(cs, "case int i when (int) i > 0:");
        contains(cs, "\"a\" or \"b\" => 1,");
        contains(cs, "_ => throw new ArgumentException(s)");
    }

    @Language("java")
    private static final String LOOPS = """
            package org.example.flow;
            class Loops {
                static int search(int[][] grid, int target) {
                    int found = -1;
                    outer:
                    for (int i = 0; i < grid.length; i++) {
                        for (int j = 0; j < grid[i].length; j++) {
                            if (grid[i][j] < 0) continue outer;
                            if (grid[i][j] == target) { found = i; break outer; }
                        }
                    }
                    return found;
                }
                static long sum(int... values) {
                    long total = 0;
                    for (int v : values) total += v;
                    int k = 0;
                    do { k++; } while (k < 3);
                    while (k > 0) k--;
                    return total;
                }
            }
            """;

    @Test
    public void loops() {
        String cs = translate("Loops", LOOPS);
        contains(cs, "if (grid[i][j] < 0) { goto outer_continue; }");
        contains(cs, "goto outer_break;");
        contains(cs, "outer_continue: ;");
        contains(cs, "outer_break: ;");
        contains(cs, "for (int i = 0; i < grid.Length; i++) {");
        contains(cs, "internal static long Sum(params int[] values) {");
        contains(cs, "foreach (int v in values) { total += v; }");
    }

    @Language("java")
    private static final String CONDITIONAL_VARS = """
            package org.example.nodes;
            public class Nodes {
                static abstract class Node<A> { abstract int size(); }
                static final class Indexed<A> extends Node<A> { int size() { return 1; } }
                static final class Unindexed<A> extends Node<A> { int size() { return 0; } }
                static <A> int build(boolean joiners) {
                    var node = joiners ? new Indexed<A>() : new Unindexed<A>();
                    return node.size();
                }
                static String describe(Object bean) {
                    var beanClass = bean == null ? "null" : bean.getClass();
                    return "" + beanClass;
                }
                static int same(boolean b) {
                    var n = b ? new Indexed<String>() : new Indexed<String>();
                    return n.size();
                }
                static abstract class Exists<T, R> { abstract int size(); }
                static final class IndexedExists<T, R> extends Exists<T, R> { int size() { return 1; } }
                static final class UnindexedExists<T, R> extends Exists<T, R> { int size() { return 0; } }
                static <A, B> int exists(boolean joiners) {
                    var node = joiners ? new IndexedExists<A, B>() : new UnindexedExists<A, String>();
                    return node.size();
                }
                interface Named { String name(); }
                interface Sized { int size(); }
                static final class Genuine implements Named, Sized {
                    public String name() { return "g"; } public int size() { return 1; }
                }
                static final class Shadow implements Named, Sized {
                    public String name() { return "s"; } public int size() { return 0; }
                }
                static String metaModel(boolean genuine) {
                    var model = genuine ? new Genuine() : new Shadow();
                    return model.name();
                }
            }
            """;

    /** Java's var of a conditional is its branches' common supertype; C#'s var needs one of them: declared. */
    @Test
    public void conditionalVars() {
        String cs = translate("Nodes", CONDITIONAL_VARS);
        contains(cs, "Node<A> node = joiners ? new Indexed<A>() : new Unindexed<A>();");
        contains(cs, "object beanClass = bean == null ? \"null\" : bean.GetType();");
        contains(cs, "var n = b ? new Indexed<string>() : new Indexed<string>();");
        // not when C# cannot write Java's common type: with a wildcard, or an intersection of the program's types
        contains(cs, "var node = joiners ? new IndexedExists<A, B>() : new UnindexedExists<A, string>();");
        contains(cs, "var model = genuine ? new Genuine() : new Shadow();");
    }

    @Language("java")
    private static final String TWO_ITERABLES = """
            package org.example.iter;
            import java.util.Iterator;
            import java.util.List;
            public class Selectors {
                interface Selector<T> extends Iterable<T> { long size(); }
                interface ListIterable<T> extends Iterable<T> { List<T> list(); }
                static class FromList<T> implements Selector<T>, ListIterable<T> {
                    private final List<T> items;
                    FromList(List<T> items) { this.items = items; }
                    public long size() { return items.size(); }
                    public List<T> list() { return items; }
                    public Iterator<T> iterator() { return items.iterator(); }
                }
                static final class Cached<T> extends FromList<T> {
                    Cached(List<T> items) { super(items); }
                }
                interface EntitySelector<T> extends Selector<T>, ListIterable<T> { }
                static abstract class Caching<T> implements EntitySelector<T> {
                    public long size() { return 0; }
                }
                static <T> T first(EntitySelector<T> selector) { return selector.iterator().next(); }
            }
            """;

    /**
     * Two interfaces that are iterable each have default GetEnumerators; a class implementing both declares its own,
     * which C# finds most specific (CS8705), and its subclasses inherit it.
     */
    @Test
    public void twoIterables() {
        String cs = translate("Selectors", TWO_ITERABLES);
        contains(cs, "internal class FromList<T> : ISelector<T>, IListIterable<T> {");
        contains(cs, "public IEnumerator<T> GetEnumerator() => Iterator().AsEnumerator();");
        contains(cs, "internal sealed class Cached<T> : FromList<T> { internal Cached(List<T> items) : base(items) { } }");
        // through an interface that extends both, which has no defaults of its own
        contains(cs, "internal abstract class Caching<T> : IEntitySelector<T> {");
        // a call through the interface that extends both: its own Iterator(), hiding theirs (CS0121)
        contains(cs, "interface IEntitySelector<T> : ISelector<T>, IListIterable<T> { new IJavaIterator<T> Iterator(); }");
        contains(cs, "public IEnumerator<T> GetEnumerator() => Iterator().AsEnumerator();\n    System.Collections");
    }

    @Language("java")
    private static final String ABSTRACT_ITERATORS = """
            package org.example.iterators;
            import java.util.Iterator;
            public class Iterators {
                static abstract class SelectionIterator<S> implements Iterator<S> {
                    @Override public void remove() { throw new UnsupportedOperationException(); }
                }
                static final class Countdown extends SelectionIterator<Integer> {
                    private int left = 3;
                    @Override public boolean hasNext() { return left > 0; }
                    @Override public Integer next() { return left--; }
                }
            }
            """;

    /** An abstract class leaves a compatibility interface's methods, IJavaIterator's, to its subclasses: abstract. */
    @Test
    public void abstractIterators() {
        String cs = translate("Iterators", ABSTRACT_ITERATORS);
        contains(cs, "public abstract bool HasNext();");
        contains(cs, "public abstract S Next();");
        // the override of S Next() in a SelectionIterator<int> is the value type
        contains(cs, "public override int Next() => left--;");
    }

    @Language("java")
    private static final String MARKERS = """
            package org.example.events;
            import java.util.ArrayList;
            import java.util.EventListener;
            import java.util.List;
            public class Events {
                interface PhaseListener extends EventListener { void phaseStarted(); }
                static abstract class Support<E extends EventListener> {
                    protected final List<E> listeners = new ArrayList<>();
                    void add(E listener) { listeners.add(listener); }
                }
                static final class PhaseSupport extends Support<PhaseListener> {
                    void fire() { listeners.forEach(PhaseListener::phaseStarted); }
                }
            }
            """;

    /** A marker interface, EventListener, is no C# type: left out of the bases and of the constraints. */
    @Test
    public void markers() {
        String cs = translate("Events", MARKERS);
        contains(cs, "internal interface IPhaseListener {");
        contains(cs, "internal abstract class Support<E> {");
    }

    @Language("java")
    private static final String STATIC_SCOPES = """
            package org.example.scopes;
            public abstract class Move<T> {
                protected static int size(Object o) { return 1; }
                abstract T value();
                public record Id(long key, Kind kind) {
                    public enum Kind { NODE, INPUT }
                    Id(long key) { this(key, Kind.NODE); }
                }
            }
            """;

    /**
     * A generic class's protected static is its companion's, a static class, which has no protected members: internal.
     * A positional record's parameter list does not see the record's nested types: qualified.
     */
    @Test
    public void staticScopes() {
        String cs = translate("Move", STATIC_SCOPES);
        contains(cs, "public static class Move { internal static int Size(object o) => 1; }");
        contains(cs, "public sealed record Id(long Key, Id.Kind? kind) {");
    }

    @Language("java")
    private static final String EXPRESSIONS = """
            package org.example.expr;
            class Expressions {
                interface Op { int apply(int a, int b); }
                static int twice(Op op, int x) { return op.apply(x, x); }
                static int add(int a, int b) { return a + b; }
                static boolean same(String a, String b) { return a == b; }
                static String kind(Object o) {
                    if (o instanceof String s && !s.isEmpty()) return s;
                    if (!(o instanceof Integer)) return "other";
                    return o == null ? "null" : "int";
                }
                static int bits(int x) { return (x >>> 3) ^ ~x & 0xff; }
                static Class<?> type() { return Expressions.class; }
                static int[] numbers() { return new int[] { 1, 2, 3 }; }
                static long widen(int i) { return (long) i << 2; }
                static char letter() { return '\\''; }
                static final class Box<T> {
                    T value;
                    boolean holds(T other) { return value == other; }
                    boolean lacks(T other) { return !(value == other); }
                    boolean empty() { return value == null; }
                    @Override public boolean equals(Object o) { return o instanceof Box<?> b && b.value == value; }
                    @Override public int hashCode() { return 1; }
                }
            }
            """;

    @Test
    public void expressions() {
        String cs = translate("Expressions", EXPRESSIONS);
        contains(cs, "internal static bool Same(string a, string b) => object.ReferenceEquals(a, b);");
        contains(cs, "if (o is string s && !(s.Length == 0)) { return s; }");
        contains(cs, "if (o is not int) { return \"other\"; }");
        contains(cs, "x >>> 3");
        contains(cs, "typeof(Expressions)");
        contains(cs, "new int[] { 1, 2, 3 }");
        contains(cs, "'\\''");
        // == on a type parameter's value is Java's identity, which C#'s == does not allow
        contains(cs, "internal bool Holds(T other) => object.ReferenceEquals(value, other);");
        contains(cs, "internal bool Empty() => value == null;");
        // a wildcard test of the class itself, in its equals: its own type parameters, not object
        contains(cs, "public override bool Equals(object o) => o is Box<T> b && object.ReferenceEquals(b.value, value);");
    }

    @Language("java")
    private static final String TRY = """
            package org.example.io;
            class Resources {
                static class Res implements AutoCloseable {
                    public void close() { }
                    int read() { return 1; }
                }
                static int one() throws Exception {
                    try (Res a = new Res(); Res b = new Res()) {
                        return a.read() + b.read();
                    }
                }
                static int caught() {
                    try (Res r = new Res()) {
                        return r.read();
                    } catch (IllegalStateException | UnsupportedOperationException e) {
                        return -1;
                    } finally {
                        System.gc();
                    }
                }
            }
            """;

    @Test
    public void tryWithResources() {
        String cs = translate("Resources", TRY);
        contains(cs, "using (var a = new Res())");
        contains(cs, "using (var b = new Res())");
        contains(cs, "catch (Exception e) when (e is InvalidOperationException || e is NotSupportedException) {");
    }

    @Language("java")
    private static final String UTIL = """
            package org.example.util;
            public final class Strings {
                private Strings() { }
                public static final String EMPTY = "";
                public static <T extends Comparable<T>> T max(T a, T b) { return a.compareTo(b) >= 0 ? a : b; }
            }
            """;

    @Test
    public void staticClass() {
        String cs = translate("Strings", UTIL);
        contains(cs, "public static class Strings {");
        contains(cs, "public static T Max<T>(T a, T b) where T : IComparable<T> => a.CompareTo(b) >= 0 ? a : b;");
    }

    @Language("java")
    private static final String RECORD = """
            package org.example.geo;
            record Point(int x, int y) {
                Point scale(int f) { return new Point(x * f, y() * f); }
                static Point origin() { return new Point(0, 0); }
                @Override
                public boolean equals(Object o) { return o instanceof Point(int px, var py) && px == x && py == y; }
                @Override
                public int hashCode() { return 31 * x + y; }
            }
            record Line(Point from, Point to) {
                static int dx(Object o) {
                    return switch (o) {
                        case Line(Point(var x1, var y1), Point(int x2, int y2)) -> x2 - x1;
                        default -> 0;
                    };
                }
                static boolean vertical(Object o) {
                    if (o instanceof Line(Point(var x1, var y1), Point to) && to.x() == x1) return true;
                    return false;
                }
            }
            record Id(String name, int n) {
                @Override
                public boolean equals(Object o) { return o instanceof Id id && id.name.equals(name); }
            }
            """;

    /**
     * A record is a positional record. Its equals(Object) is C#'s Equals(R other), as a record synthesizes
     * Equals(object), and its hashCode stays the components' where it declares none; a record pattern is a property
     * pattern.
     */
    @Test
    public void record() {
        String cs = translate("Point", RECORD);
        contains(cs, "internal sealed record Point(int X, int Y) {");
        contains(cs, "internal Point Scale(int f) => new Point(X * f, Y * f);");
        contains(cs, "public bool Equals(Point o) => o is Point { X: var px, Y: var py } && px == X && py == Y;");
        contains(cs, "public override int GetHashCode()");
        contains(cs, "Line { From: Point { X: var x1, Y: var y1 }, To: Point { X: var x2, Y: var y2 } }");
        contains(cs, "o is Line { From: Point { X: var x1, Y: var y1 }, To: var to } && to.X == x1");
        contains(cs, "public override int GetHashCode() => HashCode.Combine(Name, N);");
    }

    @Language("java")
    private static final String LAMBDAS = """
            package org.example.fn;
            import java.util.function.Function;
            import java.util.function.Supplier;
            class Lambdas {
                static int len(String s) { return 1; }
                static Function<String, Integer> lengths() { return Lambdas::len; }
                static Function<Integer, Integer> inc() { return x -> x + 1; }
                static Supplier<StringBuilder> fresh() { return StringBuilder::new; }
                static Function<Integer, Integer> block() {
                    return x -> {
                        int y = x * 2;
                        return y + 1;
                    };
                }
            }
            """;

    @Test
    public void lambdas() {
        String cs = translate("Lambdas", LAMBDAS);
        contains(cs, "internal static Func<string, int> Lengths() => Lambdas.Len;");
        contains(cs, "internal static Func<int, int> Inc() => x => x + 1;");
        contains(cs, "() => new StringBuilder()");
    }

    @Language("java")
    private static final String ACCESS = """
            package org.example.access;
            import java.util.List;
            class Outer {
                private int secret;
                private static class Helper {
                    private int used;
                    private int unused;
                    private Helper() { }
                    private int peek(Outer o) { return o.secret + unused; }
                }
                int read() { return new Helper().used; }
                static class Key<T extends Number> { }
                Key raw() { return null; }
                static class Base { protected void m() { } }
                static class Derived extends Base { @Override public void m() { } }
            }
            """;

    /**
     * Java's private reaches across the nested types of one top-level type; C#'s does not. What the outside reaches
     * is internal, the rest stays private. An override keeps the access of what it overrides: C# may not widen it.
     */
    @Test
    public void access() {
        String cs = translate("Access", ACCESS);
        contains(cs, "private int secret;");
        contains(cs, "internal int used;");
        contains(cs, "private int unused;");
        contains(cs, "internal Helper() { }");
        contains(cs, "private int Peek(Outer o) => o.secret + unused;");
        contains(cs, "internal Key<IConvertible> Raw() => null;");
        contains(cs, "protected internal override void M() { }");
    }

    @Language("java")
    private static final String SCOPES = """
            package org.example.scope;
            class Scopes {
                static final int[][] TABLE = { null, { 1, 2 }, {} };
                static int later(int[] xs) {
                    for (int k = 0; k < xs.length; k++) {
                        int last = xs[k];
                        if (last > 0) return last;
                    }
                    int last = -1;
                    return last;
                }
                static String twice(Object o, Object p) {
                    if (o instanceof String s) return s;
                    if (p instanceof String s) return s + s;
                    return "";
                }
            }
            """;

    /**
     * C# scopes a local to its whole block, and a pattern variable of an if condition to the enclosing block: names
     * Java can reuse clash in C#, and are renamed. A jagged array's nested initializers are array creations.
     */
    @Test
    public void scopes() {
        String cs = translate("Scopes", SCOPES);
        contains(cs, "internal static readonly int[][] TABLE = { null, new int[] { 1, 2 }, new int[] { } };");
        contains(cs, "int last2 = xs[k];\nif (last2 > 0) { return last2; }");
        contains(cs, "int last = -1;\nreturn last;");
        contains(cs, "if (o is string s) { return s; }");
        contains(cs, "if (p is string s2) { return s2 + s2; }");
    }

    @Language("java")
    private static final String CONVERSIONS = """
            package org.example.conv;
            import java.util.HashMap;
            import java.util.Map;
            import java.util.Set;
            class Factory<E> {
                private int size() { return 3; }
                static class Item<E> {
                    final Set<E>[] buckets = new Set[2];
                    int n(Factory<E> f) { return f.size(); }
                }
                static <T> T none() { return null; }
                static String escape(char c) {
                    switch (c) {
                        case 0x8: return "\\b";
                        default: return "";
                    }
                }
                static int orZero(Integer i, Integer count) { return i == null ? 0 : i + count; }
                static int index(Map<String, Integer> map, String key, Integer boxed) {
                    Map<Integer, String> names = new HashMap<>();
                    String name = names.get(boxed);
                    int i = map.get(key);
                    if ((Boolean) (Object) Boolean.TRUE) return boxed;
                    return i + name.length();
                }
            }
            """;

    /**
     * A static nested type of a generic type is printed beside it (C# would make it generic in the outer type's
     * parameters); the outer type's private members it uses become internal. C# does not unbox: an Integer where an
     * int is expected, or as a key, gets a cast. Java's null of a type parameter is default; a raw array creation
     * takes the declared type's arguments.
     */
    @Test
    public void conversions() {
        String cs = translate("Factory", CONVERSIONS);
        contains(cs, "internal sealed class Item<E> {");
        contains(cs, "internal int Size() => 3;");
        contains(cs, "internal readonly ISet<E>[] buckets = new ISet<E>[2];");
        contains(cs, "internal static T None<T>() => default;");
        contains(cs, "string name = names.Get((int) boxed);");
        contains(cs, "int i = (int) map.GetValueOrNull(key);");
        // known-ugly: Java's own (Boolean) (Object) cast chain, kept as it is
        contains(cs, "if ((bool) ((object) true)) { return (int) boxed; }");
        contains(cs, "case '\\b': return");
        contains(cs, "internal static int OrZero(int? i, int? count) => i == null ? 0 : (int) i + (int) count;");
    }

    @Language("java")
    private static final String LOCAL_CLASSES = """
            package org.example.local;
            import java.util.ArrayList;
            import java.util.List;
            class Locals {
                private int base = 1;
                int count(int n) {
                    class Entry {
                        final int value;
                        Entry(int value) { this.value = value; }
                    }
                    List<Entry> entries = new ArrayList<>();
                    for (int i = 0; i < n; i++) entries.add(new Entry(i));
                    return entries.size();
                }
                int captures(int n) {
                    class Adder {
                        int add(int x) { return x + n + base; }
                    }
                    return new Adder().add(1);
                }
            }
            """;

    /** A local class that captures nothing is lifted into its enclosing type; one that captures is not translated. */
    @Test
    public void localClasses() {
        String cs = translate("Locals", LOCAL_CLASSES);
        contains(cs, "private sealed class Entry {");
        contains(cs, "entries.Add(new Entry(i));");
        assertFalse(cs.contains("class Adder"), cs);
    }

    @Language("java")
    private static final String FUNCTIONAL = """
            package org.example.fun;
            import java.util.ArrayList;
            import java.util.List;
            class Graph {
                interface Visitor {
                    int visit(String node);
                }
                interface Listener {
                    void changed(String node);
                }
                static class Recorder implements Listener {
                    final List<String> seen = new ArrayList<>();
                    public void changed(String node) { seen.add(node); }
                }
                static int walk(Visitor v) { return v.visit("a"); }
                static int lengths() { return walk(s -> s.length()) + walk(Graph::one); }
                static int counted() {
                    return walk(new Visitor() {
                        @Override
                        public int visit(String node) { return 2; }
                    });
                }
                static Visitor bound(Visitor v) { return v::visit; }
                static int one(String s) { return 1; }
                static void tell(Listener l) { l.changed("b"); }
                static void quiet() { tell(n -> { }); }
            }
            """;

    /**
     * A translated functional interface that nothing in the program implements is a C# delegate; one that a class
     * implements stays an interface, with an adapter class for its lambdas.
     */
    @Test
    public void functionalInterface() {
        String cs = translate("Graph", FUNCTIONAL);
        contains(cs, "internal delegate int Visitor(string node);");
        contains(cs, "internal static int Walk(Visitor v) => v(\"a\");");
        contains(cs, "internal static int Lengths() => Walk(s => s.Length) + Walk(Graph.One);");
        contains(cs, "internal static int Counted() => Walk((string node) => 2);");
        contains(cs, "internal static Visitor Bound(Visitor v) => v.Invoke;");
        contains(cs, "public sealed class Lambda(Action<string> f) : IListener { public void Changed(string node) => f(node); }");
        contains(cs, "internal static void Tell(IListener l) { l.Changed(\"b\"); }");
        contains(cs, "internal static void Quiet() { Tell(new IListener.Lambda(n => { })); }");
    }

    @Language("java")
    private static final String ANONYMOUS = """
            package org.example.anon;
            import java.util.ArrayList;
            import java.util.List;
            class Engine {
                interface Graph {
                    List<String> nodes();
                    String first();
                }
                abstract static class Counter {
                    final int start;
                    Counter(int start) { this.start = start; }
                    abstract int next();
                }
                private final String prefix = "n";
                int size(Graph g) { return g.nodes().size(); }
                int run(int count) {
                    List<String> all = new ArrayList<>();
                    Graph g = new Graph() {
                        @Override
                        public List<String> nodes() { return all; }
                        @Override
                        public String first() { return prefix + count; }
                    };
                    Counter c = new Counter(count) {
                        private int i = start;
                        @Override
                        int next() { return i++; }
                    };
                    return size(g) + c.next();
                }
            }
            """;

    /**
     * An anonymous class that does not become a lambda is hoisted into a private nested class: what it captures is
     * passed to its constructor, the enclosing instance as {@code outer}, and its fields are initialised there.
     */
    @Test
    public void anonymousClasses() {
        String cs = translate("Engine", ANONYMOUS);
        contains(cs, "IGraph g = new GraphImpl(this, all, count);");
        contains(cs, "Counter c = new CounterImpl(count);");
        contains(cs, "private sealed class GraphImpl : IGraph {");
        contains(cs, "internal GraphImpl(Engine outer, List<string> all, int count) {");
        contains(cs, "public string First() => outer.prefix + count;");
        contains(cs, "private sealed class CounterImpl : Counter {");
        contains(cs, "internal CounterImpl(int p0) : base(p0) { this.i = start; }");
    }

    @Language("java")
    private static final String IDIOMS = """
            package org.example.idiom;
            import java.util.List;
            class Node implements Cloneable {
                int value;
                @Override
                public Node clone() {
                    try {
                        return (Node) super.clone();
                    } catch (CloneNotSupportedException e) {
                        throw new RuntimeException(e);
                    }
                }
                static int sum(List<Integer> values) {
                    int total = 0;
                    for (Integer v : values) {
                        if (v == null) v = 0;
                        total += v;
                    }
                    return total;
                }
            }
            """;

    /**
     * C#'s object has no clone to override: Java's clone is a method of its own, super.clone() is MemberwiseClone.
     * C#'s iteration variable is read-only: a loop that assigns it works on a copy.
     */
    @Test
    public void idioms() {
        String cs = translate("Node", IDIOMS);
        contains(cs, "internal sealed class Node {");
        contains(cs, "public Node Clone() {");
        contains(cs, "return (Node) base.MemberwiseClone();");
        contains(cs, "foreach (int? vItem in values) {\nint? v = vItem;");
    }

    @Language("java")
    private static final String EXPOSED = """
            package org.example.exposed;
            import java.util.ArrayList;
            import java.util.List;
            public class Result {
                private static class Pair {
                    final int a;
                    Pair(int a) { this.a = a; }
                }
                private static class Hidden {
                }
                private final List<Pair> pairs = new ArrayList<>();
                public List<Pair> pairs() { return pairs; }
                Hidden hidden() { return new Hidden(); }
            }
            """;

    /**
     * Java's public method may name a less accessible type, C#'s may not: the type becomes as visible as the member
     * that exposes it, no more than the member's own type.
     */
    @Test
    public void exposedTypes() {
        String cs = translate("Result", EXPOSED);
        contains(cs, "public sealed class Pair {");
        contains(cs, "internal sealed class Hidden {");
    }

    @Language("java")
    private static final String SUPPLIERS = """
            package org.example.supply;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.Optional;
            class Defaults<R extends Defaults<R>> {
                static List<String> names(List<String> given) { return Optional.ofNullable(given).orElseGet(List::of); }
                static List<String> copy(List<String> given) { return Optional.ofNullable(given).orElseGet(ArrayList::new); }
                static String name(String given) { return Optional.ofNullable(given).orElseGet(() -> "none"); }
                static Defaults raw() { return null; }
            }
            """;

    /**
     * C# cannot call a lambda where it is written: a supplier argument that a template calls is written in place.
     * An F-bounded type used raw is its companion, which no type argument has to satisfy (see {@link #companions}).
     */
    @Test
    public void suppliers() {
        String cs = translate("Defaults", SUPPLIERS);
        contains(cs, "internal static List<string> Names(List<string> given) => given ?? new List<string>();");
        contains(cs, "internal static List<string> Copy(List<string> given) => given ?? new List<string>();");
        contains(cs, "internal static string Name(string given) => given ?? \"none\";");
        contains(cs, "internal static IDefaults Raw() => null;");
    }

    @Language("java")
    private static final String NAMESPACES = """
            package org.example.exception.query;
            class Query {
                static void check(String s) { if (s == null) throw new IllegalStateException("no " + s); }
            }
            """;

    /**
     * A namespace segment with the name of a type, of the program or of the BCL, is plural: the namespace
     * {@code Org.Example.Exception} would hide {@code System.Exception}.
     */
    @Test
    public void namespaces() {
        String cs = translate("Query", NAMESPACES);
        contains(cs, "namespace Org.Example.Exceptions.Queries;");
    }

    @Language("java")
    private static final String LANGUAGE = """
            package org.example.lang;
            import java.util.HashMap;
            import java.util.Map;
            class Metadata {
                interface Result {
                    boolean success();
                    default boolean failed() { return !success(); }
                }
                interface Listener<T> {
                    void onEvent(T event);
                }
                interface StartListener extends Listener<String> {
                }
                static class Ok implements Result {
                    public boolean success() { return true; }
                    boolean check() { return failed(); }
                }
                abstract static class Builder<T extends Builder<T>> {
                    String name;
                    T name(String name) { this.name = name; return (T) this; }
                }
                private final Map<String, Object> metadata = new HashMap<>();
                static Metadata metadata(String key, Object value) { return new Metadata(); }
                Object get(String key) { return metadata.get(key); }
            }
            """;

    /**
     * A C# class does not inherit its interfaces' default methods: a call goes through the interface. A cast to a type
     * parameter goes through object. A method named as its class is renamed. An adapter substitutes the arguments a
     * functional interface gives its generic super-interface.
     */
    @Test
    public void language() {
        String cs = translate("Metadata", LANGUAGE);
        contains(cs, "internal bool Check() => ((IResult) this).Failed();");
        contains(cs, "return (T) (object) this;");
        contains(cs, "internal static Metadata Of(string key, object value) => new Metadata();");
        contains(cs, "public sealed class Lambda(Action<string> f) : IStartListener { public void OnEvent(string @event) => f(@event); }");
    }

    @Language("java")
    private static final String ANNOTATIONS = """
            package org.example.attr;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import java.lang.reflect.Method;
            class Tools {
                @Retention(RetentionPolicy.RUNTIME)
                @Target({ElementType.METHOD})
                @interface Tool {
                    String name() default "";
                    String[] value() default "";
                    boolean required() default true;
                    Class<? extends Ranker> ranker() default NullRanker.class;
                    interface Ranker { int rank(Method m); }
                    interface NullRanker extends Ranker { }
                }
                static boolean ranked(Tool tool) { return tool.ranker() != Tool.NullRanker.class; }
                @Tool(value = "Adds", name = "add")
                int add(int a, int b) { return a + b; }
                @Deprecated
                int old() { return 0; }
                static String nameOf(Tool tool) { return tool.name(); }
            }
            """;

    /**
     * An annotation type is an attribute class with properties and their defaults; a use is an attribute, its value
     * element positional; reading an element reads the property.
     */
    @Test
    public void annotations() {
        String cs = translate("Tools", ANNOTATIONS);
        contains(cs, "[AttributeUsage(AttributeTargets.Method)]");
        contains(cs, "internal sealed class ToolAttribute : Attribute {");
        contains(cs, "public ToolAttribute(params string[] value) { Value = value; }");
        contains(cs, "public string Name { get; set; } = \"\";");
        contains(cs, "public string[] Value { get; set; } = new string[] { \"\" };");
        contains(cs, "public bool Required { get; set; } = true;");
        contains(cs, "[Tool(\"Adds\", Name = \"add\")]");
        contains(cs, "[Obsolete]");
        contains(cs, "internal static string NameOf(ToolAttribute tool) => tool.Name;");
        // an annotation's nested types are the attribute's
        contains(cs, "public Type Ranker { get; set; } = typeof(Tools.ToolAttribute.INullRanker);");
        contains(cs, "public interface INullRanker : Tools.ToolAttribute.IRanker {");
    }

    @Language("java")
    private static final String RECORDS = """
            package org.example.rec;
            interface Result<T> {
                T response();
            }
            record Success<T>(T response, String note) implements Result<T> {
                public Success {
                    if (response == null) throw new IllegalArgumentException("response");
                    note = note == null ? "" : note.trim();
                }
            }
            """;

    /**
     * A record with a compact constructor is a record with get-only properties and that constructor, which assigns
     * them after its body (the CST's compact constructor does). An accessor implementing an interface method implements it explicitly.
     */
    @Test
    public void records() {
        String cs = translate("Result", RECORDS);
        contains(cs, "internal sealed record Success<T> : IResult<T> {");
        contains(cs, "public T Response { get; }");
        contains(cs, "note = note == null ? \"\" : note.Trim();\nResponse = response;\nNote = note;");
        contains(cs, "T IResult<T>.Response() => Response;");
    }

    @Language("java")
    private static final String UNBOUND = """
            package org.example.unbound;
            import java.util.Collection;
            import java.util.List;
            class Checks {
                static <T extends Collection<?>> T ensureNotEmpty(T c) { if (c.isEmpty()) throw new IllegalArgumentException(); return c; }
                static int size(List<?> list) { return list.size(); }
                static List<String> names(List<String> names) { return ensureNotEmpty(names); }
            }
            """;

    /** A collection of {@code ?} is C#'s non-generic interface, which every generic collection implements. */
    @Test
    public void unboundWildcards() {
        String cs = translate("Checks", UNBOUND);
        contains(cs, "internal static T EnsureNotEmpty<T>(T c) where T : ICollection {");
        contains(cs, "internal static int Size(IList list) => list.Count;");
    }

    @Language("java")
    private static final String INTERFACES = """
            package org.example.interfaces;
            interface Listener<E> {
                void onEvent(E event);
                Class<E> eventClass();
            }
            interface StringListener extends Listener<String> {
                default Class<String> eventClass() { return String.class; }
            }
            interface Failure {
                Failure withCode(int code);
            }
            interface Model {
                int dimension();
                String embed(String text);
            }
            abstract class AbstractModel implements Model {
                public int dimension() { return embed("test").length(); }
            }
            final class UpperModel extends AbstractModel {
                public String embed(String text) { return text.toUpperCase(); }
            }
            interface Stream {
                String getSemantics();
                boolean distinct();
                int size();
            }
            abstract class BaseStream {
                private final String semantics;
                BaseStream(String semantics) { this.semantics = semantics; }
                public String getSemantics() { return semantics; }
                public boolean distinct() { return false; }
            }
            abstract class MiddleStream extends BaseStream implements Stream {
                MiddleStream() { super("all"); }
            }
            final class LeafStream extends MiddleStream {
                public int size() { return 0; }
            }
            class Holder {
                static final class SimpleFailure implements Failure {
                    public SimpleFailure withCode(int code) { return this; }
                }
            }
            """;

    /**
     * C#'s interface members implement others only explicitly, and an abstract class declares what it leaves. A
     * superclass's method implements a subclass's interface in C# as in Java: it stays a method, and the abstract
     * class in between does not declare it again.
     */
    @Test
    public void interfaceImplementations() {
        String cs = translate("Listener", INTERFACES);
        contains(cs, "Type IListener<string>.EventClass() => typeof(string);");
        contains(cs, "public SimpleFailure WithCode(int code) => this;");
        contains(cs, "IFailure IFailure.WithCode(int code) => WithCode(code);");
        contains(cs, "public abstract string Embed(string text);");
        contains(cs, "public override string Embed(string text) => text.ToUpperInvariant();");
        contains(cs, "public string GetSemantics() => semantics;");
        contains(cs, "public abstract int Size { get; }");
        assertFalse(cs.contains("abstract bool Distinct()"), cs);
        assertFalse(cs.contains("abstract string GetSemantics()"), cs);
    }

    @Language("java")
    private static final String MEMBER_NAMES = """
            package org.example.members;
            final class Match<Embedded> {
                private final Embedded embedded;
                Match(Embedded embedded) { this.embedded = embedded; }
                Embedded embedded() { return embedded; }
            }
            final class Outcome {
                enum Result { SUCCESS, FAILURE }
                private final Result result;
                Outcome(Result result) { this.result = result; }
                Result result() { return result; }
            }
            """;

    /** C# has no member named as a type parameter or a nested type of its type. */
    @Test
    public void memberNames() {
        String cs = translate("Match", MEMBER_NAMES);
        contains(cs, "internal sealed class Match<TEmbedded> {");
        contains(cs, "internal TEmbedded Embedded { get; }");
        contains(cs, "internal Result? ResultValue() => result;");
    }

    @Language("java")
    private static final String CAPTURE = """
            package org.example.capture;
            import java.util.List;
            interface Named { String name(); String title(); }
            class Context<Embedded> {
                List<Embedded> items() { return List.of(); }
            }
            interface Listener {
                default void onRequest(Context<?> context) { }
                default int size(Context<? extends Named> context) { return context.items().size(); }
                default int length(Context<? extends String> context) { return context.items().size(); }
            }
            final class Counting implements Listener {
                public void onRequest(Context<?> context) { System.out.println(context.items().size()); }
            }
            """;

    /** Java captures a wildcard in the body; C#'s method takes it as a type parameter of its own. */
    @Test
    public void wildcardCapture() {
        String cs = translate("Capture", CAPTURE);
        contains(cs, "void OnRequest<TEmbedded>(Context<TEmbedded> context) { }");
        contains(cs, "int Size<TEmbedded>(Context<TEmbedded> context) where TEmbedded : INamed");
        contains(cs, "int Length(Context<string> context)");
        contains(cs, "public void OnRequest<TEmbedded>(Context<TEmbedded> context)");
    }

    @Language("java")
    private static final String TYPE_TOKENS = """
            package org.example.tokens;
            import java.util.List;
            class Json {
                static <T> T fromJson(String json, Class<T> type) {
                    System.out.println(type.getName());
                    return null;
                }
                static <T> List<T> all(String json, Class<T> type) { return List.of(fromJson(json, type)); }
                static <T> String describe(Class<T> type) { return type.getName(); }
                static String use(Class<?> unknown) { return describe(unknown); }
                static Integer number(String json) { return fromJson(json, Integer.class); }
            }
            """;

    /** A type token, {@code Class<T> type}, is the method's type argument when every call says what it is. */
    @Test
    public void typeTokens() {
        String cs = translate("Json", TYPE_TOKENS);
        contains(cs, "internal static T FromJson<T>(string json) {");
        contains(cs, "Console.Out.WriteLine(typeof(T)");
        contains(cs, "internal static List<T> All<T>(string json) => new List<T> { FromJson<T>(json) };");
        contains(cs, "FromJson<int>(json)");
        contains(cs, "internal static string Describe<T>(Type type)");
    }

    @Language("java")
    private static final String COMPANION = """
            package org.example.companion;
            public final class Response<T> {
                private static final String EMPTY = "";
                private final T content;
                private Response(T content) { this.content = content; }
                public T content() { return content; }
                public String describe() { return EMPTY + content + count(); }
                private static int count() { return 1; }
                public static <T> Response<T> from(T content) { return new Response<>(content); }
            }
            interface Store<E> {
                int LIMIT = 10;
                static <E> Store<E> empty() { return null; }
                E get();
            }
            class User {
                Response<String> hello() { return Response.from("hello" + Store.LIMIT); }
            }
            """;

    /** C# reaches a generic class's statics through a constructed type: they are in a non-generic class beside it. */
    @Test
    public void companionClasses() {
        String cs = translate("Response", COMPANION);
        contains(cs, "public sealed class Response<T> {");
        contains(cs, "public string Describe() => Response.EMPTY + Content + Response.Count();");
        contains(cs, "internal Response(T content)");
        contains(cs, "public static class Response {");
        contains(cs, "internal const string EMPTY = \"\";");
        contains(cs, "internal static int Count() => 1;");
        contains(cs, "public static Response<T> From<T>(T content) => new Response<T>(content);");
        contains(cs, "internal static class IStore {");
        contains(cs, "public const int LIMIT = 10;");
        contains(cs, "Response.From(\"hello\" + IStore.LIMIT)");
    }

    @Language("java")
    private static final String DELEGATE_CLASH = """
            package org.example.clash;
            public class Button {
                public interface Listener { void clicked(); }
                private Listener listener;
                public Listener getListener() { return listener; }
                public void setListener(Listener listener) { this.listener = listener; }
            }
            """;

    /**
     * The properties analysis names types as the printing does: a nested delegate keeps its Java name, so a property
     * of that name would clash with it (CS0102) and the accessors stay methods.
     */
    @Test
    public void propertyClashesWithDelegate() {
        String cs = translate("Button", DELEGATE_CLASH);
        contains(cs, "public delegate void Listener();");
        contains(cs, "public Listener GetListener()");
    }

    @Language("java")
    private static final String SHADOWED_CAPTURE = """
            package org.example.capture;
            public class Steps {
                interface Step { int first(); int second(); }
                static Step make() {
                    int x = 5;
                    return new Step() {
                        public int first() { int x = 1; return x; }
                        public int second() { return x; }
                    };
                }
            }
            """;

    /** A local of one of the anonymous class's methods does not hide a capture of the same name in another. */
    @Test
    public void shadowedCapture() {
        String cs = translate("Steps", SHADOWED_CAPTURE);
        contains(cs, "return new StepImpl(x);");
        contains(cs, "internal StepImpl(int x) { this.x = x; }");
    }

    @Language("java")
    private static final String OPERATORS = """
            package org.example.operators;
            import java.math.BigInteger;
            public class Operators {
                enum Color { RED, GREEN }
                static int compareNegated(BigInteger x, BigInteger y) { return x.negate().compareTo(y); }
                static boolean differs(Color c, Object o) { return !(c == o); }
            }
            """;

    /**
     * A prefix template is no atom: the negated receiver is parenthesized. A negated == of a value and an object is
     * the negated object.Equals that == of them is, not C#'s != (CS0019).
     */
    @Test
    public void operators() {
        String cs = translate("Operators", OPERATORS);
        contains(cs, "(-x).CompareTo(y)");
        contains(cs, "!(object.Equals(c, o))");
    }

    @Language("java")
    private static final String HOISTED_SUPER = """
            package org.example.hoisted;
            import java.util.List;
            public class Holders {
                abstract static class Holder<T> {
                    final T value;
                    Holder(T value, List<? extends T> more) { this.value = value; }
                    abstract T describe();
                }
                static Holder<String> empty(List<String> names) {
                    return new Holder<>(null, names) { String describe() { return "none"; } };
                }
                static Holder<Integer> one(int i) {
                    return new Holder<Integer>(i, List.of()) { Integer describe() { return value; } };
                }
                static class Ones extends Holder<Integer> {
                    Ones() { super(1, List.of()); }
                    Integer describe() { return value; }
                }
            }
            """;

    /**
     * A hoisted anonymous class's constructor takes the superclass constructor's parameters as the class extends it,
     * and the creation converts its arguments to them; an override of a type parameter's member with a boxed type
     * argument has C#'s type argument, {@code int}, not {@code int?}.
     */
    @Test
    public void hoistedSuperArguments() {
        String cs = translate("Holders", HOISTED_SUPER);
        contains(cs, "=> new HolderImpl(default, names);");
        contains(cs, "internal HolderImpl(string p0, List<string> p1) : base(p0, p1) { }");
        contains(cs, "internal HolderImpl2(int p0, List<int> p1) : base(p0, p1) { }");
        contains(cs, "internal override int Describe() => value;");
    }

    @Language("java")
    private static final String MUTUAL_INNER = """
            package org.example.mutual;
            public class Tree {
                int size;
                class Branch {
                    Leaf leaf() { return new Leaf(); }
                    int size() { return size; }
                }
                class Leaf {
                    Branch branch() { return new Branch(); }
                }
            }
            """;

    /**
     * Two inner classes that create each other: the one that only creates the other, which uses the enclosing
     * instance, needs that instance too, whichever of them is decided first.
     */
    @Test
    public void mutualInnerClasses() {
        String cs = translate("Tree", MUTUAL_INNER);
        contains(cs, "internal Leaf(Tree outer) { this.outer = outer; }");
        contains(cs, "internal Branch Branch() => new Branch(outer);");
        contains(cs, "internal Leaf Leaf() => new Leaf(outer);");
    }

    @Language("java")
    private static final String PROPERTIES = """
            package org.example.properties;
            public class Person {
                private String name;
                private final int age;
                Person(int age) { this.age = age; }
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                int getAge() { return age; }
                public boolean isAdult() { return age >= 18; }
            }
            class Greeter {
                String greet(Person p) {
                    p.setName("Ann");
                    return p.getName() + (p.isAdult() ? "" : " (" + p.getAge() + ")");
                }
            }
            """;

    /**
     * A setter of the getter's field is the property's set; the property is as visible as its getter; a method that
     * computes is no getter. Calls are the property's reads and assignments.
     */
    @Test
    public void properties() {
        String cs = translate("Person", PROPERTIES);
        contains(cs, "public string Name { get; set; }");
        contains(cs, "internal int Age { get; }");
        contains(cs, "internal Person(int age) { this.Age = age; }");
        contains(cs, "public bool IsAdult() => Age >= 18;");
        contains(cs, "p.Name = \"Ann\";");
        contains(cs, "return p.Name + (p.IsAdult() ? \"\" : \" (\" + p.Age + \")\");");
    }

    @Language("java")
    private static final String JOINERS = """
            package org.example.joiners;
            public class Joiners<A> {
                interface BiJoiner<A, B> { }
                @SuppressWarnings("unchecked")
                <B> int join(Joiners<B> other, BiJoiner<A, B>... joiners) { return joiners.length; }
                @SuppressWarnings("unchecked")
                <B> int join(Joiners<B> other, BiJoiner<A, B> joiner) { return join(other, new BiJoiner[] { joiner }); }
                <B> int join(Joiners<B> other) { return join(other, new BiJoiner[0]); }
            }
            """;

    /** A raw array passed as a variable arity parameter's array gets the parameter's type arguments. */
    @Test
    public void rawVarargsArray() {
        String cs = translate("Joiners", JOINERS);
        contains(cs, "Join(other, new IBiJoiner<A, B>[] { joiner })");
        contains(cs, "Join(other, new IBiJoiner<A, B>[0])");
    }

    @Language("java")
    private static final String MAPPINGS = """
            package org.example.mappings;
            import java.util.function.Function;
            public class Mappings {
                abstract static class Stream {
                    protected abstract <M> M defaultMapping();
                    Function<String, Integer> lengths() { return defaultMapping(); }
                }
                static final class UniStream extends Stream {
                    @Override
                    protected Function<String, Integer> defaultMapping() { return String::length; }
                }
            }
            """;

    /**
     * Java overrides a generic method with one that is not, by erasure; C# prints the generic one with its type
     * parameter erased, the override's result covariant, and a call's result cast to what Java inferred.
     */
    @Test
    public void erasedGenerics() {
        String cs = translate("Mappings", MAPPINGS);
        contains(cs, "protected internal abstract object DefaultMapping();");
        contains(cs, "protected internal override Func<string, int> DefaultMapping()");
        contains(cs, "(Func<string, int>) DefaultMapping()");
    }

    @Language("java")
    private static final String PLACERS = """
            package org.example.placers;
            import java.util.Iterator;
            import java.util.List;
            public class Placers {
                interface Placer<P> extends Iterable<P> { boolean isEmpty(); }
                abstract static class AbstractPlacer<P> implements Placer<P> {
                    public boolean isEmpty() { return !iterator().hasNext(); }
                }
                static final class ListPlacer extends AbstractPlacer<String> {
                    private final List<String> list = List.of("a");
                    public Iterator<String> iterator() { return list.iterator(); }
                }
            }
            """;

    /**
     * An interface that extends Iterable is enumerable by default implementations over its Iterator(), which an
     * abstract class that leaves it to its subclasses declares abstract.
     */
    @Test
    public void iterableInterfaces() {
        String cs = translate("Placers", PLACERS);
        contains(cs, "IJavaIterator<P> Iterator();");
        contains(cs, "IEnumerator<P> IEnumerable<P>.GetEnumerator() => Iterator().AsEnumerator();");
        contains(cs, "public abstract IJavaIterator<P> Iterator();");
        contains(cs, "public override IJavaIterator<string> Iterator() => list.Iterator();");
    }

    @Language("java")
    private static final String STREAMS = """
            package org.example.streams;
            import java.util.function.Function;
            public class Streams<A> {
                interface Stream<T> { }
                static final class MapStream<A, NewA> implements Stream<NewA> {
                    MapStream(Streams<A> parent, Function<A, NewA> mapping) { }
                }
                <S extends Stream<?>> S share(S stream) { return stream; }
                <ResultA_> Stream<ResultA_> map(Function<A, ResultA_> mapping) {
                    return share(new MapStream<>(this, mapping));
                }
                static final class Bridge<A> implements Stream<A> { Bridge(Streams<?> parent) { } }
                static final class Group<A, NewA> implements Stream<NewA> { void setBridge(Bridge<NewA> bridge) { } }
                <S extends Stream<?>> S share(S stream, java.util.function.Consumer<S> consumer) {
                    consumer.accept(stream);
                    return stream;
                }
                <NewA> Stream<NewA> group(Group<A, NewA> group) { return share(new Bridge<>(this), group::setBridge); }
                static final class Bridge2<A, B> implements Stream<B> { Bridge2(Streams<?> parent) { } }
                static final class Joined<A, B> implements Stream<B> { void setBridge(Bridge2<A, B> bridge) { } }
                <B> Stream<B> join(Joined<A, B> joined) { return share(new Bridge2<>(this), joined::setBridge); }
                static <T> String key(Function<T, ?> mapping, T t) { return String.valueOf(mapping.apply(t)); }
                static <T, P> String keyed(Function<T, P> mapping, T t) { return key(mapping, t); }
                static String raw(Function mapping) { return String.valueOf(mapping.apply("x")); }
                static <T, P> String rawKeyed(Function<T, P> mapping) { return raw(mapping); }
                static String supplied(java.util.function.Supplier<?> supplier) { return String.valueOf(supplier.get()); }
                static <P> String supply(java.util.function.Supplier<P> supplier) { return supplied(supplier); }
                abstract static class Builder { abstract <S extends Stream<?>> S build(S s); }
                static final class SimpleBuilder extends Builder { <S extends Stream<?>> S build(S s) { return s; } }
                record Pair<L, R>(L left, R right) { }
                static <E> java.util.Set<E> fromMap(java.util.Map<E, Boolean> map) { return map.keySet(); }
                static <T> java.util.Set<T> identitySet() { return fromMap(new java.util.HashMap<>()); }
                static final class Slot<R, P> { Slot(boolean min, java.util.NavigableMap<P, R> map) { } }
                static <R extends Comparable<R>> Slot<R, R> minSlot() { return new Slot<>(true, new java.util.TreeMap<>()); }
                static <L, R> java.util.function.BiFunction<L, R, Pair<L, R>> pairing() { return Pair::new; }
                static <V> java.util.function.IntFunction<java.util.Set<V>> sets() { return java.util.HashSet::new; }
                static <R> java.util.List<R> empty() { return new java.util.ArrayList<>(); }
                static <R> java.util.function.Supplier<java.util.List<R>> emptySupplier() { return Streams::empty; }
                static <T, R> R max(Function<T, R> mapping, T t) { return mapping.apply(t); }
                static <T, P> P maxed(Function<T, P> mapping, T t) { return max(mapping, t); }
            }
            """;

    /** A diamond's type arguments are the ones Java infers, never the created class's own type parameters. */
    @Test
    public void diamond() {
        String cs = translate("Streams", STREAMS);
        contains(cs, "new MapStream<A, ResultA_>(");
        // no argument mentions Bridge's A: the type Java inferred for share's S, which group::setBridge binds
        contains(cs, "new Bridge<NewA>(this)");
        contains(cs, "new Bridge2<A, B>(this)");
        // Func<T, P> where Func<T, object> is expected: C#'s variance is for reference types only
        contains(cs, "Key(x0_ => mapping(x0_), t)");
        contains(cs, "Raw(x0_ => mapping((T) x0_))");
        contains(cs, "Supplied(() => supplier())");
        // the callee's own R: C# infers it from the variable, a lambda would hide it
        contains(cs, "Max(mapping, t)");
        // a method group whose type parameter only the result mentions: C# does not infer it from the delegate
        contains(cs, "=> Streams.Empty<R>;");
        // a raw constructor reference creates the type Java inferred for its result
        contains(cs, "(p0, p1) => new Pair<L, R>(p0, p1)");
        contains(cs, "new HashSet<V>(p0)");
        // a diamond whose parameter's type is not a type parameter: Map<E, Boolean>, as inferred
        contains(cs, "new Dictionary<T, bool>()");
        // an argument of a creation: the creation's own type arguments, as printed
        contains(cs, "new SortedDictionary<R, R>()");
        // an override inherits its constraints, and may not repeat them (CS0460)
        // Stream<?>, a bound: the wildcard interface IStream, which every IStream<T> is
        contains(cs, "internal abstract S Build<S>(S s) where S : IStream;");
        contains(cs, "internal Bridge(IStreams parent)");
        contains(cs, "internal override S Build<S>(S s) => s;");
    }

    @Language("java")
    private static final String SCORES = """
            package org.example.scores;
            import java.util.List;
            public class Scores {
                public interface Score<Score_ extends Score<Score_>> extends Comparable<Score_> {
                    Score_ add(Score_ other);
                    Score_ negate();
                    boolean isZero();
                    String toShortString();
                }
                public record SimpleScore(int value) implements Score<SimpleScore> {
                    public SimpleScore add(SimpleScore other) { return new SimpleScore(value + other.value); }
                    public SimpleScore negate() { return new SimpleScore(-value); }
                    public boolean isZero() { return value == 0; }
                    public String toShortString() { return "" + value; }
                    public int compareTo(SimpleScore other) { return Integer.compare(value, other.value); }
                }
                public interface Director<Solution_, Score_ extends Score<Score_>> {
                    Score_ calculate(Solution_ solution);
                    List<Score_> history();
                }
                static int compare(Score<?> a, Score<?> b) { return ((Score) a).compareTo(b); }
                static final class MinCollector<A, P extends Comparable<? super P>> {
                    private final java.util.function.Function<A, P> mapper;
                    MinCollector(java.util.function.Function<A, P> mapper) { this.mapper = mapper; }
                    @Override public boolean equals(Object o) {
                        if (o == null || getClass() != o.getClass()) return false;
                        MinCollector<?, ?> that = (MinCollector<?, ?>) o;
                        return java.util.Objects.equals(mapper, that.mapper);
                    }
                    @Override public int hashCode() { return mapper.hashCode(); }
                }
                static boolean anyZero(List<Score<?>> scores) {
                    for (Score<?> score : scores) if (score.isZero() || score.negate().isZero()) return true;
                    return false;
                }
                static <S> String describe(Director<S, ?> director, S solution) {
                    return director.calculate(solution).toShortString();
                }
                public record Inner<Score_ extends Score<Score_>>(Score_ raw, int unassigned)
                        implements Comparable<Inner<Score_>> {
                    public int compareTo(Inner<Score_> other) { return raw.compareTo(other.raw); }
                }
                static int compareInner(Inner<?> a, Inner<?> b) { return ((Inner) a).compareTo(b); }
            }
            """;

    /**
     * A wildcard of a self-bounded type has no C# type argument: its companion, IScore beside IScore<Score_>, which
     * declares the members a wildcard's user calls, the removed parameter in a result erased to its bound's companion.
     */
    @Test
    public void companions() {
        String cs = translate("Scores", SCORES);
        contains(cs, "public interface IScore<Score_> : IComparable<Score_>, IScore where Score_ : IScore<Score_> {");
        // Comparable<Score_>: the companion is C#'s non-generic IComparable, which the type implements by a cast
        contains(cs, "public interface IScore : IComparable {");
        contains(cs, "int IComparable.CompareTo(object other) => CompareTo((Score_) other!);");
        // Comparable<Inner<Score_>>, of the type itself, likewise
        contains(cs, "public interface IInner : IComparable {");
        contains(cs, "int IComparable.CompareTo(object other) => CompareTo((Inner<Score_>) other!);");
        contains(cs, "IScore Negate();");
        // the bridges: every IScore<S> is an IScore
        contains(cs, "IScore IScore.Negate() => Negate();");
        contains(cs, "bool IScore.IsZero() => IsZero();");
        // a result of the removed parameter in a collection has no companion form: History is not the companion's
        contains(cs, "public interface IDirector<Solution_> { IScore Calculate(Solution_ solution); }");
        contains(cs, "internal static bool AnyZero(List<IScore> scores)");
        contains(cs, "internal static string Describe<S>(IDirector<S> director, S solution)");
        assertFalse(cs.contains("IScore<object>"), cs);
        // in its own equals, the type's wildcard is the type itself, which has the private field
        contains(cs, "MinCollector<A, P> that = (MinCollector<A, P>) o;");
    }

    @Language("java")
    private static final String LIBRARY_FUNCTIONAL = """
            package org.example.filters;
            import java.util.function.BiPredicate;
            import java.util.function.ToIntFunction;
            public class Filters {
                interface Movable<S> extends BiPredicate<S, Object> {
                    default Movable<S> and(Movable<S> other) { return (s, e) -> test(s, e) && other.test(s, e); }
                }
                interface PinReader extends ToIntFunction<Object> { }
                static boolean movable(Movable<String> m, Object e) { return m.test("s", e); }
                static int pin(PinReader r, Object e) { return r == null ? 0 : r.applyAsInt(e); }
                static final class Always implements Movable<String> {
                    public boolean test(String s, Object e) { return true; }
                }
            }
            """;

    /**
     * A functional interface of the program that extends the library's, MovableFilter of BiPredicate: C# cannot extend
     * the delegate, the interface declares the method itself, in PascalCase, and a call through it is no invocation.
     */
    @Test
    public void libraryFunctionalInterfaces() {
        String cs = translate("Filters", LIBRARY_FUNCTIONAL);
        contains(cs, "bool Test(S ");
        contains(cs, "Test(s, e) && other.Test(s, e)");
        contains(cs, "m.Test(\"s\", e)");
        contains(cs, "r.ApplyAsInt(e)");
        contains(cs, "public bool Test(string s, object e) => true;");
    }

    @Language("java")
    private static final String BOXED_REFERENCES = """
            package org.example.boxedrefs;
            import java.util.function.Function;
            import java.util.function.Supplier;
            public class Counts {
                static final class State {
                    Long result() { return 1L; }
                    static Long initial() { return 0L; }
                    Long own() { return 2L; }
                    Supplier<Long> mine() { return this::own; }
                }
                static Function<State, Long> finisher() { return State::result; }
                static Supplier<Long> first() { return State::initial; }
            }
            """;

    /**
     * A method reference whose result is a Long, long?, into a functional interface whose result is a type parameter,
     * C#'s long in Func<State, long>: a method group does not convert, a lambda unboxes.
     */
    @Test
    public void boxedMethodReferences() {
        String cs = translate("Counts", BOXED_REFERENCES);
        contains(cs, "(State p0) => (long) p0.Result()");
        contains(cs, "() => (long) State.Initial()");
        contains(cs, "() => (long) this.Own()");
    }

    @Language("java")
    private static final String LIFECYCLES = """
            package org.example.lifecycles;
            public class Lifecycles {
                public interface Tuple { int size(); }
                public record Pair(int size) implements Tuple {}
                public interface ActivitySupport { boolean isActive(); }
                public interface TupleLifecycle<Tuple_ extends Tuple> extends ActivitySupport {
                    void insert(Tuple_ tuple);
                    void afterAllFactsInserted(boolean canProduce);
                    static <T extends Tuple> TupleLifecycle<T> same(TupleLifecycle<T> lifecycle) { return lifecycle; }
                }
                public abstract static class Node<Left_ extends Tuple> {
                    private final TupleLifecycle<?> downstream;
                    protected Node(TupleLifecycle<?> downstream) { this.downstream = downstream; }
                    public boolean active() { return downstream.isActive(); }
                    public void done() { downstream.afterAllFactsInserted(true); }
                }
                public static final class PairNode extends Node<Pair> {
                    PairNode(TupleLifecycle<Pair> next) { super(TupleLifecycle.same(next)); }
                }
                public static class Box<T> {
                    private final T value;
                    Box(T value) { this.value = value; }
                    public T get() { return value; }
                    public int count() { return 1; }
                    public void set(T t) { }
                    public <M> M mapping() { return null; }
                }
                public static final class Named extends Box<String> {
                    Named() { super("n"); }
                    @Override public Object mapping() { return "m"; }
                }
                static int counted(Box<?> box) { return box.count(); }
                static int sized(Box<?> box) { return box.count(); }
                static int sized(Named named) { return 0; }
                static int total(java.util.List<Box<?>> boxes) {
                    int sum = 0;
                    for (var box : boxes) sum += counted(box);
                    return sum;
                }
                static Box<?> any(boolean b) { return b ? new Box<>("s") : new Box<>(1); }
                public interface Grade<G extends Grade<G>> { }
                public interface Impacter<G extends Grade<G>> {
                    java.util.List<G> impacts();
                    static <G extends Grade<G>> Impacter<G> none() { return null; }
                }
                static int impacts(Impacter<?> impacter) { return impacter.impacts().size(); }
                public abstract static class Base { }
                public static final class Node2<T> extends Base { public int size() { return 0; } }
                static Node2<?> node() { return new Node2<String>(); }
                static int based(Base base) { return 1; }
                static int nodes() { var n = node(); return based(n) + n.size(); }
                static int countOf(boolean b) { return any(b).count(); }
                static Object first(boolean b) { return any(b).get(); }
                static int sum(java.util.List<? extends Box<?>> boxes) { return boxes.size(); }
                public static class Holder<T> {
                    final Object[] items = {};
                    public T first() { return null; }
                }
                public static class Typed<A> {
                    private final Class<A> type;
                    Typed(Class<A> type) { this.type = type; }
                    public Class<A> kind() { return type; }
                }
                public static class Counter<T> { int count; T last; }
                public static final class Key<T extends Tuple> { final String name = "k"; }
                static <T extends Tuple> T attribute(Key<T> key) { return null; }
                static final Key<?>[] KEYS = {};
                static int named() { int n = 0; for (Key<?> k : KEYS) { n += k.name.length(); attribute(k); } return n; }
                static final class Uses {
                    private Counter<?> counter;
                    void bump() { counter.count = 1; }
                    private Holder<?> holder;
                    private Typed<?> typed;
                    private Box<? extends Tuple> boxed;
                    int items() { return holder.items.length; }
                    String name() { return typed.kind().getSimpleName(); }
                }
            }
            """;

    /**
     * A wildcard of any generic type the program declares a field, a constructor's parameter, a result, a local with
     * has no C# type argument either, where a method's parameter would capture it: the type's wildcard interface,
     * without the wildcarded type parameters, which every X<A> implements.
     */
    @Test
    public void wildcardInterfaces() {
        String cs = translate("Lifecycles", LIFECYCLES);
        contains(cs, "public interface ITupleLifecycle : IActivitySupport {");
        // its statics, the companion's, which has the wildcard interface's name: one interface
        contains(cs, "public static ITupleLifecycle<T> Same<T>(ITupleLifecycle<T> lifecycle) where T : ITuple => lifecycle;");
        contains(cs, "base(ITupleLifecycle.Same(next))");
        assertFalse(cs.contains("static class ITupleLifecycle"), cs);
        contains(cs, "void ITupleLifecycle.AfterAllFactsInserted(bool canProduce) => AfterAllFactsInserted(canProduce);");
        contains(cs, "private readonly ITupleLifecycle downstream;");
        contains(cs, "Node(ITupleLifecycle downstream)");
        contains(cs, "public interface IBox {");
        contains(cs, "internal static IBox Any(bool b)");
        // a self-bounded interface with a companion keeps both, as before the companion became the wildcard interface
        contains(cs, "public static class IImpacter {");
        // the wildcard interface where its class's superclass is expected: a cast
        contains(cs, "Based((Base) n)");
        // a result of the wildcarded parameter is its bound, a parameter's keeps the member out
        contains(cs, "object Get { get; }");
        assertFalse(cs.contains("void Set(object"), cs);
        assertFalse(cs.contains("ITupleLifecycle<ITuple>"), cs);
        assertFalse(cs.contains("Box<object>"), cs);
        // a parameter of a type with a wildcard interface is not captured: an element of a List<Box<?>> is passed
        contains(cs, "internal static int Counted(IBox box)");
        // beside an overload, captured: C# finds IBox as specific as a Named (CS0121)
        contains(cs, "internal static int Sized<T>(Box<T> box)");
        // a generic method C# prints erased is no wildcard interface's
        assertFalse(cs.contains("IBox.Mapping"), cs);
        // a field read through the wildcard is a property of the wildcard interface, of the field's name
        contains(cs, "private IHolder holder;");
        contains(cs, "internal object[] items { get; }");
        contains(cs, "object[] IHolder.items => items;");
        // a wildcard passed where the type is declared with arguments, Key<T>: no wildcard interface, which infers no T
        contains(cs, "Key<ITuple>[] KEYS");
        // one written through it is not: Counter keeps its bound
        contains(cs, "private Counter<object> counter;");
        // Class<A>, C#'s Type, needs no argument
        contains(cs, "private ITyped typed;");
        contains(cs, "public interface ITyped { Type Kind { get; } }");
        // ? extends Tuple is its bound, what the code stores there
        contains(cs, "private Box<ITuple> boxed;");
    }

    @Language("java")
    private static final String PHASE_SCOPES = """
            package org.example.phasescopes;
            public class PhaseScopes {
                public interface Score<Score_ extends Score<Score_>> { boolean isZero(); }
                public record SimpleScore(int value) implements Score<SimpleScore> {
                    public boolean isZero() { return value == 0; }
                }
                public record Inner<Score_ extends Score<Score_>>(Score_ raw) { boolean zero() { return raw.isZero(); } }
                public interface Director<Solution_, Score_ extends Score<Score_>> { Score_ calculate(Solution_ solution); void reset(); }
                static class SolverScope<Solution_> {
                    private Inner<?> best;
                    private Director<Solution_, ?> director;
                    public <Score_ extends Score<Score_>> Inner<Score_> getBest() { return (Inner<Score_>) best; }
                    public <Score_ extends Score<Score_>> Director<Solution_, Score_> getDirector() {
                        return (Director<Solution_, Score_>) director;
                    }
                }
                static class PhaseScope<Solution_> {
                    SolverScope<Solution_> solverScope;
                    public <Score_ extends Score<Score_>> Director<Solution_, Score_> getDirector() {
                        return solverScope.getDirector();
                    }
                    public <Score_ extends Score<Score_>> Inner<Score_> getBest() { return solverScope.getBest(); }
                }
                static <Solution_> boolean zero(PhaseScope<Solution_> scope) { return scope.getBest().raw().isZero(); }
                static <Solution_> String log(PhaseScope<Solution_> scope) { return "" + scope.getBest(); }
                static <Solution_, Score_ extends Score<Score_>> Score_ calculate(PhaseScope<Solution_> scope, Solution_ s) {
                    Director<Solution_, Score_> d = scope.getDirector();
                    return d.calculate(s);
                }
                interface Acceptor { }
                static <T> T newInstance(Object bean, String name, Class<T> clazz) {
                    return null;
                }
                static Acceptor acceptor(Class<? extends Acceptor> c) { return newInstance(null, "a", c); }
                static Object any(Class<?> c) { return newInstance(null, "a", c); }
                static SimpleScore simple(PhaseScope<String> scope) {
                    Inner<SimpleScore> best = scope.getBest();
                    return best.raw();
                }
            }
            """;

    /**
     * A self-bounded type parameter that only the result mentions: Java infers a raw or captured type, which no C#
     * type argument is. The method is printed with it erased, its result the companion or the wildcard's bound, and
     * a call where Java's type is writable casts to it. A record's companion has its components. A {@code Class<T>} is C#'s {@code Type}, from which C#
     * infers nothing: T is the wildcard's bound.
     */
    @Test
    public void resultOnlySelfBound() {
        String cs = translate("PhaseScopes", PHASE_SCOPES);
        // a record has a companion too, with its components, and its package-private methods internal
        contains(cs, "IScore Raw { get; }");
        contains(cs, "internal bool Zero();");
        contains(cs, "IScore IInner.Raw => Raw;");
        contains(cs, "public IInner GetBest() => (IInner) (object) best;");
        contains(cs, "public IDirector<Solution_> GetDirector() => solverScope.GetDirector();");
        contains(cs, "scope.GetBest().Raw.IsZero()");
        contains(cs, "IDirector<Solution_, Score_> d = (IDirector<Solution_, Score_>) (object) scope.GetDirector();");
        contains(cs, "Inner<SimpleScore> best = (Inner<SimpleScore>) (object) scope.GetBest();");
        contains(cs, "NewInstance<IAcceptor>(null, \"a\", c)");
        contains(cs, "NewInstance<object>(null, \"a\", c)");
    }

    @Language("java")
    private static final String NODES = """
            package org.example.nodes;
            public class Nodes {
                abstract static class AbstractNode<A> { abstract A value(); }
                public static final class TextNode extends AbstractNode<String> { String value() { return "x"; } }
                static class Hidden { }
            }
            """;

    /** Java's public class may extend a package-private one, C#'s may not (CS0060): the base is public. */
    @Test
    public void exposedBases() {
        String cs = translate("Nodes", NODES);
        contains(cs, "public abstract class AbstractNode<A>");
        contains(cs, "public sealed class TextNode : AbstractNode<string>");
        assertFalse(cs.contains("public class Hidden"), cs);
    }

    @Language("java")
    private static final String MODES = """
            package org.example.modes;
            public class Modes {
                enum Mode {
                    FAST(1), FULL_ASSERT(3);
                    private final int level;
                    Mode(int level) { this.level = level; }
                    int level() { return level; }
                }
                static boolean asserted(Mode mode) {
                    return switch (mode) {
                        case FULL_ASSERT -> true;
                        case FAST -> false;
                    };
                }
                static int weight(Mode mode) {
                    int w = 0;
                    switch (mode) {
                        case FAST: w++;
                        case FULL_ASSERT: w += mode.level(); break;
                    }
                    return w;
                }
            }
            """;

    /** An enum C# declares as a class has no constants for a case: its instances match on their Name. */
    @Test
    public void switchOnEnumClass() {
        String cs = translate("Modes", MODES);
        contains(cs, "{ Name: \"FULL_ASSERT\" } => true");
        contains(cs, "case { Name: \"FAST\" }:");
        // a fall-through into a pattern's section: goto case cannot name it, a statement label can
        contains(cs, "goto case_FULL_ASSERT;");
        contains(cs, "case_FULL_ASSERT: ;");
    }

    @Language("java")
    private static final String STORE = """
            package org.example.store;
            import java.util.Objects;
            public class Store {
                private final Object[] values = new Object[4];
                @SuppressWarnings("unchecked")
                <V> V get(int i) { return (V) values[i]; }
                String name() {
                    String s = get(0);
                    return Objects.requireNonNullElseGet(s, () -> {
                        String fallback = "x";
                        return fallback + values.length;
                    });
                }
            }
            """;

    /**
     * A method type parameter only the result mentions gets Java's inferred type argument (CS0411); a supplier with a
     * block body is called through its delegate where a template calls it.
     */
    @Test
    public void inferredAndSupplied() {
        String cs = translate("Store", STORE);
        contains(cs, "string s = Get<string>(0);");
        contains(cs, "s ?? ((Func<string>) (() => {");
        contains(cs, "}))();");
    }

    @Language("java")
    private static final String SELECTORS = """
            package org.example.selectors;
            import java.util.ArrayList;
            import java.util.List;
            public class Selectors {
                interface Selector { boolean isNeverEnding(); }
                abstract static class AbstractSelector implements Selector {
                    protected final String name = getClass().getSimpleName();
                    protected final List<String> seen = new ArrayList<>();
                }
                abstract static class AbstractDemandEnabledSelector extends AbstractSelector {
                    @Override
                    public abstract boolean equals(Object other);
                    @Override
                    public abstract int hashCode();
                }
                abstract static class GenericSelector extends AbstractDemandEnabledSelector { }
                static final class FromSolution extends GenericSelector {
                    private final boolean random;
                    FromSolution(boolean random) { this.random = random; }
                    public boolean isNeverEnding() { return random; }
                    public boolean equals(Object other) { return other == this; }
                    public int hashCode() { return 1; }
                }
            }
            """;

    /**
     * An interface's method that an abstract class leaves to its subclasses is declared abstract once, by the first
     * abstract class, also when another abstract class is in between; an initializer that uses this runs in the
     * constructor, as Java's does.
     */
    @Test
    public void abstractAndInitializers() {
        String cs = translate("Selectors", SELECTORS);
        assertEquals(1, cs.split("public abstract bool NeverEnding", -1).length - 1, cs);
        contains(cs, "protected internal readonly string name;");
        contains(cs, "this.name = this.GetType().Name;");
        contains(cs, "protected AbstractSelector() {");
        contains(cs, "this.seen = new List<string>();");
    }

    @Language("java")
    private static final String INNER = """
            package org.example.inner;
            import java.util.Iterator;
            public class Range implements Iterable<Long> {
                private final long from;
                private final long to;
                public Range(long from, long to) { this.from = from; this.to = to; }
                public Iterator<Long> iterator() { return new Upcoming(); }
                long width() { return to - from; }
                Upcoming twice(Range other) { return other.new Upcoming(); }
                java.util.function.LongFunction<Stepped> stepper() { return Stepped::new; }
                class Upcoming implements Iterator<Long> {
                    private long next = from;
                    public boolean hasNext() { return next < to; }
                    public Long next() { return next++; }
                    long left() { return width() - (next - Range.this.from); }
                    Upcoming copy() { return new Upcoming(); }
                }
                class Stepped extends Upcoming {
                    private final long step;
                    Stepped(long step) { this.step = step; }
                    long step() { return step; }
                }
                class Plain {
                    int one() { return 1; }
                }
            }
            """;

    /** A Java inner class's enclosing instance is C#'s field outer, which its constructors take first. */
    @Test
    public void innerClasses() {
        String cs = translate("Range", INNER);
        // created with this in the enclosing type's code, with the scope of other.new Upcoming()
        contains(cs, "public IJavaIterator<long> Iterator() => new Upcoming(this);");
        contains(cs, "internal Upcoming Twice(Range other) => new Upcoming(other);");
        // the field, the default constructor that takes it, and the initializer that uses it, moved after it
        contains(cs, "private readonly Range outer;");
        contains(cs, "this.outer = outer;");
        contains(cs, "this.next = outer.from;");
        assertFalse(cs.contains("private long next = outer.from;"), cs);
        // the enclosing instance's members, and Range.this
        contains(cs, "public bool HasNext() => next < outer.to;");
        contains(cs, "internal long Left() => outer.Width() - (next - outer.from);");
        // created with the enclosing instance from an inner class's code; passed on to the superclass
        contains(cs, "internal Upcoming Copy() => new Upcoming(outer);");
        contains(cs, "internal Stepped(Range outer, long step) : base(outer) {");
        // a constructor reference takes it too
        contains(cs, "p0 => new Stepped(this, p0)");
        // an inner class that does not use its enclosing instance does not take it
        contains(cs, "internal sealed class Plain { internal int One() => 1; }");
    }
}
