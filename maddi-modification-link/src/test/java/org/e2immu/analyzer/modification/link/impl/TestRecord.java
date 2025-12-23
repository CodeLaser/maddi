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
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRecord extends CommonTest {

    @Language("java")
    private static final String INPUT_WRAP2 = """
            package a.b;
            import java.util.List;
            class C<X> {
                record R<V>(V v) { }
                <Y> List<R<Y>> wrap(Y y)  { return List.of(new R<>(y)); }
                <Y> List<R<Y>> wrap1(Y y)  { R<Y> r = new R<>(y); List<R<Y>> list = List.of(r); return list; }
            }
            """;

    @DisplayName("T->R[](T), wrapped 2x")
    @Test
    public void testWrap2() {
        TypeInfo C = javaInspector.parse(INPUT_WRAP2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo wrap1 = C.findUniqueMethod("wrap1", 1);
        MethodLinkedVariables mlvWrap1 = wrap1.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(wrap1));

        VariableData vd0 = VariableDataImpl.of(wrap1.methodBody().statements().getFirst());
        VariableInfo viR = vd0.variableInfo("r");
        assertEquals("r.§y≡0:y", viR.linkedVariables().toString());

        VariableData vd1 = VariableDataImpl.of(wrap1.methodBody().statements().get(1));
        VariableInfo viList = vd1.variableInfo("list");
        assertEquals("list.§$s∋r,list.§$s≥0:y", viList.linkedVariables().toString());

        assertEquals("[-] --> wrap1.§$s≥0:y", mlvWrap1.toString());

        MethodInfo wrap = C.findUniqueMethod("wrap", 1);
        MethodLinkedVariables mlvWrap = wrap.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(wrap));
        assertEquals("[-] --> wrap.§$s≥0:y", mlvWrap.toString());
    }
}
