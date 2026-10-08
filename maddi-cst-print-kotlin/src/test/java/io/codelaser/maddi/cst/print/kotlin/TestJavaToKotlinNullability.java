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

package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The printer's side of nullability: verdicts (here by hand, in production maddi-mod's NullabilityPass) put the
 * {@code ?} on declarations, and the {@link KotlinPrintOptions.NullCheck} policy writes {@code !!} or {@code ?.}
 * where a value Kotlin types nullable is used as non-null.
 */
public class TestJavaToKotlinNullability extends CommonJavaToKotlin {

    private static void contains(String kotlin, String expected) {
        assertTrue(strip(kotlin).contains(strip(expected)), () -> "expected\n" + expected + "\nin\n" + kotlin);
    }

    private static String strip(String s) {
        return s.lines().map(String::strip).collect(Collectors.joining("\n"));
    }

    /** NULLABLE for the declarations named here: fields and parameters by name, returns by method, locals by name. */
    private record ByName(Set<String> nullable, Set<String> checked) implements NullabilityVerdicts {
        ByName(Set<String> nullable) {
            this(nullable, Set.of());
        }

        /** In a call that is an operand of {@code &&}, as {@code v.m()} in {@code v != null && v.m()}. */
        @Override
        public boolean nonNullAt(io.codelaser.maddi.cst.api.expression.Expression expression,
                                 io.codelaser.maddi.cst.api.variable.Variable variable) {
            return checked.contains("&&" + variable.simpleName());
        }

        /** Known non-null in every statement but the one that checks it, as after {@code if (v == null) return;}. */
        @Override
        public boolean nonNullAt(io.codelaser.maddi.cst.api.statement.Statement statement,
                                 io.codelaser.maddi.cst.api.variable.Variable variable) {
            return checked.contains(variable.simpleName())
                   && !(statement instanceof io.codelaser.maddi.cst.api.statement.IfElseStatement);
        }

        /**
         * {@code "x"}: x is nullable; {@code "x[]"}: x's elements are; {@code "x<>"}, {@code "x<,>"}: its first, second
         * type argument; {@code "x<<>>"}: its first type argument's first.
         */
        private ParameterizedType verdict(String name, ParameterizedType type) {
            ParameterizedType verdict = nullable.contains(name) ? type.withNullable(NullableState.NULLABLE) : null;
            if (nullable.contains(name + "[]")) {
                ParameterizedType array = verdict != null ? verdict : type;
                verdict = array.withComponentType(array.componentType().withNullable(NullableState.NULLABLE));
            }
            for (int i = 0; i < 2; i++) {
                if (!nullable.contains(name + (i == 0 ? "<>" : "<,>"))) continue;
                ParameterizedType generic = verdict != null ? verdict : type;
                java.util.List<ParameterizedType> arguments = new java.util.ArrayList<>(generic.parameters());
                arguments.set(i, arguments.get(i).withNullable(NullableState.NULLABLE));
                verdict = generic.withParameters(arguments);
            }
            if (nullable.contains(name + "<<>>")) {
                ParameterizedType generic = verdict != null ? verdict : type;
                java.util.List<ParameterizedType> arguments = new java.util.ArrayList<>(generic.parameters());
                ParameterizedType inner = arguments.getFirst();
                java.util.List<ParameterizedType> innerArguments = new java.util.ArrayList<>(inner.parameters());
                innerArguments.set(0, innerArguments.getFirst().withNullable(NullableState.NULLABLE));
                arguments.set(0, inner.withParameters(innerArguments));
                verdict = generic.withParameters(arguments);
            }
            return verdict;
        }

        @Override
        public ParameterizedType field(FieldInfo fieldInfo) {
            return verdict(fieldInfo.name(), fieldInfo.type());
        }

        @Override
        public ParameterizedType parameter(ParameterInfo parameterInfo) {
            return verdict(parameterInfo.name(), parameterInfo.parameterizedType());
        }

        @Override
        public ParameterizedType returnType(MethodInfo methodInfo) {
            return verdict(methodInfo.name() + "()", methodInfo.returnType());
        }

        /** {@code "!x"} in checked: local x is asserted at its declaration. */
        @Override
        public boolean assertedAtDeclaration(MethodInfo method, Element declaration, LocalVariable variable) {
            return checked.contains("!" + variable.simpleName());
        }

        @Override
        public ParameterizedType local(MethodInfo method, Element declaration, LocalVariable variable) {
            // "undecided*": a verdict that is no decision, as the pass gives for a degraded method's unreached local
            if (variable.simpleName().startsWith("undecided")) return variable.parameterizedType();
            return verdict(variable.simpleName(), variable.parameterizedType());
        }
    }

    @Language("java")
    private static final String EARLY = """
            package a;
            import java.util.*;
            class D {
                final List<String> seen = new ArrayList<>();
                String find(String k) { return k.isEmpty() ? null : k; }
                int early(String k) {
                    String s = find(k);
                    seen.add(s);
                    return s.length();
                }
                int late(String k) {
                    String t = find(k);
                    return t.length();
                }
            }
            """;

    /**
     * A local the pass asserts at its declaration, because Java dereferences it unconditionally further on: the `!!`
     * is the declaration's, a behaviour change of its own (the null no longer reaches `seen`), and reported as such.
     */
    @Test
    public void assertedAtDeclaration() {
        KotlinPrintOptions options = new KotlinPrintOptions(new ByName(Set.of("find()"), Set.of("!s")),
                KotlinPrintOptions.NullCheck.ASSERT);
        String kotlin = kotlin(EARLY, options);
        contains(kotlin, "val s = find(k)!!");
        contains(kotlin, "seen.add(s)");
        List<KotlinPrintMessage> messages = messages(EARLY.replace("class D", "class D2"), options);
        assertTrue(messages.stream().anyMatch(m -> m.code() == KotlinPrintMessage.Code.ASSERT_AT_DECLARATION
                                                   && m.line() == 7), messages.toString());
        // t is not asserted at its declaration: its `!!`, if any, is an ordinary one
        assertTrue(messages.stream().noneMatch(m -> m.code() == KotlinPrintMessage.Code.ASSERT_AT_DECLARATION
                                                    && m.line() == 12), messages.toString());
    }

    @Language("java")
    private static final String NULLS = """
            package a;
            import java.util.Map;
            class C {
                String name;
                void use(String s) { }
                String find(Map<String, String> m, String k) { return m.get(k); }
                int len(Map<String, String> m, String k) { return m.get(k).length(); }
                void m(Map<String, String> m) {
                    use(name);
                    String x = null;
                    x = "a";
                    String y = m.get("k");
                    use(y);
                    for (String s : names()) use(s);
                    String undecided = "u";
                    int undecidedCount = 0;
                }
                java.util.List<String> names() { return null; }
            }
            class D {
                D(java.util.List<String> in) {
                    String x = null;
                    for (String s : in) x = s;
                }
            }
            """;

    private static final KotlinPrintOptions VERDICTS = new KotlinPrintOptions(
            new ByName(Set.of("name", "find()", "x", "names()")), KotlinPrintOptions.NullCheck.ASSERT);

    @Test
    public void declarationsAndAssertions() {
        String kotlin = kotlin(NULLS, VERDICTS);
        contains(kotlin, "var name: String? = null");
        contains(kotlin, "open fun find(m: MutableMap<String, String>, k: String): String? = m.get(k)");
        contains(kotlin, "open fun len(m: MutableMap<String, String>, k: String): Int = m.get(k)!!.length");
        contains(kotlin, "use(name!!)");
        contains(kotlin, "var x: String? = null");
        // no verdict for y: Kotlin infers String? from Map.get, so its use as a non-null argument is asserted
        contains(kotlin, "val y = m.get(\"k\")");
        contains(kotlin, "for (s in names()!!) {");
        // an UNSPECIFIED local verdict is no decision: nullable, except for a primitive
        // declared, then assigned (a val all the same): Kotlin smart-casts after an assignment, not after a typed declaration's initializer
        contains(kotlin, """
                val undecided: String?
                undecided = "u"
                """);
        contains(kotlin, "val undecidedCount = 0");
        // a constructor body is an init block: its locals get their verdicts too
        contains(kotlin, """
                init {
                    var x: String? = null
                """); // Kotlin does not loop over a nullable collection
    }

    @Test
    public void safeCall() {
        String kotlin = kotlin(NULLS, new KotlinPrintOptions(VERDICTS.verdicts(), KotlinPrintOptions.NullCheck.SAFE_CALL));
        contains(kotlin, "m.get(k)?.length");
        contains(kotlin, "use(name!!)"); // not a receiver: still asserted
    }

    @Language("java")
    private static final String BOXED = """
            package a;
            class V {
                final int a;
                V(int a, int b) { this.a = a + b; }
                V(Integer a, Integer b) { this(a.intValue(), b.intValue()); }
            }
            """;

    /** Java's int and Integer overloads are both Int in Kotlin: the boxed one takes Int?. */
    @Test
    public void boxedOverloads() {
        String kotlin = kotlin(BOXED);
        contains(kotlin, "constructor(a: Int, b: Int)");
        contains(kotlin, "constructor(a: Int?, b: Int?) : this(a!!.toInt(), b!!.toInt())");
        assertFalse(kotlin.contains("constructor(a: Int, b: Int) : this"), kotlin);
    }

    @Language("java")
    private static final String CHECKED = """
            package a;
            class C {
                int m(String p, String q) {
                    if (p == null) return 0;
                    return p.length() + q.length();
                }
            }
            """;

    /** A use-site fact: after the null check, Kotlin smart-casts p, and the printer writes no `!!`. */
    @Test
    public void useSiteFacts() {
        String kotlin = kotlin(CHECKED, new KotlinPrintOptions(new ByName(Set.of("p", "q"), Set.of("p")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "open fun m(p: String?, q: String?): Int {");
        contains(kotlin, "return p.length + q!!.length");
    }

    @Language("java")
    private static final String ARRAYS = """
            package a;
            class C {
                String[] names;
                int first() { return names[0].length(); }
            }
            """;

    /** A nullable array is indexed with `!!`; its elements are not nullable for it (no element verdict yet). */
    @Test
    public void nullableArray() {
        String kotlin = kotlin(ARRAYS, new KotlinPrintOptions(new ByName(Set.of("names")), KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "var names: Array<String>? = null");
        contains(kotlin, "names!![0].length");
    }

    @Language("java")
    private static final String CONDITION = """
            package a;
            class C {
                boolean m(String v) { return v != null && v.isEmpty(); }
            }
            """;

    /** A per-expression fact: within one statement, the call after the null test needs no `!!`. */
    @Test
    public void expressionFacts() {
        String kotlin = kotlin(CONDITION, new KotlinPrintOptions(new ByName(Set.of("v"), Set.of("&&v")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "v != null && v.isEmpty()");
    }

    @Language("java")
    private static final String ELEMENTS = """
            package a;
            class D {
                private final int[][] table = { null, {1, 2} };
                private String label;
                private final String fixed;
                D(String fixed, String label) { this.fixed = fixed; this.label = label; }
                Object[] pair(String s) { return new Object[]{null, s}; }
                int m(int i) {
                    int[] row = table[i];
                    int n = table[i].length;
                    if (label != null) n += label.length();
                    if (fixed != null) n += fixed.length();
                    return n + row.length;
                }
            }
            """;

    /**
     * An array's elements have their own state: {@code Array<IntArray?>}, and an element read is nullable in Kotlin,
     * which never smart-casts it. Nor does Kotlin smart-cast a {@code var} property, whatever the fact says; a
     * {@code val} one it does.
     */
    @Test
    public void arrayElementsAndProperties() {
        String kotlin = kotlin(ELEMENTS, new KotlinPrintOptions(
                new ByName(Set.of("table[]", "row", "label", "fixed", "pair()[]"), Set.of("label", "fixed")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "private val table: Array<IntArray?> = arrayOf<IntArray?>(null, intArrayOf(1, 2))");
        contains(kotlin, "table[i]!!.size");
        contains(kotlin, "label!!.length");
        contains(kotlin, "n += fixed.length");
        contains(kotlin, "row!!.size");
        // an array created with its elements takes the element state of the type it is returned as
        contains(kotlin, "fun pair(s: String): Array<Any?> = arrayOf<Any?>(null, s)");
    }

    @Language("java")
    private static final String CONTENT = """
            package a;
            import java.util.*;
            class E {
                static class Keyed<T> { T find(String k) { return null; } }
                static class Bag<T> { void put(T t) { } T take() { throw new UnsupportedOperationException(); } }
                static class Sub<X> extends Bag<X> { }
                private final Sub<String> loose = new Sub<>();
                private final Sub<String> strict = new Sub<>();
                private final Keyed<String> keyed = new Keyed<>();
                private final List<String> names = new ArrayList<>();
                private final List<String> maybe = new ArrayList<>();
                private final List<List<String>> rows = new ArrayList<>();
                private final String[] slots = new String[2];
                int m(Map<String, String> map, String k) {
                    rows.add(new ArrayList<>());
                    rows.get(0).add(map.get(k));
                    int checked = Objects.requireNonNull(maybe.get(0)).length() + Objects.requireNonNull(names.get(0)).length();
                    names.add(map.get(k));
                    maybe.add(map.get(k));
                    slots[0] = map.get(k);
                    loose.put(map.get(k));
                    strict.put(map.get(k));
                    int inherited = loose.take().length() + strict.take().length();
                    return maybe.get(0).length() + names.get(0).length() + keyed.find(k).length();
                }
            }
            """;

    /**
     * Content takes its state from the type argument or element type: a nullable value is asserted where it is
     * written into a non-null slot ({@code List<String>}, {@code Array<String>}), and passes into a nullable one
     * ({@code List<String?>}), whose reads are then asserted where they are dereferenced.
     */
    @Test
    public void containerContent() {
        String kotlin = kotlin(CONTENT, new KotlinPrintOptions(new ByName(Set.of("maybe<>", "find()", "loose<>", "rows<<>>")),
                KotlinPrintOptions.NullCheck.ASSERT));
        // Kotlin's generics are invariant: the constructor call takes the declaration's states
        contains(kotlin, "private val maybe: MutableList<String?> = ArrayList<String?>()");
        contains(kotlin, "private val names: MutableList<String> = ArrayList<String>()");
        // ...at any depth: ArrayList<MutableList<String>> is no MutableList<MutableList<String?>> (fernflower: 25)
        contains(kotlin, "private val rows: MutableList<MutableList<String?>> = ArrayList<MutableList<String?>>()");
        // Objects.requireNonNull(x): T is inferred nullable from a nullable x, so the call is x!!
        contains(kotlin, "val checked = maybe.get(0)!!.length + names.get(0).length");
        contains(kotlin, "names.add(map.get(k)!!)");
        contains(kotlin, "maybe.add(map.get(k))");
        contains(kotlin, "slots[0] = map.get(k)!!");
        contains(kotlin, "maybe.get(0)!!.length + names.get(0).length");
        // a member's own '?' survives its receiver's non-null type argument: find(k): T? on a Keyed<String>
        contains(kotlin, "keyed.find(k)!!.length");
        // an inherited member is seen through the supertype that declares it: Sub<String?> calling Bag.put(T)
        contains(kotlin, "loose.put(map.get(k))");
        contains(kotlin, "strict.put(map.get(k)!!)");
        contains(kotlin, "loose.take()!!.length + strict.take().length");
    }

    @Language("java")
    private static final String TARGETS = """
            package a;
            import java.util.*;
            class G {
                private final Map<String, Map<String, Integer>> nested = new HashMap<>();
                private List<String> lst;
                private List<String> pairs;
                Map<String, Integer> m(String k, List<String> src) {
                    pairs = src != null ? new ArrayList<>(src) : null;
                    use(lst = new ArrayList<>());
                    return nested.computeIfAbsent(k, x -> new HashMap<>());
                }
                static void use(List<String> l) { }
            }
            """;

    /** A constructed type takes its target's states through a conditional, an assignment's value, a lambda's result. */
    @Test
    public void constructorTargets() {
        String kotlin = kotlin(TARGETS, new KotlinPrintOptions(new ByName(Set.of("lst", "lst<>", "pairs", "pairs<>")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "pairs = if (src != null) ArrayList<String?>(src) else null");
        contains(kotlin, "ArrayList<String?>().also { lst = it }");
        // the diamond as the lambda's result: Kotlin infers HashMap's arguments, the map's states included
        contains(kotlin, "{ x -> HashMap() }");
    }

    @Language("java")
    private static final String ITERATOR = """
            package a;
            import java.util.*;
            class It<E> implements Iterator<E> {
                private final List<E> items = new ArrayList<>();
                private int pos;
                public boolean hasNext() { return true; }
                public E next() { return pos < items.size() ? items.get(pos++) : null; }
            }
            """;

    /**
     * fernflower's FastSparseSetIterator: next() really returns null, but Kotlin's Iterator<E>.next(): E cannot be
     * overridden by E?. The override keeps E and casts the null, unchecked, as Java does; a `!!` would throw.
     */
    @Test
    public void nullForATypeVariable() {
        String kotlin = kotlin(ITERATOR, new KotlinPrintOptions(new ByName(Set.of("next()")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "override fun next(): E");
        contains(kotlin, ") as E");
        assertFalse(kotlin.contains("next(): E?"), kotlin);
    }

    @Language("java")
    private static final String USES = """
            package a;
            import java.util.*;
            class F {
                private final List<String> maybe = new ArrayList<>();
                private final Queue<String> queue = new ArrayDeque<>();
                private String label;
                static void take(List<String> list) { }
                static class Node { final String value; Node(String value) { this.value = value; } }
                private final Map<String, String> values = new HashMap<>();
                static void use(String s) { }
                int m(Map<String, Integer> map, String k) {
                    int n = 0;
                    for (String s : maybe) n += s.length();
                    List<String> copy = maybe;
                    for (String t : copy) n += t.length();
                    queue.add(label);
                    take(new ArrayList<>());
                    Node node = new Node(label);
                    for (Map.Entry<String, String> entry : values.entrySet()) use(entry.getValue());
                    String[] pair = { label, "x" };
                    long longOnes = maybe.stream().filter(s -> s.length() > 3).count();
                    maybe.forEach(s -> use(s));
                    return n + map.get(k) + 1;
                }
                @Override public String toString() { return label; }
            }
            """;

    /**
     * Where Kotlin decides a type the printer cannot write: a loop variable is typed by what it loops over, an
     * operator has no nullable operand, a library member's type argument is ours, and an override of a Kotlin
     * member keeps its non-null result.
     */
    @Test
    public void usesKotlinTypes() {
        String kotlin = kotlin(USES, new KotlinPrintOptions(
                new ByName(Set.of("maybe<>", "label", "toString()", "list<>", "value", "values<,>")), KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "for (s in maybe) { n += s!!.length }");
        // an untyped local has its initializer's type: copy is a MutableList<String?> too
        contains(kotlin, "for (t in copy) { n += t!!.length }");
        contains(kotlin, "queue.add(label!!)");
        contains(kotlin, "take(ArrayList<String?>())");
        contains(kotlin, "return n + map.get(k)!! + 1");
        // a primary-constructor property has its field's type, and its argument is checked against it
        contains(kotlin, "class Node(val value: String?)");
        // through a nested type argument: map.entries is a Set<Entry<String, String?>>
        contains(kotlin, "for (entry in values.entries) {use(entry.value!!) }");
        contains(kotlin, "val node = Node(label)");
        // a lambda's parameters are typed by the call: a Stream<String?> filters String?s
        contains(kotlin, ".filter( { s -> s!!.length > 3 })");
        contains(kotlin, "maybe.forEach( { s -> use(s!!) })");
        // array elements into a non-null element type are asserted
        contains(kotlin, "arrayOf<String>(label!!, \"x\")");
        contains(kotlin, "override fun toString(): String = label!!");
    }
}
