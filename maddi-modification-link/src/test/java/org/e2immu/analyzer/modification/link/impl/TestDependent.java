package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.Stage;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

// FIXME independent vs dependent in normal method linking

public class TestDependent extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.ArrayList;import java.util.List;
            class X<T> {
                T extract1(List<T> list) {
                    return list.getFirst(); // non-modifying
                }
                T extract2(List<T> list) {
                    return list.removeFirst(); // modifying
                }
                T extract3(List<T> list) {
                    List<T> copy = new ArrayList<>(list);
                    return copy.removeFirst(); // modifying, but not dependent-linked to list
                }
                T extract4(List<T> list) {
                    List<T> copy = list;
                    return copy.removeFirst(); // modifying, trivially dependent-linked to list
                }
                T extract5(List<T> list) {
                    List<T> sub = list.subList(0, 10);
                    return sub.removeFirst(); // modifying, dependent-linked to list
                }
            }
            """;

    @DisplayName("classic example of (in)dependent linking")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo extract1 = X.findUniqueMethod("extract1", 1);
        MethodLinkedVariables mlv1 = extract1.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(extract1));
        assertEquals("[-] --> extract1∈0:list.§ts", mlv1.toString());

        MethodInfo extract2 = X.findUniqueMethod("extract2", 1);
        MethodLinkedVariables mlv2 = extract2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(extract2));
        assertEquals("[-] --> extract2∈0:list*.§ts", mlv2.toString());

        MethodInfo extract3 = X.findUniqueMethod("extract3", 1);
        MethodLinkedVariables mlv3 = extract3.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(extract3));
        VariableData vd03 = VariableDataImpl.of(extract3.methodBody().statements().getFirst());
        VariableInfo vi03copy = vd03.variableInfo("copy");
        assertEquals("copy.§ts⊆0:list.§ts", vi03copy.linkedVariables().toString());
        VariableData vd13 = VariableDataImpl.of(extract3.methodBody().statements().get(1));
        VariableInfo vi13copy = vd13.variableInfo("copy");
        assertEquals("copy.§ts~0:list.§ts,copy.§ts∋extract3", vi13copy.linkedVariables().toString());
        VariableInfo vi13list = vd13.variableInfo(extract3.parameters().getFirst());
        // FIXME 1
        //assertEquals("0:list.§t~copy.§ts", vi13list.linkedVariables().toString());
        // FIXME 2
        assertEquals("[-] --> extract3∈0:list*.§ts", mlv3.toString());

        MethodInfo extract4 = X.findUniqueMethod("extract4", 1);
        MethodLinkedVariables mlv4 = extract4.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(extract4));
        VariableData vd04 = VariableDataImpl.of(extract4.methodBody().statements().getFirst());
        VariableInfo vi04copy = vd04.variableInfo("copy");
        assertEquals("copy←0:list", vi04copy.linkedVariables().toString());
        assertEquals("[-] --> extract4∈0:list*.§ts", mlv4.toString());

        MethodInfo extract5 = X.findUniqueMethod("extract5", 1);
        MethodLinkedVariables mlv5 = extract5.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(extract5));
        VariableData vd05 = VariableDataImpl.of(extract5.methodBody().statements().getFirst());
        VariableInfo vi05sub = vd05.variableInfo("sub");
        assertEquals("sub.§m≡0:list.§m,sub.§ts⊆0:list.§ts", vi05sub.linkedVariables().toString());
        assertEquals("[-] --> extract5∈0:list*.§ts", mlv5.toString());
    }
}
