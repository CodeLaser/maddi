/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.impl.translate;

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.variable.*;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
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
