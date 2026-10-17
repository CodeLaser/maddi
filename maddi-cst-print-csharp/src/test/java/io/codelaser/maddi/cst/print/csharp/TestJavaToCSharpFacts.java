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

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.print.translate.PrintMessage;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The analyses' facts in the printing ({@link CSharpFacts}): read-only parameters, nullable declarations and the
 * {@code !} of a nullable context, dictionary keys as {@code notnull}, parameter preconditions. Each test writes its samples' translations to {@code build/csharp-samples/}.
 */
public class TestJavaToCSharpFacts extends CommonJavaToCSharp {

    @Language("java")
    private static final String FACTS = """
            package org.example.facts;
            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            public class Catalog {
                private final List<String> names = new ArrayList<>();
                private String description;
                private List<String> stored;
                int score(List<String> items, Map<String, Integer> weights) {
                    int n = 0;
                    for (String s : items) {
                        if (items.contains(s) && weights.get(s) != null) n += weights.get(s);
                    }
                    return n + items.indexOf("x");
                }
                void addAll(List<String> more) { names.addAll(more); }
                void keep(List<String> list) { this.stored = list; }
                String find(String key) {
                    String d = description;
                    return key.isEmpty() ? null : d;
                }
                int size(Map<String, Integer> table) { return table.size(); }
                int sizeOf(Map<String, Integer> given) { return size(given); }
                int width(Map<String, Integer> sized) { return sized.size(); }
                int fresh() { return width(new HashMap<>()); }
                int length() { return find("k").length() + description.length(); }
                void collect(List<String> out) { out.add(find("k")); out.add(description); }
                boolean has(List<String> l) { return l.contains(description) || l.contains(null); }
                String[] pair() {
                    String[] a = { description, null };
                    a[1] = null;
                    return a;
                }
                String either(boolean b) { return b ? description : null; }
                Object cast() { return (CharSequence) find("k"); }
                int unbox(Map<String, Integer> m, List<Integer> l, Integer boxed, Integer checked) {
                    int a = m.get("k");
                    int b = l.get(0);
                    int c = boxed;
                    return checked == null ? a : a + b + c + checked;
                }
                void put(Map<Integer, String> byId, Integer id) { byId.put(id, description); }
                static <T> T ensure(T maybe) {
                    if (maybe == null) throw new IllegalArgumentException();
                    return maybe;
                }
                void over(String s) { }
                void over(Integer i) { }
                void callOver() { over((String) null); }
                int printed(Object o) { return o.toString().length(); }
                static class Holder<T> {
                    T held;
                    T held() { return held; }
                }
            }
            """;

    /** The analysis's verdicts as a client supplies them: these parameters unmodified, these declarations nullable. */
    private static final CSharpFacts CATALOG_FACTS = new CSharpFacts() {
        @Override
        public boolean unmodified(ParameterInfo parameter) {
            return Set.of("items", "weights", "more", "list", "table", "sized").contains(parameter.name());
        }

        @Override
        public NullableState nullable(ParameterInfo parameter) {
            return "maybe".equals(parameter.name()) ? NullableState.NULLABLE
                    : NullableState.UNSPECIFIED;
        }

        @Override
        public NullableState nullable(FieldInfo field) {
            return "description".equals(field.name()) || "held".equals(field.name())
                    ? NullableState.NULLABLE
                    : NullableState.UNSPECIFIED;
        }

        @Override
        public NullableState nullable(MethodInfo method) {
            // Map.get as an annotated JDK has it
            return "find".equals(method.name()) || "get".equals(method.name())
                                                   && "java.util.Map".equals(method.typeInfo().fullyQualifiedName())
                    ? NullableState.NULLABLE
                    : NullableState.UNSPECIFIED;
        }
    };

    /**
     * A collection parameter the method does not modify is read-only, when its uses allow (a stored one is not); the
     * calls on it are a read-only collection's. A declaration that may be null is T?.
     */
    @Test
    public void facts() {
        policy = CSharpProgram.Policy.DEFAULT.withFacts(CATALOG_FACTS);
        String cs = translate("Catalog", FACTS);
        contains(cs, "using System.Linq;");
        contains(cs, "private string? description;");
        contains(cs, "internal int Score(IReadOnlyList<string> items, IReadOnlyDictionary<string, int> weights) {");
        contains(cs, "if (items.Contains(s) && (weights.ContainsKey(s) ? weights[s] : (int?) null) != null)");
        contains(cs, "return n + JavaCollections.IndexOf(items, \"x\");");
        contains(cs, "internal void AddAll(IReadOnlyList<string> more)");
        contains(cs, "internal void Keep(List<string> list) { this.stored = list; }");
        contains(cs, "internal string? Find(string key)");
        // a caller's IDictionary is no IReadOnlyDictionary; a Dictionary is
        contains(cs, "internal int Size(IDictionary<string, int> table)");
        contains(cs, "internal int Width(IReadOnlyDictionary<string, int> sized)");
        contains(cs, "string d = description;");
    }

    @Language("java")
    private static final String KEYS = """
            package org.example.keys;
            import java.util.HashMap;
            import java.util.Map;
            import java.util.TreeMap;
            public class Index<K, V> {
                private final Map<K, V> byKey = new HashMap<>();
                public static <A extends Comparable<A>, B> TreeMap<A, B> sorted(Map<A, B> m) { return new TreeMap<>(m); }
                public <C> Map<C, V> copy(C first) { Map<C, V> m = new HashMap<>(); return m; }
                public <D> int size(Map<D, V> m) { return m.size(); }
                static <E, F> Map<E, F> group(java.util.function.Function<? super F, ? extends E> key) {
                    return keyed(key);
                }
                static <G, H> Map<G, H> keyed(java.util.function.Function<? super H, ? extends G> key) {
                    return new HashMap<>();
                }
                static class Sub<S, U> extends Index<S, U> { }
                static class Plain<P> { P value; }
            }
            """;

    /**
     * In a nullable context, a type parameter that is a dictionary's key is {@code where K : notnull}, as C#'s
     * Dictionary wants (CS8714), and so is one passed where such a key is expected; IDictionary takes any key.
     */
    @Test
    public void notNullKeys() {
        policy = CSharpProgram.Policy.DEFAULT.withFacts(new CSharpFacts() {
            @Override
            public boolean nullabilityAnalysed() {
                return true;
            }
        });
        String cs = translate("Index", KEYS);
        contains(cs, "public class Index<K, V> where K : notnull {");
        contains(cs, "where A : notnull, IComparable<A>");
        contains(cs, "Copy<C>(C first) where C : notnull");
        contains(cs, "class Sub<S, U> : Index<S, U> where S : notnull {");
        // through a wildcard: E is passed as ? extends G
        contains(cs, "where E : notnull");
        contains(cs, "where G : notnull");
        assertFalse(cs.contains("where D : notnull"), cs);
        assertFalse(cs.contains("where P : notnull"), cs);
        assertFalse(cs.contains("where V : notnull"), cs);
    }

    @Language("java")
    private static final String PRECONDITION = """
            package org.example.pre;
            class P {
                interface Visitor { void visit(String n); }
                static class Printer implements Visitor {
                    public void visit(String n) { System.out.println(n.length()); }
                }
                static String find(String k) { return k.isEmpty() ? null : k; }
                static void go(Visitor v, String k) {
                    v.visit(find(k));
                }
            }
            """;

    /**
     * CodeLaser/maddi#125: a parameter the analysis declares non-null as a precondition of its override family is
     * non-nullable on the interface and the override alike; a caller that may pass null says {@code !}, which does
     * not throw, so the method throws on the null as Java's does. An INFO message at each declaration; no
     * {@code ArgumentNullException.ThrowIfNull}.
     */
    @Test
    public void preconditionNonNull() {
        policy = CSharpProgram.Policy.DEFAULT.withFacts(new CSharpFacts() {
            @Override
            public NullableState nullable(MethodInfo method) {
                return "find".equals(method.name()) ? NullableState.NULLABLE
                        : NullableState.UNSPECIFIED;
            }

            @Override
            public boolean preconditionNonNull(ParameterInfo parameter) {
                return "n".equals(parameter.name());
            }

            @Override
            public boolean nullabilityAnalysed() {
                return true;
            }
        });
        Translation translation = translation(PRECONDITION);
        String cs = translation.csharp();
        contains(cs, "void Visit(string n);");
        contains(cs, "public void Visit(string n)");
        contains(cs, "v.Visit(Find(k)!);");
        assertFalse(cs.contains("ThrowIfNull"), cs);
        List<PrintMessage<CSharpPrintCode>> preconditions = translation.messages().stream()
                .filter(m -> m.code() == CSharpPrintCode.PRECONDITION_NON_NULL).toList();
        assertEquals(List.of(3, 5),
                preconditions.stream().map(PrintMessage::line).distinct().sorted().toList(), translation.messages().toString());
        assertTrue(preconditions.stream()
                .allMatch(m -> m.severity() == PrintMessage.Severity.INFO), preconditions.toString());
    }

    /**
     * With the program's nullability analysed, C# checks it: a local's reference type is T?, as var's is, and a
     * dereference of a value that may be null has a {@code !}.
     */
    @Test
    public void nullabilityAnalysed() {
        policy = CSharpProgram.Policy.DEFAULT.withFacts(new CSharpFacts() {
            @Override
            public NullableState nullable(FieldInfo field) {
                return CATALOG_FACTS.nullable(field);
            }

            @Override
            public NullableState nullable(MethodInfo method) {
                return CATALOG_FACTS.nullable(method);
            }

            @Override
            public NullableState nullable(ParameterInfo parameter) {
                return CATALOG_FACTS.nullable(parameter);
            }

            @Override
            public boolean nullabilityAnalysed() {
                return true;
            }
        });
        String cs = translate("Catalog", FACTS);
        contains(cs, "private string? description;");
        contains(cs, "string? d = description;");
        contains(cs, "int n = 0;");
        // a dereference of a value that may be null: Java throws there, C# too, and ! says so
        contains(cs, "Find(\"k\")!.Length + description!.Length");
        // a value that may be null into a non-null parameter, a list's element here
        contains(cs, "@out.Add(Find(\"k\")!);");
        contains(cs, "@out.Add(description!);");
        // Java's contains(Object) takes null, C#'s Contains(T) not: it is asserted whatever the hints
        contains(cs, "l.Contains(description!) || l.Contains(null!)");
        // an array's elements are not nullable in C#: the null they hold, as in Java, is asserted
        contains(cs, "{ description!, null! }");
        contains(cs, "a[1] = null!;");
        // a method whose result the analysis does not find nullable
        contains(cs, "(b ? description : null)!");
        // a cast keeps the null of a value that may be null
        contains(cs, "(string?) Find(\"k\")");
        // unboxing throws on null, in C# as in Java: an Integer is C#'s int? whatever its declaration's verdict
        contains(cs, "int a = (int) m.GetValueOrNull(\"k\")!;");
        contains(cs, "int b = (int) l[0];");
        contains(cs, "int c = (int) boxed!;");
        // a boxed key is unboxed into Dictionary<int, string>'s K: its cast asserts it, once
        contains(cs, "byId[(int) id!] = description!;");
        // a nullable parameter of a type parameter's type: T is inferred without ?, the result is not null
        contains(cs, "internal static T Ensure<T>(T? maybe) {");
        // a cast of null, which picks an overload, keeps the null
        contains(cs, "Over((string?) null);");
        // C#'s object.ToString() is string?
        contains(cs, "o.ToString()!.Length");
        // a class's type parameter takes the ? of a nullable declaration
        contains(cs, "internal T? Held { get; }");
    }

    @Language("java")
    private static final String LIBRARY_NULLS = """
            package org.example.lib;
            import java.util.Deque;
            import java.util.List;
            import java.util.Map;
            import java.util.Optional;
            class Lib {
                static int length(Map<String, String> m, String k) {
                    if (m.containsKey(k)) {
                        String v = m.get(k);
                        return v.length();
                    }
                    return 0;
                }
                @SuppressWarnings("unchecked")
                static <T> T attribute(Map<String, Object> m, String k) { return (T) m.get(k); }
                static List<String> values(List<String> keys, Map<String, String> m) {
                    return keys.stream().map(k -> m.get(k)).toList();
                }
                static int first(Deque<String> d) { return d.poll().length(); }
                static int index(Map<String, Integer> m, String k) {
                    if (m.containsKey(k)) {
                        int i = m.get(k);
                        return i;
                    }
                    return -1;
                }
                static Optional<String> find(List<String> l) { return l.stream().findFirst(); }
                static int found(List<String> l) { return find(l).get().length(); }
            }
            """;

    /**
     * A library member whose C# result is {@code T?} where Java's is not known to be nullable: the compatibility
     * library's {@code Map.Get} and {@code PollFirst} (a mapping file's {@code ->?}). Its value is asserted where it
     * goes into what is not nullable: a local the analysis finds not null after {@code containsKey}, a lambda's
     * result, a dereference, the operand of a cast to a method's type parameter, {@code (T) x!}. An Optional is
     * {@code T?}, whatever the analysis says of the Optional itself.
     */
    @Test
    public void nullableLibraryResults() {
        policy = CSharpProgram.Policy.DEFAULT.withFacts(new CSharpFacts() {
            @Override
            public NullableState nullable(MethodInfo method,
                    Element declaration,
                    LocalVariable variable) {
                return Set.of("v", "i").contains(variable.simpleName()) ? NullableState.NONNULL
                        : NullableState.NULLABLE;
            }

            @Override
            public boolean nullabilityAnalysed() {
                return true;
            }
        });
        String cs = translate("Lib", LIBRARY_NULLS);
        contains(cs, "string? v = m.Get(k)!;");
        contains(cs, "return v.Length;");
        contains(cs, "(T) m.Get(k)!;");
        contains(cs, "keys.Select(k => m.Get(k)!)");
        contains(cs, "d.PollFirst()!.Length");
        // a primitive local: the unboxing asserts, once
        contains(cs, "int i = (int) m.GetValueOrNull(k)!;");
        assertFalse(cs.contains("!!"), cs);
        // an empty Optional is null: Optional<T> is T?, and get() is asserted where it is dereferenced
        contains(cs, "internal static string? Find(List<string> l) => l.FirstOrDefault();");
        contains(cs, "Find(l)!.Length");
    }

    @Language("java")
    private static final String NULLABLE_FAMILY = """
            package org.example.family;
            import java.util.Iterator;
            class Selections {
                static abstract class Upcoming<S> implements Iterator<S> {
                    protected abstract S createUpcoming();
                    public boolean hasNext() { return true; }
                    public S next() { return createUpcoming(); }
                }
                static final class Filtering<S> extends Upcoming<S> {
                    private S last;
                    protected S createUpcoming() { return last; }
                }
                static abstract class Selection<S> implements Iterator<S> { }
                static final class Countdown extends Selection<String> {
                    public boolean hasNext() { return false; }
                    public String next() { return null; }
                }
            }
            """;

    /**
     * A method's result is nullable where an override's may be null: C# lets an override narrow T? to T, not widen
     * T to T? (CS8764), and the analysis decides each method alone.
     */
    @Test
    public void nullableFamily() {
        policy = CSharpProgram.Policy.DEFAULT.withFacts(new CSharpFacts() {
            @Override
            public NullableState nullable(MethodInfo method) {
                boolean nullable = "createUpcoming".equals(method.name()) && "Filtering".equals(method.typeInfo().simpleName())
                                   || "next".equals(method.name()) && "Countdown".equals(method.typeInfo().simpleName());
                return nullable ? NullableState.NULLABLE : NullableState.NONNULL;
            }

            @Override
            public boolean nullabilityAnalysed() {
                return true;
            }
        });
        String cs = translate("Selections", NULLABLE_FAMILY);
        contains(cs, "protected internal abstract S? CreateUpcoming();");
        contains(cs, "protected internal override S? CreateUpcoming() => last;");
        // the analysis has next() non-null: the call through the nullable base is asserted
        contains(cs, "public S Next() => CreateUpcoming()!;");
        // a compatibility interface's method, redeclared abstract, likewise
        contains(cs, "public abstract S? Next();");
    }

    @Language("java")
    private static final String NULLABLE_GROUPS = """
            package org.example.groups;
            import java.util.function.Function;
            import java.util.function.Supplier;
            class Replays {
                private Object last;
                Object replayed() { return last; }
                static Object initial() { return null; }
                Object peek() { return last; }
                Supplier<Object> fromThis() { return this::replayed; }
                static Supplier<Object> fromType() { return Replays::initial; }
                static Function<Replays, Object> unbound() { return Replays::peek; }
                Long tally() { return null; }
                static Function<Replays, Long> tallies() { return Replays::tally; }
            }
            """;

    /**
     * A method group of a nullable result where the delegate's is not (CS8621) is a lambda with !, where its receiver
     * is evaluated once anyway: a type, this, the lambda's own parameter.
     */
    @Test
    public void nullableGroups() {
        policy = CSharpProgram.Policy.DEFAULT.withFacts(new CSharpFacts() {
            @Override
            public NullableState nullable(MethodInfo method) {
                return Set.of("replayed", "initial", "peek").contains(method.name()) ? NullableState.NULLABLE
                        : NullableState.NONNULL;
            }

            @Override
            public boolean nullabilityAnalysed() {
                return true;
            }
        });
        String cs = translate("Replays", NULLABLE_GROUPS);
        contains(cs, "internal Func<object> FromThis() => () => this.Replayed()!;");
        contains(cs, "internal static Func<object> FromType() => () => Replays.Initial()!;");
        contains(cs, "internal static Func<Replays, object> Unbound() => (Replays p0) => p0.Peek()!;");
        // an unboxing cast, of C#'s long? (CS8629)
        contains(cs, "(Replays p0) => (long) p0.Tally()!;");
    }

    @Language("java")
    private static final String VALUE_NULLS = """
            package org.example.values;
            import java.math.BigDecimal;
            class Weights {
                enum Kind { PENALTY, REWARD }
                interface Builder { String build(Kind kind, BigDecimal weight); }
                static class Named implements Builder {
                    public String build(Kind kind, BigDecimal weight) { return "named"; }
                }
                static void validate(Number total) { }
                static void penalize(BigDecimal total) { validate(total); }
                static void named(String name) { }
                static int hash(Object o) { named(o.getClass().getName()); return o.getClass().getName().hashCode(); }
                static java.util.List<String> names(java.util.List<Class<?>> types) {
                    return types.stream().map(Class::getName).toList();
                }
            }
            """;

    /**
     * A decimal? or a Kind? is what C# takes to be possibly null whatever the analysis says of its declaration: into
     * an object or an IConvertible, and into a lambda class's delegate, it is asserted, as an int? is.
     */
    @Test
    public void valueNulls() {
        policy = CSharpProgram.Policy.DEFAULT.withFacts(new CSharpFacts() {
            @Override
            public boolean nullabilityAnalysed() {
                return true;
            }
        });
        String cs = translate("Weights", VALUE_NULLS);
        contains(cs, "internal static void Penalize(decimal? total) { Validate(total!); }");
        contains(cs, "public string Build(Kind? kind, decimal? weight) => f((Kind) kind!, (decimal) weight!);");
        // Type.FullName is a string? in C#, never null in Java's getName()
        contains(cs, "named(o.GetType().FullName!);");
        contains(cs, "return o.GetType().FullName!.GetHashCode();");
        contains(cs, "types.Select((Type p0) => p0.FullName!).ToList();");
    }

    @Language("java")
    private static final String OPEN_NULLS = """
            package org.example.open;
            import jakarta.xml.bind.Marshaller;
            import jakarta.xml.bind.ValidationEvent;
            import java.io.Writer;
            import java.util.Iterator;
            import java.util.List;
            class Open<T> implements Iterable<String> {
                private Iterator<String> source;
                public Iterator<String> iterator() { return source; }
                void write(Marshaller m, T root, Writer w) throws Exception { m.marshal(root, w); }
                void keep(List<T> l, T t) { l.add(t); }
                static String where(ValidationEvent e) { return e.getLocator().getNode().getNodeName(); }
                static <N extends Event> Box<N> recast(Box<? extends Event> b) { return new Box<>(); }
            }
            interface Event {
            }
            class Box<E extends Event> {
            }
            """;

    /**
     * What C# takes to be possibly null beyond the analysis's verdicts: the iterator a class's enumerator walks, a
     * value of an unconstrained type parameter into {@code object}, a library result C# declares nullable
     * ({@code GetNode()}); and a nullable parameter whose wildcards are the method's type parameters.
     */
    @Test
    public void openNulls() {
        policy = CSharpProgram.Policy.DEFAULT.withFacts(new CSharpFacts() {
            @Override
            public NullableState nullable(MethodInfo method) {
                return "iterator".equals(method.name()) ? NullableState.NULLABLE : NullableState.NONNULL;
            }

            @Override
            public NullableState nullable(ParameterInfo parameter) {
                return "recast".equals(parameter.methodInfo().name()) ? NullableState.NULLABLE : NullableState.NONNULL;
            }

            @Override
            public boolean nullabilityAnalysed() {
                return true;
            }
        });
        String cs = translate("Open", OPEN_NULLS);
        contains(cs, "GetEnumerator() => Iterator()!.AsEnumerator();");
        contains(cs, "m.Marshal(root!, w);");
        // into a type parameter, C# does not warn
        contains(cs, "l.Add(t);");
        contains(cs, "e.GetLocator().GetNode()!.Name");
        // a wildcard is a method's type parameter in C#; the parameter keeps its ?
        contains(cs, "Recast<N, TE>(Box<TE>? b)");
    }
}
