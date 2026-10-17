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

import io.codelaser.maddi.cst.print.translate.PrintMessage;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.impl.analysis.NullabilityFacts;
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
    private record ByName(Set<String> nullable, Set<String> checked) implements NullabilityFacts {
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

        /** {@code "=m()"} in checked: m's return is decided non-null (a verdict, not the absence of one). */
        @Override
        public ParameterizedType returnType(MethodInfo methodInfo) {
            if (checked.contains("=" + methodInfo.name() + "()")) {
                return methodInfo.returnType().withNullable(NullableState.NONNULL);
            }
            return verdict(methodInfo.name() + "()", methodInfo.returnType());
        }

        /** {@code "!x"} in checked: local x is asserted at its declaration. */
        @Override
        public boolean assertedAtDeclaration(MethodInfo method, Element declaration, LocalVariable variable) {
            return checked.contains("!" + variable.simpleName());
        }

        /** {@code "^x"} in checked: parameter x is declared non-null as its method's precondition. */
        @Override
        public boolean preconditionNonNull(ParameterInfo parameterInfo) {
            return checked.contains("^" + parameterInfo.name());
        }

        /** {@code "%x"} in checked: parameter x's method answers null with its own exception. */
        @Override
        public boolean rejectsNull(ParameterInfo parameterInfo) {
            return checked.contains("%" + parameterInfo.name());
        }

        /** {@code "~x"} in checked: local x is proven unobserved before its first dereference. */
        @Override
        public boolean unobservedBeforeDereference(MethodInfo method, Element declaration, LocalVariable variable) {
            return checked.contains("~" + variable.simpleName());
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
        List<PrintMessage<KotlinPrintCode>> messages = messages(EARLY.replace("class D", "class D2"), options);
        assertTrue(messages.stream().anyMatch(m -> m.code() == KotlinPrintCode.ASSERT_AT_DECLARATION
                                                   && m.line() == 7), messages.toString());
        // with the linking engine's proof that nothing in between refers to s: INFO, not a behaviour change
        List<PrintMessage<KotlinPrintCode>> proven = messages(EARLY.replace("class D", "class D3"), new KotlinPrintOptions(
                new ByName(Set.of("find()"), Set.of("!s", "~s")), KotlinPrintOptions.NullCheck.ASSERT));
        assertTrue(proven.stream().anyMatch(m -> m.code() == KotlinPrintCode.ASSERT_AT_DECLARATION_UNOBSERVED
                                                 && m.severity() == PrintMessage.Severity.INFO && m.line() == 7),
                proven.toString());
        // t is not asserted at its declaration: its `!!`, if any, is an ordinary one
        assertTrue(messages.stream().noneMatch(m -> m.code() == KotlinPrintCode.ASSERT_AT_DECLARATION
                                                    && m.line() == 12), messages.toString());
    }

    @Language("java")
    private static final String PRECONDITION = """
            package a;
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
     * CodeLaser/maddi#125: a parameter the pass declares non-null as a precondition of its override family (the
     * verdict is the family's: the printer prints it as given on the interface and on the override). The caller that
     * may pass null asserts, and the NullPointerException moves from the method's body to the call, a behaviour
     * change reported at each declaration.
     */
    @Test
    public void preconditionNonNull() {
        KotlinPrintOptions options = new KotlinPrintOptions(new ByName(Set.of("find()"), Set.of("^n")),
                KotlinPrintOptions.NullCheck.ASSERT);
        String kotlin = kotlin(PRECONDITION, options);
        contains(kotlin, "fun visit(n: String)");
        contains(kotlin, "override fun visit(n: String)");
        contains(kotlin, "v.visit(find(k)!!)");
        List<PrintMessage<KotlinPrintCode>> messages = messages(PRECONDITION.replace("class P {", "class P2 {"), options);
        List<PrintMessage<KotlinPrintCode>> preconditions = messages.stream()
                .filter(m -> m.code() == KotlinPrintCode.PRECONDITION_NON_NULL).toList();
        org.junit.jupiter.api.Assertions.assertEquals(List.of(3, 5), preconditions.stream().map(PrintMessage::line).distinct().sorted().toList(),
                messages.toString());
        assertTrue(preconditions.stream().allMatch(m -> m.severity() == PrintMessage.Severity.BEHAVIOUR_CHANGE
                && m.detail().equals("the NPE moves from visit's body to the call")), preconditions.toString());
        // not a precondition: no message, though the parameter is just as non-null
        List<PrintMessage<KotlinPrintCode>> none = messages(PRECONDITION.replace("class P {", "class P3 {"),
                new KotlinPrintOptions(new ByName(Set.of("find()")), KotlinPrintOptions.NullCheck.ASSERT));
        assertTrue(none.stream().noneMatch(m -> m.code() == KotlinPrintCode.PRECONDITION_NON_NULL), none.toString());
    }

    @Language("java")
    private static final String INFERRED_LAMBDA_PARAMETERS = """
            package a;
            import java.io.IOException;
            import java.io.InputStream;
            import java.util.Map;
            class L {
                static InputStream open(String name) { return null; }
                static void consume(InputStream stream) throws IOException { stream.read(); }
                static void check(String key, Object value) { System.out.println(key.length()); }
                static void read(String name) throws IOException {
                    try (InputStream stream = open(name)) {
                        consume(stream);
                    }
                }
                static void all(Map<String, Object> map) {
                    map.forEach((key, value) -> check(key, value));
                }
            }
            """;

    /**
     * A lambda parameter Kotlin types by inference, not by its verdict: {@code use}'s is its receiver's type
     * (fernflower's {@code loader.getClassStream(name).use { in -> StructClass.create(in, ...) }}), forEach's
     * {@code key} the receiver's key type (langchain4j's {@code metadata.forEach { key, value -> validate(key, value) }}
     * on a {@code Map<String?, *>}). Into a non-null parameter both need their {@code !!}.
     */
    @Test
    public void inferredLambdaParameters() {
        String kotlin = kotlin(INFERRED_LAMBDA_PARAMETERS, new KotlinPrintOptions(new ByName(Set.of("open()", "map<>")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "open(name).use { stream ->");
        contains(kotlin, "consume(stream!!)");
        contains(kotlin, "check(key!!, value)");
        // without the nullable receivers: no assertion
        String plain = kotlin(INFERRED_LAMBDA_PARAMETERS.replace("class L {", "class L2 {"),
                new KotlinPrintOptions(new ByName(Set.of()), KotlinPrintOptions.NullCheck.ASSERT));
        contains(plain, "consume(stream)");
        contains(plain, "check(key, value)");
    }

    @Language("java")
    private static final String STREAMS = """
            package a;
            import java.util.*;
            class S {
                static int size(String s) { return s.length(); }
                int total(List<String> items) { return items.stream().mapToInt(S::size).sum(); }
                Optional<String> any(List<String> items) { return items.stream().filter(t -> t.isEmpty()).findAny(); }
                Optional<String> last(Optional<String> o) { return o.map(t -> { if (t.isEmpty()) return null; return t; }); }
                Optional<String> plain(List<String> names) { return names.stream().findFirst(); }
                long nulls(List<String> items) { return items.stream().filter(Objects::isNull).count(); }
                String upper(String s) { return s.toUpperCase(); }
                List<String> uppers(List<String> items) { return items.stream().map(this::upper).toList(); }
                static String str(Object o) { return o.toString(); }
                static int pick(String text) { return text == null ? 0 : 1; }
                static int pick(Integer number) { return 2; }
                static int none() { return pick((String) null); }
                static int count(String... ss) { return ss == null ? 0 : ss.length; }
                static int noArray() { return count((String[]) null); }
                boolean empty(Collection<?> c) { return c.stream().map(S::str).anyMatch(String::isEmpty); }
            }
            """;

    /**
     * Java's Optional never holds null, Kotlin's type argument says what went in: a nullable element or a map lambda
     * that returns null. And a method reference on nullable elements asserts them where its parameter is non-null.
     */
    @Test
    public void streamsOfNullableElements() {
        String kotlin = kotlin(STREAMS, new KotlinPrintOptions(new ByName(Set.of("items<>", "text")), KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "mapToInt({ S.size(it!!) })");
        contains(kotlin, "findAny() as Optional<String>)");
        contains(kotlin, "as Optional<String>)");
        // names' elements are non-null: no cast
        contains(kotlin, "= names.stream().findFirst()");
        // a library's parameter takes the null
        contains(kotlin, "filter(Objects ::isNull)");
        // a bound reference; a Collection<?>'s elements are Any? in Kotlin
        contains(kotlin, "{ this.upper(it!!) }).toList()");
        contains(kotlin, "map({ S.str(it!!) })");
        // a cast null picks the overload; `null as String` would throw
        contains(kotlin, "pick(null as String?)");
        // a varargs array is spread, which Kotlin cannot do with a nullable one
        contains(kotlin, "count(*(null as Array<String>))");
    }

    @Language("java")
    private static final String LAMBDAS = """
            package a;
            import java.util.*;
            class Lm {
                static class St { int id; List<String> ex() { return id > 0 ? null : new ArrayList<>(); } }
                void sort(List<St> sorted) { Collections.sort(sorted, Comparator.comparingInt(o -> o.id)); }
                String first(Optional<List<St>> o) {
                    return o.map(stats -> stats.isEmpty() ? null : stats.get(0).ex())
                            .map(exprs -> exprs.size() == 1 ? exprs.get(0) : null).orElse(null);
                }
            }
            """;

    /**
     * Lambda parameters Kotlin infers nullable: a key extractor sorting nullable elements (Java throws there too), and
     * an Optional.map after one that may return null (Java never calls it with null; the `!!` cannot throw).
     */
    @Test
    public void inferredNullableLambdaParameters() {
        String kotlin = kotlin(LAMBDAS, new KotlinPrintOptions(new ByName(Set.of("sorted<>", "ex()")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "o -> o!!.id");
        contains(kotlin, "exprs!!.size");
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
    private static final String BOXED_SIGNATURES = """
            package a;
            class W {
                private final Long total;
                private W(Long total) { this.total = total; }
                static W from(Long total) { return new W(total); }
                public Long total() { return total; }
                private Long twice() { return total * 2; }
                public String describe(Integer count) { return "count=" + count + ", total=" + total; }
            }
            """;

    /**
     * Java code calls {@code total()} returning a {@code java.lang.Long}: Kotlin's non-null Long is a {@code long}, so a
     * boxed type in a signature Java can call is nullable whatever the verdict. String concatenation prints a null.
     */
    @Test
    public void boxedSignatures() {
        String kotlin = kotlin(BOXED_SIGNATURES, VERDICTS);
        contains(kotlin, "fun from(total: Long?): W");
        contains(kotlin, "open fun total(): Long? = total");
        contains(kotlin, "private fun twice(): Long");
        contains(kotlin, "\"count=\" + count + \", total=\" + total");
    }

    @Language("java")
    private static final String UNDECLARED = """
            package a;
            import java.util.*;
            class M<T> {
                private final T value;
                M(T value) { this.value = value; }
                static RuntimeException runtime(String format, Object... args) { return new RuntimeException(format); }
                void use(Object o) { }
                RuntimeException m(Map<String, ?> s, String name) {
                    s.forEach((k, w) -> use(w.getClass()));
                    use(value);
                    return runtime("%s %s", name, s);
                }
                long limit(Integer x) { return x; }
            }
            """;

    /**
     * Values Kotlin types nullable without a verdict saying so: a varargs element, a lambda parameter from a
     * {@code Map<String, *>}, an unbounded T; and a nullable Integer widened to a long.
     */
    @Test
    public void undeclaredNullable() {
        String kotlin = kotlin(UNDECLARED, VERDICTS);
        contains(kotlin, "use(w!!.javaClass)");
        contains(kotlin, "use(value!!)");
        contains(kotlin, "runtime(\"%s %s\", name!!, s)");
        contains(kotlin, "x!!.toLong()");
    }

    @Language("java")
    private static final String UNCHECKED_TYPE_VARIABLE = """
            package a;
            class Box<T> {
                private final T value;
                Box(T value) {
                    if (value == null) throw new IllegalArgumentException("value");
                    this.value = value;
                }
                static <T> Box<T> of(T x) { return new Box<>(x); }
            }
            """;

    /**
     * A null into a parameter typed by an unbounded type variable: Kotlin checks nothing there, so the null reaches the
     * constructor, whose own check throws what Java's caller expects; {@code x!!} would throw an NPE first.
     */
    @Test
    public void uncheckedTypeVariable() {
        String kotlin = kotlin(UNCHECKED_TYPE_VARIABLE, VERDICTS);
        contains(kotlin, "Box<T>((x as T))");
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

    /**
     * guava's ImmutableSortedSet.lower(): NavigableSet is no Kotlin type, its {@code lower(e): E!} is a platform type,
     * and the override returns E? as the verdict says.
     */
    @Test
    public void nullForATypeVariableOfAJavaLibraryType() {
        String kotlin = kotlin("""
                package a;
                import java.util.concurrent.Callable;
                class Last<E> implements Callable<E> {
                    E last;
                    public E call() { return last; }
                }
                """, new KotlinPrintOptions(new ByName(Set.of("call()", "last")), KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "override fun call(): E?");
        assertFalse(kotlin.contains(" as E"), kotlin);
        compiles(kotlin);
    }

    /**
     * guava's ThreadFactoryBuilder.doBuild(): {@code builder.backingThreadFactory != null ? builder.backingThreadFactory
     * : defaultThreadFactory()}, another object's var field, which Kotlin does not smart-cast: the elvis reads it once.
     */
    @Test
    public void elvisOnAnotherObjectsField() {
        String kotlin = kotlin("""
                package a;
                class Builder {
                    Runnable backing;
                    Builder backing(Runnable r) { this.backing = r; return this; }
                    static Runnable build(Builder builder) {
                        Runnable backing = builder.backing != null ? builder.backing : () -> { };
                        return backing;
                    }
                }
                """, new KotlinPrintOptions(new ByName(Set.of("backing")), KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "builder.backing ?: ");
        compiles(kotlin);
    }

    /**
     * guava's Ascii: toUpperCase(String string), nullable, beside toUpperCase(CharSequence chars). A String argument
     * fits both in Kotlin, neither more specific: the cast to String? picks Java's.
     */
    @Test
    public void nullableOverloadBesideAWiderOne() {
        String kotlin = kotlin("""
                package a;
                final class Up {
                    static String up(String string) { return string == null ? "" : string.toUpperCase(); }
                    static String up(CharSequence chars) { return chars.toString(); }
                    static String caller(String s) { return up(s); }
                }
                """, new KotlinPrintOptions(new ByName(Set.of("string")), KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "up((s as String?))");
        compiles(kotlin);
    }

    /**
     * guava's ThreadFactoryBuilder.doBuild(): {@code requireNonNull(count)} inside the anonymous ThreadFactory, of the
     * enclosing method's {@code AtomicLong count = nameFormat != null ? new AtomicLong(0) : null}: count!!.
     */
    @Test
    public void capturedNullableLocalInAnAnonymousClass() {
        String kotlin = kotlin("""
                package a;
                import java.util.concurrent.atomic.AtomicLong;
                import java.util.function.Supplier;
                import static java.util.Objects.requireNonNull;
                class Factory {
                    static Supplier<String> build(String nameFormat) {
                        AtomicLong count = (nameFormat != null) ? new AtomicLong(0) : null;
                        return new Supplier<String>() {
                            @Override public String get() {
                                if (nameFormat != null) return nameFormat + requireNonNull(count).getAndIncrement();
                                return "";
                            }
                        };
                    }
                }
                """, new KotlinPrintOptions(new ByName(Set.of("nameFormat", "count")), KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "count!!.getAndIncrement()");
        compiles(kotlin);
    }

    /**
     * langchain4j's Utils.firstNotNull: {@code <T> @NonNull T firstNotNull(String name, T... values)}, nullable
     * elements, whose T the printer bounds by Any: T, not T & Any, which Kotlin allows only for a nullable bound.
     */
    @Test
    public void nonNullTypeVariableBoundedByAny() {
        String kotlin = kotlin("""
                package a;
                import java.lang.annotation.*;
                @Target(ElementType.TYPE_USE) @interface NonNull { }
                final class U {
                    @SafeVarargs
                    static <T> @NonNull T firstNotNull(String name, T... values) {
                        for (T v : values) if (v != null) return v;
                        throw new IllegalArgumentException(name);
                    }
                }
                """, new KotlinPrintOptions(new ByName(Set.of("values[]")), KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "fun <T : Any> firstNotNull(");
        assertFalse(kotlin.contains("& Any"), kotlin);
        compiles(kotlin);
    }

    /**
     * guava's ImmutableMap.SerializedForm.legacyReadResolve(): {@code V[] values = (V[]) this.values} into a local
     * the verdicts make an {@code Array<V?>}: the cast takes the local's element state.
     */
    @Test
    public void arrayCastTakesTheTargetsElementState() {
        String kotlin = kotlin("""
                package a;
                class Form<V> {
                    private final Object stored;
                    Form(Object stored) { this.stored = stored; }
                    int count() {
                        V[] values = (V[]) this.stored;
                        int n = 0;
                        for (V v : values) if (v != null) n++;
                        return n;
                    }
                }
                """, new KotlinPrintOptions(new ByName(Set.of("values", "values[]", "stored")), KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "as Array<V?>");
        compiles(kotlin);
    }

    /**
     * As {@link #nullForATypeVariable}, where the pass holds next() non-null for Kotlin's member and a branch is a
     * nullable call (fernflower's FastFixedSetIterator: {@code pointer < size ? getKey(pointer) : null}): the
     * conditional is cast as a whole, not asserted.
     */
    @Test
    public void nullForATypeVariableHeldNonNull() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Keys<E> implements Iterator<E> {
                    private int pos;
                    public boolean hasNext() { return true; }
                    E key(int i) { return null; }
                    public E next() { return pos < 2 ? key(pos++) : null; }
                }
                """, new KotlinPrintOptions(new ByName(Set.of("key()"), Set.of("=next()")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "override fun next(): E");
        contains(kotlin, ") as E");
        compiles(kotlin);
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
        contains(kotlin, "class Node(@JvmField val value: String?)");
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

    @Language("java")
    private static final String CASTS = """
            package a;
            import java.util.Map;
            class C {
                String level(Map<String, Object> p) {
                    String l = (String) p.get("k");
                    return l;
                }
                int len(Map<String, Object> p) { return ((String) p.get("k")).length(); }
                String name(Object o) { return (String) o; }
            }
            """;

    /**
     * Java's {@code (String) p.get(k)} lets null through, Kotlin's {@code as String} throws: a cast of a value Kotlin
     * types nullable into a nullable target is to the nullable type. Dereferenced, Java throws on the null as well.
     */
    @Test
    public void castOfNullableValue() {
        String kotlin = kotlin(CASTS, new KotlinPrintOptions(new ByName(Set.of("l", "level()")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "p.get(\"k\") as String?");
        contains(kotlin, "(p.get(\"k\") as String).length");
        contains(kotlin, "= o as String"); // o is not nullable
    }

    @Language("java")
    private static final String LOOKUP_SUBTYPE = """
            package a;
            import java.util.List;
            import java.util.Set;
            class E { }
            class V extends E { }
            class C {
                boolean m(List<E> lst, Set<E> set, E e) {
                    V v = null;
                    if (e instanceof V w) v = w;
                    lst.remove(v);
                    return set.contains(v);
                }
            }
            """;

    /**
     * Java's {@code lst.remove(v)} with a null {@code v} removes nothing; Kotlin's {@code v!!} would throw. The stdlib
     * extension takes a nullable element, but infers its T from the inputs only: a subtype is cast up.
     */
    @Test
    public void lookupOfNullableSubtype() {
        String kotlin = kotlin(LOOKUP_SUBTYPE, new KotlinPrintOptions(new ByName(Set.of("v")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "lst.remove((v as E?))");
        contains(kotlin, "set.contains((v as E?))");
    }

    @Language("java")
    private static final String SUPER_LIST = """
            package a;
            import java.util.List;
            class C {
                static boolean choice(String head, List<? super String> lst, boolean b) {
                    String post = null;
                    if (b) post = head;
                    lst.add(head);
                    lst.remove(post);
                    lst.add(0, post);
                    return true;
                }
            }
            """;

    /** A {@code MutableList<in String?>} takes a String? in: no {@code !!} on what Java adds or removes. */
    @Test
    public void superWildcardOfNullableElements() {
        String kotlin = kotlin(SUPER_LIST, new KotlinPrintOptions(new ByName(Set.of("lst<>", "post")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "lst: MutableList<in String?>");
        contains(kotlin, "lst.remove(post)");
        contains(kotlin, "lst.add(0, post)");
    }

    @Language("java")
    private static final String CONDITIONAL_ITERABLE = """
            package a;
            import java.util.List;
            class C {
                List<String> a;
                List<String> b;
                void use(String s) { }
                void m(boolean c) {
                    for (String s : c ? a : b) use(s);
                }
            }
            """;

    /** The loop variable over {@code if (c) a else b} has the branches' element type: a nullable one is asserted. */
    @Test
    public void conditionalIterable() {
        String kotlin = kotlin(CONDITIONAL_ITERABLE, new KotlinPrintOptions(new ByName(Set.of("a<>", "b<>")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "use(s!!)");
    }

    @Language("java")
    private static final String NULLABLE_BESIDE_VARARGS = """
            package a;
            class M {
                M(String text) { }
                M(String name, Object... contents) { }
                static M of() { return new M("hi"); }
                static M named() { return new M("n", "c"); }
            }
            """;

    /**
     * {@code M(text: String?)} beside {@code M(name: String, vararg contents: Any)}: Kotlin finds the second more
     * specific for {@code M("hi")}, Java takes the first; the argument is cast to the nullable parameter type.
     */
    @Test
    public void nullableBesideVarargs() {
        String kotlin = kotlin(NULLABLE_BESIDE_VARARGS, new KotlinPrintOptions(new ByName(Set.of("text")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "M((\"hi\" as String?))");
        contains(kotlin, "M(\"n\", \"c\")");
    }

    @Language("java")
    private static final String NON_NULL_RESULT_OF_NULLABLE_T = """
            package a;
            class V {
                static <T> T ensureNotNull(T object, String name) {
                    if (object == null) throw new IllegalArgumentException(name);
                    return object;
                }
                static <T> T firstNotNull(T... values) {
                    for (T v : values) if (v != null) return v;
                    throw new IllegalArgumentException();
                }
                static <T> T identity(T t) { return t; }
                static <T> T getOrDefault(T value, java.util.function.Supplier<T> supplier) {
                    return value != null ? value : supplier.get();
                }
            }
            """;

    /**
     * A nullable {@code T?} parameter beside a non-null {@code T} result: Kotlin's unbounded T would take the nullable
     * argument's type, and the result with it. {@code T : Any} keeps the result non-null at the call site.
     */
    @Test
    public void nonNullResultOfNullableTypeParameter() {
        String kotlin = kotlin(NON_NULL_RESULT_OF_NULLABLE_T, new KotlinPrintOptions(new ByName(Set.of("object", "values[]", "value")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "fun <T : Any> ensureNotNull(`object`: T?, name: String): T");
        contains(kotlin, "fun <T : Any> firstNotNull(vararg values: T?): T");
        contains(kotlin, "fun <T> identity(t: T): T");
        // the supplier's T reaches the result: a Supplier<X?> makes it nullable
        contains(kotlin, "fun <T> getOrDefault(value: T?, supplier: java.util.function.Supplier<T>): T");
        // an elvis (#140): its type is supplier.get()'s, T, so no cast
        contains(kotlin, "): T = value ?: supplier.get()");
    }

    @Language("java")
    private static final String BRANCHES = """
            package a;
            import java.util.*;
            class Node {
                final List<Node> preds = new ArrayList<>();
                final List<Node> succs = new ArrayList<>();
                int size() { return preds.size(); }
                int adjacent(boolean p) {
                    int n = 0;
                    for (Node a : p ? preds : succs) n += a.size();
                    Node[] firsts = { null };
                    Node[] seconds = { null };
                    for (Node f : p ? firsts : seconds) n += f.size();
                    return n;
                }
            }
            """;

    /**
     * Kotlin types a conditional by both branches: iterating {@code p ? preds : succs} over a {@code List<Node>} and a
     * {@code List<Node?>} yields a {@code Node?}, whichever branch is first, so the loop variable's dereference is
     * asserted. The same for arrays with nullable elements.
     */
    @Test
    public void conditionalBranchesJoinNullability() {
        String kotlin = kotlin(BRANCHES, new KotlinPrintOptions(new ByName(Set.of("succs<>", "seconds[]")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "for (a in if (p) preds else succs) {n += a!!.size() }");
        contains(kotlin, "n += f!!.size()");
    }

    @Language("java")
    private static final String LANGCHAIN4J_SHAPES = """
            package a;
            import java.lang.reflect.Type;
            import java.util.*;
            import java.util.function.Function;
            class Shapes {
                @interface Sub { Class<?> value(); }
                static <T> T fromJson(String text, Class<T> type) { return null; }
                static <T> T fromJson(String json, Type type) { return null; }
                static Map<String, Object> read(String s) { return fromJson(s, Map.class); }
                static final Function<String, Integer> DEFAULT = q -> 3;
                static Class<?> subtype(Sub sub) { return sub.value(); }
                static String describe(Class<?> c) { return null; }
                static String orDefault(String d, Class<?> c) { return Optional.ofNullable(d).orElse(describe(c)); }
            }
            """;

    /**
     * langchain4j's last printer errors: an overload Java chose, which a nullable parameter makes ambiguous for Kotlin;
     * a raw Map from a generic call; a SAM constructor into a field whose type arguments the verdicts made nullable;
     * an annotation's Class element, a KClass property in Kotlin; a nullable value into a library result's member
     * ({@code Optional.ofNullable(d).orElse(…)}), which takes null as in Java.
     */
    @Test
    public void langchain4jShapes() {
        String kotlin = kotlin(LANGCHAIN4J_SHAPES, new KotlinPrintOptions(new ByName(Set.of("text", "DEFAULT<,>", "d", "describe", "orDefault")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "fromJson((s as String?), MutableMap::class.java)");
        contains(kotlin, "as MutableMap<String, Any>)");
        contains(kotlin, "val DEFAULT: Function<String, Int?> = Function<String, Int?> { q -> 3 }");
        contains(kotlin, "fun subtype(sub: Sub): Class<*> = sub.value.java");
        contains(kotlin, "Optional.ofNullable(d).orElse(describe(c))");
        assertFalse(kotlin.contains("describe(c)!!"), kotlin);
    }

    private static final String SMART_CASTS = """
            package a;
            class SC {
                static int f(String s, String t, boolean c) {
                    int n = s.length();
                    n += s.length();
                    if (c && t.isEmpty()) n++;
                    n += t.length();
                    return n + t.length();
                }
                static int g(String u, boolean c) {
                    int n = 0;
                    if (c) {
                        n = u.length();
                        n += u.length();
                    }
                    return n + u.length() + u.length();
                }
                static int m(String p, String q) {
                    String x = p;
                    int n = x.length();
                    n += x.length();
                    x = q;
                    n += x.length();
                    return n + x.length();
                }
                static int k(String w, int i) {
                    int n = 0;
                    switch (i) {
                        case 1:
                            n = w.length();
                            break;
                        case 2:
                            n = w.length() + 1;
                            break;
                        default:
                            n = 2;
                    }
                    return n;
                }
            }
            """;

    /**
     * After {@code x!!} Kotlin smart-casts a val: what that assertion dominates needs none. Not after one on the right
     * of {@code &&}, which may not run; not after a branch, whose assertions hold inside it only.
     */
    @Test
    public void smartCastAfterAssertion() {
        String kotlin = kotlin(SMART_CASTS, new KotlinPrintOptions(new ByName(Set.of("s", "t", "u", "w", "x", "q")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "var n = s!!.length\nn += s.length");
        contains(kotlin, "if (c && t!!.isEmpty())");
        contains(kotlin, "n += t!!.length\nreturn n + t.length");
        contains(kotlin, "n = u!!.length\nn += u.length");
        // later in its own statement too (in source order: the printer may print a part twice)
        contains(kotlin, "return n + u!!.length + u.length");
        // a var: known until it is assigned again
        contains(kotlin, "var n = x!!.length\nn += x.length\nx = q\nn += x!!.length\nreturn n + x.length");
        // a switch entry's assertion does not reach the other entries
        assertTrue(kotlin.split("w!!\\.length", -1).length == 3, kotlin);
    }

    private static final String NULL_OR_EMPTY = """
            package a;
            import java.util.List;
            class NE {
                static boolean blank(String s) { return s == null || s.isEmpty(); }
                static boolean some(List<String> l) { return l != null && !l.isEmpty(); }
                static boolean other(String s, String t) { return s == null || t.isEmpty(); }
            }
            """;

    /** {@code x == null || x.isEmpty()} is Kotlin's {@code x.isNullOrEmpty()}; not when the two test different values. */
    @Test
    public void nullOrEmpty() {
        String kotlin = kotlin(NULL_OR_EMPTY, new KotlinPrintOptions(new ByName(Set.of("s", "l", "t")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "fun blank(s: String?): Boolean = s.isNullOrEmpty()");
        contains(kotlin, "fun some(l: MutableList<String>?): Boolean = !l.isNullOrEmpty()");
        contains(kotlin, "s == null || t!!.isEmpty()");
    }

    private static final String REJECTS_NULL = """
            package a;
            class Doc {
                private final String text;
                Doc(String text) {
                    if (text == null) throw new IllegalArgumentException("text cannot be null");
                    this.text = text;
                }
            }
            """;

    /**
     * CodeLaser/maddi-mod#28: a non-null parameter whose method answers null with its own exception. Kotlin's entry
     * check throws a NullPointerException first for a Java caller's null: reported, the declaration stays non-null.
     * Not where the parameter is nullable, which the method's own check still guards.
     */
    @Test
    public void nullRejectionReplaced() {
        List<PrintMessage<KotlinPrintCode>> messages = messages(REJECTS_NULL,
                new KotlinPrintOptions(new ByName(Set.of(), Set.of("%text")), KotlinPrintOptions.NullCheck.ASSERT));
        List<PrintMessage<KotlinPrintCode>> replaced = messages.stream()
                .filter(m -> m.code() == KotlinPrintCode.NULL_REJECTION_REPLACED).toList();
        org.junit.jupiter.api.Assertions.assertEquals(1, replaced.size(), messages.toString());
        assertTrue(replaced.getFirst().severity() == PrintMessage.Severity.BEHAVIOUR_CHANGE
                   && replaced.getFirst().line() == 4, replaced.toString());
        contains(kotlin(REJECTS_NULL.replace("class Doc", "class Doc2"),
                new KotlinPrintOptions(new ByName(Set.of(), Set.of("%text")), KotlinPrintOptions.NullCheck.ASSERT)),
                "class Doc2(text: String)");
        List<PrintMessage<KotlinPrintCode>> nullable = messages(REJECTS_NULL.replace("class Doc", "class Doc3"),
                new KotlinPrintOptions(new ByName(Set.of("text"), Set.of("%text")), KotlinPrintOptions.NullCheck.ASSERT));
        assertTrue(nullable.stream().noneMatch(m -> m.code() == KotlinPrintCode.NULL_REJECTION_REPLACED),
                nullable.toString());
    }

    private static final String LAMBDA_RESULT = """
            package a;
            import java.util.function.Function;
            public class Retriever {
                private final Function<String, Double> minScore;
                public Retriever(Double minScore) { this(query -> minScore, 0); }
                private Retriever(Function<String, Double> dynamicMinScore, int unused) { this.minScore = dynamicMinScore; }
            }
            """;

    /**
     * A public signature's boxed parameter is Kotlin's {@code Double?} whatever the verdict (the Java ABI), so a lambda
     * returning it into a {@code Function<String, Double>} asserts it where the pass decides the lambda's result
     * non-null; without that verdict it prints as before (langchain4j EmbeddingStoreContentRetriever, maddi-mod#38).
     */
    @Test
    public void lambdaResultIntoNonNullSlot() {
        String kotlin = kotlin(LAMBDA_RESULT, new KotlinPrintOptions(new ByName(Set.of(), Set.of("=apply()")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "minScore: Double?");
        contains(kotlin, "{ query -> minScore!! }");
        String undecided = kotlin(LAMBDA_RESULT.replace("Retriever", "Retriever2"),
                new KotlinPrintOptions(new ByName(Set.of()), KotlinPrintOptions.NullCheck.ASSERT));
        contains(undecided, "{ query -> minScore }");
    }

    private static final String TYPE_TOKEN = """
            package a;
            import java.util.List;
            class Tokens {
                static abstract class TypeRef<T> { }
                private static final TypeRef<List<String>> LIST_TYPE = new TypeRef<>() { };
            }
            """;

    /**
     * {@code new TypeRef<>() {}} takes the type arguments of the field it initializes, as a constructor call does:
     * Kotlin's generics are invariant (langchain4j DefaultStructuredPromptFactory's MAP_TYPE, maddi-mod#38).
     */
    @Test
    public void typeTokenTakesTheFieldsTypeArguments() {
        String kotlin = kotlin(TYPE_TOKEN, new KotlinPrintOptions(new ByName(Set.of("LIST_TYPE<<>>")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "LIST_TYPE: TypeRef<MutableList<String?>> = object : TypeRef<MutableList<String?>>() { }");
    }

    private static final String VAL_COPIES = """
            package a;
            import java.util.List;
            class Stat {
                private final List<String> exprents;
                private String label;
                Stat(List<String> e) { exprents = e; }
                List<String> getExprents() { return exprents; }
                String getLabel() { return label; }
                void setLabel(String l) { label = l; }
                static int count(Stat stat) {
                    if (stat.getExprents() != null && !stat.getExprents().isEmpty()) return stat.getExprents().size();
                    if (stat.getLabel() != null) return stat.getLabel().length();
                    return 0;
                }
            }
            """;

    /**
     * A getter of a final field, tested against null and used again: a local val copy, which the test smart-casts.
     * Not for a getter of a field that changes ({@code label}, which {@code setLabel} assigns).
     */
    @Test
    public void valCopyOfAGetter() {
        String kotlin = kotlin(VAL_COPIES, new KotlinPrintOptions(new ByName(Set.of("getExprents()", "getLabel()",
                "exprents", "label", "l")), KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "val exprents = stat.getExprents()\nif (exprents != null && !exprents.isEmpty()) { return exprents.size }");
        contains(kotlin, "stat.getLabel()!!.length");
    }

    @Language("java")
    private static final String NULL_CHECK_SHAPES = """
            package a;
            import java.util.Objects;
            class C {
                String name;
                Integer count;
                String elvis(String s, String d) { return s != null ? s : d; }
                String elvisFlipped(String s) { return s == null ? "none" : s; }
                Integer safeCall(String s) { return s != null ? s.length() : null; }
                String safeCallFlipped(String s) { return s == null ? null : s.trim(); }
                String safeCallArgument(String s) { return s != null ? s.concat(s) : null; }
                String twice() { return name() != null ? name() : "x"; }
                String name() { return name; }
                boolean objectsEquals(String a, String b) { return Objects.equals(a, b); }
                boolean equalsNonNull(String a, String b) { return a.equals(b); }
                boolean notEquals(String a, String b) { return !a.equals(b); }
                boolean equalsNullable(String s, String b) { return s.equals(b); }
                boolean incomparable(Integer i, Long l) { return Objects.equals(i, l) || i.equals(l); }
                boolean floating(Double x, Double y) { return Objects.equals(x, y); }
                String concatenated(String s, String a) { return s != null ? s : a + "!"; }
                String nested(String s, boolean c) { return s != null ? s : c ? "x" : "y"; }
            }
            """;

    /** CodeLaser/maddi#140, item 1: Java's null-check conditionals and equals calls, as Kotlin writes them. */
    @Test
    public void nullCheckShapes() {
        String kotlin = kotlin(NULL_CHECK_SHAPES, new KotlinPrintOptions(new ByName(Set.of("s", "name()")),
                KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "fun elvis(s: String?, d: String): String = s ?: d");
        contains(kotlin, "fun elvisFlipped(s: String?): String = s ?: \"none\"");
        contains(kotlin, "fun safeCall(s: String?): Int? = s?.length");
        contains(kotlin, "= (s?.trim())!!");
        // the argument reads s: kept, ?. gives it no smart cast
        contains(kotlin, "if (s != null) s.plus(s) else null");
        // a call is evaluated twice in Java, once by ?:: kept
        contains(kotlin, "if (name() != null) name() else \"x\"");
        contains(kotlin, "fun objectsEquals(a: String, b: String): Boolean = a == b");
        contains(kotlin, "fun equalsNonNull(a: String, b: String): Boolean = a == b");
        contains(kotlin, "fun notEquals(a: String, b: String): Boolean = a != b");
        // s.equals(b) throws in Java when s is null; s == b does not
        contains(kotlin, "s!!.equals(b)");
        // Int == Long does not compile; Double == Double is IEEE on non-null doubles
        contains(kotlin, "Objects.equals(i, l) || i!!.equals(l)");
        contains(kotlin, "Objects.equals(x, y)");
        contains(kotlin, "s ?: a + \"!\"");
        contains(kotlin, "s ?: (if (c) \"x\" else \"y\")");
    }

    /**
     * guava's ForwardingCollection: an override of a mapped collection member takes Kotlin's parameter type, the
     * owner's type variable, whatever the verdict says: {@code add(element: E?)} overrides nothing.
     */
    @Test
    public void mappedOverrideParameterKeepsTheTypeVariable() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                abstract class Forwarding<E> extends AbstractCollection<E> {
                    abstract Collection<E> delegate();
                    @Override public boolean add(E element) { return delegate().add(element); }
                }
                """, new KotlinPrintOptions(new ByName(Set.of("element")), KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "override fun add(element: E): Boolean");
        compiles(kotlin);
    }

    /**
     * Null stops at a type-variable slot (maddi-mod 77674c0c): a {@code List<K>} into which Java adds null
     * (fernflower's VBStyleCollection) and a {@code List<T>} collected from a nullable result (langchain4j's
     * BatchResponse.responses()). Java's list holds the nulls; the printer casts, unchecked, where Kotlin's types
     * disagree with the declaration.
     */
    @Test
    public void nullIntoTypeVariableSlot() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class VB<E, K> {
                    private final List<K> keys = new ArrayList<>();
                    void addNull() { keys.add(null); }
                    static class Item<T> {
                        T value;
                        boolean ok() { return value != null; }
                        T response() { return value; }
                    }
                    static <T> List<T> responses(List<Item<T>> results) {
                        return results.stream().filter(Item::ok).map(Item::response).toList();
                    }
                }
                """, new KotlinPrintOptions(new ByName(Set.of("response()", "value")), KotlinPrintOptions.NullCheck.ASSERT));
        contains(kotlin, "keys.add((null as K))");
        contains(kotlin, "as MutableList<T>)");
        compiles(kotlin);
    }

    /**
     * langchain4j's ModerationModel: {@code moderate(List.of(message))} with a nullable message. List.of is a static
     * of a mapped type, called with Java's parameter types, and takes no null all the same: asserted as in Java, where
     * it throws.
     */
    @Test
    public void nullableIntoListOfAsserted() {
        KotlinPrintOptions options = new KotlinPrintOptions(new ByName(Set.of("message")),
                KotlinPrintOptions.NullCheck.ASSERT);
        String kotlin = kotlin("""
                package a;
                import java.util.List;
                interface Moderation {
                    int moderate(List<String> messages);
                    default int moderate(String message) { return moderate(List.of(message)); }
                }
                """, options);
        contains(kotlin, "message!!");
        compiles(kotlin);
    }

    /**
     * guava's Ordering: {@code <E> List<E> sortedCopy(Iterable<E> elements)} prints {@code <E : Any>} where its
     * parameter's E is nullable and its result's is not; AllEqualOrdering's override takes the same bound, which
     * Kotlin wants identical.
     */
    @Test
    public void overrideTakesOverriddenBound() {
        KotlinPrintOptions options = new KotlinPrintOptions(new ByName(Set.of("e")), KotlinPrintOptions.NullCheck.ASSERT);
        String kotlin = kotlin("""
                package a;
                abstract class Ord {
                    <E> E first(E e) { return e; }
                }
                class AllEqual extends Ord {
                    @Override <E> E first(E e) { return e; }
                }
                """, options);
        contains(kotlin, "override fun <E : Any> first(");
        compiles(kotlin);
    }

    /**
     * guava's {@code Equivalence<T> implements BiPredicate<@Nullable T, @Nullable T>}: the supertype clause keeps the
     * type-use annotation's state, so that {@code test(t: T?, u: T?)} overrides what it says.
     */
    @Test
    public void supertypeArgumentDeclaredNullable() {
        KotlinPrintOptions options = new KotlinPrintOptions(new ByName(Set.of("t", "u")), KotlinPrintOptions.NullCheck.ASSERT);
        String kotlin = kotlin("""
                package a;
                import java.util.function.BiPredicate;
                import java.lang.annotation.*;
                @Target(ElementType.TYPE_USE) @interface Nullable { }
                abstract class Equiv<T> implements BiPredicate<@Nullable T, @Nullable T> {
                    @Override public boolean test(@Nullable T t, @Nullable T u) { return t == u; }
                }
                """, options);
        contains(kotlin, "BiPredicate<T?, T?>");
        compiles(kotlin);
    }

    /**
     * guava's {@code Iterators.partitionImpl}: {@code new UnmodifiableIterator<List<@Nullable T>>() {...}}. The
     * anonymous class's header keeps the type-use annotation's state, as a named class's does, so that its
     * {@code next(): MutableList<T?>} overrides what the header says (CodeLaser/maddi#144).
     */
    @Test
    public void anonymousSupertypeArgumentDeclaredNullable() {
        // the results as the pass reads them from guava's annotations: next() a List<T?>, the factories an It<List<T?>>
        KotlinPrintOptions options = new KotlinPrintOptions(
                new ByName(Set.of("next()<>", "get()<>", "ofClass()<<>>", "ofInterface()<<>>")), KotlinPrintOptions.NullCheck.ASSERT);
        String kotlin = kotlin("""
                package a;
                import java.util.Iterator;
                import java.util.List;
                import java.lang.annotation.*;
                @Target(ElementType.TYPE_USE) @interface Nullable { }
                class Partition {
                    abstract static class It<E> implements Iterator<E> {
                        @Override public boolean hasNext() { return false; }
                    }
                    static <T> It<List<@Nullable T>> ofClass() {
                        return new It<List<@Nullable T>>() {
                            @Override public List<@Nullable T> next() { return new java.util.ArrayList<>(); }
                        };
                    }
                    static <T> java.util.function.Supplier<List<@Nullable T>> ofInterface() {
                        return new java.util.function.Supplier<List<@Nullable T>>() {
                            @Override public List<@Nullable T> get() { return new java.util.ArrayList<>(); }
                        };
                    }
                }
                """, options);
        contains(kotlin, "object : It<MutableList<T?>>()");
        contains(kotlin, "object : java.util.function.Supplier<MutableList<T?>>");
        compiles(kotlin);
    }

    /**
     * guava's TopKSelector, ImmutableSet: {@code buffer = (T[]) new Object[2 * k]} into a field whose elements are
     * nullable. The cast takes the target's element state ({@code as Array<T?>}): Kotlin's arrays are invariant.
     */
    @Test
    public void arrayCastTakesTargetElementState() {
        KotlinPrintOptions options = new KotlinPrintOptions(new ByName(Set.of("buffer[]", "grid[]")),
                KotlinPrintOptions.NullCheck.ASSERT);
        String kotlin = kotlin("""
                package a;
                class TopK<T> {
                    private final T[] buffer;
                    TopK(int k) { this.buffer = (T[]) new Object[2 * k]; }
                    T first() { return buffer[0]; }
                }
                """, options);
        contains(kotlin, "as Array<T?>");
        compiles(kotlin);
    }

    /**
     * guava's Collections2, Maps: {@code return Lists.newArrayList(iterator()).toArray()} from a toArray() whose
     * Object[] holds nulls: toTypedArray<Any?>(), as Kotlin's arrays are invariant.
     */
    @Test
    public void toArrayTakesTargetElementState() {
        KotlinPrintOptions options = new KotlinPrintOptions(new ByName(Set.of("copy()[]")),
                KotlinPrintOptions.NullCheck.ASSERT);
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Copy {
                    private final List<String> items = new ArrayList<>();
                    Object[] copy() { return items.toArray(); }
                }
                """, options);
        contains(kotlin, "toTypedArray<Any?>()");
        compiles(kotlin);
    }

    /**
     * guava's ImmutableMap declares {@code abstract @Nullable V get(@Nullable Object key)}, and its subclasses override
     * it again. Kotlin's own Map declares {@code get(key: K): V?}, so the overrides keep the verdict's {@code V?}
     * instead of being clamped to V, as an override of Iterator.next(): E is.
     */
    @Test
    public void mapGetOverrideStaysNullable() {
        KotlinPrintOptions options = new KotlinPrintOptions(new ByName(Set.of("get()")), KotlinPrintOptions.NullCheck.ASSERT);
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                abstract class Base<K, V> extends AbstractMap<K, V> {
                    @Override public abstract V get(Object key);
                }
                class Single<K, V> extends Base<K, Set<V>> {
                    private final Map<K, V> delegate = new HashMap<>();
                    @Override public Set<V> get(Object key) {
                        V v = delegate.get(key);
                        return v == null ? null : Collections.singleton(v);
                    }
                    @Override public Set<Map.Entry<K, Set<V>>> entrySet() { return Collections.emptySet(); }
                }
                """, options);
        contains(kotlin, "abstract override fun get(key: K): V?");
        contains(kotlin, "override fun get(key: K): MutableSet<V>?");
        compiles(kotlin);
    }

    /**
     * guava's NullsFirstOrdering extends Ordering<@Nullable T> and overrides {@code <S extends @Nullable T> reverse()}:
     * the bound's type-use annotation prints ({@code S : T?}), else the override's bound differs from the overridden's.
     */
    @Test
    public void typeParameterBoundDeclaredNullable() {
        KotlinPrintOptions options = new KotlinPrintOptions(new ByName(Set.of()), KotlinPrintOptions.NullCheck.ASSERT);
        String kotlin = kotlin("""
                package a;
                import java.lang.annotation.*;
                @Target(ElementType.TYPE_USE) @interface Nullable { }
                abstract class Ord<T> {
                    <S extends T> Ord<S> reverse() { throw new UnsupportedOperationException(); }
                }
                class NullsFirst<T> extends Ord<@Nullable T> {
                    @Override <S extends @Nullable T> Ord<S> reverse() { throw new UnsupportedOperationException(); }
                }
                """, options);
        contains(kotlin, "<S : T?>");
        compiles(kotlin);
    }

    /** guava's AbstractNavigableMap: {@code Iterators.<@Nullable Entry<K, V>>getNext(it, null)} keeps the annotation. */
    @Test
    public void explicitTypeArgumentDeclaredNullable() {
        KotlinPrintOptions options = new KotlinPrintOptions(new ByName(Set.of("first()")), KotlinPrintOptions.NullCheck.ASSERT);
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                import java.lang.annotation.*;
                @Target(ElementType.TYPE_USE) @interface Nullable { }
                class Its {
                    static <T> T getNext(Iterator<? extends T> it, T defaultValue) { return it.hasNext() ? it.next() : defaultValue; }
                    static String first(List<String> list) { return Its.<@Nullable String>getNext(list.iterator(), null); }
                }
                """, options);
        contains(kotlin, "getNext<String?>(");
        compiles(kotlin);
    }

    /**
     * guava's GeneralRangeTest: a local whose type argument is nullable, from a generic call whose arguments make
     * Kotlin infer it non-null. The local's type is written, so that a later Int? range fits it.
     */
    @Test
    public void localTypedWhereGenericCallInfersNonNull() {
        KotlinPrintOptions options = new KotlinPrintOptions(new ByName(Set.of("r<>")), KotlinPrintOptions.NullCheck.ASSERT);
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Ranges {
                    static <T> List<T> range(T lower, T upper) { List<T> l = new ArrayList<>(); l.add(lower); l.add(upper); return l; }
                    static int size() {
                        List<Integer> r = range(2, 4);
                        return r.size();
                    }
                }
                """, options);
        contains(kotlin, "val r: MutableList<Int?>");
        compiles(kotlin);
    }
}
