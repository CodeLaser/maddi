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

package io.codelaser.maddi.java.openjdk.method;

import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestMethodCall4 extends CommonTest {
    @Language("java")
    private static final String INPUT0 = """
            package a.b;
            
            import java.text.DecimalFormat;
            
            public class C {
            
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
        scan("a.b.C", INPUT0);
    }

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            
            import java.util.List;
            
            public class C {
            
                private Object[][] makeArray(List<Object[]> list) {
                    return list.toArray(new Object[list.size()][4]);
                }
            
            }
            """;

    @Test
    public void test1() {
        scan("a.b.C", INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            
            import java.util.ArrayList;
            import java.util.Map;
            
            public class C {
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
        scan("a.b.C", INPUT2);
    }

    @Language("java")
    private static final String CAN_BE_SERIALIZED = """
            package a;
            import java.util.List;
            
            public interface CanBeSerialized {
                boolean isX();
                List<String> list();
            }
            
            """;
    @Language("java")
    private static final String SERIALIZER = """
            package a;
            public abstract class Serializer<X> {
                protected X element;
            
                @Override
                public String toString() {
                    return element.toString(); // there are no type bounds here
                }
            }
            """;

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            
            import a.CanBeSerialized;
            import a.Serializer;
            
            public class C<E extends CanBeSerialized> extends Serializer<E> {
            
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
        scan(false, "a.CanBeSerialized", CAN_BE_SERIALIZED, "a.Serializer",
                SERIALIZER, "a.b.C", INPUT3);
    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            
            import java.io.Serializable;
            import java.util.List;
            
            /*
            UnboundMethodParameterType in combination with erasure; arrayPenalty
             */
            public class CC {
            
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
       scan("a.b.CC", INPUT5);
    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            
            import java.util.Arrays;
            
            public class C {
            
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
                    return Arrays.compare(multiExpression.expressions(), ((C) v).multiExpression.expressions());
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
            
            public class C {
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
        scan("a.b.C", INPUT8);
    }

    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            
            import java.util.Arrays;
            
            public class C {
            
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
        scan("a.b.C", INPUT9);
    }

}
