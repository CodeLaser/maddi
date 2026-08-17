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

import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestMethodCall5 extends CommonTest {
    @Language("java")
    private static final String INPUT0 = """
            package a.b;
            
            import java.io.Serializable;
            
            public class C {
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
        scan("a.b.C", INPUT0);
    }

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            
            import java.util.Set;
            
            // should catch an error, but does not...?
            public class C {
            
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
        scan("a.b.C", INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            
            public class C {
            
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
        scan("a.b.C", INPUT2);
    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            
            import java.util.Properties;
            
            public class C {
            
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
        scan("a.b.C", INPUT7);
    }


    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            
            import java.io.Serializable;
            import java.util.Date;
            import java.util.SortedMap;
            import java.util.TreeMap;
            
            public class C {
            
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
        scan("a.b.C", INPUT8);
    }

    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            
            import java.text.DecimalFormat;
            import java.text.NumberFormat;
            import java.util.ArrayList;
            import java.util.Collection;
            import java.util.Iterator;
            import java.util.List;
            
            public class C {
            
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
        scan("a.b.C", INPUT9);
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            
            public class C {
            
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
    public void test3() {
        scan("a.b.C", INPUT3);
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            
            public class C {
            
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
    public void test4() {
        scan("a.b.C", INPUT4);
    }

}
