package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRecord extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public record R(int a, String b) {}
            """;

    @DisplayName("record")
    @Test
    public void test1() {
        TypeInfo R = javaInspector.parse(INPUT1);
        MethodInfo syntheticConstructor = R.findConstructor(2);
        assertTrue(syntheticConstructor.isSyntheticConstructor());
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(syntheticConstructor);
        Statement s0 = syntheticConstructor.methodBody().statements().get(0);
        assertEquals("this.a=a;", s0.toString());

        VariableData vd0 = VariableDataImpl.of(s0);
        assertEquals("a.b.R.<init>(int,String):0:a, a.b.R.a, a.b.R.this", vd0.knownVariableNamesToString());

        VariableInfo vi0FieldA = vd0.variableInfo("a.b.R.a");
        assertEquals("D:-, A:[0]", vi0FieldA.assignments().toString());
        assertEquals("-", vi0FieldA.reads().toString());

        VariableInfo vi0ParamA = vd0.variableInfo("a.b.R.<init>(int,String):0:a");
        assertEquals("D:-, A:[]", vi0ParamA.assignments().toString());
        assertEquals("0", vi0ParamA.reads().toString());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class X {
                static class M { int i; int get() { return i; } void set(int i) { this.i = i; }}
                record R(M a, M b) {}
                static void modifyA(R r) {
                    M a = r.a;
                    M b = r.b;
                    a.set(3);
                }
            }
            """;

    @DisplayName("modify one component")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);
        MethodInfo modifyA = X.findUniqueMethod("modifyA", 1);
        {
            Statement s0 = modifyA.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            // r, a, and r.a should exist in the first statement
            assertEquals("a, a.b.X.R.a#a.b.X.modifyA(a.b.X.R):0:r, a.b.X.modifyA(a.b.X.R):0:r",
                    vd0.knownVariableNamesToString());
        }
    }

}
