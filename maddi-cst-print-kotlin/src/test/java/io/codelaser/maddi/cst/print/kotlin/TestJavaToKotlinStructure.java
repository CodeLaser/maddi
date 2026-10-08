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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step 3 of the Java-to-Kotlin translation: where Java and Kotlin put things differently. Statics, constructors,
 * records, nested classes, pattern variables, the JDK types Kotlin maps to its own, functional interfaces, and the
 * implicit conversions Java does and Kotlin does not.
 */
public class TestJavaToKotlinStructure extends CommonJavaToKotlin {

    private static void contains(String kotlin, String expected) {
        assertTrue(strip(kotlin).contains(strip(expected)), () -> "expected\n" + expected + "\nin\n" + kotlin);
    }

    private static String strip(String s) {
        return s.lines().map(String::strip).collect(Collectors.joining("\n"));
    }

    @Language("java")
    private static final String STATICS = """
            package a;
            class Base {
                public static final int TYPE_INT = 4;
                protected static int helper(int x) { return x + 1; }
            }
            class C extends Base {
                static final String NAME = "c";
                private static int count;
                static { count = 1; }
                int m(String s) { return TYPE_INT + helper(count) + Integer.parseInt(s) + NAME.length(); }
            }
            """;

    /** Kotlin inherits no statics: they go to a companion object, and one inherited from a supertype is qualified. */
    @Test
    public void staticsInCompanion() {
        String kotlin = kotlin(STATICS);
        contains(kotlin, "const val TYPE_INT: Int = 4");
        // Java's protected is also package access, which Kotlin's is not: public, as package-private is
        contains(kotlin, "    fun helper(x: Int): Int = x + 1");
        contains(kotlin, """
                companion object {
                    const val NAME: String = "c"
                    private var count: Int = 0
                    init { count = 1 }
                }
                """);
        contains(kotlin, "Base.TYPE_INT + Base.helper(count) + java.lang.Integer.parseInt(s) + NAME.length");
    }

    @Language("java")
    private static final String CONSTRUCTORS = """
            package a;
            class Base { Base(int x) { } }
            class C extends Base {
                private final int size;
                private final String label;
                private final int doubled = size * 2;
                C(int size, String name) {
                    super(size + 1);
                    this.size = size;
                    label = name.trim();
                }
                class Inner { int get() { return size; } }
            }
            record Point(int x, int y) {
                Point { if (x < 0) throw new IllegalArgumentException(); }
                int sum() { return x() + y; }
            }
            """;

    /**
     * One constructor is the primary one, its body an {@code init} block, which may assign vals; the parameters are in
     * scope in property initializers, where the Java meant the field. A record is a data class.
     */
    @Language("java")
    private static final String VISIBILITY = """
            package a;
            import java.util.*;
            class V {
                static class Base extends AbstractList<String> {
                    protected final List<String> items = new ArrayList<>();
                    protected Base() { }
                    public String get(int i) { return items.get(i); }
                    public int size() { return items.size(); }
                    @Override protected void removeRange(int from, int to) { }
                }
                static class Copy {
                    final int n;
                    Copy(int n) { this.n = n; }
                    @Override public Copy clone() { return new Copy(n); }
                }
                abstract static class Placeholder {
                    @Override public Placeholder clone() { return null; }
                }
                static int sibling(Base b) { return b.items.size(); }
            }
            """;

    @Language("java")
    private static final String COLLECTION = """
            package a;
            import java.util.*;
            class Coll<E> extends ArrayList<E> {
                @Override public boolean remove(Object o) { return super.remove(o); }
                @Override public E remove(int i) { return super.remove(i); }
                @Override public boolean addAll(Collection<? extends E> c) { return super.addAll(c); }
                @Override public int size() { return super.size(); }
                @Override public boolean contains(Object o) { return super.contains(o); }
                static class Entry implements Map.Entry<String, Integer> {
                    public String getKey() { return "k"; }
                    public Integer getValue() { return 1; }
                    public Integer setValue(Integer v) { return v; }
                }
            }
            """;

    /** Overrides of Java collection members take Kotlin's mapped signatures, which are the ones it can override. */
    @Test
    public void collectionOverrides() {
        String kotlin = kotlin(COLLECTION);
        contains(kotlin, "override fun remove(o: E): Boolean");
        contains(kotlin, "override fun removeAt(i: Int): E");
        contains(kotlin, "override fun addAll(c: Collection<E>): Boolean");
        contains(kotlin, "override val size: Int get() = super.size");
        contains(kotlin, "override fun contains(o: E): Boolean");
        contains(kotlin, "override val key: String get() = \"k\"");
        contains(kotlin, "override val value: Int get() = 1");
        contains(kotlin, "override fun setValue(v: Int): Int");
    }

    @Language("java")
    private static final String RAW_ARRAYS = """
            package a;
            class R {
                static class Box<T> { }
                static final Box[] EMPTY = new Box[0];
                private final Box<Integer>[][] grid = new Box[3][];
                int m() {
                    Box[][] g = grid;
                    Box<Integer>[] row = new Box[2];
                    g[0] = row;
                    Box<Integer>[] empty = EMPTY;
                    g[1] = EMPTY;
                    return g.length + empty.length;
                }
            }
            """;

    /** Kotlin has no raw types and its arrays are invariant: a raw array creation or local takes the typed side's arguments. */
    @Test
    public void rawArrays() {
        String kotlin = kotlin(RAW_ARRAYS);
        // arrayOfNulls is an Array<X?>; the declarations' elements are non-null (filled before they are read)
        contains(kotlin, "private val grid: Array<Array<Box<Int>>> = (arrayOfNulls<Array<Box<Int>>>(3) as Array<Array<Box<Int>>>)");
        contains(kotlin, "val row: Array<Box<Int>> = (arrayOfNulls<Box<Int>>(2) as Array<Box<Int>>)");
        contains(kotlin, "val g = grid");
        // a raw value where a typed one is expected: Java's unchecked conversion, Kotlin's unchecked cast
        contains(kotlin, "val empty: Array<Box<Int>> = (EMPTY as Array<Box<Int>>)");
        contains(kotlin, "g[1] = (EMPTY as Array<Box<Int>>)");
    }

    @Language("java")
    private static final String GENERIC_ARRAYS = """
            package a;
            import java.util.Map;
            class G {
                static final Map<Integer, Integer[]> M = Map.of(1, new Integer[]{1, 2}, 2, new Integer[]{null, 3});
            }
            """;

    /** An array for a method's type parameter: one V for all of Map.of's values, so no arrayOf<Int> fixes one of them. */
    @Test
    public void arraysForMethodTypeParameters() {
        String kotlin = kotlin(GENERIC_ARRAYS);
        contains(kotlin, "Map.of(1, arrayOf(1, 2), 2, arrayOf(null, 3))");
    }

    @Language("java")
    private static final String UNIMPORTED = """
            package a;
            import java.util.ArrayList;
            import javax.net.ssl.SSLParameters;
            class U {
                void m(SSLParameters params) {
                    params.setServerNames(new ArrayList<>());
                }
            }
            """;

    /** The diamond's argument is written out in Kotlin: a type the Java file never named, so never imported. */
    @Test
    public void importsWhatOnlyKotlinNames() {
        String kotlin = kotlin(UNIMPORTED);
        contains(kotlin, "import javax.net.ssl.SNIServerName");
        contains(kotlin, "ArrayList<SNIServerName>()");
    }

    @Language("java")
    private static final String TARGET_TYPED = """
            package a;
            import java.util.*;
            class W {
                List<String> m(boolean b) {
                    List<String> elements = Collections.emptyList();
                    if (b) elements = new ArrayList<>();
                    return elements;
                }
                Comparator<String> c() {
                    return Comparator.comparingLong(s -> s.isEmpty() ? s.length() : Long.MIN_VALUE);
                }
            }
            """;

    /** What Java infers from the target: a call's type arguments, a lambda result's widening to the interface's long. */
    @Test
    public void typedByTheTarget() {
        String kotlin = kotlin(TARGET_TYPED);
        contains(kotlin, "var elements: MutableList<String> = Collections.emptyList()");
        contains(kotlin, "s.length.toLong()");
    }

    @Language("java")
    private static final String BOUNDS = """
            package a;
            import java.util.Collection;
            class Bounded<N extends Number> {
                <T extends Collection<String>> T fill(T c) {
                    c.add("x");
                    return c;
                }
            }
            """;

    /** A bound is what makes its members callable on a T. */
    @Test
    public void typeParameterBounds() {
        String kotlin = kotlin(BOUNDS);
        contains(kotlin, "class Bounded<N : Number>");
        contains(kotlin, "fun <T : MutableCollection<String>> fill(c: T): T");
    }

    @Language("java")
    private static final String UNBOXING = """
            package a;
            import java.util.*;
            class N {
                int low(Map<String, Integer> m, String k) { return Math.min(m.get(k), 3); }
                boolean both(boolean b, Map<String, Boolean> m, String k) { return b && m.get(k); }
                int count(Map<String, Integer> m, String name) {
                    Integer counter = m.get(name);
                    m.put(name, counter == null ? counter = 0 : ++counter);
                    return counter;
                }
            }
            """;

    /** Java unboxes into a library's int and into &&; an assignment as a branch's value keeps its smart casts. */
    @Test
    public void unboxingAndBranchAssignments() {
        String kotlin = kotlin(UNBOXING);
        contains(kotlin, "Math.min(m.get(k)!!, 3)");
        contains(kotlin, "b && m.get(k)!!");
        contains(kotlin, "if (counter == null) { counter = 0; counter } else ++counter");
    }

    @Language("java")
    private static final String LOOKUPS = """
            package a;
            import java.util.*;
            class L {
                boolean has(Set<String> s, Map<String, Integer> m, List<String> l, Map<String, String> names, String k) {
                    return s.contains(names.get(k)) || m.get(names.get(k)) != null || m.containsKey(names.get(k))
                           || l.indexOf(names.get(k)) >= 0 || s.remove(names.get(k));
                }
                boolean kept(Set<Object> objects, Map<String, String> names, String k) {
                    return objects.contains(names.get(k));
                }
            }
            """;

    /** A lookup with a nullable key: Java's takes an Object and finds nothing, Kotlin's stdlib extension takes a K?. */
    @Test
    public void nullableLookups() {
        String kotlin = kotlin(LOOKUPS);
        contains(kotlin, "s.contains(names.get(k))");
        contains(kotlin, "m.get(names.get(k)) != null");
        contains(kotlin, "m.containsKey(names.get(k))");
        contains(kotlin, "l.indexOf(names.get(k))");
        contains(kotlin, "s.remove(names.get(k))");
        // an argument of a narrower type keeps its `!!`: Kotlin's extension cannot infer its T from a Set<Any> and a
        // String?
        contains(kotlin, "names.get(k)!!)");
    }

    @Language("java")
    private static final String NON_NULL_FILTER = """
            package a;
            import java.util.*;
            class F {
                boolean any(List<String> names, Map<String, List<String>> m, String x) {
                    return names.stream().map(m::get).filter(Objects::nonNull).anyMatch(l -> l.contains(x))
                           || names.stream().map(m::get).filter(l -> l != null).anyMatch(l -> l.isEmpty());
                }
            }
            """;

    /** After Java's filter(Objects::nonNull) the elements are non-null; Kotlin's stream needs a map { it!! } to know. */
    @Test
    public void nonNullFilter() {
        String kotlin = kotlin(NON_NULL_FILTER);
        contains(kotlin, "filter(Objects ::nonNull).map { it!! }");
        contains(kotlin, "filter( { l -> l != null }).map { it!! }");
    }

    @Language("java")
    private static final String HOISTED = """
            package a;
            import java.util.*;
            class H {
                void m(Map<Integer, List<Integer>> map, int id) {
                    List<Integer> lst = map.get(id);
                    if (lst == null) map.put(id, lst = new ArrayList<>());
                    lst.add(id);
                }
                int n(Map<Integer, List<Integer>> map, int id) {
                    List<Integer> lst;
                    return map.put(id, lst = new ArrayList<>()) == null ? lst.size() : 0;
                }
            }
            """;

    /** An assignment as an argument of a statement call goes first: as a value it is an also { }, which kills smart casts. */
    @Test
    public void hoistedAssignments() {
        String kotlin = kotlin(HOISTED);
        contains(kotlin, """
                        lst = ArrayList<Int>()
                        map.put(id, lst)
                """);
        // not a statement: the value form stays
        contains(kotlin, ".also { lst = it }");
    }

    @Language("java")
    private static final String NULL_OVERRIDES = """
            package a;
            import java.util.*;
            class E {
                Map.Entry<Integer, String> entry(int k, String v) {
                    return new Map.Entry<>() {
                        public Integer getKey() { return k; }
                        public String getValue() { return v; }
                        public String setValue(String value) { return null; }
                    };
                }
                void sort(List<Integer> ids, HashMap<Integer, Integer> order) {
                    ids.sort(Comparator.comparing(order::get));
                }
            }
            """;

    /**
     * Kotlin's Map.Entry.setValue returns a non-null V: a stub returning null throws UnsupportedOperationException, as
     * Java's contract allows. A key extractor with a nullable result gets its `!!`: Java throws comparing the null.
     */
    @Test
    public void nullOverridesAndComparingKeys() {
        String kotlin = kotlin(NULL_OVERRIDES);
        contains(kotlin, "override fun setValue(value: String): String = throw UnsupportedOperationException()");
        contains(kotlin, "Comparator.comparing( { order.get(it)!! })");
    }

    @Test
    public void protectedAndClone() {
        String kotlin = kotlin(VISIBILITY);
        // protected Java members are package-visible: public in Kotlin, a library override keeps its protected
        contains(kotlin, "val items: MutableList<String> = ArrayList<String>()");
        assertFalse(kotlin.contains("protected val items"), kotlin);
        assertFalse(kotlin.contains("protected constructor"), kotlin);
        contains(kotlin, "protected override fun removeRange(from: Int, to: Int)");
        // a clone() of Object's: Kotlin's Any declares none, kotlin.Cloneable does
        contains(kotlin, "class Copy(val n: Int) : Cloneable {");
        contains(kotlin, "override fun clone(): Copy");
        // Kotlin's clone() cannot return null: the deliberate behaviour change is an exception instead
        contains(kotlin, "override fun clone(): Placeholder = throw CloneNotSupportedException()");
    }

    @Test
    public void constructorsAndRecords() {
        String kotlin = kotlin(CONSTRUCTORS);
        contains(kotlin, "open class C(size: Int, name: String) : Base(size + 1) {");
        contains(kotlin, "private val doubled: Int = this.size * 2");
        contains(kotlin, """
                init {
                    this.size = size
                    label = name.trim()
                }
                """);
        contains(kotlin, "open inner class Inner {");
        contains(kotlin, "data class Point(val x: Int, val y: Int) {");
        contains(kotlin, "init { if (x < 0) {"); // the compact constructor's body
        contains(kotlin, "throw IllegalArgumentException() } }");
        contains(kotlin, "fun sum(): Int = x + y");
        assertFalse(kotlin.contains("fun x()"), kotlin);
    }

    @Language("java")
    private static final String PATTERNS = """
            package a;
            class C {
                Object f;
                int m(Object o) {
                    if (o instanceof String s && !s.isEmpty()) return s.length();
                    if (f instanceof String t) return t.length();
                    String s = "local";
                    return s.length();
                }
            }
            """;

    /**
     * {@code s} is {@code o}, smart-cast; a field is not smart-cast, so {@code t} is a cast. A later local of the same
     * name is itself again.
     */
    @Test
    public void patternVariables() {
        String kotlin = kotlin(PATTERNS);
        contains(kotlin, "if (o is String && !o.isEmpty()) { return o.length }");
        contains(kotlin, "if (f is String) { return (f as String).length }");
        contains(kotlin, """
                val s = "local"
                return s.length
                """);
    }

    @Language("java")
    private static final String MAPPED = """
            package a;
            import java.util.*;
            class C {
                int m(List<String> list, Map<String, Integer> map, String s) {
                    List<String> copy = new ArrayList<>(list);
                    copy.remove(0);
                    int n = 0;
                    for (Map.Entry<String, Integer> e : map.entrySet()) n += e.getKey().length() + e.getValue();
                    return n + copy.size() + s.charAt(0) + s.indexOf('x') + s.replaceAll("a+", "b").length();
                }
            }
            """;

    /** The JDK types Kotlin maps: mutable collection interfaces, and the Kotlin member for the Java one. */
    @Test
    public void mappedTypes() {
        String kotlin = kotlin(MAPPED);
        assertFalse(kotlin.contains("import java.util.List"), kotlin);
        contains(kotlin, "open fun m(list: MutableList<String>, map: MutableMap<String, Int>, s: String): Int {");
        contains(kotlin, "val copy: MutableList<String> = ArrayList<String>(list)");
        contains(kotlin, "copy.removeAt(0)");
        contains(kotlin, "for (e in map.entries) { n += e.key.length + e.value }");
        contains(kotlin, "copy.size + s[0].code + s.indexOf('x') + s.replace(\"a+\".toRegex(), \"b\").length");
    }

    @Language("java")
    private static final String FUNCTIONAL = """
            package a;
            interface Visitor { int visit(String s); }
            class C {
                short code;
                int run(Visitor v) { return v.visit("x"); }
                int m(int p) {
                    p = p + 1;
                    Visitor v = s -> s.length() + p0();
                    long total = code;
                    if (code == p) total++;
                    return run(v) + (int) total;
                }
                int p0() { return 0; }
                @Override public boolean equals(Object o) { return o == this; }
            }
            enum E { A, B }
            class D {
                void sw(E e) { switch (e) { case A: System.out.println(); } }
            }
            """;

    /**
     * A functional interface is a {@code fun interface}, and a lambda names it; a reassigned parameter is a local
     * var; Java's implicit widening is explicit; a {@code when} over an enum is exhaustive; equals takes {@code Any?}.
     */
    @Test
    public void conversions() {
        String kotlin = kotlin(FUNCTIONAL);
        contains(kotlin, "fun interface Visitor {");
        contains(kotlin, "val v: Visitor = Visitor { s -> s.length + p0() }");
        contains(kotlin, """
                open fun m(p: Int): Int {
                    var p = p
                    p = p + 1
                """);
        contains(kotlin, "var total: Long = code.toLong()");
        contains(kotlin, "if (code.toInt() == p) { total++ }");
        contains(kotlin, "override fun equals(o: Any?): Boolean = o === this");
        contains(kotlin, "else -> { }");
    }

    @Language("java")
    private static final String LAMBDA_PARAMETER = """
            package a;
            import java.util.*;
            class C {
                void m(Map<String, List<String>> map) {
                    map.compute("k", (k, v) -> { if (v == null) v = new ArrayList<>(); v.add(k); return v; });
                }
            }
            """;

    /** A lambda parameter the body assigns is a `var` copy, as a method's: Kotlin's lambda parameters are vals. */
    @Test
    public void reassignedLambdaParameter() {
        contains(kotlin(LAMBDA_PARAMETER), """
                { k, v ->
                var v = v
                if (v == null) {""");
    }

    @Language("java")
    private static final String VALS = """
            package a;
            import java.util.function.Supplier;
            class C {
                private int fixed = 1;
                private int counted = 0;
                private int byAnonymous = 0;
                int visible = 2;
                int m(int p) {
                    int a = p + 1;
                    int b = 0;
                    b += a;
                    int c;
                    c = 3;
                    Supplier<Integer> s = () -> { int x = 1; x++; int y = 2; return x + y; };
                    Runnable r = new Runnable() { public void run() { byAnonymous = 1; } };
                    counted++;
                    return a + b + c + fixed + s.get();
                }
            }
            """;

    /**
     * A variable that is never assigned after its declaration is a {@code val}: a local in its own body (a lambda's
     * included), a private field with an initializer anywhere in its compilation unit (anonymous classes included).
     */
    @Test
    public void vals() {
        String kotlin = kotlin(VALS);
        contains(kotlin, "private val fixed: Int = 1");
        contains(kotlin, "private var counted: Int = 0");
        contains(kotlin, "private var byAnonymous: Int = 0");
        contains(kotlin, "var visible: Int = 2"); // not private: another class may assign it
        contains(kotlin, "val a = p + 1");
        contains(kotlin, "var b = 0");
        contains(kotlin, "var c: Int"); // declared without a value: which assignment comes first is flow
        contains(kotlin, "var x = 1");
        contains(kotlin, "val y = 2");
    }
}
