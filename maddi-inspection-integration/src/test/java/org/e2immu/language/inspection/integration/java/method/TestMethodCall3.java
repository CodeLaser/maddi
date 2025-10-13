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

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.impl.info.ImportComputerImpl;
import org.e2immu.language.cst.impl.info.TypePrinterImpl;
import org.e2immu.language.cst.print.FormatterImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMethodCall3 extends CommonTest {
    @Language("java")
    private static final String INPUT0 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.annotation.Modified;
            import org.e2immu.annotation.NotModified;

            import java.util.function.BinaryOperator;

            public abstract class MethodCall_30 {

                private int j;

                @NotModified
                protected BinaryOperator<Integer> m1;

                @NotModified
                protected abstract int m2(int i, int j);

                @Modified
                public int same1(int k) {
                    int d = m2(m1.apply(2, k), m1.apply(j = j + 1, k + 4));
                    return Math.max(d, Math.min(k, 10));
                }
            }
            """;

    @Test
    public void test0() {
        javaInspector.parse(INPUT0);
    }

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.net.URI;
            import java.net.http.HttpRequest;
            import java.time.Duration;
            import java.util.Objects;

            public class MethodCall_31 {

                private static final long DEFAULT_TIMEOUT = 30L;
                private static final String ACCEPT = "Accept";

                public static HttpRequest same4(URI uri, String a1, String a2, Long timeout) {
                    HttpRequest.Builder builder = HttpRequest.newBuilder()
                            .GET()
                            .uri(uri)
                            .timeout(Duration.ofMillis(Objects.requireNonNullElse(timeout, DEFAULT_TIMEOUT)))
                            .header(ACCEPT, a1)
                            .header("Accept", a2);
                    return builder.build();
                }
            }
            """;

    @Language("java")
    private static final String OUTPUT1 = """
            package org.e2immu.analyser.resolver.testexample;
            import java.net.URI;
            import java.net.http.HttpRequest;
            import java.time.Duration;
            import java.util.Objects;
            public class MethodCall_31 {
                private static final long DEFAULT_TIMEOUT = 30L;
                private static final String ACCEPT = "Accept";
                public static HttpRequest same4(URI uri, String a1, String a2, Long timeout) {
                    HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .GET()
                        .uri(uri)
                        .timeout(Duration.ofMillis(Objects.requireNonNullElse(timeout, DEFAULT_TIMEOUT)))
                        .header(ACCEPT, a1)
                        .header("Accept", a2);
                    return builder.build();
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        Qualification qualification = javaInspector.runtime().qualificationQualifyFromPrimaryType();
        OutputBuilder ob = new TypePrinterImpl(typeInfo, false).print(new ImportComputerImpl(), qualification, true);
        Formatter formatter = new FormatterImpl(javaInspector.runtime(), FormattingOptionsImpl.DEFAULT);
        String s = formatter.write(ob);

        assertEquals(OUTPUT1, s);
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.junit.jupiter.api.Test;

            import java.util.List;

            import static org.junit.jupiter.api.Assertions.assertFalse;
            import static org.junit.jupiter.api.Assertions.assertTrue;

            public class MethodCall_32 {

                public interface I {
                    int i();
                }

                public interface J {
                    char j();
                }

                public static <T extends I> T filterByID(T t, long theID) {
                    return filter(t, new long[]{theID}, null);
                }

                public static <T extends I> T filterByID(T t, long[] theIDs) {
                    return filter(t, theIDs, null);
                }

                public static <T extends I> T filterByID(T t, List<Long> theIDs) {
                    return filter(t, theIDs.stream().mapToLong(l -> l).toArray(), null);
                }

                public static <T extends I> T filter(T t, long[] theIDs, T target) {
                    return null;
                }

                public static <T extends J> T filterByID(T t, long theID) {
                    return filter(t, new long[]{theID}, null);
                }

                public static <T extends J> T filterByID(T t, long[] theIDs) {
                    return filter(t, theIDs, null);
                }

                public static <T extends J> T filterByID(T t, List<Long> theIDs) {
                    return filter(t, theIDs.stream().mapToLong(l -> l).toArray(), null);
                }

                public static <T extends J> T filter(T t, long[] theIDs, T target) {
                    return null;
                }

                record X(int i) implements I {
                }

                X test1(X x, long id) {
                    return filterByID(x, new long[]{id});
                }

                X test2(X x, long id) {
                    return filterByID(x, id);
                }

                record Y(char j) implements J {
                }

                Y test3(Y y, long id) {
                    return filterByID(y, new long[]{id});
                }

                Y test4(Y y, long id) {
                    return filterByID(y, id);
                }

                interface II extends I {

                }

                record XX(int i) implements II {
                }

                void method(XX xx) {

                }
                void test5(XX xx) {
                    // evaluated expression of filterByID is of type MethodCallErasure
                    method(filterByID(xx, 1));
                }

                @Test
                public void test() {
                    assertFalse(J.class.isAssignableFrom(XX.class));
                    assertTrue(I.class.isAssignableFrom(XX.class));
                    assertTrue(II.class.isAssignableFrom(XX.class));
                }
            }
            """;

    @Test
    public void test2() {
        javaInspector.parse(INPUT2);
    }

    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;


            import java.util.Arrays;

            public class MethodCall_33 {

                interface Expression extends Comparable<Expression> { }
                static class ArrayInitializer implements Expression {
                    private Expression[] expressions;

                    int internalCompareTo(Expression v) {
                        return Arrays.compare(expressions, ((ArrayInitializer) v).expressions);
                    }

                    @Override
                    public int compareTo(Expression o) {
                        return 0;
                    }
                }
            }
            """;

    @Test
    public void test3() {
        javaInspector.parse(INPUT3);
    }

    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;

            public class MethodCall_34 {

                public String test1(boolean b) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(b ? "x" : 3);
                    return sb.toString();
                }

                public String test2(CharSequence cs) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(cs);
                    return sb.toString();
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

            import java.io.Serializable;

            public class MethodCall_35 {

                static class A {

                }

                static class B extends A implements Serializable {

                    @Override
                    public boolean equals(Object o) {
                        return super.equals(o);
                    }

                    // this is bad practice, but it occurs in the wild :-(
                    public boolean equals(A a) {
                        return equals((Object) a);
                    }
                }

                boolean method(B a, B b) {
                    return b.equals(a);
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

            public class MethodCall_36 {
                public static <T> T[] removeObject(T[] arr, T el) {
                    return arr;
                }
                public static <T> T[] removeObject(T[] arr, T[] el) {
                    return arr;
                }

                public void test() {
                    String[] s1 = { "hello" };
                    String[] s2 = { "there" };
                    removeObject(s1, s2);
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

            import java.util.Map;

            public class MethodCall_37 {

                interface  DV extends Comparable<DV> {}

                static <T extends Comparable<? super T>> int compareMaps(Map<T, DV> map1, Map<T, DV> map2) {
                    int differentValue = 0;
                    for (Map.Entry<T, DV> e : map1.entrySet()) {
                        DV dv = map2.get(e.getKey());
                        if (dv == null) {
                            return 0;
                        }
                    }
                    return differentValue;
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

            import java.io.IOException;

            import java.io.Writer;

            public class MethodCall_38 {

                public static class ArrayList<I> extends java.util.ArrayList<I> {
                }

                private ArrayList<String> list;

                public void update(Writer writer) throws IOException {
                    // important for this test is to have the list.get(i) as the argument to write
                    // noinspection ALL
                    for (int i = 0; i < list.size(); i++) {
                        writer.write(list.get(i));
                    }
                }

                public void setList(ArrayList<String> list) {
                    this.list = list;
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

            public class MethodCall_39 {

                private final double FIELD = 3.14;
                private final Integer c = 300_000;

                public static double multiply(double x, double y) {
                    return x*y;
                }
                public static double subtract(Number n, Number m){
                    return n.doubleValue() - m.doubleValue();
                }

                public double method(double d1, double d2) {
                    double d = multiply(FIELD, subtract(d1, d2));
                    return d;
                }

                public double method2(double d1, double d2) {
                    return multiply(c, subtract(d1, d2));
                }
            }
            """;

    @Test
    public void test9() {
        javaInspector.parse(INPUT9);
    }

}
