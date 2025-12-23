package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestForEach extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            public class X<T> {
                public String nice(List<T> list) {
                    StringBuilder sb = new StringBuilder();
                    for(T t: list) {
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
        ParameterInfo list = method.parameters().getFirst();
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        TypeInfo iterator = javaInspector.compiledTypesManager().get(Iterator.class);
        MethodInfo next = iterator.findUniqueMethod("next", 0);
        MethodLinkedVariables mtlNext = next.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[] --> next∈this.§es", mtlNext.toString());

        Statement forEach = method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(forEach);
        VariableInfo t1 = vd1.variableInfoContainerOrNull("t").best(Stage.EVALUATION);
        Links tlvT1 = t1.linkedVariablesOrEmpty();
        assertEquals("t∈0:list.§ts,t≤0:list", tlvT1.toString());

        VariableInfo list1 = vd1.variableInfo(list, Stage.EVALUATION);
        Links lvList1 = list1.linkedVariablesOrEmpty();
        assertEquals("t∈0:list.§ts,t≤0:list", lvList1.toString());

        Statement append = forEach.block().statements().getFirst();
        VariableData vd100 = VariableDataImpl.of(append);
        VariableInfo t100 = vd100.variableInfo("t");
        Links tlvT100 = t100.linkedVariablesOrEmpty();
        assertEquals("t∈0:list.§ts,t≤0:list", tlvT100.toString());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class X<T> {
                 static class II {
                    void method1(String s) { }
                    void method2(int i) { }
                 }
                 void method() {
                     List<II> iis = new ArrayList<>(); // no
                     iis.add(new II());
                     for(II ii: iis) ii.method2(2); // no
                 }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 0);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        Statement add = method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(add);
        VariableInfo iis1 = vd1.variableInfoContainerOrNull("iis").best();
        Links tlvT1 = iis1.linkedVariablesOrEmpty();
        assertEquals("-", tlvT1.toString());

        Statement forEach = method.methodBody().statements().get(2);
        VariableData vd2 = VariableDataImpl.of(forEach);
        VariableInfo ii2 = vd2.variableInfoContainerOrNull("ii").best(Stage.EVALUATION);
        Links tlvT2E = ii2.linkedVariablesOrEmpty();
        assertEquals("ii∈iis.§$s", tlvT2E.toString());

        Statement call2 = forEach.block().statements().getFirst();
        VariableData vd200 = VariableDataImpl.of(call2);
        VariableInfo ii200 = vd200.variableInfo("ii");
        Links tlvII200 = ii200.linkedVariablesOrEmpty();
        assertEquals("ii∈iis.§$s", tlvII200.toString(), "Should have been inherited from previous");
        MethodCall methodCall = (MethodCall) call2.expression();

        // test writeMethodCall
        Value.VariableBooleanMap linkedToObject = methodCall.analysis()
                .getOrDefault(LinkComputerImpl.VARIABLES_LINKED_TO_OBJECT, ValueImpl.VariableBooleanMapImpl.EMPTY);
        assertEquals("ii=true, iis=false", nice(linkedToObject.map()));
    }
}
