package org.e2immu.parser.java;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestParseArray extends CommonTestParse {
    @Language("java")
    private static final String INPUT1 = """
            public class Array_1 {

                public Array_1(String s) {

                }

                private Array_1[] copiesOfMyself;

                private void make() {
                    copiesOfMyself = new Array_1[3];
                }

                Array_1 get(int i) {
                    return copiesOfMyself[i];
                }
            }
            """;

    @Test
    public void test1() {
        parse(INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            public class Array_2 {

                public void method() {
                    int[] a = new int[3];
                }
            }
            """;

    @Test
    public void test2() {
        parse(INPUT2);
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
                  long i = 0, result[] = new long[list.length];
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
    private static final String OUTPUT3C = "class X{public static long[] method(Long[] list){long i=0,result[]=new long[list.length];for(long v:list){result[i++]=v;}return result;}}";

    @Test
    public void test3() {
        TypeInfo typeInfo = parse(INPUT3);
        assertEquals(OUTPUT3, typeInfo.print(runtime.qualificationQualifyFromPrimaryType()).toString());
    }

    @Test
    public void test3B() {
        TypeInfo typeInfo = parse(INPUT3B);
        assertEquals(OUTPUT3, typeInfo.print(runtime.qualificationQualifyFromPrimaryType()).toString());
    }

    @Test
    public void test3C() {
        TypeInfo typeInfo = parse(INPUT3C);
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
        parse(INPUT4);
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
        parse(INPUT5);
    }
}
