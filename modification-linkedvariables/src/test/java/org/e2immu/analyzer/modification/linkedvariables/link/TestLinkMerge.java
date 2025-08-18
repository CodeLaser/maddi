package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestLinkMerge extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            class X {
                static <T> boolean setAdd(Set<T> set, T s) {
                    if(s != null) {
                        return set.add(s);
                    }
                    return false;
                }
            }
            """;

    @DisplayName("simple merge")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo setAdd = X.findUniqueMethod("setAdd", 2);
        VariableData vd = VariableDataImpl.of(setAdd);
        assertNotNull(vd);
        ParameterInfo set = setAdd.parameters().getFirst();
        Statement s0 = setAdd.methodBody().statements().getFirst();
        Statement s000 = s0.block().statements().getFirst();

        VariableData vd000 = VariableDataImpl.of(s000);
        VariableInfo viSet000 = vd000.variableInfo(set);
        assertFalse(viSet000.isUnmodified());
        assertEquals("0-4-*:s", viSet000.linkedVariables().toString());

        VariableData vd0 = VariableDataImpl.of(s0);
        VariableInfo viSet0 = vd0.variableInfo(set);
        assertEquals("0-4-*:s", viSet0.linkedVariables().toString());
        assertFalse(viSet0.isUnmodified());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class X {
                static <T> boolean setAdd(Set<T> set, T t) {
                    Set<T> i;
                    if(t == null) {
                        i = null;
                    } else {
                        i = set;
                    }
                    if(i != null) {
                        i.add(t);
                    }
                    return false;
                }
            }
            """;

    @DisplayName("simple merge, one intermediate local variable")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo setAdd = X.findUniqueMethod("setAdd", 2);
        VariableData vd = VariableDataImpl.of(setAdd);
        assertNotNull(vd);
        ParameterInfo set = setAdd.parameters().getFirst();
        {
            IfElseStatement s1 = (IfElseStatement) setAdd.methodBody().statements().get(1);
            {
                Statement s110 = s1.elseBlock().statements().getFirst();
                VariableData vd110 = VariableDataImpl.of(s110);
                VariableInfo viI100 = vd110.variableInfo("i");
                assertEquals("-1-:set", viI100.linkedVariables().toString());
                VariableInfo viSet100 = vd110.variableInfo(set);
                assertEquals("-1-:i", viSet100.linkedVariables().toString());
            }
            {
                VariableData vd1 = VariableDataImpl.of(s1);
                VariableInfo viI1 = vd1.variableInfo("i");
                assertEquals("-1-:set", viI1.linkedVariables().toString());
                VariableInfo viSet1 = vd1.variableInfo(set);
                assertEquals("-1-:i", viSet1.linkedVariables().toString());
            }
        }
        {
            IfElseStatement s2 = (IfElseStatement) setAdd.methodBody().statements().get(2);
            {
                Statement s200 = s2.block().statements().getFirst();
                VariableData vd200 = VariableDataImpl.of(s200);
                VariableInfo viI200 = vd200.variableInfo("i");
                assertEquals("-1-:set, 0-4-*:t", viI200.linkedVariables().toString());
                assertFalse(viI200.isUnmodified());

                VariableInfo viSet200 = vd200.variableInfo(set);
                assertEquals("-1-:i, 0-4-*:t", viSet200.linkedVariables().toString());

                // test the propagation of the @Modified property via clustering
                assertFalse(viSet200.isUnmodified());
            }
            {
                VariableData vd2 = VariableDataImpl.of(s2);
                VariableInfo viI2 = vd2.variableInfo("i");
                assertEquals("-1-:set, 0-4-*:t", viI2.linkedVariables().toString());
                assertFalse(viI2.isUnmodified());

                VariableInfo viSet2 = vd2.variableInfo(set);
                assertEquals("-1-:i, 0-4-*:t", viSet2.linkedVariables().toString());

                // test the propagation of the @Modified property via merge
                assertFalse(viSet2.isUnmodified());
            }
        }
    }
}
