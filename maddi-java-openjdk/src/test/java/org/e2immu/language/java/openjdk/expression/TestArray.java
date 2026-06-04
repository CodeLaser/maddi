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

package org.e2immu.language.java.openjdk.expression;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestArray extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class C {
                public C(String s) {
                }
            
                private C[] copiesOfMyself;
            
                private void make() {
                    copiesOfMyself = new C[3];
                }
            
                C get(int i) {
                    return copiesOfMyself[i];
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = scan("a.b.C", INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
           
            public class C {
            
                public void method() {
                    int[] a = new int[3];
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo C = scan("a.b.C", INPUT2);
    }


    @Language("java")
    private static final String INPUT3 = """
            class X {
                public static long[] method(Long[] list) {
                  long[] result = new long[list.length];
                  int i = 0;
                  for (long v : list) {
                    result[i++] = v;
                  }
                  return result;
                }
            }
            """;

    @Language("java")
    private static final String INPUT3B = """
            class X {
                public static long[] method(Long list[]) {
                  long result[] = new long[list.length];
                  int i = 0;
                  for (long v : list) {
                    result[i++] = v;
                  }
                  return result;
                }
            }
            """;


    @Language("java")
    private static final String INPUT3C = """
            class X {
                public static long[] method(Long list[]) {
                  int i = 0;
                  long j=1, result[] = new long[list.length];
                  for (long v : list) {
                    result[i++] = v;
                  }
                  return result;
                }
            }
            """;

    @Language("java")
    private static final String OUTPUT3 = "class X{public static long[] method(Long[] list){long[] result=new long[list.length];int i=0;for(long v:list){result[i++]=v;}return result;}}";

    @Language("java")
    private static final String OUTPUT3C = "class X{public static long[] method(Long[] list){int i=0;long j=1,result[]=new long[list.length];for(long v:list){result[i++]=v;}return result;}}";

    @Test
    public void test3() {
        TypeInfo typeInfo = scan("X", INPUT3);
        assertEquals(OUTPUT3, typeInfo.print(runtime.qualificationQualifyFromPrimaryType()).toString());
    }

    @Test
    public void test3B() {
        TypeInfo typeInfo = scan("X", INPUT3B);
        assertEquals(OUTPUT3, typeInfo.print(runtime.qualificationQualifyFromPrimaryType()).toString());
    }

    @Test
    public void test3C() {
        TypeInfo typeInfo = scan("X", INPUT3C);
        assertEquals(OUTPUT3C, typeInfo.print(runtime.qualificationQualifyFromPrimaryType()).toString());
    }

    @Language("java")
    private static final String INPUT4 = """
            class Test {
                public static int method(String[][] array) {
                    return array[0].length;
                }
            }
            """;

    @Test
    public void test4() {
        scan("Test", INPUT4);
    }

    @Language("java")
    private static final String INPUT5 = """
            class Test {
                public static String dot(final double[] r1, final double[] r2) {
                    return r1.length + " " + r2.
                           length;
                }
            }
            """;

    @DisplayName("length on a different line")
    @Test
    public void test5() {
        scan("Test", INPUT5);
    }

    @Language("java")
    private static final String INPUT6 = """
            class Test {
                public static double[] copy(double[] in) {
                    return in.clone();
                }
            }
            """;

    @DisplayName("clone method on array")
    @Test
    public void test6() {
        scan("Test", INPUT6);
    }
}
