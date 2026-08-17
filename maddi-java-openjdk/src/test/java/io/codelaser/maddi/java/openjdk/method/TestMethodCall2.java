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

package org.e2immu.language.java.openjdk.method;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMethodCall2 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            
            import java.util.*;
            
            public class C {
                private static final Map<String, Integer> PRIORITY = new HashMap<>();
            
                static {
                    PRIORITY.put("e2container", 1);
                    PRIORITY.put("e2immutable", 2);
                }
            
                static {
                    PRIORITY.put("e1container", 3);
                    PRIORITY.put("e1immutable", 4);
                }
            
                private static int priority(String in) {
                    return PRIORITY.getOrDefault(in.substring(0, in.indexOf('-')), 10);
                }
            
                private static String highestPriority(String[] annotations) {
                    List<String> toSort = new ArrayList<>(Arrays.asList(annotations));
                    toSort.sort(Comparator.comparing(C::priority));
                    return toSort.get(0);
                }
            }
            """;

    // more of a method call test
    @Test
    public void test1() {
        TypeInfo C = scan("a.b.C", INPUT1);
        assertEquals(4, C.methods().size());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            
            import org.e2immu.annotation.NotNull;
            
            import java.util.List;
            
            public class C {
            
                @NotNull(content = true)
                private final List<String> list;
            
                public C(@NotNull(content = true) List<String> list) {
                    this.list = list;
                }
            
                public int method() {
                    int res = 3;
                    for (String s : list.subList(0, 10)) {
                        if (s.length() == 9) {
                            res = 4;
                            break;
                        }
                    }
                    return res;
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo C = scan("a.b.C", INPUT2);

        FieldInfo list = C.getFieldByName("list", true);
        assertEquals("@NotNull(content=true)", list.annotations().getFirst().toString());

        MethodInfo constructor = C.findConstructor(1);
        ParameterInfo p0 = constructor.parameters().getFirst();
        assertEquals("@NotNull(content=true)", p0.annotations().getFirst().toString());
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            
            import java.util.function.Consumer;
            
            public class C {
            
                interface Analyser {
                    record SharedState(int iteration, String context) {}
                }
            
                static class MethodAnalyser implements Analyser {
            
                    private record SharedState(boolean allowBreaking) {}
            
                    public void init() {
                        Consumer<SharedState> consumer= sharedState -> method(sharedState);
                    }
            
                    public boolean method(SharedState sharedState) {
                        return true;
                    }
            
                    public boolean other() {
                        return false;
                    }
                }
            }
            """;

    @Test
    public void test24() {
        scan("a.b.C", INPUT3);
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            
            import java.util.List;
            import java.util.function.Consumer;
            import java.util.function.Predicate;
            
            public class C {
            
                interface Element {
            
                    default void visit(Consumer<Element> consumer) {
                        subElements().forEach(element -> element.visit(consumer));
                        consumer.accept(this);
                    }
            
                    default void visit(Predicate<Element> predicate) {
                        if (predicate.test(this)) {
                            subElements().forEach(element -> element.visit(predicate));
                        }
                    }
            
                    List<Element> subElements();
                }
            
                public void method(Element e) {
                    e.visit(element -> {
                        System.out.println("?");
                        return true;
                    });
                }
            
                /* Compilation error:
                public void method2(Element e) {
                    e.visit(element -> System.out.println("?"));
                }
                */
            
                public void method2(Element e) {
                    e.visit(element -> {
                        System.out.println("?");
                    });
                }
            
                public void method3(Element e) {
                    e.visit(element -> {
                        System.out.println("?");
                        if (element == null) {
                            return false;
                        } else {
                            return true;
                        }
                    });
                }
            
                public void method4(Element e) {
                    e.visit(element -> {
                        try {
                            System.out.println("Hello");
                            return true;
                        } finally {
                            System.out.println("?");
                        }
                    });
                }
            
                /*
                Compilation error:
                public void method5(Element e, List<String> list) {
                    e.visit(element -> {
                        for (String s : list) {
                           return true;
                        }
                    });
                }
                 */
            
                /*
                Compilation error: (cannot distinguish!)
                public void method5(Element e) {
                    e.visit(element -> {
                        throw new UnsupportedOperationException("?" + element);
                    });
                }*/
            }
            """;

    @Test
    public void test4() {
        TypeInfo C = scan("a.b.C", INPUT4);
        TypeInfo element = C.findSubType("Element");
        assertTrue(element.methods().stream()
                .filter(m -> "visit".equals(m.name()))
                .allMatch(MethodInfo::isDefault));
    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            
            import java.util.Map;
            
            public class C {
            
                public void method(Map<String, Integer> map) {
                    map.merge("abc", 4, (i1, i2) -> {
                        throw new UnsupportedOperationException();
                    });
                }
            }
            """;

    @Test
    public void test5() {
        scan("a.b.C", INPUT5);
    }

    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            
            import java.util.List;
            import java.util.function.BiConsumer;
            import java.util.function.Consumer;
            import java.util.function.Predicate;
            
            // see also MethodCall_7
            public class C<A, B> {
            
                public void method(List<B> list, Predicate<B> b) {
                    b.test(list.get(0));
                }
            
                public void method(List<B> list, Consumer<B> b) {
                    b.accept(list.get(0));
                }
            
                public void method(List<A> list, BiConsumer<A, B> a) {
                    a.accept(list.get(0), null);
                }
            
                public void test(A a, B b) {
                    // COMPILATION ERROR: method(List.of(bb), System.out::println);
                    method(List.of(a), (x, y) -> System.out.println(x + " " + y));
                    method(List.of(b), x -> x.toString().length() > 3);
                }
            }
            """;

    @Test
    public void test6() {
        scan("a.b.C", INPUT6);
    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            
            import java.util.Arrays;
            import java.util.function.Supplier;
            
            public class C {
                interface HasSize {
                    int size();
                }
            
                static class ImmutableArrayOfHasSize {
            
                    public final HasSize[] elements;
            
                    public ImmutableArrayOfHasSize(int size, Supplier<HasSize> generator) {
                        elements = new HasSize[size];
                        Arrays.setAll(elements, i -> generator.get());
                    }
            
                }
            }
            """;

    @Test
    public void test7() {
        scan("a.b.C", INPUT7);
    }

    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            
            import java.util.Map;
            import java.util.TreeMap;
            import java.util.function.BiConsumer;
            
            // a variant in Import fails because of a static import, see Import_11
            public class C {
                interface Variable {
                }
            
                interface DV {
                }
            
                private static class Node {
                    Map<Variable, DV> dependsOn;
                    final Variable variable;
            
                    private Node(Variable v) {
                        variable = v;
                    }
                }
            
                private final Map<Variable, Node> nodeMap = new TreeMap<>();
            
                public void visit(BiConsumer<Variable, Map<Variable, DV>> consumer) {
                    nodeMap.values().forEach(n -> consumer.accept(n.variable, n.dependsOn));
                }
            
            }
            
            """;

    @Test
    public void test8() {
        scan("a.b.C", INPUT8);
    }

}
