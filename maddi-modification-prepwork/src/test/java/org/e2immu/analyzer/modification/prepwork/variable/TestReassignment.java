package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestReassignment extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                public static int method(int i, int j) {
                    int k = i*j;
                    if(k < 0) return -k;
                    k = -k;
                    if(k % 2 == 0) return k;
                    k = k + 1;
                    return k/2;
                }
            }
            """;

    @DisplayName("two unconditional re-assigns")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        MethodInfo method = X.findUniqueMethod("method", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        Statement s0 = method.methodBody().statements().get(0);
        VariableData vd0 = VariableDataImpl.of(s0);
        VariableInfo vi0k = vd0.variableInfo("k");
        assertEquals("D:0, A:[0]", vi0k.assignments().toString());

        Statement s2 = method.methodBody().statements().get(2);
        VariableData vd2 = VariableDataImpl.of(s2);
        VariableInfo vi2k = vd2.variableInfo("k");
        assertEquals("D:0, A:[0, 2]", vi2k.assignments().toString());

        Statement s5 = method.methodBody().statements().get(5);
        VariableData vd5 = VariableDataImpl.of(s5);
        VariableInfo vi5k = vd5.variableInfo("k");
        assertEquals("D:0, A:[0, 2, 4]", vi5k.assignments().toString());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X {
                public static int method(int i, int j) {
                    int k;
                    if(i < 0) {
                        k = j+i;
                    } else {
                        k = j-i;
                    }
                    if(k % 2 == 0) {
                        k += 1;
                        return k;
                    }
                    return k/2;
                }
            }
            """;

    @DisplayName("one conditional re-assign, with return")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        MethodInfo method = X.findUniqueMethod("method", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        Statement s1 = method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo vi1k = vd1.variableInfo("k");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M]", vi1k.assignments().toString());

        Statement s200 = method.methodBody().statements().get(2).block().statements().get(0);
        VariableData vd200 = VariableDataImpl.of(s200);
        VariableInfo vi200k = vd200.variableInfo("k");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M, 2.0.0]", vi200k.assignments().toString());

        Statement s3 = method.methodBody().statements().get(3);
        VariableData vd3 = VariableDataImpl.of(s3);
        VariableInfo vi3k = vd3.variableInfo("k");
        // NOTE: the 2=M, which is unexpected/unwanted?
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M, 2.0.0, 2=M]", vi3k.assignments().toString());
    }


    @Language("java")
    private static final String INPUT2b = """
            package a.b;
            class X {
                public static int method(int i, int j) {
                    int k;
                    if(i < 0) {
                        k = j+i;
                    } else {
                        k = j-i;
                    }
                    if(k % 2 == 0) {
                        k += 1;
                        System.out.println("k="+k);
                    }
                    return k/2;
                }
            }
            """;

    @DisplayName("one conditional re-assign, without return")
    @Test
    public void test2b() {
        TypeInfo X = javaInspector.parse(INPUT2b);
        MethodInfo method = X.findUniqueMethod("method", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        Statement s1 = method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo vi1k = vd1.variableInfo("k");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M]", vi1k.assignments().toString());

        Statement s200 = method.methodBody().statements().get(2).block().statements().get(0);
        VariableData vd200 = VariableDataImpl.of(s200);
        VariableInfo vi200k = vd200.variableInfo("k");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M, 2.0.0]", vi200k.assignments().toString());

        Statement s3 = method.methodBody().statements().get(3);
        VariableData vd3 = VariableDataImpl.of(s3);
        VariableInfo vi3k = vd3.variableInfo("k");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M, 2.0.0]", vi3k.assignments().toString());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class X {
                public static int method(int i, int j) {
                    int k;
                    if(i < 0) {
                        k = j+i;
                    } else {
                        k = j-i;
                    }
                    if(k % 2 == 0) {
                        if(j > 0) {
                            k += 1;
                        } else {
                            k -= 1;
                        }
                        return k;
                    }
                    return k/2;
                }
            }
            """;

    @DisplayName("split conditional re-assign")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        MethodInfo method = X.findUniqueMethod("method", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        Statement s1 = method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo vi1k = vd1.variableInfo("k");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M]", vi1k.assignments().toString());

        Statement s201 = method.methodBody().statements().get(2).block().statements().get(1);
        VariableData vd201 = VariableDataImpl.of(s201);
        VariableInfo vi201k = vd201.variableInfo("k");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M, 2.0.0.0.0, 2.0.0.1.0, 2.0.0=M]",
                vi201k.assignments().toString());

        Statement s3 = method.methodBody().statements().get(3);
        VariableData vd3 = VariableDataImpl.of(s3);
        VariableInfo vi3k = vd3.variableInfo("k");
        // NOTE: the 2.0.0=M, 2=M, which is unexpected/unwanted?
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M, 2.0.0.0.0, 2.0.0.1.0, 2.0.0=M, 2=M]",
                vi3k.assignments().toString());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            class X {
                public static int method(int i, int j) {
                    int k;
                    if(i < 0) {
                        k = j+i;
                    } else {
                        k = j-i;
                    }
                    if(k % 2 == 0) {
                        if(j > 0) {
                            k += 1;
                            System.out.println("k = "+k);
                        }
                        return k;
                    }
                    return k/2;
                }
            }
            """;

    @DisplayName("split, partial conditional re-assign")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        MethodInfo method = X.findUniqueMethod("method", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        Statement s1 = method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo vi1k = vd1.variableInfo("k");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M]", vi1k.assignments().toString());

        Statement s201 = method.methodBody().statements().get(2).block().statements().get(1);
        VariableData vd201 = VariableDataImpl.of(s201);
        VariableInfo vi201k = vd201.variableInfo("k");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M, 2.0.0.0.0]",
                vi201k.assignments().toString());

        Statement s3 = method.methodBody().statements().get(3);
        VariableData vd3 = VariableDataImpl.of(s3);
        VariableInfo vi3k = vd3.variableInfo("k");
        // NOTE: no merge, because no exit
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M, 2.0.0.0.0]", vi3k.assignments().toString());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            class X {
                public static int method(int i, int j) {
                    int k;
                    if(i < 0) {
                        k = j+i;
                    } else {
                        k = j-i;
                    }
                    if(k % 2 == 0) {
                        if(j > 0) {
                            k += 1;
                            System.out.println("k = "+k);
                            k += 1;
                            System.out.println("k = "+k);
                        }
                        return k;
                    }
                    return k/2;
                }
            }
            """;

    @DisplayName("split, partial conditional, 2x re-assign")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        MethodInfo method = X.findUniqueMethod("method", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        Statement s1 = method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo vi1k = vd1.variableInfo("k");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M]", vi1k.assignments().toString());

        Statement s201 = method.methodBody().statements().get(2).block().statements().get(1);
        VariableData vd201 = VariableDataImpl.of(s201);
        VariableInfo vi201k = vd201.variableInfo("k");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M, 2.0.0.0.0, 2.0.0.0.2]",
                vi201k.assignments().toString());

        Statement s3 = method.methodBody().statements().get(3);
        VariableData vd3 = VariableDataImpl.of(s3);
        VariableInfo vi3k = vd3.variableInfo("k");
        // NOTE: no merge, because no exit
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M, 2.0.0.0.0, 2.0.0.0.2]", vi3k.assignments().toString());
    }

}
