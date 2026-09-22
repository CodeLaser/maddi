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
package io.codelaser.maddi.java.openjdk.translate;

import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A local variable carries its assignment expression, and the declaration and every reference share one
 * {@code LocalVariable}. When a translation changes that expression -- here a constant inside a lambda -- the
 * declaration used to get a new variable holding the new lambda while every reference kept the old one, whose
 * implementation type belongs to the type that was translated AWAY. A rewire of the result then met a lambda
 * whose type no longer had a single abstract method (the jfocus transform, 2026-09-21).
 */
public class TestTranslateLocalHoldingLambda extends CommonTest {

    @Language("java")
    public static final String INPUT = """
            package a.b;
            import java.util.Map;
            import java.util.function.Consumer;
            class X {
                void method(Map<String, String> a, Map<String, String> b) {
                    Consumer<Map<String, String>> put = m -> m.put("k", "v");
                    put.accept(a);
                    put.accept(b);
                }
            }
            """;

    @DisplayName("the references follow the declaration: one variable, one lambda, owned by the translated type")
    @Test
    public void test() {
        TypeInfo X = scan("a.b.X", INPUT);
        TranslationMap tm = runtime.newTranslationMapBuilder()
                .setClearAnalysis(true)
                .put(runtime.newStringConstant("k"), runtime.newStringConstant("j"))
                .build();
        TypeInfo translated = X.translate(tm).getFirst();
        assertNotSame(X, translated);
        MethodInfo method = translated.findUniqueMethod("method", 2);
        LocalVariableCreation lvc = (LocalVariableCreation) method.methodBody().statements().get(0);
        LocalVariable declared = lvc.localVariable();
        Lambda lambda = (Lambda) declared.assignmentExpression();
        assertTrue(lambda.methodInfo().methodBody().toString().contains("\"j\""), lambda.methodInfo().methodBody().toString());
        assertSame(translated, lambda.methodInfo().typeInfo().compilationUnitOrEnclosingType().getRight(),
                "the lambda's implementation type belongs to the translated X");
        for (int i = 1; i <= 2; i++) {
            ExpressionAsStatement eas = (ExpressionAsStatement) method.methodBody().statements().get(i);
            MethodCall call = (MethodCall) eas.expression();
            LocalVariable referenced = (LocalVariable) ((VariableExpression) call.object()).variable();
            assertSame(declared, referenced, "statement " + i + " refers to the declared variable, not to a copy");
        }
    }
}
