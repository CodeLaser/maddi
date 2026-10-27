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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.TypeExpression;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.This;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestTranslate extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            class C {
              static String S = "abc";
              String s;
              C(String s) { this.s = s; }
              String print() { return this.s + C.S; }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        TypeInfo cc = runtime.newTypeInfo(typeInfo.compilationUnit(), "CC");
        cc.builder().setParentClass(runtime.objectParameterizedType())
                .setTypeNature(runtime.typeNatureClass())
                .setAccess(runtime.accessPackage());
        MethodInfo print = typeInfo.findUniqueMethod("print", 0);
        TranslationMap tm = runtime.newTranslationMapBuilder()
                .put(typeInfo.asSimpleParameterizedType(), cc.asSimpleParameterizedType())
                .build();
        MethodInfo ccPrint = print.translate(tm).getFirst();
        if (ccPrint.methodBody().statements().getFirst() instanceof ReturnStatement rs) {
            if (rs.expression() instanceof BinaryOperator bo) {
                if (bo.rhs() instanceof VariableExpression ve
                    && ve.variable() instanceof FieldReference fr
                    && fr.scope() instanceof TypeExpression te) {
                    assertEquals(cc, te.parameterizedType().typeInfo());
                } else fail();
                if (bo.lhs() instanceof VariableExpression ve
                    && ve.variable() instanceof FieldReference fr
                    && fr.scopeVariable() instanceof This thisVar) {
                    assertEquals(cc, thisVar.typeInfo());
                    assertFalse(fr.isDefaultScope()); // because of a mismatch between the owner and thisVar.typeInfo
                } else fail();
            } else fail();
        } else fail();
        cc.builder().addMethod(ccPrint);

        assertEquals("class CC{String print(){return this.s+CC.S;}}",
                cc.print(runtime.qualificationQualifyFromPrimaryType()).toString());
    }
}
