package org.e2immu.analyzer.modification.link.typelink;


import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.LinksImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.Stage;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestConstructor extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class X {
                static class II {
                    void method1(String s) { }
                    void method2(int i) { }
                }
                void methodA(List<II> input) {
                    List<II> iis = new ArrayList<>(input);
                    iis.add(new II());
                    for(II ii: iis) ii.method1("abc");
                    iis.remove(0).method2(4);
                }
                void methodB(List<II> input) {
                    List<II> iis = new ArrayList<>(input);
                    II removed = iis.removeFirst();
                    removed.method2(4);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        tlc.doPrimaryType(X);
        {
            MethodInfo methodB = X.findUniqueMethod("methodB", 1);

            TypeInfo list = javaInspector.compiledTypesManager().get(List.class);
            MethodInfo removeFirst = list.findUniqueMethod("removeFirst", 0);
            MethodLinkedVariables removeFirstMtl = removeFirst.analysis().getOrNull(METHOD_LINKS,
                    MethodLinkedVariablesImpl.class);
            assertEquals("[] --> removeFirst<this.es", removeFirstMtl.toString());

            Statement s0 = methodB.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo iis = vd0.variableInfo("iis");
            Links tlvIIS = iis.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("iis.es~0:input.es", tlvIIS.toString());

            Statement s1 = methodB.methodBody().statements().get(1);
            VariableInfo removed1 = VariableDataImpl.of(s1).variableInfo("removed");
            Links tlvT1 = removed1.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("removed<iis.es", tlvT1.toString());

            Statement callM2 = methodB.methodBody().statements().get(2);
            VariableData vd2 = VariableDataImpl.of(callM2);
            VariableInfo removed = vd2.variableInfoContainerOrNull("removed").best(Stage.EVALUATION);
            Links tlvT2 = removed.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("removed<iis.es", tlvT2.toString());
        }
        {
            MethodInfo methodA = X.findUniqueMethod("methodA", 1);

            Statement callM2 = methodA.methodBody().statements().get(3);
            MethodCall methodCall = (MethodCall) callM2.expression();
            Links tlvMc = methodCall.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            // FIXME this is important information but not for modification?
            assertEquals("iis(*:0);input(*:0)", tlvMc.toString());
        }
    }

}
