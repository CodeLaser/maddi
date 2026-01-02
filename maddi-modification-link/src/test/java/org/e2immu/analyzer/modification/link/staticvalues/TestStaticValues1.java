package org.e2immu.analyzer.modification.link.staticvalues;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestStaticValues1 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            class X {
                int method() {
                    int j=3;
                    return j;
                }
            }
            """;

    @DisplayName("direct assignment")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = X.findUniqueMethod("method", 0);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        Statement s0 = method.methodBody().statements().getFirst();
        VariableData vd0 = VariableDataImpl.of(s0);

        VariableInfo vi0J = vd0.variableInfo("j");
        assertEquals("j←$_ce0", vi0J.linkedVariables().toString());
        assertEquals("[] --> -", mlv.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class X {
                int j;
                int k;
                X setJ(int jp) {
                    j=jp;
                    return this;
                }
                X setJK(int jp, int kp) {
                    j=jp;
                    k=kp;
                    return this;
                }
                static X method(int i) {
                    X x = new X().setJ(i);
                    return x;
                }
                static X methodC() {
                    X x = new X().setJ(3);
                    return x;
                }
                static X method2(int a, int b) {
                    X x = new X().setJK(a, b);
                    return x;
                }
            }
            """;

    @DisplayName("assignment to field")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo setJ = X.findUniqueMethod("setJ", 1);
        MethodLinkedVariables mlvSetJ = setJ.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(setJ));
        FieldInfo fieldJ = X.getFieldByName("j", true);
        {
            Statement s0 = setJ.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi0J = vd0.variableInfo(runtime.newFieldReference(fieldJ));
            assertEquals("this.j←0:jp", vi0J.linkedVariables().toString());
        }
        assertEquals("[0:jp→this.j] --> setJ←this,setJ.j←0:jp", mlvSetJ.toString());

        MethodInfo setJK = X.findUniqueMethod("setJK", 2);
        MethodLinkedVariables mlvSetJK = setJK.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(setJK));
        {
            Statement s0 = setJK.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi0J = vd0.variableInfo(runtime.newFieldReference(fieldJ));
            assertEquals("this.j←0:jp", vi0J.linkedVariables().toString());
        }
        assertEquals("[0:jp→this.j, 1:kp→this.k] --> setJK←this,setJK.j←0:jp,setJK.k←1:kp", mlvSetJK.toString());


        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        {
            VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
            assertEquals("a.b.X.j#scope16-15:16-21, a.b.X.method(int):0:i, x", vd0.knownVariableNamesToString());
            VariableInfo viX = vd0.variableInfo("x");
            assertEquals("x.j←0:i", viX.linkedVariables().toString());

            assertEquals("[-] --> method.j←0:i", mlv.toString());
        }
        MethodInfo methodC = X.findUniqueMethod("methodC", 0);
        MethodLinkedVariables mlvC = methodC.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(methodC));
        {
            VariableData vd0 = VariableDataImpl.of(methodC.methodBody().statements().getFirst());
            assertEquals("a.b.X.j#scope20-15:20-21, x", vd0.knownVariableNamesToString());
            VariableInfo viX = vd0.variableInfo("x");
            assertEquals("x.j←$_ce1", viX.linkedVariables().toString());

            // TODO should we have kept $_ce1?
            assertEquals("[] --> -", mlvC.toString());
        }
        MethodInfo method2 = X.findUniqueMethod("method2", 2);
        MethodLinkedVariables mlv2 = method2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method2));
        {
            VariableData vd0 = VariableDataImpl.of(method2.methodBody().statements().getFirst());
            assertEquals("a.b.X.method2(int,int):0:a, a.b.X.method2(int,int):1:b, x", vd0.knownVariableNamesToString());
            VariableInfo viX = vd0.variableInfo("x");
            assertEquals("x.j←0:a,x.k←1:b", viX.linkedVariables().toString());
            assertEquals("a, b", viX.linkedVariables().primaryAssigned().stream().map(Variable::simpleName)
                    .sorted().collect(Collectors.joining(", ")));
            assertEquals("[-, -] --> method2.j←0:a,method2.k←1:b", mlv2.toString());
        }

    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.Set;
            class X {
                static class R { int i; }
            
                int method(R r) {
                    r.i = 3;
                    return r.i+2;
                }
            }
            """;

    @DisplayName("one level deep")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        TypeInfo R = X.findSubType("R");
        FieldInfo iField = R.getFieldByName("i", true);

        MethodInfo method = X.findUniqueMethod("method", 1);
        ParameterInfo r = method.parameters().getFirst();
        {
            Statement s0 = method.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi0R = vd0.variableInfo(r);
            assertEquals("0:r.i←$_ce0", vi0R.linkedVariables().toString());
            Variable ce = vi0R.linkedVariables().stream().findFirst().orElseThrow().to();
            assertEquals("3", ((LocalVariable) ce).assignmentExpression().toString());
            VariableExpression scope = runtime.newVariableExpressionBuilder().setVariable(r).setSource(iField.source()).build();
            Variable ri = runtime.newFieldReference(iField, scope, iField.type());
            assertEquals("r.i", ri.toString());
            VariableInfo vi0Ri = vd0.variableInfo(ri);
            assertEquals("0:r.i←$_ce0", vi0Ri.linkedVariables().toString());
        }
        {
            Statement s1 = method.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);

            VariableInfo vi1Rv = vd1.variableInfo(method.fullyQualifiedName());
            assertEquals("-", vi1Rv.linkedVariables().toString());
        }
        MethodLinkedVariables mlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertTrue(mlv.isEmpty());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.Set;
            class X {
                record R(int i, int j) {}
                record S(R r, int k) {}
            
                int method(S s) {
                    s.r.i = 3;
                    s.k = s.r.j;
                    return s.r.i+s.r.j+s.k;
                }
            }
            """;

    @DisplayName("two levels deep")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        TypeInfo S = X.findSubType("S");
        FieldInfo rInS = S.getFieldByName("r", true);

        MethodInfo method = X.findUniqueMethod("method", 1);
        ParameterInfo s = method.parameters().getFirst();
        {
            Statement s0 = method.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            assertEquals("""
                    a.b.X.R.i#a.b.X.S.r#a.b.X.method(a.b.X.S):0:s, a.b.X.S.r#a.b.X.method(a.b.X.S):0:s, a.b.X.method(a.b.X.S):0:s\
                    """, vd0.knownVariableNamesToString());

            assertNotNull(rInS.source());
            VariableExpression scope = runtime.newVariableExpressionBuilder().setVariable(s).setSource(rInS.source()).build();
            VariableInfo vi0Sri = vd0.variableInfo("a.b.X.R.i#a.b.X.S.r#a.b.X.method(a.b.X.S):0:s");
            String expected = "0:s.r.i←$_ce0";
            assertEquals(expected, vi0Sri.linkedVariables().toString());

            FieldReference sr = runtime.newFieldReference(rInS, scope, rInS.type());
            assertEquals("s.r", sr.toString());
            VariableInfo vi0Sr = vd0.variableInfo(sr);
            assertEquals(expected, vi0Sr.linkedVariables().toString());
            VariableInfo vi0S = vd0.variableInfo(s);
            assertEquals(expected, vi0S.linkedVariables().toString());
        }
        {
            Statement s1 = method.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            assertEquals("0:s.r.i←$_ce0,0:s.r.j→0:s.k",
                    vd1.variableInfo(s).linkedVariables().toString());
        }
    }


    @Language("java")
    private static final String INPUT5b = """
            package a.b;
            import java.util.Set;
            class X {
                record R(int i, int j) {}
                record S(R r, int k) {}
            
                int method(S s) {
                    s.r().i = 3;
                    s.k = s.r().j;
                    return s.r().i+s.r.j()+s.k;
                }
            }
            """;

    @DisplayName("two levels deep, accessors")
    @Test
    public void test5b() {
        TypeInfo X = javaInspector.parse(INPUT5b);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        TypeInfo S = X.findSubType("S");
        FieldInfo rInS = S.getFieldByName("r", true);

        MethodInfo method = X.findUniqueMethod("method", 1);
        ParameterInfo s = method.parameters().getFirst();
        {
            Statement s0 = method.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            assertEquals("""
                    a.b.X.R.i#a.b.X.S.r#a.b.X.method(a.b.X.S):0:s, a.b.X.S.r#a.b.X.method(a.b.X.S):0:s, a.b.X.method(a.b.X.S):0:s\
                    """, vd0.knownVariableNamesToString());

            assertNotNull(rInS.source());
            VariableExpression scope = runtime.newVariableExpressionBuilder().setVariable(s).setSource(rInS.source()).build();
            VariableInfo vi0Sri = vd0.variableInfo("a.b.X.R.i#a.b.X.S.r#a.b.X.method(a.b.X.S):0:s");
            String expected = "0:s.r.i←$_ce0";
            assertEquals(expected, vi0Sri.linkedVariables().toString());

            FieldReference sr = runtime.newFieldReference(rInS, scope, rInS.type());
            assertEquals("s.r", sr.toString());
            VariableInfo vi0Sr = vd0.variableInfo(sr);
            assertEquals(expected, vi0Sr.linkedVariables().toString());
            VariableInfo vi0S = vd0.variableInfo(s);
            assertEquals(expected, vi0S.linkedVariables().toString());
        }
        {
            Statement s1 = method.methodBody().statements().getFirst();
            VariableData vd1 = VariableDataImpl.of(s1);
            assertEquals("0:s.r.i←$_ce0,?", vd1.variableInfo(s).linkedVariables().toString());
        }
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.util.Set;
            class X {
                record R(int i, int j) {}
                record S(R r, int k) {}
                record T(S s, int l) {}
            
                void method1(T t) {
                    t.s.r.i = 3;
                }
                void method2(T t) {
                    t.s().r().i = 3;
                }
            }
            """;

    @DisplayName("three levels deep")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        TypeInfo S = X.findSubType("S");
        TypeInfo T = X.findSubType("T");
        FieldInfo rInS = S.getFieldByName("r", true);
        FieldInfo sInT = T.getFieldByName("s", true);

        MethodInfo method1 = X.findUniqueMethod("method1", 1);
        test6Method(method1, sInT, rInS);
        MethodInfo method2 = X.findUniqueMethod("method2", 1);
        test6Method(method2, sInT, rInS);
    }


    private void test6Method(MethodInfo method, FieldInfo sInT, FieldInfo rInS) {
        ParameterInfo t = method.parameters().getFirst();

        Statement s0 = method.methodBody().statements().getFirst();
        VariableData vd0 = VariableDataImpl.of(s0);

        // at this point, only s.r.i has a static value E=3; s.r and s do not have one ... should they?
        // s.r should have component i=3
        // s should have r.i=3
        VariableExpression scopeT = runtime.newVariableExpressionBuilder().setVariable(t).setSource(t.source()).build();
        FieldReference ts = runtime.newFieldReference(sInT, scopeT, sInT.type());
        assertEquals("t.s", ts.toString());

        VariableExpression scopeTs = runtime.newVariableExpressionBuilder().setVariable(ts).setSource(t.source()).build();
        FieldReference tsr = runtime.newFieldReference(rInS, scopeTs, rInS.type());
        assertEquals("t.s.r", tsr.toString());

        String expected = "0:t.s.r.i←$_ce0";

        VariableInfo vi0Tsr = vd0.variableInfo(tsr);
        assertEquals(expected, vi0Tsr.linkedVariables().toString());

        VariableInfo vi0Ts = vd0.variableInfo(ts);
        assertEquals(expected, vi0Ts.linkedVariables().toString());

        VariableInfo vi0T = vd0.variableInfo(t);
        assertEquals(expected, vi0T.linkedVariables().toString());
    }

}
