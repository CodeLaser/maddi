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
}
