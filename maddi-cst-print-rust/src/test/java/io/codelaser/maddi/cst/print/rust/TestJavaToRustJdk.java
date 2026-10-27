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

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The JDK as std: comparators, a map's value modified in place, Object values, null-safe string calls. */
public class TestJavaToRustJdk extends CommonJavaToRust {

    @Language("java")
    private static final String CATALOG = """
            package a.b;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            public class Catalog {
                private final Map<String, Integer> views = new HashMap<>();
                private final Map<String, Map<String, String>> articles = new HashMap<>();
                private final Map<String, Object> data = new HashMap<>();
                private static final Map<String, Double> RATES = Map.of("basic", 1.0, "premium", 2.5);

                public static double rate(String tier) {
                    return RATES.getOrDefault(tier, 1.0);
                }

                public List<String> top(int n) {
                    return views.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                            .limit(n).map(Map.Entry::getKey).toList();
                }

                public void setTitle(String id, String title) {
                    articles.getOrDefault(id, new HashMap<>()).put("title", title);
                }

                public String describe(String key) {
                    return "value: " + data.get(key);
                }

                public boolean isExpress(String destination) {
                    return "express".equalsIgnoreCase(destination);
                }

                public void count(String key) {
                    views.merge(key, 1, Integer::sum);
                }
            }
            """;

    @DisplayName("comparator, getOrDefault in place, Object displayed, null-safe equalsIgnoreCase, a static Map.of: compiles")
    @Test
    public void catalog() throws Exception {
        List<TypeInfo> types = parse(CATALOG);
        TypeInfo catalog = types.getFirst();
        modifying(catalog, "setTitle", "count");
        nonModifying(catalog, "top", "describe", "isExpress");
        independentResult(catalog, "top");
        independentResult(catalog, "describe");
        for (String m : List.of("setTitle", "describe", "isExpress")) {
            method(catalog, m).parameters().forEach(CommonJavaToRust::borrowed);
        }
        kept(method(catalog, "count").parameters().getFirst(), ValueImpl.IndependentImpl.INDEPENDENT_HC);
        borrowed(method(catalog, "rate").parameters().getFirst());
        catalog.getFieldByName("RATES", true).analysis().set(PropertyImpl.UNMODIFIED_FIELD, ValueImpl.BoolImpl.TRUE);

        RustTranslator.Result result = translate(types, nullable(Set.of("destination", "describe()")));
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "v.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap());");
        contains(rust, "self.articles.get_mut(id).unwrap_or(&mut HashMap::new()).insert(");
        contains(rust, "crate::jrt::to_string(");
        contains(rust, "static CATALOG_RATES: std::sync::LazyLock<HashMap<String, f64>> = std::sync::LazyLock::new(|| "
                       + "HashMap::from([(\"basic\".to_string(), 1.0), (\"premium\".to_string(), 2.5)]));");
        contains(rust, "return CATALOG_RATES.get(tier).copied().unwrap_or(1.0);");
        contains(rust, "destination.as_ref().map_or(false, |v| \"express\".eq_ignore_ascii_case(v))");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String EVENTS = """
            package a.b;
            public class Events {
                public interface Listener {
                    void on(String event);
                }

                public interface Factory {
                    String make(String a, int n);
                }

                public static void fire(Listener listener, String event) {
                    listener.on(event);
                }

                public static void start() {
                    fire(e -> System.out.println(e), "start");
                }

                public static String made() {
                    Factory f = (a, n) -> a.repeat(n);
                    return f.make("x", 3);
                }
            }
            """;

    /** A closure implements a source interface with one method: a blanket impl, and the closure's parameters typed. */
    @DisplayName("lambdas implementing source functional interfaces: compiles")
    @Test
    public void events() throws Exception {
        List<TypeInfo> types = parse(EVENTS);
        TypeInfo events = types.getFirst();
        TypeInfo listener = events.findSubType("Listener");
        TypeInfo factory = events.findSubType("Factory");
        nonModifying(listener, "on");
        nonModifying(factory, "make");
        borrowed(method(listener, "on").parameters().getFirst());
        borrowed(method(factory, "make").parameters().getFirst());
        independentResult(factory, "make");
        method(events, "fire").parameters().forEach(CommonJavaToRust::borrowed);

        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "impl<F: Fn(&str)> EventsListener for F {");
        contains(rust, "impl<F: Fn(&str, i32) -> String> EventsFactory for F {");
        contains(rust, "Self::fire(&|e: &str| { println!(\"{}\", e); }, \"start\");");
        contains(rust, "let f = |a: &str, n: i32| a.repeat(n as usize);");
        assertEquals(List.of(), rustcErrors(), rust);
    }
}
