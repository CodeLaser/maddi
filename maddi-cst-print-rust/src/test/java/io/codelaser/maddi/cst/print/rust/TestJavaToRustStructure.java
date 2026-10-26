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

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        contains(rust, "return self.items.keys().cloned().collect();");
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
     * A superclass is a {@code base} field; inherited members are reached through it. The interface method has no
     * verdict, so its trait takes {@code &mut self}; the implementation's own {@code &self} is an inherent method the
     * trait's delegates to.
     */
    @DisplayName("inheritance as composition, trait impl delegating to the implementation's own verdicts: compiles")
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
        contains(rust, "pub base: ControllersBase,");
        contains(rust, "base: ControllersBase::new(\"orders\".to_string()),");
        contains(rust, "self.base.audit(\"placed\");");
        contains(rust, "self.base.name.len() as i32 > 0");
        contains(rust, "return orders.base.log_size();");
        contains(rust, """
                fn is_valid(&mut self) -> bool {
                    Self::is_valid(self)
                }""");
        contains(rust, "pub fn is_valid(&self) -> bool {");
        assertEquals(List.of(), rustcErrors(), rust);
    }
}
