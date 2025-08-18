package org.e2immu.language.inspection.integration.java.print;

import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Cast;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCastAndMemberAccess extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;
            record X(int x) { }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        Runtime r = javaInspector.runtime();

        ParameterizedType objectArray = r.objectParameterizedType().copyWithArrays(1);
        LocalVariable v = r.newLocalVariable("v", objectArray);
        DependentVariable v0 = r.newDependentVariable(r.newVariableExpression(v), r.intZero(), r.objectParameterizedType());
        assertEquals("v[0]", v0.toString());
        VariableExpression veV0 = r.newVariableExpression(v0);
        Source src = r.newParserSource("-", 0, 0, 1, 1);
        Cast asObjectArray = r.newCastBuilder().setExpression(veV0).setParameterizedType(objectArray).setSource(src).build();
        assertEquals("(Object[])v[0]", asObjectArray.toString());

        DependentVariable d1 = r.newDependentVariable(asObjectArray, r.intOne());
        VariableExpression veD1 = r.newVariableExpression(d1);
        assertEquals("((Object[])v[0])[1]", veD1.toString());

        Cast asX = r.newCastBuilder().setExpression(veD1).setParameterizedType(X.asParameterizedType()).setSource(src).build();
        assertEquals("(X)((Object[])v[0])[1]", asX.toString());

        FieldInfo x = X.getFieldByName("x", true);
        FieldReference dotX = r.newFieldReference(x, asX, r.intParameterizedType());
        assertEquals("((org.e2immu.analyser.resolver.testexample.X)((Object[])v[0])[1]).x", dotX.toString());
    }

    private record X(int x) {
    }

    void testCode(Object[] v) {
        Object[] os = (Object[]) v[0];
        Object o = ((Object[]) v[0])[1];

        int x = ((X) ((Object[]) v[0])[1]).x;
    }
}
