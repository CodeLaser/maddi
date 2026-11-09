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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;

/** The shapes of a Java program as Rust items: trait, record, enum, constants, nullable state, the collections. */
public class TestJavaToRustStructure extends CommonJavaToRust {

    @Language("java")
    private static final String SHAPES = """
            package a.b;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            public class Shapes {
                public interface Shape {
                    double area();
                    String name();
                }

                public record Circle(double r) implements Shape {
                    public double area() {
                        return Math.PI * r * r;
                    }

                    public String name() {
                        return "circle";
                    }
                }

                public enum Color { RED, DARK_GREEN }

                public static final int MAX = 10;
                private final Map<String, Integer> counts = new HashMap<>();
                private String last;

                public void count(String key) {
                    Integer c = counts.get(key);
                    if (c == null) {
                        counts.put(key, 1);
                    } else {
                        counts.put(key, c + 1);
                    }
                    last = key;
                }

                public String last() {
                    return last;
                }

                public int longWords(List<String> words) {
                    int n = 0;
                    for (int i = 0; i < words.size(); i++) {
                        if (words.get(i).length() > 3) n++;
                    }
                    return n;
                }

                public String label(int i) {
                    return i > MAX ? "big" : "small " + i;
                }

                public static double total(List<Shape> shapes) {
                    double sum = 0;
                    for (Shape s : shapes) sum += s.area();
                    return sum;
                }
            }
            """;

    @DisplayName("trait, record implementing it, plain enum, constant, nullable field, map, ternary: compiles")
    @Test
    public void shapes() throws Exception {
        List<TypeInfo> types = parse(SHAPES);
        TypeInfo shapes = types.getFirst();
        TypeInfo shape = shapes.findSubType("Shape");
        TypeInfo circle = shapes.findSubType("Circle");
        nonModifying(shape, "area", "name");
        nonModifying(circle, "area", "name");
        modifying(shapes, "count");
        nonModifying(shapes, "last", "longWords", "label");
        kept(method(shapes, "count").parameters().getFirst(), ValueImpl.IndependentImpl.INDEPENDENT_HC);
        borrowed(method(shapes, "longWords").parameters().getFirst());
        borrowed(method(shapes, "total").parameters().getFirst());
        independentResult(shapes, "last");

        RustTranslator.Result result = translate(types, nullable(Set.of("last", "last()")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, """
                pub trait ShapesShape {
                    fn area(&self) -> f64;
                    fn name(&self) -> String;
                }""");
        contains(rust, """
                #[derive(Debug, Clone, PartialEq)]
                pub struct ShapesCircle {
                    pub r: f64,
                }""");
        contains(rust, "impl ShapesShape for ShapesCircle {");
        contains(rust, """
                pub enum ShapesColor {
                    Red,
                    DarkGreen,
                }""");
        contains(rust, "pub const MAX: i32 = 10;");
        contains(rust, "last: Option<String>,");
        contains(rust, "pub fn last(&self) -> Option<String> {");
        contains(rust, "for i in 0..words.len() as i32 {");
        contains(rust, "pub fn total(shapes: &Vec<Box<dyn ShapesShape>>) -> f64 {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String STOCK = """
            package a.b;
            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            public class Stock {
                public record Item(String name, int count) {}

                private final Map<String, Item> items = new HashMap<>();

                public void add(String name, int count) {
                    Item old = items.get(name);
                    int total = old == null ? count : old.count() + count;
                    items.put(name, new Item(name, total));
                }

                public Item find(String name) {
                    return items.get(name);
                }

                public List<String> names() {
                    return new ArrayList<>(items.keySet());
                }
            }
            """;

    /** The end-to-end demo of #167: a record's accessors, its borrowed string, a copied record, collected keys. */
    @DisplayName("record accessors and constructor, an @Immutable record copied out of a map, a map's keys collected")
    @Test
    public void recordsInAMap() throws Exception {
        List<TypeInfo> types = stock(ValueImpl.ImmutableImpl.IMMUTABLE);
        String rust = module("a.b");
        contains(rust, "pub fn new(name: &str, count: i32) -> Self {");
        contains(rust, "name: name.to_string()");
        contains(rust, "pub fn count(&self) -> i32 {");
        contains(rust, "return self.items.get(name).cloned();");
        contains(rust, "return self.items.keys().cloned().collect::<Vec<String>>();");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    /**
     * With hidden content, a copy would duplicate what Java shares: the record is held as an {@code Rc}, whose clone
     * is a share. The map holds {@code Rc}s, and {@code find} hands out another reference to the same object.
     */
    @DisplayName("an @Immutable(hc=true) record is shared as an Rc: the result is a clone of the Rc: compiles")
    @Test
    public void recordWithHiddenContentIsShared() throws Exception {
        stock(ValueImpl.ImmutableImpl.IMMUTABLE_HC);
        String rust = module("a.b");
        contains(rust, "items: HashMap<String, Rc<StockItem>>,");
        contains(rust, "pub fn find(&self, name: &str) -> Option<Rc<StockItem>> {");
        contains(rust, "return self.items.get(name).cloned();");
        contains(rust, "self.items.insert(name.to_string(), Rc::new(StockItem::new(name, total)));");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    private List<TypeInfo> stock(Value.Immutable itemImmutable) throws Exception {
        List<TypeInfo> types = parse(STOCK);
        TypeInfo stock = types.getFirst();
        TypeInfo item = stock.findSubType("Item");
        modifying(stock, "add");
        nonModifying(stock, "find", "names");
        borrowed(method(stock, "add").parameters().getFirst());
        borrowed(method(stock, "find").parameters().getFirst());
        independentResult(stock, "find");
        independentResult(stock, "names");
        borrowed(item.constructors().getFirst().parameters().getFirst());
        immutable(item, itemImmutable);

        RustTranslator.Result result = translate(types, nullable(Set.of("old", "find()")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        return types;
    }

    @Language("java")
    private static final String CONTROLLERS = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Controllers {
                public interface Validatable {
                    boolean isValid();
                }

                public static class Base {
                    private final List<String> log = new ArrayList<>();
                    protected final String name;

                    public Base(String name) {
                        this.name = name;
                    }

                    public void audit(String what) {
                        log.add(name + ": " + what);
                    }

                    public int logSize() {
                        return log.size();
                    }
                }

                public static class Orders extends Base implements Validatable {
                    private int count;

                    public Orders() {
                        super("orders");
                    }

                    public void place() {
                        count++;
                        audit("placed");
                    }

                    public boolean isValid() {
                        return count >= 0 && name.length() > 0;
                    }
                }

                public static int sizeOf(Orders orders) {
                    return orders.logSize();
                }
            }
            """;

    /**
     * A class and its subclass are one struct with a kind (RustHierarchies, TestJavaToRustHierarchy): an inherited
     * member is the struct's own, the subclass's field is in its variant's data. The interface method has no verdict,
     * so its trait takes {@code &mut self}; the implementation's own {@code &self} is an inherent method the trait's
     * delegates to.
     */
    @DisplayName("a closed hierarchy as one struct, trait impl delegating to the implementation's own verdicts: compiles")
    @Test
    public void inheritance() throws Exception {
        List<TypeInfo> types = parse(CONTROLLERS);
        TypeInfo controllers = types.getFirst();
        TypeInfo base = controllers.findSubType("Base");
        TypeInfo orders = controllers.findSubType("Orders");
        modifying(base, "audit");
        nonModifying(base, "logSize");
        borrowed(method(base, "audit").parameters().getFirst());
        modifying(orders, "place");
        nonModifying(orders, "isValid");
        borrowed(method(controllers, "sizeOf").parameters().getFirst());

        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "ControllersOrders(ControllersOrdersData),");
        contains(rust, "let mut this = Self::new_controllers_base(\"orders\".to_string());");
        contains(rust, "self.kind.controllers_orders_mut().count += 1;");
        contains(rust, "self.audit(\"placed\");");
        contains(rust, "(self.name.len() as i32 > 0)");
        contains(rust, "return orders.log_size();");
        contains(rust, """
                fn is_valid(&mut self) -> bool {
                    Self::is_valid(self)
                }""");
        contains(rust, "pub fn is_valid(&self) -> bool {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String SHARED_BASE = """
            package a.b;
            import java.util.List;
            public class Stmts {
                static class Statement<T> {
                    private final List<T> labels;
                    Statement(List<T> labels) { this.labels = labels; }
                    int count() { return labels.size(); }
                }
                static class Switch extends Statement<String> {
                    Switch() { super(List.of()); }
                }
            }
            """;

    /**
     * A superclass whose objects are shared in an {@code Rc} is still owned as the {@code base} part. A generic
     * superclass keeps composition: a closed hierarchy is one struct only without type parameters.
     */
    @DisplayName("a base whose class is shared in an Rc is the plain struct: compiles")
    @Test
    public void sharedBase() throws Exception {
        List<TypeInfo> types = parse(SHARED_BASE);
        TypeInfo statement = types.getFirst().findSubType("Statement");
        immutable(statement, ValueImpl.ImmutableImpl.IMMUTABLE_HC);
        nonModifying(statement, "count");
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub base: StmtsStatement<String>,");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String HASHED_RECORD = """
            package a.b;
            import java.util.List;
            import java.util.Set;
            public class Recs {
                record R<T>(Set<T> s, List<T> l) {}
                static <T> boolean same(R<T> a, R<T> b) { return a.equals(b); }
            }
            """;

    /** A generic record with a set: a field-wise PartialEq, bounded as its impl is; derive would bound T by PartialEq. */
    @DisplayName("a generic record with a HashSet field compares field-wise: compiles")
    @Test
    public void hashedRecord() throws Exception {
        List<TypeInfo> types = parse(HASHED_RECORD);
        TypeInfo recs = types.getFirst();
        nonModifying(recs, "same");
        method(recs, "same").parameters().forEach(CommonJavaToRust::borrowed);
        translate(types);
        String rust = module("a.b");
        contains(rust, "PartialEq for RecsR<T> {\nfn eq(&self, other: &Self) -> bool {\nself.s == other.s && self.l == other.l");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String NESTED_RECORD = """
            package a.b;
            public class Nest {
                record S(String c) {}
                record R(S b) {}
            }
            """;

    /** The facts call the canonical constructor's record parameter independent: borrowed, and cloned into the field. */
    @DisplayName("a record kept by value in a record, from a borrowed parameter, is cloned: compiles")
    @Test
    public void nestedRecord() throws Exception {
        List<TypeInfo> types = parse(NESTED_RECORD);
        TypeInfo r = types.getFirst().findSubType("R");
        r.constructors().getFirst().parameters().forEach(CommonJavaToRust::borrowed);
        translate(types);
        String rust = module("a.b");
        contains(rust, "pub fn new(b: &NestS) -> Self {\nSelf { b: b.clone() }");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String OPAQUE = """
            package a.b;
            import java.net.URI;
            public class Holder {
                public record Link(String name, URI uri) { }
                private final Throwable cause;
                private final Class<?> kind;
                private int hits;
                public Holder(Throwable cause, Class<?> kind) {
                    this.cause = cause;
                    this.kind = kind;
                }
                public Throwable cause() { return cause; }
                public void hit() { hits++; }
                public int hits() { return hits; }
            }
            """;

    @DisplayName("an external type without a Rust counterpart is an opaque stand-in: what holds it still compiles")
    @Test
    public void opaqueExternalTypes() throws Exception {
        List<TypeInfo> types = parse(OPAQUE);
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        // was 'URI', 'Throwable', 'Class<?>' as written: no such Rust type, and the struct went with them (E0412)
        contains(rust, "pub uri: crate::jrt::URI,");
        contains(rust, "cause: crate::jrt::Throwable,");
        contains(rust, "kind: crate::jrt::Class,");
        String jrt = module("jrt");
        contains(jrt, "/// `java.net.URI`, not translated.");
        contains(jrt, "pub struct Throwable;");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String GENERIC_METHOD = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Registry {
                public interface Listener {
                    void onEvent(String event);
                    <T> T convert(T t);
                }
                public static class Printer implements Listener {
                    private final List<String> seen = new ArrayList<>();
                    public void onEvent(String event) { seen.add(event); }
                    public <T> T convert(T t) { return t; }
                }
                private final List<Listener> listeners = new ArrayList<>();
                public void add(Listener l) { listeners.add(l); }
                public void fire(String event) { for (Listener l : listeners) l.onEvent(event); }
            }
            """;

    @DisplayName("an interface with a generic method is still a trait object: the method is kept out of the vtable")
    @Test
    public void genericMethodInTrait() throws Exception {
        List<TypeInfo> types = parse(GENERIC_METHOD);
        TypeInfo registry = types.getFirst();
        TypeInfo listener = registry.findSubType("Listener");
        TypeInfo printer = registry.findSubType("Printer");
        modifying(listener, "onEvent");
        nonModifying(listener, "convert");
        modifying(printer, "onEvent");
        nonModifying(printer, "convert");
        modifying(registry, "add", "fire");
        kept(method(registry, "add").parameters().getFirst(), ValueImpl.IndependentImpl.INDEPENDENT_HC);
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        // was 'fn convert<T>(&self, t: T) -> T;' alone: Box<dyn RegistryListener> was E0038, the struct removed
        contains(rust, "where Self: Sized;");
        contains(rust, "listeners: Vec<Box<dyn RegistryListener>>,");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String INTERFACE_CONSTANTS = """
            package a.b;
            public class Codes {
                public interface CodeConstants {
                    int TYPE_INT = 1;
                    int TYPE_LONG = 2;
                    String NAME = "codes";
                }
                public static class Reader implements CodeConstants {
                    public int width(int type) {
                        switch (type) {
                            case TYPE_INT: return 4;
                            case CodeConstants.TYPE_LONG: return 8;
                            default: return 0;
                        }
                    }
                    public String name() { return NAME + TYPE_INT; }
                }
            }
            """;

    @DisplayName("an interface's constants are module-level consts, also as match patterns")
    @Test
    public void interfaceConstants() throws Exception {
        List<TypeInfo> types = parse(INTERFACE_CONSTANTS);
        TypeInfo reader = types.getFirst().findSubType("Reader");
        nonModifying(reader, "width", "name");
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        // was a 'static mut' never declared: unresolved, and in a match an undeclared name binds (E0408)
        contains(rust, "pub const CODES_CODE_CONSTANTS_TYPE_INT: i32 = 1;");
        contains(rust, "pub const CODES_CODE_CONSTANTS_NAME: &'static str = \"codes\";");
        contains(rust, "CODES_CODE_CONSTANTS_TYPE_LONG => {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String COLLECTION_SUBCLASS = """
            package a.b;
            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.List;
            public class Coll {
                public static class Named<E> extends ArrayList<E> {
                    private final HashMap<String, Integer> index = new HashMap<>();
                    public Named() { super(); }
                    public void addNamed(String name, E e) { index.put(name, size()); super.add(e); }
                    public boolean has(String name) { return index.containsKey(name) && !isEmpty(); }
                    @Override public boolean add(E e) { index.put("", size()); return super.add(e); }
                }
                public static class User {
                    public int count(Named<String> n) { int c = 0; for (String s : n) if (!s.isEmpty()) c++; return c + n.size(); }
                    public String first(Named<String> n) { return n.get(0); }
                    public int all(List<String> l) { return l.size(); }
                    public int viaList(Named<String> n) { return all(n); }
                    public int blocks(Named<Block> n) { int c = 0; for (Block b : n) c += b.id; return c + n.size(); }
                }
                // modified elsewhere, so no derived Clone: the Deref must not ask for one
                public static class Block { int id; void bump() { id++; } }
            }
            """;

    @DisplayName("a class extending a JDK collection keeps the collection as its base, and dereferences to it")
    @Test
    public void collectionSubclass() throws Exception {
        List<TypeInfo> types = parse(COLLECTION_SUBCLASS);
        TypeInfo named = types.getFirst().findSubType("Named");
        TypeInfo user = types.getFirst().findSubType("User");
        modifying(named, "addNamed", "add");
        nonModifying(named, "has");
        nonModifying(user, "count", "first", "all", "viaList");
        modifying(types.getFirst().findSubType("Block"), "bump");
        nonModifying(user, "blocks");
        for (String m : new String[]{"count", "first", "all", "viaList", "blocks"}) {
            borrowed(method(user, m).parameters().getFirst());
        }
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        // was a struct without the list: len, push, iter and indexing unresolved (fernflower's VBStyleCollection)
        contains(rust, "pub base: Vec<E>,");
        contains(rust, "type Target = Vec<E>;");
        contains(rust, "impl<E> std::ops::Deref for CollNamed<E> {");
        // super.add is the list's push, not the override's own add; its value Java's true
        contains(rust, "return { self.base.push(e); true };");
        contains(rust, "return self.all(n);");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String ENUM_WITH_MASK = """
            package a.b;
            public class Edges {
                public enum EdgeType {
                    REGULAR(1), BREAK(4);
                    private final int mask;
                    EdgeType(int mask) { this.mask = mask; }
                    public int mask() { return mask; }
                }
                public static class Edge {
                    private final EdgeType type;
                    public Edge(EdgeType type) { this.type = type; }
                    public EdgeType getType() { return type; }
                    public boolean isBreak() { return getType() == EdgeType.BREAK; }
                }
            }
            """;

    @DisplayName("an enum with constant state is Copy: its getter returns the value, == compares values")
    @Test
    public void enumWithConstantStateIsCopy() throws Exception {
        List<TypeInfo> types = parse(ENUM_WITH_MASK);
        TypeInfo edge = types.getFirst().findSubType("Edge");
        nonModifying(edge, "getType", "isBreak");
        nonModifying(types.getFirst().findSubType("EdgeType"), "mask");
        translate(types);
        String rust = module("a.b");
        // was '-> &EdgesEdgeType', and '&EdgesEdgeType == EdgesEdgeType' (E0277; fernflower's StatEdge.EdgeType)
        contains(rust, "pub fn get_type(&self) -> EdgesEdgeType {");
        assertEquals(List.of(), rustcErrors(), rust);
    }
}
