package org.e2immu.analyzer.modification.link.typelink;


import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.LinksImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
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

import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.LinkComputerImpl.VARIABLES_LINKED_TO_OBJECT;
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
                    II ii2 = iis.remove(0);
                    ii2.method2(4);
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
            assertEquals("[] --> removeFirst<this.§es", removeFirstMtl.toString());

            Statement s0 = methodB.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo iis = vd0.variableInfo("iis");
            Links tlvIIS = iis.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("iis.§$s~0:input.§$s", tlvIIS.toString());

            Statement s1 = methodB.methodBody().statements().get(1);
            VariableInfo removed1 = VariableDataImpl.of(s1).variableInfo("removed");
            Links tlvT1 = removed1.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("removed<0:input.§$s,removed<iis.§$s", tlvT1.toString());

            Statement callM2 = methodB.methodBody().statements().get(2);
            VariableData vd2 = VariableDataImpl.of(callM2);
            VariableInfo removed = vd2.variableInfoContainerOrNull("removed").best(Stage.EVALUATION);
            Links tlvT2 = removed.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("removed<0:input.§$s,removed<iis.§$s", tlvT2.toString());
        }
        {
            MethodInfo methodA = X.findUniqueMethod("methodA", 1);

            Statement s0 = methodA.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo iis = vd0.variableInfo("iis");
            Links tlvIIS = iis.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("iis.§$s~0:input.§$s", tlvIIS.toString());

            Statement s1 = methodA.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo iis1 = vd1.variableInfo("iis");
            Links tlvIIS1 = iis1.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("iis.§$s~0:input.§$s", tlvIIS1.toString());

            Statement s2 = methodA.methodBody().statements().get(2);
            VariableData vd2 = VariableDataImpl.of(s2);
            VariableInfo iis2 = vd2.variableInfo("iis");
            Links tlvIIS2 = iis2.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("iis.§$s>ii,iis.§$s~0:input.§$s", tlvIIS2.toString());

            Statement s3 = methodA.methodBody().statements().get(3);
            VariableData vd3 = VariableDataImpl.of(s3);
            VariableInfo iis3 = vd3.variableInfo("iis");
            Links tlvIIS3 = iis3.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
            assertEquals("iis.§$s>ii,iis.§$s>ii2,iis.§$s~0:input.§$s", tlvIIS3.toString());

            Statement callM2 = methodA.methodBody().statements().get(4);
            MethodCall methodCall = (MethodCall) callM2.expression();
            assertEquals("ii2.method2(4)", methodCall.toString());
            Value.VariableBooleanMap map = methodCall.analysis().getOrDefault(VARIABLES_LINKED_TO_OBJECT,
                    ValueImpl.VariableBooleanMapImpl.EMPTY);
            assertEquals("a.b.X.methodA(java.util.List<a.b.X.II>):0:input=false, ii2=true, iis=false",
                    nice(map.map()));
        }
    }

}
