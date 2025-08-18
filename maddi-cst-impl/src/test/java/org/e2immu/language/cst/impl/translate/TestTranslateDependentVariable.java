package org.e2immu.language.cst.impl.translate;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.*;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTranslateDependentVariable {
    private final Runtime r = new RuntimeImpl();

    @Test
    public void test() {
        TypeInfo ti = r.newTypeInfo(r.stringTypeInfo(), "X");
        FieldInfo fi = r.newFieldInfo("array", false, r.intParameterizedType().copyWithArrays(1), ti);
        LocalVariable x = r.newLocalVariable("x", ti.asParameterizedType());
        FieldReference fr = r.newFieldReference(fi,
                r.newVariableExpressionBuilder().setVariable(x).setSource(r.parseSourceFromCompact2("1-2:3-4")).build(),
                fi.type());
        assertEquals("x.array", fr.toString());
        LocalVariable i = r.newLocalVariable("i", r.intParameterizedType());
        DependentVariable dv = r.newDependentVariable(r.newVariableExpression(fr), r.newVariableExpression(i));
        assertEquals("x.array[i]", dv.toString());

        This thisVar = r.newThis(ti.asParameterizedType());
        TranslationMap tm = r.newTranslationMapBuilder()
                .put(x, thisVar) // variable
                .put(r.newVariableExpression(i), r.intOne()) // expression
                .build();
        Variable variable = tm.translateVariable(dv);
        // !!! x.array[i] is not present in the map; we must go via VariableExpression
        assertEquals("x.array[i]", variable.toString());
        Variable translated = tm.translateVariableRecursively(variable);
        assertEquals("this.array[1]", translated.toString());
    }
}
