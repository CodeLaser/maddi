/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
