package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.LinkComputerImpl.VARIABLES_LINKED_TO_OBJECT;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVariablesLinkedToObject extends CommonTest {
    @Language("java")
    String INPUT1 = """
            package a.b.ii;
            class C1 {
                interface II {
                    void method1(String s);
                    void method2(int i);
                }
                void method2(Object o, String string) {
                   II ii = (II)o; // yes
                   ii.method1(string);
                   II ii2 = (II)o; // no
                   ii2.method2(1);
                }
            }
            """;

    @DisplayName("multiple casts")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(B);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = B.findUniqueMethod("method2", 2);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        assertEquals("[-, -] --> -", mlv.toString());

        VariableData vd2 = VariableDataImpl.of(method.methodBody().statements().get(2));
        VariableInfo vi2ii2 = vd2.variableInfo("ii2");
        assertEquals("ii2.§m≡ii.§m,ii2.§m≡0:o.§m,ii2←0:o,ii2≡ii", vi2ii2.linkedVariables().toString());

        MethodCall mc2 = (MethodCall) method.methodBody().statements().getLast().expression();
        ValueImpl.VariableBooleanMapImpl vbm = mc2.analysis().getOrNull(VARIABLES_LINKED_TO_OBJECT,
                ValueImpl.VariableBooleanMapImpl.class);
        assertEquals("a.b.ii.C1.method2(Object,String):0:o=false, ii2=true", vbm.toString());
    }

}
