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

public class TestMethodCall4 extends CommonTest {
    @Language("java")
    private static final String INPUT0= """
            package org.e2immu.analyser.resolver.testexample;

            import java.text.DecimalFormat;

            public class MethodCall_40 {

                /*
                candidate methods have formal types double, long, Object.
                Result of valueOf is Long
                 */
                public String method(String value) {
                    DecimalFormat format = new DecimalFormat("000000000000");
                    return format.format(Long.valueOf(value));
                }
            }
            """;

    @Test
    public void test0() {
        javaInspector.parse(INPUT0);
    }

    @Language("java")
    private static final String INPUT1= """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;

            public class MethodCall_41 {

                private Object[][] makeArray(List<Object[]> list) {
                    return list.toArray(new Object[list.size()][4]);
                }

            }
            """;
    @Test
    public void test1() {
        javaInspector.parse(INPUT1);
    }
    @Language("java")
    private static final String INPUT2= """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.ArrayList;
            import java.util.Map;

            public class MethodCall_42 {
                public static final String TRUE = "1";
                public static final short True = 1;

                public static Boolean toBoolean(short bool) {
                    if (bool == True) {
                        return Boolean.TRUE;
                    }
                    return Boolean.FALSE;
                }

                public static Boolean toBoolean(String bool) {
                    if (bool == null) {
                        return Boolean.FALSE;
                    }
                    if (bool.equalsIgnoreCase(TRUE) || bool.equalsIgnoreCase("true")) {
                        return Boolean.TRUE;
                    }
                    return Boolean.FALSE;
                }

                public String method(Map<String, ArrayList<String>> map) {
                    return map.entrySet().stream().filter(e -> toBoolean(e.getValue().get(0)))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(null);
                }
            }
            """;
    @Test
    public void test2() {
        javaInspector.parse(INPUT2);
    }
    @Language("java")
    private static final String INPUT3= """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.language.inspection.integration.java.importhelper.a.CanBeSerialized;
            import org.e2immu.language.inspection.integration.java.importhelper.a.Serializer;

            public class MethodCall_43<E extends CanBeSerialized> extends Serializer<E> {

                // Compare to Serializer: the type E has a type bound!!
                public int method() {
                    int sum = 0;
                    for (int i = 0; i < element.list().size(); i++) {
                        sum += element.isX() ? 1 : 0;
                    }
                    return sum;
                }
            }
            """;
    @Test
    public void test3() {
        javaInspector.parse(INPUT3);
    }

    @Language("java")
    private static final String INPUT5= """
            package org.e2immu.analyser.resolver.testexample;

            import java.io.Serializable;
            import java.util.List;

            /*
            UnboundMethodParameterType in combination with erasure; arrayPenalty
             */
            public class MethodCall_45 {

                interface B {
                }

                interface C extends Serializable {
                }

                interface A extends Serializable, B {
                }

                static long[] ids(B[] bs) {
                    return new long[]{0L};
                }

                static long[] ids(C c) {
                    return new long[]{1L};
                }

                long[] call1(A[] as) {
                    return ids(as);
                }

                long[] call2(List<A> as) {
                    return ids(as.toArray(new A[0]));
                }
            }
            """;
    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }

    @Language("java")
    private static final String INPUT7= """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Arrays;

            public class MethodCall_47 {

                // ensure that there are sufficient methods so that Element and Expression cannot be @FunctionalInterface
                interface Element {
                    int method1();
                    int method2();
                }

                interface Expression extends Element, Comparable<Expression> {
                    int method3();
                    int method4();
                }

                record MultiExpression(Expression... expressions) {
                }

                MultiExpression multiExpression;

                int internalCompareTo(Expression v) {
                    return Arrays.compare(multiExpression.expressions(), ((MethodCall_47) v).multiExpression.expressions());
                }
            }
            """;
    @Test
    public void test7() {
        javaInspector.parse(INPUT7);
    }
    @Language("java")
    private static final String INPUT8= """
            package org.e2immu.analyser.resolver.testexample;

            public class MethodCall_48 {
                public static class ArrayList<I> extends java.util.ArrayList<I> {
                }

                ArrayList<Long> list = new ArrayList<>();

                public static long[] toPrimitive(Long[] array) {
                    return null;
                }

                public static int[] toPrimitive(Integer[] array) {
                    return null;
                }

                public int method() {
                    /*
                    Current test problem: The erasure results of evaluating the toArray call contain no 'Long' info, and therefore,
                    the two toPrimitive methods are ranked equally.
                     */
                    Long[] longs = new Long[list.size()];
                    long[] contactIdsArray = toPrimitive(list.toArray(longs));
                    return contactIdsArray.length;
                }
            }
            """;

    @Test
    public void test8() {
        javaInspector.parse(INPUT8);
    }
    @Language("java")
    private static final String INPUT9= """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Arrays;

            public class MethodCall_49 {

                record R(String date) {
                }

                void method1(R[] rs) {
                    // Arrays.sort(T[] ts, Comparator<? super T> c)
                    Arrays.sort(rs, (r1, r2) -> r1.date.compareTo(r2.date));
                }

                void method2(R[] rs) {
                    Arrays.sort(rs, (r1, r2) -> r1.date().compareTo(r2.date()));
                }
            }
            """;
    @Test
    public void test9() {
        javaInspector.parse(INPUT9);
    }

}
