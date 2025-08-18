package org.e2immu.analyzer.modification.linkedvariables.staticvalues;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.StaticValues;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestStaticValuesAssignment extends CommonTest {

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
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method = X.findUniqueMethod("method", 0);
        {
            Statement s0 = method.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi0J = vd0.variableInfo("j");
            assertEquals("", vi0J.linkedVariables().toString());
            assertEquals("E=3", vi0J.staticValues().toString());
        }
        {
            Statement s1 = method.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);

            VariableInfo vi1Rv = vd1.variableInfo(method.fullyQualifiedName());
            assertEquals("-1-:j", vi1Rv.linkedVariables().toString());
            assertEquals("E=3", vi1Rv.staticValues().toString());
        }

        StaticValues methodSv = method.analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
        assertEquals("E=3", methodSv.toString());
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
                static X method() {
                    X x = new X().setJ(3);
                    return x;
                }
            }
            """;

    @DisplayName("assignment to field in builder")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepWork(X);

        MethodInfo method = X.findUniqueMethod("method", 0);
        {
            VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().get(0));
            assertEquals("a.b.X.j#scope16-15:16-21, x", vd0.knownVariableNamesToString());
        }

        analyzer.go(analysisOrder);

        FieldInfo fieldJ = X.getFieldByName("j", true);
        MethodInfo setJ = X.findUniqueMethod("setJ", 1);
        {
            Statement s0 = setJ.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi0J = vd0.variableInfo(runtime.newFieldReference(fieldJ));
            assertEquals("-1-:jp", vi0J.linkedVariables().toString());
            assertEquals("E=jp", vi0J.staticValues().toString());
        }
        {
            Statement s1 = setJ.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);

            VariableInfo vi1Rv = vd1.variableInfo(setJ.fullyQualifiedName());
            assertEquals("-1-:this", vi1Rv.linkedVariables().toString());
            assertEquals("E=this this.j=jp", vi1Rv.staticValues().toString());
        }

        StaticValues setJSv = setJ.analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
        assertEquals("E=this this.j=jp", setJSv.toString());

        MethodInfo setJK = X.findUniqueMethod("setJK", 2);
        StaticValues setJKSv = setJK.analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
        assertEquals("E=this this.j=jp, this.k=kp", setJKSv.toString());

        {
            Statement s0 = method.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi0X = vd0.variableInfo("x");
            assertEquals("Type a.b.X E=new X() this.j=3", vi0X.staticValues().toString());
        }
        StaticValues methodSv = method.analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
        assertEquals("Type a.b.X E=new X() this.j=3", methodSv.toString());
    }


    @Language("java")
    private static final String INPUT2b = """
            package a.b;
            import java.util.Set;
            class X {
                int j;
                int k;
                void setJ(int jp) {
                    j=jp;
                }
                void setJK(int jp, int kp) {
                    j=jp;
                    k=kp;
                }
                static X method() {
                    X x = new X();
                    x.setJ(3);
                    return x;
                }
            }
            """;

    @DisplayName("assignment to field in builder, non-fluent")
    @Test
    public void test2b() {
        TypeInfo X = javaInspector.parse(INPUT2b);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        FieldInfo fieldJ = X.getFieldByName("j", true);
        MethodInfo setJ = X.findUniqueMethod("setJ", 1);
        {
            Statement s0 = setJ.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi0J = vd0.variableInfo(runtime.newFieldReference(fieldJ));
            assertEquals("-1-:jp", vi0J.linkedVariables().toString());
            assertEquals("E=jp", vi0J.staticValues().toString());
        }

        StaticValues setJSv = setJ.analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
        assertNotNull(setJSv);
        assertEquals("this.j=jp", setJSv.toString());

        MethodInfo setJK = X.findUniqueMethod("setJK", 2);
        StaticValues setJKSv = setJK.analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
        assertEquals("this.j=jp, this.k=kp", setJKSv.toString());

        MethodInfo method = X.findUniqueMethod("method", 0);
        {
            Statement s0 = method.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi0X = vd0.variableInfo("x");
            assertEquals("Type a.b.X E=new X()", vi0X.staticValues().toString());
        }
        {
            Statement s1 = method.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);

            VariableInfo vi1X = vd1.variableInfo("x");
            assertEquals("Type a.b.X E=new X() this.j=3", vi1X.staticValues().toString());
        }
        StaticValues methodSv = method.analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
        assertEquals("Type a.b.X E=new X() this.j=3", methodSv.toString());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Set;
            record X(int j, int k) {
            
                class Builder {
                    int j;
                    int k;
                    Builder setJ(int jp) {
                        j=jp;
                        return this;
                    }
                    Builder setK(int kp) {
                        k=kp;
                        return this;
                    }
                    Builder setJK(int jp, int kp) {
                        j=jp;
                        k=kp;
                        return this;
                    }
                    X build() {
                        return new X(j, k);
                    }
                }
                static X justJ4(int jp) {
                    Builder b = new Builder().setJK(jp, 4);
                    return b.build();
                }
                static X justJ(int jp) {
                    Builder b = new Builder().setJ(jp);
                    return b.build();
                }
                static X setJK(int jp, int kp) {
                    Builder b = new Builder().setJ(jp).setK(kp);
                    return b.build();
                }
            }
            """;

    @DisplayName("from builder to built class")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        TypeInfo builder = X.findSubType("Builder");
        MethodInfo build = builder.findUniqueMethod("build", 0);
        StaticValues svBuild = build.analysis().getOrDefault(STATIC_VALUES_METHOD, NONE);
        assertEquals("Type a.b.X E=new X(this.j,this.k) this.j=this.j, this.k=this.k", svBuild.toString());

        MethodInfo builderSetJK = builder.findUniqueMethod("setJK", 2);
        assertTrue(builderSetJK.isFluent());

        MethodInfo builderSetJ = builder.findUniqueMethod("setJ", 1);
        assertTrue(builderSetJ.isFluent());
        assertSame(builder.getFieldByName("j", true), builderSetJ.getSetField().field());

        MethodInfo builderSetK = builder.findUniqueMethod("setK", 1);
        assertTrue(builderSetK.isFluent());
        assertSame(builder.getFieldByName("k", true), builderSetK.getSetField().field());

        {
            MethodInfo justJ = X.findUniqueMethod("justJ", 1);
            Statement s0 = justJ.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0B = vd0.variableInfo("b");
            assertEquals("Type a.b.X.Builder E=new Builder() this.j=jp", vi0B.staticValues().toString());

            Statement s1 = justJ.methodBody().lastStatement();
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo vi1Rv = vd1.variableInfo(justJ.fullyQualifiedName());
            assertEquals("Type a.b.X E=new Builder() this.j=jp", vi1Rv.staticValues().toString());
        }

        {
            MethodInfo justJ4 = X.findUniqueMethod("justJ4", 1);
            Statement s0 = justJ4.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0B = vd0.variableInfo("b");
            assertEquals("this.j=jp, this.k=4", vi0B.staticValues().toString());

            Statement s1 = justJ4.methodBody().lastStatement();
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo vi1Rv = vd1.variableInfo(justJ4.fullyQualifiedName());
            assertEquals("Type a.b.X E=b this.j=jp, this.k=4", vi1Rv.staticValues().toString());
        }

        {
            MethodInfo setJK = X.findUniqueMethod("setJK", 2);
            assertFalse(setJK.isFluent());

            Statement s0 = setJK.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0B = vd0.variableInfo("b");
            assertEquals("Type a.b.X.Builder E=new Builder() this.j=jp, this.k=kp", vi0B.staticValues().toString());

            Statement s1 = setJK.methodBody().lastStatement();
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo vi1Rv = vd1.variableInfo(setJK.fullyQualifiedName());
            assertEquals("Type a.b.X E=new Builder() this.j=jp, this.k=kp", vi1Rv.staticValues().toString());
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
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        TypeInfo R = X.findSubType("R");
        FieldInfo iField = R.getFieldByName("i", true);

        MethodInfo method = X.findUniqueMethod("method", 1);
        ParameterInfo r = method.parameters().get(0);
        {
            Statement s0 = method.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi0R = vd0.variableInfo(r);
            assertEquals("", vi0R.linkedVariables().toString());
            assertEquals("this.i=3", vi0R.staticValues().toString());

            VariableExpression scope = runtime.newVariableExpressionBuilder().setVariable(r).setSource(iField.source()).build();
            Variable ri = runtime.newFieldReference(iField, scope, iField.type());
            assertEquals("r.i", ri.toString());
            VariableInfo vi0Ri = vd0.variableInfo(ri);
            assertEquals("", vi0Ri.linkedVariables().toString());
            assertEquals("E=3", vi0Ri.staticValues().toString());
        }
        {
            Statement s1 = method.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);

            VariableInfo vi1Rv = vd1.variableInfo(method.fullyQualifiedName());
            assertEquals("", vi1Rv.linkedVariables().toString());
            assertEquals("", vi1Rv.staticValues().toString());
        }

        StaticValues methodSv = method.analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
        assertEquals("", methodSv.toString());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.Set;
            class X {
                record R(int i, int j) {}
                record S(R r, int k) {}
            
                int method(S s) {
                    s.r().i = 3;
                    s.k = s.r.j;
                    return s.r().i+s.r.j()+s.k;
                }
            }
            """;

    @DisplayName("two levels deep")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        TypeInfo R = X.findSubType("R");
        FieldInfo iField = R.getFieldByName("i", true);
        TypeInfo S = X.findSubType("S");
        FieldInfo rInS = S.getFieldByName("r", true);

        MethodInfo method = X.findUniqueMethod("method", 1);
        ParameterInfo s = method.parameters().get(0);
        {
            Statement s0 = method.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            assertEquals("""
                    a.b.X.R.i#a.b.X.S.r#a.b.X.method(a.b.X.S):0:s, a.b.X.S.r#a.b.X.method(a.b.X.S):0:s, a.b.X.method(a.b.X.S):0:s\
                    """, vd0.knownVariableNamesToString());

            assertNotNull(rInS.source());
            VariableExpression scope = runtime.newVariableExpressionBuilder().setVariable(s).setSource(rInS.source()).build();
            VariableInfo vi0Sri = vd0.variableInfo("a.b.X.R.i#a.b.X.S.r#a.b.X.method(a.b.X.S):0:s");
            assertEquals("E=3", vi0Sri.staticValues().toString());

            FieldReference sr = runtime.newFieldReference(rInS, scope, rInS.type());
            assertEquals("s.r", sr.toString());
            VariableInfo vi0Sr = vd0.variableInfo(sr);
            assertEquals("this.i=3", vi0Sr.staticValues().toString());
            VariableInfo vi0S = vd0.variableInfo(s);
            assertEquals("this.r.i=3", vi0S.staticValues().toString());
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
                    t.s.r().i = 3;
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
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        TypeInfo S = X.findSubType("S");
        TypeInfo T = X.findSubType("T");
        FieldInfo rInS = S.getFieldByName("r", true);
        FieldInfo sInT = T.getFieldByName("s", true);

        MethodInfo method1 = X.findUniqueMethod("method1", 1);
        test6Method1(method1, sInT, rInS);
        MethodInfo method2 = X.findUniqueMethod("method2", 1);
        test6Method2(method2, sInT, rInS);
    }

    private void test6Method1(MethodInfo method, FieldInfo sInT, FieldInfo rInS) {
        ParameterInfo t = method.parameters().get(0);

        Statement s0 = method.methodBody().statements().get(0);
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

        VariableInfo vi0Tsr = vd0.variableInfo(tsr);
        assertEquals("this.i=3", vi0Tsr.staticValues().toString());

        VariableInfo vi0Ts = vd0.variableInfo(ts);
        assertEquals("this.r.i=3", vi0Ts.staticValues().toString());

        VariableInfo vi0T = vd0.variableInfo(t);
        assertEquals("this.s.r.i=3", vi0T.staticValues().toString());
    }

    private void test6Method2(MethodInfo method, FieldInfo sInT, FieldInfo rInS) {
        ParameterInfo t = method.parameters().get(0);

        Statement s0 = method.methodBody().statements().get(0);
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

        VariableInfo vi0Tsr = vd0.variableInfo(tsr);
        assertEquals("this.i=3", vi0Tsr.staticValues().toString());

        VariableInfo vi0Ts = vd0.variableInfo(ts);
        assertEquals("this.r.i=3", vi0Ts.staticValues().toString());

        VariableInfo vi0T = vd0.variableInfo(t);
        assertEquals("this.s.r.i=3", vi0T.staticValues().toString());
    }

}
