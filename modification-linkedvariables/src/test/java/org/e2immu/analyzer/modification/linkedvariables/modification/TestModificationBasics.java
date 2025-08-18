package org.e2immu.analyzer.modification.linkedvariables.modification;

import org.e2immu.analyzer.modification.linkedvariables.IteratingAnalyzer;
import org.e2immu.analyzer.modification.linkedvariables.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.linkedvariables.impl.MethodModAnalyzerImpl;
import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestModificationBasics extends CommonTest {
    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.List;
            import java.util.Iterator;
            class Test {
                public Iterator<String> m(List<String> items) {
                    return items.iterator();
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo X = javaInspector.parse(INPUT);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo m = X.findUniqueMethod("m", 1);
        assertFalse(m.isModifying());
        assertFalse(m.parameters().getFirst().isModified());
    }

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.ArrayList;import java.util.List;
            class X {
                static class M {
                    int i;
                    void setI(int i) { this.i = i; }
                }
                void modifyParam(List<M> list, int k) {
                    list.get(0).setI(k);
                }
                void modifyParam2(List<M> list, int k) {
                    M m = list.get(0);
                    m.setI(k);
                }
                void modifyParam3(List<M> list, int k) {
                    List<M> copy = new ArrayList<>(list);
                    M m = list.get(0);
                    m.setI(k);
                }
            }
            """;


    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        {
            MethodInfo m = X.findUniqueMethod("modifyParam", 2);
            assertFalse(m.isModifying());
            ParameterInfo pi0 = m.parameters().getFirst();
            assertFalse(pi0.isUnmodified());
        }
        {
            MethodInfo m = X.findUniqueMethod("modifyParam2", 2);
            assertFalse(m.isModifying());
            ParameterInfo pi0 = m.parameters().getFirst();
            assertFalse(pi0.isUnmodified());
            VariableData vd0 = VariableDataImpl.of( m.methodBody().statements().getFirst());
            VariableInfo vi0m = vd0.variableInfo("m");
            assertEquals("*M-2-0M|*-0:_synthetic_list, -1-:_synthetic_list[0], *M-2-0M|*-0.0:list",
                    vi0m.linkedVariables().toString());
        }
        {
            MethodInfo m = X.findUniqueMethod("modifyParam3", 2);
            assertFalse(m.isModifying());
            ParameterInfo pi0 = m.parameters().getFirst();
            assertFalse(pi0.isUnmodified());
            {
                VariableData vd1 = VariableDataImpl.of(m.methodBody().statements().get(1));
                VariableInfo vi1m = vd1.variableInfo("m");
                assertEquals("*M-2-0M|*-0:_synthetic_list, -1-:_synthetic_list[0], *M-4-0M:copy, *M-2-0M|*-0.0:list",
                        vi1m.linkedVariables().toString());
                VariableInfo vi1list = vd1.variableInfo(pi0);
                assertFalse(vi1list.isModified());
            }
            {
                VariableData vd2 = VariableDataImpl.of(m.methodBody().statements().get(2));
                VariableInfo vi2m = vd2.variableInfo("m");
                assertEquals("""
                                *M-2-0M|*-0:_synthetic_list, -1-:_synthetic_list[0], *M-4-0M:copy, -2-|*-0:i, \
                                -2-|*-0:k, *M-2-0M|*-0.0:list\
                                """,
                        vi2m.linkedVariables().toString());
                VariableInfo vi1list = vd2.variableInfo(pi0);
                assertTrue(vi1list.isModified());
            }
        }
    }
}
