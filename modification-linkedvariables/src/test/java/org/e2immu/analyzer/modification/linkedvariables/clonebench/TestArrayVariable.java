package org.e2immu.analyzer.modification.linkedvariables.clonebench;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class TestArrayVariable extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.lang.reflect.Array;
            public class C {
                public static void put(Object array, Object element, int index) {
                    if (array instanceof Object[]) {
                        ((Object[]) array)[index] = element;
                    } else {
                        Array.set(array, index, element);
                    }
                }
            }
            """;

    @DisplayName("array variable")
    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(C);
        analyzer.go(ao);
        MethodInfo put = C.findUniqueMethod("put", 3);
        ParameterInfo put0 = put.parameters().get(0);

        Statement s000 = put.methodBody().statements().get(0).block().statements().get(0);
        VariableData vd000 = VariableDataImpl.of(s000);

        Variable av = ((DependentVariable) ((Assignment) s000.expression()).variableTarget()).arrayVariable();
        assertSame(put0, av);

        VariableInfo viPut0 = vd000.variableInfo(put0);
        assertEquals("0-4-*:array[index], 0-4-*:element", viPut0.linkedVariables().toString());
        assertEquals("0-4-*:array[index], 0-4-*:element", put0.analysis()
                .getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER, LinkedVariablesImpl.class).toString());
        /*
        20241229
        part of an as yet not fully resolved issue: should "array" be @Modified or not?
        of course its object graph is modified, but it is done through casting.

        the current code would have to use the parameterized type of DV.arrayExpression() rather that
        that of DV.arrayVariable() to allow for -2- links which would propagate the modification.
         */
        assertTrue(put0.isModified());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class X {
              public void method() {
                for (int i = 0; i < 8; i++) {
                  for (int id = 0; id < 8; id++) {
                    OldBoard[i][id][move] = TheBoard[i][id];
                  }
                }
              }
            
              int[][] TheBoard;
              int[][][] OldBoard;
              int move = 0;
            }
            """;

    @DisplayName("array variable")
    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2);
        List<Info> ao = prepWork(C);
        analyzer.go(ao);
    }
}
