package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals("copy.§ts∋extract3,copy.§ts~0:list.§ts", vi13copy.linkedVariables().toString());
        VariableInfo vi13list = vd13.variableInfo(extract3.parameters().getFirst());
        assertEquals("0:list.§ts∋extract3,0:list.§ts~copy.§ts", vi13list.linkedVariables().toString());
        assertEquals("[-] --> extract3∈0:list.§ts", mlv3.toString());

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


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Iterator;
            import java.util.List;
            import java.util.Set;
            class X<T> {
                T method(List<T> list) {
                    Iterator<T> iterator = list.iterator();
                    T next = iterator.next();
                    return next;
                }
            }
            """;

    @DisplayName("iterator() in Iterable is independent with an exception")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo add = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlvAdd = add.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(add));

        VariableData add0 = VariableDataImpl.of(add.methodBody().statements().getFirst());
        VariableInfo viIterator0 = add0.variableInfo("iterator");
        assertEquals("iterator.§m☷0:list.§m,iterator.§ts⊆0:list.§ts", viIterator0.linkedVariables().toString());
        // make sure that list is not modified!
        assertEquals("[-] --> method∈0:list.§ts", mlvAdd.toString());
    }



    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Iterator;
            import java.util.List;
            import java.util.Set;
            class X<T> {
                T method(List<T> list) {
                    Iterator<T> iterator = list.iterator();
                    T next = iterator.next();
                    iterator.remove();
                    return next;
                }
            }
            """;

    @DisplayName("iterator() in Iterable is independent with an exception, 2")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo add = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlvAdd = add.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(add));

        VariableData add0 = VariableDataImpl.of(add.methodBody().statements().getFirst());
        VariableInfo viIterator0 = add0.variableInfo("iterator");
        assertEquals("iterator.§m☷0:list.§m,iterator.§ts⊆0:list.§ts", viIterator0.linkedVariables().toString());
        // make sure that list is modified! we must pass the .remove() method
        assertEquals("[-] --> method∈0:list*.§ts", mlvAdd.toString());
    }
}
