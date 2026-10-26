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

package io.codelaser.maddi.cst.print.rust;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mapping table of CodeLaser/maddi#167, row by row: a Java example, the verdicts the analysis computes for it
 * (taken from a run of the analyzer on the same code, see rust-printing.md), the Rust they print as, and rustc's
 * judgement. Each row has a counterpart where it can: the neighbouring verdict, and the error rustc gives for it, so
 * that the row is not merely compatible with the code but required by it.
 */
public class TestJavaToRustMapping extends CommonJavaToRust {

    @Language("java")
    private static final String PARAMS = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Params {
                public static int sum(List<Item> items) { int s = 0; for (Item i : items) s += i.get(); return s; }
                public static void clear(List<Item> items) { items.clear(); }
                public static void touchAll(List<Item> items) { for (Item i : items) i.touch(); }
                public static int use() {
                    List<Item> items = new ArrayList<>();
                    items.add(new Item());
                    touchAll(items);
                    int s = sum(items);
                    clear(items);
                    return s + sum(items);
                }
            }
            class Item {
                private int n;
                public int get() { return n; }
                public void touch() { n++; }
            }
            """;

    private List<TypeInfo> params(boolean touchAllUnmodified) {
        List<TypeInfo> types = parse(PARAMS);
        TypeInfo params = types.stream().filter(t -> t.simpleName().equals("Params")).findFirst().orElseThrow();
        TypeInfo item = types.stream().filter(t -> t.simpleName().equals("Item")).findFirst().orElseThrow();
        nonModifying(item, "get");
        modifying(item, "touch");
        borrowed(method(params, "sum").parameters().getFirst());
        modified(method(params, "clear").parameters().getFirst());
        // modified through its elements: the deep verdict (maddi-mod#59, #62)
        if (touchAllUnmodified) borrowed(method(params, "touchAll").parameters().getFirst());
        else modified(method(params, "touchAll").parameters().getFirst());
        return types;
    }

    /** Rows 1 to 4: {@code @NotModified} is {@code &T}, {@code @Modified} {@code &mut T}; the receiver likewise. */
    @DisplayName("rows 1-4: unmodified &T, modified &mut T, also through the elements; &self, &mut self: compiles")
    @Test
    public void parametersAndReceivers() throws Exception {
        translate(params(false));
        String rust = module("a.b");
        contains(rust, "pub fn sum(items: &Vec<Item>) -> i32 {");
        contains(rust, "pub fn clear(items: &mut Vec<Item>) {");
        contains(rust, "pub fn touch_all(items: &mut Vec<Item>) {");
        contains(rust, "pub fn get(&self) -> i32 {");
        contains(rust, "pub fn touch(&mut self) {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @DisplayName("rows 1-2, counterpart: a parameter modified through its elements, called unmodified: E0596")
    @Test
    public void elementModificationMissed() throws Exception {
        translate(params(true));
        String rust = module("a.b");
        contains(rust, "pub fn touch_all(items: &Vec<Item>) {");
        List<String> errors = rustcErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("E0596")), () -> String.join("\n", errors) + "\n" + rust);
    }

    @Language("java")
    private static final String CONTAINERS = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Bag {
                private final List<String> list = new ArrayList<>();
                public void addAll(List<String> more) { list.addAll(more); }
                public void add(String s) { list.add(s); }
                public boolean has(String s) { return list.contains(s); }
                public void drainInto(List<String> target) { target.addAll(list); list.clear(); }
            }
            """;

    /**
     * Row 5: a {@code @Container} modifies none of its methods' arguments, so none of its parameters is {@code &mut}.
     * Without {@code drainInto}, {@code Bag} is a container (the analysis says so); with it, it is not, and that
     * parameter is the only {@code &mut}.
     */
    @DisplayName("row 5: a @Container's parameters are & or moved, never &mut; the one that is not: &mut: compiles")
    @Test
    public void container() throws Exception {
        List<TypeInfo> types = parse(CONTAINERS);
        TypeInfo bag = types.getFirst();
        modifying(bag, "addAll", "add", "drainInto");
        nonModifying(bag, "has");
        borrowed(method(bag, "addAll").parameters().getFirst());
        borrowed(method(bag, "add").parameters().getFirst());
        borrowed(method(bag, "has").parameters().getFirst());
        modified(method(bag, "drainInto").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub fn add_all(&mut self, more: &Vec<String>) {");
        contains(rust, "pub fn add(&mut self, s: &str) {");
        contains(rust, "pub fn has(&self, s: &str) -> bool {");
        contains(rust, "pub fn drain_into(&mut self, target: &mut Vec<String>) {");
        assertEquals(1, rust.split(": &mut ", -1).length - 1, rust);
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String COPY = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Copy {
                private final List<String> list;
                public Copy(List<String> in) { this.list = new ArrayList<>(in); }
                public int size() { return list.size(); }
                public static int use() {
                    List<String> l = new ArrayList<>();
                    l.add("x");
                    Copy c = new Copy(l);
                    l.add("y");
                    return c.size() + l.size();
                }
            }
            """;

    private String copy(io.codelaser.maddi.cst.api.analysis.Value.Independent independent) throws Exception {
        List<TypeInfo> types = parse(COPY);
        TypeInfo copy = types.getFirst();
        nonModifying(copy, "size");
        if (independent.isIndependent()) borrowed(copy.constructors().getFirst().parameters().getFirst());
        else kept(copy.constructors().getFirst().parameters().getFirst(), independent);
        translate(types);
        return module("a.b");
    }

    /**
     * Row 6: an independent constructor parameter is copied by the constructor: borrowed, {@code &T}, and the copy is
     * the constructor's own ({@code clone()}, Java's {@code new ArrayList<>(in)}). The caller keeps its list.
     */
    @DisplayName("row 6: an independent constructor parameter is borrowed and copied; the caller keeps it: compiles")
    @Test
    public void independentConstructorParameter() throws Exception {
        String rust = copy(ValueImpl.IndependentImpl.INDEPENDENT);
        contains(rust, "pub fn new(r#in: &Vec<String>) -> Self {");
        contains(rust, "list: r#in.clone(),");
        contains(rust, "let c = Self::new(&l);");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @DisplayName("row 6, counterpart: called dependent, the parameter is moved in; the caller's later use is E0382")
    @Test
    public void independentConstructorParameterCalledDependent() throws Exception {
        String rust = copy(ValueImpl.IndependentImpl.DEPENDENT);
        contains(rust, "pub fn new(r#in: Vec<String>) -> Self {");
        List<String> errors = rustcErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("E0382")), () -> String.join("\n", errors) + "\n" + rust);
    }

    @Language("java")
    private static final String VIEW = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class View {
                private final List<String> list;
                public View(List<String> list) { this.list = list; }
                public int size() { return list.size(); }
                public String first() { return list.get(0); }
                public static int use() {
                    List<String> l = new ArrayList<>();
                    l.add("x");
                    View v = new View(l);
                    return v.size() + l.size();
                }
                public static View escape() {
                    List<String> l = new ArrayList<>();
                    return new View(l);
                }
            }
            """;

    /**
     * Row 7: a dependent field, which stores what it is given, is a borrow with a lifetime. Reading the list through
     * the view and directly, while the view lives, is two shared borrows: allowed. The view may not outlive the list:
     * {@code escape} returns a view of a local, rustc's E0515, where Java keeps the list alive as long as the view.
     */
    @DisplayName("row 7: a dependent field is a borrow with a lifetime; a view outliving its list is E0515")
    @Test
    public void dependentField() throws Exception {
        List<TypeInfo> types = parse(VIEW);
        TypeInfo view = types.getFirst();
        nonModifying(view, "size", "first");
        unmodifiedField(view, "list");
        kept(view.constructors().getFirst().parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub struct View<'a> {\nlist: &'a Vec<String>,\n}");
        contains(rust, "impl<'a> View<'a> {");
        contains(rust, "pub fn new(list: &'a Vec<String>) -> Self {");
        contains(rust, "let v = View::new(&l);");
        List<String> errors = rustcErrors();
        assertEquals(1, errors.size(), () -> String.join("\n", errors) + "\n" + rust);
        assertTrue(errors.getFirst().contains("E0515"), () -> String.join("\n", errors) + "\n" + rust);
    }

    @Language("java")
    private static final String BOX = """
            package a.b;
            public class Box<T> {
                private T t;
                public Box(T t) { this.t = t; }
                public T get() { return t; }
                public void set(T t) { this.t = t; }
                public static int use() {
                    Box<String> b = new Box<>("x");
                    return b.get().length();
                }
                public static int supplied(java.util.function.Supplier<Integer> s) { return s.get(); }
                public static int peeked(java.util.function.Supplier<Integer> s) { return s.get(); }
                public static void fed(java.util.function.Consumer<String> c) { c.accept("x"); }
                public static int both() { fed(s -> System.out.println(s)); return supplied(() -> 1) + peeked(() -> 2); }
            }
            """;

    /**
     * Row 8: hidden content is a type parameter without bounds: the type can store it, hand it out and replace it,
     * never look inside. The parameters that keep it are independent with hidden content (moved in), and the result
     * that is the hidden content shares it: a borrow. The class is called {@code Box}, as Rust's own: it is printed
     * {@code JBox}, so that a {@code Box<dyn Fn…>} stays Rust's. A closure parameter the method only calls is borrowed,
     * never a borrowed Box: a {@code dyn Fn} as {@code &dyn Fn} also when the analysis says the call modifies it
     * ({@code supplied}'s verdict is the CLI's: applying a function may modify, the marker its callers resolve,
     * maddi-mod#69), and the caller passes {@code &|| …}; a {@code dyn FnMut} as {@code &mut dyn FnMut}.
     */
    @DisplayName("row 8: hidden content is an unbounded generic T, handed out as &T: compiles")
    @Test
    public void hiddenContent() throws Exception {
        List<TypeInfo> types = parse(BOX);
        TypeInfo box = types.getFirst();
        nonModifying(box, "get");
        modifying(box, "set");
        kept(box.constructors().getFirst().parameters().getFirst(), ValueImpl.IndependentImpl.INDEPENDENT_HC);
        kept(method(box, "set").parameters().getFirst(), ValueImpl.IndependentImpl.INDEPENDENT_HC);
        method(box, "get").analysis().set(io.codelaser.maddi.cst.impl.analysis.PropertyImpl.INDEPENDENT_METHOD,
                ValueImpl.IndependentImpl.INDEPENDENT_HC);
        modified(method(box, "supplied").parameters().getFirst());
        modified(method(box, "fed").parameters().getFirst());
        borrowed(method(box, "peeked").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub struct JBox<T> {\nt: T,\n}");
        contains(rust, "JBox::new(");
        contains(rust, "pub fn supplied(s: &dyn Fn() -> i32) -> i32 {");
        contains(rust, "pub fn fed(c: &mut dyn FnMut(&str)) {");
        contains(rust, "Self::fed(&mut |s: &str| { println!(\"{}\", s); });");
        contains(rust, "Self::supplied(&|| 1)");
        contains(rust, "pub fn peeked(s: &dyn Fn() -> i32) -> i32 {");
        contains(rust, "pub fn get(&self) -> &T {");
        contains(rust, "pub fn set(&mut self, t: T) {");
        assertFalse(rust.contains("T: "), rust);
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String PRIMITIVE_FUNCTIONS = """
            package a.b;
            import java.util.function.*;
            public class Functions {
                static int supply(IntSupplier s) { return s.getAsInt(); }
                static boolean test(IntPredicate p, int i) { return p.test(i); }
                static int length(ToIntFunction<String> f, String s) { return f.applyAsInt(s); }
                static String join(BinaryOperator<String> op, String a, String b) { return op.apply(a, b); }
                static boolean same(BiPredicate<String, String> p, String a, String b) { return p.test(a, b); }
                static int combine(IntBinaryOperator op) { return op.applyAsInt(3, 4); }
                static long widen(IntToLongFunction f) { return f.applyAsLong(5); }
                static void each(IntConsumer c) { c.accept(1); c.accept(2); }
                static int all() {
                    int n = supply(() -> 1) + length(s -> s.length(), "abc") + combine((x, y) -> x * y);
                    each(i -> System.out.println(i));
                    if (test(i -> i > 0, n) && same((a, b) -> a.equals(b), "x", join((a, b) -> a + b, "", "x"))) {
                        return (int) widen(i -> i * 2L);
                    }
                    return n;
                }
            }
            """;

    /**
     * The primitive specializations of {@code java.util.function} and the interfaces without a closure of their own
     * ({@code BinaryOperator}, {@code BiPredicate}, {@code ToIntFunction}) are the closure their method describes: a
     * primitive by value, a type argument borrowed, a {@code void} method an {@code FnMut}.
     */
    @DisplayName("row 13b: IntSupplier, ToIntFunction, BinaryOperator, … are closures of their method's shape: compiles")
    @Test
    public void primitiveFunctions() throws Exception {
        List<TypeInfo> types = parse(PRIMITIVE_FUNCTIONS);
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub fn supply(mut s: Box<dyn Fn() -> i32>) -> i32 {");
        contains(rust, "pub fn test(mut p: Box<dyn Fn(i32) -> bool>, i: i32) -> bool {");
        contains(rust, "pub fn length(mut f: Box<dyn Fn(&str) -> i32>,");
        contains(rust, "return (f)(&s);");
        contains(rust, "pub fn join(mut op: Box<dyn Fn(&str, &str) -> String>,");
        contains(rust, "pub fn same(mut p: Box<dyn Fn(&str, &str) -> bool>,");
        contains(rust, "pub fn combine(mut op: Box<dyn Fn(i32, i32) -> i32>) -> i32 {");
        contains(rust, "pub fn widen(mut f: Box<dyn Fn(i32) -> i64>) -> i64 {");
        contains(rust, "pub fn each(mut c: Box<dyn FnMut(i32)>) {");
        contains(rust, "Self::combine(Box::new(move |x: i32, y: i32| x * y))");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String MOVES = """
            package a.b;
            import java.util.*;
            public class Moves {
                private String message;
                private final List<String> list;
                private final Set<String> set;
                public Moves(List<String> input, Set<String> s) { this.list = List.copyOf(input); this.set = new HashSet<>(s); }
                public void setMessage(String message) { this.message = message; }
                public void twice(String m) { this.message = m; System.out.println(m); }
                public static void fill(int[] array) { array[0] = 1; }
                public static int first(int[] array) { return array[0]; }
                public static int call() { int[] a = new int[2]; fill(a); return first(a); }
            }
            """;

    /**
     * A parameter moved in and read once, outside a loop and a closure, is moved at that read: no {@code .clone()} of
     * a string, and Java's copy of a collection ({@code List.copyOf}, a copy constructor) is the collection itself, as
     * nothing reads it afterwards. One read twice is still cloned. A borrowed array is a slice: a Java array never
     * changes length. (Both from worktree-maddi-bug's Java -> Rust -> clippy experiment: needless_pass_by_value,
     * ptr_arg.)
     */
    @DisplayName("a once-read parameter moved in is moved, not cloned; a borrowed array is a slice: compiles")
    @Test
    public void movesAndSlices() throws Exception {
        List<TypeInfo> types = parse(MOVES);
        TypeInfo moves = types.getFirst();
        modifying(moves, "setMessage", "twice");
        modified(method(moves, "fill").parameters().getFirst());
        borrowed(method(moves, "first").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "self.message = message;");
        contains(rust, "list: input,");
        contains(rust, "set: s,");
        contains(rust, "self.message = m.clone();");
        contains(rust, "pub fn fill(array: &mut [i32]) {");
        contains(rust, "pub fn first(array: &[i32]) -> i32 {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String NODES = """
            package a.b;
            import java.util.*;
            public class Node {
                private Node parent;
                private final List<Map.Entry<String, Integer>> counts = new ArrayList<>();
                public void setParent(Node parent) { this.parent = parent; }
                public boolean same(Node other) { return this == other; }
                public static int depth(Node n) { int d = 0; while (n.parent != null) { n = n.parent; d++; } return d; }
                public void count(String key, int value) { counts.add(new AbstractMap.SimpleEntry<>(key, value)); }
                public int total() { int t = 0; for (Map.Entry<String, Integer> e : counts) t += e.getValue(); return t; }
            }
            """;

    /**
     * A struct holding itself by value has no size (E0072): the field is a {@code Box}, here {@code Option<Box<Node>>},
     * read as {@code as_deref()} and written boxed. Java's {@code ==} on objects is identity: {@code std::ptr::eq}. A
     * nullable value assigned where the verdicts hold the target non-null is unwrapped, Java's NPE as a panic. A map
     * entry is the {@code (key, value)} tuple a map's iteration yields; a {@code SimpleEntry} is constructed as one.
     */
    @DisplayName("a recursive field is boxed, == on objects is identity, an entry is a tuple: compiles")
    @Test
    public void recursiveIdentityEntries() throws Exception {
        List<TypeInfo> types = parse(NODES);
        TypeInfo node = types.getFirst();
        modifying(node, "setParent", "count");
        nonModifying(node, "same", "total");
        kept(method(node, "setParent").parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        borrowed(method(node, "same").parameters().getFirst());
        borrowed(method(node, "depth").parameters().getFirst());
        translate(types, nullable(java.util.Set.of("parent")));
        String rust = module("a.b");
        contains(rust, "parent: Option<Box<Node>>,");
        contains(rust, "counts: Vec<(String, i32)>,");
        contains(rust, "self.parent = parent.map(Box::new);");
        contains(rust, "return std::ptr::eq(self, other);");
        contains(rust, "n = n.parent.as_deref().unwrap();");
        contains(rust, "self.counts.push((key, value));");
        contains(rust, "t += e.1.clone();");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String STORED_T = """
            package a.b;
            import java.util.List;
            public class G {
                public static <T> void listAdd(List<T> list, T t) { list.add(t); }
                public static <T> T same(T t) { return t; }
                public static <T> List<T> one(T t) { return List.of(t); }
                public static <T> int size(List<T> list, T t) { return t == null ? 0 : list.size(); }
                public static <T> boolean has(List<T> list, T t) { return list.contains(t); }
                public static <K> int distinct(List<K> keys) { return new java.util.HashSet<>(keys).size(); }
            }
            """;

    /**
     * A {@code T} parameter is independent whatever the body does with it (an unbounded {@code T} has no content to
     * modify; independence is measured against {@code this} and the result). The body decides: one it stores is moved
     * in, one it only looks at is borrowed.
     */
    @DisplayName("a T parameter the body stores is moved in, one it only reads is borrowed: compiles")
    @Test
    public void storedTypeParameter() throws Exception {
        List<TypeInfo> types = parse(STORED_T);
        TypeInfo g = types.getFirst();
        for (String m : List.of("listAdd", "same", "one", "size", "has")) {
            method(g, m).parameters().forEach(p -> {
                if (p.parameterizedType().isTypeParameter()) borrowed(p);
            });
        }
        modified(method(g, "listAdd").parameters().getFirst());
        borrowed(method(g, "size").parameters().getFirst());
        borrowed(method(g, "has").parameters().getFirst());
        borrowed(method(g, "distinct").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub fn list_add<T>(list: &mut Vec<T>, t: T) {");
        contains(rust, "pub fn same<T>(t: T) -> T {");
        contains(rust, "pub fn one<T>(t: T) -> Vec<T> {");
        contains(rust, "pub fn size<T>(list: &Vec<T>, t: &T) -> i32 {");
        // the bounds the uses need (RustBounds): contains compares, a HashSet hashes, and copies its elements
        contains(rust, "pub fn has<T: PartialEq>(list: &Vec<T>, t: &T) -> bool {");
        contains(rust, "pub fn distinct<K: Clone + Eq + std::hash::Hash>(keys: &Vec<K>) -> i32 {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String NULLABLE_GETTERS = """
            package a.b;
            import java.util.List;
            public class Tags {
                private List<String> tags;
                private String label;
                public void setTags(List<String> tags) { this.tags = tags; }
                public List<String> tags() { return tags; }
                public String label() { return label; }
            }
            """;

    /** A nullable field handed out as a borrow is {@code Option<&T>}: the Option's content borrowed, not the Option. */
    @DisplayName("a nullable field returned as a borrow is as_ref() / as_deref(): compiles")
    @Test
    public void nullableBorrowedResult() throws Exception {
        List<TypeInfo> types = parse(NULLABLE_GETTERS);
        TypeInfo tags = types.getFirst();
        modifying(tags, "setTags");
        nonModifying(tags, "tags", "label");
        dependentResult(tags, "tags");
        dependentResult(tags, "label");
        translate(types, nullable(java.util.Set.of("tags", "label", "tags()", "label()")));
        String rust = module("a.b");
        contains(rust, "pub fn tags(&self) -> Option<&Vec<String>> {\nreturn self.tags.as_ref();");
        contains(rust, "pub fn label(&self) -> Option<&str> {\nreturn self.label.as_deref();");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String STREAMS = """
            package a.b;
            import java.util.List;
            import java.util.stream.Stream;
            public class Names {
                private final List<String> names;
                public Names(List<String> names) { this.names = names; }
                public Stream<String> longOnes() { return names.stream().filter(n -> n.length() > 3); }
                public static long count(Stream<String> s) { return s.count(); }
            }
            """;

    /**
     * A stream is an iterator chain: {@code Box<dyn Iterator<Item = T>>}, {@code + '_} as a method's result (it borrows
     * the object), {@code impl Iterator} as a borrowed parameter.
     */
    @DisplayName("Stream<T> as a declared type is a dyn Iterator, boxed where returned: compiles")
    @Test
    public void streamsAsIterators() throws Exception {
        List<TypeInfo> types = parse(STREAMS);
        TypeInfo names = types.getFirst();
        nonModifying(names, "longOnes");
        kept(names.constructors().getFirst().parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub fn long_ones(&self) -> Box<dyn Iterator<Item = String> + '_> {");
        contains(rust, "pub fn count(mut s: Box<dyn Iterator<Item = String>>) -> i64 {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String LINKED = """
            package a.b;
            import java.util.List;
            public class Linked {
                private int a;
                private final List<String> names = new java.util.ArrayList<>();
                public Linked set(int a) { this.a = a; return this; }
                public List<String> view() { return java.util.Collections.unmodifiableList(names); }
                public Items build() { return new Items("x", List.of()); }
                public static Items copy(String name, List<String> items) { return new Items(name, items); }
                public static String first(List<String> l) { return l.get(0); }
                record Items(String name, List<String> items) { }
            }
            """;

    /**
     * Where a result comes from, which the links know and the independence verdicts do not ({@code LinkFacts}): a new
     * object is owned even where the verdict says it borrows the receiver ({@code build()}, independent apart from
     * hidden content), and the parameters it keeps are moved in ({@code copy}); a result inside a parameter's object
     * borrows that parameter ({@code first}); a new object holding the receiver's contents that no return creates
     * ({@code view}, Collections.unmodifiableList) stays the borrow the verdict says.
     */
    @DisplayName("link facts: a new result is owned, its kept parameters moved in; one inside a parameter borrows it")
    @Test
    public void linkFacts() throws Exception {
        List<TypeInfo> types = parse(LINKED);
        TypeInfo linked = types.getFirst();
        modifying(linked, "set");
        fluent(linked, "set");
        nonModifying(linked, "build", "view");
        dependentResult(linked, "build");
        dependentResult(linked, "view");
        method(linked, "copy").parameters().forEach(CommonJavaToRust::borrowed);
        borrowed(method(linked, "first").parameters().getFirst());
        MethodInfo build = method(linked, "build");
        MethodInfo copy = method(linked, "copy");
        MethodInfo first = method(linked, "first");
        MethodInfo view = method(linked, "view");
        io.codelaser.maddi.cst.impl.analysis.LinkFacts links = new io.codelaser.maddi.cst.impl.analysis.LinkFacts() {
            @Override
            public Result result(MethodInfo m) {
                return m == build || m == copy || m == view ? Result.NEW : m == first ? Result.EXISTING : null;
            }

            @Override
            public boolean receiverInResult(MethodInfo m) {
                return m == view;
            }

            @Override
            public java.util.Set<io.codelaser.maddi.cst.api.info.ParameterInfo> keptInResult(MethodInfo m) {
                return m == copy ? java.util.Set.copyOf(copy.parameters())
                        : m == first ? java.util.Set.of(first.parameters().getFirst()) : java.util.Set.of();
            }
        };
        translate(types, RustFacts.of(io.codelaser.maddi.cst.impl.analysis.NullabilityFacts.STORED, RustFacts.STORED,
                links));
        String rust = module("a.b");
        contains(rust, "pub fn build(&self) -> LinkedItems {");
        // a new object holding the receiver's contents that the method does not create itself: a view, borrowed
        contains(rust, "pub fn view(&self) -> &Vec<String> {");
        contains(rust, "pub fn copy(name: String, items: Vec<String>) -> LinkedItems {");
        contains(rust, "pub fn first(l: &Vec<String>) -> &str {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String KEYWORD_PARAMETER = """
            package a.b;
            public class Use {
                interface Strategy { int apply(String in); }
                static int run(Strategy s) { return s.apply("x"); }
                static int go() { return run(in -> in.length()); }
            }
            """;

    /** A parameter named as a Rust keyword ({@code r#in}) is a type only in the blanket impl's {@code Fn} bound (gson). */
    @DisplayName("a raw-identifier parameter is a type only in a functional interface's Fn bound: compiles")
    @Test
    public void keywordParameterInFnBound() throws Exception {
        List<TypeInfo> types = parse(KEYWORD_PARAMETER);
        TypeInfo strategy = types.getFirst().findSubType("Strategy");
        nonModifying(strategy, "apply");
        borrowed(method(strategy, "apply").parameters().getFirst());
        borrowed(method(types.getFirst(), "run").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "impl<F: Fn(&str) -> i32> UseStrategy for F {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String WRITE_THROUGH = """
            package a.b;
            import java.util.Map;
            public class Keeper {
                static class Box { int j; void set(int v) { j = v; } }
                private Box kept;
                private Box other;
                public void mutate() { kept.set(1); }
                public void assign() { other.j = 2; }
                public static void touch(Map<String, Box> m, String k) { m.get(k).set(3); }
            }
            """;

    /**
     * A write through a nullable place reaches it exclusively: {@code as_mut()} for a modifying call or an assignment
     * through a nullable field, {@code get_mut(k)} for a map's value that is modified.
     */
    @DisplayName("writes through a nullable field or a map's value reach it with as_mut() / get_mut(): compiles")
    @Test
    public void writeThroughExclusive() throws Exception {
        List<TypeInfo> types = parse(WRITE_THROUGH);
        TypeInfo keeper = types.getFirst();
        TypeInfo box = keeper.findSubType("Box");
        modifying(box, "set");
        modifying(keeper, "mutate", "assign");
        modified(method(keeper, "touch").parameters().getFirst());
        borrowed(method(keeper, "touch").parameters().get(1));
        translate(types, nullable(java.util.Set.of("kept", "other")));
        String rust = module("a.b");
        contains(rust, "self.kept.as_mut().unwrap().set(1);");
        contains(rust, "self.other.as_mut().unwrap().j = 2;");
        contains(rust, "m.get_mut(k).as_mut().unwrap().set(3);");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String MUTABLE_GETTERS = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Bag {
                static class Inner { int i; }
                private final List<String> items = new ArrayList<>();
                private final Inner inner = new Inner();
                public List<String> items() { return items; }
                public Inner inner() { return inner; }
                public int size() { return items().size(); }
                public static void fill(Bag b) { b.items().add("x"); b.inner().i = 3; }
            }
            """;

    /**
     * A caller that modifies what a getter hands out needs it exclusively: the getter gets an exclusive twin,
     * {@code items_mut(&mut self) -> &mut Vec<String>}, which those call sites call; a reading call site keeps the getter.
     */
    @DisplayName("a getter a caller modifies through gets an _mut twin: compiles")
    @Test
    public void mutableGetterTwin() throws Exception {
        List<TypeInfo> types = parse(MUTABLE_GETTERS);
        TypeInfo bag = types.getFirst();
        nonModifying(bag, "items", "inner", "size");
        dependentResult(bag, "items");
        dependentResult(bag, "inner");
        modified(method(bag, "fill").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub fn items_mut(&mut self) -> &mut Vec<String> {\n&mut self.items\n}");
        contains(rust, "pub fn inner_mut(&mut self) -> &mut BagInner {");
        contains(rust, "b.items_mut().push(\"x\".to_string());");
        contains(rust, "b.inner_mut().i = 3;");
        contains(rust, "return self.items().len() as i32;");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String PAIR = """
            package a.b;
            public record Pair<T>(T left, T right) {
                public Pair<T> swap() { return new Pair<>(right, left); }
            }
            class Holder {
                private final Pair<String> pair;
                Holder(Pair<String> pair) { this.pair = pair; }
                String left() { return pair.left(); }
                static String use() {
                    Pair<String> p = new Pair<>("a", "b");
                    Holder h1 = new Holder(p);
                    Holder h2 = new Holder(p.swap());
                    Holder h3 = new Holder(p);
                    return h1.left() + h2.left() + h3.left();
                }
            }
            """;

    /**
     * Row 9: an {@code @Immutable(hc=true)} object is shared without a lock or a {@code RefCell}: an {@code Rc}, which
     * two holders keep at once, as they keep the same Java object. Its hidden content is shared by cloning, under a
     * {@code Clone} bound: {@code swap} builds a new pair over the same two values.
     */
    @DisplayName("row 9: @Immutable(hc=true) is shared as Rc<T>, its hidden content cloned under T: Clone: compiles")
    @Test
    public void immutableHcShared() throws Exception {
        List<TypeInfo> types = parse(PAIR);
        TypeInfo pair = types.stream().filter(t -> t.simpleName().equals("Pair")).findFirst().orElseThrow();
        TypeInfo holder = types.stream().filter(t -> t.simpleName().equals("Holder")).findFirst().orElseThrow();
        immutable(pair, ValueImpl.ImmutableImpl.IMMUTABLE_HC);
        nonModifying(pair, "swap");
        independentResult(pair, "swap");
        pair.constructors().getFirst().parameters()
                .forEach(p -> kept(p, ValueImpl.IndependentImpl.INDEPENDENT_HC));
        nonModifying(holder, "left");
        kept(holder.constructors().getFirst().parameters().getFirst(), ValueImpl.IndependentImpl.INDEPENDENT_HC);
        translate(types);
        String rust = module("a.b");
        contains(rust, "impl<T: Clone> Pair<T> {");
        contains(rust, "pub fn swap(&self) -> Rc<Pair<T>> {");
        contains(rust, "return Rc::new(Pair::new(self.right.clone(), self.left.clone()));");
        contains(rust, "pair: Rc<Pair<String>>,");
        contains(rust, "let h1 = Self::new(p.clone());");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String FINALE = """
            package a.b;
            public class Finale {
                private final StringBuilder sb = new StringBuilder();
                public Finale add(String s) { sb.append(s); return this; }
                public String build() { return sb.toString(); }
                public static String use() {
                    Finale f = new Finale();
                    f.add("a").add("b");
                    return f.build();
                }
                public static String reuse() {
                    Finale f = new Finale();
                    String s = f.build();
                    f.add("c");
                    return s;
                }
            }
            """;

    /**
     * Row 10: a {@code @Finalizer} takes {@code self} by value: the call consumes the object. The book's rule, that
     * nothing may be called on the object after its finalizer, is rustc's E0382 in {@code reuse}. A fluent method
     * ({@code return this}) hands back the borrow of its receiver.
     */
    @DisplayName("row 10: a @Finalizer takes self by value; a call after it is E0382")
    @Test
    public void finalizer() throws Exception {
        List<TypeInfo> types = parse(FINALE);
        TypeInfo finale = types.getFirst();
        modifying(finale, "add");
        fluent(finale, "add");
        finalizer(finale, "build");
        borrowed(method(finale, "add").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub fn add(&mut self, s: &str) -> &mut Self {");
        contains(rust, "pub fn build(self) -> String {");
        List<String> errors = rustcErrors();
        assertEquals(1, errors.size(), () -> String.join("\n", errors) + "\n" + rust);
        assertTrue(errors.getFirst().contains("E0382"), () -> String.join("\n", errors) + "\n" + rust);
    }

    @Language("java")
    private static final String CONFIG = """
            package a.b;
            import io.codelaser.maddi.support.SetOnce;
            public class Config {
                private final SetOnce<String> name = new SetOnce<>();
                private int reads;
                public void setName(String s) { name.set(s); }
                public String name() { return name.get(); }
                public boolean isNamed() { return name.isSet(); }
                public void read() { reads++; }
                public static String use() {
                    Config c = new Config();
                    c.read();
                    c.setName("x");
                    c.read();
                    return c.name();
                }
                public static String early() {
                    Config c = new Config();
                    String n = c.name();
                    c.setName("x");
                    return n;
                }
                public static void twice() {
                    Config c = new Config();
                    c.setName("x");
                    c.setName("y");
                }
            }
            """;

    /**
     * Row 11: an eventually immutable type is a typestate. {@code setName} is the {@code @Mark} (it sets the
     * {@code SetOnce}), {@code name()} is {@code @Only(after)}: the struct gets a state parameter, the mark consumes
     * the object in its before state and returns it in its after state. Reading the name before it is set, and setting
     * it twice, throw in Java; in Rust neither method exists in that state: E0599, twice.
     */
    @DisplayName("row 11: @Mark/@Only as a typestate; a call on the wrong side is E0599")
    @Test
    public void typestate() throws Exception {
        List<TypeInfo> types = parse(CONFIG);
        TypeInfo config = types.getFirst();
        mark(config, "setName", "name");
        only(config, "name", "name", true);
        modifying(config, "read");
        nonModifying(config, "name", "isNamed");
        borrowed(method(config, "setName").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub struct Config<S = Before> {");
        contains(rust, "_s: PhantomData<S>,");
        contains(rust, "impl Config<Before> {");
        contains(rust, "pub fn set_name(mut self, s: &str) -> Config<After> {");
        contains(rust, "impl<S> Config<S> {");
        contains(rust, "impl Config<After> {\npub fn name(&self) -> String {");
        contains(rust, "let mut c = c.set_name(\"x\");");
        List<String> errors = rustcErrors();
        assertEquals(2, errors.size(), () -> String.join("\n", errors) + "\n" + rust);
        assertTrue(errors.stream().allMatch(e -> e.contains("E0599")), () -> String.join("\n", errors) + "\n" + rust);
        // both in early() and twice(), after use(): use() compiles
        int early = (int) rust.substring(0, rust.indexOf("pub fn early()")).lines().count() + 1;
        assertTrue(errors.stream().allMatch(e -> Integer.parseInt(e.replaceFirst("^.*?\\.rs:(\\d+):.*$", "$1")) > early),
                () -> String.join("\n", errors) + "\n" + rust);
    }

    @Language("java")
    private static final String LOGS = """
            package a.b;
            import io.codelaser.maddi.annotation.rare.IgnoreModifications;
            import java.util.ArrayList;
            import java.util.List;
            public class Logs {
                private final List<String> buffer = new ArrayList<>();
                private int total;
                @IgnoreModifications
                public void log(String s) { buffer.add(s); }
                public int twice(int x) { log("twice"); return 2 * x; }
                public void add(int x) { log("add"); total += x; }
                public int logged() { return buffer.size(); }
                public void clear() { buffer.clear(); }
            }
            """;

    private String logs(boolean ignore) throws Exception {
        List<TypeInfo> types = parse(LOGS);
        TypeInfo logs = types.getFirst();
        if (ignore) ignoreModifications(logs, "log");
        modifying(logs, "log", "add", "clear");
        // twice() modifies nothing but through log(): non-modifying, when log's modifications are ignored
        nonModifying(logs, "twice", "logged");
        borrowed(method(logs, "log").parameters().getFirst());
        translate(types);
        return module("a.b");
    }

    /**
     * Row 12: {@code @IgnoreModifications} on a method is interior mutability. The log method takes {@code &self},
     * so that a method which only logs is {@code &self} too, as its verdict says; the buffer it writes is a
     * {@code RefCell}, borrowed mutably there, read through {@code borrow()}, reached through {@code get_mut()} where
     * the object is held exclusively.
     */
    @DisplayName("row 12: @IgnoreModifications is &self over a RefCell; without it, the logging caller is E0596")
    @Test
    public void ignoreModifications() throws Exception {
        String rust = logs(true);
        contains(rust, "buffer: RefCell<Vec<String>>,");
        contains(rust, "buffer: RefCell::new(Vec::new()),");
        contains(rust, "pub fn log(&self, s: &str) {\n(*self.buffer.borrow_mut()).push(s.to_string());");
        contains(rust, "pub fn twice(&self, x: i32) -> i32 {");
        contains(rust, "return (*self.buffer.borrow()).len() as i32;");
        contains(rust, "(*self.buffer.get_mut()).clear();");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @DisplayName("row 12, counterpart: the log method's modifications not ignored, the logging &self caller is E0596")
    @Test
    public void ignoreModificationsMissing() throws Exception {
        String rust = logs(false);
        contains(rust, "pub fn log(&mut self, s: &str) {");
        List<String> errors = rustcErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("E0596")), () -> String.join("\n", errors) + "\n" + rust);
    }

    @Language("java")
    private static final String COUNTER = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Counter {
                private static int count;
                static final List<String> names = new ArrayList<>();
                static List<String> log = new ArrayList<>();
                public static int next() { return ++count; }
                public static int peek() { return count; }
                public static int nextTwice() { return next() + next(); }
            }
            class Client {
                static int twice() { return Counter.next() + Counter.next(); }
                static int current() { return Counter.peek(); }
                static void note(String s) { Counter.log.add(s); }
                static int pure(int x) { return 2 * x; }
                static int viaPure(int x) { return pure(x); }
                static int apply(java.util.function.Supplier<Integer> s) { return s.get(); }
                static int viaReference() { return apply(Counter::next); }
            }
            """;

    private String counter(boolean verdict) throws Exception {
        List<TypeInfo> types = parse(COUNTER);
        TypeInfo client = types.stream().filter(t -> t.simpleName().equals("Client")).findFirst().orElseThrow();
        // a verdict on a method touching no static mut of ours: how a library contract (System.setOut) arrives
        if (verdict) staticSideEffects(client, "pure");
        translate(types);
        return module("a.b");
    }

    /**
     * Row 13: Java's mutable static state is a {@code static mut}, which Rust lets only {@code unsafe} code touch,
     * reading as well as writing: a method touching one, of any type, is an {@code unsafe fn}, and so is every method
     * calling one, as E0133 demands (RustUnsafety). No verdict is needed for that: {@code @StaticSideEffects} counts
     * the modifications of another type's static state (maddi-mod#65), which {@code twice} and {@code note} have, but
     * not {@code current}'s read through {@code peek}, nor {@code nextTwice}'s, within Counter.
     */
    @DisplayName("row 13: static state is a static mut, a method touching it, or calling one that does, an unsafe fn: compiles")
    @Test
    public void staticSideEffects() throws Exception {
        String rust = counter(false);
        contains(rust, "static mut COUNTER_COUNT: i32 = 0;");
        contains(rust, "pub static mut COUNTER_LOG: Vec<String> = Vec::new();");
        contains(rust, "pub unsafe fn next() -> i32 {\nunsafe {");
        contains(rust, "pub unsafe fn peek() -> i32 {");
        contains(rust, "pub unsafe fn next_twice() -> i32 {");
        contains(rust, "unsafe fn twice() -> i32 {");
        contains(rust, "unsafe fn current() -> i32 {");
        contains(rust, "pub unsafe fn note(");
        contains(rust, "fn pure(x: i32) -> i32 {\nreturn 2 * x;");
        assertFalse(rust.contains("unsafe fn pure"), rust);
        assertFalse(rust.contains("unsafe fn via_pure"), rust);
        // a method reference is a closure written in the unsafe body, which it inherits; 'apply' calls a closure
        contains(rust, "unsafe fn via_reference() -> i32 {");
        assertFalse(rust.contains("unsafe fn apply"), rust);
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @DisplayName("row 13, the verdict: @StaticSideEffects is an unsafe fn without a static mut, and so are its callers")
    @Test
    public void staticSideEffectsVerdict() throws Exception {
        String rust = counter(true);
        contains(rust, "unsafe fn pure(x: i32) -> i32 {");
        contains(rust, "unsafe fn via_pure(x: i32) -> i32 {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String NULLS = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            public class Nulls {
                private final Map<String, String> map = new HashMap<>();
                private String last;
                public String find(String k) { return map.get(k); }
                public void put(String k, String v) { map.put(k, v); last = k; }
                public String last() { return last; }
            }
            """;

    /** Row 14: nullable is {@code Option<T>}. */
    @DisplayName("row 14: a nullable field and results are Option<T>: compiles")
    @Test
    public void nullability() throws Exception {
        List<TypeInfo> types = parse(NULLS);
        TypeInfo nulls = types.getFirst();
        nonModifying(nulls, "find", "last");
        modifying(nulls, "put");
        borrowed(method(nulls, "find").parameters().getFirst());
        method(nulls, "put").parameters().forEach(CommonJavaToRust::borrowed);
        independentResult(nulls, "find");
        independentResult(nulls, "last");
        translate(types, nullable(Set.of("last", "find()", "last()")));
        String rust = module("a.b");
        contains(rust, "last: Option<String>,");
        contains(rust, "pub fn find(&self, k: &str) -> Option<String> {");
        contains(rust, "pub fn last(&self) -> Option<String> {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String APPLY = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.function.Function;
            public class Apply {
                static int whole(List<String> list, Function<List<String>, Integer> f) {
                    return f.apply(list);
                }
                static int element(List<String> list, Function<String, Integer> f) {
                    return f.apply(list.get(0));
                }
                static int use() {
                    List<String> list = new ArrayList<>();
                    list.add("a");
                    return whole(list, l -> l.size()) + element(list, s -> s.length());
                }
            }
            """;

    /*
     A parameter handed to a passed function, whole or an element of it, is unmodified with an entry in
     MODIFIED_THROUGH_PASSED_FUNCTION (CodeLaser/maddi-mod#67, #69): modified only when the caller's function modifies.
     The printer's closures borrow their arguments shared (dyn Fn(&T)), so the unmodified verdict is the one that
     compiles: '&Vec<String>' into 'Fn(&Vec<String>)', and the element borrowed, '&list[0]'. A caller passing a
     function that modifies is rustc's E0596 in that closure: the call site the entry resolves to modified.
     */
    @DisplayName("a parameter handed to a passed function, whole or an element: & into Fn(&T), compiles")
    @Test
    public void passedFunction() throws Exception {
        List<TypeInfo> types = parse(APPLY);
        TypeInfo apply = types.getFirst();
        throughPassedFunction(method(apply, "whole").parameters().getFirst(), "1:0");
        throughPassedFunction(method(apply, "element").parameters().getFirst(), "1:0:e");
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub fn whole(list: &Vec<String>, mut f: Box<dyn Fn(&Vec<String>) -> i32>) -> i32 {\nreturn (f)(list);");
        contains(rust, "return (f)(&list[0]);");
        assertEquals(List.of(), rustcErrors(), rust);
    }
}
