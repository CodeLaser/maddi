package org.e2immu.analyzer.modification.link.impl.collection;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestRedundantLinks extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class X {
                List<String> method(List<String> list) {
                    List<String> l1 = new ArrayList<>(list);
                    List<String> l2 = new ArrayList<>(l1);
                    List<String> l3 = new ArrayList<>(l2);
                    return l3;
                }
            }
            """;

    @DisplayName("simple chain")
    @Test
    public void test1a() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("method", 1);
        ParameterInfo list = method.parameters().getFirst();

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodLinkedVariables mlvSet = tlc.doMethod(method);

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo vi0L1 = vd0.variableInfo("l1");
        assertEquals("l1.§$s⊆0:list.§$s", vi0L1.linkedVariables().toString());
        VariableInfo vi0List = vd0.variableInfo(list);
        assertEquals("0:list.§$s⊇l1.§$s", vi0List.linkedVariables().toString());

        VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
        assertEquals("[l1, a.b.X.method(java.util.List<String>):0:list, l2]",
                vd1.knownVariableNames().toString());
        VariableInfo vi1L1 = vd1.variableInfo("l1");
        assertEquals("l1.§$s⊆0:list.§$s,l1.§$s⊇l2.§$s",
                vi1L1.linkedVariables().toString());
        VariableInfo vi1List = vd1.variableInfo(list);
        // only points to l1
        assertEquals("0:list.§$s⊇l1.§$s,0:list.§$s⊇l2.§$s", vi1List.linkedVariables().toString());
        // TODO: 0:list.§$s⊇l2.§$s used to be ignored, now, we keep it 202604
        VariableInfo vi1L2 = vd1.variableInfo("l2");
        assertEquals("l2.§$s⊆0:list.§$s,l2.§$s⊆l1.§$s", vi1L2.linkedVariables().toString());
        // TODO l2.§$s⊆0:list.§$s used to be ignored, now, we keep it 202604

        VariableData vd3 = VariableDataImpl.of(method.methodBody().statements().getLast());
        VariableInfo vi3L1 = vd3.variableInfo("l1");
        assertEquals("""
                l1.§$s⊇method.§$s,l1.§$s⊆0:list.§$s,l1.§$s⊇l2.§$s,l1.§$s⊇l3.§$s\
                """, vi3L1.linkedVariables().toString());
        VariableInfo vi3L2 = vd3.variableInfo("l2");
        assertEquals("l2.§$s⊇method.§$s,l2.§$s⊆0:list.§$s,l2.§$s⊆l1.§$s,l2.§$s⊇l3.§$s",
                vi3L2.linkedVariables().toString());
        // TODO used to be "l2.§$s⊆l1.§$s" only
        VariableInfo vi3L3 = vd3.variableInfo("l3");
        assertFalse(vi3L3.isModified());
        assertEquals("l3.§$s→method.§$s,l3.§$s⊆0:list.§$s,l3.§$s⊆l1.§$s,l3.§$s⊆l2.§$s,l3→method",
                vi3L3.linkedVariables().toString());
        // TODO l3.§$s⊆0:list.§$s andl3.§$s⊆l1.§$s dropped
        assertTrue(mlvSet.modified().isEmpty());

        assertEquals("[-] --> method.§$s⊆0:list.§$s", mlvSet.toString());
    }

}
