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

import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestMethodCall5 extends CommonTest {
    @Language("java")
    private static final String INPUT0 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.io.Serializable;

            public class MethodCall_50 {
                public static int size(Serializable o) {
                    return o.hashCode();
                }

                public int go(String[] strings) {
                    return size(strings);
                }

                public int go2(long[] longs) {
                    return size(longs);
                }

                interface X {
                }

                int go3(X[] xs) {
                    return size(xs);
                }

                int go4(X x) {
                    //  return size(x); ILLEGAL
                    return 0;
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

            import java.util.Set;

            // should catch an error, but does not...?
            public class MethodCall_51 {

                static class ResolverPath implements java.io.Serializable {

                    interface PathProcessor {
                        void processPath(ResolverPath path);
                    }

                    public void get(Set<ResolverPath> paths, PathProcessor processor) {
                    }
                }


                interface PathProcessor extends ResolverPath.PathProcessor {
                    void setOuterJoin(boolean outerJoin);
                }

                private PathProcessor processor;

                public void method() {
                    new ResolverPath().get(null, processor);
                }

            }""";

    @Test
    public void test1() {
        javaInspector.parse(INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            public class MethodCall_52 {

                static class MyException extends RuntimeException implements java.io.Serializable {

                    public MyException(long anError, String... args) {
                        this(anError, args, true);
                    }

                    public MyException(long anError, String[] args, boolean logTrace) {
                    }
                }

                <V> void method(long id, V value) {
                    throw new MyException(3L, String.valueOf(id), String.valueOf(value));
                }

                <V> String method2(V value) {
                    return String.valueOf(value);
                }
            }
            """;

    @Test
    public void test2() {
        javaInspector.parse(INPUT2);
    }

    @Language("java")
    private static final String INPUT7 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Properties;

            public class MethodCall_57 {

                Properties properties;
                private static final String X = "x";


                static class III  {
                }

                static class II extends III {
                }

                static class I extends II {
                }

                interface K {
                    R makeR();
                }

                record R(I[] is) {
                }

                void method(K k) {
                    properties.put(X, k.makeR().is());
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

            import java.io.Serializable;
            import java.util.Date;
            import java.util.SortedMap;
            import java.util.TreeMap;

            public class MethodCall_58 {

                static abstract class I implements Comparable<I>, Cloneable, Serializable {
                }

                static class D extends I implements Serializable {
                    D(Date date) {
                    }

                    @Override
                    public int compareTo(I o) {
                        return 0;
                    }
                }

                static double add(double x, double y) {
                    return x + y;
                }

                public void method1(Date date, double n) {
                    SortedMap<D, Double> map = new TreeMap<>();
                    map.compute(new D(date), (k, v) -> add(v == null ? 0 : (double) v, n));
                }

                public void method2(Date date, double n) {
                    SortedMap<D, Double> map = new TreeMap<>();
                    map.compute(new D(date), (k, v) -> add(v == null ? 0L : v, n));
                }

                static double setValue(Double d) {
                    return d;
                }

                public double method3(String tmp) {
                    return setValue(tmp != null && tmp.length() > 0 ? Double.valueOf(tmp) : 0d);
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

            import java.text.DecimalFormat;
            import java.text.NumberFormat;
            import java.util.ArrayList;
            import java.util.Collection;
            import java.util.Iterator;
            import java.util.List;

            public class MethodCall_59 {

                public void method(Collection ids) {
                    List newList = new ArrayList();
                    Iterator it = ids.iterator();
                    while (it.hasNext()) {
                        NumberFormat f = new DecimalFormat("0000000");
                        String format = f.format(it.next());
                        newList.add(format);
                    }
                }
            }
            """;

    @Test
    public void test9() {
        javaInspector.parse(INPUT9);
    }


    @Language("java")
    private static final String INPUT11 = """
            package org.e2immu.analyser.resolver.testexample;

            public class MethodCall_56 {

                interface I<T> {

                }
                static class AI<A>  implements I<A> {

                }
                static class BI<B> implements I<B> {

                }

                String method(I<? extends Number> i) {
                    return "hello "+i;
                }
                public String method(boolean b) {
                    return method(b ? new AI<Long>(): new BI<Integer>());
                }
            }
            """;

    @Test
    public void test11() {
        javaInspector.parse(INPUT11);
    }


    @Language("java")
    private static final String INPUT12 = """
            package org.e2immu.analyser.resolver.testexample;

            public class MethodCall_55 {

                interface D {
                    long id();
                }

                String method(D d) {
                    // noinspection ALL
                    StringBuilder buf = new StringBuilder();
                    buf.append(d.id() != Long.MIN_VALUE ? d.id() : "");
                    return buf.toString();
                }
            }
            """;

    @Test
    public void test12() {
        javaInspector.parse(INPUT12);
    }

}
