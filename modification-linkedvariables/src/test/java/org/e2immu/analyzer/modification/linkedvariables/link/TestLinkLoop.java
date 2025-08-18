package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestLinkLoop extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            class X {
                static <T> void  print(Set<T> set) {
                    for(T t: set) {
                        System.out.println(t);
                    }
                }
            }
            """;

    @DisplayName("add to set, immutable type parameter")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method = X.findUniqueMethod("print", 1);
        VariableData vd = VariableDataImpl.of(method);
        assertNotNull(vd);
        ParameterInfo set = method.parameters().get(0);
        Statement s0 = method.methodBody().statements().get(0);
        Statement s000 = s0.block().statements().get(0);

        VariableData vd000 = VariableDataImpl.of(s000);
        VariableInfo viT = vd000.variableInfo("t");
        assertFalse(viT.isModified());
        assertEquals("*-4-0:set", viT.linkedVariables().toString());
        VariableInfo viSet = vd000.variableInfo(set);
        assertEquals("0-4-*:t", viSet.linkedVariables().toString());
    }

}
