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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;

/**
 * The verdicts as Rust's ownership modes (CodeLaser/maddi#167), and rustc as their judge: correct verdicts compile;
 * a wrong one is an error at the line that contradicts it.
 */
public class TestJavaToRustOwnership extends CommonJavaToRust {

    @Language("java")
    private static final String INVENTORY = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Inventory {
                private final List<String> names = new ArrayList<>();
                private int count;

                public void add(String name) {
                    names.add(name);
                    count++;
                }

                public int count() {
                    return count;
                }

                public List<String> names() {
                    return names;
                }

                public boolean contains(String name) {
                    return names.contains(name);
                }

                public static int total(List<Inventory> list) {
                    int sum = 0;
                    for (Inventory i : list) sum += i.count();
                    return sum;
                }
            }
            """;

    /** The verdicts the analysis computes for {@link #INVENTORY}. */
    private static TypeInfo inventory(List<TypeInfo> types) {
        TypeInfo inventory = types.getFirst();
        modifying(inventory, "add");
        nonModifying(inventory, "count", "names", "contains");
        // 'name' becomes an element of the list: kept, with hidden content
        kept(method(inventory, "add").parameters().getFirst(), ValueImpl.IndependentImpl.INDEPENDENT_HC);
        borrowed(method(inventory, "contains").parameters().getFirst());
        borrowed(method(inventory, "total").parameters().getFirst());
        dependentResult(inventory, "names");
        independentResult(inventory, "count");
        return inventory;
    }

    @DisplayName("correct verdicts compile")
    @Test
    public void correctVerdictsCompile() throws Exception {
        List<TypeInfo> types = parse(INVENTORY);
        inventory(types);
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "pub fn add(&mut self, name: String) {");
        contains(rust, "pub fn count(&self) -> i32 {");
        contains(rust, "pub fn names(&self) -> &Vec<String> {");
        contains(rust, "pub fn contains(&self, name: &str) -> bool {");
        contains(rust, "pub fn total(list: &Vec<Inventory>) -> i32 {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @DisplayName("a modifying method the verdicts call non-modifying: rustc's E0596 at the modification")
    @Test
    public void wrongModificationVerdict() throws Exception {
        List<TypeInfo> types = parse(INVENTORY);
        TypeInfo inventory = types.getFirst();
        // the deliberate mistake: 'add' modifies 'names' and 'count'
        nonModifying(inventory, "add", "count", "names", "contains");
        kept(method(inventory, "add").parameters().getFirst(), ValueImpl.IndependentImpl.INDEPENDENT_HC);
        borrowed(method(inventory, "contains").parameters().getFirst());
        borrowed(method(inventory, "total").parameters().getFirst());
        dependentResult(inventory, "names");
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub fn add(&self, name: String) {");
        List<String> errors = rustcErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("E0596")), () -> String.join("\n", errors) + "\n" + rust);
    }

    @Language("java")
    private static final String HOLDER = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Holder {
                private final List<String> list;

                public Holder(List<String> list) {
                    this.list = list;
                }

                public int size() {
                    return list.size();
                }

                public static int shared() {
                    List<String> l = new ArrayList<>();
                    Holder h = new Holder(l);
                    l.add("x");
                    return h.size();
                }
            }
            """;

    /**
     * The constructor keeps the caller's list (dependent), and the caller goes on modifying it: Java's aliasing plus
     * modification, the case that stops a type at {@code @FinalFields}. The dependent field is a borrow with a
     * lifetime, so Rust refuses exactly that: the caller's modification while the holder lives is rustc's E0502. The
     * verdicts are right; Java's program needs another representation in Rust ({@code Rc<RefCell<…>>}).
     */
    private static final String SUB_HOLDER = """
            package a.b;
            import java.util.List;
            class Holder<T> {
                private final List<T> list;
                Holder(List<T> list) { this.list = list; }
                int size() { return list.size(); }
            }
            class Counted extends Holder<String> {
                private int count;
                Counted(List<String> list) { super(list); }
            }
            """;

    /**
     * A subclass outside any hierarchy (a generic superclass is no closed hierarchy's root) is its superclass as a
     * {@code base} field: when the superclass borrows, so does
     * the subclass, and it declares the lifetime its base field needs (E0106 otherwise, which removed fernflower's
     * test fixture and with it every test built on it).
     */
    @DisplayName("a subclass whose base borrows declares the lifetime")
    @Test
    public void baseWithLifetime() throws Exception {
        List<TypeInfo> types = parse(SUB_HOLDER);
        TypeInfo holder = types.stream().filter(t -> t.simpleName().equals("Holder")).findFirst().orElseThrow();
        nonModifying(holder, "size");
        unmodifiedField(holder, "list");
        kept(holder.constructors().getFirst().parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        translate(types);
        String rust = module("a.b");
        contains(rust, "struct Holder<'a, T> {");
        contains(rust, "struct Counted<'a> {\n    pub base: Holder<'a, String>,");
        List<String> errors = rustcErrors();
        assertTrue(errors.stream().noneMatch(e -> e.contains("E0106") || e.contains("E0261")),
                () -> String.join("\n", errors) + "\n" + rust);
    }

    @DisplayName("a dependent field is a borrow; the caller's modification while it lives is rustc's E0502")
    @Test
    public void dependentFieldIsBorrowed() throws Exception {
        List<TypeInfo> types = parse(HOLDER);
        TypeInfo holder = types.getFirst();
        nonModifying(holder, "size");
        unmodifiedField(holder, "list");
        MethodInfo constructor = holder.constructors().getFirst();
        kept(constructor.parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub struct Holder<'a> {\nlist: &'a Vec<String>,\n}");
        contains(rust, "pub fn new(list: &'a Vec<String>) -> Self {");
        contains(rust, "let h = Holder::new(&l);");
        List<String> errors = rustcErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("E0502")), () -> String.join("\n", errors) + "\n" + rust);
    }

    @Language("java")
    private static final String NULLABLE_IMMUTABLE_LOCAL = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Stack {
                public static final class Kind {
                    private final int code;
                    public Kind(int code) { this.code = code; }
                    public int code() { return code; }
                }
                private final List<Kind> items = new ArrayList<>();
                public void pushIf(boolean b, int code) {
                    Kind k = null;
                    if (b) k = new Kind(code);
                    if (k != null) {
                        items.add(k);
                        items.add(k);
                    }
                }
            }
            """;

    @DisplayName("an owned Option of an immutable value is cloned whole, not with Option<&T>'s cloned()")
    @Test
    public void nullableImmutableLocal() throws Exception {
        List<TypeInfo> types = parse(NULLABLE_IMMUTABLE_LOCAL);
        TypeInfo stack = types.getFirst();
        TypeInfo kind = stack.findSubType("Kind");
        immutable(kind, ValueImpl.ImmutableImpl.IMMUTABLE);
        nonModifying(kind, "code");
        modifying(stack, "pushIf");
        translate(types);
        String rust = module("a.b");
        // was 'k.cloned().unwrap()': no method cloned on Option<StackKind> (E0599; fernflower's InstructionImpact)
        contains(rust, "k.clone().unwrap()");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String NULLABLE_ARRAY = """
            package a.b;
            public class Instr {
                private final int[] operands;
                public Instr(int[] operands) { this.operands = operands; }
                public int count() { return operands == null ? 0 : operands.length; }
                public int operand(int i) { return operands[i]; }
            }
            """;

    @DisplayName("a nullable array's length and elements are reached through unwrap, as Java's access would throw")
    @Test
    public void nullableArray() throws Exception {
        List<TypeInfo> types = parse(NULLABLE_ARRAY);
        TypeInfo instr = types.getFirst();
        nonModifying(instr, "count", "operand");
        translate(types, nullable(java.util.Set.of("operands")));
        String rust = module("a.b");
        // was 'self.operands[i as usize]' and 'self.operands.len()' on an Option<Vec<i32>> (E0608, E0599; fernflower)
        contains(rust, "self.operands.as_ref().unwrap()[i as usize]");
        contains(rust, "self.operands.as_ref().unwrap().len() as i32");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String LIFETIME_CYCLE = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Cyc {
                public static class Holder {
                    private final List<String> names;
                    public Holder(List<String> names) { this.names = names; }
                    public int n() { return names.size(); }
                }
                public static class A {
                    private final List<B> bs = new ArrayList<>();
                    private Holder h;
                }
                public static class B {
                    private A a;
                }
            }
            """;

    @DisplayName("a struct on a cycle with a struct carrying the lifetime carries it too, whichever is asked first")
    @Test
    public void lifetimeThroughCycle() throws Exception {
        List<TypeInfo> types = parse(LIFETIME_CYCLE);
        TypeInfo holder = types.getFirst().findSubType("Holder");
        nonModifying(holder, "n");
        unmodifiedField(holder, "names");
        kept(holder.constructors().getFirst().parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        translate(types, nullable(java.util.Set.of("h", "a")));
        String rust = module("a.b");
        // was 'pub struct CycB {' with 'a: Option<Box<CycA<'_>>>': E0106, B decided without a lifetime while A was
        // in progress (fernflower's StatEdge and Statement)
        contains(rust, "pub struct CycB<'a> {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String GETTER_TO_MODIFIED = """
            package a.b;
            public class Gs {
                public static class Edge {
                    private int n;
                    public void bump() { n++; }
                }
                public static class Stat {
                    private final Edge ifEdge = new Edge();
                    public Edge getIfEdge() { return ifEdge; }
                }
                public static void touch(Edge e) { e.bump(); }
                public static void run(Stat s) { touch(s.getIfEdge()); }
            }
            """;

    @DisplayName("a getter's result handed to a parameter the callee modifies: the getter's exclusive twin")
    @Test
    public void getterToModifiedParameter() throws Exception {
        List<TypeInfo> types = parse(GETTER_TO_MODIFIED);
        TypeInfo gs = types.getFirst();
        modifying(gs.findSubType("Edge"), "bump");
        nonModifying(gs.findSubType("Stat"), "getIfEdge");
        dependentResult(gs.findSubType("Stat"), "getIfEdge");
        modifying(gs, "touch", "run");
        exclusive(method(gs, "touch").parameters().getFirst());
        exclusive(method(gs, "run").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        // was 'Self::touch(s.get_if_edge())': a & where the parameter is &mut (E0308; fernflower's removeSuccessor)
        contains(rust, "Self::touch(s.get_if_edge_mut());");
        contains(rust, "pub fn get_if_edge_mut(&mut self) -> &mut GsEdge {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String RECEIVER_INTO_PARAMETER = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            public class Node {
                public static class Engine {
                    private final Map<String, String> variables = new HashMap<>();
                    public boolean checkAndSet(String name, String value) {
                        String old = variables.get(name);
                        if (old != null) return old.equals(value);
                        variables.put(name, value);
                        return true;
                    }
                }
                private final String value = "v";
                public boolean match(String name, Engine engine) { return engine.checkAndSet(name, value); }
                public static boolean run(Node node) {
                    Engine engine = new Engine();
                    return node.match("x", engine) && node.match("y", engine);
                }
            }
            """;

    /**
     * CodeLaser/maddi#202, pattern 4 (fernflower's Exprent.match(MatchNode, MatchEngine)): the parameter is dependent
     * because the receiver's content goes INTO it, and modified; the method does not keep it. Moved in, the caller
     * would lose the engine after the first match: it is a {@code &mut}.
     */
    @DisplayName("a modified parameter that receives the receiver's content, not kept, is &mut")
    @Test
    public void receiverIntoParameterIsExclusive() throws Exception {
        List<TypeInfo> types = parse(RECEIVER_INTO_PARAMETER);
        TypeInfo node = types.getFirst();
        TypeInfo engine = node.findSubType("Engine");
        modifying(engine, "checkAndSet");
        MethodInfo checkAndSet = method(engine, "checkAndSet");
        borrowed(checkAndSet.parameters().getFirst());
        kept(checkAndSet.parameters().get(1), ValueImpl.IndependentImpl.DEPENDENT);
        nonModifying(node, "match", "run");
        MethodInfo match = method(node, "match");
        borrowed(match.parameters().getFirst());
        match.parameters().get(1).analysis().set(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE);
        match.parameters().get(1).analysis().set(PropertyImpl.INDEPENDENT_PARAMETER,
                ValueImpl.IndependentImpl.DEPENDENT);
        borrowed(method(node, "run").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "engine: &mut NodeEngine");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String STATIC_WITH_LIFETIME = """
            package a.b;
            import java.util.List;
            public class Attr {
                public static final Key<Attr> CODE = new Key<>("Code");
                public static class Key<T extends Attr> {
                    public final String name;
                    public Key(String name) { this.name = name; }
                }
                private final List<String> list;
                public Attr(List<String> list) { this.list = list; }
                public int size() { return list.size(); }
                public static boolean isCode(String name) { return CODE.name.equals(name); }
            }
            """;

    /**
     * CodeLaser/maddi#202 (fernflower's StructGeneralAttribute.ATTRIBUTE_* keys): a static's type mentioned its owner's
     * lifetime, which is not in scope at module level (E0261). The item, and every function reading it, was lost.
     */
    @DisplayName("a static whose type has a lifetime holds it for 'static")
    @Test
    public void staticWithLifetime() throws Exception {
        List<TypeInfo> types = parse(STATIC_WITH_LIFETIME);
        TypeInfo attr = types.getFirst();
        nonModifying(attr, "size", "isCode");
        unmodifiedField(attr, "list");
        unmodifiedField(attr, "CODE");
        kept(attr.constructors().getFirst().parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        borrowed(method(attr, "isCode").parameters().getFirst());
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub struct Attr<'a> {");
        contains(rust, "static ATTR_CODE: ");
        assertFalse(rust.matches("(?s).*static [A-Z_]+: [^=\\n]*'a.*"), rust);
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String UPCAST_TO_BASE = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class Console<T> {
                private final List<T> sources = new ArrayList<>();
                void addSource(T t) { sources.add(t); }
                int count() { return sources.size(); }
            }
            class TestConsole extends Console<String> {
                private int closed;
                void close() { closed++; }
            }
            class Fixture {
                private TestConsole decompiler;
                void setUp() { decompiler = new TestConsole(); }
                Console<String> getDecompiler() { return decompiler; }
                int run(String name) {
                    getDecompiler().addSource(name);
                    decompiler.addSource(name);
                    return decompiler.count();
                }
            }
            """;

    /**
     * CodeLaser/maddi#203 (fernflower's DecompilerTestFixture.getDecompiler, a TestConsoleDecompiler field returned as
     * the ConsoleDecompiler it extends): a composed subclass Derefs to its base, so Java's implicit upcast is Rust's
     * deref coercion, an Option's content through as_deref, and a call of the base's method on the subclass an auto-deref.
     */
    @DisplayName("a composed subclass returned or used as its superclass: Deref to the base")
    @Test
    public void upcastToBase() throws Exception {
        List<TypeInfo> types = parse(UPCAST_TO_BASE);
        TypeInfo console = types.stream().filter(t -> t.simpleName().equals("Console")).findFirst().orElseThrow();
        TypeInfo test = types.stream().filter(t -> t.simpleName().equals("TestConsole")).findFirst().orElseThrow();
        TypeInfo fixture = types.stream().filter(t -> t.simpleName().equals("Fixture")).findFirst().orElseThrow();
        modifying(console, "addSource");
        nonModifying(console, "count");
        kept(method(console, "addSource").parameters().getFirst(), ValueImpl.IndependentImpl.INDEPENDENT_HC);
        modifying(test, "close");
        modifying(fixture, "setUp", "run");
        nonModifying(fixture, "getDecompiler");
        dependentResult(fixture, "getDecompiler");
        borrowed(method(fixture, "run").parameters().getFirst());
        translate(types, nullable(Set.of("decompiler", "getDecompiler()")));
        String rust = module("a.b");
        contains(rust, "impl std::ops::Deref for TestConsole {");
        contains(rust, "self.decompiler.as_deref()");
        assertEquals(List.of(), rustcErrors(), rust);
    }
}
