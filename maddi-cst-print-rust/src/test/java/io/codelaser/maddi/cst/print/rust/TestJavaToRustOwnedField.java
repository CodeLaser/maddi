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
}
