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
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Statements and expressions beyond the basics: switch, enum, StringBuilder, streams, equals and hashCode. */
public class TestJavaToRustStatements extends CommonJavaToRust {

    @Language("java")
    private static final String ORDERS = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.Objects;
            public class Orders {
                public enum Status { NEW, PAID, SHIPPED, CANCELLED }

                public static final class Order {
                    private final int id;
                    private final String customer;
                    private Status status = Status.NEW;

                    public Order(int id, String customer) {
                        this.id = id;
                        this.customer = customer;
                    }

                    public int id() { return id; }
                    public String customer() { return customer; }
                    public Status status() { return status; }

                    public void advance() {
                        switch (status) {
                            case NEW -> status = Status.PAID;
                            case PAID -> status = Status.SHIPPED;
                            default -> { }
                        }
                    }

                    @Override
                    public boolean equals(Object o) {
                        if (this == o) return true;
                        if (!(o instanceof Order other)) return false;
                        return id == other.id;
                    }

                    @Override
                    public int hashCode() {
                        return Objects.hash(id);
                    }
                }

                private final List<Order> orders = new ArrayList<>();

                public void add(Order order) {
                    orders.add(order);
                }

                public static String label(Status status) {
                    return switch (status) {
                        case NEW -> "new";
                        case PAID, SHIPPED -> "active";
                        case CANCELLED -> "gone";
                    };
                }

                public static int weight(int code) {
                    switch (code) {
                        case 1:
                            return 10;
                        case 2:
                        case 3:
                            return 20;
                        default:
                            return 0;
                    }
                }

                public List<String> customers() {
                    return orders.stream().map(Order::customer).distinct().toList();
                }

                public long paid() {
                    return orders.stream().filter(o -> o.status() == Status.PAID).count();
                }

                public String report() {
                    StringBuilder sb = new StringBuilder();
                    for (Order o : orders) {
                        sb.append(o.id()).append(": ").append(label(o.status())).append('\\n');
                    }
                    return sb.toString();
                }

                public int[] counts() {
                    int[] result = new int[Status.values().length];
                    for (Order o : orders) result[o.status().ordinal()]++;
                    return result;
                }

                public boolean same(Order a, Order b) {
                    return a.equals(b);
                }
            }
            """;

    @DisplayName("switch as match, enum values and ordinal, StringBuilder, streams, equals as PartialEq: compiles")
    @Test
    public void orders() throws Exception {
        List<TypeInfo> types = parse(ORDERS);
        TypeInfo orders = types.getFirst();
        TypeInfo order = orders.findSubType("Order");
        nonModifying(order, "id", "customer", "status", "hashCode");
        modifying(order, "advance");
        modifying(orders, "add");
        nonModifying(orders, "customers", "paid", "report", "counts", "same");
        kept(method(orders, "add").parameters().getFirst(), ValueImpl.IndependentImpl.DEPENDENT);
        borrowed(method(orders, "same").parameters().get(0));
        borrowed(method(orders, "same").parameters().get(1));
        independentResult(order, "customer");
        independentResult(orders, "customers");

        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, """
                match self.status {
                    OrdersStatus::New => {
                        self.status = OrdersStatus::Paid;
                    }""");
        contains(rust, "OrdersStatus::Paid | OrdersStatus::Shipped => \"active\".to_string(),");
        contains(rust, "2 | 3 => {");
        contains(rust, "self.orders.iter().map(|x| x.customer())");
        contains(rust, """
                sb.push_str(&(o.id()).to_string());
                sb.push_str(": ");""");
        contains(rust, "let mut result = vec![0; OrdersStatus::values().len()];");
        contains(rust, "result[o.status().ordinal() as usize] += 1;");
        contains(rust, """
                impl PartialEq for OrdersOrder {
                    fn eq(&self, other: &Self) -> bool {
                        return self.id == other.id;
                    }
                }""");
        contains(rust, "impl std::hash::Hash for OrdersOrder {");
        contains(rust, "return a == b;");
        assertEquals(List.of(), rustcErrors(), rust);
    }

    @Language("java")
    private static final String ALIASING = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            public class Aliasing {
                private final Map<String, Integer> counts = new HashMap<>();
                private final Map<String, Object> data = new HashMap<>();
                private String mode = "a";
                private int changes;

                public void setMode(String m) {
                    String old = mode;
                    mode = m;
                    record(old, m);
                }

                public void record(String from, String to) {
                    changes++;
                }

                public void touch() {
                    record(mode, "b");
                }

                public void bump(String key) {
                    counts.put(key, counts.getOrDefault(key, 0) + 1);
                }

                public void put(String key, int value) {
                    data.put(key, value);
                }

                public int get(String key) {
                    return (int) data.get(key);
                }

                public boolean isMode(String m) {
                    return "a".equals(data.get(m));
                }
            }
            """;

    /** Java's aliasing of immutable values, which Rust's borrows would reject: copies; Object as Box<dyn Any>. */
    @DisplayName("a string kept across an assignment, a field passed to a &mut self method, Object values: compiles")
    @Test
    public void aliasing() throws Exception {
        List<TypeInfo> types = parse(ALIASING);
        TypeInfo aliasing = types.getFirst();
        modifying(aliasing, "setMode", "record", "touch", "bump", "put");
        nonModifying(aliasing, "get", "isMode");
        for (String m : List.of("setMode", "bump", "put", "get", "isMode")) {
            borrowed(method(aliasing, m).parameters().getFirst());
        }
        borrowed(method(aliasing, "record").parameters().get(0));
        borrowed(method(aliasing, "record").parameters().get(1));

        RustTranslator.Result result = translate(types);
        assertTrue(result.crashes().isEmpty(), () -> String.join("\n", result.crashes()));
        String rust = module("a.b");
        contains(rust, "let old = self.mode.clone();");
        contains(rust, "self.record(&self.mode.clone(), \"b\");");
        contains(rust, "{ let v = self.counts.get(key).copied().unwrap_or(0) + 1; self.counts.insert(key.to_string(), v) }");
        contains(rust, "self.data.insert(key.to_string(), Box::new(value));");
        contains(rust, "return *self.data.get(key).unwrap().downcast_ref::<i32>().unwrap();");
        assertEquals(List.of(), rustcErrors(), rust);
    }
}
