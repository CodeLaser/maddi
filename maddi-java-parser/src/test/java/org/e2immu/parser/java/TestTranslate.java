package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.TypeExpression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
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
        MethodInfo ccPrint = print.translate(tm).get(0);
        if (ccPrint.methodBody().statements().get(0) instanceof ReturnStatement rs) {
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

        assertEquals("class CC{String print(){return this.s+C.S;}}", cc.print(runtime.qualificationQualifyFromPrimaryType()).toString());
    }
}
