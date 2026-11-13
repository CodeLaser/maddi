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

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;

/**
 * {@code OWNED_FIELD} (maddi-mod docs/roadmap/ownership.md stage 4): a field whose object only its holder reaches is a
 * plain field, the object moved in. Without the verdict, a class whose objects a parameter keeps and a field holds is
 * a {@code Shared} handle wherever it is held ({@code RustShared}).
 */
public class TestJavaToRustOwnedField extends CommonJavaToRust {

    @Language("java")
    private static final String OWN = """
            package a.b;
            public class Own {
                public static class Counter {
                    private int n;
                    public void inc() { n++; }
                    public int value() { return n; }
                }
                // the constructor stores what it is given
                public static class Holder {
                    private final Counter c;
                    private Holder(Counter c) { this.c = c; }
                    public static Holder make() { return new Holder(new Counter()); }
                    public void tick() { c.inc(); }
                    public int get() { return c.value(); }
                }
                public static class Gauge {
                    private int n;
                    public void inc() { n++; }
                    public int value() { return n; }
                }
                // a setter stores what it is given
                public static class Meter {
                    private Gauge g;
                    private void set(Gauge gauge) { this.g = gauge; }
                    public void reset() { set(new Gauge()); }
                    public void tick() { g.inc(); }
                    public int get() { return g.value(); }
                }
            }
            """;

    private List<TypeInfo> types(boolean owned) {
        List<TypeInfo> types = parse(OWN);
        TypeInfo own = types.getFirst();
        TypeInfo counter = own.findSubType("Counter");
        TypeInfo holder = own.findSubType("Holder");
        TypeInfo gauge = own.findSubType("Gauge");
        TypeInfo meter = own.findSubType("Meter");
        modifying(counter, "inc");
        nonModifying(counter, "value");
        modifying(gauge, "inc");
        nonModifying(gauge, "value");
        modifying(holder, "tick");
        nonModifying(holder, "get", "make");
        modifying(meter, "tick", "reset", "set");
        nonModifying(meter, "get");
        kept(holder.constructors().getFirst().parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        kept(method(meter, "set").parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        if (owned) {
            holder.getFieldByName("c", true).analysis().set(PropertyImpl.OWNED_FIELD, ValueImpl.BoolImpl.TRUE);
            meter.getFieldByName("g", true).analysis().set(PropertyImpl.OWNED_FIELD, ValueImpl.BoolImpl.TRUE);
        }
        return types;
    }

    @DisplayName("without OWNED_FIELD: kept and held, so Shared handles")
    @Test
    public void notOwned() throws Exception {
        RustTranslator.Result result = translate(types(false), nullable(Set.of("g")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "c: crate::jrt::Shared<OwnCounter>,");
        contains(rust, "return Self::new(crate::jrt::Shared::new(OwnCounter::new()));");
        contains(rust, "g: Option<crate::jrt::Shared<OwnGauge>>,");
    }

    @DisplayName("OWNED_FIELD: plain fields, the objects moved in")
    @Test
    public void owned() throws Exception {
        RustTranslator.Result result = translate(types(true), nullable(Set.of("g")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "c: OwnCounter,");
        contains(rust, "fn new(c: OwnCounter) -> Self {");
        contains(rust, "return Self::new(OwnCounter::new());");
        // Java's null before the first set: the nullability verdict's Option
        contains(rust, "g: Option<OwnGauge>,");
        contains(rust, "fn set(&mut self, gauge: OwnGauge) {");
        assertFalse(rust.contains("Shared"), rust);
        assertFalse(rust.contains("'a"), rust);
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String BAG = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class Bags {
                public static class Item {
                    private int n;
                    public void inc() { n++; }
                }
                public static class Bag {
                    private final List<Item> items = new ArrayList<>();
                    public void add(Item i) { items.add(i); }
                    public int size() { return items.size(); }
                }
                public static class Shelf {
                    private final List<Item> all = new ArrayList<>();
                    // a new bag, through a local, holding the shelf's items: a dependent result, yet fresh
                    public Bag collect() {
                        Bag b = new Bag();
                        for (Item i : all) b.add(i);
                        return b;
                    }
                }
            }
            """;

    private List<TypeInfo> bagTypes(boolean fresh) {
        List<TypeInfo> types = parse(BAG);
        TypeInfo bags = types.getFirst();
        TypeInfo item = bags.findSubType("Item");
        TypeInfo bag = bags.findSubType("Bag");
        TypeInfo shelf = bags.findSubType("Shelf");
        modifying(item, "inc");
        modifying(bag, "add");
        nonModifying(bag, "size");
        nonModifying(shelf, "collect");
        kept(method(bag, "add").parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        dependentResult(shelf, "collect");
        if (fresh) method(shelf, "collect").analysis().set(PropertyImpl.FRESH_METHOD, ValueImpl.BoolImpl.TRUE);
        return types;
    }

    @DisplayName("without FRESH_METHOD: a dependent result through a local is a borrow")
    @Test
    public void notFresh() throws Exception {
        translate(bagTypes(false));
        contains(module("a.b"), "pub fn collect(&self) -> &BagsBag {");
    }

    @DisplayName("FRESH_METHOD: the new object is returned by value")
    @Test
    public void fresh() throws Exception {
        RustTranslator.Result result = translate(bagTypes(true));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "pub fn collect(&self) -> BagsBag {");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String BUF = """
            package a.b;
            public class Bufs {
                public static class Buf {
                    private final StringBuilder sb = new StringBuilder();
                    public Buf add(String s) { sb.append(s); return this; }
                    public int length() { return sb.length(); }
                }
                public static class Printer {
                    // fernflower's TextBuffer: a fluent chain on a fresh local is the result
                    public Buf print(String s) {
                        Buf b = new Buf();
                        return b.add("[").add(s).add("]");
                    }
                    public Buf quote(String s) { return new Buf().add("'").add(s).add("'"); }
                }
            }
            """;

    @DisplayName("an owned result made by a fluent chain: the calls, then the object")
    @Test
    public void fluentChainResult() throws Exception {
        List<TypeInfo> types = parse(BUF);
        TypeInfo bufs = types.getFirst();
        TypeInfo buf = bufs.findSubType("Buf");
        TypeInfo printer = bufs.findSubType("Printer");
        modifying(buf, "add");
        fluent(buf, "add");
        nonModifying(buf, "length");
        nonModifying(printer, "print", "quote");
        independentResult(printer, "print");
        independentResult(printer, "quote");
        method(printer, "print").analysis().set(PropertyImpl.FRESH_METHOD, ValueImpl.BoolImpl.TRUE);
        method(printer, "quote").analysis().set(PropertyImpl.FRESH_METHOD, ValueImpl.BoolImpl.TRUE);
        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "return { b.add(\"[\".to_string()).add(s).add(\"]\".to_string()); b };");
        contains(rust,
                "return { let mut __r = BufsBuf::new(); __r.add(\"'\".to_string()).add(s).add(\"'\".to_string()); __r };");
        assertEquals(List.of(), rustcErrors(), rust);
    }
}
