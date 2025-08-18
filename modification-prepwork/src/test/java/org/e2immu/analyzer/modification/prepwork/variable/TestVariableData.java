package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ForStatement;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestVariableData extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
                void method1(String s) {
                    assert s != null;
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = javaInspector.parseReturnAll(INPUT, JavaInspectorImpl.FAIL_FAST).get(0);
        MethodInfo method1 = typeInfo.findUniqueMethod("method1", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(javaInspector.runtime());
        analyzer.doMethod(method1);
        VariableData vd = VariableDataImpl.of(method1);
        assert vd != null;

        // there is no This present
        assertEquals("a.b.C.method1(String):0:s", vd.knownVariableNamesToString());
        List<VariableInfo> vis = vd.variableInfoStream().toList();
        assertEquals(1, vis.size());

        VariableData statementVd = method1.methodBody().statements().get(0).analysis()
                .getOrNull(VariableDataImpl.VARIABLE_DATA, VariableData.class);
        assertSame(vd, statementVd);

        ParameterInfo pi = method1.parameters().get(0);
        VariableInfo vi0 = vd.variableInfo(pi.fullyQualifiedName());
        assertEquals("0", vi0.reads().toString());
        assertTrue(vi0.assignments().hasNotYetBeenAssigned());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class C {
                void method1(String s) {
                    assert s != null;
                    System.out.println(s);
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parseReturnAll(INPUT2, JavaInspectorImpl.FAIL_FAST).get(0);
        MethodInfo method1 = typeInfo.findUniqueMethod("method1", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(javaInspector.runtime());
        analyzer.doMethod(method1);

        VariableData vd = VariableDataImpl.of(method1);

        assertEquals("a.b.C.method1(String):0:s, java.lang.System.out", vd.knownVariableNamesToString());

        List<VariableInfo> vis = vd.variableInfoStream().toList();
        assertEquals(2, vis.size());

        Statement s0 = method1.methodBody().statements().get(0);
        VariableData vd0 = VariableDataImpl.of(s0);
        assertNotNull(vd0);
        Statement s1 = method1.methodBody().statements().get(1);
        VariableData vd1 = s1.analysis().getOrNull(VariableDataImpl.VARIABLE_DATA, VariableData.class);
        assertSame(vd, vd1);

        ParameterInfo pi = method1.parameters().get(0);
        VariableInfo vi0 = vd.variableInfo(pi.fullyQualifiedName());
        assertEquals("0, 1", vi0.reads().toString());
        assertTrue(vi0.assignments().hasNotYetBeenAssigned());
        VariableInfoContainer vic0 = vd.variableInfoContainerOrNull(pi.fullyQualifiedName());
        assertSame(vi0, vic0.best());
        assertSame(vi0, vic0.best(Stage.MERGE));
        assertSame(vi0, vic0.best(Stage.EVALUATION));
        VariableInfo vii = vic0.best(Stage.INITIAL);
        assertNotSame(vi0, vii);

        VariableInfoContainer vic00 = vd0.variableInfoContainerOrNull(pi.fullyQualifiedName());
        assertSame(vii, vic00.best());
        assertSame(vii, vic00.best(Stage.EVALUATION));
        assertNotSame(vii, vic00.getPreviousOrInitial());
        assertTrue(vic00.isInitial());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class C {
                int j;
                void method1(String s) {
                    assert s != null;
                    if(j > 0) {
                        System.out.println(s);
                    }
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parseReturnAll(INPUT3, JavaInspectorImpl.FAIL_FAST).get(0);
        MethodInfo method1 = typeInfo.findUniqueMethod("method1", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(javaInspector.runtime());
        analyzer.doMethod(method1);

        VariableData vd0 = method1.methodBody().statements().get(0).analysis().getOrNull(VariableDataImpl.VARIABLE_DATA,
                VariableData.class);

        VariableData vd = VariableDataImpl.of(method1);
        assert vd != null;
        assertEquals("a.b.C.j, a.b.C.method1(String):0:s, a.b.C.this, java.lang.System.out", vd.knownVariableNamesToString());

        List<VariableInfo> vis = vd.variableInfoStream().toList();
        assertEquals(4, vis.size());

        VariableInfoContainer vicJ = vd.variableInfoContainerOrNull("a.b.C.j");
        assertNotNull(vicJ);
        assertEquals("1-E", vicJ.best().reads().toString());
        assertFalse(vd0.isKnown(vicJ.variable().fullyQualifiedName()));

        VariableInfoContainer vicThis = vd.variableInfoContainerOrNull("a.b.C.this");
        assertNotNull(vicThis);
        VariableInfo this1E = vicThis.best(Stage.EVALUATION);
        assertEquals("1-E", this1E.reads().toString());
        VariableInfo this1M = vicThis.best();
        assertNotSame(this1E, this1M);
        assertEquals("1-E", this1M.reads().toString());
        // check that This is not yet present in statement 0
        assertFalse(vd0.isKnown(vicThis.variable().fullyQualifiedName()));

        VariableInfoContainer vicOut = vd.variableInfoContainerOrNull("java.lang.System.out");
        assertNotNull(vicOut);
        VariableInfo out1E = vicOut.best(Stage.EVALUATION);
        VariableInfo out1M = vicOut.best();
        assertNotSame(out1M, out1E);
        assertEquals("-", out1E.reads().toString());
        assertEquals("1.0.0", out1M.reads().toString());
        assertFalse(vd0.isKnown(vicOut.variable().fullyQualifiedName()));

        ReturnVariable rv = new ReturnVariableImpl(method1);
        assertFalse(vd.isKnown(rv.fullyQualifiedName()));
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            class C {
                int j;
                int method1(String s) {
                    assert s != null;
                    if(j > 0) {
                        System.out.println(s);
                    } else {
                        //noinspection ALL
                        int c = s.length();
                        j = c;
                    }
                    return j+1;
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = javaInspector.parseReturnAll(INPUT4, JavaInspectorImpl.FAIL_FAST).get(0);
        MethodInfo method1 = typeInfo.findUniqueMethod("method1", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(javaInspector.runtime());
        analyzer.doMethod(method1);

        VariableData vd = VariableDataImpl.of(method1);
        assert vd != null;

        Statement s111 = method1.methodBody().statements().get(1)
                .otherBlocksStream().findFirst().orElseThrow().statements().get(1);
        assertEquals("1.1.1", s111.source().index());

        VariableInfoContainer vicJ = vd.variableInfoContainerOrNull("a.b.C.j");
        assertNotNull(vicJ);
        VariableInfo viJ = vicJ.best();
        assertEquals("1-E, 2", viJ.reads().toString());
        assertEquals("D:-, A:[1.1.1]", viJ.assignments().toString());

        ReturnVariable rv = new ReturnVariableImpl(method1);

        Statement s2 = method1.methodBody().statements().get(2);
        VariableData vd2 = VariableDataImpl.of(s2);
        VariableInfo rvVi2 = vd2.variableInfo(rv.fullyQualifiedName());

        assertTrue(vd.isKnown(rv.fullyQualifiedName()));
        VariableInfo viRv = vd.variableInfo(rv.fullyQualifiedName());
        assertEquals("-", viRv.reads().toString());
        assertEquals("D:-, A:[2]", viRv.assignments().toString());

    }


    @Language("java")
    private static final String INPUT5 = """
            public class X {
                private static final boolean WANT_PROGRESS = true;
                public static short[] numbers;
                private static int progressCounter;
            
                public static void run() {
                    int i, j, l;
                    short NUMNUMBERS = 10;
                    numbers = new short[NUMNUMBERS];
                    int time = (int) System.currentTimeMillis();
                    for (i = 0; i < NUMNUMBERS; i++) {
                        numbers[i] = (short) (NUMNUMBERS - 1 - i);
                    }
                    for (i = 0; i < NUMNUMBERS; i++) { // 5
                        for (j = 0; j < NUMNUMBERS - i - 1; j++) { // 5.0.0
                            if (numbers[j] > numbers[j + 1]) { // 5.0.0.0.0
                                short temp = numbers[j];
                                numbers[j] = numbers[j + 1];
                                numbers[j + 1] = temp;
                            }
                        }
                        if (WANT_PROGRESS) {
                            System.out.println(i);
                        }
                    }
                    time = (int) System.currentTimeMillis() - time;
                    System.out.print(time);
                    System.out.print("End\\n");
                }
            }
            """;

    @Test
    public void test5() {
        TypeInfo typeInfo = javaInspector.parseReturnAll(INPUT5, JavaInspectorImpl.FAIL_FAST).get(0);
        MethodInfo method1 = typeInfo.findUniqueMethod("run", 0);
        PrepAnalyzer analyzer = new PrepAnalyzer(javaInspector.runtime());
        analyzer.doMethod(method1);

        VariableData vd = VariableDataImpl.of(method1);
        assert vd != null;

        ForStatement fs4 = (ForStatement) method1.methodBody().statements().get(4);
        VariableData vd4 = VariableDataImpl.of(fs4);
        VariableInfo i4 = vd4.variableInfo("i");
        assertEquals("4-E, 4.0.0, 4:E, 4;E", i4.reads().toString());
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.io.File;
            import java.util.Vector;
            
            public class X {
              public File method(String basename, String extension) {
                File dir = getTemporaryDirectory();
                File f;
                synchronized (temps) {
                  do f = new File(dir, basename + (int) (Math.random() * 999999) + extension);
                  while (temps.contains(f) || f.exists());
                  temps.addElement(f);
                }
                return f;
              }
            
              private File tempDir;
              private final Vector<File> temps = new Vector<>();
            
              public File getTemporaryDirectory() {
                return tempDir;
              }
            }
            """;

    // get set first, otherwise they appear out of order
    @DisplayName("accessors cause a variable to be created in the method calling them")
    @Test
    public void test6() {

        TypeInfo typeInfo = javaInspector.parse(INPUT6);
        PrepAnalyzer analyzer = new PrepAnalyzer(javaInspector.runtime());
        analyzer.doPrimaryType(typeInfo);
        MethodInfo getTemporaryDirectory = typeInfo.findUniqueMethod("getTemporaryDirectory", 0);
        FieldInfo tempDir = getTemporaryDirectory.getSetField().field();
        assertEquals("tempDir", tempDir.name());

        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().get(0));
        assert vd0 != null;

        FieldReference frTempDir = runtime.newFieldReference(tempDir);
        assertTrue(vd0.isKnown(frTempDir.fullyQualifiedName()));
    }


    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            
            public class X {
                public String method(String in) {
                  {
                     String s = in.toLowerCase();
                     {
                        System.out.println(s);
                     }
                  }
                  {
                     String s = in.toUpperCase();
                     {
                        System.out.println(s);
                     }
                  }
                }
            }
            """;

    @DisplayName("block scope and clear analysis")
    @Test
    public void test7() {

        TypeInfo typeInfo = javaInspector.parse(INPUT7);
        PrepAnalyzer analyzer = new PrepAnalyzer(javaInspector.runtime());
        analyzer.doPrimaryType(typeInfo);
        MethodInfo method = typeInfo.findUniqueMethod("method", 1);
        VariableData vd = VariableDataImpl.of(method.methodBody().lastStatement());
        assertEquals("a.b.X.method(String):0:in, java.lang.System.out", vd.knownVariableNamesToString());
        TranslationMap tm = runtime.newTranslationMapBuilder()
                .setClearAnalysis(true)
                .build();
        TypeInfo t2 = typeInfo.translate(tm).getFirst();
        assertNotSame(typeInfo, t2);
        assertEquals("""
                package a.b;
                public class X{public String method(String in){{String s=in.toLowerCase();{System.out.println(s);}}{String s=in.toUpperCase();{System.out.println(s);}}}}\
                """, t2.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        analyzer.doPrimaryType(t2);
    }


    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            import java.io.IOException;
            public class X {
                public String method(Exception in) {
                    String s;
                    if (in instanceof RuntimeException e) {
                        s = e.toString();
                    } else {
                        if (in instanceof IOException e1) {
                            s = "?";
                        } else {
                            String e2 = in.toString();
                            s = e2;
                        }
                    }
                    return s;
                }
            }
            """;

    @DisplayName("scope of pattern variables")
    @Test
    public void test8() {
        TypeInfo typeInfo = javaInspector.parse(INPUT8);
        PrepAnalyzer analyzer = new PrepAnalyzer(javaInspector.runtime());
        analyzer.doPrimaryType(typeInfo);
        MethodInfo method = typeInfo.findUniqueMethod("method", 1);

        IfElseStatement ifElse1 = (IfElseStatement) method.methodBody().statements().get(1);
        Statement s100 = ifElse1.block().statements().get(0);
        assertEquals("a.b.X.method(Exception):0:in, e, s",
                VariableDataImpl.of(s100).knownVariableNamesToString());

        IfElseStatement ifElse110 = (IfElseStatement) ifElse1.elseBlock().statements().get(0);
        Statement s11000 = ifElse110.block().statements().get(0);
        assertEquals("a.b.X.method(Exception):0:in, e1, s",
                VariableDataImpl.of(s11000).knownVariableNamesToString());

        Statement s11010 = ifElse110.elseBlock().statements().get(0);
        assertEquals("1.1.0.1.0", s11010.source().index());
        assertEquals("a.b.X.method(Exception):0:in, e2, s",
                VariableDataImpl.of(s11010).knownVariableNamesToString());
        Statement s11011 = ifElse110.elseBlock().statements().get(1);
        assertEquals("a.b.X.method(Exception):0:in, e2, s",
                VariableDataImpl.of(s11011).knownVariableNamesToString());

        VariableData vd110 = VariableDataImpl.of(ifElse110);
        assertEquals("a.b.X.method(Exception):0:in, e1, s", vd110.knownVariableNamesToString());

        VariableData vd1 = VariableDataImpl.of(ifElse1);
        assertEquals("a.b.X.method(Exception):0:in, e, s", vd1.knownVariableNamesToString());

        VariableData vd2 = VariableDataImpl.of(method.methodBody().statements().get(2));
        assertEquals("a.b.X.method(Exception), a.b.X.method(Exception):0:in, s", vd2.knownVariableNamesToString());
    }


    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            import java.io.IOException;
            public class X {
                public String method(Exception in) {
                    String s;
                    if (in instanceof RuntimeException e) {
                        s = e.toString();
                    } else {
                        if (in instanceof IOException e) {
                            s = "?" + e;
                        } else {
                            String e = in.toString();
                            s = e;
                        }
                    }
                    return s;
                }
            }
            """;

    @DisplayName("scope of pattern variables, now with same variable name")
    @Test
    public void test9() {
        TypeInfo typeInfo = javaInspector.parse(INPUT9);
        PrepAnalyzer analyzer = new PrepAnalyzer(javaInspector.runtime());
        analyzer.doPrimaryType(typeInfo);
    }

}
