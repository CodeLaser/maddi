package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestAssignmentsReturn extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                static int method(char c) {
                    int i;
                    if(c == '?') {
                        return 0;
                    } else {
                        i  = 1;
                    }
                    return i;
                }
            }
            """;

    @DisplayName("if-else with single return")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);
        assertEquals("a.b.X.method(char), a.b.X.method(char):0:c, i", vdMethod.knownVariableNamesToString());

        VariableInfo iVi = vdMethod.variableInfo("i");
        assertEquals("2", iVi.reads().toString());
        assertEquals("D:0, A:[1.1.0, 1=M]", iVi.assignments().toString());
    }

}
