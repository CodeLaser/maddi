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
import io.codelaser.maddi.cst.api.info.TypeInfo;
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

    /**
     * nacos's AbstractRequestCallBack implements the raw RequestCallBack: a supertype takes no star projection, its
     * erasure's Any?. And RequestMeta's {@code if (abilityTable.get(key))} of a Map<String, Boolean>: unboxed, asserted.
     */
    @Test
    public void rawSupertypeAndBoxedCondition() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Response { }
                interface CallBack<T extends Response> { void onResponse(T response); }
                @SuppressWarnings("rawtypes")
                abstract class AbstractCallBack implements CallBack {
                    long timeout;
                }
                @SuppressWarnings({"rawtypes", "unchecked"})
                class Future {
                    CallBack callBack;
                    void done(Response response, Map map) {
                        callBack.onResponse(response);
                        map.put("k", response);
                    }
                }
                class Meta {
                    Map<String, Boolean> abilities = new HashMap<>();
                    int status(String key) {
                        if (abilities.get(key)) return 1;
                        return 0;
                    }
                }
                """);
        contains(kotlin, "AbstractCallBack : CallBack<Response>");
        contains(kotlin, "(callBack as CallBack<Response>).onResponse(response)");
        contains(kotlin, "(map as MutableMap<Any?, Any?>).put(");
        contains(kotlin, "if (abilities.get(key)!!)");
        compiles(kotlin);
    }

    /**
     * nacos's RequestMeta, {@code abilityTable.get(key) ? SUPPORTED : NOT_SUPPORTED}, and DistroLoadDataTask's
     * {@code !loadCompletedMap.get(each)}, of a Map<String, Boolean>: unboxed by Java, asserted, as an if's
     * condition is.
     */
    @Test
    public void boxedConditionInExpressions() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Meta {
                    Map<String, Boolean> abilities = new HashMap<>();
                    String status(String key) {
                        return abilities.get(key) ? "on" : "off";
                    }
                    boolean pending(String key) {
                        return !abilities.containsKey(key) || !abilities.get(key);
                    }
                }
                """);
        contains(kotlin, "if (abilities.get(key)!!)");
        contains(kotlin, "!abilities.get(key)!!");
        compiles(kotlin);
    }

    /**
     * nacos's DumpService: {@code registerSubscriber(new Subscriber() {…})} and ServerLoaderController's
     * {@code new RequestCallBack() {…}}, anonymous classes of a raw supertype: erased as a named class's header is,
     * so that onEvent(Event) overrides.
     */
    @Test
    public void rawAnonymousSupertype() {
        String kotlin = kotlin("""
                package a;
                class Event { }
                abstract class Subscriber<T extends Event> {
                    abstract void onEvent(T event);
                }
                interface CallBack<T extends Event> { void onResponse(T response); }
                @SuppressWarnings("rawtypes")
                class Dump {
                    static void register(Subscriber subscriber) { }
                    static void call(CallBack callBack) { }
                    void init() {
                        register(new Subscriber() {
                            @Override
                            void onEvent(Event event) { }
                        });
                        call(new CallBack() {
                            @Override
                            public void onResponse(Event response) { }
                        });
                    }
                }
                """);
        contains(kotlin, "object : Subscriber<Event>()");
        contains(kotlin, "object : CallBack<Event>");
        compiles(kotlin);
    }

    /**
     * nacos's DefaultGrpcClientConfig: {@code private String name} beside {@code name()} of the RpcClientConfig
     * interface. No enum implements the interface, so its {@code name()} stays a function, and the field keeps its
     * name beside it (where it is a property, the field is renamed, as an enum's is).
     */
    @Test
    public void nameFieldBesideNameProperty() {
        String kotlin = kotlin("""
                package a;
                interface ClientConfig {
                    String name();
                }
                class GrpcConfig implements ClientConfig {
                    private String name = "grpc";
                    @Override
                    public String name() {
                        return this.name;
                    }
                    public void setName(String name) {
                        this.name = name;
                    }
                }
                """);
        contains(kotlin, "private var name: String = \"grpc\"");
        contains(kotlin, "override fun name(): String = this.name");
        compiles(kotlin);
    }

    /**
     * jenkins's {@code @Extension(ordinal = 1000)}: an int into the annotation's {@code double ordinal()}, which Java
     * widens and Kotlin does not.
     */
    @Test
    public void annotationValueWidened() {
        String kotlin = kotlin("""
                package a;
                import java.lang.annotation.*;
                @Retention(RetentionPolicy.RUNTIME)
                @interface Extension {
                    double ordinal() default 0;
                    long size() default 1;
                }
                @Extension(ordinal = 1000, size = 2)
                class Impl { }
                @Extension(ordinal = -1)
                class Low { }
                """);
        contains(kotlin, "ordinal = 1000.0");
        contains(kotlin, "ordinal = -1.0");
        contains(kotlin, "val ordinal: Double = 0.0");
        compiles(kotlin);
    }

    /**
     * jenkins's CLICommand.handleException: a switch of type patterns, {@code case CmdLineException c -> …}, a guarded
     * one, and {@code case null, default}: Kotlin's {@code is} arms, the pattern variable the smart-cast selector,
     * a guard's {@code if}, and else.
     */
    @Test
    public void typePatternSwitch() {
        String kotlin = kotlin("""
                package a;
                class Handler {
                    static int exitCode(Throwable e) {
                        int code;
                        switch (e) {
                            case IllegalArgumentException iae -> code = iae.getMessage() == null ? 2 : 3;
                            case IllegalStateException ise when ise.getCause() != null -> code = 4;
                            case RuntimeException re -> {
                                code = 5;
                                System.out.println(re.getMessage());
                            }
                            case null, default -> code = 1;
                        }
                        return code;
                    }
                    static String kind(Object o) {
                        return switch (o) {
                            case String s -> s.trim();
                            case Integer i -> Integer.toString(i + 1);
                            default -> "?";
                        };
                    }
                }
                """);
        contains(kotlin, "is IllegalArgumentException ->");
        contains(kotlin, "is IllegalStateException if ");
        contains(kotlin, "is String -> o.trim()");
        compiles(kotlin);
    }

    /**
     * nacos's RpcConstants.ClientSuffix: a member class, not static, of constants only. Kotlin's inner class can have
     * no companion object; nothing of it uses the outer instance, so it is a nested class.
     */
    @Test
    public void innerConstantHolder() {
        String kotlin = kotlin("""
                package a;
                public class Constants {
                    public static final String PREFIX = "rpc" + Suffix.TLS;
                    public class Suffix {
                        public static final String TLS = ".tls";
                    }
                }
                """);
        assertFalse(kotlin.contains("inner"), kotlin);
        compiles(kotlin);
    }

    /**
     * nacos's ServerMemberManager.MemberInfoReportTask and TcpHealthCheckProcessor.TaskProcessor: an inner class with
     * instance members and a static constant. Kotlin's inner class has no companion object; the constant goes to the
     * enclosing class's, where the inner class finds it by its simple name.
     */
    @Test
    public void innerClassConstantHoisted() {
        String kotlin = kotlin("""
                package a;
                public class Manager {
                    private long last;
                    class ReportTask implements Runnable {
                        public static final long REPORT_INTERVAL = 50000L;
                        private long reported;
                        @Override
                        public void run() {
                            if (System.currentTimeMillis() - last > REPORT_INTERVAL) reported++;
                        }
                    }
                    class Processor {
                        private static final int MAX_WAIT = 500;
                        int waited;
                        boolean late() { return waited > MAX_WAIT; }
                    }
                }
                """);
        contains(kotlin, "inner class ReportTask");
        compiles(kotlin);
    }

    /**
     * nacos's BaseHttpMethod and ConnectionType: an enum's own {@code name} field, which would hide Kotlin's
     * Enum.name: renamed, as a field clashing with a mapped property is.
     */
    @Test
    public void enumNameField() {
        String kotlin = kotlin("""
                package a;
                enum Method {
                    GET("get"), POST("post");
                    private final String name;
                    Method(String name) {
                        this.name = name;
                    }
                    public String getName() {
                        return name;
                    }
                    static Method of(String name) {
                        for (Method m : values()) {
                            if (m.name.equals(name)) return m;
                        }
                        throw new IllegalArgumentException(name);
                    }
                }
                """);
        contains(kotlin, "_name");
        compiles(kotlin);
    }

    /**
     * nacos's RequestMeta: double-brace initialization, {@code new HashMap<String, String>(8) {{ put(…); }}}, an
     * anonymous subclass with an instance initializer: an object expression whose init block puts.
     */
    @Test
    public void doubleBraceInitialization() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Meta {
                    String version = "1";
                    Map<String, String> labels() {
                        return new HashMap<String, String>(8) {
                            {
                                put("version", version);
                                put("name", "x");
                            }
                        };
                    }
                }
                """);
        contains(kotlin, "init {");
        assertFalse(kotlin.contains("constructor()"), kotlin);
        compiles(kotlin);
    }

    /**
     * nacos's AbstractHealthChecker, annotated with its own nested class ({@code defaultImpl = None.class}), which
     * Kotlin resolves outside the class: AbstractHealthChecker.None. And NacosInjected's {@code default @NacosProperties},
     * an annotation instance: NacosProperties().
     */
    @Test
    public void annotationsNamingNestedClassesAndAnnotationDefaults() {
        String kotlin = kotlin("""
                package a;
                import a.Checker.None;
                import java.lang.annotation.*;
                @Retention(RetentionPolicy.RUNTIME) @interface Impl { Class<?> value(); String name() default ""; }
                @Retention(RetentionPolicy.RUNTIME) @interface Props { String endpoint() default ""; }
                @Retention(RetentionPolicy.RUNTIME) @interface Injected { Props properties() default @Props; }
                @Impl(value = None.class, name = None.TYPE)
                abstract class Checker {
                    static class None extends Checker { static final String TYPE = "NONE"; }
                }
                """);
        contains(kotlin, "Checker.None::class");
        contains(kotlin, "Checker.None.TYPE");
        contains(kotlin, "= Props()");
        compiles(kotlin);
    }

    /**
     * nacos's NacosProperties: an annotation type whose constants concatenate other constants, and whose parameters
     * default to them: {@code const val}s, as Kotlin wants for an annotation parameter's default.
     */
    @Test
    public void annotationDefaultsOfConcatenatedConstants() {
        String kotlin = kotlin("""
                package a;
                import java.lang.annotation.*;
                @Retention(RetentionPolicy.RUNTIME)
                @interface Props {
                    String PREFIX = "nacos.";
                    String ENDPOINT = "endpoint";
                    String ENDPOINT_PLACEHOLDER = "${" + PREFIX + ENDPOINT + ":}";
                    int TIMEOUT = 30 * 1000;
                    String endpoint() default ENDPOINT_PLACEHOLDER;
                    int timeout() default TIMEOUT;
                }
                """);
        contains(kotlin, "const val ENDPOINT_PLACEHOLDER: String");
        contains(kotlin, "const val TIMEOUT: Int");
        compiles(kotlin);
    }

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
        // an argument of a narrower type is cast up: Kotlin's extension cannot infer its T from a Set<Any> and a
        // String?, and a `!!` throws where Java finds nothing
        contains(kotlin, "names.get(k) as Any?)");
    }

    @Language("java")
    private static final String NON_NULL_FILTER = """
            package a;
            import java.util.*;
            class F {
                boolean any(List<String> names, Map<String, List<String>> m, String x) {
                    return names.stream().map(m::get).filter(Objects::nonNull).anyMatch(l -> l.contains(x))
                           || names.stream().map(m::get).filter(l -> l != null).anyMatch(l -> l.isEmpty())
                           || names.stream().map(m::get).filter(l -> l != null && l.size() > 1).anyMatch(l -> l.isEmpty());
                }
            }
            """;

    /** After Java's filter(Objects::nonNull) the elements are non-null; Kotlin's stream needs a map { it!! } to know. */
    @Test
    public void nonNullFilter() {
        String kotlin = kotlin(NON_NULL_FILTER);
        contains(kotlin, "filter(Objects ::nonNull).map { it!! }");
        contains(kotlin, "filter( { l -> l != null }).map { it!! }");
        // jenkins's LogRotator: the test in a conjunction
        contains(kotlin, "l.size > 1 }).map { it!! }");
        compiles(kotlin);
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

    @Language("java")
    private static final String READ_ONLY = """
            package a;
            import java.util.*;
            class RO {
                private static int count(List<String> items) {
                    int n = 0;
                    for (String s : items) n += s.length();
                    return n;
                }
                private static void fill(List<String> items) { items.add("x"); }
                private static List<String> same(List<String> items) { return items; }
                int open(List<String> items) { return items.size(); }
                private static int sizeOf(Map<String, Object> map) { check(map); return describe(map) + Objects.hashCode(map); }
                private static void check(Object o) { }
                private static int describe(Map<String, Object> m) { return m.size(); }
                private static int stuffs(Map<String, Object> m) { fillMap(m); return 0; }
                private static void fillMap(Map<String, Object> m) { m.put("a", 1); }
                private static void touch(List<StringBuilder> items) { for (StringBuilder sb : items) sb.append("x"); }
            }
            """;

    /**
     * A collection parameter the modification analysis proves unmodified gets Kotlin's read-only (covariant) type, in
     * a method nothing can override. Here the analysis result is set by hand on count's and open's parameters.
     */
    @Test
    public void readOnlyParameters() {
        String kotlin = kotlin(READ_ONLY, type -> type.methods().stream()
                .filter(m -> !m.name().startsWith("fill"))
                .forEach(m -> m.parameters().forEach(p -> {
                    if (m.name().equals("touch")) {
                        // deep: modified through its elements; structural: the list itself is not
                        p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER,
                                io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.FALSE);
                        p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.STRUCTURALLY_UNMODIFIED_PARAMETER,
                                io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE);
                    } else {
                        p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER,
                                io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE);
                    }
                })));
        contains(kotlin, "fun count(items: List<String>): Int");
        contains(kotlin, "fun fill(items: MutableList<String>)");
        // the structural verdict decides: a List<StringBuilder> whose elements are appended to is a read-only List
        contains(kotlin, "fun touch(items: List<StringBuilder>)");
        // unmodified, but returned where a MutableList is declared: only a parameter that is merely read
        contains(kotlin, "fun same(items: MutableList<String>): MutableList<String>");
        // open: overridable, so its parameter keeps Java's mutable type whatever the analysis says
        contains(kotlin, "fun open(items: MutableList<String>): Int");
        // passed on: to a non-collection parameter, a library method, a read-only parameter -- read; to one that is not
        contains(kotlin, "fun sizeOf(map: Map<String, Any>): Int");
        contains(kotlin, "fun describe(m: Map<String, Any>): Int");
        contains(kotlin, "fun stuffs(m: MutableMap<String, Any>): Int");
    }

    @Language("java")
    private static final String READ_ONLY_PASSED_ON = """
            package a;
            import java.util.*;
            class J {
                private static final Set<String> KEYS = Set.of("type", "description");
                static <T> T ensureNotNull(T object, String name) {
                    if (object == null) throw new IllegalArgumentException(name);
                    return object;
                }
                public static String fromMap(Map<String, Object> map) {
                    ensureNotNull(map, "map");
                    if (map.containsKey("$ref")) {
                        Object ref = map.get("$ref");
                        if (!isRepresentable(map, KEYS)) return rawFallback(map);
                        return String.valueOf(ref);
                    }
                    return new StringBuilder().append(optionalString(map, "description")).toString();
                }
                private static boolean isRepresentable(Map<String, Object> map, Set<String> allowedKeys) {
                    return allowedKeys.containsAll(map.keySet()) && !hasNullValue(map);
                }
                private static boolean hasNullValue(Map<String, Object> map) {
                    for (Object value : map.values()) if (value == null) return true;
                    return false;
                }
                private static String rawFallback(Map<String, Object> map) { return String.valueOf(map); }
                private static final ThreadLocal<Map<String, Object>> CURRENT = new ThreadLocal<>();
                private static void keep(Map<String, Object> map) { CURRENT.set(map); }
                private static String optionalString(Map<String, Object> map, String field) {
                    Object v = map.get(field);
                    return v instanceof String s ? s : null;
                }
            }
            """;

    /** langchain4j's JsonSchemaElementJsonUtils.fromMap: an unmodified map passed on only to read-only parameters. */
    @Test
    public void readOnlyPassedOn() {
        String kotlin = kotlin(READ_ONLY_PASSED_ON, type -> type.methods().forEach(m -> m.parameters().forEach(
                p -> p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER,
                        io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE))));
        contains(kotlin, "fun fromMap(map: Map<String, Any>): String");
        contains(kotlin, "fun isRepresentable(map: Map<String, Any>, allowedKeys: Set<String>): Boolean");
        // ThreadLocal<MutableMap<String, Any>>.set(value: T): the receiver's T is the mutable type
        contains(kotlin, "fun keep(map: MutableMap<String, Any>)");
    }

    /**
     * guava's ImmutableIntArray.Builder: addAll(Iterable), where it is modified, stays mutable; the addAll(Collection)
     * it calls then stays mutable too, or a MutableCollection argument fits both, neither more specific in Kotlin.
     */
    @Test
    public void overloadReadOnlyOnlyWithTheWiderOne() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                final class B {
                    int n;
                    B addAll(Iterable<Integer> values) {
                        if (values instanceof Collection) return addAll((Collection<Integer>) values);
                        Iterator<Integer> it = values.iterator();
                        while (it.hasNext()) { n += it.next(); it.remove(); }
                        return this;
                    }
                    B addAll(Collection<Integer> values) { n += values.size(); return this; }
                }
                """, type -> type.methods().forEach(m -> m.parameters().stream()
                .filter(p -> p.parameterizedType().typeInfo().simpleName().equals("Collection"))
                .forEach(p -> p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER,
                        io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE))));
        contains(kotlin, "fun addAll(values: MutableCollection<Int>)");
        compiles(kotlin);
    }

    /**
     * As {@link #overloadReadOnlyOnlyWithTheWiderOne}, as guava has it: addAll(Iterable) only reads its values, both
     * are unmodified. The Iterable's read-only form is Kotlin's Iterable, of which a cast MutableCollection is a
     * subtype as much as of Collection: the two print as one, both read-only or both mutable.
     */
    @Test
    public void overloadsReadOnlyTogether() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                final class B {
                    int n;
                    B addAll(Iterable<Integer> values) {
                        if (values instanceof Collection) return addAll((Collection<Integer>) values);
                        for (Integer value : values) n += value;
                        return this;
                    }
                    B addAll(Collection<Integer> values) { n += values.size(); return this; }
                }
                """, type -> type.methods().forEach(m -> m.parameters().forEach(p -> p.analysis().set(
                io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER,
                io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE))));
        contains(kotlin, "fun addAll(values: MutableCollection<Int>)");
        compiles(kotlin);
    }

    /**
     * guava's FilteredEntryMultimap.AsMap, an AbstractMap<K, Collection<V>>: remove(Object key) and containsValue's
     * Object take the type argument as it is, MutableCollection<V>, not addAll's read-only Collection<E>; ArrayTable's
     * ColumnMap.put(key, value: V) with V a Map takes MutableMap.
     */
    @Test
    public void mappedOverrideOfACollectionTypeArgument() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                final class AsMap<K, V> extends AbstractMap<K, Collection<V>> {
                    public Set<Map.Entry<K, Collection<V>>> entrySet() { return new HashSet<>(); }
                    public boolean containsValue(Object value) { return false; }
                    public Collection<V> put(K key, Collection<V> value) { throw new UnsupportedOperationException(); }
                }
                """);
        contains(kotlin, "override fun containsValue(value: MutableCollection<V>): Boolean");
        contains(kotlin, "override fun put(key: K, value: MutableCollection<V>)");
        compiles(kotlin);
    }

    /**
     * guava's ObjectArrays.toArrayImpl(Collection<?> c): c only read, but passed on to fillArray(Iterable<?>), whose
     * translated parameter is a MutableIterable: c stays a MutableCollection.
     */
    @Test
    public void readOnlyNotIntoATranslatedIterable() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                final class Arr {
                    static int toArrayImpl(Collection<?> c) { return fillArray(c, c.size()); }
                    private static int fillArray(Iterable<?> elements, int n) {
                        for (Object element : elements) n += element.hashCode();
                        return n;
                    }
                }
                """, type -> type.methods().forEach(m -> m.parameters().forEach(
                p -> p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER,
                        io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE))));
        contains(kotlin, "fun toArrayImpl(c: MutableCollection<*>)");
        compiles(kotlin);
    }

    /**
     * jenkins's Fingerprint.getFacets: Iterators.sequence(facets.iterator(), transientFacets.iterator()), whose
     * translated varargs parameter is a MutableIterator: what is iterated stays mutable.
     */
    @Test
    public void readOnlyNotIntoATranslatedIteratorVarargs() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                final class Its {
                    static Iterator<String> both(Collection<String> a, Collection<String> b) {
                        return sequence(a.iterator(), b.iterator());
                    }
                    @SafeVarargs
                    private static <T> Iterator<T> sequence(Iterator<? extends T>... iterators) {
                        List<T> all = new ArrayList<>();
                        for (Iterator<? extends T> it : iterators) while (it.hasNext()) all.add(it.next());
                        return all.iterator();
                    }
                }
                """, type -> type.methods().forEach(m -> m.parameters().forEach(
                p -> p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER,
                        io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE))));
        contains(kotlin, "fun both(a: MutableCollection<String>, b: MutableCollection<String>)");
        compiles(kotlin);
    }

    /**
     * guava's RegularImmutableMap.fromEntryArrayCheckingBucketOverflow: {@code Entry[] entries = n == entryArray.length
     * ? entryArray : new Entry[n]}, an invariant local: entryArray escapes through the conditional, and stays invariant.
     */
    @Test
    public void arrayParameterEscapingThroughAConditional() {
        String kotlin = kotlin("""
                package a;
                final class R {
                    static int fill(int n, String[] entryArray) {
                        String[] entries = n == entryArray.length ? entryArray : new String[n];
                        entries[0] = "x";
                        return entries.length;
                    }
                }
                """);
        assertFalse(kotlin.contains("Array<out"), kotlin);
        compiles(kotlin);
    }

    /**
     * guava's Sets.union: {@code Iterator<? extends E> itr1 = set1.iterator()} in an anonymous AbstractIterator, set1
     * a read-only Set, the field only advanced: Kotlin's read-only Iterator.
     */
    @Test
    public void advancedOnlyIteratorField() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                final class U {
                    static <E> Iterator<E> union(Set<? extends E> set1) {
                        return new Iterator<E>() {
                            final Iterator<? extends E> itr1 = set1.iterator();
                            public boolean hasNext() { return itr1.hasNext(); }
                            public E next() { return itr1.next(); }
                        };
                    }
                }
                """, type -> type.methods().forEach(m -> m.parameters().forEach(
                p -> p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER,
                        io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE))));
        contains(kotlin, "union(set1: Set<out E>)");
        contains(kotlin, "val itr1: Iterator<out E> = set1.iterator()");
        compiles(kotlin);
    }

    /**
     * guava's Maps.removeOnlySet(Set<E> set): a ForwardingSet whose delegate() returns set, a MutableSet: the
     * anonymous class's use counts, and set stays mutable.
     */
    @Test
    public void parameterReturnedByAnAnonymousClassStaysMutable() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                import java.util.function.Supplier;
                final class M {
                    static <E> Supplier<Set<E>> removeOnly(Set<E> set) {
                        return new Supplier<Set<E>>() {
                            public Set<E> get() { return set; }
                        };
                    }
                }
                """, type -> type.methods().forEach(m -> m.parameters().forEach(
                p -> p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER,
                        io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE))));
        contains(kotlin, "removeOnly(set: MutableSet<E>)");
        compiles(kotlin);
    }

    /**
     * guava's ObjectArrays.newArray(T[] reference, int length): {@code T[] empty = reference.length == 0 ? reference :
     * Arrays.copyOf(reference, 0)}, the local only handed to Arrays.copyOf: reference stays Array<out T>.
     */
    @Test
    public void arrayParameterThroughAReadOnlyLocal() {
        String kotlin = kotlin("""
                package a;
                import java.util.Arrays;
                final class O {
                    static <T> T[] newArray(T[] reference, int length) {
                        T[] empty = reference.length == 0 ? reference : Arrays.copyOf(reference, 0);
                        return Arrays.copyOf(empty, length);
                    }
                }
                """);
        contains(kotlin, "reference: Array<out T>");
        compiles(kotlin);
    }

    /**
     * The ablation switch (KotlinConf 2027): with {@link io.codelaser.maddi.cst.impl.analysis.ModificationFacts#NONE}
     * the printer ignores the stored modification verdicts, and every collection keeps Java's mutable type.
     */
    @Test
    public void readOnlyNeedsTheModificationFacts() {
        String kotlin = kotlin(READ_ONLY_PASSED_ON.replace("class J", "class J2"),
                KotlinPrintOptions.DEFAULT.withModification(io.codelaser.maddi.cst.impl.analysis.ModificationFacts.NONE),
                type -> type.methods().forEach(m -> m.parameters().forEach(
                        p -> p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER,
                                io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE))));
        contains(kotlin, "fun fromMap(map: MutableMap<String, Any>): String");
        contains(kotlin, "fun isRepresentable(map: MutableMap<String, Any>, allowedKeys: MutableSet<String>): Boolean");
    }

    @Language("java")
    private static final String GENERIC_OVERRIDE = """
            package a;
            interface Event {
                <T extends Event> Class<T> eventClass();
                <T extends Event> Builder<T> toBuilder();
                abstract class Builder<T extends Event> { }
            }
            interface Completed extends Event {
                default Class<Completed> eventClass() { return Completed.class; }
                default CompletedBuilder toBuilder() { return new CompletedBuilder(); }
                class CompletedBuilder extends Event.Builder<Completed> { }
            }
            """;

    /**
     * Java's unchecked override of a generic member by a non-generic one ({@code langchain4j}'s
     * {@code AiServiceEvent.eventClass()}): Kotlin needs the member's type parameters and result, and casts.
     */
    @Test
    public void genericMemberOverridden() {
        String kotlin = kotlin(GENERIC_OVERRIDE);
        contains(kotlin, "override fun <T : Event> eventClass(): Class<T> = (Completed::class.java) as Class<T>");
        contains(kotlin, "override fun <T : Event> toBuilder(): Builder<T> = (CompletedBuilder()) as Builder<T>");
    }

    @Language("java")
    private static final String ANNOTATION_ARGUMENTS = """
            package a;
            import java.lang.annotation.*;
            @Target({ElementType.TYPE, ElementType.METHOD})
            @interface Types {
                Type[] value();
                @interface Type { String name(); }
            }
            @interface Tags { String[] value(); }
            @interface Tool { String name() default ""; String[] value() default ""; }
            @Types({@Types.Type(name = "a"), @Types.Type(name = "b")})
            @Tags("x")
            class Annotated { @Tool("search") void search() { } }
            """;

    /**
     * A Java annotation's array {@code value} is a vararg to Kotlin; a translated annotation class's is an Array
     * property, so a single value takes brackets. A member annotation type is named through its enclosing type.
     */
    @Test
    public void annotationArguments() {
        String kotlin = kotlin(ANNOTATION_ARGUMENTS);
        contains(kotlin, "@Target(ElementType.TYPE, ElementType.METHOD)");
        contains(kotlin, "@Types([Types.Type(name = \"a\"), Types.Type(name = \"b\")])");
        contains(kotlin, "@Tags([\"x\"])");
        // value is not Tool's first element
        contains(kotlin, "@Tool(value = [\"search\"])");
    }

    @Language("java")
    private static final String CREATOR = """
            package a;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME) @interface Creator { }
            @Retention(RetentionPolicy.RUNTIME) @interface Property { String value(); }
            abstract class TextMixin {
                @Creator
                TextMixin(@Property("text") String text) { }
            }
            """;

    /** Jackson's mix-in: the creator annotation needs the primary constructor's keyword, the parameter its own. */
    @Test
    public void annotatedPrimaryConstructor() {
        String kotlin = kotlin(CREATOR);
        contains(kotlin, "abstract class TextMixin @Creator constructor(@Property(\"text\") text: String)");
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
        contains(kotlin, "class Copy(@JvmField val n: Int) : Cloneable {");
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
        contains(kotlin, "@JvmRecord data class Point(val x: Int, val y: Int) {"); // a JVM record: Java calls p.x()
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
        contains(kotlin, "val c: Int"); // declared without a value, assigned once: Kotlin checks it as javac does
        contains(kotlin, "var x = 1");
        contains(kotlin, "val y = 2");
    }

    private static final String ACCESSOR_CLASH = """
            package a;
            class C {
                protected int pointer = 0;
                protected boolean isOpen;
                int count = 0;
                private final String hidden = "h";
                public int getPointer() { return pointer; }
                public void setPointer(int pointer) { this.pointer = pointer; }
                public boolean isOpen() { return isOpen; }
                public String getHidden() { return hidden; }
                static class N {
                    private int depth = 0;
                    int getDepth() { return depth; }
                    private int[] data;
                    N(int n) { data = new int[n]; }
                    int[] getData() { return data; }
                }
            }
            """;

    /**
     * A property whose accessor has the JVM name of a method of its class is a {@code @JvmField}, without accessors:
     * {@code var pointer} next to {@code getPointer()} is a platform declaration clash. A private property has no
     * accessors; {@code isOpen}'s getter is {@code isOpen()}.
     */
    @Test
    public void accessorClash() {
        String kotlin = kotlin(ACCESSOR_CLASH);
        contains(kotlin, "@JvmField var pointer: Int = 0");
        contains(kotlin, "@JvmField var isOpen: Boolean = false");
        contains(kotlin, "@JvmField var count: Int = 0"); // a public field: Java code reads it as a field
        contains(kotlin, "\n    private val hidden: String = \"h\"");
        contains(kotlin, "@JvmField internal val depth: Int = 0");
        contains(kotlin, "@get:JvmName(\"data\\$get\") @set:JvmName(\"data\\$set\") internal lateinit var data: IntArray");
    }

    @Language("java")
    private static final String JAVA_STATICS = """
            package a;
            class C {
                static int counter = 0;
                private static int hidden = 1;
                static final String NAME = "n";
                static int next() { return counter++; }
                private static int peek() { return hidden; }
            }
            interface Codes {
                int ONE = 1;
                java.util.List<String> NAMES = java.util.List.of("a");
            }
            interface Lists {
                java.util.List<String> EMPTY = java.util.List.of();
            }
            """;

    /**
     * Static members go to the companion object; Java code reaches them as {@code C.counter} and {@code C.next()}:
     * {@code @JvmField} and {@code @JvmStatic}. Private ones, and constants, need neither. An interface's fields are
     * final.
     */
    @Test
    public void staticsForJavaCallers() {
        String kotlin = kotlin(JAVA_STATICS);
        contains(kotlin, "@JvmField var counter: Int = 0");
        contains(kotlin, "private val hidden: Int = 1");
        contains(kotlin, "const val NAME: String = \"n\"");
        contains(kotlin, "@JvmStatic fun next(): Int");
        contains(kotlin, "private fun peek(): Int");
        // an interface's fields are final; its companion's properties are all @JvmField, or none
        contains(kotlin, "const val ONE: Int = 1");
        contains(kotlin, "\n        val NAMES: MutableList<String>");
        contains(kotlin, "@JvmField val EMPTY: MutableList<String>");
    }

    @Language("java")
    private static final String ANNOTATIONS = """
            package a;
            import java.lang.annotation.*;
            import java.util.concurrent.TimeUnit;
            @Retention(RetentionPolicy.RUNTIME) @interface Timed { long value(); TimeUnit unit() default TimeUnit.SECONDS; }
            @Retention(RetentionPolicy.RUNTIME) @interface Marker { }
            @Retention(RetentionPolicy.RUNTIME) @interface Source { Class<?> value(); }
            @Retention(RetentionPolicy.RUNTIME) @interface Tags { String[] value(); }
            @Timed(value = 60, unit = TimeUnit.SECONDS)
            @Source(String.class)
            class C {
                @Marker int count;
                @Marker @Tags({"a", "b"}) public void run() { }
                @Override public String toString() { return "c"; }
                @Deprecated public void old() { }
                @SuppressWarnings("unchecked") public void raw() { }
            }
            """;

    /**
     * Java's annotations in Kotlin's syntax: named arguments, a class as {@code X::class}, an array as {@code [a, b]}.
     * {@code @Override} is the modifier, {@code @SuppressWarnings} goes, {@code @Deprecated} is Kotlin's.
     */
    @Test
    public void annotations() {
        String kotlin = kotlin(ANNOTATIONS);
        contains(kotlin, "@Timed(value = 60, unit = TimeUnit.SECONDS) @Source(String::class)");
        contains(kotlin, "@Marker @JvmField var count: Int = 0");
        contains(kotlin, "@Marker @Tags([\"a\", \"b\"]) open fun run()");
        contains(kotlin, "override fun toString(): String");
        assertFalse(kotlin.contains("@Override"), kotlin);
        contains(kotlin, "@Deprecated(\"\") open fun old()");
        assertFalse(kotlin.contains("@SuppressWarnings"), kotlin);
    }

    @Language("java")
    private static final String DUPLICATE_IMPORT = """
            package a;
            import java.util.List;
            import java.net.URI;
            import java.util.List;
            class C {
                List<URI> uris;
            }
            """;

    /** Java accepts the same import twice; the import computer keeps one. */
    @Test
    public void duplicateImport() {
        String kotlin = kotlin(DUPLICATE_IMPORT);
        contains(kotlin, "var uris: MutableList<URI>");
    }

    @Language("java")
    private static final String ANNOTATION_TYPES = """
            package a;
            import java.lang.annotation.*;
            import static java.lang.annotation.ElementType.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({METHOD, PACKAGE})
            @interface Tool {
                String name() default "";
                String[] value() default "";
                Class<?> type() default Object.class;
                boolean required() default true;
                String NO_DEFAULT = "none";
            }
            @interface Marker { }
            class C {
                @Tool(name = "n") public void run() { }
                static String join(Tool tool) { return tool.name() + tool.value().length; }
            }
            """;

    /**
     * Java's {@code @interface} is Kotlin's {@code annotation class}: the elements are the constructor's properties
     * (a {@code Class} a {@code KClass}, a single default for an array the array), read as properties. Java's
     * meta-annotations stay: they keep Java's targets ({@code PACKAGE}) and retention in the class file.
     */
    @Test
    public void annotationTypes() {
        String kotlin = kotlin(ANNOTATION_TYPES);
        contains(kotlin, "annotation class Tool(");
        contains(kotlin, "val name: String = \"\"");
        contains(kotlin, "val value: Array<String> = [\"\"]");
        contains(kotlin, "val type: kotlin.reflect.KClass<*> = Any::class");
        contains(kotlin, "val required: Boolean = true");
        contains(kotlin, "const val NO_DEFAULT: String = \"none\"");
        contains(kotlin, "annotation class Marker");
        contains(kotlin, "tool.name + tool.value.size");
        assertFalse(kotlin.contains(": Annotation"), kotlin);
    }

    @Language("java")
    private static final String DECLARATIONS = """
            package a;
            class D {
                interface Factory { <T> T create(Class<T> c); }
                interface Result<T> { T value(); String error(); }
                record Ok<T>(T value) implements Result<T> { public String error() { return "none"; } }
                static class Early {
                    private final String name;
                    Early(String n) {
                        if (n == null) {
                            this.name = "none";
                            return;
                        }
                        this.name = n;
                    }
                }
                static class Holder<T> {
                    private final Long total;
                    private final T content;
                    private final String label;
                    Holder(Long total, T content) { this(total, content, "x"); }
                    Holder(Long total, T content, String label) { this.total = total; this.content = content; this.label = label; }
                    Long total() { return total; }
                    T content() { return content; }
                    String label() { return label; }
                }
            }
            """;

    /**
     * Kotlin has no generic {@code fun interface} method, no property overriding a function and no {@code return} in
     * an init block; a final field assigned in secondary constructors is a {@code val}, as kotlinc checks those as
     * javac does.
     */
    @Test
    public void declarations() {
        String kotlin = kotlin(DECLARATIONS);
        assertTrue(kotlin.contains("    interface Factory {"), kotlin);
        assertFalse(kotlin.contains("@JvmRecord"), kotlin);
        assertTrue(kotlin.contains("override fun value(): T = value"), kotlin);
        assertTrue(kotlin.contains("constructor(n: String) {"), kotlin);
        // final fields are vals without a primary constructor too: each secondary constructor assigns them once
        assertTrue(kotlin.contains("internal val total: Long\n"), kotlin);
        assertTrue(kotlin.contains("internal val content: T\n"), kotlin);
        assertTrue(kotlin.contains("internal val label: String\n"), kotlin);
        assertTrue(kotlin.contains("internal val name: String\n"), kotlin);
    }

    @Language("java")
    private static final String READ_ONLY_FIELDS = """
            package a;
            import java.util.*;
            final class R {
                private final List<String> names;
                private final List<String> added = new ArrayList<>();
                private final Map<String, Integer> counts;
                private final List<String> leaked;
                private final List<String> tagged = new ArrayList<>();
                private final List<String> copied;
                private final List<String> wrapped = new ArrayList<>();
                R(List<String> names, Map<String, Integer> counts) {
                    this.names = names;
                    this.counts = counts;
                    this.leaked = new ArrayList<>(names);
                    this.copied = same(names);
                }
                static <T> T same(T t) { return t; }
                int copies() { return copied.size(); }
                Optional<List<String>> wrapped() { return Optional.of(wrapped); }
                List<String> names() { return names; }
                void add(String s) { added.add(s); }
                int total() {
                    int n = 0;
                    for (String s : names) n += counts.getOrDefault(s, 0);
                    return n + Objects.hash(names, counts);
                }
                List<String> leaked() { List<String> copy = leaked; return copy; }
                List<String> tagged() { return Objects.requireNonNull(tagged); }
                public String toString() { return "R" + names; }
            }
            """;

    /**
     * A private collection field the analysis proves structurally unmodified, every read of which a read-only type
     * allows, gets Kotlin's read-only type; so does the result of a closed method that returns it. Set by hand here on
     * all fields but {@code added}.
     */
    @Test
    public void readOnlyFields() {
        String kotlin = kotlin(READ_ONLY_FIELDS, type -> {
            type.fields().stream()
                    .filter(f -> !f.name().equals("added"))
                    .forEach(f -> f.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.STRUCTURALLY_UNMODIFIED_FIELD,
                            io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE));
            // what the analysis computes for same(T): it returns its argument
            type.methods().stream().filter(m -> m.name().equals("same"))
                    .forEach(m -> m.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.IDENTITY_METHOD,
                            io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE));
            notModified(type, "size", "getOrDefault");
        });
        contains(kotlin, "private val names: List<String>");
        contains(kotlin, "private val counts: Map<String, Int>");
        contains(kotlin, "fun names(): List<String> = names");
        // modified: Java's mutable type
        contains(kotlin, "private val added: MutableList<String>");
        // read into a local: Kotlin would infer the local's type from it
        contains(kotlin, "private val leaked: MutableList<String>");
        contains(kotlin, "fun leaked(): MutableList<String>");
        // into a type variable's parameter: handed back as T, to whatever the caller declares
        contains(kotlin, "private val tagged: MutableList<String>");
        // through an @Identity call into another read-only field: names stays read-only, and so is copied
        contains(kotlin, "private val copied: List<String>");
        // Optional.of(T): T comes back in the result, Optional<MutableList<String>>
        contains(kotlin, "private val wrapped: MutableList<String>");
    }

    @Language("java")
    private static final String READ_ONLY_IN_WORLD = """
            package a;
            import java.util.*;
            class Box {
                final List<String> names;
                final List<String> grown = new ArrayList<>();
                final List<String> handed;
                final int size;
                Box(List<String> names, List<String> handed) {
                    this.names = names;
                    this.handed = handed;
                    this.size = names.size() + handed.size();
                }
                List<String> names() { return names; }
                List<String> handed() { return handed; }
            }
            class Reader {
                int count(Box b) {
                    int n = 0;
                    for (String s : b.names) n += s.length();
                    return n + b.names().size();
                }
                void grow(Box b) { b.grown.add("x"); }
                void take(Box b) { fill(b.handed()); }
                static void fill(List<String> target) { target.add("y"); }
            }
            """;

    private static void unmodifiedFields(TypeInfo type) {
        type.fields().forEach(f -> f.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.STRUCTURALLY_UNMODIFIED_FIELD,
                io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE));
        notModified(type, "size");
    }

    /**
     * With the code translated together ({@link KotlinWorld}), a package-private field and an open method's result
     * are read-only when every use of them in the world allows it: {@code Reader} only reads {@code names}. Alone, the
     * printer cannot see {@code Reader}'s uses, and keeps Java's mutable types. A mutator called from another type
     * ({@code b.grown.add}) and a result handed to a parameter that modifies it keep the mutable type in the world too.
     */
    @Test
    public void readOnlyInWorld() {
        String world = kotlinInWorld(READ_ONLY_IN_WORLD, KotlinPrintOptions.DEFAULT, TestJavaToKotlinStructure::unmodifiedFields);
        contains(world, "val names: List<String>");
        contains(world, "fun names(): List<String> = names");
        contains(world, "val grown: MutableList<String>");
        contains(world, "val handed: MutableList<String>");
        contains(world, "fun handed(): MutableList<String> = handed");

        String alone = kotlin(READ_ONLY_IN_WORLD.replace("Box", "Alone").replace("Reader", "AloneReader"), KotlinPrintOptions.DEFAULT, TestJavaToKotlinStructure::unmodifiedFields);
        contains(alone, "val names: MutableList<String>");
        contains(alone, "fun names(): MutableList<String> = names");
    }

    @Language("java")
    private static final String LOCALS_ASSIGNED_ONCE = """
            package a;
            class L {
                static int f(boolean c, int[] xs) {
                    int a;
                    if (c) { a = 1; } else { a = 2; }
                    int b;
                    if (c) { b = 3; return b; }
                    b = 4;
                    int n;
                    n = 0;
                    n = 1;
                    int len;
                    while ((len = xs.length) > 10) { break; }
                    int k = 0;
                    int m;
                    for (int x : xs) { m = x; k += m; }
                    return a + b + n + len + k;
                }
            }
            """;

    /**
     * A local declared without a value is a {@code val} when each path assigns it at most once in a statement of its
     * own; a second assignment, one inside an expression (an {@code .also { }} lambda in Kotlin) or one in a loop
     * keeps the {@code var}.
     */
    @Test
    public void localsAssignedOnce() {
        String kotlin = kotlin(LOCALS_ASSIGNED_ONCE);
        contains(kotlin, "val a: Int");
        contains(kotlin, "val b: Int");
        contains(kotlin, "var n: Int");
        contains(kotlin, "var len: Int");
        contains(kotlin, "var m: Int");
    }

    @Language("java")
    private static final String SMART_CAST_AFTER_IS = """
            package a;
            class S {
                static int f(Object o, Object p, Object r) {
                    int n = 0;
                    if (o instanceof String) n += ((String) o).length();
                    if (p instanceof String && ((String) p).isEmpty()) n++;
                    if (!(r instanceof String)) return n;
                    n += ((String) r).length();
                    return n + (o instanceof Integer ? (Integer) o : 0);
                }
                static int g(Object o) {
                    int n = o instanceof String ? 1 : 0;
                    return n + ((String) o).length();
                }
            }
            """;

    /**
     * Java's {@code (T) x} after {@code x instanceof T}: Kotlin smart-casts x where the test holds, so no cast. Not where
     * the test does not dominate the cast.
     */
    @Test
    public void smartCastAfterIs() {
        String kotlin = kotlin(SMART_CAST_AFTER_IS);
        contains(kotlin, "if (o is String) { n += o.length }");
        contains(kotlin, "if (p is String && p.isEmpty())");
        contains(kotlin, "if (r !is String) { return n }");
        contains(kotlin, "n += r.length");
        contains(kotlin, "if (o is Int) o else 0");
        contains(kotlin, "n + (o as String).length");
    }

    @Language("java")
    private static final String BORN_IMMUTABLE = """
            package a;
            import java.util.*;
            final class Born {
                private final List<String> names;
                private final Map<String, Integer> counts = Map.of("a", 1, "b", 2);
                private final Set<String> none = Set.of();
                private final Map<Integer, Integer[]> pairs = Map.of(1, new Integer[]{2, null});
                private final List<String> grown = new ArrayList<>();
                Born(List<String> names) { this.names = List.copyOf(names); }
                List<String> names() { return names; }
                static List<String> letters() { return List.of("a", "b"); }
                List<String> grown() { return grown; }
                int size() { return names.size() + counts.size() + none.size() + pairs.size(); }
            }
            """;

    /**
     * maddi#141: a field or result only ever given an immutable value is read-only without the analysis'
     * structural verdict, and List.of prints as listOf where it lands read-only. The archive's
     * {@code @ImmutableContainer} on List.of, copyOf, Set.of and Map.of is set here as the stored property it becomes.
     */
    @Test
    public void bornImmutable() {
        String kotlin = kotlin(BORN_IMMUTABLE, type -> {
            KotlinReadOnlyTestAccess.forEachBody(type, body -> body.visit((io.codelaser.maddi.cst.api.element.Element e) -> {
                if (e instanceof io.codelaser.maddi.cst.api.expression.MethodCall mc && mc.methodInfo().isStatic()
                    && mc.methodInfo().typeInfo().fullyQualifiedName().startsWith("java.util.")
                    && !mc.methodInfo().analysis().haveAnalyzedValueFor(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.IMMUTABLE_METHOD)) {
                    mc.methodInfo().analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.IMMUTABLE_METHOD,
                            io.codelaser.maddi.cst.impl.analysis.ValueImpl.ImmutableImpl.IMMUTABLE_HC);
                }
                return true;
            }));
            notModified(type, "size");
        });
        contains(kotlin, "private val names: List<String>");
        // Map.of's iteration order is Java's own, not mapOf's insertion order: Java's call stays
        contains(kotlin, "private val counts: Map<String, Int> = java.util.Map.of(\"a\", 1, \"b\", 2)");
        contains(kotlin, "private val none: Set<String> = emptySet()");
        contains(kotlin, "fun names(): List<String> = names");
        contains(kotlin, "fun letters(): List<String> = listOf(\"a\", \"b\")");
        // in mapOf(k to v) the pair hides the field's type from the array: Java's call stays
        contains(kotlin, "= java.util.Map.of(1, arrayOf(2, null))");
        // copyOf keeps Java's call: listOf has no copying form
        contains(kotlin, "this.names = java.util.List.copyOf(names)");
        // a new ArrayList is no immutable value, and nothing proves it unmodified
        contains(kotlin, "private val grown: MutableList<String>");
    }

    @Language("java")
    private static final String UNMODIFIABLE_VIEW = """
            package a;
            import java.util.*;
            final class Viewed {
                private final List<String> names;
                private final Map<String, Integer> counts;
                private final List<String> grown = new ArrayList<>();
                Viewed(List<String> names, Map<String, Integer> counts) {
                    this.names = Collections.unmodifiableList(names);
                    this.counts = Collections.unmodifiableMap(counts);
                }
                List<String> names() { return names; }
                Map<String, Integer> counts() { return counts; }
                static List<String> wrap(List<String> xs) { return Collections.unmodifiableList(xs); }
                List<String> grown() { return grown; }
                int size() { return names.size() + counts.size() + grown.size(); }
            }
            """;

    /**
     * CodeLaser/maddi-mod#52: an unmodifiable view is not immutable (whoever holds the list it wraps can change it),
     * but nothing is added to or removed from it through this reference, which is all a read-only type says. The
     * archive's {@code @UnmodifiableView} on Collections.unmodifiable* is set here as the stored property it becomes.
     */
    @Test
    public void unmodifiableView() {
        String kotlin = kotlin(UNMODIFIABLE_VIEW, type -> {
            KotlinReadOnlyTestAccess.forEachBody(type, body -> body.visit((io.codelaser.maddi.cst.api.element.Element e) -> {
                if (e instanceof io.codelaser.maddi.cst.api.expression.MethodCall mc && mc.methodInfo().isStatic()
                    && "java.util.Collections".equals(mc.methodInfo().typeInfo().fullyQualifiedName())
                    && mc.methodInfo().name().startsWith("unmodifiable")
                    && !mc.methodInfo().analysis().haveAnalyzedValueFor(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIABLE_VIEW_METHOD)) {
                    mc.methodInfo().analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIABLE_VIEW_METHOD,
                            io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE);
                }
                return true;
            }));
            notModified(type, "size");
        });
        contains(kotlin, "private val names: List<String>");
        contains(kotlin, "private val counts: Map<String, Int>");
        contains(kotlin, "fun names(): List<String> = names");
        contains(kotlin, "fun counts(): Map<String, Int> = counts");
        contains(kotlin, "fun wrap(xs: MutableList<String>): List<String>");
        // a view is not an immutable value: no listOf, Java's call stays
        contains(kotlin, "Collections.unmodifiableList(names)");
        contains(kotlin, "private val grown: MutableList<String>");
    }

    @Language("java")
    private static final String FAMILY = """
            package a;
            import java.util.*;
            class Family {
                interface Counter { int count(List<String> names); }
                static class Plain implements Counter {
                    public int count(List<String> names) { return names.size(); }
                }
                static class Twice extends Plain {
                    @Override public int count(List<String> names) { return 2 * names.size(); }
                }
                interface Sink { void put(List<String> names); }
                static class Filler implements Sink {
                    public void put(List<String> names) { names.add("x"); }
                }
                static class Reader implements Sink {
                    public void put(List<String> names) { System.out.println(names.size()); }
                }
                interface Viewer { int view(List<String> names); }
                static Viewer viewer() { return names -> names.size(); }
                static class Shown implements Viewer {
                    public int view(List<String> names) { return names.size(); }
                }
            }
            """;

    /**
     * maddi-mod#25, the family rule: in the code translated together an open method's collection parameter is
     * read-only when it is in every member of its override family (Kotlin wants an override's parameter types exactly
     * the overridden member's). One member that adds keeps the whole family mutable; so does a lambda implementing
     * it, whose parameter Kotlin types from the interface.
     */
    @Test
    public void familyRule() {
        String world = kotlinInWorld(FAMILY, KotlinPrintOptions.DEFAULT, type -> {
            type.recursiveSubTypeStream().flatMap(TypeInfo::methodStream).forEach(m -> m.parameters().forEach(p -> {
                boolean adds = m.name().equals("put");
                p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.STRUCTURALLY_UNMODIFIED_PARAMETER,
                        io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.from(!adds));
            }));
            type.recursiveSubTypeStream().forEach(t -> notModified(t, "size"));
        });
        contains(world, "fun count(names: List<String>): Int");
        contains(world, "override fun count(names: List<String>): Int = names.size");
        contains(world, "override fun count(names: List<String>): Int = 2 * names.size");
        contains(world, "fun put(names: MutableList<String>)");
        contains(world, "fun view(names: MutableList<String>): Int");

        String alone = kotlin(FAMILY.replace("class Family", "class Alone"), KotlinPrintOptions.DEFAULT, type -> {
            type.recursiveSubTypeStream().flatMap(TypeInfo::methodStream).forEach(m -> m.parameters().forEach(p ->
                    p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.STRUCTURALLY_UNMODIFIED_PARAMETER,
                            io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE)));
            type.recursiveSubTypeStream().forEach(t -> notModified(t, "size"));
        });
        // the printer alone sees no family: open methods keep Java's mutable type
        contains(alone, "fun count(names: MutableList<String>): Int");
    }

    @Language("java")
    private static final String FAMILY_RESULTS = """
            package a;
            import java.util.*;
            class Results {
                interface Source { List<String> names(); }
                static class Fixed implements Source {
                    public List<String> names() { return List.of("a", "b"); }
                }
                static class Grower implements Source {
                    private final List<String> grown = new ArrayList<>();
                    public List<String> names() { return grown; }
                }
                interface Bag { List<String> items(); }
                static class Held implements Bag {
                    public List<String> items() { return List.of("c"); }
                }
                static int count(Source s) { return s.names().size(); }
                static void fill(Bag b) { b.items().add("d"); }
                interface Memory { List<String> messages(); }
                static class Window implements Memory {
                    private final List<String> kept = new ArrayList<>();
                    public List<String> messages() { return List.copyOf(kept); }
                }
                static void grow(Memory m) {
                    var l = Optional.ofNullable(m).map(Memory::messages).orElseGet(ArrayList::new);
                    l.add("e");
                }
            }
            """;

    /**
     * maddi-mod#25, the result side: in the code translated together an abstract method returns a read-only
     * collection when each call of it only reads, and an override may then return one too, or keep a
     * MutableList (Kotlin's results are covariant). A caller that adds keeps the whole line mutable.
     */
    @Test
    public void familyResults() {
        String world = kotlinInWorld(FAMILY_RESULTS, KotlinPrintOptions.DEFAULT, type -> {
            KotlinReadOnlyTestAccess.forEachBody(type, body -> body.visit((io.codelaser.maddi.cst.api.element.Element e) -> {
                if (e instanceof io.codelaser.maddi.cst.api.expression.MethodCall mc && mc.methodInfo().isStatic()
                    && mc.methodInfo().typeInfo().fullyQualifiedName().startsWith("java.util.")
                    && !mc.methodInfo().analysis().haveAnalyzedValueFor(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.IMMUTABLE_METHOD)) {
                    mc.methodInfo().analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.IMMUTABLE_METHOD,
                            io.codelaser.maddi.cst.impl.analysis.ValueImpl.ImmutableImpl.IMMUTABLE_HC);
                }
                return true;
            }));
            notModified(type, "size");
        });
        contains(world, "fun names(): List<String>\n");
        contains(world, "override fun names(): List<String> = listOf(\"a\", \"b\")");
        contains(world, "override fun names(): MutableList<String> = grown");
        contains(world, "fun items(): MutableList<String>\n");
        contains(world, "override fun items(): MutableList<String>");
        // a method reference may hand the result on to anything: Memory::messages, then add
        contains(world, "fun messages(): MutableList<String>\n");

        String alone = kotlin(FAMILY_RESULTS.replace("class Results", "class Alone"), type -> notModified(type, "size"));
        // the printer alone sees no world: abstract and overriding methods keep Java's mutable type
        contains(alone, "fun names(): MutableList<String>\n");
        compiles(world);
    }

    /**
     * maddi-mod#50: guava's printing did not finish. Each field's reads are assignments to the next two, so deciding
     * the first asked for every later one along each path to it: Fibonacci-many decisions of 40 fields, as nothing
     * decided inside an open chain was remembered.
     */
    @Test
    public void chainOfFieldsDecidedOnce() {
        int n = 40;
        StringBuilder source = new StringBuilder("package a;\nimport java.util.*;\nclass Chain {\n");
        for (int i = 0; i < n; i++) source.append("    private List<String> f").append(i).append(" = new ArrayList<>();\n");
        source.append("    void link() {\n");
        for (int i = 0; i + 1 < n; i++) {
            source.append("        f").append(i + 1).append(" = f").append(i).append(";\n");
            if (i + 2 < n) source.append("        f").append(i + 2).append(" = f").append(i).append(";\n");
        }
        source.append("    }\n    int size() { return f").append(n - 1).append(".size(); }\n}\n");
        String kotlin = org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(60),
                () -> kotlinInWorld(source.toString(), KotlinPrintOptions.DEFAULT, type -> {
                    type.fields().forEach(f -> f.analysis().set(
                            io.codelaser.maddi.cst.impl.analysis.PropertyImpl.STRUCTURALLY_UNMODIFIED_FIELD,
                            io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE));
                    notModified(type, "size");
                }));
        contains(kotlin, "private val f0: List<String>");
        contains(kotlin, "private var f" + (n - 1) + ": List<String>");
        compiles(kotlin);
    }

    /**
     * guava's AbstractBiMap.KeySet: an inner class's collection override takes Kotlin's parameter type through the
     * supertype chain, where the outer class's type variable stays itself ({@code contains(o: K)}). It used to fall
     * back to the supertype's formal parameter, {@code contains(o: E)}, which names nothing there.
     */
    @Test
    public void innerCollectionOverrideKeepsOuterTypeVariable() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class BiMap<K, V> {
                    private final Set<K> keys = new HashSet<>();
                    abstract static class Forwarding<E> extends AbstractSet<E> { }
                    class KeySet extends Forwarding<K> {
                        @Override public boolean contains(Object o) { return keys.contains(o); }
                        @Override public boolean remove(Object o) { return keys.remove(o); }
                        @Override public Iterator<K> iterator() { return keys.iterator(); }
                        @Override public int size() { return keys.size(); }
                    }
                }
                """);
        contains(kotlin, "override fun contains(o: K): Boolean");
        contains(kotlin, "override fun remove(o: K): Boolean");
        compiles(kotlin);
    }

    /**
     * guava's maps: Java's Map.remove(Object, Object), getOrDefault(Object, V) and putAll(Map<? extends K, ? extends V>)
     * are Kotlin's remove(key: K, value: V), getOrDefault(key: K, defaultValue: V) and putAll(from: Map<out K, V>).
     */
    @Test
    public void mapOverridesTakeKotlinsParameterTypes() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Forwarding<K, V> extends AbstractMap<K, V> {
                    private final Map<K, V> delegate = new HashMap<>();
                    @Override public Set<Map.Entry<K, V>> entrySet() { return delegate.entrySet(); }
                    @Override public boolean remove(Object key, Object value) { return delegate.remove(key, value); }
                    @Override public V getOrDefault(Object key, V defaultValue) { return delegate.getOrDefault(key, defaultValue); }
                    @Override public void putAll(Map<? extends K, ? extends V> map) { delegate.putAll(map); }
                }
                """);
        contains(kotlin, "override fun remove(key: K, value: V): Boolean");
        contains(kotlin, "override fun getOrDefault(key: K, defaultValue: V");
        contains(kotlin, "override fun putAll(map: Map<out K, V>)");
        compiles(kotlin);
    }

    /**
     * guava's AbstractMapBasedMultiset: a field named as a property a mapped supertype declares ({@code size} of
     * Kotlin's Collection) is renamed, else Kotlin has two properties {@code size} and each use is ambiguous.
     */
    @Test
    public void fieldClashingWithMappedPropertyIsRenamed() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Counted<E> extends AbstractCollection<E> {
                    private final List<E> items = new ArrayList<>();
                    private long size;
                    @Override public Iterator<E> iterator() { return items.iterator(); }
                    @Override public boolean add(E e) { size++; return items.add(e); }
                    @Override public int size() { return (int) size; }
                }
                """);
        contains(kotlin, "private var _size: Long");
        contains(kotlin, "override val size: Int");
        contains(kotlin, "_size++");
        compiles(kotlin);
    }

    /**
     * guava's ArrayTable and ObjectArrays import java.lang.reflect.Array: imported, it shadows Kotlin's Array, and each
     * {@code Array<V?>} is the reflection class, which takes no type arguments. It is written qualified instead.
     */
    @Test
    public void reflectArrayDoesNotShadowKotlinsArray() {
        String kotlin = kotlin("""
                package a;
                import java.lang.reflect.Array;
                class Arrays2<T> {
                    static <T> int count(T[][] rows) { return rows.length; }
                    @SuppressWarnings("unchecked")
                    static <T> T[] newArray(T[] reference, int length) {
                        return (T[]) Array.newInstance(reference.getClass().getComponentType(), length);
                    }
                }
                """);
        assertFalse(kotlin.contains("import java.lang.reflect.Array"), kotlin);
        contains(kotlin, "java.lang.reflect.Array\n            .newInstance(");
        contains(kotlin, "Array<Array<T");
        contains(kotlin, "reference: Array<out T>");
        compiles(kotlin);
    }

    /**
     * guava's multimaps: {@code new Values()} of an inner class of a superclass, its outer instance implicit, is
     * {@code Values()}; qualified, {@code Base.Values()}, Kotlin finds no receiver for it.
     */
    @Test
    public void innerClassOfSuperclassConstructedBySimpleName() {
        String kotlin = kotlin("""
                package a;
                abstract class Base {
                    class Values { }
                    class Wrapped { class It { } It it() { return new It(); } }
                    static class Sub extends Base {
                        Object values() { return new Values(); }
                    }
                }
                """);
        contains(kotlin, "= Values()");
        contains(kotlin, "= It()");
        assertFalse(kotlin.contains("Base.Values()"), kotlin);
        compiles(kotlin);
    }

    /**
     * guava's MapMakerInternalMap: a class's type parameter bounded by one of its own nested types. Kotlin resolves
     * the header outside the class's scope, so the nested type is named from the class.
     */
    @Test
    public void headerNamesOwnNestedTypesQualified() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Internal<K, E extends Internal.Entry<K, E>> implements Comparator<Internal.Entry<K, E>> {
                    interface Entry<K, E extends Entry<K, E>> { K key(); }
                    public int compare(Entry<K, E> a, Entry<K, E> b) { return 0; }
                }
                """);
        contains(kotlin, "class Internal<K, E : Internal.Entry<K, E>>");
        contains(kotlin, "Comparator<Internal.Entry<K, E>>");
        compiles(kotlin);
    }

    /**
     * guava's Predicate redeclares equals(Object) abstract, for its documentation. In Kotlin that abstract override would
     * oblige each implementing class to implement equals again; it is left out.
     */
    @Test
    public void interfaceRedeclaringEqualsLeavesItOut() {
        String kotlin = kotlin("""
                package a;
                interface Pred<T> {
                    boolean apply(T input);
                    @Override boolean equals(Object obj);
                    @Override int hashCode();
                }
                """);
        assertFalse(kotlin.contains("fun equals"), kotlin);
        assertFalse(kotlin.contains("fun hashCode"), kotlin);
        contains(kotlin, "fun apply(input: T): Boolean");
        compiles(kotlin);
    }

    /**
     * guava's {@code @Immutable(containerOf = "B")}: Java allows a single value for a named array element; Kotlin, for a
     * Java annotation, only for the positional {@code value}, which is a vararg. A named one is {@code ["B"]}.
     */
    @Test
    public void namedArrayElementOfJavaAnnotationIsAnArray() {
        String kotlin = kotlin("""
                package a;
                @javax.annotation.processing.Generated(value = "gen", comments = "c")
                class Old {
                    @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
                    @interface Marker { }
                }
                """);
        contains(kotlin, "value = [\"gen\"], comments = \"c\"");
        // the positional value of a Java annotation stays a vararg
        contains(kotlin, "@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)");
    }

    /**
     * guava's Forwarding collections: {@code removeAll(Collection<?> c)} is Kotlin's {@code removeAll(elements:
     * Collection<E>)}, read-only, and hands c to a translated method that takes a mutable collection. Cast, as the
     * same java.util.Collection is passed at runtime.
     */
    @Test
    public void forcedReadOnlyOverrideParameterIntoMutableIsCast() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                abstract class Fwd<E> extends AbstractCollection<E> {
                    protected boolean standardRemoveAll(Collection<?> collection) { collection.clear(); return true; }
                    @Override public boolean removeAll(Collection<?> c) { return standardRemoveAll(c); }
                }
                """);
        contains(kotlin, "override fun removeAll(c: Collection<E>): Boolean");
        contains(kotlin, "standardRemoveAll((c as MutableCollection<E>))");
        compiles(kotlin);
    }

    /**
     * guava's LocalCache and Maps: overrides of Map's default methods. Kotlin sees the JDK's merge and
     * computeIfPresent with definitely non-null value types ({@code V & Any}); an override printed with Java's types
     * overrides nothing.
     */
    @Test
    public void mapDefaultMethodOverrides() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                import java.util.function.*;
                abstract class Cache<K, V> extends AbstractMap<K, V> {
                    @Override public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> f) { return null; }
                    @Override public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> f) { return null; }
                    @Override public V compute(K key, BiFunction<? super K, ? super V, ? extends V> f) { return null; }
                    @Override public V computeIfAbsent(K key, Function<? super K, ? extends V> f) { return f.apply(key); }
                }
                """);
        compiles(kotlin);
    }

    /**
     * An inner class of a generic class named outside it, {@code G<String>.In}: Kotlin's type has the outer class's
     * type argument on the outer class too. {@code G.In} lacks it ("one type argument expected").
     */
    @Test
    public void innerOfGenericClassNamedOutsideIt() {
        String kotlin = kotlin("""
                package a;
                class Top {
                    static class G<K> {
                        K last;
                        class In { K get() { return last; } }
                    }
                    static class C {
                        String pass(G<String>.In x) { return x.get(); }
                    }
                }
                """);
        contains(kotlin, "x: G<String>.In");
        compiles(kotlin);
    }

    /**
     * guava's Maps.FilteredEntrySortedMap.SortedKeySet extends FilteredEntryMap's inner KeySet: an inner class whose
     * superclass is an inner class of a generic superclass of its outer class.
     */
    @Test
    public void innerClassExtendingInheritedInnerClass() {
        String kotlin = kotlin("""
                package a;
                class Outer {
                    static class Base<K> {
                        K last;
                        class Inner { K get() { return last; } }
                    }
                    static class Sub<K> extends Base<K> {
                        class SubInner extends Inner { }
                        Inner make() { return new SubInner(); }
                    }
                }
                """);
        compiles(kotlin);
    }

    /**
     * Java's {@code g.new In()} outside the outer class is Kotlin's {@code g.In()}: the constructor of an inner class
     * is reached through the outer instance by its simple name.
     */
    @Test
    public void innerClassConstructedOutsideItsOuterClass() {
        String kotlin = kotlin("""
                package a;
                class Top {
                    static class G<K> {
                        K last;
                        class In { K get() { return last; } }
                    }
                    static class A {
                        int n;
                        class In { int get() { return n; } }
                    }
                    static class C {
                        Object make(G<String> g) { return g.new In(); }
                        Object make(A a) { return a.new In(); }
                    }
                }
                """);
        contains(kotlin, "g.In()");
        contains(kotlin, "a.In()");
        compiles(kotlin);
    }

    /**
     * guava's Ordering.onResultOf(Entry::getValue): the method's type parameter F is fixed only by the method
     * reference's receiver. As a lambda, {@code { it.value }}, Kotlin has nothing to infer F from.
     */
    @Test
    public void typeParameterFixedByMethodReferenceOnly() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Ord<T> implements Comparator<T> {
                    interface Fn<F, T> { T apply(F f); }
                    public int compare(T a, T b) { return 0; }
                    <F> Ord<F> onResultOf(Fn<F, ? extends T> f) { return new Ord<>(); }
                    static <E> void sort(E[] a, Comparator<? super E> c) { }
                    static void sortByValue(Map.Entry<String, Integer>[] entries, Ord<Integer> ord) {
                        sort(entries, ord.onResultOf(Map.Entry::getValue));
                    }
                }
                """);
        contains(kotlin, "ord.onResultOf<");
        // toMap's K and U are the lambdas' results: Kotlin infers them, with the expected type's states (langchain4j)
        String toMap = kotlin("""
                package a;
                import java.util.*;
                import java.util.stream.*;
                class Collect {
                    static Map<String, Integer> invert(Map<Integer, String> m) {
                        return m.entrySet().stream().collect(Collectors.toMap(e -> e.getValue(), e -> e.getKey()));
                    }
                }
                """);
        contains(toMap, "Collectors.toMap(");
        compiles(toMap);
        compiles(kotlin);
    }

    /**
     * guava's multimaps: {@code map.remove(key)} with an {@code Object} key into a {@code Map<K, V>}. Kotlin's member
     * takes K, its stdlib extension infers K from the inputs alone, so the argument is cast to {@code K?}: unchecked,
     * as Java's lookup is, and a null key finds nothing instead of {@code !!} throwing.
     */
    @Test
    public void objectLookupIntoTypeVariableIsCast() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Multi<K, V> {
                    private final Map<K, Collection<V>> map = new HashMap<>();
                    private final Set<V> values = new HashSet<>();
                    int removeAll(Object key) {
                        Collection<V> collection = map.remove(key);
                        return collection == null ? 0 : collection.size();
                    }
                    boolean has(Object key, Object value) {
                        return map.containsKey(key) && map.get(key) != null && values.contains(value);
                    }
                }
                """);
        contains(kotlin, "map.remove((key as K?))");
        contains(kotlin, "values.contains((value as V?))");
        compiles(kotlin);
    }

    /**
     * A V looked up in a Set<V> or a Map<V, String>: Java's contains and get take an Object, and a null V finds
     * nothing (false, null). Kotlin's member takes the V itself, which may be null: no `!!`, which would throw.
     */
    @Test
    public void sameTypeVariableLookupNotAsserted() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Look<V> {
                    private final Set<V> values = new HashSet<>();
                    private final Map<V, String> names = new HashMap<>();
                    boolean hasValue(V v) { return values.contains(v); }
                    boolean named(V v) { return names.get(v) != null; }
                    boolean add(V v) { return values.add(v); }
                }
                """);
        contains(kotlin, "values.contains(v)");
        contains(kotlin, "names.get(v) != null");
        contains(kotlin, "values.add(v)");
        assertFalse(kotlin.contains("v!!"), () -> "a null v finds nothing in Java; the printed Kotlin asserts it non-null:\n" + kotlin);
        compiles(kotlin);
    }

    /**
     * guava's collectors: {@code Collector.of(Acc::create, Acc::add, Acc::combine, Acc::result)} on a generic class
     * of ours. Kotlin wants the receiver's type arguments on an unbound reference ({@code Acc<T>::add}, from the
     * functional type's first parameter), and finds a static in the companion ({@code Acc.Companion::create}).
     */
    @Test
    public void referencesIntoGenericClass() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                import java.util.stream.*;
                class Acc<T> {
                    private final List<T> items = new ArrayList<>();
                    static <T> Acc<T> create() { return new Acc<>(); }
                    void add(T t) { items.add(t); }
                    Acc<T> combine(Acc<T> other) { items.addAll(other.items); return this; }
                    List<T> result() { return items; }
                    static Collector<Object, ?, List<Object>> all() {
                        return Collector.of(Acc::create, Acc::add, Acc::combine, Acc::result);
                    }
                }
                """);
        contains(kotlin, "Acc.Companion ::create");
        contains(kotlin, "Acc<Any> ::add");
        contains(kotlin, "Acc<Any> ::combine");
        compiles(kotlin);
    }

    /**
     * guava's primitives: Java narrows an int constant into a char, byte or short implicitly
     * ({@code char MIN = 0}, {@code byte MAX_POWER_OF_TWO = 1 << 6}) and widens a char where an int goes in, and an
     * int assigned inside a condition into a long ({@code (read = in.read(buf)) != -1}). Kotlin converts explicitly.
     */
    @Test
    public void implicitPrimitiveConversions() {
        String kotlin = kotlin("""
                package a;
                import java.io.*;
                import java.util.BitSet;
                class Prim {
                    static final char MIN = 0;
                    static final char CASE_MASK = 0x20;
                    static final byte MAX_POWER_OF_TWO = 1 << 6;
                    static final short SHORT_POWER = 1 << (Short.SIZE - 2);
                    private char safeMax;
                    void reset() { safeMax = 0; }
                    static String hex(char ch) { return Integer.toHexString(ch); }
                    static void mark(BitSet table, char c) { table.set(c); }
                    static long copy(InputStream in, byte[] buf) throws IOException {
                        long total = 0;
                        long read;
                        while ((read = in.read(buf)) != -1) { total += read; }
                        return total;
                    }
                    static long mix(long h, int length) { h ^= length; return h ^ length; }
                }
                """);
        compiles(kotlin);
    }

    /**
     * guava's {@code Maps.transformValues(map, fn)} beside its SortedMap overload: a cast that keeps Java's overload
     * choice cannot name the callee's own type parameters ({@code Function<in V1, V2>}), which only it knows.
     */
    @Test
    public void overloadCastNamesNoCalleeTypeParameter() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                interface Fn<F, T> { T apply(F input); }
                class Maps2 {
                    static <K, V1, V2> Map<K, V2> transformValues(Map<K, V1> from, Fn<? super V1, V2> f) {
                        Map<K, V2> result = new HashMap<>();
                        for (Map.Entry<K, V1> e : from.entrySet()) result.put(e.getKey(), f.apply(e.getValue()));
                        return result;
                    }
                    static <K, V1, V2> SortedMap<K, V2> transformValues(SortedMap<K, V1> from, Fn<? super V1, V2> f) {
                        return new TreeMap<>(transformValues((Map<K, V1>) from, f));
                    }
                    static Map<String, Integer> lengths(Map<String, String> m) { return transformValues(m, String::length); }
                    static Map<String, Set<Integer>> copies(Map<String, List<Integer>> m) {
                        return transformValues(m, HashSet::new);
                    }
                }
                """);
        assertFalse(kotlin.contains("as Fn<"), kotlin);
        compiles(kotlin);
    }

    /**
     * guava's {@code WrappedSet extends WrappedCollection implements Set<V>}: spliterator() comes twice,
     * WrappedCollection's own and Set's default. Java takes the class's; Kotlin wants it written.
     */
    @Test
    public void inheritedDefaultsChosenExplicitly() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Wrapped<V> extends AbstractCollection<V> {
                    final List<V> delegate = new ArrayList<>();
                    @Override public Iterator<V> iterator() { return delegate.iterator(); }
                    @Override public int size() { return delegate.size(); }
                    @Override public Spliterator<V> spliterator() { return delegate.spliterator(); }
                }
                class WrappedSet<V> extends Wrapped<V> implements Set<V> { }
                """);
        contains(kotlin, "override fun spliterator(): Spliterator<V> = super<Wrapped>.spliterator()");
        compiles(kotlin);
        // without a method of the superclass's own: Set's default overrides Collection's, which Kotlin sees
        String defaults = kotlin("""
                package a;
                import java.util.*;
                class Plain<V> extends AbstractCollection<V> {
                    final List<V> delegate = new ArrayList<>();
                    @Override public Iterator<V> iterator() { return delegate.iterator(); }
                    @Override public int size() { return delegate.size(); }
                }
                class PlainSet<V> extends Plain<V> implements Set<V> { }
                """);
        compiles(defaults);
        // langchain4j's DefaultInputGuardrailExecutedEvent: a superclass's own method beside an interface's default,
        // which Kotlin accepts as Java does; no override written
        String own = kotlin("""
                package a;
                interface Named { default String name() { return "named"; } }
                interface Event extends Named { }
                class Base implements Named { public String name() { return "base"; } }
                class Sub extends Base implements Event { }
                """);
        assertFalse(own.contains("super<"), own);
        compiles(own);
    }

    /**
     * guava's DenseImmutableTable.Column, an inner AbstractMap: {@code values[keyIndex]} reads the outer table's
     * field in Java; in Kotlin, values is the inherited Map property.
     */
    @Test
    public void outerFieldNamedAsAnInheritedMappedProperty() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Table {
                    private final String[] values = new String[3];
                    class Column extends AbstractMap<Integer, String> {
                        String at(int i) { return values[i]; }
                        @Override public Set<Map.Entry<Integer, String>> entrySet() { return Collections.emptySet(); }
                    }
                }
                """);
        contains(kotlin, "this@Table.values[i]");
        compiles(kotlin);
    }

    /**
     * guava's HashBiMap.entryIterator(): an anonymous BiIterator declares {@code class MapEntry}, an inner class of
     * the anonymous class. Kotlin's object expression takes no nested class: an inner one.
     */
    @Test
    public void classInsideAnAnonymousClass() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Bi<K> {
                    K first;
                    abstract static class It<T> { abstract T output(int i); }
                    It<Object> iterator() {
                        return new It<Object>() {
                            @Override Object output(int i) { return new Entry(i); }
                            class Entry {
                                final int index;
                                Entry(int index) { this.index = index; }
                                K key() { return first; }
                            }
                        };
                    }
                }
                """);
        contains(kotlin, "inner class Entry");
        compiles(kotlin);
    }

    /**
     * guava's DirectedGraphConnections.removeSuccessor(Object node) overrides GraphConnections' removeSuccessor(N node):
     * Java's override by erasure, Kotlin's takes the overridden parameter, N.
     */
    @Test
    public void overrideByErasure() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                interface Connections<N, V> { V removeSuccessor(N node); }
                final class Directed<N, V> implements Connections<N, V> {
                    final Map<N, V> map = new HashMap<>();
                    @Override public V removeSuccessor(Object node) { return map.remove(node); }
                }
                """);
        contains(kotlin, "override fun removeSuccessor(node: N)");
        compiles(kotlin);
        // TreeRangeMap's EMPTY_SUB_RANGE_MAP: putAll(RangeMap<Comparable<?>, ?>) overriding putAll(RangeMap<K, ?
        // extends V>) of a RangeMap<Comparable<?>, Object>
        String wildcard = kotlin("""
                package a;
                interface RangeMap<K, V> { void putAll(RangeMap<K, ? extends V> rangeMap); }
                final class Tree {
                    static final RangeMap<Comparable<?>, Object> EMPTY = new RangeMap<Comparable<?>, Object>() {
                        @Override public void putAll(RangeMap<Comparable<?>, ?> rangeMap) { }
                    };
                }
                """);
        contains(wildcard, "putAll(rangeMap: RangeMap<Comparable<*>, out Any");
        compiles(wildcard);
    }

    /**
     * guava's ToDoubleRounder<X extends Number & Comparable<X>>, and a method's {@code <T extends Number &
     * Comparable<T>>}: Kotlin's where clause, after the supertypes or the result.
     */
    @Test
    public void typeParametersWithTwoBounds() {
        String kotlin = kotlin("""
                package a;
                abstract class Rounder<X extends Number & Comparable<X>> {
                    abstract X toX(double d);
                    int compare(X x, double d) { return x.compareTo(toX(d)); }
                    static <T extends Number & Comparable<T>> T max(T a, T b) { return a.compareTo(b) >= 0 ? a : b; }
                }
                """);
        contains(kotlin, "abstract class Rounder<X> where X : Number, X : Comparable<X>");
        contains(kotlin, "fun <T> max(a: T, b: T): T where T : Number, T : Comparable<T>");
        compiles(kotlin);
    }

    /**
     * guava's AbstractMapBasedMultimap.WrappedList.WrappedListIterator extends WrappedIterator, an inner class of
     * WrappedCollection, the outer WrappedList's superclass, inside the generic AbstractMapBasedMultimap<K, V>: Kotlin
     * wants the outer classes' type arguments written.
     */
    @Test
    public void innerClassExtendingAnInnerOfTheOutersSuperclass() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                abstract class Multimap<K, V> {
                    class WrappedCollection {
                        final List<V> delegate = new ArrayList<>();
                        class WrappedIterator {
                            final Iterator<V> it;
                            WrappedIterator() { it = delegate.iterator(); }
                            WrappedIterator(Iterator<V> it) { this.it = it; }
                            Iterator<V> getDelegateIterator() { return it; }
                        }
                    }
                    class WrappedList extends WrappedCollection {
                        class WrappedListIterator extends WrappedIterator {
                            WrappedListIterator() { }
                            WrappedListIterator(int index) { super(delegate.listIterator(index)); }
                            ListIterator<V> list() { return (ListIterator<V>) getDelegateIterator(); }
                        }
                    }
                }
                """);
        contains(kotlin, "inner class WrappedListIterator : Multimap<K, V>.WrappedCollection.WrappedIterator");
        compiles(kotlin);
    }

    /**
     * guava's ConcurrentHashMultiset.EntrySet extends AbstractMultiset<E>.EntrySet: the superclass's simple name is
     * the class's own, which inside it means itself in Kotlin. Qualified by the superclass's outer, with its type
     * arguments.
     */
    @Test
    public void innerClassExtendingTheSameNamedInnerOfTheOutersSuperclass() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                abstract class Base<E> {
                    class EntrySet { int size() { return 0; } }
                }
                class Concurrent<E> extends Base<E> {
                    private class EntrySet extends Base<E>.EntrySet { @Override int size() { return 1; } }
                    int count() { return new EntrySet().size(); }
                }
                """);
        contains(kotlin, "inner class EntrySet : Base<E>.EntrySet()");
        compiles(kotlin);
    }

    /**
     * guava's Maps.EntrySet.removeAll: {@code Sets.removeAllImpl(this, c.iterator())} of a read-only Collection<*>
     * parameter, whose iterator() is Kotlin's Iterator; removeAllImpl's translated Iterator parameter is a
     * MutableIterator. Java's iterator is one: cast.
     */
    @Test
    public void readOnlyCollectionsIteratorIntoAnIteratorParameter() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                final class Sets {
                    static boolean removeAllImpl(Set<?> set, Iterator<?> iterator) {
                        boolean changed = false;
                        while (iterator.hasNext()) changed |= set.remove(iterator.next());
                        return changed;
                    }
                    static final class EntrySet<E> extends AbstractSet<E> {
                        final Set<E> delegate = new HashSet<>();
                        @Override public Iterator<E> iterator() { return delegate.iterator(); }
                        @Override public int size() { return delegate.size(); }
                        @Override public boolean removeAll(Collection<?> c) { return removeAllImpl(this, c.iterator()); }
                    }
                }
                """);
        contains(kotlin, "c: Collection<");
        contains(kotlin, "(c.iterator() as MutableIterator<*>)");
        compiles(kotlin);
    }

    /**
     * guava's MoreExecutors.ScheduledListeningDecorator: its private delegate shadows ListeningDecorator's in Java; in
     * Kotlin both are internal (private members of nested classes), and the subclass's would hide the other: renamed.
     */
    @Test
    public void fieldShadowingASuperclassField() {
        String kotlin = kotlin("""
                package a;
                final class Executors {
                    private static class Decorator {
                        private final Runnable delegate;
                        Decorator(Runnable delegate) { this.delegate = delegate; }
                        void run() { delegate.run(); }
                    }
                    private static final class Scheduled extends Decorator {
                        private final Runnable delegate;
                        Scheduled(Runnable delegate) { super(delegate); this.delegate = delegate; }
                        void again() { delegate.run(); }
                    }
                }
                """);
        contains(kotlin, "_delegate");
        compiles(kotlin);
    }

    /**
     * guava's ClosingFuture.DeferredCloser: a public class whose private constructor takes a private nested type
     * into a private field, both internal in Kotlin, which the outer class must reach. A public constructor or
     * property may not expose the internal type: the header keeps the visibilities.
     */
    @Test
    public void constructorOfAPrivateNestedType() {
        String kotlin = kotlin("""
                package a;
                public final class Outer {
                    private static final class Inner { }
                    public static final class Closer {
                        private final Inner list;
                        private Closer(Inner list) { this.list = list; }
                        boolean has() { return list != null; }
                    }
                    Closer closer() { return new Closer(new Inner()); }
                }
                """);
        contains(kotlin, "class Closer internal constructor(internal val list: Inner)");
        compiles(kotlin);
        // fernflower's ExceptionTable: a private field with a Java getter is a private property, without the
        // @JvmField a public one takes against the accessor clash
        String getter = kotlin("""
                package a;
                import java.util.*;
                public class Table {
                    private final List<String> handlers;
                    public Table(List<String> handlers) { this.handlers = handlers; }
                    public List<String> getHandlers() { return handlers; }
                }
                """);
        contains(getter, "class Table(private val handlers: ");
        assertFalse(getter.contains("@JvmField"), getter);
        compiles(getter);
    }

    /**
     * guava's Types.JavaVersion: {@code static final JavaVersion CURRENT;}, assigned one of the constants in a static
     * block, is a property of the companion, not an entry.
     */
    @Test
    public void enumFieldOfItsOwnTypeIsNoEntry() {
        String kotlin = kotlin("""
                package a;
                enum Version {
                    OLD { @Override int level() { return 1; } },
                    NEW { @Override int level() { return 2; } };
                    abstract int level();
                    static final Version CURRENT;
                    static final Version[] ALL = new Version[]{OLD, NEW};
                    static {
                        if (System.getProperty("old") != null) CURRENT = OLD; else CURRENT = NEW;
                    }
                }
                """);
        assertFalse(kotlin.contains("CURRENT;"), kotlin);
        assertFalse(kotlin.contains("ALL;"), kotlin);
        compiles(kotlin);
    }

    /**
     * guava's PublicSuffixTrie.ChunksCharSequence implements CharSequence: Kotlin's length is a property and charAt
     * the operator get, which the overrides declare; its field length is renamed, as AbstractMapBasedMultiset's size.
     */
    @Test
    public void charSequenceImplementation() {
        String kotlin = kotlin("""
                package a;
                final class Chunks implements CharSequence {
                    private final String text;
                    private final int length;
                    Chunks(String text) { this.text = text; this.length = text.length(); }
                    @Override public int length() { return length; }
                    @Override public char charAt(int index) { return text.charAt(index); }
                    @Override public CharSequence subSequence(int start, int end) { return text.subSequence(start, end); }
                    @Override public String toString() { return text; }
                    char first() { return charAt(0); }
                }
                """);
        contains(kotlin, "override val length: Int");
        contains(kotlin, "override fun get(index: Int): Char");
        compiles(kotlin);
    }

    /**
     * As {@link #inheritedDefaultsChosenExplicitly}, for methods with parameters: guava's FlatMapSpliteratorOfDouble
     * inherits forEachRemaining(Consumer) from FlatMapSpliterator and as Spliterator's default; SynchronizedMultiset
     * forEach(Consumer) from SynchronizedCollection and as Multiset's default.
     */
    @Test
    public void inheritedDefaultsWithParametersChosenExplicitly() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                import java.util.function.Consumer;
                abstract class Base<T> {
                    public void forEachRemaining(Consumer<? super T> action) { }
                }
                abstract class Split<T> extends Base<T> implements Spliterator<T> { }
                interface Each<E> { void each(Consumer<? super E> a); }
                interface Bag<E> extends Each<E> { @Override default void each(Consumer<? super E> a) { } }
                class Coll<E> implements Each<E> { @Override public void each(Consumer<? super E> a) { } }
                class SyncBag<E> extends Coll<E> implements Bag<E> { }
                """);
        contains(kotlin, "override fun forEachRemaining(action: Consumer<in T>) = super<Base>.forEachRemaining(action)");
        contains(kotlin, "override fun each(a: Consumer<in E>) = super<Coll>.each(a)");
        compiles(kotlin);
        // FlatMapSpliteratorOfPrimitive's forEachRemaining(OutConsumerT) is a forEachRemaining(DoubleConsumer) to
        // FlatMapSpliteratorOfDouble, as Spliterator.OfDouble's default
        String primitive = kotlin("""
                package a;
                import java.util.*;
                import java.util.function.DoubleConsumer;
                abstract class Prim<C> {
                    public void forEachRemaining(C action) { }
                }
                abstract class PrimD extends Prim<DoubleConsumer> implements Spliterator.OfDouble { }
                """);
        contains(primitive, "override fun forEachRemaining(action: DoubleConsumer) = super<Prim>.forEachRemaining(action)");
        compiles(primitive);
    }

    /**
     * guava's ForwardingCollection implements Collection without a Java superclass: Kotlin's MutableCollection has no
     * toArray to override, so the members are declared plainly (Java's toArray on the JVM); toArray(T[]) with a T[]
     * calls Java's, which fills and returns the array it is handed.
     */
    @Test
    public void toArrayInInterfaceOnlyCollection() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                abstract class Fwd<E> implements Collection<E> {
                    abstract Collection<E> delegate();
                    @Override public Iterator<E> iterator() { return delegate().iterator(); }
                    @Override public int size() { return delegate().size(); }
                    @Override public Object[] toArray() { return delegate().toArray(); }
                    @Override public <T> T[] toArray(T[] array) { return delegate().toArray(array); }
                }
                """);
        assertFalse(kotlin.contains("override fun toArray"), kotlin);
        contains(kotlin, "(delegate() as java.util.Collection<*>).toArray(array)");
        compiles(kotlin);
    }

    /**
     * A subclass of AbstractCollection, a Java class: Kotlin sees its toArray() and toArray(T[]) as members, so
     * {@code super.toArray()} stays a call to the superclass's member. Kotlin's {@code super} is no value: it can be
     * neither cast ({@code (super as java.util.Collection<*>)}) nor the receiver of the extension toTypedArray.
     */
    @Test
    public void toArrayOnSuper() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Bag<E> extends AbstractCollection<E> {
                    private final List<E> items = new ArrayList<>();
                    @Override public Iterator<E> iterator() { return items.iterator(); }
                    @Override public int size() { return items.size(); }
                    @Override public Object[] toArray() { return super.toArray(); }
                    @Override public <T> T[] toArray(T[] array) { return super.toArray(array); }
                }
                """);
        contains(kotlin, "super.toArray()");
        contains(kotlin, "super.toArray(array)");
        compiles(kotlin);
    }

    /**
     * guava's Optional.toJavaUtil(), AbstractValueGraph.edgeValue: java.util.Optional.ofNullable(t) into an
     * Optional<T>. Kotlin's Optional bounds its T non-null, so the call is an Optional<T & Any>; the cast is unchecked.
     */
    @Test
    public void optionalOfTypeVariable() {
        String kotlin = kotlin("""
                package a;
                import java.util.Optional;
                class Box<T> {
                    private T value;
                    Optional<T> toJavaUtil() { return Optional.ofNullable(value); }
                }
                """);
        contains(kotlin, "(Optional.ofNullable(value) as Optional<T>)");
        compiles(kotlin);
    }

    /** guava's CaseFormat: constants override a non-abstract method of their enum, which Kotlin wants open. */
    @Test
    public void enumConstantOverridesOpenMethod() {
        String kotlin = kotlin("""
                package a;
                enum Format {
                    LOWER { @Override String convert(String s) { return s.toLowerCase(); } },
                    PLAIN;
                    String convert(String s) { return s; }
                }
                """);
        contains(kotlin, "open fun convert(s: String): String");
        compiles(kotlin);
    }

    /**
     * guava's LocalCache.WriteThroughEntry: fields key and value, set by the only constructor, in a Map.Entry. As a
     * primary constructor's properties they clash with Kotlin's {@code key} and {@code value}; renamed, they are not.
     */
    @Test
    public void entryFieldsNotConstructorProperties() {
        String kotlin = kotlin("""
                package a;
                import java.util.Map;
                class WriteThrough<K, V> implements Map.Entry<K, V> {
                    final K key;
                    V value;
                    WriteThrough(K key, V value) { this.key = key; this.value = value; }
                    @Override public K getKey() { return key; }
                    @Override public V getValue() { return value; }
                    @Override public V setValue(V newValue) { V old = value; value = newValue; return old; }
                }
                """);
        contains(kotlin, "_key");
        compiles(kotlin);
    }

    /**
     * guava's LocalCache serialization proxies, nested classes with {@code private void readObject(ObjectInputStream)}
     * in a class and its subclass: private, as serialization requires; internal, the subclass's "hid" the other's.
     */
    @Test
    public void serializationHooksStayPrivate() {
        String kotlin = kotlin("""
                package a;
                import java.io.*;
                class Outer {
                    static class Proxy implements Serializable {
                        private void readObject(ObjectInputStream in) throws IOException { }
                        private Object readResolve() { return this; }
                    }
                    static class LoadingProxy extends Proxy {
                        private void readObject(ObjectInputStream in) throws IOException { }
                        private Object readResolve() { return this; }
                    }
                }
                """);
        contains(kotlin, "private fun readObject(");
        assertFalse(kotlin.contains("internal fun readObject"), kotlin);
        compiles(kotlin);
    }

    /**
     * guava's MapMakerInternalMap: a package-private Segment takes a private nested Entry in its signatures. Kotlin's
     * internal Entry would be exposed by the public Segment, so the Entry prints public.
     */
    @Test
    public void privateNestedTypeExposedBySignature() {
        String kotlin = kotlin("""
                package a;
                class Map2 {
                    private static class Entry { int hash; }
                    private static class Hidden { }
                    static class Segment {
                        Entry first(Entry e) { return e; }
                        private Hidden hidden() { return new Hidden(); }
                    }
                }
                """);
        assertFalse(kotlin.contains("internal open class Entry"), kotlin);
        contains(kotlin, "internal open class Hidden");
        compiles(kotlin);
    }

    /**
     * guava's Collections2.filter: {@code new Filtered<>(checkNotNull(unfiltered))} into a constructor that keeps a
     * Collection: the pass-through's result is not read only, so neither is the parameter. A discarded
     * {@code checkNotNull(map)} and a receiver {@code checkNotNull(c).size()} are reads.
     */
    @Test
    public void passThroughReadsOnlyWhereItsResultIs() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Coll {
                    static <T> T checkNotNull(T t) { if (t == null) throw new NullPointerException(); return t; }
                    static class Filtered<E> {
                        final Collection<E> unfiltered;
                        Filtered(Collection<E> unfiltered) { this.unfiltered = unfiltered; }
                        void clear() { unfiltered.clear(); }
                    }
                    static <E> Filtered<E> filter(Collection<E> unfiltered) {
                        if (unfiltered instanceof Filtered) return null;
                        return new Filtered<>(checkNotNull(unfiltered));
                    }
                    static int size(Collection<String> c) { checkNotNull(c); return checkNotNull(c).size(); }
                }
                """, type -> type.methods().forEach(m -> m.parameters().forEach(
                p -> p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER,
                        io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE))));
        contains(kotlin, "fun <E> filter(unfiltered: MutableCollection<E>)");
        contains(kotlin, "fun size(c: Collection<String>): Int");
    }

    /**
     * guava's Maps.removeAllKeys removes through {@code map.entrySet().iterator()}, which the modification analysis
     * misses: a view is a read only where it is iterated or read in turn. A for-each over {@code map.values()} and
     * {@code new ArrayList<>(map.keySet())} are (and {@code map.keySet().contains(k)}, with the JDK's facts loaded).
     */
    @Test
    public void viewReadsOnlyWhereReadInTurn() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class Views {
                    static boolean removeAll(Map<String, Integer> map, Collection<?> keys) {
                        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
                        boolean result = false;
                        while (it.hasNext()) { if (keys.contains(it.next().getKey())) { it.remove(); result = true; } }
                        return result;
                    }
                    static int count(Map<String, Integer> map, String k) {
                        int n = k.length();
                        for (Integer v : map.values()) n += v;
                        return n + new ArrayList<>(map.keySet()).size();
                    }
                }
                """, type -> type.methods().forEach(m -> m.parameters().forEach(
                p -> p.analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER,
                        io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE))));
        contains(kotlin, "fun removeAll(map: MutableMap<String, Int>");
        contains(kotlin, "fun count(map: Map<String, Int>, k: String): Int");
        compiles(kotlin);
    }

    /**
     * guava's Maps.FilteredMapValues removes through {@code unfiltered.entrySet().iterator()}: the field is no
     * read-only Map, whatever the analysis says (maddi-mod#54); a guard only here, where without the JDK's facts
     * entrySet() is no read anyway. Java's arrays are covariant, Kotlin's are not:
     * {@code getTypeParameters()} into a {@code TypeVariable<?>[]}, a vararg returned as an {@code Object[]}.
     */
    @Test
    public void fieldViewsAndCovariantArrays() {
        String kotlin = kotlin("""
                package a;
                import java.lang.reflect.TypeVariable;
                import java.util.*;
                class Arr<T> {
                    private final Map<String, Integer> unfiltered;
                    Arr(Map<String, Integer> filtered, Map<String, Integer> unfiltered) {
                    this.filtered = filtered;
                    this.unfiltered = unfiltered;
                }
                private final Map<String, Integer> filtered;
                    boolean removeValue(Integer v) {
                        Iterator<Map.Entry<String, Integer>> it = unfiltered.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry<String, Integer> e = it.next();
                            if (e.getValue().equals(v)) { it.remove(); return true; }
                        }
                        return false;
                    }
                    int size() { return unfiltered.size(); }
                    static TypeVariable<?>[] params(Class<?> c) {
                        TypeVariable<?>[] ps = c.getTypeParameters();
                        return ps;
                    }
                    static Object[] check(Object... array) {
                        for (Object o : array) Objects.requireNonNull(o);
                        return array;
                    }
                    static Object[] copy(Object... array) {
                        Object[] copied = array.clone();
                        return copied;
                    }
                }
                """, type -> type.fields().forEach(f -> f.analysis().set(
                io.codelaser.maddi.cst.impl.analysis.PropertyImpl.STRUCTURALLY_UNMODIFIED_FIELD,
                io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE)));
        contains(kotlin, "MutableMap<String, Int>");
        contains(kotlin, "unfiltered: MutableMap<String, Int>");
        contains(kotlin, "as Array<TypeVariable<*>>");
        compiles(kotlin);
    }

    /** guava's TypeToken.ClassSet: an inner class's serialVersionUID, where Kotlin allows no companion object. */
    @Test
    public void innerClassSerialVersionUid() {
        String kotlin = kotlin("""
                package a;
                import java.io.Serializable;
                class Outer implements Serializable {
                    private static final long serialVersionUID = 1;
                    final int n = 1;
                    class Inner implements Serializable {
                        private static final long serialVersionUID = 0;
                        int get() { return n; }
                    }
                }
                """);
        contains(kotlin, "inner class Inner");
        contains(kotlin, "serialVersionUID: Long = 1");
        assertFalse(kotlin.contains("serialVersionUID: Long = 0"), kotlin);
        compiles(kotlin);
    }

    /**
     * guava's MultimapBuilder: {@code new ListMultimapBuilder<K0, @Nullable Object>() {…}} keeps its supertype's
     * type-use annotation (CodeLaser/maddi#144), so the object's supertype matches the declared return. Until that fix
     * is merged, the front end drops the annotation and the supertype prints without it.
     */
    @Test
    public void anonymousSupertypeKeepsNullableArgument() {
        String kotlin = kotlin("""
                package a;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                import java.util.*;
                class MB<K0> {
                    @Target(ElementType.TYPE_USE) @interface Nullable { }
                    abstract static class LB<K0, V0 extends @Nullable Object> {
                        abstract <K extends K0, V extends V0> List<V> build();
                    }
                    LB<K0, @Nullable Object> arrayListValues() {
                        return new LB<K0, @Nullable Object>() {
                            @Override
                            <K extends K0, V extends @Nullable Object> List<V> build() { return new ArrayList<>(); }
                        };
                    }
                }
                """);
        // TODO "object : LB<K0, Any?>()" once maddi#144's front-end fix is merged (checked against it, cb6774ece); the
        // declared return's Any? is the NullabilityPass's, not run here, so no kotlinc check
        contains(kotlin, "object : LB<K0, Any");
    }

    /**
     * guava's UnsignedInteger, UnsignedLong, AtomicDouble extend Number: Kotlin's Number declares toInt() … toDouble()
     * where Java's has intValue() …, and toByte(), toShort() abstract, which Java's implements.
     */
    @Test
    public void numberSubclass() {
        String kotlin = kotlin("""
                package a;
                final class Unsigned extends Number implements Comparable<Unsigned> {
                    private final int value;
                    Unsigned(int value) { this.value = value; }
                    @Override public int intValue() { return value; }
                    @Override public long longValue() { return value & 0xffffffffL; }
                    @Override public float floatValue() { return (float) longValue(); }
                    @Override public double doubleValue() { return (double) longValue(); }
                    @Override public int compareTo(Unsigned other) { return Integer.compareUnsigned(value, other.value); }
                    static long sum(Unsigned a, Number b) { return a.longValue() + b.longValue(); }
                }
                """);
        contains(kotlin, "override fun toInt(): Int");
        contains(kotlin, "override fun toDouble(): Double");
        contains(kotlin, "override fun toByte(): Byte = toInt().toByte()");
        compiles(kotlin);
    }

    /**
     * guava's AbstractMapBasedMultimap.AsMapEntries extends Maps.EntrySet<K, Collection<V>>, whose own K and V share
     * the outer class's names: contains(o) overrides with the subclass's element, Entry<K, Collection<V>>.
     */
    @Test
    public void elementOfAGenericSuperclassWithNamesakeParameters() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                abstract class Multi<K, V> {
                    abstract static class EntrySet<K, V> extends AbstractSet<Map.Entry<K, V>> {
                        abstract Map<K, V> map();
                        @Override public int size() { return map().size(); }
                        @Override public boolean contains(Object o) { return map().entrySet().contains(o); }
                    }
                    final Map<K, Collection<V>> submap = new HashMap<>();
                    class AsMapEntries extends EntrySet<K, Collection<V>> {
                        @Override Map<K, Collection<V>> map() { return submap; }
                        @Override public Iterator<Map.Entry<K, Collection<V>>> iterator() { return submap.entrySet().iterator(); }
                        @Override public boolean contains(Object o) { return submap.entrySet().contains(o); }
                    }
                }
                """);
        contains(kotlin, "override fun contains(o: MutableMap.MutableEntry<K, MutableCollection<V>>): Boolean");
        compiles(kotlin);
    }

    /**
     * Optional.empty() and Optional.of(value) into an Optional<T>, beside ofNullable above: the cast to Optional<T>
     * gives Kotlin nothing to infer the call's own T from.
     */
    @Test
    public void optionalEmptyAndOfIntoTypeVariable() {
        String kotlin = kotlin("""
                package a;
                import java.util.Optional;
                class Opt<T> {
                    private final T value;
                    Opt(T value) { this.value = value; }
                    Optional<T> none() { return Optional.empty(); }
                    Optional<T> some() { return Optional.of(value); }
                }
                """);
        // empty's T from Optional<T>; of's value asserted, as Kotlin's of takes a T & Any (Java's of throws on null too)
        contains(kotlin, "(Optional.empty<T>() as Optional<T>)");
        contains(kotlin, "(Optional.of(value!!) as Optional<T>)");
        compiles(kotlin);
    }

    /**
     * jenkins's Notifier: {@code BuildStepDescriptor getDescriptor()} overriding Publisher's {@code
     * Descriptor<Publisher> getDescriptor()}, a raw result Java accepts unchecked. Kotlin's {@code
     * BuildStepDescriptor<*>} is no subtype of {@code Descriptor<Publisher>}: the raw class takes the overridden
     * result's arguments, {@code BuildStepDescriptor<Publisher>}.
     */
    @Test
    public void rawResultOfAnOverride() {
        String kotlin = kotlin("""
                package a;
                class Descriptor<T> { }
                class BuildStepDescriptor<T> extends Descriptor<T> { }
                abstract class Publisher {
                    Descriptor<Publisher> getDescriptor() { return new Descriptor<>(); }
                }
                abstract class Notifier extends Publisher {
                    @Override BuildStepDescriptor getDescriptor() { return (BuildStepDescriptor) super.getDescriptor(); }
                }
                abstract class Annotator<T> {
                    abstract Annotator<T> annotate(T context);
                }
                class Url extends Annotator<Object> {
                    @Override Annotator annotate(Object context) { return this; }
                }
                """);
        contains(kotlin, "fun getDescriptor(): BuildStepDescriptor<Publisher>");
        contains(kotlin, "fun annotate(context: Any): Annotator<Any>");
        compiles(kotlin);
    }

    /**
     * jenkins's Secret converter: a raw {@code Object convert(Class type, Object value)} with a block body,
     * overriding beanutils's {@code <T> T convert(Class<T> type, Object value)}. Kotlin's override takes the member's
     * type parameter and parameter types, and casts what it returns.
     */
    @Test
    public void rawBlockOverrideOfAGenericMember() {
        String kotlin = kotlin("""
                package a;
                interface Converter {
                    <T> T convert(Class<T> type, Object value);
                }
                class Secret {
                    static Secret fromString(String s) { return new Secret(); }
                    static final Converter CONVERTER = new Converter() {
                        @Override public Object convert(Class type, Object value) {
                            if (value instanceof Secret) return value;
                            return fromString(value.toString());
                        }
                    };
                }
                """);
        contains(kotlin, "override fun <T> convert(type: Class<T>");
        contains(kotlin, "(fromString(value.toString())) as T");
        compiles(kotlin);
    }

    /**
     * jenkins's CopyOnWriteList.ConverterImpl: {@code new CopyOnWriteList(items, true)} of a raw {@code List items},
     * in a static nested class. The raw list into the constructor's {@code List<E>} is cast to Java's erasure; the
     * outer class's E is not in scope there.
     */
    @Test
    public void rawArgumentIntoOuterTypeParameterFromNestedClass() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                class CopyOnWriteList<E> {
                    private final List<E> core;
                    private CopyOnWriteList(List<E> core, boolean noCopy) { this.core = noCopy ? core : new ArrayList<>(core); }
                    int size() { return core.size(); }
                    static final class ConverterImpl {
                        Object unmarshal(String s) {
                            List items = new ArrayList();
                            items.add(s);
                            return new CopyOnWriteList(items, true);
                        }
                    }
                }
                """);
        assertFalse(kotlin.contains("MutableList<E>)"), kotlin);
        compiles(kotlin);
    }

    /**
     * jenkins's PluginManager: {@code Jenkins jenkins = Jenkins.get()}, then {@code Jenkins.getAuthentication2()},
     * in a file whose Javadoc links {@code jenkins.model.Jenkins}, which made Java's qualification spell the type in
     * full everywhere. Written {@code jenkins.model.Jenkins}, Kotlin resolves {@code jenkins} to the local variable;
     * the file imports the type, and its simple name is enough.
     */
    @Test
    public void typeNamedAfterAPackageBesideALocalOfThatName() {
        String kotlin = kotlin("""
                package hudson;
                import jenkins.model.Jenkins;
                /** Lives longer than {@link jenkins.model.Jenkins}. */
                class PluginManager {
                    String install() {
                        Jenkins jenkins = Jenkins.get();
                        String center = jenkins.getUpdateCenter();
                        return center + Jenkins.getAuthentication2();
                    }
                }
                """, java.util.Map.of("jenkins.model.Jenkins", """
                package jenkins.model;
                public class Jenkins {
                    public static Jenkins get() { return new Jenkins(); }
                    public static String getAuthentication2() { return "a"; }
                    public String getUpdateCenter() { return "u"; }
                }
                """));
        assertFalse(kotlin.contains("jenkins.model.Jenkins."), kotlin);
    }

    /**
     * jenkins's PluginManager: {@code HttpResponse<Void> httpResponse = httpClient.send(request, discarding())}, its
     * Void argument nullable (as the verdicts make it there), so that the local's type differs from the call's and is
     * written. The translation's Void is Unit; the library's result is an {@code HttpResponse<Void!>}, no
     * {@code HttpResponse<Unit>}: Kotlin infers the local's type.
     */
    @Test
    public void voidArgumentOfALibraryResult() {
        String kotlin = kotlin("""
                package a;
                import java.net.URI;
                import java.net.http.HttpClient;
                import java.net.http.HttpRequest;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                @Target(ElementType.TYPE_USE) @interface Nullable { }
                class Download {
                    interface HttpResponse { }
                    int status(HttpClient httpClient) throws Exception {
                        HttpRequest request = HttpRequest.newBuilder(URI.create("http://x")).build();
                        try {
                            java.net.http.HttpResponse<@Nullable Void> httpResponse = httpClient.send(
                                    request, java.net.http.HttpResponse.BodyHandlers.discarding());
                            return httpResponse.statusCode();
                        } catch (InterruptedException e) {
                            return -1;
                        }
                    }
                }
                """);
        assertFalse(kotlin.contains("HttpResponse<Unit>"), kotlin);
        compiles(kotlin);
    }

    /**
     * jenkins's DirScanner.Glob: {@code this(includes, excludes, true, new OpenOption[0])} into a vararg of non-null
     * OpenOption. {@code arrayOfNulls<OpenOption>(0)} is an {@code Array<OpenOption?>}; the empty array spread is
     * {@code *emptyArray<OpenOption>()}. Not left out: the call would pick the overload without the varargs.
     */
    @Test
    public void emptyArrayIntoVarargs() {
        String kotlin = kotlin("""
                package a;
                import java.nio.file.OpenOption;
                class Glob {
                    private final String includes;
                    private final OpenOption[] openOptions;
                    Glob(String includes, boolean useDefaultExcludes) {
                        this(includes, useDefaultExcludes, new OpenOption[0]);
                    }
                    Glob(String includes, boolean useDefaultExcludes, OpenOption... openOptions) {
                        this.includes = includes;
                        this.openOptions = openOptions;
                    }
                    int count() { return openOptions.length + includes.length(); }
                }
                """);
        contains(kotlin, "*emptyArray<OpenOption>()");
        compiles(kotlin);
    }

    /**
     * langchain4j's DefaultContentAggregatorTest: a bare {@code @MethodSource} names its factory by the test's own name,
     * a private static. JUnit looks for it among the test class's statics: {@code @JvmStatic}, private as in Java.
     * The api package too, as its own source set: the params classes refer to ExtensionContext, TestTemplate and
     * ExtendWith, and under the test runner the params package resolves to junit-platform-console-standalone (no
     * junit-jupiter-params on that class path), while javac loads those from junit-jupiter-api, which a class-path
     * source set must then be named after (maddi test gate, 2026-10-23).
     */
    @Test
    public void privateMethodSourceFactoryIsJvmStatic() throws java.io.IOException {
        String kotlin = kotlinWithLibrary("""
                package a;
                import java.util.stream.Stream;
                import org.junit.jupiter.params.ParameterizedTest;
                import org.junit.jupiter.params.provider.MethodSource;
                class AggregatorTest {
                    @ParameterizedTest
                    @MethodSource
                    void empty(String query) { }
                    private static Stream<String> empty() { return Stream.of("a"); }
                    @ParameterizedTest
                    @MethodSource("named")
                    void other(String query) { }
                    private static Stream<String> named() { return Stream.of("b"); }
                    private static int helper() { return 1; }
                }
                """, KotlinPrintOptions.DEFAULT, "org/junit/jupiter/params", "org/junit/jupiter/api");
        contains(kotlin, "@JvmStatic private fun empty(): Stream<String>");
        contains(kotlin, "@JvmStatic private fun named(): Stream<String>");
        contains(kotlin, "private fun helper(): Int");
        assertFalse(kotlin.contains("@JvmStatic private fun helper"), kotlin);
    }
}
