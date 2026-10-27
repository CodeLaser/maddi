/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private static final String BORROWED_TEMPORARIES = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Args {
                private List<String> arguments;
                private final List<String> names = new ArrayList<>();
                public List<String> arguments() {
                    if (arguments == null) arguments = new ArrayList<>();
                    return arguments;
                }
                public Args copy() { return new Args(); }
                public int count() { return names.size(); }
                public List<String> orNames(java.util.Optional<List<String>> opt) { return opt.orElse(names); }
                public List<String> orAlt(java.util.Optional<List<String>> opt, List<String> alt) {
                    return opt.orElseGet(() -> alt);
                }
            }
            """;

    /**
     * A result that is never null held in an Option field is a borrow of the Option's content, not of a temporary
     * moved out of the field; a method whose every return creates its result returns it owned, whatever the verdicts
     * say of it (a builder's {@code build()}).
     */
    @DisplayName("a borrowed result is no temporary: as_ref().unwrap(), an owned new object: compiles")
    @Test
    public void borrowedTemporaries() throws Exception {
        List<TypeInfo> types = parse(BORROWED_TEMPORARIES);
        TypeInfo args = types.getFirst();
        modifying(args, "arguments");
        nonModifying(args, "copy", "count");
        dependentResult(args, "arguments");
        dependentResult(args, "copy");
        nonModifying(args, "orNames");
        dependentResult(args, "orNames");
        nonModifying(args, "orAlt");
        borrowed(method(args, "orAlt").parameters().getLast());
        translate(types, nullable(java.util.Set.of("arguments")));
        String rust = module("a.b");
        contains(rust, "pub fn arguments(&mut self) -> &Vec<String> {");
        contains(rust, "return self.arguments.as_ref().unwrap();");
        contains(rust, "pub fn copy(&self) -> Args {\nreturn Self::new();");
        contains(rust, "-> Vec<String> {\nreturn opt.unwrap_or(self.names.clone());");
        contains(rust, "return opt.unwrap_or_else(|| alt.clone());");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String TO_OBJECT = """
            package a.b;
            public class Objs {
                public static Object label() { return "x"; }
                public static int count(Object o) { return o == null ? 0 : 1; }
                public static int use() { return count("y") + count(label()); }
            }
            """;

    /** A value where an {@code Object} is expected, as an argument or a result, is boxed as a {@code Box<dyn Any>}. */
    @DisplayName("a value passed or returned as an Object is boxed: compiles")
    @Test
    public void boxedAsObject() throws Exception {
        List<TypeInfo> types = parse(TO_OBJECT);
        translate(types);
        String rust = module("a.b");
        contains(rust, "return Box::new(\"x\".to_string());");
        contains(rust, "Self::count(Box::new(\"y\".to_string()))");
        contains(rust, "Self::count(Self::label())");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String PATTERNS = """
            package a.b;
            public class Pat {
                static final class M {
                    private int i;
                    int getI() { return i; }
                    void setI(int i) { this.i = i; }
                }
                static int find(Object o) { if (o instanceof M m) return m.getI(); return -1; }
                static void bump(Object o) { if (o instanceof M m) { m.setI(m.getI() + 1); } }
                static String text(Object o) { if (o instanceof String s) return s; return ""; }
                static int number(Object o) { if (o instanceof Integer i) return i; return o instanceof M ? 1 : 0; }
            }
            """;

    /** {@code instanceof} of an Object downcasts its {@code Box<dyn Any>}: a pattern variable is an {@code if let}. */
    @DisplayName("instanceof of an Object, with and without a pattern variable: compiles")
    @Test
    public void instanceOfDowncast() throws Exception {
        List<TypeInfo> types = parse(PATTERNS);
        TypeInfo pat = types.getFirst();
        TypeInfo m = pat.subTypes().getFirst();
        nonModifying(m, "getI");
        for (String name : List.of("find", "text", "number")) borrowed(method(pat, name).parameters().getFirst());
        exclusive(method(pat, "bump").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "if let Some(m) = o.downcast_ref::<PatM>() {\nreturn m.get_i();");
        contains(rust, "if let Some(m) = o.downcast_mut::<PatM>() {\nm.set_i(m.get_i() + 1);");
        contains(rust, "if let Some(s) = o.downcast_ref::<String>() {");
        contains(rust, "if let Some(&i) = o.downcast_ref::<i32>() {\nreturn i;");
        contains(rust, "return if o.is::<PatM>() { 1 } else { 0 };");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String SEQUENCED = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Seq {
                static int ends(List<String> parts) {
                    List<String> copy = new ArrayList<>(parts);
                    String first = copy.removeFirst();
                    String last = copy.removeLast();
                    StringBuilder sb = new StringBuilder(first);
                    sb.append(last);
                    return sb.length() + copy.size();
                }
                static String head() {
                    List<String> l = new ArrayList<>();
                    return l.removeFirst();
                }
                static List<String> all(List<String> in) {
                    List<String> out = new ArrayList<>();
                    in.forEach(out::add);
                    java.util.Set<String> seen = new java.util.HashSet<>();
                    in.forEach(seen::add);
                    return out;
                }
            }
            """;

    /**
     * A list's removeFirst and removeLast; StringBuilder's length, declared by AbstractStringBuilder; a bound
     * {@code out::add} as a closure.
     */
    @DisplayName("removeFirst, removeLast, StringBuilder.length and out::add: compiles")
    @Test
    public void sequencedAndBuilderLength() throws Exception {
        List<TypeInfo> types = parse(SEQUENCED);
        borrowed(method(types.getFirst(), "ends").parameters().getFirst());
        borrowed(method(types.getFirst(), "all").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "let first = copy.remove(0);");
        contains(rust, "let last = copy.pop().unwrap();");
        contains(rust, "return sb.len() as i32");
        contains(rust, "r#in.iter().for_each(|x| { out.push(x.clone()); });");
        // nothing is added to l: Rust has no element type to infer
        contains(rust, "let mut l: Vec<String> = Vec::new();");
        contains(rust, "r#in.iter().for_each(|x| { seen.insert(x.clone()); });");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String UNBOUND_LIST = """
            package a.b;
            import java.util.List;
            import java.util.Optional;
            public class Firsts {
                static int firstLength(Optional<List<String>> o) {
                    return o.map(List::getFirst).map(String::length).orElse(0);
                }
                static int size(Optional<List<String>> o) {
                    return o.map(List::size).orElse(0);
                }
            }
            """;

    /** An unbound {@code List::getFirst} borrows the first element; {@code List::size} is its length. */
    @DisplayName("List::getFirst and List::size as closures: compiles")
    @Test
    public void unboundListReferences() throws Exception {
        List<TypeInfo> types = parse(UNBOUND_LIST);
        borrowed(method(types.getFirst(), "firstLength").parameters().getFirst());
        borrowed(method(types.getFirst(), "size").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "|x| &x[0]");
        contains(rust, "|x| x.len() as i32");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String INITIALIZED_ARRAYS = """
            package a.b;
            public class Inits {
                private static final char[] HEX = {'0', '1'};
                private final String[] names = {"a", "b"};
                private final int[][] grid = {{1, 2}, {3}};
                int sum() {
                    int[] b = {1, 2};
                    return b[0] + grid[1][0] + names.length + HEX.length;
                }
            }
            """;

    /** An array initializer, also without {@code new}: a vec of owned elements, nested for a nested array. */
    @DisplayName("array initializers: compiles")
    @Test
    public void initializedArrays() throws Exception {
        List<TypeInfo> types = parse(INITIALIZED_ARRAYS);
        unmodifiedField(types.getFirst(), "HEX");
        translate(types);
        String rust = module("a.b");
        contains(rust, "vec!['0', '1']");
        contains(rust, "vec![\"a\".to_string(), \"b\".to_string()]");
        contains(rust, "vec![vec![1, 2], vec![3]]");
        contains(rust, "let b = vec![1, 2];");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String HELD_ITERATOR = """
            package a.b;
            import java.util.Iterator;
            public class Cursor {
                private final Iterator<String> it;
                Cursor(Iterator<String> it) { this.it = it; }
            }
            """;

    /** A java.util.Iterator held as a trait object: Rust's Iterator names its element type as Item. */
    @DisplayName("a kept Iterator<T> is a dyn Iterator<Item = T>: compiles")
    @Test
    public void heldIterator() throws Exception {
        List<TypeInfo> types = parse(HELD_ITERATOR);
        kept(types.getFirst().constructors().getFirst().parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        translate(types);
        String rust = module("a.b");
        contains(rust, "it: &'a mut dyn Iterator<Item = String>,");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String APPENDED_T = """
            package a.b;
            import java.util.List;
            public class Nice<T> {
                public String nice(List<T> list) {
                    StringBuilder sb = new StringBuilder();
                    for (T t : list) sb.append(t);
                    return sb.toString();
                }
            }
            """;

    /** A type parameter appended to a StringBuilder is formatted: {@code T: std::fmt::Display}. */
    @DisplayName("a type parameter appended to a StringBuilder is Display: compiles")
    @Test
    public void appendedTypeParameter() throws Exception {
        List<TypeInfo> types = parse(APPENDED_T);
        borrowed(method(types.getFirst(), "nice").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "std::fmt::Display");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String ITERATED = """
            package a.b;
            import java.util.Iterator;
            import java.util.List;
            public class It<T> {
                int first(List<String> list) {
                    Iterator<String> iterator = list.iterator();
                    String next = iterator.next();
                    return next.length();
                }
                int sum(List<Integer> list) {
                    int s = 0;
                    Iterator<Integer> it = list.iterator();
                    while (it.hasNext()) s += it.next();
                    return s;
                }
            }
            """;

    /** A collection's iterator is a peekable {@code iter()}: {@code hasNext()} peeks, {@code next()} unwraps. */
    @DisplayName("iterator, hasNext and next: compiles")
    @Test
    public void iterated() throws Exception {
        List<TypeInfo> types = parse(ITERATED);
        TypeInfo it = types.getFirst();
        nonModifying(it, "first", "sum");
        borrowed(method(it, "first").parameters().getFirst());
        borrowed(method(it, "sum").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "let mut iterator = list.iter().peekable();");
        contains(rust, "while it.peek().is_some() {");
        contains(rust, "*it.next().unwrap()");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String ENUM_STATE = """
            package a.b;
            public class Sky {
                enum Planet {
                    EARTH(9.8, "earth"), MARS(3.7, "mars");
                    private final double gravity;
                    private final String label;
                    Planet(double g, String label) { this.gravity = g; this.label = label; }
                    double gravity() { return gravity; }
                    String label() { return label; }
                    boolean heavier(Planet other) { return gravity > other.gravity; }
                }
                static double weight(Planet p, double mass) { return mass * p.gravity(); }
                static int labelLength(Planet p) { return p.label().length(); }
            }
            """;

    /** An enum field each constant sets to a literal is a match over the variants. */
    @DisplayName("an enum's constant state is a match over its variants: compiles")
    @Test
    public void enumConstantState() throws Exception {
        List<TypeInfo> types = parse(ENUM_STATE);
        TypeInfo planet = types.getFirst().subTypes().getFirst();
        nonModifying(planet, "gravity", "label", "heavier");
        translate(types);
        String rust = module("a.b");
        contains(rust, "fn gravity_value(self) -> f64 {\nmatch self {\nSelf::Earth => 9.8,\nSelf::Mars => 3.7,");
        contains(rust, "fn label_value(self) -> &'static str {");
        contains(rust, "return self.gravity_value() > other.gravity_value();");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String SHADOWED_T = """
            package a.b;
            import java.util.List;
            public class Box2<T> {
                private final List<T> items;
                public Box2(List<T> items) { this.items = items; }
                static <T> int count(List<T> list) { return list.size(); }
                <T> int size(List<T> other) { return other.size() + items.size(); }
            }
            """;

    /** A method's type parameter named like its class's is renamed: Rust refuses the shadowing (E0403). */
    @DisplayName("a method type parameter shadowing the class's is renamed: compiles")
    @Test
    public void shadowedTypeParameter() throws Exception {
        List<TypeInfo> types = parse(SHADOWED_T);
        TypeInfo box = types.getFirst();
        nonModifying(box, "count", "size");
        borrowed(method(box, "count").parameters().getFirst());
        borrowed(method(box, "size").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "fn count<T2>(list: &Vec<T2>) -> i32 {");
        contains(rust, "fn size<T2>(&self, other: &Vec<T2>) -> i32 {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String OWNED_STRINGS = """
            package a.b;
            import java.util.List;
            import java.util.function.Consumer;
            public class Str {
                private String f;
                void guarded(String s) {
                    String x = s;
                    if (x.isEmpty()) x = "default";
                    this.f = x;
                }
                static void forAll(List<String> xs, Consumer<String> c) { for (String x : xs) c.accept(x); }
                void each(List<String> xs, List<String> out) { forAll(xs, it -> out.add(it)); }
            }
            """;

    /**
     * A string local an assignment gives an owned string owns its first value too; a closure's {@code &str}
     * parameter is made a {@code String} with {@code to_string()}, which {@code clone()} would leave a {@code &str}.
     */
    @DisplayName("a reassigned string local and a &str closure parameter are owned: compiles")
    @Test
    public void ownedStrings() throws Exception {
        List<TypeInfo> types = parse(OWNED_STRINGS);
        TypeInfo str = types.getFirst();
        modifying(str, "guarded");
        nonModifying(str, "forAll", "each");
        borrowed(method(str, "guarded").parameters().getFirst());
        borrowed(method(str, "forAll").parameters().getFirst());
        exclusive(method(str, "forAll").parameters().getLast());
        borrowed(method(str, "each").parameters().getFirst());
        exclusive(method(str, "each").parameters().getLast());
        translate(types);
        String rust = module("a.b");
        contains(rust, "let mut x = s.to_string();");
        contains(rust, "out.push(it.to_string())");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String RETURNED_CLOSURE = """
            package a.b;
            import java.util.function.Function;
            import java.util.function.Supplier;
            public class Fns {
                static Function<String, Integer> length() { return s -> s.length(); }
                static Supplier<String> constant(String c) { return () -> c + "!"; }
            }
            """;

    /** A returned lambda is the {@code Box<dyn Fn…>} the signature declares. */
    @DisplayName("a returned lambda is boxed: compiles")
    @Test
    public void returnedClosure() throws Exception {
        List<TypeInfo> types = parse(RETURNED_CLOSURE);
        // the closure keeps c: owned, as the analysis says of a captured parameter
        translate(types);
        String rust = module("a.b");
        contains(rust, "return Box::new(move |s");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String GENERIC_CALLEE = """
            package a.b;
            import java.util.List;
            import java.util.function.Function;
            public class Gen {
                static <T, R> R first(List<T> list, Function<T, R> f) { return f.apply(list.get(0)); }
                static int firstLength(List<String> list) { return first(list, s -> s.length()); }
            }
            """;

    /** A closure passed to a generic method takes the call's types: {@code &str}, not the callee's {@code &T}. */
    @DisplayName("a closure for a generic callee is typed with the call's types: compiles")
    @Test
    public void closureForGenericCallee() throws Exception {
        List<TypeInfo> types = parse(GENERIC_CALLEE);
        TypeInfo gen = types.getFirst();
        nonModifying(gen, "first", "firstLength");
        borrowed(method(gen, "first").parameters().getFirst());
        borrowed(method(gen, "first").parameters().getLast());
        borrowed(method(gen, "firstLength").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        assertFalse(rust.contains("|s: &T|"), rust);
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String STRING_CONSTANT = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            public class Keys {
                static final String PASSWORD = "password";
                static int size(String value) {
                    Map<String, String> body = new HashMap<>();
                    body.put(PASSWORD, value);
                    return body.get(PASSWORD).length();
                }
            }
            """;

    /** A string constant is a {@code &'static str}: owned where a map keeps it, borrowed as it is for a lookup. */
    @DisplayName("a string constant as a map key: compiles")
    @Test
    public void stringConstantKey() throws Exception {
        List<TypeInfo> types = parse(STRING_CONSTANT);
        borrowed(method(types.getFirst(), "size").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "body.insert(Self::PASSWORD.to_string(), value.to_string());");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String ARRAYS = """
            package a.b;
            public class Arrs {
                static class Slot { int v; }
                static int fill(int n) {
                    Object[] objects = new Object[n];
                    objects[0] = "x";
                    Slot[] slots = new Slot[n];
                    slots[0].v = 3;
                    return objects.length + slots[0].v;
                }
            }
            """;

    /**
     * {@code new T[n]} of a class with a constructor without parameters builds an object per element, where Java has
     * nulls (vec![x; n] would need Clone); an Object array holds the runtime's null, and takes boxed elements.
     */
    @DisplayName("arrays of objects and of a class without Clone: compiles")
    @Test
    public void referenceArrays() throws Exception {
        List<TypeInfo> types = parse(ARRAYS);
        translate(types);
        String rust = module("a.b");
        contains(rust, "(0..n as usize).map(|_| crate::jrt::null()).collect::<Vec<Box<dyn std::any::Any>>>()");
        contains(rust, "objects[0] = Box::new(\"x\".to_string());");
        contains(rust, "let mut slots = (0..n as usize).map(|_| ArrsSlot::new()).collect::<Vec<_>>();");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String RENEWED = """
            package a.b;
            public class Sel {
                public interface Selector { boolean test(String s); }
                static class Any implements Selector { public boolean test(String s) { return true; } }
                static class Wrapper implements Selector {
                    private final Selector inner;
                    Wrapper(Selector inner) { this.inner = inner; }
                    public boolean test(String s) { return !inner.test(s); }
                }
                private boolean flag;
                public Selector filter(Selector s) {
                    if (flag) s = new Wrapper(s);
                    return s;
                }
                public Selector chain() {
                    Selector s = new Any();
                    s = filter(s);
                    return filter(s);
                }
            }
            """;

    /**
     * A parameter the body may replace by a new object and returns is the result new on one path and existing on the
     * other: moved in and returned owned, not a borrow the new object could not be. A class's object where its
     * interface is held, as a local, an assignment, an argument or a result, is boxed.
     */
    @DisplayName("a parameter replaced by a new object and returned is moved in and out; Box<dyn I>: compiles")
    @Test
    public void renewedParameter() throws Exception {
        List<TypeInfo> types = parse(RENEWED);
        TypeInfo sel = types.getFirst();
        nonModifying(sel, "filter", "chain");
        dependentResult(sel, "filter");
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub fn filter(&self, mut s: Box<dyn SelSelector>) -> Box<dyn SelSelector> {");
        // a class's object where its interface is held is boxed
        contains(rust, "s = Box::new(SelWrapper::new(s));");
        contains(rust, "let mut s: Box<dyn SelSelector> = Box::new(SelAny::new());");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String SWITCH_RESULT = """
            package a.b;
            import java.util.List;
            public class Sw {
                List<String> list1;
                List<String> list2;
                public List<String> pick(List<String> list3) {
                    return switch (list3.getFirst()) {
                        case "a" -> null;
                        case "b" -> list1;
                        case "c" -> {
                            if (list3.size() > 1) yield list2;
                            yield list1;
                        }
                        default -> throw new UnsupportedOperationException();
                    };
                }
                public String label(boolean b, String s) { return b ? s : null; }
            }
            """;

    /**
     * A switch expression or a conditional returned: each arm is converted as the result is, a field's Option borrowed,
     * a value in Some, null as None; a yield before the arm's end breaks out of the arm's labeled block.
     */
    @DisplayName("a returned switch expression converts each arm; an early yield is a labeled break: compiles")
    @Test
    public void switchExpressionResult() throws Exception {
        List<TypeInfo> types = parse(SWITCH_RESULT);
        TypeInfo sw = types.getFirst();
        nonModifying(sw, "pick", "label");
        dependentResult(sw, "pick");
        borrowed(method(sw, "pick").parameters().getFirst());
        translate(types, nullable(java.util.Set.of("list1", "list2", "pick()", "label()")));
        String rust = module("a.b");
        contains(rust, "\"a\" => None,");
        contains(rust, "\"b\" => self.list1.as_ref(),");
        contains(rust, "break 'y0 self.list2.as_ref();");
        contains(rust, "self.list1.as_ref()\n");
        contains(rust, "return if b { Some(s) } else { None };");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String STRINGS_AND_CLOSURES = """
            package a.b;
            public class Dec {
                public interface Measure { int of(String s); }
                private Measure measure;
                static String decode(String s) { return s.trim(); }
                public static String norm(String p) {
                    p = decode(p);
                    return p;
                }
                public void setMeasure(Measure measure) { this.measure = measure; }
                public static void use(Dec d) { d.setMeasure(x -> x.length()); }
                private String response;
                public String response() { return java.util.Optional.ofNullable(response).orElse("x"); }
            }
            """;

    /**
     * A string parameter the body assigns is its own {@code String}: a {@code &str} cannot hold the new value. A closure
     * passed for a nullable parameter is in {@code Some}; Optional.ofNullable of a nullable value is that Option.
     */
    @DisplayName("a reassigned string parameter is owned; a closure for a nullable parameter is Some: compiles")
    @Test
    public void reassignedStringNullableClosure() throws Exception {
        List<TypeInfo> types = parse(STRINGS_AND_CLOSURES);
        TypeInfo dec = types.getFirst();
        modifying(dec, "setMeasure");
        nonModifying(dec, "norm", "decode", "use", "response");
        translate(types, nullable(java.util.Set.of("measure", "response")));
        String rust = module("a.b");
        contains(rust, "pub fn norm(mut p: String) -> String {");
        contains(rust, "d.set_measure(Some(Box::new(");
        // Optional.ofNullable of a nullable field is a copy of its Option
        contains(rust, "return self.response.clone().unwrap_or(\"x\".to_string());");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String NULLABLE_OBJECT = """
            package a.b;
            public class Slot {
                private Object value;
                private String name;
                public void set(Object value) { this.value = value; }
                public static Slot of(Slot from, String label) {
                    Slot s = new Slot();
                    s.value = from.name;
                    s.set(label);
                    return s;
                }
                public Object name() { return name; }
            }
            """;

    /**
     * A nullable string where a nullable Object is held: the Option's content boxed, as an
     * {@code Option<Box<dyn Any>>}, assigned, passed and returned.
     */
    @DisplayName("a nullable value held as an Object is boxed inside its Option: compiles")
    @Test
    public void nullableObject() throws Exception {
        List<TypeInfo> types = parse(NULLABLE_OBJECT);
        TypeInfo slot = types.getFirst();
        modifying(slot, "set");
        nonModifying(slot, "of", "name");
        translate(types, nullable(java.util.Set.of("value", "name", "label", "name()")));
        String rust = module("a.b");
        contains(rust, "s.value = from.name.clone().map(|x| Box::new(x) as Box<dyn Any>);");
        contains(rust, ".map(|x| Box::new(x) as Box<dyn Any>));");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String LITERAL_RESULT = """
            package a.b;
            import java.util.List;
            public class Labels {
                public static List<String> labels(List<Integer> in) { return in.stream().map(x -> "s").toList(); }
            }
            """;

    /** A closure returning a string literal returns an owned String, as its declared result is. */
    @DisplayName("a closure's string literal result is owned: compiles")
    @Test
    public void literalClosureResult() throws Exception {
        List<TypeInfo> types = parse(LITERAL_RESULT);
        nonModifying(types.getFirst(), "labels");
        borrowed(method(types.getFirst(), "labels").parameters().getFirst());
        translate(types, RustFacts.STORED);
        String rust = module("a.b");
        contains(rust, "|x| \"s\".to_string()");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String CHECKED_FIELD = """
            package a.b;
            public class Checked {
                private final String text;
                public Checked(String t) {
                    if (t == null) throw new IllegalArgumentException("t");
                    this.text = t;
                }
                public String text() { return text; }
            }
            """;

    /** A nullable parameter checked before it initializes a non-null field: unwrapped, a panic where Java's NPE is. */
    @DisplayName("a nullable value initializing a non-null field is unwrapped: compiles")
    @Test
    public void checkedField() throws Exception {
        List<TypeInfo> types = parse(CHECKED_FIELD);
        nonModifying(types.getFirst(), "text");
        translate(types, nullable(java.util.Set.of("t")));
        String rust = module("a.b");
        contains(rust, "text: t.clone().unwrap(),");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String SUB_LIST = """
            package a.b;
            import java.util.List;
            public class Args {
                static int count(List<String> args) { return args.size(); }
                public static int rest(List<String> args) { return count(args.subList(1, args.size())); }
                public static List<String> head(List<String> args) { return args.subList(0, 2); }
            }
            """;

    /** {@code subList}: a copy of the range, borrowed where it is read, owned where it is returned. */
    @DisplayName("subList is a copy of the slice: compiles")
    @Test
    public void subList() throws Exception {
        List<TypeInfo> types = parse(SUB_LIST);
        TypeInfo args = types.getFirst();
        nonModifying(args, "count", "rest", "head");
        borrowed(method(args, "count").parameters().getFirst());
        borrowed(method(args, "rest").parameters().getFirst());
        borrowed(method(args, "head").parameters().getFirst());
        translate(types, RustFacts.STORED);
        String rust = module("a.b");
        contains(rust, "return Self::count(&args[1..args.len()].to_vec());");
        contains(rust, "return args[0..2].to_vec();");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String FIELD_OF_LOCAL = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Fl {
                static class A {
                    final List<String> list1 = new ArrayList<>();
                }
                public static int fill(String s) {
                    A a = new A();
                    a.list1.add(s);
                    return a.list1.size();
                }
                public static int viaLocal(String s) {
                    A a = new A();
                    List<String> l = a.list1;
                    l.add(s);
                    List<Integer> sorted = new ArrayList<>(List.of(3, 1));
                    java.util.Collections.sort(sorted);
                    return l.size() + sorted.get(0);
                }
                public static void append(StringBuilder sb) {
                    if (sb != null) sb.append("x");
                }
            }
            """;

    /**
     * A local whose object is modified through one of its fields ({@code a.list1.add(s)}), or through a local referring
     * to one, or by a JDK static ({@code Collections.sort}), is a mutable binding; so is a nullable exclusive parameter.
     */
    @DisplayName("a local modified through its field, a local over it or Collections.sort is let mut: compiles")
    @Test
    public void localModifiedThroughField() throws Exception {
        List<TypeInfo> types = parse(FIELD_OF_LOCAL);
        nonModifying(types.getFirst(), "fill", "viaLocal", "append");
        exclusive(method(types.getFirst(), "append").parameters().getFirst());
        translate(types, nullable(java.util.Set.of("sb")));
        String rust = module("a.b");
        contains(rust, "let mut a = FlA::new();");
        // a &mut of a's field, a sorted list, an Option<&mut> reached through as_mut()
        contains(rust, "let mut a = FlA::new();\nlet l = &mut a.list1;");
        contains(rust, "let mut sorted = ");
        contains(rust, "pub fn append(mut sb: Option<&mut String>) {");
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
                public static List<String> first(List<List<String>> l) { return l.get(0); }
                public <T> T id(T t) { return t; }
                public Holder hold(List<String> l) { return new Holder(l); }
                public static Linked orNew(java.util.Optional<Linked> opt) { return opt.orElseGet(Linked::new); }
                static class Holder {
                    private final List<String> held;
                    Holder(List<String> held) { this.held = held; }
                    int size() { return held.size(); }
                }
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
        nonModifying(linked, "id");
        borrowed(method(linked, "id").parameters().getFirst());
        MethodInfo build = method(linked, "build");
        MethodInfo copy = method(linked, "copy");
        MethodInfo first = method(linked, "first");
        MethodInfo view = method(linked, "view");
        MethodInfo id = method(linked, "id");
        MethodInfo hold = method(linked, "hold");
        MethodInfo orNew = method(linked, "orNew");
        TypeInfo holder = linked.findSubType("Holder");
        kept(holder.constructors().getFirst().parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        nonModifying(holder, "size");
        unmodifiedField(holder, "held");
        nonModifying(linked, "hold");
        borrowed(hold.parameters().getFirst());
        io.codelaser.maddi.cst.impl.analysis.LinkFacts links = new io.codelaser.maddi.cst.impl.analysis.LinkFacts() {
            @Override
            public Result result(MethodInfo m) {
                return m == build || m == copy || m == view || m == hold ? Result.NEW
                        : m == first || m == id || m == orNew ? Result.EXISTING : null;
            }

            @Override
            public boolean receiverInResult(MethodInfo m) {
                return m == view;
            }

            @Override
            public java.util.Set<io.codelaser.maddi.cst.api.info.ParameterInfo> keptInResult(MethodInfo m) {
                return m == copy ? java.util.Set.copyOf(copy.parameters())
                        : m == first ? java.util.Set.of(first.parameters().getFirst())
                        : m == id ? java.util.Set.of(id.parameters().getFirst())
                        : m == hold ? java.util.Set.of(hold.parameters().getFirst())
                        : m == orNew ? java.util.Set.of(orNew.parameters().getFirst()) : java.util.Set.of();
            }
        };
        translate(types, RustFacts.of(io.codelaser.maddi.cst.impl.analysis.NullabilityFacts.STORED, RustFacts.STORED,
                links));
        String rust = module("a.b");
        contains(rust, "pub fn build(&self) -> LinkedItems {");
        // a new object holding the receiver's contents that the method does not create itself: a view, borrowed
        contains(rust, "pub fn view(&self) -> &Vec<String> {");
        contains(rust, "pub fn copy(name: String, items: Vec<String>) -> LinkedItems {");
        // the result borrows the parameter, named: with &self, elision would tie it to self
        contains(rust, "pub fn first<'r>(l: &'r Vec<Vec<String>>) -> &'r Vec<String> {");
        contains(rust, "pub fn id<'r, T>(&self, t: &'r T) -> &'r T {");
        // a new struct borrowing what it keeps: the parameter gets its lifetime, not self's
        contains(rust, "pub fn hold<'r>(&self, l: &'r Vec<String>) -> LinkedHolder<'r> {");
        // the Option's content or a new object: never a borrow
        contains(rust, "pub fn or_new(mut opt: Option<Linked>) -> Linked {");
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
    private static final String INTERFACE_GETTERS = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Buf {
                public interface Buffer {
                    List<String> items();
                    String name();
                    default void add(String s) { items().add(s); }
                }
                static class Impl implements Buffer {
                    private final List<String> list = new ArrayList<>();
                    public List<String> items() { return list; }
                    public String name() { return "impl"; }
                }
                public static void reset(Buffer b) { b.items().clear(); }
            }
            """;

    /**
     * An interface's getter that a caller, or a default method, modifies through gets an exclusive twin declared in
     * the trait, and each implementation (a field getter) its own, without visibility in the trait's impl.
     */
    @DisplayName("an interface getter a default method modifies through gets an _mut twin in the trait: compiles")
    @Test
    public void interfaceGetterTwin() throws Exception {
        List<TypeInfo> types = parse(INTERFACE_GETTERS);
        TypeInfo buffer = types.getFirst().findSubType("Buffer");
        TypeInfo impl = types.getFirst().findSubType("Impl");
        nonModifying(buffer, "items", "name");
        modifying(buffer, "add");
        dependentResult(buffer, "items");
        nonModifying(impl, "items", "name");
        dependentResult(impl, "items");
        modified(method(types.getFirst(), "reset").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "fn items_mut(&mut self) -> &mut Vec<String>;");
        contains(rust, "self.items_mut().push(s);");
        contains(rust, "fn items_mut(&mut self) -> &mut Vec<String> {\n&mut self.list\n}");
        contains(rust, "b.items_mut().clear();");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String FN_MUT = """
            package a.b;
            import java.util.function.Function;
            public class Parser {
                private int j;
                int parse(String t) { j = t.length(); return j; }
                static int run(String s, Function<String, Integer> f) { return f.apply(s); }
                static int pure(String s, Function<String, Integer> f) { return f.apply(s); }
                int go(String in) { return run(in, this::parse) + pure(in, x -> x.length()); }
            }
            """;

    /**
     * A function returning a value is a {@code dyn Fn}, unless a call site passes a closure that writes what it
     * captures ({@code this::parse} assigning {@code this.j}): then {@code &mut dyn FnMut}, for every caller.
     */
    @DisplayName("a function parameter passed a closure that writes what it captures is FnMut: compiles")
    @Test
    public void fnMutFromCallSites() throws Exception {
        List<TypeInfo> types = parse(FN_MUT);
        TypeInfo parser = types.getFirst();
        modifying(parser, "parse", "go");
        borrowed(method(parser, "parse").parameters().getFirst());
        borrowed(method(parser, "run").parameters().getFirst());
        borrowed(method(parser, "pure").parameters().getFirst());
        modified(method(parser, "run").parameters().get(1));
        modified(method(parser, "pure").parameters().get(1));
        borrowed(method(parser, "go").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub fn run(s: &str, f: &mut dyn FnMut(&str) -> i32) -> i32 {");
        contains(rust, "pub fn pure(s: &str, f: &dyn Fn(&str) -> i32) -> i32 {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String FLUENT_LOCAL = """
            package a.b;
            public class Built {
                static class Builder {
                    int j;
                    int k;
                    Builder setJ(int j) { this.j = j; return this; }
                    Builder setK(int k) { this.k = k; return this; }
                    int sum() { return j + k; }
                }
                static int make(int j) {
                    Builder b = new Builder().setJ(j).setK(4);
                    return b.sum();
                }
            }
            """;

    /** A fluent chain on a new object, kept in a local, binds the object first: the chain borrows a temporary. */
    @DisplayName("a local initialized by a fluent chain on a new object binds the object, then chains: compiles")
    @Test
    public void fluentChainOnNewLocal() throws Exception {
        List<TypeInfo> types = parse(FLUENT_LOCAL);
        TypeInfo builder = types.getFirst().findSubType("Builder");
        modifying(builder, "setJ", "setK");
        fluent(builder, "setJ");
        fluent(builder, "setK");
        nonModifying(builder, "sum");
        translate(types);
        String rust = module("a.b");
        contains(rust, "let mut b = BuiltBuilder::new();\nb.set_j(j).set_k(4);");
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
                private static final List<String> ALL = new ArrayList<>();
                static void register(String s) { ALL.add(s); }
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
        // final, but its list is modified
        contains(rust, "static mut COUNTER_ALL: Vec<String> = Vec::new();");
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
