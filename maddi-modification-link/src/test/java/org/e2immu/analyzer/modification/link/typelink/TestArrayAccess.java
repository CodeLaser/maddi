package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.Stage;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestArrayAccess extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            public class X<T> {
                public String nice(T[] array) {
                    StringBuilder sb = new StringBuilder();
                    for(int i=0; i<array.length; ++i) {
                        T t = array[i];
                        sb.append(t);
                    }
                    return sb.toString();
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("nice", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        Statement forStmt = method.methodBody().statements().get(1);
        Statement assign = forStmt.block().statements().getFirst();
        VariableData vd1 = VariableDataImpl.of(assign);
        VariableInfo t1 = vd1.variableInfoContainerOrNull("t").best(Stage.EVALUATION);
        Links tlvT1 = t1.linkedVariablesOrEmpty();
        assertEquals("t←0:array[i],t∈0:array", tlvT1.toString());

        Statement append = forStmt.block().statements().getLast();
        VariableData vd100 = VariableDataImpl.of(append);
        VariableInfo t100 = vd100.variableInfo("t");
        Links tlvT100 = t100.linkedVariablesOrEmpty();
        assertEquals("t←0:array[i],t∈0:array", tlvT100.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            public class X<T> {
                public String nice(T[] array) {
                    StringBuilder sb = new StringBuilder();
                    for(int i=0; i<array.length; ++i) {
                        T t;
                        t = array[i];
                        sb.append(t);
                    }
                    return sb.toString();
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("nice", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        Statement forStmt = method.methodBody().statements().get(1);
        Statement assign = forStmt.block().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(assign);
        VariableInfo t1 = vd1.variableInfoContainerOrNull("t").best(Stage.EVALUATION);
        Links tlvT1 = t1.linkedVariablesOrEmpty();
        assertEquals("t←0:array[i],t∈0:array", tlvT1.toString());

        Statement append = forStmt.block().statements().getLast();
        VariableData vd100 = VariableDataImpl.of(append);
        VariableInfo t100 = vd100.variableInfo("t");
        Links tlvT100 = t100.linkedVariablesOrEmpty();
        assertEquals("t←0:array[i],t∈0:array", tlvT100.toString());
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.List;
            public class X {
                static class II {
                    void method1(String s) { }
                    void method2(int i) { }
                }
                void method() {
                    II[] ii = new II[5];
                    for(int j=0; j<5; j++) {
                      ii[j] = new II();
                      ii[j].method1("3");
                    }
                    ii[3].method2(0);
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 0);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        Statement forStmt = method.methodBody().statements().get(1);
        Statement assign = forStmt.block().statements().getFirst();
        VariableData vd100 = VariableDataImpl.of(assign);
        assertEquals("ii, ii[j], j", vd100.knownVariableNamesToString());
        VariableInfo iij100E = vd100.variableInfo("ii[j]");
        assertEquals("-", iij100E.linkedVariables().toString()); // NOT: ii[j]∈ii
        VariableInfo ii100E = vd100.variableInfo("ii");
        assertEquals("-", ii100E.linkedVariables().toString()); // NOT: ii[j]∈ii

        Statement callMc2 = method.methodBody().statements().getLast();
        VariableData vd2 = VariableDataImpl.of(callMc2);
        VariableInfo ii3 = vd2.variableInfoContainerOrNull("ii[3]").best(Stage.EVALUATION);
        Links tlvII3 = ii3.linkedVariablesOrEmpty();
        assertEquals("-", tlvII3.toString()); // NOT: ii[3]∈ii

        MethodCall mc2 = (MethodCall) callMc2.expression();
        Value.VariableBooleanMap tlvMc = mc2.analysis().getOrNull(LinkComputerImpl.VARIABLES_LINKED_TO_OBJECT,
                ValueImpl.VariableBooleanMapImpl.class);
        assertEquals("ii=true", tlvMc.toString()); // NOT: ii=false, ii[3]=true
    }
}
