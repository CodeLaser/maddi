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

package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class TestMethodCall0 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;

            import java.util.ArrayList;import java.util.List;

            public class MethodCall_0 {

                private List<String> list;

                public MethodCall_0() {
                   list = new ArrayList<>();
                }
                public void add(String s) {
                   list.add(s);
                }
                public String get(String s) {
                   return list.get(0);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        FieldInfo fieldInfo = typeInfo.getFieldByName("list", true);
        assertEquals("java.util.List", fieldInfo.type().typeInfo().fullyQualifiedName());
        MethodInfo add = typeInfo.findUniqueMethod("add", 1);
        if (add.methodBody().statements().getFirst() instanceof ExpressionAsStatement eas
            && eas.expression() instanceof MethodCall mc) {
            assertEquals("java.util.List.add(E)", mc.methodInfo().fullyQualifiedName());
        } else fail();

        MethodInfo get = typeInfo.findUniqueMethod("get", 1);
        if (get.methodBody().statements().getFirst() instanceof ReturnStatement rs
            && rs.expression() instanceof MethodCall mc) {
            assertEquals("java.util.List.get(int)", mc.methodInfo().fullyQualifiedName());
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;

            class MethodCall_0 {

                record Get(String s) {
                    String get() {
                        return s;
                    }
                }

                void accept(List<Get> list) {
                    list.forEach(get -> System.out.println(get.get()));
                }

                void test() {
                    accept(List.of(new Get("hello")));
                }

                void test2() {
                    accept(null);
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo consumer = javaInspector.compiledTypesManager().get(Consumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.hasBeenInspected());

        TypeInfo typeInfo = javaInspector.parse(INPUT2);
        TypeInfo subType = typeInfo.findSubType("Get");
        MethodInfo accept = typeInfo.findUniqueMethod("accept", 1);
        ParameterInfo list = accept.parameters().getFirst();
        assertSame(subType, list.parameterizedType().parameters().getFirst().typeInfo());
        if (accept.methodBody().statements().getFirst() instanceof ExpressionAsStatement eas
            && eas.expression() instanceof MethodCall mc) {
            assertEquals("java.lang.Iterable.forEach(java.util.function.Consumer<? super T>)",
                    mc.methodInfo().fullyQualifiedName());
            if (mc.parameterExpressions().getFirst() instanceof Lambda lambda) {
                assertEquals("Type java.util.function.Consumer<org.e2immu.analyser.resolver.testexample.MethodCall_0.Get>",
                        lambda.concreteFunctionalType().toString());
            } else fail();
        } else fail();
    }


    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;

            public class MethodCall_1 {

                interface Get {
                    String get();
                }

                record GetOnly(String s) implements Get {

                    @Override
                    public String get() {
                        return s;
                    }
                }

                public void accept(List<Get> list) {
                    list.forEach(get -> System.out.println(get.get()));
                }

                public void test() {
                    // here, List.of(...) becomes a List<Get> because of the context of 'accept(...)'
                    accept(List.of(new GetOnly("hello")));
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo test = typeInfo.findUniqueMethod("test", 0);
        if (test.methodBody().statements().getFirst() instanceof ExpressionAsStatement eas
            && eas.expression() instanceof MethodCall mc1
            && mc1.parameterExpressions().getFirst() instanceof MethodCall mc2) {
            assertEquals("Type java.util.List<org.e2immu.analyser.resolver.testexample.MethodCall_1.GetOnly>",
                    mc2.concreteReturnType().toString());
        } else fail();
        TypeInfo GetOnly = typeInfo.findSubType("GetOnly");
        assertEquals("@11:30-11:39",
                GetOnly.source().detailedSources().detail(DetailedSources.IMPLEMENTS).toString());
    }


    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;

            public class MethodCall_2 {

                interface Get {
                    String get();
                }

                interface Set extends Get {
                    void set(String s);
                }

                static class Both implements Set {
                    private String s;

                    public Both(String s) {
                        this.s = s;
                    }

                    @Override
                    public String get() {
                        return s;
                    }

                    @Override
                    public void set(String s) {
                        this.s = s;
                    }
                }

                public void accept(List<Get> list) {
                    list.forEach(get -> System.out.println(get.get()));
                }

                public void test() {
                    // here, List.of(...) becomes a List<Get> because of the context of 'accept(...)'
                    accept(List.of(new Both("hello")));
                }
            }
            """;

    @Test
    public void test4() {
        javaInspector.parse(INPUT4);
    }

    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;

            class MethodCall_3 {

                interface Get {
                    String get();
                }

                public void accept(List<Get> list) {
                    list.forEach(get -> System.out.println(get.get()));
                }

                public void test() {
                    accept(List.of(() -> "hello"));
                }
            }
            """;

    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }

    @Language("java")
    private static final String INPUT6 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.ArrayList;
            import java.util.List;

            public class MethodCall_4 {
                private static List<String> copy(List<String> list) {
                    return new ArrayList<>(list);
                }

                public static int length(List<String> list) {
                    return copy(list).size();
                }
            }
            """;

    @Test
    public void test6() {
        javaInspector.parse(INPUT6);
    }

    @Language("java")
    private static final String INPUT7 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Collection;
            import java.util.List;
            import java.util.Set;

            public class MethodCall_5 {

                interface Get {
                    String get();
                }

                record GetOnly(String s) implements Get {

                    @Override
                    public String get() {
                        return s;
                    }
                }

                public void accept(List<Get> list) {
                    list.forEach(get -> System.out.println(get.get()));
                }

                public void accept(Set<Get> set) {
                    set.forEach(get -> System.out.println(get.get()));
                }

                public void accept(Collection<Get> set) {
                    set.forEach(get -> System.out.println(get.get()));
                }

                public void test() {
                    // here, List.of(...) becomes a List<Get> because of the context of 'accept(...)'
                    accept(List.of(new GetOnly("hello")));
                }
            }
            """;

    @Test
    public void test7() {
        javaInspector.parse(INPUT7);
    }

    @Language("java")
    private static final String INPUT8 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.function.Function;

            public class MethodCall_6 {

                interface  A {}
                interface  B extends A {}

                public A method(Function<B, A> f, B b) {
                    return f.apply(b);
                }
                public B method(Function<A, B> f, A a) {
                    return f.apply(a);
                }

                public void test() {
                    A a = new A() {
                    };
                    B b = new B() {
                    };
                    // CAUSES "Ambiguous method call": accept(bb -> bb, b);
                    method((B bb) -> bb, b);
                    method(aa -> (B)aa, a);
                }
            }
            """;

    @Test
    public void test8() {
        javaInspector.parse(INPUT8);
    }

    @Language("java")
    private static final String INPUT9 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;
            import java.util.function.BiConsumer;
            import java.util.function.Consumer;

            // see also MethodCall_27
            public class MethodCall_7<A, B, BB extends B> {

                public void method(List<B> list, Consumer<B> b) {
                    b.accept(list.get(0));
                }

                public void method(List<A> list, BiConsumer<A, B> a) {
                    a.accept(list.get(0), null);
                }

                public void test(A a, BB bb) {
                    method(List.of(bb), System.out::println);
                    method(List.of(a), (x, y) -> System.out.println(x + " " + y));
                }
            }
            """;

    @Test
    public void test9() {
        javaInspector.parse(INPUT9);
    }


    @Language("java")
    private static final String INPUT10 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;
            import java.util.Set;

            /*
            A bit contrived
             */
            public class MethodCall_8<A, B> {

                public void method(List<A> list1, List<A> list2, List<A> list3) {
                }

                public void method(List<B> list1, Set<A> set2, List<B> list3) {
                }

                public void method(Set<A> set1, List<B> list2, List<B> list3) {
                }

                public void test(A a, B b) {
                    method(List.of(a), List.of(a), List.of(a));
                    //compilation error: method(List.of(b), List.of(a),  List.of(a));
                    method(List.of(b), Set.of(a), List.of(b));
                    method(Set.of(a), List.of(b), List.of(b));
                }
            }
            """;

    @Test
    public void test10() {
        javaInspector.parse(INPUT10);
    }


    @Language("java")
    private static final String INPUT11 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Collection;
            import java.util.List;
            import java.util.Set;

            public class MethodCall_9 {

                interface Get {
                    String get();
                }

                record GetOnly(String s) implements Get {

                    @Override
                    public String get() {
                        return s;
                    }
                }

                public void accept(Collection<Get> set) {
                    set.forEach(get -> System.out.println(get.get()));
                }

                public void accept(Set<Get> set) {
                    set.forEach(get -> System.out.println(get.get()));
                }

                public void test() {
                    // here, List.of(...) becomes a List<Get> because of the context of 'accept(...)'; then, it is compatible
                    // with Collection<Get>
                    accept(List.of(new GetOnly("hello")));
                }
            }
            """;

    @Test
    public void test11() {
        javaInspector.parse(INPUT11);
    }

}
