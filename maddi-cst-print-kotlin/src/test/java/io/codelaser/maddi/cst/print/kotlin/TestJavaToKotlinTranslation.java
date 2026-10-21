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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Java forms that have no one-to-one Kotlin counterpart, one test per translation rule. The corpus-scale check,
 * which also compiles the output, is maddi-run-openjdk's TestJavaToKotlinFernflower.
 */
public class TestJavaToKotlinTranslation extends CommonJavaToKotlin {

    /** Line by line, indentation ignored: the depth of a fragment is not what these tests are about. */
    private static void contains(String kotlin, String expected) {
        assertTrue(strip(kotlin).contains(strip(expected)), () -> "expected\n" + expected + "\nin\n" + kotlin);
    }

    private static String strip(String s) {
        return s.lines().map(String::strip).collect(java.util.stream.Collectors.joining("\n"));
    }

    @Language("java")
    private static final String SUPER_CALL = """
            package a;
            class B { public String toString() { return "b"; } }
            class C extends B {
                @Override public String toString() { return super.toString() + "c"; }
            }
            """;

    /** #103: {@code super.m()} lost its {@code super.} and became a call to the overriding method itself. */
    @Test
    public void superCall() {
        contains(kotlin(SUPER_CALL), "override fun toString(): String = super.toString() + \"c\"");
    }

    @Language("java")
    private static final String TRY_WITH_RESOURCES = """
            package a;
            import java.io.*;
            class C {
                int one(File f) throws IOException {
                    try (InputStream in = new FileInputStream(f); Reader r = new InputStreamReader(in)) {
                        return r.read();
                    }
                }
                void caught(File f) {
                    try (InputStream s = new FileInputStream(f)) {
                        s.read();
                    } catch (IllegalStateException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            """;

    /** #104: the resources were dropped, so nothing declared or closed them. Multi-catch: one clause per type. */
    @Test
    public void tryWithResources() {
        String kotlin = kotlin(TRY_WITH_RESOURCES);
        contains(kotlin, "FileInputStream(f).use { `in` -> InputStreamReader(`in`).use { r -> return r.read() } }");
        contains(kotlin, "try { FileInputStream(f).use { s -> s.read() } } catch (e: IllegalStateException) {");
        contains(kotlin, "catch (e: IOException) {");
    }

    @Language("java")
    private static final String CASTS = """
            package a;
            class C {
                int m(long l, double d, char ch, Object o) {
                    int i = (int) l;
                    char next = (char) (ch + 1);
                    int code = (int) ch;
                    byte b = (byte) d;
                    String s = (String) o;
                    return i + code + b + next + s.length();
                }
            }
            """;

    /** #105: {@code x as Int} on a Long throws where Java's {@code (int) x} truncates; primitive casts convert. */
    @Test
    public void primitiveCasts() {
        String kotlin = kotlin(CASTS);
        contains(kotlin, "val i = l.toInt()");
        contains(kotlin, "val next = (ch.code + 1).toChar()"); // Java's ch + 1 is an int
        contains(kotlin, "val code = ch.code");
        contains(kotlin, "val b = d.toInt().toByte()");
        contains(kotlin, "val s = o as String");
    }

    @Language("java")
    private static final String BYTE_AND_CHAR = """
            package a;
            class C {
                boolean newline(byte[] buf, int i, short s) {
                    byte ch = buf[i];
                    return ch == '\\r' || '\\n' == ch || s != 'x';
                }
            }
            """;

    @Language("java")
    private static final String RAW_INNER = """
            package a;
            import java.util.ArrayList;
            import java.util.List;
            class Run<J, R extends Run<J, R>> {
                class Execution {
                    void run() { }
                }
            }
            class Runner {
                private final List<Run.Execution> stack = new ArrayList<>();
                void push(Run.Execution e) { stack.add(e); e.run(); }
            }
            """;

    /** jenkins's RunnerStack: a raw inner class of a generic class, written qualified, takes the outer's arguments. */
    @Test
    public void rawInnerClassOfAGenericClass() {
        String kotlin = kotlin(RAW_INNER);
        contains(kotlin, "fun push(e: Run<*, *>.Execution)");
        compiles(kotlin);
    }

    @Language("java")
    private static final String SUPER_OF_A_NESTED_INTERFACE = """
            package a;
            class MixIn {
                interface Job {
                    default String text() { return "build"; }
                }
            }
            class Project implements MixIn.Job {
                @Override
                public String text() { return MixIn.Job.super.text() + "!"; }
            }
            """;

    /** jenkins's AbstractProject: {@code super<…>} names a nested interface as Kotlin resolves it, qualified. */
    @Test
    public void superOfANestedInterface() {
        String kotlin = kotlin(SUPER_OF_A_NESTED_INTERFACE);
        contains(kotlin, "super<MixIn.Job>.text()");
        compiles(kotlin);
    }

    @Language("java")
    private static final String VARARG_COMPONENTS = """
            package a;
            import java.nio.file.OpenOption;
            class FilePath {
                private record Archive(String root, OpenOption... openOptions) {
                    int count() { return openOptions.length; }
                }
                private static class Read {
                    private final String root;
                    private final OpenOption[] openOptions;
                    Read(String root, OpenOption... openOptions) {
                        this.root = root;
                        this.openOptions = openOptions;
                    }
                    int count() { return openOptions.length; }
                }
                int archive(String root, OpenOption... openOptions) {
                    return new Archive(root, openOptions).count() + new Read(root, openOptions).count()
                           + new Archive(root).count() + new Read(root).count()
                           + new Archive(root, java.nio.file.LinkOption.NOFOLLOW_LINKS).count();
                }
            }
            """;

    /** jenkins's FilePath: a varargs record component, and a varargs constructor parameter that is a field. */
    @Test
    public void varargComponents() {
        String kotlin = kotlin(VARARG_COMPONENTS);
        contains(kotlin, "Archive(root, arrayOf(*openOptions))");
        contains(kotlin, "Archive(root, emptyArray<OpenOption>())");
        compiles(kotlin);
    }

    @Language("java")
    private static final String MULTI_CATCH_TESTED = """
            package a;
            import java.io.IOException;
            class Build {
                static void display(IOException e) { }
                void tearDown(Runnable r) throws IOException {
                    try {
                        if (r == null) throw new IOException("none");
                        r.run();
                    } catch (IOException | RuntimeException e) {
                        if (e instanceof IOException) display((IOException) e);
                        System.out.println(e.getMessage());
                    }
                }
            }
            """;

    /** jenkins's AbstractBuild: a multi-catch whose block tests its variable's type catches their superclass. */
    @Test
    public void multiCatchTestingItsVariable() {
        String kotlin = kotlin(MULTI_CATCH_TESTED);
        contains(kotlin, "catch (e: Exception)");
        contains(kotlin, "if (e !is IOException && e !is RuntimeException) throw e");
        compiles(kotlin);
    }

    @Language("java")
    private static final String ENDLESS_FOR = """
            package a;
            import java.util.Set;
            class Workspaces {
                String allocate(String base, Set<String> inUse) {
                    for (int i = 1; ; i++) {
                        String candidate = i == 1 ? base : base + "@" + i;
                        if (inUse.contains(candidate)) continue;
                        return candidate;
                    }
                }
            }
            """;

    /** jenkins's WorkspaceList: a for without a condition does not end, so nothing need follow it. */
    @Test
    public void forWithoutACondition() {
        String kotlin = kotlin(ENDLESS_FOR);
        assertFalse(kotlin.contains("if (!(true)) break"), kotlin);
        compiles(kotlin);
    }

    @Language("java")
    private static final String RAW_LAMBDA_TARGET = """
            package a;
            interface Resolver<V> {
                @SuppressWarnings("rawtypes")
                Resolver NONE = name -> null;
                V resolve(String name);
            }
            """;

    /** jenkins's VariableResolver.NONE: a lambda into a raw type constructs its erasure, not a star projection. */
    @Test
    public void lambdaIntoARawType() {
        String kotlin = kotlin(RAW_LAMBDA_TARGET);
        contains(kotlin, "Resolver<Any?> {");
        compiles(kotlin);
    }

    @Language("java")
    private static final String RAW_CLASS_INTO_SUPER = """
            package a;
            abstract class Descriptor<T> {
                private final Class<T> clazz;
                protected Descriptor(Class<T> clazz) { this.clazz = clazz; }
                @SuppressWarnings("rawtypes")
                protected static Class self() { return Descriptor.class; }
                Class<T> clazz() { return clazz; }
            }
            class Configuration {
            }
            class ConfigurationDescriptor extends Descriptor<Configuration> {
                @SuppressWarnings("unchecked")
                ConfigurationDescriptor() { super(self()); }
            }
            """;

    /** jenkins's GlobalConfiguration: a raw Class into the supertype's Class<T>, a T no name here gives. */
    @Test
    public void rawClassIntoASupertypeParameter() {
        String kotlin = kotlin(RAW_CLASS_INTO_SUPER);
        contains(kotlin, "Descriptor<Configuration>(uncheckedClass(Descriptor.self()))");
        contains(kotlin, "private fun <T> uncheckedClass(value: Class<*>): Class<T>");
        compiles(kotlin);
    }

    @Language("java")
    private static final String RAW_CLASS_CAST = """
            package a;
            import java.util.ArrayList;
            import java.util.List;
            interface Describable<T extends Describable<T>> { }
            class DescriptorList<T extends Describable<T>> extends ArrayList<String> {
                private final Class<T> type;
                DescriptorList(Class<T> type) { this.type = type; }
            }
            abstract class Property<J> implements Describable<Property<?>> { }
            class Jobs {
                @SuppressWarnings({"unchecked", "rawtypes"})
                static final List<String> PROPERTIES = new DescriptorList<Property<?>>((Class) Property.class);
            }
            """;

    /** jenkins's Jobs: an explicit raw (Class) cast into a class's Class<T> whose bound is recursive. */
    @Test
    public void explicitRawClassCast() {
        String kotlin = kotlin(RAW_CLASS_CAST);
        contains(kotlin, "uncheckedClass(Property::class.java)");
        compiles(kotlin);
    }

    @Language("java")
    private static final String UNRELATED_LOOKUP = """
            package a;
            import java.util.Collection;
            interface Item { }
            interface Task { }
            class View {
                boolean contains(Collection<Item> items, Task task) {
                    return items.contains(task);
                }
            }
            """;

    /** jenkins's View: a lookup of a class unrelated to the element's, which a Task that is an Item would find. */
    @Test
    public void lookupOfAnUnrelatedType() {
        String kotlin = kotlin(UNRELATED_LOOKUP);
        contains(kotlin, "items.contains((task as Any?))");
        compiles(kotlin);
    }

    @Language("java")
    private static final String OBJECT_BOUND_FACTORIES = """
            package a;
            import java.util.*;
            import java.util.concurrent.ConcurrentHashMap;
            class Morph {
                private final Map<String, Object> cache = new ConcurrentHashMap<>();
                Collection<?> exclusions(Object exclusion) {
                    if (exclusion == null) return Collections.emptySet();
                    return (Collection<?>) exclusion;
                }
                int size(Object attribute) {
                    Map<String, ?> meta;
                    if (attribute instanceof Map) meta = (Map<String, ?>) attribute;
                    else meta = Collections.emptyMap();
                    return meta.size();
                }
                int none() {
                    List<Object> keys = Collections.emptyList();
                    return keys.size();
                }
                Object load(String name) {
                    return cache.computeIfAbsent(name, key -> {
                        try {
                            return Class.forName(key);
                        } catch (ClassNotFoundException e) {
                            return Optional.empty();
                        }
                    });
                }
            }
            """;

    /** jenkins's MorphTagLibrary, CachingClassLoader: a factory's variable Java binds to Object, Kotlin cannot. */
    @Test
    public void objectBoundFactories() {
        String kotlin = kotlin(OBJECT_BOUND_FACTORIES);
        contains(kotlin, "Collections.emptySet<");
        contains(kotlin, "Collections.emptyMap<String, ");
        contains(kotlin, "Optional.empty<");
        // guava's Maps: a target that names the argument fixes it, its state included
        contains(kotlin, "= Collections.emptyList()");
        compiles(kotlin);
    }

    @Language("java")
    private static final String COMPARING_RECEIVER = """
            package a;
            import java.util.Comparator;
            class Project {
                String getName() { return "p"; }
            }
            class Group {
                Project upstream() { return new Project(); }
                Project downstream() { return new Project(); }
                static final Comparator<Group> NAME_COMPARATOR = Comparator
                        .comparing((Group g) -> g.upstream().getName())
                        .thenComparing(g -> g.downstream().getName());
            }
            """;

    /** jenkins's DependencyGraph: Comparator.comparing(lambda) as a receiver types the lambda's parameter. */
    @Test
    public void comparingAsAReceiver() {
        String kotlin = kotlin(COMPARING_RECEIVER);
        contains(kotlin, "g: Group ->");
        compiles(kotlin);
    }

    /** jenkins's ConsoleCommand: Java compares a byte and a char as ints; Kotlin's Byte and Char have no common type. */
    @Test
    public void byteComparedWithAChar() {
        String kotlin = kotlin(BYTE_AND_CHAR);
        contains(kotlin, "ch.toInt() == '\\r'.code || '\\n'.code == ch.toInt() || s.toInt() != 'x'.code");
        compiles(kotlin);
    }

    @Language("java")
    private static final String FOR_LOOPS = """
            package a;
            class C {
                int m(int[] a, int n) {
                    int s = 0;
                    for (int i = 0; i < a.length; i++) s += a[i];
                    for (int i = n; i >= 0; i -= 2) s += i;
                    for (int i = 0; i < n; i++) { n--; }
                    for (int i = 0, j = 10; i < j; i++, j--) { if (i == 3) continue; s += i * j; }
                    return s;
                }
            }
            """;

    /**
     * guava's Monitor.enterWhen: {@code locked: { … break locked; … }} after a local; AbstractFutureState.get:
     * {@code long_wait_loop: if (…) { do { … break long_wait_loop; } while (…); }}, a label on an if. Kotlin's break
     * leaves loops only: each is a {@code run label@{ }} left with {@code return@label}.
     */
    @Test
    public void labelledStatementsThatAreNoLoops() {
        String kotlin = kotlin("""
                package a;
                class C {
                    boolean fair;
                    boolean tryLock() { return true; }
                    boolean enter(long n) {
                        long start = 0L;
                        locked:
                        {
                            if (!fair) {
                                if (tryLock()) break locked;
                            }
                            start = n;
                            if (!tryLock()) return false;
                        }
                        return start > 0;
                    }
                    int waitFor(int n) {
                        int k = 0;
                        long_wait_loop:
                        if (n > 0) {
                            do {
                                k++;
                                if (k > n) break long_wait_loop;
                            } while (k < 100);
                        }
                        return k;
                    }
                }
                """);
        contains(kotlin, "return@locked");
        contains(kotlin, "return@long_wait_loop");
        compiles(kotlin);
    }

    /**
     * guava's Joiner.useForNull: {@code return new Joiner(this) { … };}. In an object expression's supertype call,
     * Kotlin's this is not the enclosing instance: this@Joiner.
     */
    @Test
    public void thisInAnAnonymousClassesSuperArguments() {
        String kotlin = kotlin("""
                package a;
                class Joiner {
                    final String separator;
                    Joiner(String separator) { this.separator = separator; }
                    Joiner(Joiner prototype) { this.separator = prototype.separator; }
                    String part(Object o) { return String.valueOf(o); }
                    Joiner useForNull(String nullText) {
                        return new Joiner(this) {
                            @Override String part(Object o) { return o == null ? nullText : Joiner.this.part(o); }
                        };
                    }
                }
                """);
        contains(kotlin, "object : Joiner(this@Joiner)");
        compiles(kotlin);
    }

    /**
     * guava's ImmutableSortedSet.spliterator(): new Spliterators.AbstractSpliterator<E>(size(), …) { … }, an int into
     * the anonymous class's superclass constructor's long: Kotlin widens nothing by itself.
     */
    @Test
    public void anonymousSuperConstructorArgumentWidened() {
        String kotlin = kotlin("""
                package a;
                import java.util.*;
                import java.util.function.Consumer;
                class C<E> {
                    int size() { return 0; }
                    Spliterator<E> spliterator() {
                        return new Spliterators.AbstractSpliterator<E>(size(), Spliterator.SIZED) {
                            @Override public boolean tryAdvance(Consumer<? super E> action) { return false; }
                        };
                    }
                }
                """);
        contains(kotlin, "size().toLong()");
        compiles(kotlin);
    }

    /**
     * guava's BigIntegerMath.factorial: a long counter between two ints is a LongRange in Kotlin only from a Long; an
     * int counter against a long bound stays a while loop, where a range would make it a Long. CharMatcher.setBits:
     * an int counter between chars, which a range would make a Char.
     */
    @Test
    public void longCounterBetweenInts() {
        String kotlin = kotlin("""
                package a;
                class C {
                    static long m(int from, int n, long big) {
                        long s = 0;
                        for (long num = from; num <= n; num++) { s += num & 1L; }
                        for (long k = 0; k < n; k++) { s += k; }
                        for (int i = 0; i < big; i++) { s += i; }
                        java.util.BitSet bits = new java.util.BitSet();
                        for (int c = Character.MAX_VALUE; c >= Character.MIN_VALUE; c--) { bits.set(c); }
                        return s;
                    }
                }
                """);
        contains(kotlin, "for (num in from.toLong() .. n) {");
        contains(kotlin, "for (k in 0L until n) {");
        contains(kotlin, "while (i < big) {");
        contains(kotlin, "bits.set(c)");
        assertFalse(kotlin.contains("c.toChar()") && kotlin.contains("set(c.toChar())"), kotlin);
        compiles(kotlin);
    }

    /**
     * A range only when it is the same loop; a bound the body changes ({@code n--}) is not, because a range reads it
     * once. A {@code continue} must still run the updates.
     */
    @Test
    public void forLoops() {
        String kotlin = kotlin(FOR_LOOPS);
        contains(kotlin, "for (i in 0 until a.size) {");
        contains(kotlin, "for (i in n downTo 0 step 2) {");
        contains(kotlin, """
                        when { else -> {
                            var i = 0
                            while (i < n) {
                                n--
                                i++
                            }
                        } }
                """);
        contains(kotlin, """
                            var first1 = true
                            while (true) {
                                if (first1) first1 = false else {
                                    i++
                                    j--
                                }
                                if (!(i < j)) break
                                if (i == 3) { continue }
                """);
    }

    @Language("java")
    private static final String OLD_SWITCH = """
            package a;
            enum Color { RED, GREEN }
            class C {
                String m(int k, Color c) {
                    String r = "";
                    switch (k) {
                        case 1: r = "one";
                        case 2: r += "two"; break;
                        case 3: if (r.isEmpty()) break; r = "three"; break;
                        default: r = "d";
                    }
                    switch (c) { case RED: return "r"; case GREEN: return "g"; }
                    return r;
                }
            }
            """;

    /**
     * Fall-through copies the next case's statements; a case's final {@code break} goes; a {@code break} in the
     * middle leaves a {@code run label@{ }}, since a Kotlin {@code break} in a {@code when} leaves the enclosing loop.
     */
    @Test
    public void oldStyleSwitch() {
        String kotlin = kotlin(OLD_SWITCH);
        contains(kotlin, """
                        1 -> {
                            r = "one"
                            r += "two"
                        }
                """);
        contains(kotlin, "2 -> { r += \"two\" }");
        contains(kotlin, "run switch1@ { when (k) {");
        contains(kotlin, "if (r.isEmpty()) { return@switch1 }");
        contains(kotlin, "Color.RED -> { return \"r\" }");
        assertFalse(kotlin.contains("break"), kotlin);
    }

    @Language("java")
    private static final String LAMBDAS = """
            package a;
            import java.util.List;
            class C {
                void m(List<String> list) {
                    Runnable r = () -> { if (list.isEmpty()) return; list.clear(); };
                    java.util.function.Function<String, Integer> f = s -> { int n = s.length(); return n * 2; };
                }
            }
            """;

    /** A bare {@code return} in a lambda returns from the enclosing function in Kotlin; the last one is a value. */
    @Test
    public void lambdaReturns() {
        String kotlin = kotlin(LAMBDAS);
        contains(kotlin, "lambda@ {");
        contains(kotlin, "return@lambda");
        contains(kotlin, """
                        val n = s.length
                        n * 2
                """);
    }

    @Language("java")
    private static final String CONSTRUCTORS = """
            package a;
            class B { B(int x) { } int h() { return 1; } }
            class C extends B {
                private int fun = 3;
                private String name;
                C() { this(1); }
                C(int in) { super(in); fun = in; }
            }
            """;

    /** Delegation goes into the header; hard keywords are escaped; Java's field defaults are written out. */
    @Test
    public void constructorsAndNames() {
        String kotlin = kotlin(CONSTRUCTORS);
        contains(kotlin, "open class C : B {");
        contains(kotlin, "private var `fun`: Int = 3");
        contains(kotlin, "private lateinit var name: String");
        contains(kotlin, "constructor() : this(1)");
        contains(kotlin, "constructor(`in`: Int) : super(`in`) { `fun` = `in` }");
        contains(kotlin, "open fun h(): Int = 1");
    }

    @Language("java")
    private static final String LONG_EXPRESSION = """
            package a;
            class C {
                static final int ACC_PUBLIC = 1, ACC_PROTECTED = 2, ACC_PRIVATE = 4, ACC_ABSTRACT = 8, ACC_STATIC = 16;
                static final int ACC_FINAL = 32, ACC_STRICT = 64;
                private static final int CLASS_ALLOWED = ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE | ACC_ABSTRACT
                    | ACC_STATIC | ACC_FINAL | ACC_STRICT | ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE | ACC_ABSTRACT;
                String describe(String simpleName, String type, String source, int accessFlags, String more) {
                    return simpleName + " " + accessFlags + " " + type + " " + source + " " + more + " " + simpleName
                        + " " + type + " " + source;
                }
            }
            """;

    /** Kotlin ends a statement at a newline in front of an operator, so a long expression breaks after one. */
    @Test
    public void lineBreaksAfterOperators() {
        String kotlin = kotlin(LONG_EXPRESSION);
        assertTrue(kotlin.lines().map(String::trim).noneMatch(l -> l.startsWith("or ") || l.startsWith("+ ")),
                kotlin);
        assertTrue(kotlin.lines().anyMatch(l -> l.endsWith(" or") || l.endsWith(" +")), kotlin);
    }

    @Language("java")
    private static final String ARRAY_INITIALIZERS = """
            package a;
            class C {
                static final int[][][] TABLE = { {null, {1, 2}}, null };
                String[][] names = { {"a"}, {} };
            }
            """;

    /** An array initializer takes its type from what it initializes, not from its (possibly null) elements. */
    @Test
    public void arrayInitializers() {
        String kotlin = kotlin(ARRAY_INITIALIZERS);
        contains(kotlin, "@JvmField val TABLE: Array<Array<IntArray>> = arrayOf<Array<IntArray>>(");
        contains(kotlin, "arrayOf<IntArray>(null, intArrayOf(1, 2)),");
        contains(kotlin, "var names: Array<Array<String>> = arrayOf<Array<String>>(arrayOf<String>(\"a\"), arrayOf<String>())");
    }

    @Language("java")
    private static final String YIELD_IN_IF = """
            package a;
            class C {
                String m(int k, Object o) {
                    return switch (k) {
                        case 1 -> {
                            if (o instanceof String str) {
                                yield str;
                            } else if (o instanceof Integer n) {
                                String t = n.toString();
                                yield t;
                            }
                            throw new RuntimeException("type " + o);
                        }
                        case 2 -> {
                            String u = "u";
                            yield u;
                        }
                        default -> "d";
                    };
                }
            }
            """;

    /**
     * A {@code yield} inside an if: Kotlin's block arm takes its last expression as its value, so such an arm is a
     * {@code run { }} that returns from it; an arm that yields only at its end stays a block.
     */
    @Test
    public void yieldBeforeTheEnd() {
        String kotlin = kotlin(YIELD_IN_IF);
        contains(kotlin, "1 -> run {");
        contains(kotlin, "return@run o");
        contains(kotlin, "return@run t");
        contains(kotlin, "throw RuntimeException(");
        contains(kotlin, "val u = \"u\"");
        assertFalse(kotlin.contains("return@run u"), kotlin);
    }

    @Language("java")
    private static final String BOXED_OVERLOAD = """
            package a;
            class P {
                P(int type, Object value) { }
                P(int type, int index) { }
                static P of(int x) { return new P(1, Integer.valueOf(x)); }
                static P at(int x) { return new P(2, x); }
            }
            """;

    /**
     * Java's {@code Integer} argument takes the {@code (int, Object)} constructor; Kotlin types it {@code Int} and
     * would pick {@code (int, int)}: the argument is cast to the parameter's type.
     */
    @Test
    public void boxedArgumentKeepsJavasOverload() {
        String kotlin = kotlin(BOXED_OVERLOAD);
        contains(kotlin, "valueOf(x) as Any)");
        contains(kotlin, "P(2, x)");
    }

    @Language("java")
    private static final String TEST_FIXTURE_SHAPES = """
            package a;
            import java.util.List;
            enum Tool {
                JAVAC("javac") { public String run(String s) { return s + "c"; } },
                ECJ("ecj") { public String run(String s) { return s + "e"; } };
                private final String name;
                Tool(String name) { this.name = name; }
                public abstract String run(String s);
            }
            class Base {
                static List<String> collect(String s) { return List.of(s); }
            }
            class Sub extends Base { }
            class User {
                int count(String s) { return Sub.collect(s).size(); }
                String clean(String s) { return s.replace("\\r", "").stripTrailing(); }
            }
            """;

    /**
     * fernflower's test fixtures: an enum constant with members of its own, a static method called through a
     * subclass (a companion's members are not inherited), and Java's {@code String.stripTrailing()}.
     */
    @Test
    public void testFixtureShapes() {
        String kotlin = kotlin(TEST_FIXTURE_SHAPES);
        contains(kotlin, "JAVAC(\"javac\") {");
        contains(kotlin, "override fun run(s: String): String = s + \"c\"");
        contains(kotlin, "Base.collect(s).size");
        contains(kotlin, "as java.lang.String).stripTrailing()");
    }
}
