package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAssignmentsEmpty extends CommonTest {


    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                public static void method(String[] strings) {
                    for(String s: strings) {
                        int n = s.length();
                        if(n > 0) { }
                    }
                }
            }
            """;


    @DisplayName("merge and empty block")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);

        assertEquals("a.b.X.method(String[]):0:strings, s", vdMethod.knownVariableNamesToString());

        Statement s0 = method.methodBody().statements().get(0);
        VariableData vd0 = VariableDataImpl.of(s0);
        assertEquals("a.b.X.method(String[]):0:strings, s", vd0.knownVariableNamesToString());

        Statement s001 = s0.block().statements().get(1);
        VariableData vd001 = VariableDataImpl.of(s001);
        assertEquals("a.b.X.method(String[]):0:strings, n, s", vd001.knownVariableNamesToString());
        VariableInfoContainer vicN = vd001.variableInfoContainerOrNull("n");
        assertTrue(vicN.hasMerge());
        assertTrue(vicN.hasEvaluation());
        assertSame(vicN.best(Stage.EVALUATION), vicN.best(Stage.MERGE));
    }

}
