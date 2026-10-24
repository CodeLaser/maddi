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
            }
            """;

    /**
     * Row 8: hidden content is a type parameter without bounds: the type can store it, hand it out and replace it,
     * never look inside. The parameters that keep it are independent with hidden content (moved in), and the result
     * that is the hidden content shares it: a borrow.
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
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub struct Box<T> {\nt: T,\n}");
        contains(rust, "pub fn get(&self) -> &T {");
        contains(rust, "pub fn set(&mut self, t: T) {");
        assertFalse(rust.contains("T: "), rust);
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
}
