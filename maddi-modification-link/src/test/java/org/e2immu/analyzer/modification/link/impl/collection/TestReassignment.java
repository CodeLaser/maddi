package org.e2immu.analyzer.modification.link.impl.collection;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
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

public class TestReassignment extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.ArrayList;import java.util.Collections;
            import java.util.List;
            class X {
                String field;
                String second;
                List<String> method(List<String> in) {
                    List<String> copy = in;
                    copy.add(field);
                    copy = new ArrayList<>();
                    copy.add(second);
                    return copy;
                }
            }
            """;

    @DisplayName("Simple reassignment")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputerImpl linkComputer = new LinkComputerImpl(javaInspector);
        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlvListAdd = method.analysis().getOrCreate(METHOD_LINKS,
                () -> linkComputer.doMethod(method));

        VariableData vd2 = VariableDataImpl.of(method.methodBody().statements().get(2));
        VariableInfo field2 = vd2.variableInfo("a.b.X.field");
        assertEquals("this.field∈0:in.§$s", field2.linkedVariables().toString());
        VariableInfo copy2 = vd2.variableInfo("copy");
        // '-' because we're not tracking the intermediary variable
        assertEquals("-", copy2.linkedVariables().toString());

        VariableData vd3 = VariableDataImpl.of(method.methodBody().statements().get(3));
        VariableInfo copy3 = vd3.variableInfo("copy");
        assertEquals("copy.§$s∋this.second", copy3.linkedVariables().toString());

        // important that this.second not part of 0:in
        assertEquals("[0:in*.§$s∋this.field] --> method.§$s∋this.second", mlvListAdd.toString());
    }

}
