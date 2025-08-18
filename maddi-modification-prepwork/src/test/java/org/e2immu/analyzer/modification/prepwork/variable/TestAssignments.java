package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.Assignments;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestAssignments extends CommonTest {


    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                static int method(String in) {
                    int i = in.length();
                    int j;
                    int k;
                    if(i > 0) {
                        j = 1;
                        k = 2;
                        if(i > 1) {
                            return k;
                        }
                    } else {
                        j = 3;
                    }
                    return j;
                }
            }
            """;

    @DisplayName("basics")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);
        assertEquals(5, vdMethod.knownVariableNames().size());

        assertEquals("a.b.X.method(String), a.b.X.method(String):0:in, i, j, k",
                vdMethod.knownVariableNamesToString());
        VariableInfo inVi = vdMethod.variableInfo(method.parameters().get(0).fullyQualifiedName());
        assertEquals("in", inVi.variable().simpleName());
        assertEquals("0", inVi.reads().toString());
        assertEquals("D:-, A:[]", inVi.assignments().toString());

        VariableInfo iVi = vdMethod.variableInfo("i");
        assertEquals("3-E, 3.0.2-E", iVi.reads().toString());
        assertEquals("i", iVi.variable().simpleName());
        Assignments iA = iVi.assignments();
        assertEquals("D:0, A:[0]", iA.toString());

        IfElseStatement ifElseStatement = (IfElseStatement) method.methodBody().statements().get(3);
        Statement j1 = ifElseStatement.block().statements().get(0);
        VariableData vdJ1 = VariableDataImpl.of(j1);
        VariableInfo jViJ1 = vdJ1.variableInfo("j");
        assertEquals("-", jViJ1.reads().toString());
        assertEquals("D:1, A:[3.0.0]", jViJ1.assignments().toString());

        Statement j3 = ifElseStatement.elseBlock().statements().get(0);
        VariableData vdJ3 = VariableDataImpl.of(j3);
        VariableInfo jViJ3 = vdJ3.variableInfo("j");
        assertEquals("-", jViJ3.reads().toString());
        assertEquals("D:1, A:[3.1.0]", jViJ3.assignments().toString());

        VariableData vdJIf = VariableDataImpl.of(ifElseStatement);
        VariableInfo jViJIf = vdJIf.variableInfo("j");
        assertEquals("-", jViJIf.reads().toString());
        assertEquals("D:1, A:[3.0.0, 3.1.0, 3=M]", jViJIf.assignments().toString());

        VariableInfo jVi = vdMethod.variableInfo("j");
        assertEquals("j", jVi.variable().simpleName());
        assertEquals("4", jVi.reads().toString());
        Assignments jA = jVi.assignments();
        assertEquals("D:1, A:[3.0.0, 3.1.0, 3=M]", jA.toString());

        VariableInfo kVi = vdMethod.variableInfo("k");
        assertEquals("k", kVi.variable().simpleName());
        assertEquals("3.0.2.0.0", kVi.reads().toString());
        Assignments kA = kVi.assignments();
        assertEquals("D:2, A:[3.0.1]", kA.toString());

        VariableInfo rv = vdMethod.variableInfo(method.fullyQualifiedName());
        assertEquals("-", rv.reads().toString());
        assertEquals("D:-, A:[3.0.2.0.0, 4]", rv.assignments().toString());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X {
                static int method(String in) {
                    for(int i=0; i<in.length(); i++) {
                        if(in.charAt(i) == '0') {
                            return i;
                        }
                    }
                    return -1;
                }
            }
            """;

    @DisplayName("for-loop, local definition")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);
        // loop variable 'i' should not be known to the method
        assertEquals(2, vdMethod.knownVariableNames().size());
        assertEquals("a.b.X.method(String), a.b.X.method(String):0:in", vdMethod.knownVariableNamesToString());

        VariableInfo inVi = vdMethod.variableInfo(method.parameters().get(0).fullyQualifiedName());
        assertEquals("in", inVi.variable().simpleName());
        assertEquals("0-E, 0.0.0-E, 0;E", inVi.reads().toString());
        assertEquals("D:-, A:[]", inVi.assignments().toString());

        Statement s0 = method.methodBody().statements().get(0);
        VariableData vd0 = VariableDataImpl.of(s0);
        VariableInfo iVi = vd0.variableInfo("i");
        assertEquals("i", iVi.variable().simpleName());
        assertFalse(vd0.variableInfoContainerOrNull("i").hasMerge());
        assertEquals("0-E, 0:E, 0;E", iVi.reads().toString()); // is not a merge, so we cannot see the read in 'return i'
        Assignments iA = iVi.assignments();
        assertEquals("D:0+E, A:[0+E, 0:E]", iA.toString());

        Statement s000 = s0.block().statements().get(0);
        VariableData vd000 = VariableDataImpl.of(s000);
        assertTrue(vd000.variableInfoContainerOrNull("i").hasMerge());
        VariableInfo iVi000 = vd000.variableInfo("i");
        assertEquals("0-E, 0.0.0-E, 0.0.0.0.0, 0:E, 0;E", iVi000.reads().toString());

        VariableInfo rvVi = vdMethod.variableInfo(method.fullyQualifiedName());
        assertEquals("return method", rvVi.variable().simpleName());
        Assignments rvA = rvVi.assignments();
        assertEquals("D:-, A:[0.0.0.0.0, 1]", rvA.toString());
    }


    @Language("java")
    private static final String INPUT2b = """
            package a.b;
            class X {
                static int method(String in) {
                    for(int i=0; i<in.length(); i++) {
                        System.out.println(i);
                        for(int j=i; j<in.length(); j++) {
                            if(in.charAt(j) == '?') return i+j;
                        }
                    }
                    return -1;
                }
            }
            """;

    @DisplayName("nested for-loops, local definition")
    @Test
    public void test2b() {
        TypeInfo X = javaInspector.parse(INPUT2b);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);
        assertEquals(3, vdMethod.knownVariableNames().size());

        assertEquals("a.b.X.method(String), a.b.X.method(String):0:in, java.lang.System.out",
                vdMethod.knownVariableNamesToString());
        VariableInfo inVi = vdMethod.variableInfo(method.parameters().get(0).fullyQualifiedName());
        assertEquals("in", inVi.variable().simpleName());
        assertEquals("0-E, 0.0.1-E, 0.0.1.0.0-E, 0.0.1;E, 0;E", inVi.reads().toString());
        assertEquals("D:-, A:[]", inVi.assignments().toString());

        assertFalse(vdMethod.isKnown("i"));

        Statement s0 = method.methodBody().statements().get(0);
        VariableData vd0 = VariableDataImpl.of(s0);
        VariableInfo iVi = vd0.variableInfo("i");
        assertEquals("i", iVi.variable().simpleName());
        Assignments iA = iVi.assignments();
        assertEquals("D:0+E, A:[0+E, 0:E]", iA.toString());

        Statement s001 = s0.block().statements().get(1);
        VariableData vd001 = VariableDataImpl.of(s001);
        assertEquals("a.b.X.method(String), a.b.X.method(String):0:in, i, j, java.lang.System.out", vd001.knownVariableNamesToString());
        VariableInfo jVi = vd001.variableInfo("j");
        assertEquals("j", jVi.variable().simpleName());
        Assignments jA = jVi.assignments();
        assertEquals("D:0.0.1+E, A:[0.0.1+E, 0.0.1:E]", jA.toString());

        assertTrue(jA.between("0", "1"));
        assertTrue(jA.between("0.0.1", "1"));
        assertTrue(jA.between("0.0.1+E", "1"));
        assertFalse(jA.between("0", "0.0.0"));
        assertTrue(jA.between("0.0.1+E", "0.0.1:E"));
        assertFalse(jA.between("0.0.1+E", "0.0.1+E"));
        assertFalse(jA.between("0.0.1:E", "0.0.1:E"));
        assertTrue(jA.between("0.0.1:E", "0.0.1~"));
    }


    @Language("java")
    private static final String INPUT2c = """
            package a.b;
            class X {
                static int method(int[] ints) {
                    for(int i: ints) {
                        System.out.println(i);
                    }
                    return -1;
                }
            }
            """;

    @DisplayName("forEach")
    @Test
    public void test2c() {
        TypeInfo X = javaInspector.parse(INPUT2c);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);

        // i is not visible at the method level
        assertEquals(3, vdMethod.knownVariableNames().size());
        assertFalse(vdMethod.isKnown("i"));
        assertEquals("a.b.X.method(int[]), a.b.X.method(int[]):0:ints, java.lang.System.out",
                vdMethod.knownVariableNamesToString());

        VariableInfo intsVi = vdMethod.variableInfo(method.parameters().get(0).fullyQualifiedName());
        assertEquals("ints", intsVi.variable().simpleName());
        assertEquals("0-E", intsVi.reads().toString());
        assertEquals("D:-, A:[]", intsVi.assignments().toString());

        Statement s0 = method.methodBody().statements().get(0);
        VariableData vd0 = VariableDataImpl.of(s0);
        VariableInfo iVi = vd0.variableInfo("i");
        assertEquals("i", iVi.variable().simpleName());
        Assignments iA = iVi.assignments();
        assertEquals("D:0+E, A:[0+E]", iA.toString());
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class X {
                int method(String in) {
                    int i;
                    int j;
                    for(i=0; i<in.length(); i++) {
                        System.out.println(i);
                        for(j=i; j<in.length(); j++) {
                            if(in.charAt(j) == '!') return i+j;
                        }
                    }
                    j=in.length();
                    for(i=0; i<j; i++) {
                        System.out.println(i+" "+j);
                    }
                    return -1;
                }
            }
            """;

    @DisplayName("re-use of variables")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);
        assertEquals(5, vdMethod.knownVariableNames().size());

        assertEquals("a.b.X.method(String), a.b.X.method(String):0:in, i, j, java.lang.System.out",
                vdMethod.knownVariableNamesToString());
        VariableInfo inVi = vdMethod.variableInfo(method.parameters().get(0).fullyQualifiedName());
        assertEquals("in", inVi.variable().simpleName());
        assertEquals("2-E, 2.0.1-E, 2.0.1.0.0-E, 2.0.1;E, 2;E, 3", inVi.reads().toString());
        assertEquals("D:-, A:[]", inVi.assignments().toString());

        VariableInfo iVi = vdMethod.variableInfo("i");
        assertEquals("i", iVi.variable().simpleName());
        Assignments iA = iVi.assignments();
        assertEquals("D:0, A:[2+E, 2:E, 4+E, 4:E]", iA.toString());

        VariableInfo jVi = vdMethod.variableInfo("j");
        assertEquals("j", jVi.variable().simpleName());
        Assignments jA = jVi.assignments();
        assertEquals("D:1, A:[2.0.1+E, 2.0.1:E, 3]", jA.toString());
    }


    @Language("java")
    private static final String INPUT4 = """
            import java.util.Vector;
            public class X {
                public void quickSort(Vector elements, int lowIndex, int highIndex) {
                    int lowToHighIndex;
                    int highToLowIndex;
                    int pivotIndex;
                    Comparable pivotValue;
                    Comparable lowToHighValue;
                    Comparable highToLowValue;
                    Comparable parking;
                    int newLowIndex;
                    int newHighIndex;
                    int compareResult;
                    lowToHighIndex = lowIndex;
                    highToLowIndex = highIndex;
                    pivotIndex = (lowToHighIndex + highToLowIndex) / 2;
                    pivotValue = (Comparable) elements.elementAt(pivotIndex);
                    newLowIndex = highIndex + 1;
                    newHighIndex = lowIndex - 1;
                    while ((newHighIndex + 1) < newLowIndex) {
                        lowToHighValue = (Comparable) elements.elementAt(lowToHighIndex);
                        while (lowToHighIndex < newLowIndex & lowToHighValue.compareTo(pivotValue) < 0) {
                            newHighIndex = lowToHighIndex;
                            lowToHighIndex++;
                            lowToHighValue = (Comparable) elements.elementAt(lowToHighIndex);
                        }
                        highToLowValue = (Comparable) elements.elementAt(highToLowIndex);
                        while (newHighIndex <= highToLowIndex & (highToLowValue.compareTo(pivotValue) > 0)) {
                            newLowIndex = highToLowIndex;
                            highToLowIndex--;
                            highToLowValue = (Comparable) elements.elementAt(highToLowIndex);
                        }
                        if (lowToHighIndex == highToLowIndex) {
                            newHighIndex = lowToHighIndex;
                        } else if (lowToHighIndex < highToLowIndex) {
                            int result = lowToHighValue.compareTo(highToLowValue);
                            boolean bCompareResult = (result >= 0);
                            if (bCompareResult) {
                                parking = lowToHighValue;
                                elements.setElementAt(highToLowValue, lowToHighIndex);
                                elements.setElementAt(parking, highToLowIndex);
                                newLowIndex = highToLowIndex;
                                newHighIndex = lowToHighIndex;
                                lowToHighIndex++;
                                highToLowIndex--;
                            }
                        }
                    }
                    if (lowIndex < newHighIndex) {
                        this.quickSort(elements, lowIndex, newHighIndex);
                    }
                    if (newLowIndex < highIndex) {
                        this.quickSort(elements, newLowIndex, highIndex);
                    }
                }
            }
            """;

    @DisplayName("complex example with nested while-loops")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        MethodInfo method = X.findUniqueMethod("quickSort", 3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);
        assertEquals(14, vdMethod.knownVariableNames().size());

        assertEquals("""
                        X.quickSort(java.util.Vector,int,int):0:elements, X.quickSort(java.util.Vector,int,int):1:lowIndex, \
                        X.quickSort(java.util.Vector,int,int):2:highIndex, X.this, compareResult, highToLowIndex, \
                        highToLowValue, lowToHighIndex, lowToHighValue, newHighIndex, newLowIndex, parking, \
                        pivotIndex, pivotValue\
                        """,
                vdMethod.knownVariableNamesToString());

        VariableInfo elementsVi = vdMethod.variableInfo(method.parameters().get(0).fullyQualifiedName());
        assertEquals("elements", elementsVi.variable().simpleName());
        assertEquals("13, 16.0.0, 16.0.1.0.2, 16.0.2, 16.0.3.0.2, 16.0.4.1.0.0.2.0.1, 16.0.4.1.0.0.2.0.2, 17.0.0, 18.0.0", elementsVi.reads().toString());
        assertEquals("D:-, A:[]", elementsVi.assignments().toString());

        VariableInfo lowIndexVi = vdMethod.variableInfo(method.parameters().get(1).fullyQualifiedName());
        assertEquals("lowIndex", lowIndexVi.variable().simpleName());
        assertEquals("10, 15, 17-E, 17.0.0", lowIndexVi.reads().toString());
        assertEquals("D:-, A:[]", lowIndexVi.assignments().toString());

        VariableInfo highIndexVi = vdMethod.variableInfo(method.parameters().get(2).fullyQualifiedName());
        assertEquals("highIndex", highIndexVi.variable().simpleName());
        assertEquals("11, 14, 18-E, 18.0.0", highIndexVi.reads().toString());
        assertEquals("D:-, A:[]", highIndexVi.assignments().toString());

        VariableInfo newHighIndexVi = vdMethod.variableInfo("newHighIndex");
        assertEquals("newHighIndex", newHighIndexVi.variable().simpleName());
        assertEquals("D:08, A:[15, 16.0.1.0.0, 16.0.4.0.0, 16.0.4.1.0.0.2.0.4]",
                newHighIndexVi.assignments().toString());

        VariableInfo newLowIndexVi = vdMethod.variableInfo("newLowIndex");
        assertEquals("newLowIndex", newLowIndexVi.variable().simpleName());
        assertEquals("D:07, A:[14, 16.0.3.0.0, 16.0.4.1.0.0.2.0.3]", newLowIndexVi.assignments().toString());

        VariableInfo highToLowVi = vdMethod.variableInfo("highToLowIndex");
        assertEquals("12, 16.0.2, 16.0.3-E, 16.0.3.0.0, 16.0.3.0.1, 16.0.3.0.2, 16.0.3;E, 16.0.4-E, 16.0.4.1.0-E, 16.0.4.1.0.0.2.0.2, 16.0.4.1.0.0.2.0.3, 16.0.4.1.0.0.2.0.6", highToLowVi.reads().toString());
        assertEquals("D:01, A:[11, 16.0.3.0.1, 16.0.4.1.0.0.2.0.6]", highToLowVi.assignments().toString());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            class X {
                static void method(String in) {
                    if(in == null) {
                        throw new UnsupportedOperationException();
                    } else if(in.length() == 1) {
                        return;
                    }
                    System.out.println("Reachable");
                }
            }
            """;

    @DisplayName("exit, not all if-else branches")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);
        assertEquals(3, vdMethod.knownVariableNames().size());

        assertEquals("a.b.X.method(String), a.b.X.method(String):0:in, java.lang.System.out",
                vdMethod.knownVariableNamesToString());
        VariableInfo rvVi = vdMethod.variableInfo(method.fullyQualifiedName());
        assertEquals("D:-, A:[0.0.0, 0.1.0.0.0]", rvVi.assignments().toString());
        assertTrue(rvVi.hasBeenDefined("0.0.0"));
        assertTrue(rvVi.hasBeenDefined("0.0.1")); // fictitious here
        assertTrue(rvVi.hasBeenDefined("0.1.0.0.0"));
        assertFalse(rvVi.hasBeenDefined("1"));
        assertFalse(rvVi.hasBeenDefined("2.0.1")); // fictitious
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            class X {
                static void method(String in) {
                    if(in == null) {
                        throw new UnsupportedOperationException();
                    } else if(in.length() == 1) {
                        return;
                    } else {
                        System.out.println("Reachable");
                        return;
                    }
                    System.out.println("unreachable, illegal java");
                }
            }
            """;

    @DisplayName("exit, all if-else branches covered")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);
        assertEquals(3, vdMethod.knownVariableNames().size());

        assertEquals("a.b.X.method(String), a.b.X.method(String):0:in, java.lang.System.out",
                vdMethod.knownVariableNamesToString());
        VariableInfo rvVi = vdMethod.variableInfo(method.fullyQualifiedName());
        assertEquals("D:-, A:[0.0.0, 0.1.0.0.0, 0.1.0.1.1, 0.1.0=M, 0=M]", rvVi.assignments().toString());
        assertTrue(rvVi.hasBeenDefined("0.0.0"));
        assertTrue(rvVi.hasBeenDefined("0.1.0.0.0"));
        assertTrue(rvVi.hasBeenDefined("0.1.0.0.1")); // fictitious
        assertFalse(rvVi.hasBeenDefined("0.1.0.1.0"));
        assertTrue(rvVi.hasBeenDefined("0.1.0.1.1"));
        assertTrue(rvVi.hasBeenDefined("1"));
        assertTrue(rvVi.hasBeenDefined("2.0.1")); // fictitious
    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            public class X {
                private final Object o = new Object();
                public int method(String in) {
                    System.out.println(in);
                    synchronized (o) {
                        return in.length();
                    }
                    System.out.println("unreachable, illegal java");
                }
            }
            """;

    @DisplayName("exit, synchronized")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertEquals("a.b.X.method(String), a.b.X.method(String):0:in, a.b.X.o, a.b.X.this, java.lang.System.out",
                vdMethod.knownVariableNamesToString());

        VariableInfo rvVi = vdMethod.variableInfo(method.fullyQualifiedName());
        assertEquals("D:-, A:[1.0.0, 1=M]", rvVi.assignments().toString());
        assertFalse(rvVi.hasBeenDefined("0"));
        assertFalse(rvVi.hasBeenDefined("0.0.0")); // fictitious
        assertFalse(rvVi.hasBeenDefined("1"));
        assertFalse(rvVi.hasBeenDefined("1-E"));
        assertTrue(rvVi.hasBeenDefined("1=M"));
        assertTrue(rvVi.hasBeenDefined("1.0.0"));
        assertTrue(rvVi.hasBeenDefined("1.0.1")); // fictitious
        assertTrue(rvVi.hasBeenDefined("2"));
    }


    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            public class X {
                public static int method(String in) {
                    System.out.println(in);
                    while (true) {
                        return in.length();
                    }
                    System.out.println("unreachable, illegal java");
                }
            }
            """;

    @DisplayName("exit, while(true)")
    @Test
    public void test8() {
        TypeInfo X = javaInspector.parse(INPUT8);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertEquals("a.b.X.method(String), a.b.X.method(String):0:in, java.lang.System.out",
                vdMethod.knownVariableNamesToString());

        VariableInfo rvVi = vdMethod.variableInfo(method.fullyQualifiedName());
        assertEquals("D:-, A:[1.0.0, 1=M]", rvVi.assignments().toString());
        assertFalse(rvVi.hasBeenDefined("0"));
        assertFalse(rvVi.hasBeenDefined("0.0.0")); // fictitious
        assertFalse(rvVi.hasBeenDefined("1"));
        assertFalse(rvVi.hasBeenDefined("1-E"));
        assertTrue(rvVi.hasBeenDefined("1=M"));
        assertTrue(rvVi.hasBeenDefined("1.0.0"));
        assertTrue(rvVi.hasBeenDefined("1.0.1")); // fictitious
        assertTrue(rvVi.hasBeenDefined("2"));
    }


    @Language("java")
    private static final String INPUT8b = """
            package a.b;
            public class X {
                public static int method(String in) {
                    int i=0;
                    while (i<in.length()) {
                        return in.length();
                    }
                    System.out.println("reachable");
                    return 0;
                }
            }
            """;

    @DisplayName("exit, while(real condition)")
    @Test
    public void test8b() {
        TypeInfo X = javaInspector.parse(INPUT8b);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertEquals("a.b.X.method(String), a.b.X.method(String):0:in, i, java.lang.System.out",
                vdMethod.knownVariableNamesToString());

        VariableInfo rvVi = vdMethod.variableInfo(method.fullyQualifiedName());
        assertEquals("D:-, A:[1.0.0, 3]", rvVi.assignments().toString());
        assertFalse(rvVi.hasBeenDefined("0"));
        assertFalse(rvVi.hasBeenDefined("0.0.0")); // fictitious
        assertFalse(rvVi.hasBeenDefined("1"));
        assertTrue(rvVi.hasBeenDefined("1.0.0"));
        assertTrue(rvVi.hasBeenDefined("1.0.1")); // fictitious
        assertFalse(rvVi.hasBeenDefined("2"));
        assertTrue(rvVi.hasBeenDefined("3"));
    }


    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            public class X {
                public static int method(int j) {
                    int i=0;
                    if(j==1) {
                        System.out.println(i);
                    }
                    i=1;
                    return i;
                }
            }
            """;

    @DisplayName("simple re-assigns")
    @Test
    public void test9() {
        TypeInfo X = javaInspector.parse(INPUT9);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertEquals("a.b.X.method(int), a.b.X.method(int):0:j, i, java.lang.System.out",
                vdMethod.knownVariableNamesToString());

        VariableInfo iVi = vdMethod.variableInfo("i");
        assertEquals("1.0.0, 3", iVi.reads().toString());
        assertEquals("D:0, A:[0, 2]", iVi.assignments().toString());
        assertTrue(iVi.assignments().hasBeenAssignedAfterFor("1=M", "3", false));
        assertTrue(iVi.assignments().hasBeenAssignedAfterFor("1=M", "3.0.1", false));

        Statement s2 = method.methodBody().statements().get(2);
        VariableData vd2 = VariableDataImpl.of(s2);
        VariableInfo iVi2 = vd2.variableInfo("i");
        assertEquals("1.0.0", iVi2.reads().toString());
        assertEquals("D:0, A:[0, 2]", iVi2.assignments().toString());


    }

    @Language("java")
    public static final String INPUT10 = """
            package a.b;
            import java.io.FileNotFoundException;import java.io.IOException;
            public class X {
                public static String method(Exception exception) {
                    if (exception instanceof RuntimeException e) {
                        return e.getMessage();
                    } else if(exception instanceof FileNotFoundException e) {
                        return "io" + e.getMessage();
                    }
                    if (exception instanceof IOException e) {
                        return "uoe"+e;
                    }
                    return "?";
                }
            }
            """;

    public static String method10(Exception exception) {
        if (exception instanceof RuntimeException e) {
            return e.getMessage();
        } else if (exception instanceof FileNotFoundException e) {
            return "io" + e.getMessage();
        }
        if (exception instanceof IOException e) {
            return "uoe" + e;
        }
        return "?";
    }

    @DisplayName("restriction of pattern variables")
    @Test
    public void test10() {
        TypeInfo X = javaInspector.parse(INPUT10);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
    }


    @Language("java")
    public static final String INPUT11 = """
            package a.b;
            import java.io.IOException;
            public class X {
                public static String method(Exception exception) {
                    if(!(exception instanceof RuntimeException e) || e.getMessage().isEmpty()) {
                        if(exception instanceof IOException e) return "io" + e.getMessage();
                    } else {
                        return e.getMessage();
                    }
                    if(exception instanceof UnsupportedOperationException e) {
                        return "uoe"+e;
                    }
                    return "?";
                }
            }
            """;

    public static String method11(Exception exception) {
        if (!(exception instanceof RuntimeException e) || e.getMessage().isEmpty()) {
            if (exception instanceof IOException e) return "io" + e.getMessage();
        } else {
            return e.getMessage();
        }
        if (exception instanceof UnsupportedOperationException e) {
            return "uoe" + e;
        }
        return "?";
    }

    @DisplayName("restriction of pattern variables, negative")
    @Test
    public void test11() {
        TypeInfo X = javaInspector.parse(INPUT11);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
    }


    @Language("java")
    public static final String INPUT12 = """
            import java.util.List;
            class X {
                private int method(int iIn, int len, List<Integer> list) {
                    int i = iIn;
                    if (i < len) {
                        int j = list.get(i++);
                    }
                    return i;
                }
            }
            """;

    @DisplayName("++ in method call argument")
    @Test
    public void test12() {
        TypeInfo X = javaInspector.parse(INPUT12);
        MethodInfo method = X.findUniqueMethod("method", 3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);

        Statement lastStatement = method.methodBody().lastStatement();
        VariableData vdLast = VariableDataImpl.of(lastStatement);
        Assignments assignments = vdLast.variableInfo("i").assignments();
        assertEquals("D:0, A:[0, 1.0.0]", assignments.toString());
    }
}
