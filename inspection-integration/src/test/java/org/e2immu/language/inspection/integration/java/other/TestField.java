package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestField extends CommonTest {


    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class C {
                public final String s;
                C(String s) {
                    this.s = s;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        assertEquals("C", typeInfo.simpleName());
        MethodInfo C = typeInfo.findConstructor(1);
        ExpressionAsStatement c0 = (ExpressionAsStatement) C.methodBody().statements().get(0);
        Assignment a = (Assignment) c0.expression();
        VariableExpression ve = (VariableExpression) ((FieldReference) a.variableTarget()).scope();
        assertEquals("5-9:5-12", ve.source().compact2());
        DetailedSources ds = ve.source().detailedSources();
        assertNull(ds);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            class C {
              List<String> stringList;
              List<Integer> intList1 = List.of(), intList2, intList3 = null;
              int i, iArray[], j;
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
        assertEquals("C", typeInfo.simpleName());

        FieldInfo stringList = typeInfo.fields().get(0);
        assertEquals("Type java.util.List<String>", stringList.type().toString());
        FieldInfo intList1 = typeInfo.fields().get(1);
        assertEquals("Type java.util.List<Integer>", intList1.type().toString());
        assertEquals("List.of()", intList1.initializer().toString());
        FieldInfo intList2 = typeInfo.fields().get(2);
        assertEquals("Type java.util.List<Integer>", intList2.type().toString());
        assertTrue(intList2.initializer().isEmpty());
        FieldInfo intList3 = typeInfo.fields().get(3);
        assertEquals("Type java.util.List<Integer>", intList2.type().toString());
        assertEquals("null", intList3.initializer().toString());

        FieldInfo iArray = typeInfo.getFieldByName("iArray", true);
        assertEquals("Type int[]", iArray.type().toString());
        FieldInfo j = typeInfo.getFieldByName("j", true);
        assertEquals("Type int", j.type().toString());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class B {
                final static int MAX = 3;
                public boolean m(int j) {
                  return B.MAX < j;
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3);
        assertEquals("B", typeInfo.simpleName());
        Statement s0 = typeInfo.findUniqueMethod("m", 1).methodBody().statements().getFirst();
        if (s0.expression() instanceof BinaryOperator bo) {
            assertEquals("B.MAX", bo.lhs().toString());
            if (bo.lhs() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
                assertEquals("B", fr.scope().toString());
                assertEquals("5-14:5-14", fr.scope().source().compact2());
            } else fail();
        } else fail("Have " + s0.expression());

    }
}
